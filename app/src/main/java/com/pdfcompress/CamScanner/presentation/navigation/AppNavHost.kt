package com.pdfcompress.CamScanner.presentation.navigation

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
import com.pdfcompress.CamScanner.AppContainer
import com.pdfcompress.CamScanner.presentation.compress.CompressScreen
import com.pdfcompress.CamScanner.presentation.compress.CompressViewModel
import com.pdfcompress.CamScanner.presentation.history.HistoryScreen
import com.pdfcompress.CamScanner.presentation.history.HistoryViewModel
import com.pdfcompress.CamScanner.presentation.home.HomeScreen
import com.pdfcompress.CamScanner.presentation.imagestopdf.ImagesToPdfScreen
import com.pdfcompress.CamScanner.presentation.imagestopdf.ImagesToPdfViewModel
import com.pdfcompress.CamScanner.presentation.merge.MergeScreen
import com.pdfcompress.CamScanner.presentation.merge.MergeViewModel
import com.pdfcompress.CamScanner.presentation.settings.SettingsScreen
import com.pdfcompress.CamScanner.presentation.settings.SettingsViewModel
import com.pdfcompress.CamScanner.presentation.split.SplitScreen
import com.pdfcompress.CamScanner.presentation.split.SplitViewModel
import com.pdfcompress.CamScanner.presentation.watermark.WatermarkScreen
import com.pdfcompress.CamScanner.presentation.watermark.WatermarkViewModel

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
                    onNavigateToSplit = { navController.navigate(Screen.Split.route) },
                    onNavigateToCompress = { navController.navigate(Screen.Compress.route) },
                    onNavigateToWatermark = { navController.navigate(Screen.Watermark.route) }
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

            composable(Screen.Compress.route) {
                val compressViewModel: CompressViewModel = viewModel(
                    factory = CompressViewModel.provideFactory(
                        useCase = container.compressPdfUseCase,
                        safHelper = container.safHelper
                    )
                )
                CompressScreen(
                    viewModel = compressViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Watermark.route) {
                val watermarkViewModel: WatermarkViewModel = viewModel(
                    factory = WatermarkViewModel.provideFactory(
                        useCase = container.addWatermarkUseCase,
                        safHelper = container.safHelper
                    )
                )
                WatermarkScreen(
                    viewModel = watermarkViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
