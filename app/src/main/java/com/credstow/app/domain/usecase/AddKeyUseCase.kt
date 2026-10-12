package com.credstow.app.domain.usecase

import com.credstow.app.core.AppConst
import com.credstow.app.core.ErrorKind
import com.credstow.app.core.Outcome
import com.credstow.app.core.VaultException
import com.credstow.app.domain.KeyRepository
import com.credstow.app.domain.ProState
import com.credstow.app.model.NewKey

/**
 * Adds a key, enforcing the free-tier limit.
 *
 * - Free users: hard stop at [AppConst.FREE_KEY_LIMIT] keys, returning
 *   [ErrorKind.MaxKeysExceeded] so the UI can prompt to upgrade.
 * - Pro users: unlimited.
 */
class AddKeyUseCase(
    private val repository: KeyRepository,
    private val proState: ProState,
) {
    suspend operator fun invoke(key: NewKey): Outcome<String> {
        val isPro = proState.isProNow()
        val count = repository.count()
        if (!isPro && count >= AppConst.FREE_KEY_LIMIT) {
            return Outcome.error(
                "Free tier allows up to ${AppConst.FREE_KEY_LIMIT} keys. Upgrade to Pro for unlimited.",
                ErrorKind.MaxKeysExceeded,
                VaultException.MaxKeysExceededException(),
            )
        }
        return repository.addKey(key)
    }
}

