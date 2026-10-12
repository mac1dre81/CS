package com.credstow.app.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.first
import com.credstow.app.core.AppConst
import com.credstow.app.data.local.SortBy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Thin DataStore-backed settings store. Holds non-secret UX toggles plus the
 * set of purchased product IDs (the single source of truth for "is Pro").
 */
class SettingsRepository(private val store: DataStore<Preferences>) {

    val isPro: Flow<Boolean> = store.data
        .map { it[Keys.PurchasedProductIds]?.contains(AppConst.PRO_PRODUCT_ID) == true }

    val onboardingCompleted: Flow<Boolean> = store.data
        .map { it[Keys.OnboardingCompleted] ?: false }

    val clipboardAutoClearSeconds: Flow<Int> = store.data
        .map { it[Keys.ClipboardAutoClearSeconds] ?: AppConst.CLIPBOARD_AUTO_CLEAR_DEFAULT_SECONDS }

    val biometricLockEnabled: Flow<Boolean> = store.data
        .map { it[Keys.BiometricLockEnabled] ?: false }

    /** Minutes before the app auto-locks after biometric unlock. 0 = never auto-lock. */
    val autoLockAfterMinutes: Flow<Int> = store.data
        .map { it[Keys.AutoLockAfterMinutes] ?: AppConst.AUTO_LOCK_DEFAULT_MINUTES }

    /** In-app text-size multiplier (accessibility). 1.0f == system default. */
    val fontScale: Flow<Float> = store.data
        .map { it[Keys.FontScale] ?: AppConst.FONT_SCALE_DEFAULT }

    /** Last key-count milestone a free interstitial was shown for (0 = none yet). */
    val lastInterstitialMilestone: Flow<Int> = store.data
        .map { it[Keys.LastInterstitialMilestone] ?: 0 }

    /** Epoch-ms of the last shown interstitial (for the session/cooldown rule). */
    val lastInterstitialShownAt: Flow<Long> = store.data
        .map { it[Keys.LastInterstitialShownAt] ?: 0L }

    /** Pro feature: user's preferred key list sort order. */
    val keySortBy: Flow<SortBy> = store.data
        .map { fromSortBy(it[Keys.KeySortBy]) }

    /** Free 7-day Pro trial end timestamp (epoch-ms). 0 = no trial active. */
    val proTrialEndsAt: Flow<Long> = store.data
        .map { it[Keys.ProTrialEndsAt] ?: 0L }

    suspend fun isProNow(): Boolean = isPro.first()

    /** Returns true if a free 7-day Pro trial is currently active. */
    suspend fun isProTrialActive(): Boolean =
        proTrialEndsAt.first() > System.currentTimeMillis()

    suspend fun setOnboardingCompleted(value: Boolean) {
        store.edit { it[Keys.OnboardingCompleted] = value }
    }

    suspend fun setClipboardAutoClearSeconds(seconds: Int) {
        store.edit { it[Keys.ClipboardAutoClearSeconds] = seconds }
    }

    suspend fun setBiometricLockEnabled(value: Boolean) {
        store.edit { it[Keys.BiometricLockEnabled] = value }
    }

    suspend fun setAutoLockAfterMinutes(minutes: Int) {
        store.edit { it[Keys.AutoLockAfterMinutes] = minutes }
    }

    suspend fun setFontScale(scale: Float) {
        store.edit { it[Keys.FontScale] = scale }
    }

    suspend fun setLastInterstitialMilestone(value: Int) {
        store.edit { it[Keys.LastInterstitialMilestone] = value }
    }

    suspend fun setLastInterstitialShownAt(value: Long) {
        store.edit { it[Keys.LastInterstitialShownAt] = value }
    }

    suspend fun setPurchasedProductIds(ids: Set<String>) {
        store.edit { it[Keys.PurchasedProductIds] = ids }
    }

    suspend fun getPurchasedProductIds(): Set<String> =
        store.data.first()[Keys.PurchasedProductIds] ?: emptySet()

    suspend fun setKeySortBy(sortBy: SortBy) {
        store.edit { it[Keys.KeySortBy] = sortBy.column }
    }

    /** Start or clear the free Pro trial. Pass 0 to clear. */
    suspend fun setProTrialEndsAt(timestamp: Long) {
        store.edit { it[Keys.ProTrialEndsAt] = timestamp }
    }

    private fun fromSortBy(value: String?): SortBy =
        SortBy.entries.firstOrNull { it.column == value } ?: SortBy.CreatedAt

    object Keys {
        val OnboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val ClipboardAutoClearSeconds = intPreferencesKey("clipboard_auto_clear_seconds")
        val BiometricLockEnabled = booleanPreferencesKey("biometric_lock_enabled")
        val AutoLockAfterMinutes = intPreferencesKey("auto_lock_after_minutes")
        val LastInterstitialMilestone = intPreferencesKey("last_interstitial_milestone")
        val LastInterstitialShownAt = longPreferencesKey("last_interstitial_shown_at")
        val FontScale = floatPreferencesKey("font_scale")
        val PurchasedProductIds = stringSetPreferencesKey("purchased_product_ids")
        val KeySortBy = stringPreferencesKey("key_sort_by")
        val ProTrialEndsAt = longPreferencesKey("pro_trial_ends_at")
    }
}

