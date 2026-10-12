package com.credstow.app.data

import com.google.common.truth.Truth.assertThat
import com.credstow.app.core.Outcome
import com.credstow.app.crypto.AesGcmCrypto
import com.credstow.app.crypto.InMemorySecretKeyProvider
import com.credstow.app.crypto.PasswordCrypto
import com.credstow.app.data.local.SortBy
import com.credstow.app.domain.usecase.ExportBackupUseCase
import com.credstow.app.domain.usecase.ImportBackupUseCase
import com.credstow.app.model.NewKey
import com.credstow.app.model.Provider
import com.credstow.app.model.VaultKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Unit tests for the new Pro features:
 *  - Key expiration date (set / retrieve / export-import round-trip)
 *  - Last-used tracking (getPlainKey updates lastUsedAt)
 *  - Sort order logic (SortBy enum + sortKeys helper)
 */
class ProFeaturesTest {

    private fun repo() = KeyRepositoryImpl(
        dao = FakeKeyDao(),
        secretKeyProvider = InMemorySecretKeyProvider(),
        crypto = AesGcmCrypto(),
        ioDispatcher = Dispatchers.Unconfined,
    )

    private fun validKey(label: String, value: String) =
        NewKey(Provider.OPENAI, label, value, "")

    /** Extract the key id from an Outcome<String> returned by addKey. */
    private suspend fun addKey(r: KeyRepositoryImpl, label: String, value: String): String {
        val result = r.addKey(validKey(label, value))
        assertThat(result).isInstanceOf(Outcome.Success::class.java)
        return (result as Outcome.Success).value as String
    }

    // ---- Expiry date ----

    @Test
    fun setExpiryDatePersistsAndIsRetrievedViaObserveKeys() = runTest {
        val r = repo()
        val id = addKey(r, "Work", "sk-prod-1234567890abcdef")

        val expiry = System.currentTimeMillis() + 86_400_000L // 1 day from now
        val result = r.setExpiryDate(id, expiry)
        assertThat(result).isInstanceOf(Outcome.Success::class.java)

        val observed = r.observeKeys().first()
        assertThat(observed).hasSize(1)
        assertThat(observed.first().expiryDate).isEqualTo(expiry)
    }

    @Test
    fun clearExpiryDateSetsNull() = runTest {
        val r = repo()
        val id = addKey(r, "Work", "sk-prod-1234567890abcdef")

        val expiry = System.currentTimeMillis() + 86_400_000L
        r.setExpiryDate(id, expiry)

        r.setExpiryDate(id, null)
        val observed = r.observeKeys().first()
        assertThat(observed.first().expiryDate).isNull()
    }

    @Test
    fun exportImportPreservesExpiryDate() = runTest {
        val expiry = System.currentTimeMillis() + 86_400_000L
        val r = repo()
        r.addKey(
            NewKey(
                provider = Provider.OPENAI,
                label = "Work",
                apiKeyValue = "sk-prod-1234567890abcdef",
                notes = "",
                expiryDate = expiry,
            )
        )

        val codec = BackupCodec(PasswordCrypto())
        val blob = (ExportBackupUseCase(r, codec)("safe-password-123") as Outcome.Success).value as String

        val r2 = repo()
        val imported = ImportBackupUseCase(r2, codec)(blob, "safe-password-123")
        assertThat(imported).isInstanceOf(Outcome.Success::class.java)
        assertThat((imported as Outcome.Success).value).isEqualTo(1)

        val restored = r2.observeKeys().first()
        assertThat(restored).hasSize(1)
        assertThat(restored.first().expiryDate).isEqualTo(expiry)
    }

    // ---- Last-used tracking ----

    @Test
    fun getPlainKeyUpdatesLastUsedAt() = runTest {
        val r = repo()
        val id = addKey(r, "Work", "sk-prod-1234567890abcdef")

        val key = r.getById(id)
        assertThat(key?.lastUsedAt).isNull()

        val before = System.currentTimeMillis()
        val plain = r.getPlainKey(id)
        val after = System.currentTimeMillis()

        assertThat(plain).isEqualTo("sk-prod-1234567890abcdef")

        val updated = r.getById(id)
        assertThat(updated?.lastUsedAt).isNotNull()
        assertThat(updated!!.lastUsedAt!!).isIn(before..after)
    }

