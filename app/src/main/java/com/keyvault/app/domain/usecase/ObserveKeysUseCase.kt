package com.keyvault.app.domain.usecase

import com.keyvault.app.domain.KeyRepository
import com.keyvault.app.model.VaultKey
import kotlinx.coroutines.flow.Flow

class ObserveKeysUseCase(private val repository: KeyRepository) {
    operator fun invoke(): Flow<List<VaultKey>> = repository.observeKeys()
}
