package com.credstow.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.credstow.app.prefs.SettingsRepository
import kotlinx.coroutines.launch

class OnboardingViewModel(private val settings: SettingsRepository) : ViewModel() {

    fun completeOnboarding() {
        viewModelScope.launch {
            settings.setOnboardingCompleted(true)
        }
    }
}

