package com.credstow.app.data

import com.credstow.app.crypto.PasswordCrypto
import com.credstow.app.crypto.PasswordCipherResult
import com.credstow.app.model.BackupEntry
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets

/**
 * Encodes/decodes an encrypted, password-protected backup blob.
 *
 * Layout: `Json(PasswordCipherResult)` where the inner ciphertext is itself a
 * `Json(List<BackupEntry>)`. Two layers of framing keeps the envelope
 * forward-compatible (future KDFs/versions bump [PasswordCipherResult.version]).
 *
 * Plaintext keys appear only transiently in memory between decrypt and
 * re-encrypt; they are never persisted to disk.
 */
class BackupCodec(
    private val passwordCrypto: PasswordCrypto,
    private val json: Json = defaultJson,
) {

    /** Encrypts all entries under the user's password, returning a self-contained string. */
    fun encode(entries: List<BackupEntry>, password: String): String {
        require(password.isNotBlank()) { "Backup password must not be empty" }
        val plain = json.encodeToString<List<BackupEntry>>(entries)
        val envelope = passwordCrypto.encrypt(plain.toByteArray(StandardCharsets.UTF_8), password)
        return json.encodeToString<PasswordCipherResult>(envelope)
    }

    /** Decrypts a backup blob. Returns null if the password is wrong or the blob is corrupt. */
    fun decode(blob: String, password: String): List<BackupEntry>? = runCatching {
        val envelope = json.decodeFromString<PasswordCipherResult>(blob.trim())
        val bytes = passwordCrypto.decrypt(envelope, password) ?: return@runCatching null
        val plain = bytes.toString(StandardCharsets.UTF_8)
        json.decodeFromString<List<BackupEntry>>(plain)
    }.getOrNull()

    companion object {
        val defaultJson: Json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            classDiscriminator = "type"
            isLenient = true
        }
    }
}

