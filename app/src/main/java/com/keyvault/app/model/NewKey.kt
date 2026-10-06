package com.keyvault.app.model

/** Request to create a new vault key. [apiKeyValue] is the raw secret. */
data class NewKey(
    val provider: Provider,
    val label: String,
    val apiKeyValue: String,
    val notes: String,
    val customProviderName: String = "",
)
