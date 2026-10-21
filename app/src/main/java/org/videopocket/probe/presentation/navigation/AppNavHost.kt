package org.videopocket.probe.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.videopocket.probe.AppContainer
import org.videopocket.probe.presentation.history.HistoryScreen
import org.videopocket.probe.presentation.history.HistoryViewModel
import org.videopocket.probe.presentation.home.HomeScreen
import org.videopocket.probe.presentation.imagestopdf.ImagesToPdfScreen
import org.videopocket.probe.presentation.imagestopdf.ImagesToPdfViewModel
import org.videopocket.probe.presentation.merge.MergeScreen
import org.videopocket.probe.presentation.merge.MergeViewModel
import org.videopocket.probe.presentation.settings.SettingsScreen
import org.videopocket.probe.presentation.settings.SettingsViewModel
import org.videopocket.probe.presentation.split.SplitScreen
import org.videopocket.probe.presentation.split.SplitViewModel

@Composable
fun AppNavHost(
    container: AppContainer,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavRoutes = listOf(
        Screen.Home.route,
        Screen.History.route,
        Screen.Settings.route
    )

    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Screen.Home.route,
                        onClick = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == Screen.Home.route) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "الرئيسية"
                            )
                        },
                        label = { Text("الرئيسية") }
                    )

                    NavigationBarItem(
                        selected = currentRoute == Screen.History.route,
                        onClick = {
                            navController.navigate(Screen.History.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == Screen.History.route) Icons.Filled.History else Icons.Outlined.History,
                                contentDescription = "السجل"
                            )
                        },
                        label = { Text("السجل") }
                    )

                    NavigationBarItem(
                        selected = currentRoute == Screen.Settings.route,
                        onClick = {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == Screen.Settings.route) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = "الإعدادات"
                            )
                        },
                        label = { Text("الإعدادات") }
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToMerge = { navController.navigate(Screen.Merge.route) },
                    onNavigateToImagesToPdf = { navController.navigate(Screen.ImagesToPdf.route) },
                    onNavigateToSplit = { navController.navigate(Screen.Split.route) }
                )
            }

            composable(Screen.History.route) {
                val historyViewModel: HistoryViewModel = viewModel(
                    factory = HistoryViewModel.provideFactory(
                        getHistory = container.getHistoryUseCase,
                        clearHistory = container.clearHistoryUseCase,
                        safHelper = container.safHelper
                    )
                )
                HistoryScreen(viewModel = historyViewModel)
            }

            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.provideFactory(
                        getSettings = container.getSettingsUseCase,
                        updateSettings = container.updateSettingsUseCase
                    )
                )
                SettingsScreen(viewModel = settingsViewModel)
            }

            composable(Screen.Merge.route) {
                val mergeViewModel: MergeViewModel = viewModel(
                    factory = MergeViewModel.provideFactory(
                        mergeUseCase = container.mergePdfsUseCase,
                        safHelper = container.safHelper
                    )
                )
                MergeScreen(
                    viewModel = mergeViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ImagesToPdf.route) {
                val imagesToPdfViewModel: ImagesToPdfViewModel = viewModel(
                    factory = ImagesToPdfViewModel.provideFactory(
                        useCase = container.imagesToPdfUseCase,
                        safHelper = container.safHelper
                    )
                )
                ImagesToPdfScreen(
                    viewModel = imagesToPdfViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Split.route) {
                val splitViewModel: SplitViewModel = viewModel(
                    factory = SplitViewModel.provideFactory(
                        useCase = container.splitPdfUseCase,
                        safHelper = container.safHelper
                    )
                )
                SplitScreen(
                    viewModel = splitViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
