package com.credstow.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.credstow.app.crypto.SecretKeyProvider
import com.credstow.app.util.B64
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Production [SecretKeyProvider].
 *
 * Strategy: keep a randomly-generated 256-bit Data-Encryption Key (DEK) in
 * [EncryptedSharedPreferences] (which itself encrypts with a Keystore-backed
 * MasterKey). The DEK is a normal JCE AES key, so [AesGcmCrypto] never has to
 * know it came from the Keystore — the same code runs in unit tests with an
 * in-memory key.
 *
 * The DEK is generated once and cached for the process; it is never
 * persisted in plaintext, never logged, and never leaves the device.
 */
class AndroidSecretKeyProvider(context: Context) : SecretKeyProvider {

    private val appContext = context.applicationContext
    private val masterKey = MasterKey.Builder(appContext, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = lazy {
        EncryptedSharedPreferences.create(
            appContext,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    @Volatile
    private var cached: SecretKey? = null

    override fun getOrCreateKey(): SecretKey =
        cached ?: synchronized(this) {
            cached ?: loadOrCreate().also { cached = it }
        }

    private fun loadOrCreate(): SecretKey {
        val stored = prefs.value.getString(KEY_ALIAS, null)
        if (!stored.isNullOrBlank()) {
            val raw = B64.decode(stored)
            if (raw != null && raw.size >= 32) return SecretKeySpec(raw, "AES")
        }
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        prefs.value.edit()
            .putString(KEY_ALIAS, B64.encode(key.encoded))
            .apply()
        return key
    }

    companion object {
        private const val PREFS_NAME = "credstow_master_prefs"
        private const val KEY_ALIAS = "data_encryption_key"
    }
}

