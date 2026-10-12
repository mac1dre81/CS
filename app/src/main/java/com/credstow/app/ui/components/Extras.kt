package com.credstow.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ChipColors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.credstow.app.R
import com.credstow.app.model.KeyStatus
import com.credstow.app.model.Provider
import com.credstow.app.domain.usecase.KeyFilter
import com.credstow.app.ui.viewmodel.KeyListViewModel

@Composable
fun FilterRow(
    filter: KeyFilter,
    vm: KeyListViewModel,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            AssistChip(
                onClick = { vm.setProviderFilter(null) },
                label = { Text(stringResource(R.string.list_filter_all)) },
                colors = chipColors(selected = filter.provider == null),
                modifier = Modifier.padding(end = 4.dp),
            )
            Provider.entries.forEach { p ->
                AssistChip(
                    onClick = {
                        if (filter.provider == p) vm.setProviderFilter(null) else vm.setProviderFilter(p)
                    },
                    label = { Text(providerLabel(p)) },
                    colors = chipColors(selected = filter.provider == p),
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            val statuses = listOf(KeyStatus.UNTESTED, KeyStatus.VALID, KeyStatus.INVALID)
            statuses.forEach { s ->
                AssistChip(
                    onClick = {
                        if (filter.status == s) vm.setStatusFilter(null) else vm.setStatusFilter(s)
                    },
                    label = { Text(statusLabel(s)) },
                    colors = chipColors(selected = filter.status == s, tintForStatus(s)),
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun chipColors(selected: Boolean, tint: Color = Color.Unspecified): ChipColors =
    if (selected) {
        AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            labelColor = MaterialTheme.colorScheme.primary,
        )
    } else {
        AssistChipDefaults.assistChipColors(
            containerColor = Color.Transparent,
            labelColor = if (tint != Color.Unspecified) tint else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

@Composable
private fun tintForStatus(s: KeyStatus): Color = when (s) {
    KeyStatus.VALID -> Color(0xFF16A34A)
    KeyStatus.INVALID -> Color(0xFFDC2626)
    KeyStatus.UNTESTED -> Color(0xFF64748B)
    KeyStatus.TESTING -> Color(0xFF2563EB)
    KeyStatus.ERROR -> Color(0xFFDC2626)
}

@Composable
private fun statusLabel(s: KeyStatus): String = when (s) {
    KeyStatus.VALID -> stringResource(R.string.status_active)
    KeyStatus.INVALID -> stringResource(R.string.status_invalid)
    KeyStatus.UNTESTED -> stringResource(R.string.status_untested)
    KeyStatus.TESTING -> stringResource(R.string.status_testing)
    KeyStatus.ERROR -> stringResource(R.string.status_error)
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.list_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.list_empty_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun UpgradeDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.list_free_limit_reached)) },
        text = { Text(stringResource(R.string.list_free_limit_body)) },
        confirmButton = {
            TextButton(onClick = { onUpgrade(); onDismiss() }) { Text(stringResource(R.string.list_upgrade)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
        modifier = modifier,
    )
}

@Composable
fun NoResultsState(
    modifier: Modifier = Modifier,
    onClear: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.list_no_results_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.list_no_results_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onClear) {
            Text(stringResource(R.string.list_clear_filters))
        }
    }
}

