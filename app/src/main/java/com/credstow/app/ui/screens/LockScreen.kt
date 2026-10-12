package com.credstow.app.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.credstow.app.AuthTracker
import com.credstow.app.R
import com.credstow.app.ui.util.BiometricAuth
import com.credstow.app.util.findActivity
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

private const val TAG = "CredStow"

/**
 * Lightweight lock screen shown when the auto-lock timer expires.
 *
 * Mirrors SplashScreen's biometric flow but is triggered from MainActivity.onStart()
 * when the user returns to the app after the inactivity threshold. On successful
 * auth, records the timestamp in [AuthTracker] so subsequent foreground checks are green.
 *
 * Like SplashScreen, this is deliberately fail-open: if the prompt errors, is
 * unavailable, or times out, the user still gets access. The lock is a convenience,
 * not a prison.
 */
@Composable
fun LockScreen(
    onAuthenticated: () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() as? FragmentActivity }
    val auth = remember(activity) { activity?.let { BiometricAuth(it) } }
    var authAttempted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (activity == null || auth == null) {
            // No activity attached — fail open.
            Log.d(TAG, "LockScreen: no FragmentActivity, fail-open")
            authAttempted = true
            onAuthenticated()
            return@LaunchedEffect
        }
        // Mirror SplashScreen's polling — wait for the FragmentManager state to clear
        // after the Activity is started (prevents the "Called after onSaveInstanceState"
        // silent-drop bug on cold-recreate / OEM slow-start paths).
        var waited = 0
        while (activity.supportFragmentManager.isStateSaved && waited < 2000) {
            Log.d(TAG, "LockScreen: fm.isStateSaved=true, waiting (${waited}ms)")
            delay(50); waited += 50
        }
        val ready = activity.supportFragmentManager.isStateSaved == false
        if (ready) {
            try {
                withTimeout(12_000) {
                    val result = auth.authenticate(
                        title = context.getString(R.string.biometric_prompt_title),
                        subtitle = context.getString(R.string.biometric_prompt_subtitle),
                    )
                    Log.d(TAG, "LockScreen: auth result=$result")
                    authAttempted = true
                    if (result) {
                        AuthTracker.lastAuthenticatedAt = System.currentTimeMillis()
                    }
                    // Fail-open: proceed whether or not auth succeeded.
                    onAuthenticated()
                }
            } catch (te: TimeoutCancellationException) {
                Log.d(TAG, "LockScreen: authenticate timed out -> fail-open")
                authAttempted = true
                onAuthenticated()
            } catch (e: Throwable) {
                Log.d(TAG, "LockScreen: threw ${e.javaClass.simpleName} -> fail-open")
                authAttempted = true
                onAuthenticated()
            }
        } else {
            // Activity not fully started yet — fail open to avoid a hung lock.
            Log.d(TAG, "LockScreen: fm.isStateSaved still true -> fail-open")
            authAttempted = true
            onAuthenticated()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                text = stringResource(R.string.lock_screen_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            if (!authAttempted) {
                Spacer(Modifier.height(24.dp))
                val loadingDesc = stringResource(R.string.lock_screen_prompt)
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
}
