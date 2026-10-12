package com.credstow.app.data

import com.google.common.truth.Truth.assertThat
import com.credstow.app.core.Outcome
import com.credstow.app.crypto.AesGcmCrypto
import com.credstow.app.crypto.InMemorySecretKeyProvider
import com.credstow.app.crypto.PasswordCrypto
import com.credstow.app.domain.usecase.ExportBackupUseCase
import com.credstow.app.domain.usecase.ImportBackupUseCase
import com.credstow.app.model.NewKey
import com.credstow.app.model.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Verifies that import results are visible through the observeKeys() Flow —
 * the exact path the KeyListScreen uses to render its list.
 */
class ImportFlowTest {

    private fun repo() = KeyRepositoryImpl(
        dao = FakeKeyDao(),
        secretKeyProvider = InMemorySecretKeyProvider(),
        crypto = AesGcmCrypto(),
        ioDispatcher = Dispatchers.Unconfined,
    )

    private fun validKey(label: String, value: String) =
        NewKey(Provider.OPENAI, label, value, "")

    @Test
    fun observeKeysEmitsImportedKeys() = runTest {
        // 1. Create a repo with two keys
        val r = repo()
        r.addKey(validKey("Work", "sk-prod-1234567890abcdef"))
        r.addKey(validKey("Personal", "sk-ant-1234567890abcdef"))

        // 2. Export the vault
        val codec = BackupCodec(PasswordCrypto())
        val exported = ExportBackupUseCase(r, codec)("safe-password-123")
        val blob = (exported as Outcome.Success).value as String

        // 3. Import into a FRESH repo (simulates restore-on-new-device)
        val r2 = repo()
        // 3a. Fresh repo must start with zero keys (BEFORE import)
        assertThat(r2.observeKeys().first()).isEmpty()

        val imported = ImportBackupUseCase(r2, codec)(blob, "safe-password-123")
        assertThat(imported).isInstanceOf(Outcome.Success::class.java)
        assertThat((imported as Outcome.Success).value).isEqualTo(2)

        // 4. CRITICAL: verify observeKeys() Flow sees the imported keys
        val observed = r2.observeKeys().first()
        assertThat(observed).hasSize(2)
        val labels = observed.map { it.label }
        assertThat(labels).containsExactly("Work", "Personal")

        // 5. Verify decryption works for both imported keys
        val workKey = observed.first { it.label == "Work" }
        assertThat(r2.getPlainKey(workKey.id)).isEqualTo("sk-prod-1234567890abcdef")
    }

    @Test
    fun observeKeysEmitsImportedKeysAfterDirectAdd() = runTest {
        // Verify that keys can be observed through the flow AFTER being
        // added via addKey AND after being imported via ImportBackupUseCase.
        val r = repo()
        val codec = BackupCodec(PasswordCrypto())

        // Add a key directly via the repository
        r.addKey(validKey("Direct", "sk-direct-1234567890"))
        assertThat(r.observeKeys().first()).hasSize(1)

        // Export and import into a DIFFERENT fresh repo
        val exported = ExportBackupUseCase(r, codec)("safe-password-123")
        val blob = (exported as Outcome.Success).value as String

        val r2 = repo()
        // The fresh repo should have NO keys before import
        assertThat(r2.observeKeys().first()).isEmpty()

        val imported = ImportBackupUseCase(r2, codec)(blob, "safe-password-123")
        assertThat(imported).isInstanceOf(Outcome.Success::class.java)

        // After import, observeKeys() should emit the imported keys
        val observed = r2.observeKeys().first()
        assertThat(observed).hasSize(1)
        assertThat(observed.first().label).isEqualTo("Direct")
        assertThat(observed.first().providerId).isEqualTo("openai")
    }

    @Test
    fun trimHandlesWhitespaceInBlob() = runTest {
        val r = repo()
        r.addKey(validKey("Trimmed", "sk-trim-1234567890"))

        val codec = BackupCodec(PasswordCrypto())
        val exported = ExportBackupUseCase(r, codec)("safe-password-123")
        val blob = (exported as Outcome.Success).value as String

        // Simulate clipboard adding leading/trailing whitespace
        val paddedBlob = "  $blob  "

        val r2 = repo()
        val imported = ImportBackupUseCase(r2, codec)(paddedBlob, "safe-password-123")
        assertThat(imported).isInstanceOf(Outcome.Success::class.java)
        assertThat((imported as Outcome.Success).value).isEqualTo(1)

        // The imported key must be visible through observeKeys()
        val observed = r2.observeKeys().first()
        assertThat(observed).hasSize(1)
        assertThat(observed.first().label).isEqualTo("Trimmed")
    }
}
