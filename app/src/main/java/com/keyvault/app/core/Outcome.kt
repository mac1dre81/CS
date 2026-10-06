package com.keyvault.app.core

/**
 * Central limits & product identifiers for KeyVault.
 * MVP pricing (see App Info Doc §6):
 *  - Free: local storage, up to [FREE_KEY_LIMIT] keys.
 *  - Pro: one-time purchase [PRO_PRODUCT_ID] -> unlimited keys, test ping,
 *          encrypted backup/import, biometric lock.
 */
object AppConst {
    const val FREE_KEY_LIMIT = 3
    const val PRO_PRODUCT_ID = "pro_unlock"
    const val PREMIUM_PRICE_PLACEHOLDER = "$4.99 one-time"
    const val CLIPBOARD_AUTO_CLEAR_DEFAULT_SECONDS = 30
    const val CLIPBOARD_AUTO_CLEAR_MIN_SECONDS = 5
    const val CLIPBOARD_AUTO_CLEAR_MAX_SECONDS = 120

    /** In-app font-scale presets (Accessibility). 1.0f == "system default" (no override). */
    const val FONT_SCALE_SMALL = 0.9f
    const val FONT_SCALE_DEFAULT = 1.0f
    const val FONT_SCALE_LARGE = 1.15f
    const val FONT_SCALE_EXTRA_LARGE = 1.35f

    /** AES/GCM cipher parameter: 12-byte IV, 128-bit auth tag, 256-bit key. */
    const val AES_KEY_SIZE_BITS = 256
    const val GCM_IV_SIZE_BYTES = 12
    const val GCM_TAG_SIZE_BITS = 128
}

/** Generic success/failure envelope returned by mutating use-cases. */
sealed class Outcome<out T> {
    /** Operation completed; [value] holds the result (may be Unit). */
    data class Success<T>(val value: T) : Outcome<T>()

    /** Operation failed. [kind] lets the UI react specifically (e.g. show upgrade). */
    data class Error(
        val message: String,
        val kind: ErrorKind = ErrorKind.Unknown,
        val cause: Throwable? = null,
    ) : Outcome<Nothing>()

    companion object {
        fun <T> success(value: T): Outcome<T> = Success(value)
        fun <T> error(message: String, kind: ErrorKind = ErrorKind.Unknown, cause: Throwable? = null): Outcome<T> =
            Error(message, kind, cause)
    }
}

enum class ErrorKind {
    MaxKeysExceeded, InvalidInput, Crypto, Backup, Network, Billing, Auth, Biometric, Unknown
}

/** Domain-level exceptions. Messages are safe to surface to the user (never the key). */
sealed class VaultException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class MaxKeysExceededException : VaultException(
        "Free tier allows up to ${AppConst.FREE_KEY_LIMIT} keys. Upgrade to Pro for unlimited keys."
    )
    class InvalidInputException(message: String) : VaultException(message)
    class CryptoException(message: String, cause: Throwable? = null) : VaultException(message, cause)
    class BackupException(message: String, cause: Throwable? = null) : VaultException(message, cause)
    class NetworkException(message: String, cause: Throwable? = null) : VaultException(message, cause)
}
