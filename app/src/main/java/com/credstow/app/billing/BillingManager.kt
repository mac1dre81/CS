package com.credstow.app.billing

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.PurchasesUpdatedListener
import com.credstow.app.core.AppConst
import com.credstow.app.prefs.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * Manages the one-time Pro purchase (`pro_unlock`).
 *
 * Responsibilities:
 *  - connect to Play Billing and (re)query owned INAPP purchases,
 *  - persist the owned product ids to [SettingsRepository] (the Pro source of truth),
 *  - launch the billing flow when the user upgrades.
 *
 * All Play-Billing calls happen off the UI thread; every external call is
 * defensive so a flaky BillingClient never crashes the app (it simply stays Free).
 */
class BillingManager(
    context: Context,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
) : PurchasesUpdatedListener {

    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val bgDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    private var connected = false

    private val client: BillingClient by lazy {
        BillingClient.newBuilder(appContext)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .setListener(this)
            .build()
    }

    private val _events = MutableSharedFlow<BillingEvent>(replay = 0)
    val events: Flow<BillingEvent> = _events.asSharedFlow()
    val isPro: Flow<Boolean> = settings.isPro

    /** Call from onStart; idempotent. */
    fun start() {
        if (connected) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    connected = true
                    refreshPurchases()
                } else {
                    scheduleRetry()
                }
            }

            override fun onBillingServiceDisconnected() {
                connected = false
                scheduleRetry()
            }
        })
    }

    private fun scheduleRetry() {
        main.postDelayed({ start() }, RETRY_MS_MS)
    }

    fun end() {
        try {
            if (client.isReady) client.endConnection()
        } catch (_: Throwable) { /* best-effort */ }
        connected = false
    }

    /** Re-query owned INAPP purchases and persist the active set. */
    private fun refreshPurchases() {
        scope.launch(bgDispatcher) {
            try {
                client.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                ) { result, purchases ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        val active = (purchases ?: emptyList())
                            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                            .mapNotNull { it.skus.firstOrNull() }
                            .toSet()
                        scope.launch { settings.setPurchasedProductIds(active) }
                    }
                }
            } catch (_: Throwable) {
                // Stay Free if Billing is unavailable.
            }
        }
    }

    /** Public hook so the UI can re-sync purchases after returning from the checkout flow. */
    fun syncPurchases() = refreshPurchases()

    /** Product details (carries the localized price) for the Pro unlock. */
    suspend fun queryProductDetails(): List<ProductDetails> = withContext(bgDispatcher) {
        try {
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(AppConst.PRO_PRODUCT_ID)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    )
                )
                .build()
            suspendCancellableCoroutine { cont ->
                client.queryProductDetailsAsync(params) { _, details ->
                    cont.resume(details ?: emptyList()) { /* no-op on cancellation */ }
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun launchBillingFlow(activity: Activity) {
        scope.launch {
            val details = queryProductDetails()
            if (details.isEmpty()) {
                _events.emit(BillingEvent.Error("Pro unlock is only available in the Play Store release of CredStow"))
                return@launch
            }
            val params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(details.first())
                            .build()
                    )
                )
                .build()
            val rc = client.launchBillingFlow(activity, params)
            if (rc.responseCode != BillingClient.BillingResponseCode.OK) {
                _events.emit(BillingEvent.Error("Unable to start checkout (code=${rc.responseCode})", rc.responseCode))
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            process(purchases)
        }
    }

    private fun process(purchases: List<Purchase>) {
        for (p in purchases) {
            if (p.purchaseState == Purchase.PurchaseState.PURCHASED && !p.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(p.purchaseToken)
                    .build()
                client.acknowledgePurchase(params, object : AcknowledgePurchaseResponseListener {
                    override fun onAcknowledgePurchaseResponse(result: BillingResult) {
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) refreshPurchases()
                    }
                })
            }
        }
    }

    companion object {
        private const val RETRY_MS_MS = 30_000L
    }
}

