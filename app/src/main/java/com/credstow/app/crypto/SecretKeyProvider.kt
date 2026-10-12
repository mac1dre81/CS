package com.credstow.app.crypto

import javax.crypto.SecretKey

/**
 * Provides the long-lived AES-256 data-encryption key (DEK).
 *
 * Production implementation wraps the DEK with the Android Keystore (hardware
 * backed when available) so the DEK never leaves the device in plaintext and
 * cannot be extracted even from a rooted filesystem. A JVM test implementation
 * backs this with an in-memory [javax.crypto.KeyGenerator] so the crypto logic
 * can be exercised without Android.
 */
interface SecretKeyProvider {
    fun getOrCreateKey(): SecretKey
}

