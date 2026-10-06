package com.keyvault.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

private const val TAG = "KeyVault"

/**
 * Loads and shows a single AdMob interstitial.
 *
 * Lifetime: the caller pre-loads on the free home screen (see KeyListScreen)
 * and calls [show] when a milestone / session trigger fires. If the ad isn't
 * ready when [show] is called, it no-ops and invokes [onDismissed] immediately
 * — a later trigger will retry once the ad has loaded. The ad is re-loaded
 * after each show/dismiss so the next trigger has a chance to fire.
 *
 * Pro-gating is the caller's job: KeyListViewModel only ever emits a
 * `ShowInterstitial` event for free users, so this manager is never invoked
 * for Pro (zero interstitials on the paid version).
 */
class InterstitialAdManager(
    context: Context,
    private val adUnitId: String,
) {
    private val ctx = context.applicationContext
    private var ad: InterstitialAd? = null
    private var loading = false
    private var loadScheduled = false

    /** Pre-loads (or re-loads) the interstitial so it's ready to show. */
    fun load() {
        if (loading || loadScheduled || ad != null) return
        loadScheduled = true
        InterstitialAd.load(
            ctx,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    ad = interstitialAd
                    loading = false
                    loadScheduled = false
                    Log.d(TAG, "InterstitialAd: loaded (ready)")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    ad = null
                    loading = false
                    loadScheduled = false
                    Log.d(TAG, "InterstitialAd: load failed: ${error.message}")
                }
            },
        )
    }

    fun isLoaded(): Boolean = ad != null

    /**
     * Shows the ad over [activity]. If it isn't loaded yet, schedules a load
     * and invokes [onDismissed] immediately so the caller can continue — a
     * later trigger will retry the show once the ad has loaded.
     */
    fun show(activity: Activity, onDismissed: () -> Unit) {
        val ready = ad
        if (ready == null) {
            if (!loading && !loadScheduled) load()
            Log.d(TAG, "InterstitialAd: not ready, skipping this trigger")
            onDismissed()
            return
        }
        ready.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "InterstitialAd: dismissed")
                ad = null
                load()
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.d(TAG, "InterstitialAd: show failed: ${error.message}")
                ad = null
                load()
                onDismissed()
            }
        }
        ready.show(activity)
    }
}
