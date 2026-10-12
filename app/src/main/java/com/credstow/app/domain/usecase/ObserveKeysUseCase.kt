package com.credstow.app.domain.usecase

import com.credstow.app.domain.KeyRepository
import com.credstow.app.model.VaultKey
import kotlinx.coroutines.flow.Flow

class ObserveKeysUseCase(private val repository: KeyRepository) {
    operator fun invoke(): Flow<List<VaultKey>> = repository.observeKeys()
}

