package com.credstow.app.ui.components

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

private const val TAG = "CredStow"

/**
 * A thin Compose wrapper around an AdMob banner [AdView].
 *
 * Uses Google's demo (test) ad unit by default so it is safe to click during
 * development — `loadAd` failures are logged, never thrown, so a network or
 * fill hiccup can never crash the app. For production, pass your real AdMob
 * banner unit id (created in the AdMob console) instead of the test id.
 */
@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    adUnitId: String,
) {
    AndroidView(
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                try {
                    // Google's demo ad units (ca-app-pub-3940...) serve test ads
                    // to any device without registering a test device, so no
                    // addTestDevice(...) call is required here.
                    val request = AdRequest.Builder().build()
                    loadAd(request)
                } catch (e: Throwable) {
                    // Ad loading is asynchronous and the SDK already surfaces
                    // failures on the "Ads" logcat tag; this is just a hard
                    // safety net so the UI can never crash.
                    Log.w(TAG, "AdBanner failed to load: ${e.message}")
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .testTag("ad_banner"),
    )
}

