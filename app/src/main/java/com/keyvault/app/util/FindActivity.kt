package com.keyvault.app.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Walks the context chain to find the attached [Activity] (needed for the Billing
 * flow). The previous version only inspected each context's *base*, so when
 * [LocalContext.current] was the Activity itself (the common Compose-in-an-Activity
 * case) the chain never matched and it threw `IllegalStateException` on every
 * Settings entry. Inspecting the current node first fixes that.
 */
fun Context.findActivity(): Activity {
    var ctx: Context = this
    while (true) {
        if (ctx is Activity) return ctx
        if (ctx is ContextWrapper) {
            ctx = ctx.baseContext
        } else {
            break
        }
    }
    throw IllegalStateException("Context is not attached to an Activity: $this")
}
