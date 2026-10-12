package com.credstow.app.billing

import android.content.Context
import android.content.pm.ApplicationInfo
import com.credstow.app.domain.ProState
import com.credstow.app.prefs.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

/**
 * Bridges the DataStore-backed purchase state to the [ProState] domain contract.
 *
 * In debug builds (APK installed from Android Studio / `adb install-debug`),
 * all Pro features are unlocked regardless of purchase state. This lets
 * developers and QA test unlimited keys, biometric lock, backup/restore,
 * and interstitial-suppression without buying the product.
 *
 * The debug check uses [ApplicationInfo.FLAG_DEBUGGABLE], which is set
 * automatically by the build system for `debug` build-type variants and
 * is never present in release builds.
 */
class SettingsProState(
    private val settings: SettingsRepository,
    context: Context,
) : ProState {

    private val isDebugBuild =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    override val isPro: Flow<Boolean> =
        if (isDebugBuild) flowOf(true)
        else settings.isPro

    override suspend fun isProNow(): Boolean =
        isDebugBuild || settings.isProNow()
}
