package com.credstow.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.credstow.app.AuthTracker
import com.credstow.app.billing.BillingManager
import com.credstow.app.core.AppConst
import com.credstow.app.di.ServiceLocator
import com.credstow.app.ui.navigation.CredStowNavHost
import com.credstow.app.ui.screens.LockScreen
import com.credstow.app.ui.theme.CredStowTheme
import com.credstow.app.ui.viewmodel.SplashViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var billing: BillingManager
    private lateinit var sl: ServiceLocator

    /**
     * Whether the app content is currently "locked" (auto-lock timer expired
     * and biometric lock is enabled). Observed by Compose via collectAsState.
     *
     * On cold start this is `false` because SplashScreen handles the initial
     * biometric prompt. On return-from-background, onStart() checks the
     * auto-lock timer and may flip this to `true` to overlay the LockScreen.
     */
    private val isLocked = MutableStateFlow(false)

    /** True only for the very first onStart() — used to skip the auto-lock check on cold start. */
    private var isFirstOnStart = true

    override fun onCreate(savedInstanceState: Bundle?) {
        // Native splash screen (Android 12+ API, backported via androidx.core).
        // installSplashScreen() must be called before super.onCreate() and
        // returns a SplashScreen object that we can use to control visibility.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sl = ServiceLocator.getInstance(this)
        billing = sl.billingManager
        AuthTracker.init(this)

        // Early-create the SplashViewModel so we can observe destination
        // resolution and tell the native splash when it's safe to hide.
        val splashVm = SplashViewModel(
            sl.settingsRepository,
            sl.proState,
            sl.interstitialAdManager,
        )
        splashScreen.setKeepOnScreenCondition {
            splashVm.destination.value == null
        }

        // Prevent screenshots of key material — this is a security-first app.
        // FLAG_SECURE applies to the entire app, ensuring masks/keys are never
        // captured via screenshot, screen recording, or in the recents preview.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        setContent {
            val fontScale by sl.settingsRepository.fontScale
                .collectAsState(initial = AppConst.FONT_SCALE_DEFAULT)
            val locked by isLocked.collectAsState()

            CredStowTheme(fontScale = fontScale) {
                Box(Modifier.fillMaxSize()) {
                    CredStowNavHost(sl, modifier = Modifier.fillMaxSize())
                    if (locked) {
                        LockScreen(
                            onAuthenticated = {
                                AuthTracker.lastAuthenticatedAt = System.currentTimeMillis()
                                isLocked.value = false
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        billing.start()
        if (isFirstOnStart) {
            isFirstOnStart = false
            // Cold start: SplashScreen (the first route in NavHost) handles the
            // biometric prompt if needed. Don't overlay the LockScreen.
            isLocked.value = false
        } else {
            // Returning from background: check the auto-lock timer.
            lifecycleScope.launch {
                val needsUnlock = AuthTracker.requiresUnlock(sl.settingsRepository)
                isLocked.value = needsUnlock
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Security: instantly clear any key left on the clipboard when the app
        // moves to the background. The timer-based auto-clear still applies for
        // users who stay in the app, but this prevents secret leakage when they
        // switch to another app (e.g. to paste a key into a browser).
        ServiceLocator.getInstance(this).clipboardHelper.clearNow()
        billing.end()
    }
}

/** Convenience for tests / screenshots: render the nav host without Activity boilerplate. */
@Composable
fun PreviewNavHost(sl: ServiceLocator) {
    CredStowTheme {
        CredStowNavHost(sl)
    }
}
