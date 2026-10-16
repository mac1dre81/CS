@file:OptIn(ExperimentalFoundationApi::class)
package com.credstow.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.credstow.app.R
import com.credstow.app.model.KeyStatus
import com.credstow.app.model.Provider
import com.credstow.app.model.VaultKey
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun providerTint(provider: Provider): Color = when (provider) {
    Provider.OPENAI -> Color(0xFF10A37F)
    Provider.ANTHROPIC -> Color(0xFFF59E0B)
    Provider.GOOGLE -> Color(0xFF1A73E8)
    Provider.OPENROUTER -> Color(0xFF7C3AED)
    Provider.AZURE_OPENAI -> Color(0xFF0078D4)
    Provider.HUGGING_FACE -> Color(0xFFFFD166)
    Provider.COHERE -> Color(0xFF325491)
    Provider.MISTRAL -> Color(0xFFEF4444)
    Provider.TOGETHER -> Color(0xFFEC4899)
    Provider.XAI_GROK -> Color(0xFF0D9488)
    Provider.REPLICATE -> Color(0xFF0EA5E9)
    Provider.OTHER -> Color(0xFF64748B)
}

/**
 * Localized label for a provider. For [Provider.OTHER] (the custom-typed bucket)
 * an optional `providerId` is returned verbatim when it carries a user-typed
 * provider name, so the list/detail surfaces e.g. "MyCustom LLM" instead of "Other".
 */
@Composable
fun providerLabel(provider: Provider, providerId: String = ""): String = when (provider) {
    Provider.OPENAI -> stringResource(R.string.provider_openai)
    Provider.ANTHROPIC -> stringResource(R.string.provider_anthropic)
    Provider.GOOGLE -> stringResource(R.string.provider_google)
    Provider.OPENROUTER -> stringResource(R.string.provider_openrouter)
    Provider.AZURE_OPENAI -> stringResource(R.string.provider_azure_openai)
    Provider.HUGGING_FACE -> stringResource(R.string.provider_hugging_face)
    Provider.COHERE -> stringResource(R.string.provider_cohere)
    Provider.MISTRAL -> stringResource(R.string.provider_mistral)
    Provider.TOGETHER -> stringResource(R.string.provider_together)
    Provider.XAI_GROK -> stringResource(R.string.provider_xai_grok)
    Provider.REPLICATE -> stringResource(R.string.provider_replicate)
    Provider.OTHER -> if (providerId.isNotBlank() && providerId != Provider.OTHER.id) providerId
        else stringResource(R.string.provider_other)
}

@Composable
fun ProviderBadge(
    provider: Provider,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(if (size <= 28.dp) 8.dp else 12.dp))
            .background(providerTint(provider))
            .padding(size / 5)
            .testTag("provider_badge_${provider.name.lowercase()}"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = provider.shortName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontFamily = FontFamily.SansSerif,
            // Provider initials are decorative; the provider name is announced
            // via the supporting text in KeyListItem or KeyDetailContent.
            maxLines = 1,
        )
    }
}

@Composable
fun statusColor(status: KeyStatus): Color = when (status) {
    KeyStatus.VALID -> Color(0xFF16A34A)
    KeyStatus.INVALID -> Color(0xFFDC2626)
    KeyStatus.UNTESTED -> Color(0xFF64748B)
    KeyStatus.TESTING -> Color(0xFF2563EB)
    KeyStatus.ERROR -> Color(0xFFDC2626)
}