    // ---- Sort order ----

    @Test
    fun sortByCreatedAtOrdersNewestFirst() = runTest {
        val r = repo()
        addKey(r, "A", "sk-a1234567890abcdef00")
        Thread.sleep(5)
        addKey(r, "B", "sk-b1234567890abcdef00")
        Thread.sleep(5)
        addKey(r, "C", "sk-c1234567890abcdef00")

        val keys = r.observeKeys().first()
        val sorted = sortKeys(keys, SortBy.CreatedAt)
        assertThat(sorted.map { it.label }).containsExactly("C", "B", "A").inOrder()
    }

    @Test
    fun sortByLabelOrdersAlphabetically() = runTest {
        val r = repo()
        addKey(r, "Zebra", "sk-z1234567890abcdef00")
        addKey(r, "Apple", "sk-a1234567890abcdef00")
        addKey(r, "Mango", "sk-m1234567890abcdef00")

        val keys = r.observeKeys().first()
        val sorted = sortKeys(keys, SortBy.Label)
        assertThat(sorted.map { it.label }).containsExactly("Apple", "Mango", "Zebra").inOrder()
    }

    @Test
    fun sortByLastUsedOrdersMostRecentFirst() = runTest {
        val r = repo()
        val aId = addKey(r, "A", "sk-a1234567890abcdef00")
        Thread.sleep(5)
        val bId = addKey(r, "B", "sk-b1234567890abcdef00")
        Thread.sleep(5)
        val cId = addKey(r, "C", "sk-c1234567890abcdef00")

        // Use them in a specific order: C, A, B
        r.getPlainKey(cId)
        Thread.sleep(5)
        r.getPlainKey(aId)
        Thread.sleep(5)
        r.getPlainKey(bId)

        val keys = r.observeKeys().first()
        val sorted = sortKeys(keys, SortBy.LastUsedAt)
        assertThat(sorted.map { it.label }).containsExactly("B", "A", "C").inOrder()
    }

    @Test
    fun sortByExpiryDateOrdersSoonestFirst() = runTest {
        val r = repo()
        val now = System.currentTimeMillis()
        val aId = addKey(r, "Late", "sk-l1234567890abcdef00")
        r.setExpiryDate(aId, now + 3_600_000L)  // 1h
        val bId = addKey(r, "Early", "sk-e1234567890abcdef00")
        r.setExpiryDate(bId, now + 1_800_000L)  // 30min
        val cId = addKey(r, "None", "sk-n1234567890abcdef00")
        r.setExpiryDate(cId, null)

        val keys = r.observeKeys().first()
        val sorted = sortKeys(keys, SortBy.ExpiryDate)
        // "None" (null expiry) should sink to the bottom
        assertThat(sorted.map { it.label }).containsExactly("Early", "Late", "None").inOrder()
    }

    @Test
    fun sortByCreatedAtIsDefaultWhenNoLastUsed() = runTest {
        val r = repo()
        addKey(r, "A", "sk-a1234567890abcdef00")
        Thread.sleep(5)
        addKey(r, "B", "sk-b1234567890abcdef00")

        val keys = r.observeKeys().first()
        val sorted = sortKeys(keys, SortBy.LastUsedAt)
        // Neither was used, so createdAt DESC is the tiebreaker
        assertThat(sorted.map { it.label }).containsExactly("B", "A").inOrder()
    }

    // ---- Sort helper (mirrors KeyListViewModel.sortKeys) ----

    private fun sortKeys(keys: List<VaultKey>, sort: SortBy): List<VaultKey> = when (sort) {
        SortBy.CreatedAt -> keys.sortedByDescending { it.createdAt }
        SortBy.Label -> keys.sortedBy { it.label }
        SortBy.LastUsedAt -> keys.sortedWith(
            compareByDescending<VaultKey> { it.lastUsedAt ?: 0L }
                .thenByDescending { it.createdAt }
        )
        SortBy.ExpiryDate -> keys.sortedWith(
            compareBy<VaultKey> { it.expiryDate ?: Long.MAX_VALUE }
                .thenByDescending { it.createdAt }
        )
    }
}
