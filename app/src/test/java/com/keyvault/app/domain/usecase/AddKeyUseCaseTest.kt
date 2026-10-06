package com.keyvault.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.keyvault.app.core.AppConst
import com.keyvault.app.core.ErrorKind
import com.keyvault.app.core.Outcome
import com.keyvault.app.crypto.InMemorySecretKeyProvider
import com.keyvault.app.crypto.AesGcmCrypto
import com.keyvault.app.data.FakeKeyDao
import com.keyvault.app.data.KeyRepositoryImpl
import com.keyvault.app.domain.ProState
import com.keyvault.app.model.NewKey
import com.keyvault.app.model.Provider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test

private class FakeProState(private val pro: Boolean) : ProState {
    override val isPro: Flow<Boolean> = flowOf(pro)
    override suspend fun isProNow(): Boolean = pro
}

private fun repo() = KeyRepositoryImpl(
    dao = FakeKeyDao(),
    secretKeyProvider = InMemorySecretKeyProvider(),
    crypto = AesGcmCrypto(),
    ioDispatcher = Dispatchers.Unconfined,
)

class AddKeyUseCaseTest {

    private fun validKey(i: Int) = NewKey(
        provider = Provider.OPENAI,
        label = "key $i",
        apiKeyValue = "sk-${i}validkey1234567890",
        notes = "",
    )

    @Test
    fun freeUserCanAddUpToLimit() = runTest {
        val addKey = AddKeyUseCase(repo(), FakeProState(pro = false))
        repeat(AppConst.FREE_KEY_LIMIT) {
            val outcome = addKey(validKey(it))
            assertThat(outcome).isInstanceOf(Outcome.Success::class.java)
        }
    }

    @Test
    fun freeUserIsBlockedBeyondLimit() = runTest {
        val r = repo()
        val addKey = AddKeyUseCase(r, FakeProState(pro = false))
        repeat(AppConst.FREE_KEY_LIMIT) { addKey(validKey(it)) }

        val extra = addKey(validKey(99))
        assertThat(extra).isInstanceOf(Outcome.Error::class.java)
        assertThat((extra as Outcome.Error).kind).isEqualTo(ErrorKind.MaxKeysExceeded)
        assertThat(r.count()).isEqualTo(AppConst.FREE_KEY_LIMIT)
    }

    @Test
    fun proUserIsUnbounded() = runTest {
        val r = repo()
        val addKey = AddKeyUseCase(r, FakeProState(pro = true))
        val target = AppConst.FREE_KEY_LIMIT + 3
        repeat(target) { addKey(validKey(it)) }
        assertThat(r.count()).isEqualTo(target)
    }
}
