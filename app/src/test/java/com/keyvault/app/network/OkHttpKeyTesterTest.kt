package com.keyvault.app.network

import com.google.common.truth.Truth.assertThat
import com.keyvault.app.model.KeyStatus
import com.keyvault.app.model.Provider
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test

class OkHttpKeyTesterTest {

    @Test
    fun interpretStatusCodes() {
        assertThat(OkHttpKeyTester.interpret(200)).isEqualTo(KeyStatus.VALID)
        assertThat(OkHttpKeyTester.interpret(204)).isEqualTo(KeyStatus.VALID)
        assertThat(OkHttpKeyTester.interpret(299)).isEqualTo(KeyStatus.VALID)
        assertThat(OkHttpKeyTester.interpret(401)).isEqualTo(KeyStatus.INVALID)
        assertThat(OkHttpKeyTester.interpret(403)).isEqualTo(KeyStatus.INVALID)
        assertThat(OkHttpKeyTester.interpret(404)).isEqualTo(KeyStatus.ERROR)
        assertThat(OkHttpKeyTester.interpret(500)).isEqualTo(KeyStatus.ERROR)
        assertThat(OkHttpKeyTester.interpret(0)).isEqualTo(KeyStatus.ERROR)
    }

    private fun endpoint(server: MockWebServer): ApiRequest = ApiRequest { _ ->
        Request.Builder()
            .url(server.url("/v1/models"))
            .get()
            .build()
    }

    @Test
    fun server200ReturnsValid() = runTest {
        val server = MockWebServer()
        server.start()
        val tester = OkHttpKeyTester(
            OkHttpClient.Builder().build(),
            endpoints = mapOf(Provider.OPENAI to endpoint(server)),
        )
        server.enqueue(MockResponse().setResponseCode(200))
        assertThat(tester.test(Provider.OPENAI, "sk-test")).isEqualTo(KeyStatus.VALID)
        server.shutdown()
    }

    @Test
    fun server401ReturnsInvalid() = runTest {
        val server = MockWebServer()
        server.start()
        val tester = OkHttpKeyTester(
            OkHttpClient.Builder().build(),
            endpoints = mapOf(Provider.OPENAI to endpoint(server)),
        )
        server.enqueue(MockResponse().setResponseCode(401))
        assertThat(tester.test(Provider.OPENAI, "sk-bad")).isEqualTo(KeyStatus.INVALID)
        server.shutdown()
    }

    @Test
    fun server500ReturnsError() = runTest {
        val server = MockWebServer()
        server.start()
        val tester = OkHttpKeyTester(
            OkHttpClient.Builder().build(),
            endpoints = mapOf(Provider.OPENAI to endpoint(server)),
        )
        server.enqueue(MockResponse().setResponseCode(500))
        assertThat(tester.test(Provider.OPENAI, "sk-oops")).isEqualTo(KeyStatus.ERROR)
        server.shutdown()
    }
}
