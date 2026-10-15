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
import com.credstow.app.core.AppConst
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

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
        val isTrialActive: Boolean = false,
        val proTrialEndsAt: Long = 0L,
        val keyCount: Int = 0,
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

    /** Free 7-day trial end timestamp (epoch-ms). 0 = no trial. */
    val proTrialEndsAt: StateFlow<Long> = settings.proTrialEndsAt
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    /** Effective Pro state: true if purchased OR trial is active. */
    private val isProEffective = combine(isProState, proTrialEndsAt) { purchased, ends ->
        purchased || (ends > 0L && ends > System.currentTimeMillis())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _filter = MutableStateFlow(KeyFilter())
    val filter: StateFlow<KeyFilter> = _filter

    /** IDs of keys that have been swiped away but not yet confirmed-deleted. */
    private val pendingDeleteIds = MutableStateFlow<Set<String>>(emptySet())

    /**
     * IDs of keys currently "unlocked" for swipe-to-delete.
     *
     * Lock-to-delete: by default, every key is locked — swiping has no effect.
     * The user must long-press a key to unlock it, then swipe right to delete.
     * Long-pressing an already-unlocked key re-locks it. Keys auto-relock
     * after 5 seconds of inactivity so the list is never left in an unsafe state.
     */
    private val _unlockedKeys = MutableStateFlow<Set<String>>(emptySet())
    val unlockedKeys: StateFlow<Set<String>> = _unlockedKeys

    val uiState: StateFlow<UiState> = combine(allKeys, _filter, isProEffective, pendingDeleteIds, keySortBy) { keys, f, pro, pending, sort ->
        val isTrialActive = pro && !isProState.value
        val filtered = searchAndFilter(keys, f).filterNot { it.id in pending }
        val sorted = sortKeys(filtered, sort)
        UiState(
            keys = sorted,
            filter = f,
            isLoading = false,
            isPro = pro,
            isTrialActive = isTrialActive,
            proTrialEndsAt = proTrialEndsAt.value,
            keyCount = keys.size,
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

    // ---- Lock-to-delete ----
    // Keys are locked by default: swiping to delete is disabled until the
    // user long-presses the key to unlock it. A second long-press re-locks.

    /** True if this key's row currently allows swipe-to-delete. */
    fun isKeyUnlocked(id: String): Boolean = _unlockedKeys.value.contains(id)

    /** Toggle the locked/unlocked state of a key. */
    fun toggleKeyLock(id: String) {
        _unlockedKeys.update { current ->
            if (current.contains(id)) current - id else current + id
        }
        // Auto-relock: schedule a re-lock after 5 seconds so the list is
        // never left in an "all keys unlocked" state if the user walks away.
        viewModelScope.launch {
            kotlinx.coroutines.delay(5_000L)
            _unlockedKeys.update { it - id }
        }
    }

    /** Lock all keys immediately — called when the user scrolls or navigates away. */
    fun lockAllKeys() {
        _unlockedKeys.value = emptySet()
    }

    fun canAddKeyNow(): Boolean = canAddKey(isProState.value, allKeys.value.size)

    /** True when a free user is approaching the key limit (2 of 3 keys used) — show a gentle Pro banner. */
    val shouldShowProBanner: Boolean
        get() = !uiState.value.isPro && uiState.value.keyCount >= AppConst.FREE_KEY_LIMIT - 1

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
    // Policy: show an interstitial only when ALL of these are true:
    //   1. User does NOT have Pro
    //   2. User is at a "natural pause point" (list resume, settings visit,
    //      task completed — e.g. key test, clipboard copy)
    //   3. >= 3 consecutive key saves since the last ad
    //      (Play policy: 2 actions minimum between ads; we use 3 for safety)
    //   4. >= 1 minute of session time
    //   5. >= 2 minutes since the last ad (cooldown)
    // The old 30s background ticker was removed — ads only fire at pause
    // points so they can never interrupt a user mid-action.
    private var sessionStartMs = System.currentTimeMillis()
    private var lastSeenKeyCount = 0
    private var lastSeenKeyCountInitialized = false
    private val adLock = Mutex()

    /** Called by KeyListScreen on RESUMED — a natural pause point
     * (user returned from Add/Edit, Settings, or Key Detail). */
    fun onScreenResume() {
        lockAllKeys()  // Re-lock all keys after navigation
        viewModelScope.launch { checkAndShowAd(atPausePoint = true) }
    }

    /** Called from other natural pause points (e.g. after a key test
     * completes, after clipboard copy, after visiting Settings). */
    fun onPausePoint() {
        viewModelScope.launch { checkAndShowAd(atPausePoint = true) }
    }

    private suspend fun checkAndShowAd(atPausePoint: Boolean) {
        adLock.withLock {
            if (atPausePoint) {
                trackNewSaves()
            }
            if (isProState.value) return@withLock

            val now = System.currentTimeMillis()
            val consecutiveSaves = settings.consecutiveSavesSinceLastAd.first()
            val lastShown = settings.lastInterstitialShownAt.first()
            val timeSinceLastAd = if (lastShown > 0L) now - lastShown else now
            val sessionMs = now - sessionStartMs

            if (AdRules.shouldShowAd(
                consecutiveSaves = consecutiveSaves,
                sessionMs = sessionMs,
                timeSinceLastAd = timeSinceLastAd,
                isPro = isProState.value,
                atPausePoint = atPausePoint,
            )) {
                settings.setLastInterstitialShownAt(now)
                settings.setConsecutiveSavesSinceLastAd(0)
                _events.emit(Event.ShowInterstitial)
            }
        }
    }

    /** Compares current key count to last-seen count at a pause point.
     *  Increments the consecutive-saves counter for each new key added.
     *  Resets to 0 if keys were deleted (deletions aren't "saves"). */
    private suspend fun trackNewSaves() {
        val current = allKeys.value.size
        if (!lastSeenKeyCountInitialized) {
            lastSeenKeyCount = current
            lastSeenKeyCountInitialized = true
            return
        }
        if (current > lastSeenKeyCount) {
            val saves = settings.consecutiveSavesSinceLastAd.first()
            settings.setConsecutiveSavesSinceLastAd(saves + (current - lastSeenKeyCount))
        } else if (current < lastSeenKeyCount) {
            settings.setConsecutiveSavesSinceLastAd(0)
        }
        lastSeenKeyCount = current
    }
}

