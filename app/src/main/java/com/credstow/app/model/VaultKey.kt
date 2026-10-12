package com.credstow.app.model

/**
 * Display model for a stored key.
 *
 * Holds NO plaintext. [maskedKey] is a derived preview (last 4 chars visible)
 * computed once at storage time, so listing/detail never need to decrypt.
 * Plaintext is only ever produced transiently, inside the trust boundary, for
 * copy-to-clipboard or the user-initiated test ping.
 */
data class VaultKey(
    val id: String,
    val provider: Provider,
    val label: String,
    val maskedKey: String,
    val notes: String,
    val status: KeyStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val providerId: String = "",
)

