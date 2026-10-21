package com.credstow.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.credstow.app.core.AppConst
import com.credstow.app.domain.ProState
import com.credstow.app.prefs.SettingsRepository
import com.credstow.app.ui.navigation.Routes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Decides the first destination based solely on onboarding state. */
class SplashViewModel(
    settings: SettingsRepository,
    private val proState: ProState,
    private val interstitialAdManager: com.credstow.app.ads.InterstitialAdManager,
) : ViewModel() {

    val destination: StateFlow<String?> = settings.onboardingCompleted
        .map { completed -> if (completed) Routes.LIST else Routes.ONBOARDING }
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    val biometricLockEnabled: StateFlow<Boolean> = settings.biometricLockEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = false)

    /** Auto-lock threshold (minutes). 0 means "never auto-lock". */
    val autoLockAfterMinutes: StateFlow<Int> = settings.autoLockAfterMinutes
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = AppConst.AUTO_LOCK_DEFAULT_MINUTES)

    init {
        // Pre-load the interstitial as early as possible — while the splash
        // screen is visible — so it's ready by the time a milestone/ session
        // trigger fires on the key list screen. Free users only.
        viewModelScope.launch {
            if (!proState.isPro.first()) {
                interstitialAdManager.load()
            }
        }
    }
}

