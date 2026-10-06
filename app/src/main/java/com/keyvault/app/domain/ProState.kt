package com.keyvault.app.domain

import kotlinx.coroutines.flow.Flow

/**
 * Read-only view of Pro (paid) entitlements.
 *
 * Production implementation reads purchased product IDs from the Play Billing
 * client + persisted preferences. A test fake returns a fixed value so the
 * free-tier/tier-upgrade rules can be exercised without Play services.
 */
interface ProState {
    val isPro: Flow<Boolean>
    suspend fun isProNow(): Boolean
}
