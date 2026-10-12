package com.credstow.app.billing

import com.credstow.app.domain.ProState
import com.credstow.app.prefs.SettingsRepository
import kotlinx.coroutines.flow.Flow

/** Bridges the DataStore-backed purchase state to the [ProState] domain contract. */
class SettingsProState(private val settings: SettingsRepository) : ProState {
    override val isPro: Flow<Boolean> = settings.isPro
    override suspend fun isProNow(): Boolean = settings.isProNow()
}

