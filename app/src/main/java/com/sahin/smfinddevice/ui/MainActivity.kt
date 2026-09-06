package com.sahin.smfinddevice.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sahin.smfinddevice.storage.SecureConfigStore
import com.sahin.smfinddevice.ui.screens.AboutScreen
import com.sahin.smfinddevice.ui.screens.ActivityScreen
import com.sahin.smfinddevice.ui.screens.AuthorizedSetupScreen
import com.sahin.smfinddevice.ui.screens.BackupRestoreScreen
import com.sahin.smfinddevice.ui.screens.BatteryOptimizationScreen
import com.sahin.smfinddevice.ui.screens.DashboardScreen
import com.sahin.smfinddevice.ui.screens.HelpFaqScreen
import com.sahin.smfinddevice.ui.screens.PermissionsScreen
import com.sahin.smfinddevice.ui.screens.PrivacyScreen
import com.sahin.smfinddevice.ui.screens.RecoveryInActionScreen
import com.sahin.smfinddevice.ui.screens.SettingsScreen
import com.sahin.smfinddevice.ui.screens.setup.SetupWizardScreen
import com.sahin.smfinddevice.ui.theme.SMFindDeviceTheme
import com.sahin.smfinddevice.viewmodel.DashboardViewModel

private object Routes {
    const val SETUP = "setup"
    const val HOME = "home"
    const val ACTIVITY_TAB = "activity_tab"
    const val SETTINGS_TAB = "settings_tab"
    const val AUTHORIZED_SETUP = "authorized_setup"
    const val PERMISSIONS = "permissions"
    const val RECOVERY_IN_ACTION = "recovery_in_action"
    const val ABOUT = "about"
    const val BATTERY = "battery"
    const val BACKUP = "backup"
    const val HELP = "help"
    const val PRIVACY = "privacy"
}

private data class BottomTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val BOTTOM_TABS = listOf(
    BottomTab(Routes.HOME, "Home", Icons.Default.Home),
    BottomTab(Routes.ACTIVITY_TAB, "Activity", Icons.Default.ListAlt),
    BottomTab(Routes.SETTINGS_TAB, "Settings", Icons.Default.Settings)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val store = SecureConfigStore.getInstance(applicationContext)
        val setupComplete = store.hasAnyActiveRecoveryNumber() && store.enabledCommands().isNotEmpty()

        setContent {
            SMFindDeviceTheme {
                val navController = rememberNavController()
                // Shared across Home + Recovery In Action so a test started on one screen
                // is reflected on the other without re-triggering location acquisition.
                val dashboardViewModel: DashboardViewModel = viewModel(viewModelStoreOwner = this@MainActivity)

                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                val showBottomBar = currentRoute in BOTTOM_TABS.map { it.route }

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                BOTTOM_TABS.forEach { tab ->
                                    val selected = backStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            navController.navigate(tab.route) {
                                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                                        label = {
                                            Text(
                                                tab.label,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        },
                                        alwaysShowLabel = true
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = if (setupComplete) Routes.HOME else Routes.SETUP,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Routes.SETUP) {
                            SetupWizardScreen(
                                onFinished = {
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.SETUP) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Routes.HOME) {
                            DashboardScreen(
                                viewModel = dashboardViewModel,
                                onOpenRecoveryNumbers = { navController.navigate(Routes.AUTHORIZED_SETUP) },
                                onOpenCommands = { navController.navigate(Routes.AUTHORIZED_SETUP) },
                                onOpenPermissions = { navController.navigate(Routes.PERMISSIONS) },
                                onOpenSettings = { navController.navigate(Routes.SETTINGS_TAB) },
                                onOpenActivity = { navController.navigate(Routes.ACTIVITY_TAB) },
                                onOpenRecoveryInAction = { navController.navigate(Routes.RECOVERY_IN_ACTION) }
                            )
                        }
                        composable(Routes.ACTIVITY_TAB) {
                            ActivityScreen()
                        }
                        composable(Routes.SETTINGS_TAB) {
                            SettingsScreen(
                                onOpenPermissions = { navController.navigate(Routes.PERMISSIONS) },
                                onOpenBattery = { navController.navigate(Routes.BATTERY) },
                                onOpenBackup = { navController.navigate(Routes.BACKUP) },
                                onOpenAbout = { navController.navigate(Routes.ABOUT) },
                                onOpenHelp = { navController.navigate(Routes.HELP) },
                                onOpenPrivacy = { navController.navigate(Routes.PRIVACY) }
                            )
                        }
                        composable(Routes.AUTHORIZED_SETUP) {
                            AuthorizedSetupScreen(onBack = { navController.popBackStack() })
                        }
                        composable(Routes.PERMISSIONS) {
                            PermissionsScreen(onBack = { navController.popBackStack() })
                        }
                        composable(Routes.RECOVERY_IN_ACTION) {
                            RecoveryInActionScreen(
                                onBack = { navController.popBackStack() },
                                viewModel = dashboardViewModel
                            )
                        }
                        composable(Routes.ABOUT) {
                            AboutScreen(onBack = { navController.popBackStack() })
                        }
                        composable(Routes.BATTERY) {
                            BatteryOptimizationScreen(onBack = { navController.popBackStack() })
                        }
                        composable(Routes.BACKUP) {
                            BackupRestoreScreen(onBack = { navController.popBackStack() })
                        }
                        composable(Routes.HELP) {
                            HelpFaqScreen(onBack = { navController.popBackStack() })
                        }
                        composable(Routes.PRIVACY) {
                            PrivacyScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
