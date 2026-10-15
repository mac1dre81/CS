package com.credstow.app.ads

/**
 * Pure, side-effect-free ad-scheduling rules.
 *
 * Interstitial ad policy (Play-compliant):
 *  - Fires only for free (non-Pro) users.
 *  - Requires >= 2 consecutive key saves since the last ad.
 *  - Requires >= 2 minutes of in-app session time.
 *  - Requires >= 3 minutes since the last interstitial (cooldown).
 *  - Requires the user to be at a "natural pause point" —
 *    e.g. returning to the list page, visiting Settings,
 *    or completing a task (key test / clipboard copy).
 *
 * This avoids showing interstitials mid-action (e.g. while the
 * user is editing a key or scrolling through the vault).
 */
object AdRules {
    const val CONSECUTIVE_SAVES_THRESHOLD = 2
    const val SESSION_MIN_MS = 120_000L    // ~2 min in-app before an ad can fire
    const val AD_COOLDOWN_MS = 180_000L    // minimum gap between interstitials (3 min)

    /**
     * Returns true when **all** conditions for showing an interstitial are met.
     *
     * @param consecutiveSaves   Number of key saves since the last ad was shown.
     * @param sessionMs           Milliseconds since the current session started.
     * @param timeSinceLastAd     Milliseconds since the last interstitial was shown. 0 = never.
     * @param isPro               True if the user has Pro (ads are always suppressed).
     * @param atPausePoint        True when the user is at a natural pause point in the UI.
     */
    fun shouldShowAd(
        consecutiveSaves: Int,
        sessionMs: Long,
        timeSinceLastAd: Long,
        isPro: Boolean,
        atPausePoint: Boolean,
    ): Boolean = !isPro && atPausePoint &&
        consecutiveSaves >= CONSECUTIVE_SAVES_THRESHOLD &&
        sessionMs >= SESSION_MIN_MS &&
        timeSinceLastAd >= AD_COOLDOWN_MS
}
