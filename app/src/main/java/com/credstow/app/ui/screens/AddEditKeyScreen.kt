@file:OptIn(ExperimentalMaterial3Api::class)

package com.credstow.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.credstow.app.R
import com.credstow.app.model.Provider
import com.credstow.app.ui.components.ApiKeyField
import com.credstow.app.ui.components.providerLabel
import com.credstow.app.ui.viewmodel.AddEditKeyViewModel
import com.credstow.app.util.findActivity
import com.credstow.app.util.formatDate
import kotlinx.coroutines.flow.collect

@Composable
fun AddEditKeyScreen(
    vm: AddEditKeyViewModel,
    onDone: () -> Unit,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by vm.ui.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var providerDialog by remember { mutableStateOf(false) }
    var selectedProvider by rememberSaveable { mutableStateOf(ui.provider) }

    LaunchedEffect(Unit) {
        vm.saved.collect { onDone() }
    }
    LaunchedEffect(Unit) {
        vm.requestUpgrade.collect { onUpgrade() }
    }
    val saveError = ui.saveError
    LaunchedEffect(saveError) {
        if (saveError != null) snackbarHostState.showSnackbar(saveError)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (ui.isEdit) stringResource(R.string.edit_title)
                        else stringResource(R.string.add_title),
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onDone) { Text(stringResource(R.string.action_cancel)) }
                },
                actions = {
                    TextButton(
                        onClick = { vm.save() },
                        enabled = !ui.isSaving,
                        modifier = Modifier.testTag("save_key_button"),
                    ) {
                        Text(
                            stringResource(R.string.action_save),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // Provider selector — a clickable row (Text + icon) instead of a read-only
            // TextField, whose own gesture arena can swallow the tap and prevent the
            // picker dialog from opening on some devices.
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.field_provider),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !ui.isLoading) { providerDialog = true }
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                        .testTag("add_edit_provider_selector"),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = providerLabel(ui.provider, ui.customProviderName),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (ui.provider == Provider.OTHER && ui.customProviderName.isBlank())
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = stringResource(R.string.accessibility_provider_selector),
                        )
                    }
                }
            }
            // When "Other" is selected, let the user type any API provider name.
            if (ui.provider == Provider.OTHER) {
                OutlinedTextField(
                    value = ui.customProviderName,
                    onValueChange = vm::onCustomProviderName,
                    label = { Text(stringResource(R.string.field_provider_custom)) },
                    placeholder = { Text(stringResource(R.string.field_provider_custom_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_edit_custom_provider_field"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !ui.isLoading,
                )
                Spacer(Modifier.height(16.dp))
            }
            Spacer(Modifier.height(16.dp))

            // Label
            OutlinedTextField(
                value = ui.label,
                onValueChange = vm::onLabel,
                label = { Text(stringResource(R.string.field_label)) },
                placeholder = { Text(stringResource(R.string.field_label_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_edit_label_field"),
                shape = RoundedCornerShape(12.dp),
                enabled = !ui.isLoading,
            )
            Spacer(Modifier.height(16.dp))

            // API key (masked) or masked preview when editing without replacement
            if (ui.isEdit && !ui.replacingKey && !ui.existingMaskedKey.isNullOrEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                ) {
                    Column {
                        Text(
                            text = ui.existingMaskedKey ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = { vm.onReplaceKey() }) {
                            Text(stringResource(R.string.action_replace_key))
                        }
                    }
                }
            } else {
                ApiKeyField(
                    value = ui.apiKey,
                    onValueChange = vm::onApiKey,
                    visible = ui.apiKeyVisible,
                    onToggleVisibility = vm::toggleApiKeyVisibility,
                    label = stringResource(R.string.field_api_key),
                    hint = stringResource(R.string.field_api_key_hint),
                    singleLine = true,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(16.dp))

            // Notes
            OutlinedTextField(
                value = ui.notes,
                onValueChange = vm::onNotes,
                label = { Text(stringResource(R.string.field_notes)) },
                placeholder = { Text(stringResource(R.string.field_notes_hint)) },
                singleLine = false,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = !ui.isLoading,
            )

            // Expiry date (Pro feature)
            Spacer(Modifier.height(16.dp))
            val expiry = ui.expiryDate
            val nowMs = System.currentTimeMillis()
            val expiryColor = if (expiry != null && expiry <= nowMs) MaterialTheme.colorScheme.error
                else if (expiry != null && expiry <= nowMs + 7 * 24 * 60 * 60 * 1000L) Color(0xFFFF6B35)
                else MaterialTheme.colorScheme.tertiary
            val ctx = LocalContext.current
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {
                    val activity = ctx.findActivity() as? androidx.fragment.app.FragmentActivity
                    if (activity != null) {
                        val cal = java.util.Calendar.getInstance()
                        if (expiry != null) cal.timeInMillis = expiry
                        android.app.DatePickerDialog(
                            activity,
                            { _, y, m, d ->
                                val c = java.util.Calendar.getInstance().apply {
                                    set(y, m, d, 0, 0, 0)
                                    set(java.util.Calendar.MILLISECOND, 0)
                                }
                                vm.onExpiryDate(c.timeInMillis)
                            },
                            cal.get(java.util.Calendar.YEAR),
                            cal.get(java.util.Calendar.MONTH),
                            cal.get(java.util.Calendar.DAY_OF_MONTH),
                        ).show()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = if (expiry != null) expiryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = if (expiry != null) formatDate(expiry)
                        else stringResource(R.string.expiry_no_date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (expiry != null) expiryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (expiry != null) {
                    TextButton(onClick = { vm.onExpiryDate(null) }) {
                        Text(stringResource(R.string.expiry_clear))
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }

    if (providerDialog) {
        AlertDialog(
            onDismissRequest = { providerDialog = false },
            title = { Text(stringResource(R.string.field_provider)) },
                text = {
                    Column {
                        Provider.entries.forEach { p ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedProvider = p },
                            ) {
                                RadioButton(
                                    selected = selectedProvider == p,
                                    onClick = { selectedProvider = p },
                                )
                                Text(providerLabel(p), modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        vm.onProvider(selectedProvider)
                        providerDialog = false
                    }) {
                        Text(stringResource(R.string.action_save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { providerDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            )
        }
    }

