package com.credstow.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.credstow.app.R
import com.credstow.app.ads.InterstitialAdManager
import com.credstow.app.billing.BillingManager
import com.credstow.app.crypto.AesGcmCrypto
import com.credstow.app.crypto.PasswordCrypto
import com.credstow.app.crypto.SecretKeyProvider
import com.credstow.app.data.AndroidSecretKeyProvider
import com.credstow.app.data.BackupCodec
import com.credstow.app.data.KeyRepositoryImpl
import com.credstow.app.data.local.KeyDao
import com.credstow.app.data.local.KeyDatabase
import com.credstow.app.data.local.DatabaseMigrations
import com.credstow.app.domain.KeyRepository
import com.credstow.app.domain.ProState
import com.credstow.app.domain.usecase.AddKeyUseCase
import com.credstow.app.domain.usecase.CanAddKeyUseCase
import com.credstow.app.domain.usecase.DeleteKeyUseCase
import com.credstow.app.domain.usecase.ExportBackupUseCase
import com.credstow.app.domain.usecase.ImportBackupUseCase
import com.credstow.app.domain.usecase.ObserveKeysUseCase
import com.credstow.app.domain.usecase.SearchAndFilterUseCase
import com.credstow.app.domain.usecase.TestKeyUseCase
import com.credstow.app.domain.usecase.UpdateKeyUseCase
import com.credstow.app.network.KeyTester
import com.credstow.app.network.realKeyTester
import com.credstow.app.prefs.SettingsRepository
import com.credstow.app.billing.SettingsProState
import com.credstow.app.util.ClipboardHelper
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
        Room.databaseBuilder(appContext, KeyDatabase::class.java, "CredStow.db")
            .addMigrations(*DatabaseMigrations.ALL.toTypedArray())
            // Only wipe+recreate on a downgrade (older APK over newer), never on
            // an unmatched upgrade — explicit migrations above handle all known
            // forward paths. This preserves user keys across version bumps.
            .fallbackToDestructiveMigrationOnDowngrade()
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
    val proState: ProState by lazy { SettingsProState(settingsRepository, appContext) }
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


