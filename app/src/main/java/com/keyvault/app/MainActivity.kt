package com.keyvault.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import com.keyvault.app.billing.BillingManager
import com.keyvault.app.core.AppConst
import com.keyvault.app.di.ServiceLocator
import com.keyvault.app.ui.navigation.KeyVaultNavHost
import com.keyvault.app.ui.theme.KeyVaultTheme

class MainActivity : AppCompatActivity() {

    private lateinit var billing: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sl = ServiceLocator.getInstance(this)
        billing = sl.billingManager
        setContent {
            // Re-reads the accessibility font scale so the whole UI re-composes
            // with the new typography instantly when the user changes it.
            val fontScale = sl.settingsRepository.fontScale
                .collectAsState(initial = AppConst.FONT_SCALE_DEFAULT).value
            KeyVaultTheme(fontScale = fontScale) {
                KeyVaultNavHost(sl)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        billing.start()
    }

    override fun onStop() {
        super.onStop()
        billing.end()
    }
}

/** Convenience for tests / screenshots: render the nav host without Activity boilerplate. */
@Composable
fun PreviewNavHost(sl: ServiceLocator) {
    KeyVaultTheme {
        KeyVaultNavHost(sl)
    }
}
