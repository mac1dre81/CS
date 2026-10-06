package com.keyvault.app.billing

import com.keyvault.app.domain.ProState
import com.keyvault.app.prefs.SettingsRepository
import kotlinx.coroutines.flow.Flow

/** Bridges the DataStore-backed purchase state to the [ProState] domain contract. */
class SettingsProState(private val settings: SettingsRepository) : ProState {
    override val isPro: Flow<Boolean> = settings.isPro
    override suspend fun isProNow(): Boolean = settings.isProNow()
}
