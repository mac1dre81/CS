package com.keyvault.app.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Copies text to the system clipboard and (optionally) auto-clears it after a
 * configurable delay so secrets don't linger.
 *
 * Safety: the scheduled clear only wipes the clipboard when it still holds
 * exactly what we put there, so pasting something else is never clobbered.
 */
class ClipboardHelper(context: Context) {

    private val cm = context.applicationContext
        .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    private var lastCopied: String? = null
    private var clearJob: Job? = null
    private var clearScope: CoroutineScope? = null

    fun copy(text: String, autoClearSeconds: Int, scope: CoroutineScope, onCopied: (() -> Unit)? = null) {
        clearJob?.cancel()
        val clip = ClipData.newPlainText(LABEL_API_KEY, text)
        cm.setPrimaryClip(clip)
        lastCopied = text
        clearScope = scope
        onCopied?.invoke()
        if (autoClearSeconds > 0) {
            clearJob = scope.launch {
                delay(autoClearSeconds.toLong() * 1000L)
                if (isActive && matchesCurrentClip()) {
                    cm.setPrimaryClip(ClipData.newPlainText("", ""))
                }
                lastCopied = null
            }
        } else {
            lastCopied = null
            clearJob = null
        }
    }

    fun clearScheduled() {
        clearJob?.cancel()
        clearJob = null
        clearScope = null
        lastCopied = null
    }

    private fun matchesCurrentClip(): Boolean {
        return try {
            val clip = cm.primaryClip ?: return false
            if (clip.itemCount < 1) return false
            val current = clip.getItemAt(0).text?.toString()
            current != null && current.isNotEmpty() && current == lastCopied
        } catch (_: Throwable) {
            false
        }
    }

    companion object {
        const val LABEL_API_KEY = "KeyVault API key"
    }
}
