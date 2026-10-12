package com.credstow.app.billing

/** One-shot UI events emitted by [BillingManager]. */
sealed interface BillingEvent {
    data class Purchased(val productId: String) : BillingEvent
    data class Error(val message: String, val code: Int = -1) : BillingEvent
}

