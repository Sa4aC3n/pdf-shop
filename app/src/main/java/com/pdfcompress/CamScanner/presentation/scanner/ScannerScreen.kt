package com.pdfcompress.CamScanner.presentation.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.pdfcompress.CamScanner.core.files.DocumentImageEnhancer
import com.pdfcompress.CamScanner.core.files.ScanFilter
import com.pdfcompress.CamScanner.core.ui.components.ErrorBanner
import com.pdfcompress.CamScanner.core.ui.components.LoadingDialog
import java.io.File
import java.util.concurrent.Executor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onImageImported(uri)
        }
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.saveAsPdf(uri)
        }
    }

    Scaffold(
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!hasCameraPermission) {
                CameraPermissionView(
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onImportFromGallery = { galleryLauncher.launch(arrayOf("image/*")) },
                    onNavigateBack = onNavigateBack
                )
            } else if (uiState.isCameraMode) {
                CameraCaptureView(
                    flashMode = uiState.flashMode,
                    capturedCount = uiState.capturedPages.size,
                    onToggleFlash = { viewModel.toggleFlash() },
                    onCapture = { file -> viewModel.onPageCaptured(file) },
                    onOpenGallery = { galleryLauncher.launch(arrayOf("image/*")) },
                    onReviewClicked = { viewModel.switchToReview() },
                    onNavigateBack = onNavigateBack
                )
            } else {
                PageReviewView(
                    uiState = uiState,
                    onFilterSelected = { filter -> viewModel.setActivePageFilter(filter) },
                    onRotate = { viewModel.rotateActivePage() },
                    onDelete = { idx -> viewModel.removePage(idx) },
                    onAddMore = { viewModel.switchToCamera() },
                    onSelectPage = { idx -> viewModel.switchToReview(idx) },
                    onSavePdf = {
                        val defaultName = "scan_${System.currentTimeMillis()}.pdf"
                        savePdfLauncher.launch(defaultName)
                    },
                    onNavigateBack = {
                        if (uiState.capturedPages.isNotEmpty()) {
                            viewModel.switchToCamera()
                        } else {
                            onNavigateBack()
                        }
                    }
                )
            }

            uiState.errorMessage?.let { error ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter)
                ) {
                    ErrorBanner(
                        errorMessage = error,
                        onRetry = { viewModel.dismissError() }
                    )
                }
            }

            if (uiState.isProcessing) {
                LoadingDialog(
                    title = "جارٍ تجهيز وحفظ مستند PDF…",
                    message = uiState.progressMessage,
                    progressPercent = uiState.progressPercent
                )
            }

            uiState.successSizeBytes?.let { size ->
                val sizeFormatted = if (size > 1024 * 1024) {
                    String.format("%.2f ميجابايت", size / (1024f * 1024f))
                } else {
                    String.format("%.1f كيلوبايت", size / 1024f)
                }

                AlertDialog(
                    onDismissRequest = { viewModel.dismissSuccess() },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    },
                    title = { Text("تم مسح وحفظ المستند بنجاح!") },
                    text = {
                        Column {
                            Text(
                                text = uiState.outputName ?: "المستند",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("عدد الصفحات: ${uiState.capturedPages.size}")
                            Text("حجم الملف الناتج: $sizeFormatted")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "تمت معالجة الفلاتر محلياً وتوليد المستند بنجاح 100% بدون إنترنت.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.dismissSuccess()
                                onNavigateBack()
                            }
                        ) {
                            Text("العودة للرئيسية")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissSuccess() }) {
                            Text("إغلاق")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun CameraPermissionView(
    onRequestPermission: () -> Unit,
    onImportFromGallery: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "إذن الكاميرا مطلوب",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "يحتاج ماسح المستندات إلى صلاحية الكاميرا لالتقاط المستندات والأوراق ضوئياً مباشرة على جهازك دون اتصال بالإنترنت.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("منح إذن الكاميرا")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onImportFromGallery,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("أو استيراد صورة من المعرض")
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onNavigateBack) {
            Text("الرجوع")
        }
    }
}

@Composable
fun CameraCaptureView(
    flashMode: Int,
    capturedCount: Int,
    onToggleFlash: () -> Unit,
    onCapture: (File) -> Unit,
    onOpenGallery: () -> Unit,
    onReviewClicked: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val capture = ImageCapture.Builder()
                        .setFlashMode(flashMode)
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build()
                    imageCapture = capture

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            ctx as androidx.lifecycle.LifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            capture
                        )
                    } catch (_: Exception) {}
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Viewfinder guide
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 96.dp)
                .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
        )

        // Top controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "الرجوع",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = onToggleFlash,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                val flashIcon = when (flashMode) {
                    ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                    ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                    else -> Icons.Default.FlashOff
                }
                Icon(imageVector = flashIcon, contentDescription = "الفلاش", tint = Color.White)
            }
        }

        // Bottom controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Import from gallery
            IconButton(
                onClick = onOpenGallery,
                modifier = Modifier
                    .size(52.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "المعرض",
                    tint = Color.White
                )
            }

            // Shutter Button
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable {
                        val capture = imageCapture ?: return@clickable
                        val photoFile = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                        capture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    onCapture(photoFile)
                                }

                                override fun onError(exception: ImageCaptureException) {}
                            }
                        )
                    }
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.Red)
                )
            }

            // Captured pages badge / review button
            if (capturedCount > 0) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onReviewClicked),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$capturedCount",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(52.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageReviewView(
    uiState: ScannerUiState,
    onFilterSelected: (ScanFilter) -> Unit,
    onRotate: () -> Unit,
    onDelete: (Int) -> Unit,
    onAddMore: () -> Unit,
    onSelectPage: (Int) -> Unit,
    onSavePdf: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val activePage = uiState.capturedPages.getOrNull(uiState.activePageIndex)

    var previewBitmap by remember(activePage?.id, activePage?.rotationDegrees, activePage?.filter) {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(activePage?.id, activePage?.rotationDegrees, activePage?.filter) {
        if (activePage != null && activePage.rawFile.exists()) {
            var bmp = BitmapFactory.decodeFile(activePage.rawFile.absolutePath)
            if (activePage.rotationDegrees % 360f != 0f) {
                bmp = DocumentImageEnhancer.rotate(bmp, activePage.rotationDegrees)
            }
            if (activePage.filter != ScanFilter.ORIGINAL) {
                bmp = DocumentImageEnhancer.applyFilter(bmp, activePage.filter)
            }
            previewBitmap = bmp
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "معاينة المستند (${uiState.activePageIndex + 1}/${uiState.capturedPages.size})"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = onRotate) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.RotateRight, contentDescription = "تدوير 90°")
                    }
                    IconButton(onClick = { onDelete(uiState.activePageIndex) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف الصفحة",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                // Filter selection chips
                Text(
                    text = "فلتر المستند:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScanFilter.entries.forEach { filter ->
                        val isSelected = activePage?.filter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { onFilterSelected(filter) },
                            label = { Text(filter.titleAr) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onAddMore,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("صفحة أخرى")
                    }

                    Button(
                        onClick = onSavePdf,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تصدير كـ PDF (${uiState.capturedPages.size})")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main page preview
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    previewBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "معاينة الصفحة",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Thumbnail Carousel
            if (uiState.capturedPages.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(uiState.capturedPages) { index, page ->
                        val isCurrent = index == uiState.activePageIndex
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isCurrent) 2.dp else 1.dp,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectPage(index) }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
