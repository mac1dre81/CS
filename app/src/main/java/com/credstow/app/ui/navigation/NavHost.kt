package com.credstow.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import com.credstow.app.R
import com.credstow.app.di.ServiceLocator
import com.credstow.app.ui.screens.AddEditKeyScreen
import com.credstow.app.ui.screens.KeyDetailScreen
import com.credstow.app.ui.screens.KeyListScreen
import com.credstow.app.ui.screens.OnboardingScreen
import com.credstow.app.ui.screens.SettingsScreen
import com.credstow.app.ui.screens.SplashScreen
import com.credstow.app.ui.viewmodel.AddEditKeyViewModel
import com.credstow.app.ui.viewmodel.KeyDetailViewModel
import com.credstow.app.ui.viewmodel.KeyListViewModel
import com.credstow.app.ui.viewmodel.OnboardingViewModel
import com.credstow.app.ui.viewmodel.SettingsViewModel
import com.credstow.app.ui.viewmodel.SplashViewModel
import com.credstow.app.ui.viewmodel.factoryFor

private const val NAV_ANIM_MS = 250

/**
 * Single NavHost. Navigation is single-top from the splash destination so the
 * back stack never holds the launch screen after the first real destination.
 *
 * Predictive back gesture: the composable destinations below are equipped
 * with enter/exit transitions. On Android 13+ (API 33), the system's
 * predictive back gesture drives these transitions automatically — the
 * incoming screen slides in from the right while the outgoing screen
 * slides out, giving the user a native, polished back-swipe.
 */
@Composable
fun CredStowNavHost(
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

        composable(
            Routes.ONBOARDING,
            enterTransition = { fadeIn(tween(NAV_ANIM_MS)) },
            exitTransition = { fadeOut(tween(NAV_ANIM_MS)) },
            popEnterTransition = { fadeIn(tween(NAV_ANIM_MS)) },
            popExitTransition = { fadeOut(tween(NAV_ANIM_MS)) },
        ) { entry ->
            val vm = entry.vm<OnboardingViewModel>(sl)
            OnboardingScreen(vm) {
                navController.navigate(Routes.LIST) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            }
        }

        composable(
            Routes.LIST,
            enterTransition = { slideInHorizontally(tween(NAV_ANIM_MS)) { it } },
            exitTransition = { slideOutHorizontally(tween(NAV_ANIM_MS)) { -it } },
            popEnterTransition = { slideInHorizontally(tween(NAV_ANIM_MS)) { -it } },
            popExitTransition = { slideOutHorizontally(tween(NAV_ANIM_MS)) { it } },
        ) { entry ->
            val vm = entry.vm<KeyListViewModel>(sl)
            KeyListScreen(vm = vm, navController = navController)
        }

        composable(
            Routes.ADD,
            enterTransition = { slideInHorizontally(tween(NAV_ANIM_MS)) { it } },
            exitTransition = { slideOutHorizontally(tween(NAV_ANIM_MS)) { it } },
            popEnterTransition = { slideInHorizontally(tween(NAV_ANIM_MS)) { it } },
            popExitTransition = { slideOutHorizontally(tween(NAV_ANIM_MS)) { it } },
        ) { entry ->
            val vm = entry.vm<AddEditKeyViewModel>(sl)
            AddEditKeyScreen(
                vm = vm,
                onDone = { navController.popBackStack() },
                onUpgrade = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(
            Routes.EDIT_PATTERN,
            enterTransition = { slideInHorizontally(tween(NAV_ANIM_MS)) { it } },
            exitTransition = { slideOutHorizontally(tween(NAV_ANIM_MS)) { it } },
            popEnterTransition = { slideInHorizontally(tween(NAV_ANIM_MS)) { it } },
            popExitTransition = { slideOutHorizontally(tween(NAV_ANIM_MS)) { it } },
        ) { entry ->
            val vm = entry.vm<AddEditKeyViewModel>(sl)
            AddEditKeyScreen(
                vm = vm,
                onDone = { navController.popBackStack() },
                onUpgrade = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(
            Routes.DETAIL_PATTERN,
            enterTransition = { slideInHorizontally(tween(NAV_ANIM_MS)) { it } },
            exitTransition = { slideOutHorizontally(tween(NAV_ANIM_MS)) { -it } },
            popEnterTransition = { slideInHorizontally(tween(NAV_ANIM_MS)) { it } },
            popExitTransition = { slideOutHorizontally(tween(NAV_ANIM_MS)) { -it } },
        ) { entry ->
            val vm = entry.vm<KeyDetailViewModel>(sl)
            KeyDetailScreen(vm = vm, navController = navController)
        }

        composable(
            Routes.SETTINGS,
            enterTransition = { slideInHorizontally(tween(NAV_ANIM_MS)) { it } },
            exitTransition = { slideOutHorizontally(tween(NAV_ANIM_MS)) { it } },
            popEnterTransition = { slideInHorizontally(tween(NAV_ANIM_MS)) { it } },
            popExitTransition = { slideOutHorizontally(tween(NAV_ANIM_MS)) { it } },
        ) { entry ->
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

