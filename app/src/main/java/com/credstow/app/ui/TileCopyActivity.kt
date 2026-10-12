package com.credstow.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.credstow.app.AuthTracker
import com.credstow.app.R
import com.credstow.app.di.ServiceLocator
import com.credstow.app.model.KeyStatus
import com.credstow.app.model.VaultKey
import com.credstow.app.ui.components.statusColor
import com.credstow.app.ui.theme.CredStowTheme
import com.credstow.app.ui.util.BiometricAuth
import com.credstow.app.util.findActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Launched from the Quick Settings tile. Shows a minimal key picker so the
 * user can copy a key without opening the full app.
 *
 * If biometric lock is enabled and the auto-lock timer has expired, the user
 * must authenticate first (fail-open if the prompt errors, matching
 * SplashScreen behaviour).
 *
 * After copying: a toast is shown and the Activity finishes immediately.
 */
class TileCopyActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sl = ServiceLocator.getInstance(this)

        setContent {
            CredStowTheme {
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()

                // Collect settings as state for immediate reads
                val biometricLockEnabled by sl.settingsRepository.biometricLockEnabled
                    .collectAsState(false)

                // Mutable UI state
                var authState by rememberSaveable { mutableStateOf(false) } // true = unlocked
                val keys = remember { mutableStateOf<List<VaultKey>>(emptyList()) }

                // On launch: check auth, then load keys
                LaunchedEffect(Unit) {
                    val needsUnlock = if (biometricLockEnabled) {
                        AuthTracker.requiresUnlock(sl.settingsRepository)
                    } else {
                        false
                    }

                    if (needsUnlock) {
                        val activity = context.findActivity() as? FragmentActivity
                        val auth = activity?.let { BiometricAuth(it) }
                        val result = auth?.authenticate(
                            title = getString(R.string.biometric_prompt_title),
                            subtitle = getString(R.string.biometric_prompt_subtitle),
                        ) ?: false
                        if (result) {
                            AuthTracker.lastAuthenticatedAt = System.currentTimeMillis()
                        }
                        // Fail-open: proceed regardless.
                    }
                    authState = true

                    // Load keys
                    keys.value = sl.keyRepository.getKeys()
                }

                if (!authState) {
                    AuthLockOverlay()
                } else {
                    KeyPicker(
                        keys = keys.value,
                        onKeyCopied = { key ->
                            coroutineScope.launch {
                                val plain = sl.keyRepository.getPlainKey(key.id)
                                if (plain == null) {
                                    Toast.makeText(
                                        context,
                                        "Could not decrypt key",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                    return@launch
                                }
                                val autoClear = sl.settingsRepository.clipboardAutoClearSeconds.first()
                                sl.clipboardHelper.copy(plain, autoClear, this)
                                Toast.makeText(
                                    context,
                                    "Copied ${key.label} to clipboard",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                finish()
                            }
                        },
                        onDismiss = { finish() },
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthLockOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = "Authenticating…",
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun KeyPicker(
    keys: List<VaultKey>,
    onKeyCopied: (VaultKey) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Drag handle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 32.dp, height = 4.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(0.2f), RoundedCornerShape(2.dp)),
            )
        }

        Text(
            text = "Copy key",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        if (keys.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No keys saved yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(keys) { key ->
                    KeyPickerItem(key = key, onClick = { onKeyCopied(key) })
                }
            }
        }
    }
}

@Composable
private fun KeyPickerItem(
    key: VaultKey,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            val provider = com.credstow.app.model.Provider.fromId(key.providerId)
            Text(
                text = provider.shortName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .size(width = 28.dp, height = 20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = key.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = key.maskedKey,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusChip(status = key.status)
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun StatusChip(status: KeyStatus) {
    val color = statusColor(status)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f),
    ) {
        Text(
            text = status.name.take(3),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
