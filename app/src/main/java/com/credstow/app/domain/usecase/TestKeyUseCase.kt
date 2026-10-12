package com.credstow.app.domain.usecase

import com.credstow.app.core.Outcome
import com.credstow.app.core.VaultException
import com.credstow.app.core.runOutcome
import com.credstow.app.domain.KeyRepository
import com.credstow.app.model.KeyStatus
import com.credstow.app.network.KeyTester

/**
 * Runs a user-initiated validity ping for a key.
 *
 * Flow: mark TESTING -> decrypt -> ping provider -> persist result. The plaintext
 * key is passed straight to the tester and never stored/logged.
 */
class TestKeyUseCase(
    private val repository: KeyRepository,
    private val tester: KeyTester,
) {
    suspend operator fun invoke(id: String): Outcome<KeyStatus> = runOutcome {
        val vaultKey = repository.getById(id)
            ?: throw VaultException.InvalidInputException("Key not found")
        if (!vaultKey.provider.supportsTest) {
            throw VaultException.InvalidInputException(
                "Test ping is not available for ${vaultKey.provider.title}"
            )
        }
        repository.updateStatus(id, KeyStatus.TESTING)
        val plain = repository.getPlainKey(id)
            ?: throw VaultException.CryptoException("Unable to decrypt key")
        val result = tester.test(vaultKey.provider, plain)
        repository.updateStatus(id, result)
        result
    }
}

