package com.keyvault.app.ui.util

import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val TAG = "KeyVault"

/**
 * Best-effort biometric unlock helper.
 *
 * API verified against androidx.biometric 1.2.0-alpha04 (the version this
 * project pins via `libs.biometric`):
 *  - [BiometricManager.Authenticators] exposes `BIOMETRIC_STRONG`,
 *    `BIOMETRIC_WEAK` and `DEVICE_CREDENTIAL` (there is no bare `BIOMETRIC`).
 *  - the success callback receives a [BiometricPrompt.AuthenticationResult]
 *    (there is no `AuthResult`).
 *  - [BiometricPrompt.authenticate] takes a single [BiometricPrompt.PromptInfo];
 *    cancellation is via [BiometricPrompt.cancelAuthentication] (there is no
 *    `authenticate(PromptInfo, CancellationSignal)` overload).
 *
 * Crash safety: every public entry is wrapped so that if the framework throws
 * (no hardware, no enrolled credential, OEM block, etc.) we log the exact
 * reason with `Log.d(TAG, ...)` and resume `false` — the caller always proceeds
 * instead of trapping the user on the splash screen. `DEVICE_CREDENTIAL` is in
 * the allowed set, so a PIN/pattern/password is always available as a fallback.
 */
class BiometricAuth(private val activity: FragmentActivity) {

    private val authenticators: Int =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL

    /**
     * Whether the system reports a usable authenticator. Read once and logged so
     * `adb logcat -s KeyVault` reveals the raw canAuthenticate() return code
     * and makes OEM quirks (e.g. an Oppo that never reports SUCCESS) visible.
     */
    val isAvailable: Boolean = runCatching {
        BiometricManager.from(activity).canAuthenticate(authenticators)
    }.getOrNull()
        ?.also { code ->
            Log.d(TAG, "BiometricAuth: canAuthenticate=$code (SUCCESS=${BiometricManager.BIOMETRIC_SUCCESS})")
        } == BiometricManager.BIOMETRIC_SUCCESS

    /**
     * Prompts the user. Returns true on success, false otherwise.
     *
     * This *always* attempts the prompt when there is a viable activity — the
     * [isAvailable] flag is only used for logging, not to short-circuit the call,
     * so a device that flips between "no strong biometric" and "device credential
     * present" still gets a fallback UI from the framework instead of being
     * silently skipped (which was the "prompt never appears on relaunch" bug).
     */
    suspend fun authenticate(title: CharSequence, subtitle: CharSequence): Boolean =
        suspendCancellableCoroutine { cont ->
        Log.d(TAG, "BiometricAuth.authenticate: start (isAvailable=$isAvailable)")
        try {
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                // setNegativeButtonText is intentionally OMITTED. The framework
                // throws IllegalArgumentException("Negative text must not be set
                // if device credential authentication is allowed") when a
                // negative button is combined with DEVICE_CREDENTIAL — and
                // DEVICE_CREDENTIAL is in our allowed set so a PIN/pattern/
                // password always remains available as a fallback. The system
                // prompt provides its own dismiss affordance (back / timeout).
                .setAllowedAuthenticators(authenticators)
                .build()
            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        Log.d(TAG, "BiometricAuth: onAuthenticationSucceeded")
                        cont.resume(true)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        Log.d(TAG, "BiometricAuth: onAuthenticationError code=$errorCode msg=$errString")
                        cont.resume(false)
                    }

                    override fun onAuthenticationFailed() {
                        Log.d(TAG, "BiometricAuth: onAuthenticationFailed")
                    }
                },
            )
            cont.invokeOnCancellation {
                Log.d(TAG, "BiometricAuth: cancelling prompt")
                prompt.cancelAuthentication()
            }
            Log.d(TAG, "BiometricAuth: calling prompt.authenticate()")
            prompt.authenticate(promptInfo)
        } catch (e: Throwable) {
            // Catches PromptInfo.build() / BiometricPrompt construction /
            // authenticate() errors here (logged under KeyVault) instead of
            // letting them bubble up as "SplashGate: threw ...".
            Log.d(TAG, "BiometricAuth.authenticate threw: ${e.javaClass.simpleName}: ${e.message}")
            cont.resume(false)
        }
    }
}
