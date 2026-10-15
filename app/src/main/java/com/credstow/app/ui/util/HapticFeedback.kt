package com.credstow.app.ui.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * Haptic feedback helpers for tactile responses across the app.
 *
 * Uses the platform [Vibrator] API for consistent cross-version behaviour
 * (API 21+). All calls are best-effort: missing hardware or permission issues
 * are silently swallowed so haptics can never crash the UI.
 */

/**
 * Medium impact — used for key copy (clipboard), biometric success.
 * ~20ms pulse provides a subtle but perceptible tap.
 */
fun Context.vibrateMedium() {
    val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    if (v?.hasVibrator() == true) {
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
}

/**
 * Success tick — used for biometric auth success, key save confirmation.
 * Shorter ~10ms pulse distinguishes a success from a copy.
 */
fun Context.vibrateTick() {
    val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    if (v?.hasVibrator() == true) {
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
}

/**
 * Error buzz — used for failed key test, copy errors, swipe-to-delete.
 * A short double-pulse pattern signals "error" distinctively.
 */
fun Context.vibrateError() {
    val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    if (v?.hasVibrator() == true) {
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
}
