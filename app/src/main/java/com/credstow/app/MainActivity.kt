package com.credestow.app

import android.os.Bundle
import androidx.activity.compose.setContent
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
        super.onCreate(savedInstanceState)
        sl = ServiceLocator.getInstance(this)
        billing = sl.billingManager
        AuthTracker.init(this)

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
