package com.keyvault.app.crypto

import com.keyvault.app.core.AppConst
import com.keyvault.app.util.B64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Password-authenticated encryption of a backup blob.
 *
 * A fresh random salt is generated per backup; the key is derived with PBKDF2.
 * The same [AesGcmCrypto] primitive provides authenticated encryption, but the
 * key here comes from a password (not the Keystore DEK), so backups can be
 * restored on a different device once the user enters their password.
 */
class PasswordCrypto(private val deriver: Pbkdf2KeyDeriver = Pbkdf2KeyDeriver()) {

    fun encrypt(plain: ByteArray, password: String): PasswordCipherResult {
        require(password.isNotEmpty()) { "Backup password must not be empty" }
        val salt = deriver.generateSalt()
        val key = deriver.derive(password, salt, Pbkdf2KeyDeriver.DEFAULT_ITERS)
        return doEncrypt(plain, key, salt)
    }

    fun decrypt(envelope: PasswordCipherResult, password: String): ByteArray? = runCatching {
        val salt = B64.decode(envelope.salt) ?: return null
        val key = deriver.derive(password, salt, envelope.iterations)
        val nonce = B64.decode(envelope.nonce) ?: return null
        val cipherText = B64.decode(envelope.cipherText) ?: return null
        val cipher = Cipher.getInstance(AesGcmCrypto.TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(AppConst.GCM_TAG_SIZE_BITS, nonce))
        cipher.doFinal(cipherText)
    }.getOrNull()

    private fun doEncrypt(plain: ByteArray, key: SecretKey, salt: ByteArray): PasswordCipherResult {
        val cipher = Cipher.getInstance(AesGcmCrypto.TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val nonce = cipher.iv ?: error("Cipher produced no IV")
        val cipherText = cipher.doFinal(plain)
        return PasswordCipherResult(
            version = 1,
            kdf = "PBKDF2WithHmacSHA256",
            iterations = Pbkdf2KeyDeriver.DEFAULT_ITERS,
            salt = B64.encode(salt),
            nonce = B64.encode(nonce),
            cipherText = B64.encode(cipherText),
        )
    }
}
