package com.pdfcompress.CamScanner.presentation.navigation

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.padding

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.History

import androidx.compose.material.icons.filled.Home

import androidx.compose.material.icons.filled.Settings

import androidx.compose.material.icons.outlined.History

import androidx.compose.material.icons.outlined.Home

import androidx.compose.material.icons.outlined.Settings

import androidx.compose.material3.HorizontalDivider

import androidx.compose.material3.Icon

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.NavigationBar

import androidx.compose.material3.NavigationBarItem

import androidx.compose.material3.NavigationBarItemDefaults

import androidx.compose.material3.Scaffold

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.runtime.collectAsState

import androidx.compose.runtime.getValue

import androidx.compose.runtime.rememberCoroutineScope

import androidx.compose.ui.Modifier

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.unit.dp

import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.navigation.NavGraph.Companion.findStartDestination

import androidx.navigation.NavHostController

import androidx.navigation.compose.NavHost

import androidx.navigation.compose.composable

import androidx.navigation.compose.currentBackStackEntryAsState

import androidx.navigation.compose.rememberNavController

import com.pdfcompress.CamScanner.AppContainer

import com.pdfcompress.CamScanner.domain.model.UserSettings

import com.pdfcompress.CamScanner.presentation.compress.CompressScreen

import com.pdfcompress.CamScanner.presentation.compress.CompressViewModel

import com.pdfcompress.CamScanner.presentation.extracttext.ExtractTextScreen

import com.pdfcompress.CamScanner.presentation.extracttext.ExtractTextViewModel

import com.pdfcompress.CamScanner.presentation.history.HistoryScreen

import com.pdfcompress.CamScanner.presentation.history.HistoryViewModel

import com.pdfcompress.CamScanner.presentation.home.HomeScreen

import com.pdfcompress.CamScanner.presentation.imagestopdf.ImagesToPdfScreen

import com.pdfcompress.CamScanner.presentation.imagestopdf.ImagesToPdfViewModel

import com.pdfcompress.CamScanner.presentation.merge.MergeScreen

import com.pdfcompress.CamScanner.presentation.merge.MergeViewModel

import com.pdfcompress.CamScanner.presentation.ocr.OcrScreen

import com.pdfcompress.CamScanner.presentation.ocr.OcrViewModel

import com.pdfcompress.CamScanner.presentation.idcard.IdCardScreen

import com.pdfcompress.CamScanner.presentation.idcard.IdCardViewModel

import com.pdfcompress.CamScanner.presentation.passport.PassportScreen

import com.pdfcompress.CamScanner.presentation.passport.PassportViewModel

import com.pdfcompress.CamScanner.presentation.backup.BackupRestoreScreen

import com.pdfcompress.CamScanner.presentation.backup.BackupRestoreViewModel

import com.pdfcompress.CamScanner.presentation.pdftoimages.PdfToImagesScreen

import com.pdfcompress.CamScanner.presentation.pdftoimages.PdfToImagesViewModel

import com.pdfcompress.CamScanner.presentation.repair.RepairScreen

import com.pdfcompress.CamScanner.presentation.repair.RepairViewModel

import com.pdfcompress.CamScanner.presentation.scanner.ScannerScreen

import com.pdfcompress.CamScanner.presentation.scanner.ScannerViewModel

import com.pdfcompress.CamScanner.presentation.settings.SettingsScreen

import com.pdfcompress.CamScanner.presentation.settings.SettingsViewModel

import com.pdfcompress.CamScanner.presentation.split.SplitScreen

import com.pdfcompress.CamScanner.presentation.split.SplitViewModel

import com.pdfcompress.CamScanner.presentation.watermark.WatermarkScreen

import com.pdfcompress.CamScanner.presentation.watermark.WatermarkViewModel
import com.pdfcompress.CamScanner.presentation.rotate.RotateScreen
import com.pdfcompress.CamScanner.presentation.rotate.RotateViewModel
import com.pdfcompress.CamScanner.presentation.protect.ProtectScreen
import com.pdfcompress.CamScanner.presentation.protect.ProtectViewModel
import com.pdfcompress.CamScanner.presentation.unlock.UnlockScreen
import com.pdfcompress.CamScanner.presentation.unlock.UnlockViewModel
import com.pdfcompress.CamScanner.presentation.metadata.MetadataScreen
import com.pdfcompress.CamScanner.presentation.metadata.MetadataViewModel
import com.pdfcompress.CamScanner.presentation.flatten.FlattenScreen
import com.pdfcompress.CamScanner.presentation.flatten.FlattenViewModel

import kotlinx.coroutines.launch

@Composable

