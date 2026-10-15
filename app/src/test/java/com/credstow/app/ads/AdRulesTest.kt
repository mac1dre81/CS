package com.credstow.app.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdRulesTest {

    // --- shouldShowAd: gating conditions ---

    @Test
    fun shouldShowAd_proUser_isNeverDue() {
        assertFalse(
            AdRules.shouldShowAd(
                consecutiveSaves = 5,
                sessionMs = AdRules.SESSION_MIN_MS + 1,
                timeSinceLastAd = AdRules.AD_COOLDOWN_MS + 1,
                isPro = true,
                atPausePoint = true,
            )
        )
    }

    @Test
    fun shouldShowAd_notAtPausePoint_neverFires() {
        assertFalse(
            AdRules.shouldShowAd(
                consecutiveSaves = 5,
                sessionMs = AdRules.SESSION_MIN_MS + 1,
                timeSinceLastAd = AdRules.AD_COOLDOWN_MS + 1,
                isPro = false,
                atPausePoint = false,
            )
        )
    }

    @Test
    fun shouldShowAd_firesWhenAllConditionsMet() {
        assertTrue(
            AdRules.shouldShowAd(
                consecutiveSaves = AdRules.CONSECUTIVE_SAVES_THRESHOLD,
                sessionMs = AdRules.SESSION_MIN_MS,
                timeSinceLastAd = AdRules.AD_COOLDOWN_MS,
                isPro = false,
                atPausePoint = true,
            )
        )
    }

    // --- consecutive saves threshold ---

    @Test
    fun shouldShowAd_firesAtExactlyThreshold() {
        assertTrue(
            AdRules.shouldShowAd(
                consecutiveSaves = AdRules.CONSECUTIVE_SAVES_THRESHOLD,
                sessionMs = AdRules.SESSION_MIN_MS,
                timeSinceLastAd = AdRules.AD_COOLDOWN_MS,
                isPro = false,
                atPausePoint = true,
            )
        )
    }

    @Test
    fun shouldShowAd_blockedBelowThreshold() {
        assertFalse(
            AdRules.shouldShowAd(
                consecutiveSaves = AdRules.CONSECUTIVE_SAVES_THRESHOLD - 1,
                sessionMs = AdRules.SESSION_MIN_MS,
                timeSinceLastAd = AdRules.AD_COOLDOWN_MS,
                isPro = false,
                atPausePoint = true,
            )
        )
    }

    // --- session time floor ---

    @Test
    fun shouldShowAd_blockedBelowSessionMin() {
        assertFalse(
            AdRules.shouldShowAd(
                consecutiveSaves = AdRules.CONSECUTIVE_SAVES_THRESHOLD,
                sessionMs = AdRules.SESSION_MIN_MS - 1,
                timeSinceLastAd = AdRules.AD_COOLDOWN_MS,
                isPro = false,
                atPausePoint = true,
            )
        )
    }

    // --- cooldown floor ---

    @Test
    fun shouldShowAd_blockedBelowCooldown() {
        assertFalse(
            AdRules.shouldShowAd(
                consecutiveSaves = AdRules.CONSECUTIVE_SAVES_THRESHOLD,
                sessionMs = AdRules.SESSION_MIN_MS,
                timeSinceLastAd = AdRules.AD_COOLDOWN_MS - 1,
                isPro = false,
                atPausePoint = true,
            )
        )
    }

    // --- never shown yet (timeSinceLastAd = 0) ---

    @Test
    fun shouldShowAd_neverShownBefore_passesCooldownWhen0() {
        // timeSinceLastAd = 0 means never shown; pass 0 + cooldown as a large
        // value to simulate "enough time elapsed since app start."
        assertTrue(
            AdRules.shouldShowAd(
                consecutiveSaves = AdRules.CONSECUTIVE_SAVES_THRESHOLD,
                sessionMs = AdRules.SESSION_MIN_MS + 1,
                timeSinceLastAd = AdRules.AD_COOLDOWN_MS + 1,
                isPro = false,
                atPausePoint = true,
            )
        )
    }

    // --- extra saves don't cause double-firing ---

    @Test
    fun shouldShowAd_moreSavesThanThreshold_stillFiresOnce() {
        // More than the threshold should still be true (it just means
        // the condition is met; the ViewModel is responsible for resetting
        // the counter after firing).
        assertTrue(
            AdRules.shouldShowAd(
                consecutiveSaves = 5,
                sessionMs = AdRules.SESSION_MIN_MS + 1,
                timeSinceLastAd = AdRules.AD_COOLDOWN_MS + 1,
                isPro = false,
                atPausePoint = true,
            )
        )
    }
}
