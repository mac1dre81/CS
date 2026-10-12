@file:OptIn(ExperimentalMaterial3Api::class)

package com.credstow.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.credstow.app.R
import com.credstow.app.model.Provider
import com.credstow.app.model.VaultKey
import com.credstow.app.ui.components.AppKeyIcon
import com.credstow.app.ui.components.ProviderBadge
import com.credstow.app.ui.components.StatusChip
import com.credstow.app.ui.components.providerLabel
import com.credstow.app.ui.navigation.Routes
import com.credstow.app.ui.util.BiometricAuth
import com.credstow.app.ui.viewmodel.KeyDetailViewModel
import com.credstow.app.util.formatDate
import com.credstow.app.util.formatDateTime
import com.credstow.app.util.findActivity
import kotlinx.coroutines.flow.collect

@Composable
fun KeyDetailScreen(
    vm: KeyDetailViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val key by vm.key.collectAsState()
    val canTest by vm.canTest.collectAsState()
    val isTesting by vm.isTesting.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDelete by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context.findActivity() as? androidx.fragment.app.FragmentActivity

    LaunchedEffect(Unit) {
        vm.message.collect { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(Unit) {
        vm.deleted.collect { navController.popBackStack() }
    }
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            if (event is KeyDetailViewModel.Event.RequestBiometricAuth) {
                val auth = activity?.let { BiometricAuth(it) }
                val result = auth?.authenticate(
                    title = context.getString(R.string.biometric_prompt_title),
                    subtitle = context.getString(R.string.biometric_prompt_subtitle),
                ) ?: false
                if (result) vm.onBiometricAuthSuccess()
            }
        }
    }

    val supportsTest = key?.provider?.supportsTest == true
    val showTestFab = !showDelete && supportsTest

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(key?.label ?: stringResource(R.string.detail_loading)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { vm.onCopy() }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.detail_copy))
                    }
                    key?.let {
                        IconButton(onClick = { navController.navigate(Routes.edit(it.id)) }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.detail_edit))
                        }
                    }
                    IconButton(onClick = { showDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.detail_delete))
                    }
                },
            )
        },
        floatingActionButton = {
            if (showTestFab) {
                FloatingActionButton(
                    onClick = { if (!isTesting) vm.onTest() },
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(stringResource(R.string.detail_test))
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { padding ->
        if (key == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                AppKeyIcon(Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            }
        } else {
            KeyDetailContent(key!!, vm, Modifier.padding(padding))
        }
    }

    if (showDelete && key != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.detail_delete_confirm, key!!.label)) },
            text = { Text(stringResource(R.string.detail_delete, key!!.label)) },
            confirmButton = {
                TextButton(onClick = { vm.onDelete(); showDelete = false }) { Text(stringResource(R.string.detail_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun KeyDetailContent(key: VaultKey, vm: KeyDetailViewModel, modifier: Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = key.maskedKey,
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            color = if (key.status.name == "VALID") Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurface,
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = { vm.onCopy() }) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.detail_copy))
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 12.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProviderBadge(provider = key.provider, size = 32.dp)
            Text(
                providerLabel(key.provider, key.providerId),
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.weight(1f))
            StatusChip(key.status)
        }

        Text(
            stringResource(R.string.detail_created, formatDateTime(key.createdAt)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.detail_updated, formatDateTime(key.updatedAt)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (key.lastUsedAt != null) {
            Text(
                stringResource(R.string.detail_last_used, formatDateTime(key.lastUsedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                stringResource(R.string.detail_last_used_never),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Expiration badge (Pro feature)
        if (key.expiryDate != null) {
            val now = System.currentTimeMillis()
            val expiryColor = if (key.expiryDate <= now) MaterialTheme.colorScheme.error
            else if (key.expiryDate <= now + 7 * 24 * 60 * 60 * 1000L) Color(0xFFFF6B35)
            else MaterialTheme.colorScheme.tertiary
            val expiryText = if (key.expiryDate <= now) stringResource(R.string.expiry_expired)
            else stringResource(R.string.expiry_expires_on, formatDate(key.expiryDate))
            Text(
                expiryText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = expiryColor,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        if (key.notes.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.field_notes),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(key.notes, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(24.dp))
    }
}

