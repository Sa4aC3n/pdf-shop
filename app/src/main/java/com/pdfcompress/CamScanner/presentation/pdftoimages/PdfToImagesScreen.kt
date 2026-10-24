package com.pdfcompress.CamScanner.presentation.pdftoimages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pdfcompress.CamScanner.core.ui.components.EmptyStateView
import com.pdfcompress.CamScanner.core.ui.components.ErrorBanner
import com.pdfcompress.CamScanner.core.ui.components.LoadingDialog
import com.pdfcompress.CamScanner.core.ui.components.ModernBottomBarContainer
import com.pdfcompress.CamScanner.core.ui.components.ModernGradientButton
import com.pdfcompress.CamScanner.core.ui.components.ModernSoftButton
import com.pdfcompress.CamScanner.core.ui.components.OutputFileActionsRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToImagesScreen(
    viewModel: PdfToImagesViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setSourceFile(uri)
        }
    }

    val savePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            if (uiState.selectedPages.size == 1) "image/jpeg" else "application/zip"
        )
    ) { destinationUri: Uri? ->
        if (destinationUri != null) {
            viewModel.startExport(destinationUri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF إلى صور") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "الرجوع"
                        )
                    }
                }
            )
        },
        bottomBar = {
            ModernBottomBarContainer {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModernSoftButton(
                        text = if (uiState.sourceUri == null) "اختيار مستند" else "تغيير المستند",
                        icon = Icons.Default.FileOpen,
                        onClick = { filePickerLauncher.launch(arrayOf("application/pdf")) },
                        enabled = !uiState.isProcessing,
                        modifier = Modifier.weight(1f)
                    )

                    ModernGradientButton(
                        text = "تصدير (${uiState.selectedPages.size})",
                        icon = Icons.Default.Image,
                        onClick = {
                            val defaultName = if (uiState.selectedPages.size == 1) {
                                "page_${uiState.selectedPages.first()}_${System.currentTimeMillis()}.jpg"
                            } else {
                                "exported_pages_${System.currentTimeMillis()}.zip"
                            }
                            savePickerLauncher.launch(defaultName)
                        },
                        enabled = uiState.selectedPages.isNotEmpty() && !uiState.isProcessing,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            uiState.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                ErrorBanner(
                    errorMessage = error,
                    onRetry = { viewModel.retryLastExport() },
                    onDismiss = { viewModel.dismissError() }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (uiState.sourceUri == null) {
                EmptyStateView(
                    title = "اختر ملف PDF لتصدير صفحاته إلى صور",
                    description = "يحول محرك النظام صفحات المستند إلى صور عالية الدقة (JPG)، وإذا تم اختيار صفحات متعددة يتم تجميعها في أرشيف ZIP منظم.",
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Image
                )
            } else if (uiState.isReadingPages) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("جارٍ قراءة صفحات المستند…")
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.sourceName ?: "",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "إجمالي الصفحات: ${uiState.totalPages} | المحددة للتصدير: ${uiState.selectedPages.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { viewModel.selectAll() }) {
                            Text("تحديد الكل")
                        }
                        TextButton(onClick = { viewModel.clearSelection() }) {
                            Text("إلغاء")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 72.dp),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.totalPages) { index ->
                        val pageNum = index + 1
                        val isSelected = uiState.selectedPages.contains(pageNum)

                        Box(
                            modifier = Modifier
                                .aspectRatio(0.8f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable { viewModel.togglePage(pageNum) }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "$pageNum",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "JPG",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(18.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.isProcessing) {
        LoadingDialog(
            title = "جارٍ تصدير الصفحات كصور محلياً…",
            message = uiState.progressMessage,
            progressPercent = uiState.progressPercent,
            onCancel = { viewModel.cancelProcessing() }
        )
    }

    uiState.successResult?.let { result ->
        val sizeStr = if (result.outputSizeBytes > 1024 * 1024) {
            String.format("%.2f MB", result.outputSizeBytes / (1024f * 1024f))
        } else {
            String.format("%.1f KB", result.outputSizeBytes / 1024f)
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
            title = { Text("تم التصدير بنجاح!") },
            text = {
                Column {
                    Text(
                        text = uiState.outputName ?: "الملف",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("عدد الصفحات المصدّرة: ${result.totalExportedPages}")
                    Text("حجم الملف: $sizeStr")
                    if (result.isZipArchive) {
                        Text(
                            text = "تم تجميع الصفحات في أرشيف ZIP عالي الجودة.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "تم حفظ الملف في الوجهة المختارة محلياً.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutputFileActionsRow(
                        outputUri = uiState.outputUri,
                        defaultMimeType = if (result.isZipArchive) "application/zip" else "image/jpeg"
                    )
                }
            },
            confirmButton = {
                ModernGradientButton(
                    text = "العودة للرئيسية",
                    onClick = {
                        viewModel.dismissSuccess()
                        onNavigateBack()
                    },
                    height = 44.dp
                )
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissSuccess() }) {
                    Text("إغلاق")
                }
            }
        )
    }
}
