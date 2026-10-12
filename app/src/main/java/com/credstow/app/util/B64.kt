package com.credstow.app.util

/**
 * Tiny cross-platform Base64 helper backed by the platform/JDK decoder.
 *
 * On Android (minSdk 24) this is provided by the core-library desugaring
 * dependency, so the same source compiles cleanly into the JVM unit tests
 * (where java.util.Base64 is part of the JDK).
 */
internal object B64 {
    fun encode(bytes: ByteArray): String =
        java.util.Base64.getEncoder().encodeToString(bytes)

    fun decode(text: String): ByteArray? = runCatching {
        java.util.Base64.getDecoder().decode(text)
    }.getOrNull()
}

/** Mask a secret for display: keeps the first 4 and last 4 chars, bullets the rest. */
fun maskKey(apiKey: String): String {
    val s = apiKey.trim()
    if (s.isEmpty()) return ""
    if (s.length <= 8) return BULLET.repeat(s.length.coerceAtLeast(4))
    val first = s.take(4)
    val last = s.takeLast(4)
    val middle = BULLET.repeat(s.length - 8)
    return "$first$middle$last"
}

private const val BULLET = "•"

