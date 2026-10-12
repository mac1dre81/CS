package com.credstow.app.ads

/**
 * Pure, side-effect-free ad-scheduling rules. Deliberately has no Android
 * dependencies so the entire decision matrix is unit-tested on the JVM
 * (see AdRulesTest).
 *
 * Two independent triggers, BOTH gated by `isPro` — Pro users must never see
 * an interstitial:
 *  - milestone: a full-page ad every [KEYS_PER_AD] keys present in the vault
 *    (so "every 3 API keys saved" → an ad fires once the vault holds 3, 6, 9…
 *    keys, shown once per milestone).
 *  - session:   an ad after [SESSION_MIN_MS] of cumulative foreground time in
 *    the app, with [AD_COOLDOWN_MS] enforced between any two interstitials
 *    (this is the "ads should play after some time in the app even if no keys
 *    are being saved" requirement).
 */
object AdRules {
    const val KEYS_PER_AD = 3
    const val SESSION_MIN_MS = 120_000L // ~2 min in-app before a time-based ad fires
    const val AD_COOLDOWN_MS = 180_000L // minimum gap between any two interstitials
    const val TICK_MS = 30_000L // how often the in-app scheduler re-checks the session rule

    /**
     * Milestone ad is due when the key count is a positive multiple of
     * [KEYS_PER_AD] and we have not already shown one for this exact count.
     * If keys are deleted below the last shown milestone, call [resetMilestone]
     * first so the milestone can re-trigger when keys are re-added.
     */
    fun milestoneDue(keyCount: Int, lastShownMilestone: Int, isPro: Boolean): Boolean =
        !isPro && keyCount > 0 && keyCount % KEYS_PER_AD == 0 && keyCount != lastShownMilestone

    /** True once the session has lasted long enough AND the cooldown has elapsed. */
    fun sessionDue(
        now: Long,
        sessionStart: Long,
        lastShownAt: Long,
        isPro: Boolean,
    ): Boolean = !isPro &&
        (now - sessionStart) >= SESSION_MIN_MS &&
        (now - lastShownAt) >= AD_COOLDOWN_MS

    /**
     * If the current key count dropped below the last milestone we showed
     * (keys were deleted), reset the tracker to 0 so the next milestone can
     * fire again. Otherwise keep the last shown milestone.
     */
    fun resetMilestone(keyCount: Int, lastShownMilestone: Int): Int =
        if (keyCount < lastShownMilestone) 0 else lastShownMilestone
}

