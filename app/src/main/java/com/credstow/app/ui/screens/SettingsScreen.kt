@file:OptIn(ExperimentalMaterial3Api::class)

package com.credstow.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import com.credstow.app.ui.components.UpgradeDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.credstow.app.R
import com.credstow.app.billing.BillingEvent
import com.credstow.app.core.AppConst
import com.credstow.app.data.local.SortBy
import com.credstow.app.ui.theme.CredStowTheme
import com.credstow.app.ui.viewmodel.SettingsViewModel
import com.credstow.app.util.findActivity
import kotlinx.coroutines.flow.collect

@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val isPro by vm.isPro.collectAsState()
    val autoClear by vm.clipboardAutoClearSeconds.collectAsState()
    val biometric by vm.biometricLockEnabled.collectAsState()
    val autoLock by vm.autoLockAfterMinutes.collectAsState(AppConst.AUTO_LOCK_DEFAULT_MINUTES)
    val fontScale by vm.fontScale.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var exportDialog by rememberSaveable { mutableStateOf(false) }
    var importDialog by rememberSaveable { mutableStateOf(false) }
    var importBlob by rememberSaveable { mutableStateOf("") }
    var importPw by rememberSaveable { mutableStateOf("") }
    var exportPw by rememberSaveable { mutableStateOf("") }
    var showUpgrade by rememberSaveable { mutableStateOf(false) }
    var sortDialog by rememberSaveable { mutableStateOf(false) }

    val activity = LocalContext.current.findActivity()

    LaunchedEffect(Unit) {
        vm.messages.collect { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(Unit) {
        vm.billingEvents.collect { event ->
            when (event) {
                is BillingEvent.Purchased -> snackbarHostState.showSnackbar(activity.getString(R.string.settings_pro_unlocked))
                is BillingEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding),
        ) {
            // --- Pro ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.settings_pro_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        stringResource(R.string.settings_pro_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    if (isPro) {
                        Text(
                            stringResource(R.string.settings_pro_unlocked),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF16A34A),
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    } else {
                        TextButton(onClick = { vm.unlockPro(activity) }) {
                            Text("${stringResource(R.string.settings_pro_price)}  \u2022  ${stringResource(R.string.settings_pro_upgrade)}")
                        }
                    }
                }
            }

            // --- General ---
            SettingsRow(
                title = stringResource(R.string.settings_biometric),
                subtitle = stringResource(R.string.settings_biometric_hint),
                trailing = {
                    Switch(
                        checked = biometric,
                        onCheckedChange = { vm.onToggleBiometric() },
                    )
                },
            )
            SettingsRow(
                title = stringResource(R.string.settings_auto_lock_title),
                subtitle = if (biometric) {
                    if (autoLock == 0) stringResource(R.string.settings_auto_lock_never)
                    else stringResource(R.string.settings_auto_lock_summary, autoLock.toString())
                } else {
                    stringResource(R.string.settings_auto_lock_summary, "—")
                },
                enabled = biometric,
                trailing = {
                    Slider(
                        value = autoLock.coerceIn(0, AppConst.AUTO_LOCK_MAX_MINUTES).toFloat(),
                        onValueChange = { vm.setAutoLockAfterMinutes(it.toInt()) },
                        valueRange = 0f..AppConst.AUTO_LOCK_MAX_MINUTES.toFloat(),
                        modifier = Modifier.size(120.dp),
                    )
                },
            )
            SettingsRow(
                title = stringResource(R.string.settings_auto_clear, autoClear.toString()),
                subtitle = stringResource(R.string.settings_auto_clear_custom),
                enabled = true,
                trailing = {
                    Slider(
                        value = autoClear.coerceIn(
                            AppConst.CLIPBOARD_AUTO_CLEAR_MIN_SECONDS,
                            AppConst.CLIPBOARD_AUTO_CLEAR_MAX_SECONDS,
                        ).toFloat(),
                        onValueChange = { vm.setAutoClear(it.toInt()) },
                        valueRange = AppConst.CLIPBOARD_AUTO_CLEAR_MIN_SECONDS.toFloat()..AppConst.CLIPBOARD_AUTO_CLEAR_MAX_SECONDS.toFloat(),
                        modifier = Modifier.size(120.dp),
                    )
                },
            )

            SettingsRow(
                title = stringResource(R.string.accessibility_font_size),
                subtitle = stringResource(R.string.accessibility_font_size_summary, "%.2f".format(fontScale)),
                trailing = {
                    Slider(
                        value = fontScale,
                        onValueChange = { vm.setFontScale(it) },
                        valueRange = AppConst.FONT_SCALE_SMALL..AppConst.FONT_SCALE_EXTRA_LARGE,
                        modifier = Modifier.size(120.dp),
                    )
                },
            )
            SettingsRow(
                title = stringResource(R.string.settings_key_sort),
                subtitle = vm.sortLabel,
                icon = Icons.Default.DisplaySettings,
                enabled = isPro,
                onClick = { if (isPro) sortDialog = true else showUpgrade = true },
            )
            SettingsRow(
                title = stringResource(R.string.display_system),
                subtitle = stringResource(R.string.display_system_hint),
                icon = Icons.Default.DisplaySettings,
                onClick = {
                    activity.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
                },
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // --- Backup (Pro) ---
            SettingsRow(
                title = stringResource(R.string.settings_export),
                icon = Icons.Default.CloudUpload,
                enabled = isPro,
                onClick = { if (isPro) exportDialog = true else showUpgrade = true },
            )
            SettingsRow(
                title = stringResource(R.string.settings_import),
                icon = Icons.Default.CloudDownload,
                enabled = isPro,
                onClick = { if (isPro) importDialog = true else showUpgrade = true },
            )
            SettingsRow(
                title = stringResource(R.string.settings_privacy),
                enabled = true,
                onClick = {
                    activity.startActivity(Intent(activity, com.credstow.app.ui.PrivacyPolicyActivity::class.java))
                },
            )
            SettingsRow(
                title = "Refresh purchases",
                enabled = true,
                onClick = { vm.refreshPro() },
            )

            Spacer(Modifier.height(80.dp))
        }
    }

    if (showUpgrade) {
        UpgradeDialog(
            onDismiss = { showUpgrade = false },
            onUpgrade = { vm.unlockPro(activity); showUpgrade = false },
        )
    }

    if (sortDialog) {
        val currentSort by vm.keySortBy.collectAsState()
        AlertDialog(
            onDismissRequest = { sortDialog = false },
            title = { Text(stringResource(R.string.settings_key_sort)) },
            confirmButton = {
                TextButton(onClick = { sortDialog = false }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { sortDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            },
            text = {
                Column {
                    val options = listOf(
                        SortBy.CreatedAt to stringResource(R.string.sort_created),
                        SortBy.Label to stringResource(R.string.sort_label),
                        SortBy.LastUsedAt to stringResource(R.string.sort_last_used),
                        SortBy.ExpiryDate to stringResource(R.string.sort_expiry),
                    )
                    options.forEach { (sort, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { vm.setKeySortBy(sort); sortDialog = false },
                        ) {
                            RadioButton(
                                selected = currentSort == sort,
                                onClick = { vm.setKeySortBy(sort); sortDialog = false },
                            )
                            Text(label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
        )
    }

    if (exportDialog) {
        var pw by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { exportDialog = false },
            title = { Text(stringResource(R.string.settings_export)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = pw,
                        onValueChange = { pw = it },
                        label = { Text(stringResource(R.string.backup_password_hint)) },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (pw.length >= 8) {
                            vm.performExport(pw)
                            exportDialog = false
                        }
                    },
                    enabled = pw.length >= 8,
                ) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { exportDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    if (importDialog) {
        AlertDialog(
            onDismissRequest = { importDialog = false },
            title = { Text(stringResource(R.string.settings_import)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = importBlob,
                        onValueChange = { importBlob = it },
                        label = { Text(stringResource(R.string.import_paste_blob_hint)) },
                        minLines = 3,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importPw,
                        onValueChange = { importPw = it },
                        label = { Text(stringResource(R.string.backup_password_hint)) },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (importPw.length >= 8) {
                            vm.performImport(importBlob, importPw)
                            importDialog = false
                        }
                    },
                    enabled = importPw.length >= 8,
                ) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { importDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val clickable = if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .then(clickable)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    // Theme-driven (NOT Color.Unspecified.copy(alpha=...), which
                    // resolves to semi-transparent black and is illegible on the
                    // dark surface). onSurfaceVariant stays readable in both light
                    // and dark schemes.
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        } else if (onClick != null) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

