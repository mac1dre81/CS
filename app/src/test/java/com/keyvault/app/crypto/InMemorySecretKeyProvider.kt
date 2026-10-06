package com.keyvault.app.crypto

import javax.crypto.SecretKey

/** JVM-only [SecretKeyProvider] backed by an in-memory AES key (for unit tests). */
class InMemorySecretKeyProvider(private val key: SecretKey = generateRandomAesKey()) : SecretKeyProvider {
    override fun getOrCreateKey(): SecretKey = key
}
