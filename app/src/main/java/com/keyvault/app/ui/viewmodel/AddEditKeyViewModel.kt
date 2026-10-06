package com.keyvault.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keyvault.app.core.ErrorKind
import com.keyvault.app.core.Outcome
import com.keyvault.app.domain.KeyRepository
import com.keyvault.app.domain.usecase.AddKeyUseCase
import com.keyvault.app.domain.usecase.UpdateKeyUseCase
import com.keyvault.app.model.NewKey
import com.keyvault.app.model.Provider
import com.keyvault.app.model.UpdateKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddEditUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val provider: Provider = Provider.OPENAI,
    val label: String = "",
    val apiKey: String = "",
    val apiKeyVisible: Boolean = false,
    val notes: String = "",
    val customProviderName: String = "",
    val existingMaskedKey: String? = null,
    val replacingKey: Boolean = false,
    val saveError: String? = null,
)

class AddEditKeyViewModel(
    private val editKeyId: String?,
    private val repository: KeyRepository,
    private val addKey: AddKeyUseCase,
    private val updateKey: UpdateKeyUseCase,
) : ViewModel() {

    private val _ui = MutableStateFlow(AddEditUiState(isEdit = editKeyId != null))
    val ui: StateFlow<AddEditUiState> = _ui.asStateFlow()

    val saved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val requestUpgrade = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        if (editKeyId != null) load(editKeyId)
    }

    private fun load(id: String) {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, saveError = null) }
            val k = repository.getById(id)
            if (k == null) {
                _ui.update { it.copy(isLoading = false, saveError = "Key not found") }
            } else {
                _ui.update {
                    it.copy(
                        isLoading = false,
                        provider = k.provider,
                        label = k.label,
                        notes = k.notes,
                        existingMaskedKey = k.maskedKey,
                        customProviderName =
                            if (k.provider == Provider.OTHER && k.providerId.isNotBlank() && k.providerId != Provider.OTHER.id)
                                k.providerId else "",
                    )
                }
            }
        }
    }

    fun onProvider(p: Provider) {
        _ui.update { it.copy(provider = p, customProviderName = if (p != Provider.OTHER) "" else it.customProviderName) }
    }
    fun onCustomProviderName(value: String) { _ui.update { it.copy(customProviderName = value) } }
    fun onLabel(value: String) { _ui.update { it.copy(label = value) } }
    fun onApiKey(value: String) { _ui.update { it.copy(apiKey = value) } }
    fun toggleApiKeyVisibility() { _ui.update { it.copy(apiKeyVisible = !it.apiKeyVisible) } }
    fun onNotes(value: String) { _ui.update { it.copy(notes = value) } }
    fun onReplaceKey() { _ui.update { it.copy(replacingKey = true, existingMaskedKey = null, apiKey = "") } }

    fun save() {
        val s = _ui.value
        when {
            s.isEdit && !s.replacingKey -> {
                // metadata-only edit: keep the existing encrypted secret.
                viewModelScope.launch { saveEdit(editKeyId!!, s.provider, s.label, apiKey = null, s.notes, s.customProviderName) }
            }
            s.isEdit -> {
                val key = s.apiKey.trim()
                if (key.isBlank()) { _ui.update { it.copy(saveError = "API key is required") }; return }
                if (s.label.isBlank()) { _ui.update { it.copy(saveError = "Label is required") }; return }
                viewModelScope.launch { saveEdit(editKeyId!!, s.provider, s.label, key, s.notes, s.customProviderName) }
            }
            else -> {
                val key = s.apiKey.trim()
                if (key.isBlank()) { _ui.update { it.copy(saveError = "API key is required") }; return }
                if (s.label.isBlank()) { _ui.update { it.copy(saveError = "Label is required") }; return }
                viewModelScope.launch { saveAdd(s.provider, s.label, key, s.notes, s.customProviderName) }
            }
        }
    }

    private suspend fun saveAdd(provider: Provider, label: String, apiKey: String, notes: String, customProviderName: String) {
        _ui.update { it.copy(isSaving = true, saveError = null) }
        when (val outcome = addKey(NewKey(provider, label, apiKey, notes, customProviderName))) {
            is Outcome.Success -> { _ui.update { it.copy(isSaving = false) }; saved.emit(Unit) }
            is Outcome.Error -> {
                _ui.update { it.copy(isSaving = false, saveError = outcome.message) }
                if (outcome.kind == ErrorKind.MaxKeysExceeded) requestUpgrade.emit(Unit)
            }
        }
    }

    private suspend fun saveEdit(id: String, provider: Provider, label: String, apiKey: String?, notes: String, customProviderName: String) {
        _ui.update { it.copy(isSaving = true, saveError = null) }
        when (val outcome = updateKey(UpdateKey(id, provider, label, apiKey, notes, customProviderName))) {
            is Outcome.Success -> { saved.emit(Unit); _ui.update { it.copy(isSaving = false) } }
            is Outcome.Error -> { _ui.update { it.copy(isSaving = false, saveError = outcome.message) } }
        }
    }
}
