@file:OptIn(ExperimentalMaterial3Api::class)

package com.credstow.app.ui.screens

import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.credstow.app.R
import com.credstow.app.core.AppConst
import com.credstow.app.AuthTracker
import com.credstow.app.ui.components.AppKeyIcon
import com.credstow.app.ui.navigation.Routes
import com.credstow.app.ui.util.BiometricAuth
import com.credstow.app.ui.viewmodel.SplashViewModel
import com.credstow.app.util.findActivity
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

private const val TAG = "CredStow"

@Composable
fun SplashScreen(
    vm: SplashViewModel,
    onNavigate: (String) -> Unit,
) {
    val destination by vm.destination.collectAsState()
    val biometricEnabled by vm.biometricLockEnabled.collectAsState()
    val autoLockMinutes by vm.autoLockAfterMinutes.collectAsState(AppConst.AUTO_LOCK_DEFAULT_MINUTES)
    val context = LocalContext.current
    val splashShownAt = remember { SystemClock.elapsedRealtime() }

    LaunchedEffect(destination) {
        destination?.let { d ->
            Log.d(TAG, "SplashGate: dest=$d biometricEnabled=$biometricEnabled")
            val remainingMs = (AppConst.SPLASH_MIN_DURATION_MS -
                (SystemClock.elapsedRealtime() - splashShownAt)).coerceAtLeast(0L)
            delay(remainingMs)
            var authResult = false
            // Only prompt if biometric lock is on AND either this is the very first
            // cold start (lastAuthenticatedAt == 0) or the auto-lock timer has expired
            // since the last successful auth. If the LockScreen (in MainActivity)
            // already handled the auth, AuthTracker.lastAuthenticatedAt will be recent
            // and we skip the SplashScreen prompt — no double-auth.
            val needsAuth = biometricEnabled && (
                AuthTracker.lastAuthenticatedAt == 0L ||
                System.currentTimeMillis() - AuthTracker.lastAuthenticatedAt > autoLockMinutes * 60_000L
            )
            if (d == Routes.LIST && needsAuth) {
                // Best-effort biometric unlock on launch. This is deliberately
                // non-blocking: if no hardware is enrolled, the framework call
                // errors, or any OEM quirk blocks the prompt, we log the reason
                // and simply proceed to the key list. DEVICE_CREDENTIAL is in the
                // allowed authenticators, so the system can always fall back to a
                // PIN/pattern/password rather than leaving the user stuck.
                val activity = context.findActivity() as? FragmentActivity
                val auth = activity?.let { BiometricAuth(it) }
                Log.d(TAG, "SplashGate: isAvailable=${auth?.isAvailable} lifecycle=${activity?.lifecycle?.currentState}")
                // The framework's BiometricPrompt (FragmentActivity variant in
                // androidx.biometric 1.2.0-alpha04) commits an internal
                // FragmentTransaction that is rejected — "Unable to start
                // authentication. Called after onSaveInstanceState." — when the
                // activity's fragment-manager state is saved (mid-recreation on
                // cold start, or right after onSaveInstanceState). Poll
                // (bounded) until the state is clear, then prompt. This mirrors
                // the framework's own guard and fires as soon as onStart() clears
                // the saved state (not later, when onResume finally arrives).
                if (activity != null) {
                    var waited = 0
                    while (activity.supportFragmentManager.isStateSaved && waited < 2000) {
                        Log.d(TAG, "SplashGate: fm.isStateSaved=true, waiting (${waited}ms)")
                        delay(50); waited += 50
                    }
                    Log.d(TAG, "SplashGate: fm.isStateSaved=${activity.supportFragmentManager.isStateSaved} after ${waited}ms lifecycle=${activity.lifecycle.currentState}")
                }
                val ready = activity?.supportFragmentManager?.isStateSaved == false
                if (ready) {
                    // Fragment state is clear (activity is started/resumed) -> the
                    // framework will display the dialog and invoke onAuthenticationSucceeded /
                    // onAuthenticationError, so the suspend always resumes. A 12s timeout
                    // guards against any OEM silent-drop so the launch can never hang.
                    try {
                        withTimeout(12_000) {
                            auth?.authenticate(
                                title = context.getString(R.string.biometric_prompt_title),
                                subtitle = context.getString(R.string.biometric_prompt_subtitle),
                            )?.let { authResult = it }
                        }
                    } catch (te: TimeoutCancellationException) {
                        Log.d(TAG, "SplashGate: authenticate timed out after 12s -> skip")
                        authResult = false
                    } catch (e: Throwable) {
                        Log.d(TAG, "SplashGate: threw ${e.javaClass.simpleName}: ${e.message}")
                        authResult = false
                    }
                } else {
                    // Activity not fully started yet (the slow-OEM cold-start path / a
                    // stuck notification panel here keeps it pinned in CREATED). Calling
                    // prompt.authenticate() now makes the framework log "Unable to start
                    // authentication. Called after onSaveInstanceState." and SILENTLY drops
                    // the callback -> the splash suspend hangs forever. Skip the lock
                    // gracefully instead so the keys stay reachable.
                    Log.d(TAG, "SplashGate: isStateSaved still true -> SKIP biometric (no hang)")
                }
            }
            Log.d(TAG, "SplashGate: authResult=$authResult -> navigate($d)")
            if (d == Routes.LIST && authResult) {
                AuthTracker.lastAuthenticatedAt = System.currentTimeMillis()
                Log.d(TAG, "SplashGate: recorded AuthTracker.lastAuthenticatedAt")
            }
            onNavigate(d)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppKeyIcon(Modifier.size(96.dp), tint = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.height(24.dp))
            val loadingDesc = stringResource(R.string.splash_loading)
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.semantics {
                    contentDescription = loadingDesc
                },
            )
        }
    }
}

