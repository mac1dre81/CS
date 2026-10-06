package com.keyvault.app.data

import com.google.common.truth.Truth.assertThat
import com.keyvault.app.core.Outcome
import com.keyvault.app.crypto.AesGcmCrypto
import com.keyvault.app.crypto.InMemorySecretKeyProvider
import com.keyvault.app.crypto.PasswordCrypto
import com.keyvault.app.domain.usecase.ExportBackupUseCase
import com.keyvault.app.domain.usecase.ImportBackupUseCase
import com.keyvault.app.model.NewKey
import com.keyvault.app.model.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ExportImportTest {

    private fun repo() = KeyRepositoryImpl(
        dao = FakeKeyDao(),
        secretKeyProvider = InMemorySecretKeyProvider(),
        crypto = AesGcmCrypto(),
        ioDispatcher = Dispatchers.Unconfined,
    )

    private fun validKey(label: String, value: String) =
        NewKey(Provider.OPENAI, label, value, "")

    @Test
    fun exportImportsEncryptedBlobAndRestoresKeys() = runTest {
        val r = repo()
        r.addKey(validKey("Work", "sk-prod-1234567890abcdef"))
        r.addKey(validKey("Personal", "sk-ant-1234567890abcdef"))

        val codec = BackupCodec(PasswordCrypto())
        val exported = ExportBackupUseCase(r, codec)("safe-password-123")
        assertThat(exported).isInstanceOf(Outcome.Success::class.java)
        val blob = (exported as Outcome.Success).value as String
        assertThat(blob).isNotEmpty()

        // The plaintext keys must never appear in the encrypted backup blob.
        assertThat(blob).doesNotContain("sk-prod-1234567890abcdef")
        assertThat(blob).doesNotContain("sk-ant-1234567890abcdef")

        // A fresh vault + the same password restores everything.
        val r2 = repo()
        val imported = ImportBackupUseCase(r2, codec)(blob, "safe-password-123")
        assertThat(imported).isInstanceOf(Outcome.Success::class.java)
        assertThat((imported as Outcome.Success).value).isEqualTo(2)
        assertThat(r2.count()).isEqualTo(2)

        // The restored plaintext matches what was imported.
        val restored = r2.getKeys().first { it.label == "Work" }
        assertThat(r2.getPlainKey(restored.id)).isEqualTo("sk-prod-1234567890abcdef")
    }

    @Test
    fun wrongPasswordFails() = runTest {
        val r = repo()
        r.addKey(validKey("Work", "sk-prod-1234567890abcdef"))
        val codec = BackupCodec(PasswordCrypto())
        val blob = (ExportBackupUseCase(r, codec)("safe-password-123") as Outcome.Success).value as String
        val result = ImportBackupUseCase(r, codec)(blob, "wrong-password")
        assertThat(result).isInstanceOf(Outcome.Error::class.java)
    }

    @Test
    fun shortBackupPasswordIsRejected() = runTest {
        val r = repo()
        r.addKey(validKey("Work", "sk-prod-1234567890abcdef"))
        val result = ExportBackupUseCase(r, BackupCodec(PasswordCrypto()))("short")
        assertThat(result).isInstanceOf(Outcome.Error::class.java)
    }
}
