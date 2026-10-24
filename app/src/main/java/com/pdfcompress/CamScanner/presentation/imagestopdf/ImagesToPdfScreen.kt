package com.pdfcompress.CamScanner.presentation.imagestopdf

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagesToPdfScreen(
    viewModel: ImagesToPdfViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addImages(uris)
        }
    }

    val savePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { destinationUri: Uri? ->
        if (destinationUri != null) {
            viewModel.startConversion(destinationUri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("صور إلى PDF") },
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
                        text = "إضافة صور",
                        icon = Icons.Default.AddPhotoAlternate,
                        onClick = { imagePickerLauncher.launch("image/*") },
                        enabled = !uiState.isProcessing,
                        modifier = Modifier.weight(1f)
                    )

                    ModernGradientButton(
                        text = "إنشاء PDF",
                        icon = Icons.Default.PictureAsPdf,
                        onClick = {
                            savePickerLauncher.launch("images_doc_${System.currentTimeMillis()}.pdf")
                        },
                        enabled = uiState.selectedImages.isNotEmpty() && !uiState.isProcessing,
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
                    onRetry = if (uiState.lastDestinationUri != null && uiState.selectedImages.isNotEmpty()) {
                        { viewModel.retryLastConversion() }
                    } else null,
                    onDismiss = { viewModel.dismissError() }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (uiState.selectedImages.isEmpty()) {
                EmptyStateView(
                    title = "لم يتم اختيار أي صور بعد",
                    description = "اختر صورة واحدة أو أكثر من معرض الصور لتحويلها إلى صفحات داخل مستند PDF بدقة عالية.",
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Image
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "الصور المختارة (${uiState.selectedImages.size}) — رتب الصفحات حسب الترتيب المطلوب:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(uiState.selectedImages) { index, item ->
                        SelectedImageCard(
                            index = index,
                            totalCount = uiState.selectedImages.size,
                            item = item,
                            onMoveUp = { viewModel.moveUp(index) },
                            onMoveDown = { viewModel.moveDown(index) },
                            onRemove = { viewModel.removeImage(index) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.isProcessing) {
        LoadingDialog(
            title = "جارٍ إنشاء مستند الـ PDF محلياً…",
            message = uiState.progressMessage,
            progressPercent = uiState.progressPercent,
            onCancel = { viewModel.cancelProcessing() }
        )
    }

    uiState.successInfo?.let { success ->
        val formattedSize = if (success.outputSizeBytes > 1024 * 1024) {
            String.format("%.2f ميجابايت", success.outputSizeBytes / (1024f * 1024f))
        } else {
            String.format("%.1f كيلوبايت", success.outputSizeBytes / 1024f)
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
            title = { Text("تم إنشاء المستند بنجاح!") },
            text = {
                Column {
                    Text("تم حفظ ملف PDF الجديد بنجاح:")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = success.outputName,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("حجم الملف الناتج: $formattedSize")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "تم تسجيل العملية في السجل، مع الحفاظ على صورك الأصلية بالكامل.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    com.pdfcompress.CamScanner.core.ui.components.OutputFileActionsRow(
                        outputUri = success.outputUri
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

@Composable
fun SelectedImageCard(
    index: Int,
    totalCount: Int,
    item: SelectedImageItem,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (item.sizeBytes > 0) {
                    val sizeStr = if (item.sizeBytes > 1024 * 1024) {
                        String.format("%.2f MB", item.sizeBytes / (1024f * 1024f))
                    } else {
                        String.format("%.1f KB", item.sizeBytes / 1024f)
                    }
                    Text(
                        text = sizeStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMoveUp, enabled = index > 0) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "تحريك لأعلى",
                        tint = if (index > 0) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f)
                    )
                }
                IconButton(onClick = onMoveDown, enabled = index < totalCount - 1) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "تحريك لأسفل",
                        tint = if (index < totalCount - 1) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f)
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
