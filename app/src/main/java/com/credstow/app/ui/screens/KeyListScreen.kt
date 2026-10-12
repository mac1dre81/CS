@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.credstow.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.credstow.app.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import com.credstow.app.ui.components.AdBanner
import com.credstow.app.ui.components.EmptyState
import com.credstow.app.ui.components.NoResultsState
import com.credstow.app.ui.components.ProLimitBanner
import com.credstow.app.ui.components.UpgradeDialog
import com.credstow.app.ui.components.FilterRow
import com.credstow.app.ui.components.KeyListItem
import com.credstow.app.ui.navigation.Routes
import com.credstow.app.domain.usecase.KeyFilter
import com.credstow.app.ui.util.BiometricAuth
import com.credstow.app.ui.viewmodel.KeyListViewModel
import com.credstow.app.di.ServiceLocator
import com.credstow.app.util.findActivity
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.flow.collect

private fun hasActiveFilter(f: KeyFilter) =
    f.provider != null || f.status != null || f.query.isNotBlank()

@Composable
fun KeyListScreen(
    vm: KeyListViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val ui by vm.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showUpgrade by rememberSaveable { mutableStateOf(false) }

    // Activity is needed both to show a full-screen interstitial and to observe
    // lifecycle so a milestone ad can fire right after returning from Save.
    val ctx = LocalContext.current
    val activity = ctx.findActivity() as? FragmentActivity

    // Pre-resolve strings for the event-collecting coroutine (stringResource
    // requires a CompositionLocal context, which is unavailable inside LaunchedEffect).
    val deletedMsg = stringResource(R.string.snackbar_key_deleted)
    val undoLabel = stringResource(R.string.snackbar_undo)
    val biometricTitle = stringResource(R.string.biometric_prompt_title)
    val biometricSubtitle = stringResource(R.string.biometric_prompt_subtitle)

    // Pre-load a free interstitial while the home screen is visible so it's
    // ready when a milestone / session trigger fires. Pro users never get here.
    LaunchedEffect(ui.isPro) {
        if (!ui.isPro) {
            ServiceLocator.getInstance(ctx).interstitialAdManager.load()
        }
    }

    // Whenever this screen is resumed (e.g. after coming back from Add/Edit),
    // let the VM re-evaluate the milestone + session ad rules.
    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.onScreenResume()
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is KeyListViewModel.Event.RequestUpgrade -> showUpgrade = true
                is KeyListViewModel.Event.Deleted -> snackbarHostState.showSnackbar(deletedMsg)
                is KeyListViewModel.Event.Message -> snackbarHostState.showSnackbar(event.text)
                is KeyListViewModel.Event.PendingDelete -> {
                    val result = snackbarHostState.showSnackbar(
                        message = deletedMsg,
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        vm.undoDelete(event.key)
                    } else {
                        vm.confirmDelete(event.key)
                    }
                }
                is KeyListViewModel.Event.ShowInterstitial -> {
                    // VM guarantees this only fires for free users; show the
                    // interstitial over the attached activity. `activity` is the
                    // AppCompatActivity hosting this composable (= FragmentActivity).
                    activity?.let { a ->
                        ServiceLocator.getInstance(ctx).interstitialAdManager.show(a) { }
                    }
                }
                is KeyListViewModel.Event.RequestBiometricAuth -> {
                    activity?.let { a ->
                        val auth = BiometricAuth(a)
                        val result = auth.authenticate(
                            title = biometricTitle,
                            subtitle = biometricSubtitle,
                        )
                        if (result) vm.onBiometricAuthSuccess()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (vm.canAddKeyNow()) navController.navigate(Routes.ADD)
                    else vm.onAddClicked()
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.list_add_key),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Smart Pro trial/limit banner for free users approaching their key limit
            if (vm.shouldShowProBanner) {
                ProLimitBanner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    keyCount = ui.keyCount,
                    onUpgrade = { navController.navigate(Routes.SETTINGS) },
                )
            }
            when {
                ui.isLoading -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                ui.keys.isEmpty() && !hasActiveFilter(ui.filter) -> {
                    EmptyState(Modifier.weight(1f))
                }
                ui.keys.isEmpty() && hasActiveFilter(ui.filter) -> {
                    NoResultsState(
                        modifier = Modifier.weight(1f),
                        onClear = { vm.clearFilters() },
                    )
                }
                else -> {
                    KeyListContent(
                        ui = ui,
                        vm = vm,
                        navController = navController,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                }
            }
            // AdMob banner for free users only; Pro users get an ad-free app.
            if (!ui.isPro) {
                AdBanner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    adUnitId = stringResource(R.string.admob_test_banner),
                )
            }
        }
    }

    if (showUpgrade) {
        UpgradeDialog(
            onDismiss = { showUpgrade = false },
            onUpgrade = {
                showUpgrade = false
                navController.navigate(Routes.SETTINGS)
            },
        )
    }
}

@Composable
private fun KeyListContent(
    ui: KeyListViewModel.UiState,
    vm: KeyListViewModel,
    navController: NavHostController,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        stickyHeader {
            Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
                OutlinedTextField(
                    value = ui.filter.query,
                    onValueChange = { vm.onSearch(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    placeholder = { Text(stringResource(R.string.list_search_hint)) },
                    singleLine = true,
                    maxLines = 1,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(),
                )
                FilterRow(filter = ui.filter, vm = vm)
                HorizontalDivider()
            }
        }
        items(ui.keys, key = { it.id }) { key ->
            val swipeState = rememberSwipeToDismissBoxState(
                initialValue = SwipeToDismissBoxValue.Settled,
                confirmValueChange = {
                    if (it == SwipeToDismissBoxValue.Settled) return@rememberSwipeToDismissBoxState true
                    vm.onSwipeDelete(key)
                    true
                },
            )
            SwipeToDismissBox(
                state = swipeState,
                backgroundContent = {
                    val progress = swipeState.progress
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                if (progress > 0f) {
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                                } else {
                                    Color.Transparent
                                }
                            ),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (progress > 0.1f) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(start = 16.dp)
                                    .size(20.dp),
                            )
                        }
                    }
                },
                enableDismissFromStartToEnd = true,
                enableDismissFromEndToStart = false,
                content = {
                    KeyListItem(
                        key = key,
                        onItemClick = { navController.navigate(Routes.detail(key.id)) },
                        onLongPressCopy = { vm.onCopy(key.id) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

