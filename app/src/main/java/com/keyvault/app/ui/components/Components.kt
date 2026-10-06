@file:OptIn(ExperimentalFoundationApi::class)
package com.keyvault.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.keyvault.app.R
import com.keyvault.app.model.KeyStatus
import com.keyvault.app.model.Provider
import com.keyvault.app.model.VaultKey

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
            .padding(size / 5),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = provider.shortName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontFamily = FontFamily.SansSerif,
        )
    }
}

@Composable
fun StatusChip(status: KeyStatus, modifier: Modifier = Modifier) {
    val (label, color) = when (status) {
        KeyStatus.VALID -> stringResource(R.string.status_active) to Color(0xFF16A34A)
        KeyStatus.INVALID -> stringResource(R.string.status_invalid) to Color(0xFFDC2626)
        KeyStatus.UNTESTED -> stringResource(R.string.status_untested) to Color(0xFF64748B)
        KeyStatus.TESTING -> stringResource(R.string.status_testing) to Color(0xFF2563EB)
        KeyStatus.ERROR -> stringResource(R.string.status_error) to Color(0xFFDC2626)
    }
    AssistChip(
        onClick = {},
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                maxLines = 1,
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.12f),
            labelColor = color,
        ),
        modifier = modifier,
    )
}

@Composable
fun KeyListItem(
    key: VaultKey,
    onItemClick: () -> Unit,
    onLongPressCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(key.label) },
        supportingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProviderBadge(provider = key.provider, size = 24.dp)
                Spacer(Modifier.width(8.dp))
                Text(providerLabel(key.provider, key.providerId), style = MaterialTheme.typography.bodyMedium)
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = key.maskedKey,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                StatusChip(status = key.status)
            }
        },
        modifier = modifier
            .combinedClickable(onClick = onItemClick, onLongClick = onLongPressCopy)
            .clip(RoundedCornerShape(8.dp)),
    )
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
        painter = painterResource(R.drawable.ic_launcher_foreground),
        contentDescription = null,
        tint = tint,
        modifier = modifier,
    )
}
