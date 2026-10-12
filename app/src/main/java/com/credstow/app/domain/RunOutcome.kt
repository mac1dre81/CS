package com.credstow.app.domain

import com.credstow.app.core.ErrorKind
import com.credstow.app.core.Outcome
import com.credstow.app.core.VaultException
import com.credstow.app.core.mapErrorKind
import kotlinx.coroutines.CancellationException

/** Runs [block] and converts success/failure into an [Outcome]. */
suspend fun <T> runOutcome(block: suspend () -> T): Outcome<T> =
    runCatching { block() }.fold(
        onSuccess = { Outcome.success(it) },
        onFailure = { e ->
            if (e is CancellationException) throw e
            Outcome.error(e.message ?: e.toString(), mapErrorKind(e), e)
        }
    )

