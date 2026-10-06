package com.keyvault.app.network

import com.keyvault.app.core.VaultException
import com.keyvault.app.model.KeyStatus
import com.keyvault.app.model.Provider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Real [KeyTester] backed by OkHttp. Sends a minimal, authenticated GET and
 * interprets the HTTP status:
 *   - 2xx      -> VALID
 *   - 401/403  -> INVALID / expired
 *   - anything else -> ERROR (treat as transient / unknown)
 *
 * The [endpoints] map is injectable so tests can redirect requests to a
 * MockWebServer. No logging interceptor is attached: provider auth headers are
 * the keys themselves.
 */
class OkHttpKeyTester(
    private val client: OkHttpClient,
    private val endpoints: Map<Provider, ApiRequest> = ApiRequest.realEndpoints(),
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : KeyTester {

    override suspend fun test(provider: Provider, apiKey: String): KeyStatus =
        withContext(io) {
            val request = endpoints[provider] ?: return@withContext KeyStatus.ERROR
            try {
                val response = client.newCall(request.build(apiKey)).execute()
                interpret(response.code)
            } catch (e: Exception) {
                KeyStatus.ERROR
            }
        }

    companion object {
        fun interpret(code: Int): KeyStatus = when {
            code in 200..299 -> KeyStatus.VALID
            code == 401 || code == 403 -> KeyStatus.INVALID
            else -> KeyStatus.ERROR
        }
    }
}

/** Builds an OkHttp [Request] for a provider given its API key. */
fun interface ApiRequest {
    fun build(apiKey: String): Request

    companion object {
        fun realEndpoints(): Map<Provider, ApiRequest> = mapOf(
            Provider.OPENAI to ApiRequest { k ->
                Request.Builder()
                    .url("https://api.openai.com/v1/models")
                    .get()
                    .header("Authorization", "Bearer $k")
                    .build()
            },
            Provider.ANTHROPIC to ApiRequest { k ->
                Request.Builder()
                    .url("https://api.anthropic.com/v1/models")
                    .get()
                    .header("x-api-key", k)
                    .header("anthropic-version", "2023-06-01")
                    .build()
            },
            Provider.GOOGLE to ApiRequest { k ->
                Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models?key=$k")
                    .get()
                    .build()
            },
            Provider.OPENROUTER to ApiRequest { k ->
                Request.Builder()
                    .url("https://openrouter.ai/api/v1/models")
                    .get()
                    .header("Authorization", "Bearer $k")
                    .build()
            },
            Provider.HUGGING_FACE to ApiRequest { k ->
                Request.Builder()
                    .url("https://api-inference.huggingface.co/models")
                    .get()
                    .header("Authorization", "Bearer $k")
                    .build()
            },
            Provider.COHERE to ApiRequest { k ->
                Request.Builder()
                    .url("https://api.cohere.ai/v1/models")
                    .get()
                    .header("Authorization", "Bearer $k")
                    .header("Cohere-Version", "2")
                    .build()
            },
            Provider.MISTRAL to ApiRequest { k ->
                Request.Builder()
                    .url("https://api.mistral.ai/v1/models")
                    .get()
                    .header("Authorization", "Bearer $k")
                    .build()
            },
            Provider.TOGETHER to ApiRequest { k ->
                Request.Builder()
                    .url("https://api.together.xyz/v1/models")
                    .get()
                    .header("Authorization", "Bearer $k")
                    .build()
            },
            Provider.XAI_GROK to ApiRequest { k ->
                Request.Builder()
                    .url("https://api.x.ai/v1/models")
                    .get()
                    .header("Authorization", "Bearer $k")
                    .build()
            },
            Provider.REPLICATE to ApiRequest { k ->
                Request.Builder()
                    .url("https://api.replicate.com/v1/account")
                    .get()
                    .header("Authorization", "Bearer $k")
                    .build()
            },
        )
    }
}
