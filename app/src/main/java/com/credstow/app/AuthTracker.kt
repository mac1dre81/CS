package com.credstow.app

import android.content.Context
import android.content.SharedPreferences
import com.credstow.app.prefs.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * Process-level + persistent tracker of the last successful biometric authentication.
 *
 * Used by two features:
 *  1. **Auto-lock timer** — MainActivity checks whether the elapsed time since
 *     the last auth exceeds the user-configured threshold; if so, it shows the
 *     LockScreen biometric prompt before revealing key content.
 *  2. **Quick Settings tile** — TileCopyActivity checks whether re-auth is needed
 *     before allowing key copy from the tile.
 *
 * The timestamp is persisted in plain SharedPreferences (not EncryptedSharedPreferences)
 * because it is not secret — it only records "when did the user last prove they
 * were present." On a fresh install or after clearing app data it defaults to 0,
 * which forces an initial auth via SplashScreen (the intended cold-start behaviour).
 */
object AuthTracker {

    private const val PREFS_NAME = "credstow_auth"
    private const val KEY_LAST_AUTH_AT = "last_auth_at"

    @Volatile
    private var prefs: SharedPreferences? = null
    @Volatile
    private var cachedLastAuthAt: Long = 0L

    /** Epoch-ms of the last successful biometric unlock. 0 if never authenticated. */
    var lastAuthenticatedAt: Long
        get() = cachedLastAuthAt
        set(value) {
            cachedLastAuthAt = value
            prefs?.edit()?.putLong(KEY_LAST_AUTH_AT, value)?.apply()
        }

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p
        cachedLastAuthAt = p.getLong(KEY_LAST_AUTH_AT, 0L)
    }

    /** True if the auto-lock threshold has been exceeded since the last auth. */
    suspend fun requiresUnlock(settings: SettingsRepository): Boolean {
        val biometricEnabled = settings.biometricLockEnabled.first()
        val autoLockMinutes = settings.autoLockAfterMinutes.first()
        if (!biometricEnabled || autoLockMinutes <= 0) return false
        val elapsedMs = System.currentTimeMillis() - cachedLastAuthAt
        val thresholdMs = autoLockMinutes * 60_000L
        return cachedLastAuthAt == 0L || elapsedMs > thresholdMs
    }
}
