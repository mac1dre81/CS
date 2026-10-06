package com.keyvault.app.domain.usecase

import com.keyvault.app.core.Outcome
import com.keyvault.app.domain.KeyRepository
import com.keyvault.app.model.VaultKey

class DeleteKeyUseCase(private val repository: KeyRepository) {
    suspend operator fun invoke(id: String): Outcome<Unit> = repository.deleteKey(id)
}

/** Whether the user can add another key given the free/pro limits. */
class CanAddKeyUseCase {
    operator fun invoke(isPro: Boolean, currentCount: Int): Boolean =
        isPro || currentCount < com.keyvault.app.core.AppConst.FREE_KEY_LIMIT
}
