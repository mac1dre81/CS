package com.keyvault.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.keyvault.app.R
import com.keyvault.app.ads.InterstitialAdManager
import com.keyvault.app.billing.BillingManager
import com.keyvault.app.crypto.AesGcmCrypto
import com.keyvault.app.crypto.PasswordCrypto
import com.keyvault.app.crypto.SecretKeyProvider
import com.keyvault.app.data.AndroidSecretKeyProvider
import com.keyvault.app.data.BackupCodec
import com.keyvault.app.data.KeyRepositoryImpl
import com.keyvault.app.data.local.KeyDao
import com.keyvault.app.data.local.KeyDatabase
import com.keyvault.app.domain.KeyRepository
import com.keyvault.app.domain.ProState
import com.keyvault.app.domain.usecase.AddKeyUseCase
import com.keyvault.app.domain.usecase.CanAddKeyUseCase
import com.keyvault.app.domain.usecase.DeleteKeyUseCase
import com.keyvault.app.domain.usecase.ExportBackupUseCase
import com.keyvault.app.domain.usecase.ImportBackupUseCase
import com.keyvault.app.domain.usecase.ObserveKeysUseCase
import com.keyvault.app.domain.usecase.SearchAndFilterUseCase
import com.keyvault.app.domain.usecase.TestKeyUseCase
import com.keyvault.app.domain.usecase.UpdateKeyUseCase
import com.keyvault.app.network.KeyTester
import com.keyvault.app.network.realKeyTester
import com.keyvault.app.prefs.SettingsRepository
import com.keyvault.app.billing.SettingsProState
import com.keyvault.app.util.ClipboardHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Manual service locator (no DI framework). Keeps the build lightweight and
 * avoids annotation-processing surface area. Each entry is a process singleton.
 */
class ServiceLocator private constructor(context: Context) {

    private val appContext = context.applicationContext

    // --- Persistence ---
    val database: KeyDatabase by lazy {
        Room.databaseBuilder(appContext, KeyDatabase::class.java, "keyvault.db")
            .fallbackToDestructiveMigration()
            .build()
    }
    val keyDao: KeyDao by lazy { database.keyDao() }

    // --- Crypto ---
    val secretKeyProvider: SecretKeyProvider by lazy { AndroidSecretKeyProvider(appContext) }
    val crypto: AesGcmCrypto by lazy { AesGcmCrypto() }
    val passwordCrypto: PasswordCrypto by lazy { PasswordCrypto() }
    val backupCodec: BackupCodec by lazy { BackupCodec(passwordCrypto, BackupCodec.defaultJson) }

    // --- Repository ---
    val keyRepository: KeyRepository by lazy {
        KeyRepositoryImpl(keyDao, secretKeyProvider, crypto, Dispatchers.IO)
    }

    // --- Networking (test ping only; no logging interceptor so keys are never logged) ---
    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
    val keyTester: KeyTester by lazy { realKeyTester(httpClient) }

    // --- Preferences / Pro ---
    val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create { appContext.preferencesDataStoreFile("settings.pb") }
    }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(dataStore) }
    val proState: ProState by lazy { SettingsProState(settingsRepository) }
    val clipboardHelper: ClipboardHelper by lazy { ClipboardHelper(appContext) }

    // --- Ads (free-tier interstitial; Pro users never receive show events) ---
    val interstitialAdManager: InterstitialAdManager by lazy {
        InterstitialAdManager(appContext, appContext.getString(R.string.admob_test_interstitial))
    }

    // --- Use cases ---
    val observeKeys by lazy { ObserveKeysUseCase(keyRepository) }
    val searchAndFilter by lazy { SearchAndFilterUseCase() }
    val addKey by lazy { AddKeyUseCase(keyRepository, proState) }
    val updateKey by lazy { UpdateKeyUseCase(keyRepository) }
    val deleteKey by lazy { DeleteKeyUseCase(keyRepository) }
    val canAddKey by lazy { CanAddKeyUseCase() }
    val testKey by lazy { TestKeyUseCase(keyRepository, keyTester) }
    val exportBackup by lazy { ExportBackupUseCase(keyRepository, backupCodec) }
    val importBackup by lazy { ImportBackupUseCase(keyRepository, backupCodec) }

    // --- Billing ---
    val billingManager: BillingManager by lazy {
        BillingManager(
            appContext,
            settingsRepository,
            CoroutineScope(SupervisorJob() + Dispatchers.IO)
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: ServiceLocator? = null

        fun getInstance(context: Context): ServiceLocator =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ServiceLocator(context.applicationContext).also { INSTANCE = it }
            }
    }
}
