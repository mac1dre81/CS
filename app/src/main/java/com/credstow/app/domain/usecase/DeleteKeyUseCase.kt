package com.credstow.app.domain.usecase

import com.credstow.app.core.Outcome
import com.credstow.app.domain.KeyRepository
import com.credstow.app.model.VaultKey

class DeleteKeyUseCase(private val repository: KeyRepository) {
    suspend operator fun invoke(id: String): Outcome<Unit> = repository.deleteKey(id)
}

/** Whether the user can add another key given the free/pro limits. */
class CanAddKeyUseCase {
    operator fun invoke(isPro: Boolean, currentCount: Int): Boolean =
        isPro || currentCount < com.credstow.app.core.AppConst.FREE_KEY_LIMIT
}

