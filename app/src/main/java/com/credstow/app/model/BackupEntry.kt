package com.credstow.app.model

import kotlinx.serialization.Serializable

/**
 * One decrypted key serialized inside an *encrypted* backup.
 *
 * SECURITY NOTE: An [apiKey] value exists in memory only while the backup is
 * being created/restored. It is never written to disk in plaintext - the whole
 * backup blob is encrypted with the user's password (AES-256-GCM via PBKDF2).
 */
@Serializable
data class BackupEntry(
    val providerId: String,
    val label: String,
    val notes: String,
    val apiKey: String,
    val createdAt: Long,
    val updatedAt: Long,
    val status: String,
    val expiryDate: Long? = null,
    val lastUsedAt: Long? = null,
)

