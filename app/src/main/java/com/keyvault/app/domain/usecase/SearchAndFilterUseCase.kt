package com.keyvault.app.domain.usecase

import com.keyvault.app.model.KeyStatus
import com.keyvault.app.model.Provider
import com.keyvault.app.model.VaultKey

/** One filter state for the key list. */
data class KeyFilter(
    val query: String = "",
    val provider: Provider? = null,
    val status: KeyStatus? = null,
)

/** Pure, allocation-light filter+search over an already-loaded key list. */
class SearchAndFilterUseCase {
    operator fun invoke(keys: List<VaultKey>, filter: KeyFilter): List<VaultKey> {
        var result = keys
        val q = filter.query.trim().lowercase()
        if (q.isNotEmpty()) {
            result = result.filter { k ->
                k.label.contains(q, ignoreCase = true) ||
                    k.provider.title.contains(q, ignoreCase = true) ||
                    k.providerId.contains(q, ignoreCase = true) ||
                    k.notes.contains(q, ignoreCase = true)
            }
        }
        if (filter.provider != null) {
            result = result.filter { it.provider == filter.provider }
        }
        if (filter.status != null) {
            result = result.filter { it.status == filter.status }
        }
        return result
    }
}
