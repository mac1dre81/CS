package com.keyvault.app.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keyvault.app.billing.BillingEvent
import com.keyvault.app.billing.BillingManager
import com.keyvault.app.core.AppConst
import com.keyvault.app.core.Outcome
import com.keyvault.app.domain.ProState
import com.keyvault.app.domain.usecase.ExportBackupUseCase
import com.keyvault.app.domain.usecase.ImportBackupUseCase
import com.keyvault.app.prefs.SettingsRepository
import com.keyvault.app.util.ClipboardHelper
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val clipboardAutoClearSeconds: StateFlow<Int> = settings.clipboardAutoClearSeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConst.CLIPBOARD_AUTO_CLEAR_DEFAULT_SECONDS)

    val fontScale: StateFlow<Float> = settings.fontScale
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConst.FONT_SCALE_DEFAULT)

    val biometricLockEnabled: StateFlow<Boolean> = settings.biometricLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

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

    fun onToggleBiometric() {
        val next = !biometricLockEnabled.value
        viewModelScope.launch { settings.setBiometricLockEnabled(next) }
    }

    fun setAutoClear(seconds: Int) {
        viewModelScope.launch { settings.setClipboardAutoClearSeconds(seconds) }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch { settings.setFontScale(scale) }
    }

    fun unlockPro(activity: Activity) {
        billingManager.launchBillingFlow(activity)
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
