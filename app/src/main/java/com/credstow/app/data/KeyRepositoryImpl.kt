package com.credstow.app.data

import com.credstow.app.core.ErrorKind
import com.credstow.app.core.Outcome
import com.credstow.app.core.runOutcome
import com.credstow.app.core.VaultException
import com.credstow.app.crypto.AesGcmCrypto
import com.credstow.app.crypto.CipherResult
import com.credstow.app.crypto.SecretKeyProvider
import com.credstow.app.domain.KeyRepository
import com.credstow.app.model.BackupEntry
import com.credstow.app.model.KeyStatus
import com.credstow.app.model.NewKey
import com.credstow.app.model.Provider
import com.credstow.app.model.UpdateKey
import com.credstow.app.model.VaultKey
import com.credstow.app.util.maskKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.credstow.app.data.local.KeyDao
import com.credstow.app.data.local.KeyEntity
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Production [KeyRepository]: stores encrypted blobs in Room, decrypts only on
 * explicit demand (copy / test / export). The DEK is provisioned by
 * [SecretKeyProvider] (Keystore-backed in production).
 */
class KeyRepositoryImpl(
    private val dao: KeyDao,
    private val secretKeyProvider: SecretKeyProvider,
    private val crypto: AesGcmCrypto,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : KeyRepository {

    override fun observeKeys(): Flow<List<VaultKey>> =
        dao.observeAll()
            .map { list -> list.map(KeyEntity::toDomain) }
            .flowOn(ioDispatcher)

    override suspend fun getKeys(): List<VaultKey> = withContext(ioDispatcher) {
        dao.getAll().map(KeyEntity::toDomain)
    }

    override suspend fun getById(id: String): VaultKey? = withContext(ioDispatcher) {
        dao.getById(id)?.toDomain()
    }

    override suspend fun count(): Int = withContext(ioDispatcher) { dao.count() }

    override suspend fun addKey(key: NewKey): Outcome<String> = withContext(ioDispatcher) {
        wrap({
            validate(key.provider, key.label, key.apiKeyValue)
            val dek = secretKeyProvider.getOrCreateKey()
            val cipher = crypto.encrypt(key.apiKeyValue.trim().toByteArray(StandardCharsets.UTF_8), dek)
            val now = System.currentTimeMillis()
            val entity = KeyEntity(
                id = UUID.randomUUID().toString(),
                providerId = key.customProviderName.ifBlank { key.provider.id },
                label = key.label.trim(),
                notes = key.notes.trim(),
                maskPreview = maskKey(key.apiKeyValue),
                nonce = cipher.nonce,
                cipherText = cipher.cipherText,
                keyVersion = KEY_VERSION,
                status = KeyStatus.UNTESTED.name,
                createdAt = now,
                updatedAt = now,
                expiryDate = key.expiryDate,
                lastUsedAt = null,
            )
            dao.insert(entity)
            entity.id
        })
    }

    override suspend fun updateKey(update: UpdateKey): Outcome<Unit> = withContext(ioDispatcher) {
        wrap({
            val existing = dao.getById(update.id)
                ?: throw VaultException.InvalidInputException("Key not found")
            val cipher = if (update.apiKeyValue != null) {
                validate(update.provider, update.label, update.apiKeyValue)
                val dek = secretKeyProvider.getOrCreateKey()
                crypto.encrypt(update.apiKeyValue.trim().toByteArray(StandardCharsets.UTF_8), dek)
            } else {
                CipherResult(existing.nonce, existing.cipherText)
            }
            val mask = update.apiKeyValue?.let { maskKey(it) } ?: existing.maskPreview
            val entity = existing.copy(
                providerId = update.customProviderName.ifBlank { update.provider.id },
                label = update.label.trim(),
                notes = update.notes.trim(),
                nonce = cipher.nonce,
                cipherText = cipher.cipherText,
                maskPreview = mask,
                updatedAt = System.currentTimeMillis(),
                expiryDate = update.expiryDate ?: existing.expiryDate,
            )
            dao.update(entity)
            Unit
        })
    }

    override suspend fun deleteKey(id: String): Outcome<Unit> = withContext(ioDispatcher) {
        wrap { dao.deleteById(id); Unit }
    }

    override suspend fun updateStatus(id: String, status: KeyStatus): Outcome<Unit> = withContext(ioDispatcher) {
        wrap {
            val entity = dao.getById(id) ?: error("Key not found")
            dao.update(entity.copy(status = status.name, updatedAt = System.currentTimeMillis()))
            Unit
        }
    }

    override suspend fun setExpiryDate(id: String, expiryDate: Long?): Outcome<Unit> = withContext(ioDispatcher) {
        wrap {
            dao.updateExpiryDate(id, expiryDate, System.currentTimeMillis())
            Unit
        }
    }

    override suspend fun updateLastUsed(id: String): Outcome<Unit> = withContext(ioDispatcher) {
        wrap {
            dao.updateLastUsed(id, System.currentTimeMillis())
            Unit
        }
    }

    override suspend fun getPlainKey(id: String): String? = withContext(ioDispatcher) {
        val entity = dao.getById(id) ?: return@withContext null
        val dek = secretKeyProvider.getOrCreateKey()
        val bytes = crypto.decrypt(CipherResult(entity.nonce, entity.cipherText), dek) ?: return@withContext null
        // Track last-used timestamp (fire-and-forget; ignore failures).
        try { dao.updateLastUsed(id, System.currentTimeMillis()) } catch (_: Throwable) {}
        String(bytes, StandardCharsets.UTF_8)
    }

    override suspend fun allPlainForExport(): List<BackupEntry> = withContext(ioDispatcher) {
        val dek = secretKeyProvider.getOrCreateKey()
        dao.getAll().mapNotNull { entity ->
            val bytes = crypto.decrypt(CipherResult(entity.nonce, entity.cipherText), dek) ?: return@mapNotNull null
            BackupEntry(
                providerId = entity.providerId,
                label = entity.label,
                notes = entity.notes,
                apiKey = String(bytes, StandardCharsets.UTF_8),
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                status = entity.status,
                expiryDate = entity.expiryDate,
                lastUsedAt = entity.lastUsedAt,
            )
        }
    }

    override suspend fun importEntries(entries: List<BackupEntry>): Outcome<Int> = withContext(ioDispatcher) {
        wrap {
            val dek = secretKeyProvider.getOrCreateKey()
            var count = 0
            for (entry in entries) {
                val plain = entry.apiKey
                if (plain.isBlank()) continue
                val cipher = crypto.encrypt(plain.trim().toByteArray(StandardCharsets.UTF_8), dek)
                val now = System.currentTimeMillis()
                dao.insert(
                    KeyEntity(
                        id = UUID.randomUUID().toString(),
                        providerId = entry.providerId,
                        label = entry.label.trim(),
                        notes = entry.notes.trim(),
                        maskPreview = maskKey(plain),
                        nonce = cipher.nonce,
                        cipherText = cipher.cipherText,
                        keyVersion = KEY_VERSION,
                        status = KeyStatus.from(entry.status).name,
                        createdAt = entry.createdAt,
                        updatedAt = now,
                        expiryDate = entry.expiryDate,
                        lastUsedAt = entry.lastUsedAt,
                    )
                )
                count++
            }
            count
        }
    }

    private suspend fun <T> wrap(block: suspend () -> T): Outcome<T> = runOutcome(block)

    private fun validate(provider: Provider, label: String, apiKey: String) {
        if (label.isBlank()) throw VaultException.InvalidInputException("Label is required")
        if (apiKey.isBlank()) throw VaultException.InvalidInputException("API key cannot be empty")
        if (apiKey.trim().length < 6) throw VaultException.InvalidInputException("API key is too short")
    }

    companion object {
        const val KEY_VERSION = 1
    }
}

