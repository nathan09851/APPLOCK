package com.nathan.applock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nathan.applock.data.AppLockerModule
import com.nathan.applock.service.AppLockAccessibilityService
import com.nathan.applock.ui.applist.AppListScreen
import com.nathan.applock.ui.onboarding.OnboardingAccessibilityScreen
import com.nathan.applock.ui.onboarding.OnboardingBatteryScreen
import com.nathan.applock.ui.onboarding.isAccessibilityServiceEnabled
import com.nathan.applock.ui.pin.PinSetupScreen
import com.nathan.applock.ui.settings.SettingsScreen

object NavRoutes {
    const val APP_LIST = "app_list"
    const val SETTINGS = "settings"
    const val PIN_SETUP = "pin_setup"
    const val ONBOARDING_ACCESSIBILITY = "onboarding_accessibility"
    const val ONBOARDING_BATTERY = "onboarding_battery"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val authRepo = AppLockerModule.secureAuthRepository
    val lockRepo = AppLockerModule.lockRepository

    val onboardingComplete by lockRepo.onboardingComplete.collectAsStateWithLifecycle(initialValue = false)
    val accessibilityEnabled = isAccessibilityServiceEnabled(context, AppLockAccessibilityService::class.java)

    val startRoute = when {
        !authRepo.isPinSetUp -> NavRoutes.PIN_SETUP
        !accessibilityEnabled && !onboardingComplete -> NavRoutes.ONBOARDING_ACCESSIBILITY
        else -> NavRoutes.APP_LIST
    }

    NavHost(
        navController = navController,
        startDestination = startRoute
    ) {
        composable(NavRoutes.APP_LIST) {
            AppListScreen(
                onNavigateToSettings = { navController.navigate(NavRoutes.SETTINGS) },
                onNavigateToPinSetup = { navController.navigate(NavRoutes.PIN_SETUP) }
            )
        }
        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChangePin = { navController.navigate(NavRoutes.PIN_SETUP) }
            )
        }
        composable(NavRoutes.PIN_SETUP) {
            PinSetupScreen(
                onSetupComplete = { needsOnboarding ->
                    if (needsOnboarding || !accessibilityEnabled) {
                        navController.navigate(NavRoutes.ONBOARDING_ACCESSIBILITY) {
                            popUpTo(NavRoutes.PIN_SETUP) { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                }
            )
        }
        composable(NavRoutes.ONBOARDING_ACCESSIBILITY) {
            OnboardingAccessibilityScreen(
                onNext = {
                    navController.navigate(NavRoutes.ONBOARDING_BATTERY) {
                        popUpTo(NavRoutes.ONBOARDING_ACCESSIBILITY) { inclusive = true }
                    }
                }
            )
        }
        composable(NavRoutes.ONBOARDING_BATTERY) {
            OnboardingBatteryScreen(
                onFinish = {
                    navController.navigate(NavRoutes.APP_LIST) {
                        popUpTo(NavRoutes.ONBOARDING_BATTERY) { inclusive = true }
                    }
                }
            )
        }
    }
}
