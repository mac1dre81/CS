package com.keyvault.app.crypto
import java.nio.charset.StandardCharsets

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PasswordCryptoTest {

    private val crypto = PasswordCrypto()

    @Test
    fun roundTripWithCorrectPassword() {
        val plain = "backup-payload-12345".toByteArray(StandardCharsets.UTF_8)
        val envelope = crypto.encrypt(plain, "correct horse battery")
        assertThat(crypto.decrypt(envelope, "correct horse battery")).isEqualTo(plain)
    }

    @Test
    fun wrongPasswordFails() {
        val envelope = crypto.encrypt("data".toByteArray(StandardCharsets.UTF_8), "correct horse battery")
        assertThat(crypto.decrypt(envelope, "wrong password")).isNull()
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyPasswordIsRejected() {
        crypto.encrypt("data".toByteArray(StandardCharsets.UTF_8), "")
    }

    @Test
    fun eachEncryptionUsesFreshSalt() {
        val a = crypto.encrypt("x".toByteArray(StandardCharsets.UTF_8), "pw")
        val b = crypto.encrypt("x".toByteArray(StandardCharsets.UTF_8), "pw")
        assertThat(a.salt).isNotEqualTo(b.salt)
    }
}
