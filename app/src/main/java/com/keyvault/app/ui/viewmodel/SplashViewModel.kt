package com.keyvault.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keyvault.app.prefs.SettingsRepository
import com.keyvault.app.ui.navigation.Routes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Decides the first destination based solely on onboarding state. */
class SplashViewModel(settings: SettingsRepository) : ViewModel() {

    val destination: StateFlow<String?> = settings.onboardingCompleted
        .map { completed -> if (completed) Routes.LIST else Routes.ONBOARDING }
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    val biometricLockEnabled: StateFlow<Boolean> = settings.biometricLockEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = false)
}
