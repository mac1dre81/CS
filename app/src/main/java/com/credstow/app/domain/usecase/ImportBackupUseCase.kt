package com.credstow.app.domain.usecase

import com.credstow.app.core.Outcome
import com.credstow.app.data.BackupCodec
import com.credstow.app.domain.KeyRepository
import com.credstow.app.model.BackupEntry

/** Restores keys from a password-encrypted backup blob. */
class ImportBackupUseCase(
    private val repository: KeyRepository,
    private val backupCodec: BackupCodec,
) {
    suspend operator fun invoke(blob: String, password: String): Outcome<Int> {
        val entries = backupCodec.decode(blob, password)
        if (entries == null) {
            return Outcome.error("Invalid backup or wrong password", com.credstow.app.core.ErrorKind.Backup)
        }
        if (entries.isEmpty()) {
            return Outcome.error("This backup contains no keys", com.credstow.app.core.ErrorKind.Backup)
        }
        return repository.importEntries(entries)
    }
}

