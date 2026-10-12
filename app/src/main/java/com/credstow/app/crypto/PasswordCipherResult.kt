package com.credstow.app.crypto

import kotlinx.serialization.Serializable

/**
 * Envelope protecting a whole backup blob with a password-derived key.
 *
 * Stored alongside the (base64) nonce/ciphertext so a backup is self-describing
 * and forward-compatible: a future KDF simply bumps [version] and [iterations].
 */
@Serializable
data class PasswordCipherResult(
    val version: Int = 1,
    val kdf: String = "PBKDF2WithHmacSHA256",
    val iterations: Int = Pbkdf2KeyDeriver.DEFAULT_ITERS,
    val salt: String,
    val nonce: String,
    val cipherText: String,
)

