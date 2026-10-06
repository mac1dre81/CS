package com.keyvault.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keyvault.app.core.Outcome
import com.keyvault.app.domain.KeyRepository
import com.keyvault.app.domain.ProState
import com.keyvault.app.domain.usecase.DeleteKeyUseCase
import com.keyvault.app.domain.usecase.TestKeyUseCase
import com.keyvault.app.model.KeyStatus
import com.keyvault.app.model.VaultKey
import com.keyvault.app.prefs.SettingsRepository
import com.keyvault.app.util.ClipboardHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KeyDetailViewModel(
    val keyId: String,
    private val repository: KeyRepository,
    private val deleteKey: DeleteKeyUseCase,
    private val testKey: TestKeyUseCase,
    private val proState: ProState,
    private val clipboardHelper: ClipboardHelper,
    private val settings: SettingsRepository,
) : ViewModel() {


    private val _key = MutableStateFlow<VaultKey?>(null)
    val key: StateFlow<VaultKey?> = _key.asStateFlow()

    val isPro: StateFlow<Boolean> = proState.isPro
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val autoClearSeconds: StateFlow<Int> = settings.clipboardAutoClearSeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 30)

    val canTest: StateFlow<Boolean> = combine(key, isPro) { k, pro ->
        pro && k?.provider?.supportsTest == true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    val message = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val deleted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _key.value = repository.getById(keyId)
        }
    }

    fun onCopy() {
        viewModelScope.launch {
            val plain = repository.getPlainKey(keyId)
            if (plain == null) {
                message.emit("Could not decrypt key")
                return@launch
            }
            clipboardHelper.copy(
                plain,
                autoClearSeconds.value,
                viewModelScope,
            )
            message.emit(
                "Key copied to clipboard. It will be cleared in ${autoClearSeconds.value}s."
            )
        }
    }

    fun onTest() {
        if (!isPro.value) {
            viewModelScope.launch { message.emit("Key test ping is a Pro feature") }
            return
        }
        viewModelScope.launch {
            _isTesting.value = true
            message.emit("Testing key…")
            val outcome = testKey(keyId)
            _isTesting.value = false
            refresh()
            when (outcome) {
                is Outcome.Success -> message.emit(statusText(outcome.value))
                is Outcome.Error -> message.emit(outcome.message)
            }
        }
    }

    fun onDelete() {
        viewModelScope.launch {
            deleteKey(keyId)
            deleted.emit(Unit)
        }
    }

    private fun statusText(status: KeyStatus): String = when (status) {
        KeyStatus.VALID -> "✅ Valid key"
        KeyStatus.INVALID -> "❌ Invalid or expired key"
        KeyStatus.ERROR -> "⚠️ Test inconclusive — check connectivity"
        KeyStatus.TESTING -> "Testing…"
        KeyStatus.UNTESTED -> "Not tested"
    }
}