@Composable
fun StatusChip(
    status: KeyStatus,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    val (label, color) = when (status) {
        KeyStatus.VALID -> stringResource(R.string.status_active) to statusColor(status)
        KeyStatus.INVALID -> stringResource(R.string.status_invalid) to statusColor(status)
        KeyStatus.UNTESTED -> stringResource(R.string.status_untested) to statusColor(status)
        KeyStatus.TESTING -> stringResource(R.string.status_testing) to statusColor(status)
        KeyStatus.ERROR -> stringResource(R.string.status_error) to statusColor(status)
    }
    val chipScale = if (size <= 20.dp) 0.75f else 1f
    AssistChip(
        onClick = {},
        enabled = false,
        label = {
            if (status == KeyStatus.TESTING) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(8.dp * chipScale)
                            .padding(end = 2.dp * chipScale),
                        strokeWidth = 1.dp,
                        color = color,
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        maxLines = 1,
                    )
                }
            } else {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    maxLines = 1,
                )
            }
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.12f),
            labelColor = color,
        ),
        modifier = modifier
            .size(height = size, width = size)
            .graphicsLayer(scaleX = chipScale, scaleY = chipScale),
    )
}

@Composable
fun KeyListItem(
    key: VaultKey,
    onItemClick: () -> Unit,
    onLongPressCopy: () -> Unit,
    onLockToggle: () -> Unit,
    locked: Boolean,
    modifier: Modifier = Modifier,
) {
    // Pre-resolve strings for non-composable semantics lambdas.
    val copyHint = stringResource(R.string.accessibility_long_press_copy)
    val lockedDesc = stringResource(R.string.accessibility_locked)
    val unlockedDesc = stringResource(R.string.accessibility_unlocked)
    val lockState = if (locked) lockedDesc else unlockedDesc

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onItemClick,
                onLongClick = onLongPressCopy,
            )
            .clip(RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .testTag("key_list_item_${key.id}")
            .semantics {
                // Announce lock state + copy gesture to screen readers.
                stateDescription = "$lockState, $copyHint"
            },
    ) {
        // Leading: provider badge (compact)
        ProviderBadge(provider = key.provider, size = 18.dp)
        Spacer(Modifier.width(6.dp))

        // Middle: label + provider name
        Column(Modifier.weight(1f)) {
            Text(
                text = key.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Text(
                providerLabel(key.provider, key.providerId),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }

        // Trailing: masked key + status + lock/unlock button
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(3.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ) {
                Text(
                    text = key.maskedKey,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = statusColor(key.status),
                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(4.dp))
            StatusChip(status = key.status, size = 14.dp)
            if (key.expiryDate != null) {
                val now = System.currentTimeMillis()
                val color = if (key.expiryDate <= now) MaterialTheme.colorScheme.error
                else if (key.expiryDate <= now + 7 * 24 * 60 * 60 * 1000L) Color(0xFFFF6B35)
                else MaterialTheme.colorScheme.tertiary
                Spacer(Modifier.width(3.dp))
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color),
                )
            }
            Spacer(Modifier.width(4.dp))
            // Lock/unlock button: toggles the key's swipe-to-delete state.
            // When locked (default), swiping is disabled — prevents accidental deletes.
            // When unlocked, swipe right to delete; tap the lock again to re-lock.
            IconButton(
                onClick = onLockToggle,
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    imageVector = if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = if (locked) lockedDesc else unlockedDesc,
                    tint = if (locked) MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f)
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
fun ApiKeyField(
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    hint: String = "",
    singleLine: Boolean = true,
    maxLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { if (label.isNotEmpty()) Text(label) },
        placeholder = { if (hint.isNotEmpty()) Text(hint) },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = singleLine,
        maxLines = maxLines,
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                val icon = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                Icon(imageVector = icon, contentDescription = if (visible) "Hide" else "Show")
            }
        },
        colors = OutlinedTextFieldDefaults.colors(),
    )
}

@Composable
fun AppKeyIcon(modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary) {
    Icon(
        painter = painterResource(R.drawable.ic_app_mark),
        contentDescription = null,
        tint = tint,
        modifier = modifier,
    )
}

@Composable
fun ProLimitBanner(
    modifier: Modifier = Modifier,
    keyCount: Int,
    onUpgrade: () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onUpgrade)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFC107),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "$keyCount/${com.credstow.app.core.AppConst.FREE_KEY_LIMIT} keys used — Pro gives you unlimited",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "Unlock unlimited keys, biometric re-auth, expiry dates, and ad-free",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onUpgrade) {
                Text(
                    stringResource(R.string.settings_pro_upgrade),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

