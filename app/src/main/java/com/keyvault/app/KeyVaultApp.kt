package com.keyvault.app

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.keyvault.app.di.ServiceLocator

/**
 * Process-level Application class. Acts as the stable anchor for the
 * [ServiceLocator]; billing lifecycle is driven from [MainActivity] because it
 * needs the Play Billing client connected only while the UI is in the foreground.
 *
 * Also initializes the Google Mobile Ads SDK once per process — required before
 * any [com.google.android.gms.ads.AdView] can load an ad.
 */
class KeyVaultApp : Application() {
    val serviceLocator: ServiceLocator
        get() = ServiceLocator.getInstance(this)

    override fun onCreate() {
        super.onCreate()
        // Prime the DI graph early, then initialize the Mobile Ads SDK. This is
        // safe on a fresh process and a no-op when Ads has already been init'd.
        ServiceLocator.getInstance(this)
        MobileAds.initialize(this)
    }
}
