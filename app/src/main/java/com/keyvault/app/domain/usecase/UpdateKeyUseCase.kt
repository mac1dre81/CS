package com.keyvault.app.domain.usecase

import com.keyvault.app.core.Outcome
import com.keyvault.app.domain.KeyRepository
import com.keyvault.app.model.UpdateKey

class UpdateKeyUseCase(private val repository: KeyRepository) {
    suspend operator fun invoke(update: UpdateKey): Outcome<Unit> = repository.updateKey(update)
}
