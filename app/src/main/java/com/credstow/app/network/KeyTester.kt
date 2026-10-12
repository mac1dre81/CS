package com.credstow.app.network

import com.credstow.app.model.KeyStatus
import com.credstow.app.model.Provider

/**
 * Verifies a single API key against its provider's endpoint.
 *
 * This is intentionally an interface so the wiring (real OkHttp vs. test fake)
 * is decoupled from business logic. Implementations MUST be user-initiated only;
 * the app never pings keys automatically or in the background.
 */
interface KeyTester {
    /** Returns the validity status. Implementations must not leak the key in logs. */
    suspend fun test(provider: Provider, apiKey: String): KeyStatus
}

