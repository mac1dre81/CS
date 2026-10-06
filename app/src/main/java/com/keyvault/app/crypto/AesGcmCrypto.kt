package com.keyvault.app.crypto

import com.keyvault.app.util.B64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Authenticated symmetric encryption for vault secrets (AES-256-GCM).
 *
 * Uses a plain JCE `SecretKey` (the DEK) so the same code runs on the JVM test
 * runner and on Android. Key provenance/ protection at rest is the job of
 * [SecretKeyProvider] (Keystore-backed in production); this class only
 * guarantees confidentiality + integrity of the bytes it is handed.
 *
 * Output of [encrypt] cannot be decrypted without both the same key and the
 * stored nonce; a tampered ciphertext/nonce fails authentication and [decrypt]
 * returns null instead of corrupted plaintext.
 */
class AesGcmCrypto {

    fun encrypt(plain: ByteArray, key: SecretKey): CipherResult {
        require(plain.isNotEmpty()) { "Refusing to encrypt empty plaintext" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val nonce = cipher.iv ?: error("Cipher produced no IV")
        val cipherText = cipher.doFinal(plain)
        return CipherResult(nonce = B64.encode(nonce), cipherText = B64.encode(cipherText))
    }

    fun decrypt(result: CipherResult, key: SecretKey): ByteArray? =
        runCatching {
            val nonce = B64.decode(result.nonce) ?: return null
            val cipherText = B64.decode(result.cipherText) ?: return null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
            cipher.doFinal(cipherText)
        }.getOrNull()

    companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val TAG_BITS = 128
    }
}
