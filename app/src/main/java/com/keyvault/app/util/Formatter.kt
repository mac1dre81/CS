package com.keyvault.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Lightweight, locale-aware date formatting (pure JDK -> JVM-testable). */
fun formatDateTime(epochMs: Long): String {
    val fmt = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
    return fmt.format(Date(epochMs))
}

fun formatDate(epochMs: Long): String {
    val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return fmt.format(Date(epochMs))
}
