package com.credstow.app.util
import java.nio.charset.StandardCharsets

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MaskKeyTest {

    @Test
    fun longKeyKeepsFirstAndLastFour() {
        val masked = maskKey("sk-prod-1234567890abcdef")
        assertThat(masked).startsWith("sk-p")
        assertThat(masked).endsWith("ef")
        // the secret middle is fully masked
        assertThat(masked).doesNotContain("1234567890abcd")
    }

    @Test
    fun shortKeyBecomesAllBullets() {
        assertThat(maskKey("abc")).isEqualTo("•".repeat(4))
        assertThat(maskKey("ab")).isEqualTo("•".repeat(4))
    }

    @Test
    fun emptyKeyStaysEmpty() {
        assertThat(maskKey("   ")).isEqualTo("")
    }

    @Test
    fun b64RoundTripsBytes() {
        val data = "some-bytes-12345".toByteArray(StandardCharsets.UTF_8)
        val encoded = B64.encode(data)
        assertThat(B64.decode(encoded)).isEqualTo(data)
    }
}

