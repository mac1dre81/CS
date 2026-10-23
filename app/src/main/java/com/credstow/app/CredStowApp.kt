package com.credstow.app

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.credstow.app.AuthTracker
import com.credstow.app.di.ServiceLocator

/**
 * Process-level Application class. Acts as the stable anchor for the
 * [ServiceLocator]; billing lifecycle is driven from [MainActivity] because it
 * needs the Play Billing client connected only while the UI is in the foreground.
 *
 * Also initializes the Google Mobile Ads SDK once per process — required before
 * any [com.google.android.gms.ads.AdView] can load an ad.
 *
 * Crashlytics is initialized only when Firebase has been auto-started from
 * app/google-services.json; otherwise the call is skipped so a missing config
 * file never crashes startup (see the conditional plugin block in build.gradle.kts).
 */
class CredStowApp : Application() {
    val serviceLocator: ServiceLocator
        get() = ServiceLocator.getInstance(this)

    override fun onCreate() {
        super.onCreate()
        AuthTracker.init(this)
        // Prime the DI graph early, then initialize the Mobile Ads SDK. This is
        // safe on a fresh process and a no-op when Ads has already been init'd.
        ServiceLocator.getInstance(this)
        MobileAds.initialize(this)
        initCrashlytics()
    }

    /**
     * Enables crash collection when Firebase is available. google-services.json is
     * git-ignored and may be absent in local/dev builds, in which case FirebaseApp
     * is never initialized and calling getInstance() would throw. Guard on that.
     */
    private fun initCrashlytics() {
        if (FirebaseApp.getApps(this).isNotEmpty()) {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        }
    }
}