fun AppNavHost(

    container: AppContainer,

    modifier: Modifier = Modifier,

    navController: NavHostController = rememberNavController()

) {

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val currentRoute = navBackStackEntry?.destination?.route

    val settings by container.settingsRepository.settings.collectAsState(initial = UserSettings())

    val coroutineScope = rememberCoroutineScope()

    val bottomNavRoutes = listOf(

        Screen.Home.route,

        Screen.History.route,

        Screen.Settings.route

    )

    val showBottomBar = currentRoute in bottomNavRoutes

    val navItemColors = NavigationBarItemDefaults.colors(

        selectedIconColor = MaterialTheme.colorScheme.primary,

        selectedTextColor = MaterialTheme.colorScheme.primary,

        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),

        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,

        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    )

    Scaffold(

        bottomBar = {

            if (showBottomBar) {

                Column {

                    HorizontalDivider(

                        thickness = 1.dp,

                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

                    )

                    NavigationBar(

                        containerColor = MaterialTheme.colorScheme.surface,

                        tonalElevation = 0.dp

                    ) {

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

                            label = {

                                Text(

                                    text = "الرئيسية",

                                    fontWeight = if (currentRoute == Screen.Home.route) FontWeight.Bold else FontWeight.Medium

                                )

                            },

                            colors = navItemColors

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

                            label = {

                                Text(

                                    text = "السجل",

                                    fontWeight = if (currentRoute == Screen.History.route) FontWeight.Bold else FontWeight.Medium

                                )

                            },

                            colors = navItemColors

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

                            label = {

                                Text(

                                    text = "الإعدادات",

                                    fontWeight = if (currentRoute == Screen.Settings.route) FontWeight.Bold else FontWeight.Medium

                                )

                            },

                            colors = navItemColors

                        )

                    }

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

                    currentSettings = settings,

                    onSelectColorTheme = { colorTheme ->

                        coroutineScope.launch {

                            container.updateSettingsUseCase.setColorTheme(colorTheme)

                        }

                    },

                    onSelectThemeMode = { themeMode ->

                        coroutineScope.launch {

                            container.updateSettingsUseCase.setTheme(themeMode)

                        }

                    },

                    onNavigateToMerge = { navController.navigate(Screen.Merge.route) },

                    onNavigateToImagesToPdf = { navController.navigate(Screen.ImagesToPdf.route) },

                    onNavigateToSplit = { navController.navigate(Screen.Split.route) },

                    onNavigateToCompress = { navController.navigate(Screen.Compress.route) },

                    onNavigateToWatermark = { navController.navigate(Screen.Watermark.route) },

                    onNavigateToPdfToImages = { navController.navigate(Screen.PdfToImages.route) },

                    onNavigateToExtractText = { navController.navigate(Screen.ExtractText.route) },

                    onNavigateToRepair = { navController.navigate(Screen.Repair.route) },

                    onNavigateToScanner = { navController.navigate(Screen.Scanner.route) },

                    onNavigateToOcr = { navController.navigate(Screen.Ocr.route) },

                    onNavigateToIdCard = { navController.navigate(Screen.IdCardScanner.route) },

                    onNavigateToPassport = { navController.navigate(Screen.PassportScanner.route) },

                    onNavigateToBackupRestore = { navController.navigate(Screen.BackupRestore.route) },
                    onNavigateToRotate = { navController.navigate(Screen.Rotate.route) },
                    onNavigateToProtect = { navController.navigate(Screen.Protect.route) },
                    onNavigateToUnlock = { navController.navigate(Screen.Unlock.route) },
                    onNavigateToMetadata = { navController.navigate(Screen.Metadata.route) },
                    onNavigateToFlatten = { navController.navigate(Screen.Flatten.route) },

                    onSelectLanguage = { lang ->

                        coroutineScope.launch {

                            container.updateSettingsUseCase.setLanguage(lang)

                        }

                    }

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

                SettingsScreen(

                    viewModel = settingsViewModel,

                    onNavigateToBackupRestore = { navController.navigate(Screen.BackupRestore.route) }

                )

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

            composable(Screen.PdfToImages.route) {

                val pdfToImagesViewModel: PdfToImagesViewModel = viewModel(

                    factory = PdfToImagesViewModel.provideFactory(

                        useCase = container.pdfToImagesUseCase,

                        safHelper = container.safHelper

                    )

                )

                PdfToImagesScreen(

                    viewModel = pdfToImagesViewModel,

                    onNavigateBack = { navController.popBackStack() }

                )

            }

            composable(Screen.ExtractText.route) {

                val context = LocalContext.current

                val extractTextViewModel: ExtractTextViewModel = viewModel(

                    factory = ExtractTextViewModel.provideFactory(

                        useCase = container.extractTextUseCase,

                        safHelper = container.safHelper,

                        context = context

                    )

                )

                ExtractTextScreen(

                    viewModel = extractTextViewModel,

                    onNavigateBack = { navController.popBackStack() }

                )

            }

            composable(Screen.Repair.route) {

                val repairViewModel: RepairViewModel = viewModel(

                    factory = RepairViewModel.provideFactory(

                        useCase = container.repairPdfUseCase,

                        safHelper = container.safHelper

                    )

                )

                RepairScreen(

                    viewModel = repairViewModel,

                    onNavigateBack = { navController.popBackStack() }

                )

            }

            composable(Screen.Scanner.route) {

                val context = LocalContext.current

                val scannerViewModel: ScannerViewModel = viewModel(

                    factory = ScannerViewModel.provideFactory(

                        scanDocumentUseCase = container.scanDocumentUseCase,

                        safHelper = container.safHelper,

                        tempFileManager = container.tempFileManager,

                        context = context

                    )

                )

                ScannerScreen(

                    viewModel = scannerViewModel,

                    onNavigateBack = { navController.popBackStack() }

                )

            }

            composable(Screen.Ocr.route) {

                val context = LocalContext.current

                val ocrViewModel: OcrViewModel = viewModel(

                    factory = OcrViewModel.provideFactory(

                        ocrUseCase = container.ocrUseCase,

                        safHelper = container.safHelper,

                        tempFileManager = container.tempFileManager,

                        context = context

                    )

                )

                OcrScreen(

                    viewModel = ocrViewModel,

                    onNavigateBack = { navController.popBackStack() }

                )

            }

            composable(Screen.IdCardScanner.route) {

                val context = LocalContext.current

                val idCardViewModel: IdCardViewModel = viewModel(

                    factory = IdCardViewModel.provideFactory(

                        context = context,

                        idCardUseCase = container.idCardUseCase,

                        safHelper = container.safHelper,

                        tempFileManager = container.tempFileManager

                    )

                )

                IdCardScreen(

                    viewModel = idCardViewModel,

                    onNavigateBack = { navController.popBackStack() }

                )

            }

            composable(Screen.PassportScanner.route) {

                val context = LocalContext.current

                val passportViewModel: PassportViewModel = viewModel(

                    factory = PassportViewModel.provideFactory(

                        context = context,

                        passportMrzUseCase = container.passportMrzUseCase,

                        safHelper = container.safHelper

                    )

                )

                PassportScreen(

                    viewModel = passportViewModel,

                    onNavigateBack = { navController.popBackStack() }

                )

            }

            composable(Screen.BackupRestore.route) {

                val context = LocalContext.current

                val backupRestoreViewModel: BackupRestoreViewModel = viewModel(

                    factory = BackupRestoreViewModel.provideFactory(

                        context = context,

                        backupRestoreUseCase = container.backupRestoreUseCase,

                        safHelper = container.safHelper

                    )

                )

                BackupRestoreScreen(

                    viewModel = backupRestoreViewModel,

                    onNavigateBack = { navController.popBackStack() }

                )

            }

            composable(Screen.Rotate.route) {
                val rotateViewModel: RotateViewModel = viewModel(
                    factory = RotateViewModel.Factory(
                        rotateUseCase = container.rotatePdfUseCase,
                        safHelper = container.safHelper
                    )
                )
                RotateScreen(
                    viewModel = rotateViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Protect.route) {
                val protectViewModel: ProtectViewModel = viewModel(
                    factory = ProtectViewModel.Factory(
                        protectUseCase = container.protectPdfUseCase,
                        safHelper = container.safHelper
                    )
                )
                ProtectScreen(
                    viewModel = protectViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Unlock.route) {
                val unlockViewModel: UnlockViewModel = viewModel(
                    factory = UnlockViewModel.Factory(
                        unlockUseCase = container.unlockPdfUseCase,
                        safHelper = container.safHelper
                    )
                )
                UnlockScreen(
                    viewModel = unlockViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Metadata.route) {
                val metadataViewModel: MetadataViewModel = viewModel(
                    factory = MetadataViewModel.Factory(
                        metadataUseCase = container.pdfMetadataUseCase,
                        safHelper = container.safHelper
                    )
                )
                MetadataScreen(
                    viewModel = metadataViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Flatten.route) {
                val flattenViewModel: FlattenViewModel = viewModel(
                    factory = FlattenViewModel.Factory(
                        flattenUseCase = container.flattenPdfUseCase,
                        safHelper = container.safHelper
                    )
                )
                FlattenScreen(
                    viewModel = flattenViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

