package com.keyvault.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import com.keyvault.app.R
import com.keyvault.app.di.ServiceLocator
import com.keyvault.app.ui.screens.AddEditKeyScreen
import com.keyvault.app.ui.screens.KeyDetailScreen
import com.keyvault.app.ui.screens.KeyListScreen
import com.keyvault.app.ui.screens.OnboardingScreen
import com.keyvault.app.ui.screens.SettingsScreen
import com.keyvault.app.ui.screens.SplashScreen
import com.keyvault.app.ui.viewmodel.AddEditKeyViewModel
import com.keyvault.app.ui.viewmodel.KeyDetailViewModel
import com.keyvault.app.ui.viewmodel.KeyListViewModel
import com.keyvault.app.ui.viewmodel.OnboardingViewModel
import com.keyvault.app.ui.viewmodel.SettingsViewModel
import com.keyvault.app.ui.viewmodel.SplashViewModel
import com.keyvault.app.ui.viewmodel.factoryFor

/**
 * Single NavHost. Navigation is single-top from the splash destination so the
 * back stack never holds the launch screen after the first real destination.
 */
@Composable
fun KeyVaultNavHost(
    sl: ServiceLocator,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier,
    ) {
        composable(Routes.SPLASH) { entry ->
            val vm = entry.vm<SplashViewModel>(sl)
            SplashScreen(vm) { destination ->
                navController.navigate(destination) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                        saveState = true
                    }
                    launchSingleTop = true
                }
            }
        }

        composable(Routes.ONBOARDING) { entry ->
            val vm = entry.vm<OnboardingViewModel>(sl)
            OnboardingScreen(vm) {
                navController.navigate(Routes.LIST) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            }
        }

        composable(Routes.LIST) { entry ->
            val vm = entry.vm<KeyListViewModel>(sl)
            KeyListScreen(vm = vm, navController = navController)
        }

        composable(Routes.ADD) { entry ->
            val vm = entry.vm<AddEditKeyViewModel>(sl)
            AddEditKeyScreen(
                vm = vm,
                onDone = { navController.popBackStack() },
                onUpgrade = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.EDIT_PATTERN) { entry ->
            val vm = entry.vm<AddEditKeyViewModel>(sl)
            AddEditKeyScreen(
                vm = vm,
                onDone = { navController.popBackStack() },
                onUpgrade = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.DETAIL_PATTERN) { entry ->
            val vm = entry.vm<KeyDetailViewModel>(sl)
            KeyDetailScreen(vm = vm, navController = navController)
        }

        composable(Routes.SETTINGS) { entry ->
            val vm = entry.vm<SettingsViewModel>(sl)
            SettingsScreen(vm = vm, navController = navController)
        }
    }
}

@Composable
private inline fun <reified T : ViewModel> NavBackStackEntry.vm(sl: ServiceLocator): T {
    val factory = remember(this) { sl.factoryFor(arguments) }
    val provider = remember(this) { ViewModelProvider(this, factory) }
    return provider[T::class.java]
}
