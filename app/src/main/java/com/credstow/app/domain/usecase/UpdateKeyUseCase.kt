package com.credstow.app.domain.usecase

import com.credstow.app.core.Outcome
import com.credstow.app.domain.KeyRepository
import com.credstow.app.model.UpdateKey

class UpdateKeyUseCase(private val repository: KeyRepository) {
    suspend operator fun invoke(update: UpdateKey): Outcome<Unit> = repository.updateKey(update)
}

