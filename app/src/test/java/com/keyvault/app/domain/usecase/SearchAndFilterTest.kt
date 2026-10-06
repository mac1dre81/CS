package com.keyvault.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.keyvault.app.crypto.InMemorySecretKeyProvider
import com.keyvault.app.crypto.AesGcmCrypto
import com.keyvault.app.data.FakeKeyDao
import com.keyvault.app.data.KeyRepositoryImpl
import com.keyvault.app.domain.usecase.KeyFilter
import com.keyvault.app.model.KeyStatus
import com.keyvault.app.model.NewKey
import com.keyvault.app.model.Provider
import com.keyvault.app.model.VaultKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SearchAndFilterTest {

    private val keys = listOf(
        VaultKey("1", Provider.OPENAI, "Work OpenAI", "sk-****1234", "prod notes", KeyStatus.VALID, 1, 1),
        VaultKey("2", Provider.ANTHROPIC, "Personal Anthropic", "sk-****5678", "dev", KeyStatus.INVALID, 2, 2),
        VaultKey("3", Provider.GOOGLE, "Gemini key", "AI****abcd", "", KeyStatus.UNTESTED, 3, 3),
        VaultKey("4", Provider.OPENROUTER, "Router key", "sk-****ef01", "prod notes", KeyStatus.VALID, 4, 4),
    )

    private val finder = SearchAndFilterUseCase()

    @Test
    fun emptyFilterKeepsAll() {
        assertThat(finder(keys, KeyFilter())).hasSize(4)
    }

    @Test
    fun filterByProvider() {
        val result = finder(keys, KeyFilter(provider = Provider.OPENAI))
        assertThat(result).hasSize(1)
        assertThat(result.first().label).isEqualTo("Work OpenAI")
    }

    @Test
    fun filterByStatusValid() {
        val result = finder(keys, KeyFilter(status = KeyStatus.VALID))
        assertThat(result.map { it.id }).containsExactly("1", "4")
    }

    @Test
    fun searchMatchesLabelAndNotes() {
        val result = finder(keys, KeyFilter(query = "prod"))
        assertThat(result.map { it.id }).containsExactly("1", "4")
    }

    @Test
    fun searchMatchesProviderName() {
        val result = finder(keys, KeyFilter(query = "google"))
        assertThat(result.map { it.id }).containsExactly("3")
    }

    // Also verifies the repository wires the filter through to observed data.
    @Test
    fun repoObservedKeysRoundTrip() = runTest {
        val r = KeyRepositoryImpl(
            dao = FakeKeyDao(),
            secretKeyProvider = InMemorySecretKeyProvider(),
            crypto = AesGcmCrypto(),
            ioDispatcher = Dispatchers.Unconfined,
        )
        r.addKey(NewKey(Provider.OPENAI, "Work OpenAI", "sk-abcdef1234567890", "prod"))
        val observed = r.observeKeys().first()
        assertThat(observed).hasSize(1)
        val filtered = finder(observed, KeyFilter(query = "work"))
        assertThat(filtered).hasSize(1)
    }
}
