package com.credstow.app.crypto

/**
 * Output of an AES-256-GCM encryption: the IV (a.k.a. nonce) and the
 * ciphertext (which, for GCM, also carries the 128-bit authentication tag).
 *
 * Both fields are Base64 strings so they survive JSON/Room text columns and
 * are never mistaken for plaintext.
 */
data class CipherResult(
    val nonce: String,
    val cipherText: String,
)

