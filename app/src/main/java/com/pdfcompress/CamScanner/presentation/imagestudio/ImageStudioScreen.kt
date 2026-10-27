package com.pdfcompress.CamScanner.presentation.imagestudio

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoSizeSelectActual
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pdfcompress.CamScanner.core.ui.components.OutputFileActionsRow
import com.pdfcompress.CamScanner.domain.model.ImageOutputFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageStudioScreen(
    viewModel: ImageStudioViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.setSourceImage(it) }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(state.selectedFormat.mimeType)
    ) { uri: Uri? ->
        uri?.let { viewModel.processImage(it) }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("استوديو الصور الشامل", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Tabs
            TabRow(selectedTabIndex = if (state.isCompressMode) 1 else 0) {
                Tab(
                    selected = !state.isCompressMode,
                    onClick = { viewModel.setMode(false) },
                    text = { Text("تحويل الصيغ") },
                    icon = { Icon(Icons.Default.Transform, contentDescription = null) }
                )
                Tab(
                    selected = state.isCompressMode,
                    onClick = { viewModel.setMode(true) },
                    text = { Text("ضغط وتصغير الحجم") },
                    icon = { Icon(Icons.Default.Compress, contentDescription = null) }
                )
            }

            // Image Picker Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (state.sourceUri == null) {
                        Text(
                            text = "اختر صورة من جهازك (PNG, JPEG, WEBP, SVG, HEIC/HEIF)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch(arrayOf("image/*", "application/octet-stream")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("اختيار صورة")
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = state.sourceName ?: "صورة مختارة",
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "الحجم الأصلي: ${formatFileSize(state.sourceSizeBytes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch(arrayOf("image/*", "application/octet-stream")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("تغيير الصورة")
                        }
                    }
                }
            }

            if (state.sourceUri != null) {
                // Format Selection
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("الصيغة الناتجة:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ImageOutputFormat.values().forEach { fmt ->
                                FilterChip(
                                    selected = state.selectedFormat == fmt,
                                    onClick = { viewModel.setFormat(fmt) },
                                    label = { Text(fmt.name) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.selectedFormat.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Compress / Resize Sliders
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("جودة الصورة (Quality):", fontWeight = FontWeight.Bold)
                            Text("${state.qualityPercent}%", fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = state.qualityPercent.toFloat(),
                            onValueChange = { viewModel.setQuality(it.toInt()) },
                            valueRange = 10f..100f,
                            steps = 17
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("أبعاد الصورة (Scale / Resize):", fontWeight = FontWeight.Bold)
                            Text("${state.scalePercent}%", fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = state.scalePercent.toFloat(),
                            onValueChange = { viewModel.setScale(it.toInt()) },
                            valueRange = 10f..100f,
                            steps = 8
                        )
                    }
                }

                // Process Button
                Button(
                    onClick = {
                        val base = state.sourceName?.substringBeforeLast('.') ?: "processed_image"
                        val prefix = if (state.isCompressMode) "compressed_" else "converted_"
                        val defaultName = "$prefix$base.${state.selectedFormat.extension}"
                        createDocumentLauncher.launch(defaultName)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !state.isProcessing
                ) {
                    if (state.isProcessing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("جاري المعالجة بدون إنترنت…")
                    } else {
                        val icon = if (state.isCompressMode) Icons.Default.Compress else Icons.Default.Transform
                        val label = if (state.isCompressMode) "ضغط وتصغير الصورة الآن" else "تحويل وحفظ الصورة الآن"
                        Icon(icon, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Progress Indicator
            if (state.isProcessing) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = state.progressMessage ?: "جاري التنفيذ…",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (state.progressPercent != null) {
                            LinearProgressIndicator(
                                progress = { (state.progressPercent ?: 0) / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            // Success Card
            if (state.successOutputName != null && state.outputUri != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تمت العملية بنجاح بدون إنترنت!",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "الملف: ${state.successOutputName}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "الحجم الجديد: ${formatFileSize(state.successSizeBytes)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (state.sourceSizeBytes > 0 && state.successSizeBytes > 0 && state.isCompressMode) {
                            val saved = ((state.sourceSizeBytes - state.successSizeBytes).toDouble() / state.sourceSizeBytes * 100).toInt()
                            if (saved > 0) {
                                Text(
                                    text = "تم توفير: $saved% من الحجم الأصلي!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutputFileActionsRow(
                            outputUri = state.outputUri,
                            defaultMimeType = state.selectedFormat.mimeType
                        )
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format("%.2f MB", mb)
    } else {
        String.format("%.1f KB", kb)
    }
}
