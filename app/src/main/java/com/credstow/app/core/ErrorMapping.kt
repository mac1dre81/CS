package com.credstow.app.core

import kotlinx.coroutines.CancellationException

/** Maps a thrown [Throwable] to the [ErrorKind] surfaced to the UI. */
internal fun mapErrorKind(e: Throwable): ErrorKind = when (e) {
    is VaultException.MaxKeysExceededException -> ErrorKind.MaxKeysExceeded
    is VaultException.InvalidInputException -> ErrorKind.InvalidInput
    is VaultException.CryptoException -> ErrorKind.Crypto
    is VaultException.BackupException -> ErrorKind.Backup
    is VaultException.NetworkException -> ErrorKind.Network
    else -> ErrorKind.Unknown
}

/**
 * Runs a use-case [block], converting thrown [VaultException]s into typed
 * [Outcome.Error]s. Used by use-cases that want exception-based control flow
 * while still returning the sealed [Outcome] envelope.
 *
 * [CancellationException] is re-thrown so that coroutine cancellation propagates
 * correctly — catching it would silently swallowed cancellation and prevent
 * the upstream job from being cancelled, leaving the caller in a suspended state.
 */
internal suspend fun <T> runOutcome(block: suspend () -> T): Outcome<T> {
    return try {
        Outcome.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Outcome.error(e.message ?: e.toString(), mapErrorKind(e), e)
    }
}


