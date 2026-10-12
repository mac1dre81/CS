package com.credstow.app.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.credstow.app.billing.BillingEvent
import com.credstow.app.billing.BillingManager
import com.credstow.app.core.AppConst
import com.credstow.app.data.local.SortBy
import com.credstow.app.core.Outcome
import com.credstow.app.domain.ProState
import com.credstow.app.domain.usecase.ExportBackupUseCase
import com.credstow.app.domain.usecase.ImportBackupUseCase
import com.credstow.app.prefs.SettingsRepository
import com.credstow.app.util.ClipboardHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val billingManager: BillingManager,
    private val settings: SettingsRepository,
    private val proState: ProState,
    private val exportBackup: ExportBackupUseCase,
    private val importBackup: ImportBackupUseCase,
    private val clipboardHelper: ClipboardHelper,
) : ViewModel() {

    val isPro: StateFlow<Boolean> = proState.isPro
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Free 7-day trial end timestamp (epoch-ms). 0 = no trial. */
    val proTrialEndsAt: StateFlow<Long> = settings.proTrialEndsAt
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val clipboardAutoClearSeconds: StateFlow<Int> = settings.clipboardAutoClearSeconds
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppConst.CLIPBOARD_AUTO_CLEAR_DEFAULT_SECONDS)

    val fontScale: StateFlow<Float> = settings.fontScale
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppConst.FONT_SCALE_DEFAULT)

    val keySortBy: StateFlow<SortBy> = settings.keySortBy
        .stateIn(viewModelScope, SharingStarted.Eagerly, SortBy.CreatedAt)

    val sortLabel: String
        get() = when (keySortBy.value) {
            SortBy.CreatedAt -> "Newest first"
            SortBy.Label -> "Name (A→Z)"
            SortBy.LastUsedAt -> "Last used"
            SortBy.ExpiryDate -> "Expires soon"
        }

    val biometricLockEnabled: StateFlow<Boolean> = settings.biometricLockEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val autoLockAfterMinutes: StateFlow<Int> = settings.autoLockAfterMinutes
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppConst.AUTO_LOCK_DEFAULT_MINUTES)

    val billingEvents: Flow<BillingEvent> = billingManager.events

    private val _exporting = MutableStateFlow(false)
    val exporting: StateFlow<Boolean> = _exporting
    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing

    val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)

    fun refreshPro() {
        viewModelScope.launch {
            billingManager.syncPurchases()
            val pro = isPro.value
            messages.emit(if (pro) "Pro is active" else "You're on the free plan (billing unavailable in dev build)")
        }
    }

    fun startProTrial() {
        viewModelScope.launch {
            // Don't start a trial if already Pro or already trialing.
            if (isPro.value || settings.isProTrialActive()) {
                messages.emit("You already have Pro access")
                return@launch
            }
            val end = System.currentTimeMillis() + AppConst.PRO_TRIAL_DURATION_MS
            settings.setProTrialEndsAt(end)
            messages.emit("Free Pro trial started! Enjoy all features for 5 days.")
        }
    }

    fun onToggleBiometric() {
        val next = !biometricLockEnabled.value
        viewModelScope.launch { settings.setBiometricLockEnabled(next) }
    }

    fun setAutoLockAfterMinutes(minutes: Int) {
        viewModelScope.launch { settings.setAutoLockAfterMinutes(minutes) }
    }

    fun setAutoClear(seconds: Int) {
        viewModelScope.launch { settings.setClipboardAutoClearSeconds(seconds) }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch { settings.setFontScale(scale) }
    }

    fun setKeySortBy(sortBy: SortBy) {
        viewModelScope.launch { settings.setKeySortBy(sortBy) }
    }

    fun unlockPro(activity: Activity) {
        billingManager.launchBillingFlow(activity)
    }

    fun redeemPromoCode(activity: Activity) {
        billingManager.redeemPromoCode(activity)
    }

    fun performExport(password: String) {
        viewModelScope.launch {
            _exporting.value = true
            when (val outcome = exportBackup(password)) {
                is Outcome.Success -> {
                    clipboardHelper.copy(
                        outcome.value,
                        AppConst.CLIPBOARD_AUTO_CLEAR_DEFAULT_SECONDS,
                        viewModelScope,
                    )
                    messages.emit(
                        "Encrypted backup copied to clipboard. Paste it into a secure file to keep it."
                    )
                }
                is Outcome.Error -> messages.emit(outcome.message)
            }
            _exporting.value = false
        }
    }

    fun performImport(blob: String, password: String) {
        viewModelScope.launch {
            _importing.value = true
            when (val outcome = importBackup(blob, password)) {
                is Outcome.Success -> messages.emit("Restored ${outcome.value} key(s) from backup.")
                is Outcome.Error -> messages.emit(outcome.message)
            }
            _importing.value = false
        }
    }
}

