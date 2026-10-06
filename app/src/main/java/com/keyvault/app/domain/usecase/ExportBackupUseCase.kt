package com.keyvault.app.domain.usecase

import com.keyvault.app.core.Outcome
import com.keyvault.app.core.runOutcome
import com.keyvault.app.data.BackupCodec
import com.keyvault.app.domain.KeyRepository
import com.keyvault.app.model.BackupEntry

/** Exports all keys to a single password-encrypted blob. */
class ExportBackupUseCase(
    private val repository: KeyRepository,
    private val backupCodec: BackupCodec,
) {
    suspend operator fun invoke(password: String): Outcome<String> = runOutcome {
        require(password.length >= MIN_BACKUP_PASSWORD) {
            "Password must be at least $MIN_BACKUP_PASSWORD characters"
        }
        val entries = repository.allPlainForExport()
        check(entries.isNotEmpty()) { "There are no keys to export" }
        backupCodec.encode(entries, password)
    }

    companion object {
        const val MIN_BACKUP_PASSWORD = 8
    }
}
