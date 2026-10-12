package com.credstow.app.domain

import com.credstow.app.core.Outcome
import com.credstow.app.model.BackupEntry
import com.credstow.app.model.KeyStatus
import com.credstow.app.model.NewKey
import com.credstow.app.model.UpdateKey
import com.credstow.app.model.VaultKey
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract. Implementations live in the `data` package; use-cases
 * depend only on this abstraction so business rules are testable on the JVM.
 */
interface KeyRepository {

    fun observeKeys(): Flow<List<VaultKey>>

    suspend fun getKeys(): List<VaultKey>

    suspend fun getById(id: String): VaultKey?

    suspend fun count(): Int

    suspend fun addKey(key: NewKey): Outcome<String>

    suspend fun updateKey(update: UpdateKey): Outcome<Unit>

    suspend fun deleteKey(id: String): Outcome<Unit>

    suspend fun updateStatus(id: String, status: KeyStatus): Outcome<Unit>

    /** Update the key's optional expiration timestamp (Pro feature). */
    suspend fun setExpiryDate(id: String, expiryDate: Long?): Outcome<Unit>

    /** Record that this key was recently used (copy/test) — updates last_used_at. */
    suspend fun updateLastUsed(id: String): Outcome<Unit>

    /** Decrypt a single key for copy-to-clipboard / test ping. Never cached outside the caller. */
    suspend fun getPlainKey(id: String): String?

    /** Decrypt every key into plaintext entries (used only to build a backup). */
    suspend fun allPlainForExport(): List<BackupEntry>

    /** Re-encrypt plaintext entries and store them (used only to restore a backup). */
    suspend fun importEntries(entries: List<BackupEntry>): Outcome<Int>
}

