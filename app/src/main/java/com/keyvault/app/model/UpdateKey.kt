package com.keyvault.app.model

/**
 * Request to update an existing key.
 *
 * [apiKeyValue] is the raw secret when the user is replacing it, or null when
 * only metadata (label/notes/provider) is changing. When it is null the
 * existing encrypted blob is preserved unchanged.
 */
data class UpdateKey(
    val id: String,
    val provider: Provider,
    val label: String,
    val apiKeyValue: String?,
    val notes: String,
    val customProviderName: String = "",
)
