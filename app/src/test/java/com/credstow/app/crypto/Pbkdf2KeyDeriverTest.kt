package com.credstow.app.crypto

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class Pbkdf2KeyDeriverTest {

    private val deriver = Pbkdf2KeyDeriver()

    @Test
    fun saltIs16Bytes() {
        assertThat(deriver.generateSalt().size).isEqualTo(Pbkdf2KeyDeriver.SALT_LEN)
    }

    @Test
    fun sameSaltAndPasswordIsDeterministic() {
        val salt = deriver.generateSalt()
        val a = deriver.derive("pw".toCharArray(), salt).encoded
        val b = deriver.derive("pw".toCharArray(), salt).encoded
        assertThat(a).isEqualTo(b)
    }

    @Test
    fun differentSaltYieldsDifferentKey() {
        val a = deriver.derive("pw".toCharArray(), deriver.generateSalt()).encoded
        val b = deriver.derive("pw".toCharArray(), deriver.generateSalt()).encoded
        assertThat(a).isNotEqualTo(b)
    }
}

