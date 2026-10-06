package com.keyvault.app.crypto

import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Derives an AES-256 key from a user password + salt using PBKDF2-HMAC-SHA256.
 *
 * The iteration count is deliberately high to slow brute force of a weak backup
 * password; it is recorded in the envelope so we can raise it in future versions.
 */
class Pbkdf2KeyDeriver {

    fun generateSalt(): ByteArray = ByteArray(SALT_LEN).apply { SecureRandom().nextBytes(this) }

    fun derive(
        password: CharArray,
        salt: ByteArray,
        iterations: Int = DEFAULT_ITERS,
        bits: Int = KEY_BITS,
    ): SecretKey {
        require(salt.size == SALT_LEN) { "salt must be $SALT_LEN bytes" }
        val spec = PBEKeySpec(password, salt, iterations, bits)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val raw = factory.generateSecret(spec).encoded
        return SecretKeySpec(raw, "AES")
    }

    /** Standalone helper: derive + wrap in one call. */
    fun derive(password: String, salt: ByteArray, iterations: Int = DEFAULT_ITERS): SecretKey =
        derive(password.toCharArray(), salt, iterations)

    companion object {
        const val DEFAULT_ITERS = 200_000
        const val SALT_LEN = 16
        const val KEY_BITS = 256
    }
}

/** Convenience: generate a random AES-256 key (used by JVM test doubles). */
fun generateRandomAesKey(): SecretKey =
    KeyGenerator.getInstance("AES").apply { init(Pbkdf2KeyDeriver.KEY_BITS) }.generateKey()
