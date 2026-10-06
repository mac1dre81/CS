package com.keyvault.app.domain

import com.keyvault.app.core.ErrorKind
import com.keyvault.app.core.Outcome
import com.keyvault.app.core.VaultException
import com.keyvault.app.core.mapErrorKind

/** Runs [block] and converts success/failure into an [Outcome]. */
suspend fun <T> runOutcome(block: suspend () -> T): Outcome<T> =
    runCatching { block() }.fold(
        onSuccess = { Outcome.success(it) },
        onFailure = { e ->
            Outcome.error(e.message ?: e.toString(), mapErrorKind(e), e)
        }
    )
