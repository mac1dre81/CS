package com.credstow.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.credstow.app.model.KeyStatus
import com.credstow.app.model.Provider
import com.credstow.app.model.VaultKey

/**
 * DB row for one vault key.
 *
 * The plaintext API key is NEVER stored here. [cipherText]/[nonce] hold the
 * AES-256-GCM-encrypted secret (key derived from the Keystore-backed DEK).
 * [maskPreview] is a derived, non-secret preview (last 4 chars) so the list and
 * detail UIs can render without ever decrypting the real secret.
 */
@Entity(tableName = "vault_keys")
data class KeyEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "provider_id") val providerId: String,
    @ColumnInfo(name = "label") val label: String,
    @ColumnInfo(name = "notes") val notes: String,
    @ColumnInfo(name = "mask_preview") val maskPreview: String,
    @ColumnInfo(name = "nonce") val nonce: String,
    @ColumnInfo(name = "cipher_text") val cipherText: String,
    @ColumnInfo(name = "key_version") val keyVersion: Int,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "expiry_date") val expiryDate: Long? = null,
    @ColumnInfo(name = "last_used_at") val lastUsedAt: Long? = null,
) {
    fun toDomain(): VaultKey = VaultKey(
        id = id,
        provider = Provider.fromId(providerId),
        label = label,
        maskedKey = maskPreview,
        notes = notes,
        status = KeyStatus.from(status),
        createdAt = createdAt,
        updatedAt = updatedAt,
        providerId = providerId,
        expiryDate = expiryDate,
        lastUsedAt = lastUsedAt,
    )
}

