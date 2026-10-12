package com.credstow.app.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdRulesTest {

    // --- milestone rule ---
    @Test
    fun milestone_proUser_isNeverDue() {
        assertFalse(AdRules.milestoneDue(keyCount = 3, lastShownMilestone = 0, isPro = true))
    }

    @Test
    fun milestone_fires_at3_6_9_oncePerMilestone() {
        assertTrue(AdRules.milestoneDue(3, 0, false))
        assertFalse(AdRules.milestoneDue(3, 3, false)) // already shown for 3
        assertTrue(AdRules.milestoneDue(6, 3, false))
        assertFalse(AdRules.milestoneDue(9, 6, true))  // pro -> blocked
    }

    @Test
    fun milestone_doesNotFireBefore3_orAtZero_orBetweenMilestones() {
        assertFalse(AdRules.milestoneDue(0, 0, false))
        assertFalse(AdRules.milestoneDue(1, 0, false))
        assertFalse(AdRules.milestoneDue(2, 0, false))
        assertFalse(AdRules.milestoneDue(4, 3, false))
        assertFalse(AdRules.milestoneDue(5, 3, false))
    }

    // --- session rule ---
    @Test
    fun session_proUser_isNeverDue() {
        assertFalse(
            AdRules.sessionDue(
                now = 1_000L + AdRules.SESSION_MIN_MS + 1,
                sessionStart = 1_000L,
                lastShownAt = 0L,
                isPro = true,
            )
        )
    }

    @Test
    fun session_firesOnceMinTimeAndCooldownElapsed() {
        val sessionStart = 1_000L
        // Both the SESSION_MIN_MS floor and the AD_COOLDOWN_MS gap must be met.
        val now = sessionStart + AdRules.SESSION_MIN_MS + AdRules.AD_COOLDOWN_MS + 1
        // last shown at the epoch -> cooldown elapsed -> fires
        assertTrue(AdRules.sessionDue(now, sessionStart, 0L, false))
    }

    @Test
    fun session_blockedUntilCooldown_passes() {
        val sessionStart = 1_000L
        val now = sessionStart + AdRules.SESSION_MIN_MS + 1
        val justAfterAd = now - 1 // cooldown (180s) not yet elapsed
        assertFalse(AdRules.sessionDue(now, sessionStart, justAfterAd, false))
    }

    @Test
    fun session_blockedUntilSessionMin_passes() {
        val sessionStart = 1_000L
        val tooEarly = sessionStart + 1L // session threshold (120s) not yet elapsed
        assertFalse(AdRules.sessionDue(tooEarly, sessionStart, 0L, false))
    }

    // --- resetMilestone ---
    @Test
    fun resetMilestone_clearsWhenCountDropsBelowLastShown() {
        assertEquals(0, AdRules.resetMilestone(keyCount = 1, lastShownMilestone = 3))
        assertEquals(0, AdRules.resetMilestone(keyCount = 0, lastShownMilestone = 3))
    }

    @Test
    fun resetMilestone_keepsWhenCountAtOrAboveLastShown() {
        assertEquals(3, AdRules.resetMilestone(keyCount = 5, lastShownMilestone = 3))
        assertEquals(3, AdRules.resetMilestone(keyCount = 3, lastShownMilestone = 3))
    }
}

