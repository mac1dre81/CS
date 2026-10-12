package com.credstow.app.ui.viewmodel

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.credstow.app.di.ServiceLocator

/**
 * Manual [ViewModelProvider.Factory]. Navigation arguments arrive as a [Bundle]
 * (the NavBackStackEntry `arguments`); keyed primitives like the vault-key id
 * are extracted here and passed directly into the ViewModels. This keeps the
 * ViewModels free of Android `SavedStateHandle` plumbing (easier to unit-test).
 */
class CredStowViewModelFactory(
    private val sl: ServiceLocator,
    private val args: Bundle? = null,
) : ViewModelProvider.Factory {

    private val keyId: String? get() = args?.getString("id")

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        SplashViewModel::class.java ->
            SplashViewModel(sl.settingsRepository)

        OnboardingViewModel::class.java ->
            OnboardingViewModel(sl.settingsRepository)

        KeyListViewModel::class.java ->
            KeyListViewModel(
                sl.observeKeys,
                sl.searchAndFilter,
                sl.deleteKey,
                sl.canAddKey,
                sl.proState,
                sl.keyRepository,
                sl.clipboardHelper,
                sl.settingsRepository,
            )

        KeyDetailViewModel::class.java ->
            KeyDetailViewModel(
                keyId = checkNotNull(keyId) { "KeyDetail requires a key id argument" },
                repository = sl.keyRepository,
                deleteKey = sl.deleteKey,
                testKey = sl.testKey,
                proState = sl.proState,
                clipboardHelper = sl.clipboardHelper,
                settings = sl.settingsRepository,
            )

        AddEditKeyViewModel::class.java ->
            AddEditKeyViewModel(
                editKeyId = keyId,
                repository = sl.keyRepository,
                addKey = sl.addKey,
                updateKey = sl.updateKey,
            )

        SettingsViewModel::class.java ->
            SettingsViewModel(
                billingManager = sl.billingManager,
                settings = sl.settingsRepository,
                proState = sl.proState,
                exportBackup = sl.exportBackup,
                importBackup = sl.importBackup,
                clipboardHelper = sl.clipboardHelper,
            )

        else -> error("Unknown ViewModel type: ${modelClass.name}")
    } as T
}

/** Convenience accessor so screens can build a factory from navigation args. */
fun ServiceLocator.factoryFor(args: Bundle?): CredStowViewModelFactory =
    CredStowViewModelFactory(this, args)

