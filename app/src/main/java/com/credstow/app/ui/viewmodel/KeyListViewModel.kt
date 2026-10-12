package com.credstow.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.credstow.app.domain.KeyRepository
import com.credstow.app.domain.ProState
import com.credstow.app.domain.usecase.CanAddKeyUseCase
import com.credstow.app.domain.usecase.DeleteKeyUseCase
import com.credstow.app.domain.usecase.KeyFilter
import com.credstow.app.domain.usecase.ObserveKeysUseCase
import com.credstow.app.domain.usecase.SearchAndFilterUseCase
import com.credstow.app.model.KeyStatus
import com.credstow.app.model.Provider
import com.credstow.app.model.VaultKey
import com.credstow.app.AuthTracker
import com.credstow.app.R
import com.credstow.app.ads.AdRules
import com.credstow.app.data.local.SortBy
import com.credstow.app.prefs.SettingsRepository
import com.credstow.app.util.ClipboardHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch

class KeyListViewModel(
    observeKeys: ObserveKeysUseCase,
    private val searchAndFilter: SearchAndFilterUseCase,
    private val deleteKey: DeleteKeyUseCase,
    private val canAddKey: CanAddKeyUseCase,
    private val proState: ProState,
    private val repository: KeyRepository,
    private val clipboardHelper: ClipboardHelper,
    private val settings: SettingsRepository,
) : ViewModel() {

    data class UiState(
        val keys: List<VaultKey> = emptyList(),
        val filter: KeyFilter = KeyFilter(),
        val isLoading: Boolean = true,
        val isPro: Boolean = false,
    )

    // SharingStarted.Eagerly is intentional here: WhileSubscribed(5_000) would
    // tear down Room's cold observeAll() Flow (and its InvalidationTracker)
    // when the KeyListScreen is not visible. On return from Settings → Import,
    // the re-subscription race can miss DB writes that happened during the
    // WhileSubscribed cancellation window, so imported keys never surface in
    // the list. Keeping the observer alive at all times guarantees the
    // InvalidationTracker fires and the StateFlow is always current.
    private val allKeys = observeKeys()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val isProState = proState.isPro
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val keySortBy = settings.keySortBy
        .stateIn(viewModelScope, SharingStarted.Eagerly, SortBy.CreatedAt)

    private val _filter = MutableStateFlow(KeyFilter())
    val filter: StateFlow<KeyFilter> = _filter

    /** IDs of keys that have been swiped away but not yet confirmed-deleted. */
    private val pendingDeleteIds = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<UiState> = combine(allKeys, _filter, isProState, pendingDeleteIds, keySortBy) { keys, f, pro, pending, sort ->
        val filtered = searchAndFilter(keys, f).filterNot { it.id in pending }
        val sorted = sortKeys(filtered, sort)
        UiState(
            keys = sorted,
            filter = f,
            isLoading = false,
            isPro = pro,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState(isLoading = true))

    private fun sortKeys(keys: List<VaultKey>, sort: SortBy): List<VaultKey> = when (sort) {
        SortBy.CreatedAt -> keys.sortedByDescending { it.createdAt }
        SortBy.Label -> keys.sortedBy { it.label }
        SortBy.LastUsedAt -> keys.sortedWith(
            compareByDescending<VaultKey> { it.lastUsedAt ?: 0L }
                .thenByDescending { it.createdAt }
        )
        SortBy.ExpiryDate -> keys.sortedWith(
            compareBy<VaultKey> { it.expiryDate ?: Long.MAX_VALUE }
                .thenByDescending { it.createdAt }
        )
    }

    /** One-shot events surfaced to the UI. */
    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    sealed interface Event {
        data object RequestUpgrade : Event
        data class Deleted(val remaining: Int) : Event
        data class Message(val text: String) : Event
        /** Free-tier only: surface a full-page interstitial to the UI. */
        data object ShowInterstitial : Event
        /** Emitted when a key is swiped-to-delete; UI must show an Undo Snackbar. */
        data class PendingDelete(val key: VaultKey) : Event
        /** Emitted when biometric re-auth is required before a copy/test action can proceed. */
        data object RequestBiometricAuth : Event
    }

    fun onSearch(query: String) {
        _filter.update { it.copy(query = query) }
    }

    fun setProviderFilter(provider: Provider?) {
        _filter.update { it.copy(provider = provider) }
    }

    fun setStatusFilter(status: KeyStatus?) {
        _filter.update { it.copy(status = status) }
    }

    fun clearFilters() {
        _filter.value = KeyFilter()
    }

    fun canAddKeyNow(): Boolean = canAddKey(isProState.value, allKeys.value.size)

    fun onDelete(id: String) {
        viewModelScope.launch {
            deleteKey(id)
            _events.emit(Event.Deleted(allKeys.value.size - 1))
        }
    }

    // ---- Swipe-to-delete with undo ----
    // The key is NOT removed from the DB until the Snackbar times out without
    // being undone. It is only hidden from the UI list (via pendingDeleteIds)
    // so the user can change their mind.

    fun onSwipeDelete(key: VaultKey) {
        pendingDeleteIds.update { it + key.id }
        viewModelScope.launch { _events.emit(Event.PendingDelete(key)) }
    }

    fun undoDelete(key: VaultKey) {
        pendingDeleteIds.update { it - key.id }
    }

    fun confirmDelete(key: VaultKey) {
        viewModelScope.launch {
            deleteKey(key.id)
            // Remove from pending AFTER DB delete so the key stays filtered
            // during the brief window between Snackbar timeout and DB write.
            pendingDeleteIds.update { it - key.id }
        }
    }

    /** ID of the key pending copy after biometric re-auth. */
    private var pendingCopyKeyId: String? = null

    fun onCopy(id: String) {
        viewModelScope.launch {
            if (AuthTracker.requiresUnlock(settings)) {
                pendingCopyKeyId = id
                _events.emit(Event.RequestBiometricAuth)
            } else {
                doCopy(id)
            }
        }
    }

    /** Called by the UI after successful biometric authentication. */
    fun onBiometricAuthSuccess() {
        AuthTracker.lastAuthenticatedAt = System.currentTimeMillis()
        pendingCopyKeyId?.let { id ->
            pendingCopyKeyId = null
            doCopy(id)
        }
    }

    private fun doCopy(id: String) {
        viewModelScope.launch {
            val autoClear = settings.clipboardAutoClearSeconds.first()
            val plain = repository.getPlainKey(id)
            if (plain == null) {
                _events.emit(Event.Message("Could not decrypt key"))
                return@launch
            }
            clipboardHelper.copy(plain, autoClear, viewModelScope)
            _events.emit(Event.Message("Key copied to clipboard. It will be cleared in ${autoClear}s."))
        }
    }

    fun onAddClicked() {
        if (!canAddKeyNow()) {
            viewModelScope.launch { _events.emit(Event.RequestUpgrade) }
        }
    }

    // ---- Free-tier interstitial scheduling (Pro users NEVER see these) ----
    // Two triggers, BOTH gated by isPro so Pro users never get interrupted:
    //  - milestone: every KEYS_PER_AD keys present in the vault (3, 6, 9 …),
    //    shown once per milestone.
    //  - session:   after ~SESSION_MIN_MS of foreground time in this session,
    //    with AD_COOLDOWN_MS enforced between any two interstitials.
    private var sessionStartMs = System.currentTimeMillis()
    private val adLock = Mutex()

    init {
        // Periodic re-check so an idle free user still gets a time-based ad.
        viewModelScope.launch {
            while (true) {
                delay(AdRules.TICK_MS)
                maybeEmitAd()
            }
        }
    }

    /** Called by KeyListScreen on each RESUMED so a milestone ad can fire
     * immediately after the user returns from saving a key. */
    fun onScreenResume() {
        viewModelScope.launch { maybeEmitAd() }
    }

    private suspend fun maybeEmitAd() {
        adLock.withLock {
            if (isProState.value) return@withLock
            val now = System.currentTimeMillis()
            val lastShown = settings.lastInterstitialShownAt.first()
            // Cooldown between any two interstitials (persisted so it survives
            // process restarts; this also de-dupes the 30s ticker + resume race).
            if (now - lastShown < AdRules.AD_COOLDOWN_MS) return@withLock

            val count = allKeys.value.size
            var lastMilestone = settings.lastInterstitialMilestone.first()
            // If keys were deleted below the milestone we last showed, reset the
            // tracker so re-adding a multiple of KEYS_PER_AD can fire again.
            val adjusted = AdRules.resetMilestone(count, lastMilestone)
            if (adjusted != lastMilestone) {
                lastMilestone = adjusted
                settings.setLastInterstitialMilestone(lastMilestone)
            }

            val milestone = AdRules.milestoneDue(count, lastMilestone, isPro = false)
            val session = AdRules.sessionDue(now, sessionStartMs, lastShown, isPro = false)
            if (milestone || session) {
                settings.setLastInterstitialShownAt(now)
                if (milestone) settings.setLastInterstitialMilestone(count)
                _events.emit(Event.ShowInterstitial)
            }
        }
    }
}

