package com.keyvault.app.crypto
import java.nio.charset.StandardCharsets

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AesGcmCryptoTest {

    private val crypto = AesGcmCrypto()
    private val key = generateRandomAesKey()

    @Test
    fun roundTripReturnsOriginalPlaintext() {
        val plain = "sk-very-secret-token-1234567890".toByteArray(StandardCharsets.UTF_8)
        val enc = crypto.encrypt(plain, key)
        assertThat(crypto.decrypt(enc, key)).isEqualTo(plain)
    }

    @Test
    fun decryptWithWrongKeyFails() {
        val enc = crypto.encrypt("secret".toByteArray(StandardCharsets.UTF_8), key)
        val wrongKey = generateRandomAesKey()
        assertThat(crypto.decrypt(enc, wrongKey)).isNull()
    }

    @Test
    fun tamperedCiphertextFailsAuthentication() {
        val enc = crypto.encrypt("secret".toByteArray(StandardCharsets.UTF_8), key)
        val tampered = enc.copy(cipherText = enc.cipherText.reversed())
        assertThat(crypto.decrypt(tampered, key)).isNull()
    }

    @Test
    fun tamperedNonceFailsAuthentication() {
        val enc = crypto.encrypt("secret".toByteArray(StandardCharsets.UTF_8), key)
        val tampered = enc.copy(nonce = enc.nonce.reversed())
        assertThat(crypto.decrypt(tampered, key)).isNull()
    }

    @Test(expected = IllegalArgumentException::class)
    fun refusesEmptyPlaintext() {
        crypto.encrypt(ByteArray(0), key)
    }
}
