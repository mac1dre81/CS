package com.credstow.app.ui.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Haptic feedback helpers for tactile responses across the app.
 *
 * Uses the platform [Vibrator] API for consistent cross-version behaviour
 * (API 21+). All calls are best-effort: missing hardware or permission issues
 * are silently swallowed so haptics can never crash the UI.
 */

/**
 * Resolves the platform [Vibrator] in a version-safe way.
 *
 * On Android 12 (API 31) and above, the recommended entry point is
 * [VibratorManager]; on older versions we fall back to the legacy
 * [Context.VIBRATOR_SERVICE] constant.
 *
 * Returns `null` if no vibrator is available or the service lookup fails.
 */
private fun Context.resolveVibrator(): Vibrator? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (_: Throwable) {
        null
    }
}

/**
 * Medium impact — used for key copy (clipboard), biometric success.
 * ~20ms pulse provides a subtle but perceptible tap.
 */
fun Context.vibrateMedium() {
    val v = resolveVibrator() ?: return
    if (!v.hasVibrator()) return

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            v.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(20)
        }
    } catch (_: Throwable) {
        // Permission or hardware issue — ignore.
    }
}

/**
 * Success tick — used for biometric auth success, key save confirmation.
 * Shorter ~10ms pulse distinguishes a success from a copy.
 */
fun Context.vibrateTick() {
    val v = resolveVibrator() ?: return
    if (!v.hasVibrator()) return

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            v.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(10)
        }
    } catch (_: Throwable) {
        // Permission or hardware issue — ignore.
    }
}

/**
 * Error buzz — used for failed key test, copy errors, swipe-to-delete.
 * A short double-pulse pattern signals "error" distinctively.
 */
fun Context.vibrateError() {
    val v = resolveVibrator() ?: return
    if (!v.hasVibrator()) return

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val pattern = longArrayOf(0, 30, 20, 30)
            v.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(60)
        }
    } catch (_: Throwable) {
        // Permission or hardware issue — ignore.
    }
}