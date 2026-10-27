package com.pdfcompress.CamScanner.presentation.history

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.WaterDamage
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Assignment
import com.pdfcompress.CamScanner.core.ui.components.EmptyStateView
import com.pdfcompress.CamScanner.core.ui.components.ModernGradientButton
import com.pdfcompress.CamScanner.core.ui.components.ModernSoftButton
import com.pdfcompress.CamScanner.core.ui.components.openOutputUri
import com.pdfcompress.CamScanner.core.ui.components.shareOutputUri
import com.pdfcompress.CamScanner.domain.model.OperationStatus
import com.pdfcompress.CamScanner.domain.model.ToolType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("سجل العمليات") },
                actions = {
                    if (uiState.items.isNotEmpty()) {
                        IconButton(onClick = { viewModel.showClearConfirm(true) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "مسح السجل"
                            )
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.items.isEmpty()) {
                EmptyStateView(
                    title = "لا توجد عمليات سابقة",
                    description = "الملفات المعالجة ستظهر هنا كبيانات وصفية للرجوع إليها سريعاً دون استهلاك مساحة تخزينية إضافية.",
                    modifier = Modifier.align(Alignment.Center),
                    icon = Icons.Default.History
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.items) { item ->
                        HistoryItemCard(item = item)
                    }
                }
            }
        }
    }

    if (uiState.showClearDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showClearConfirm(false) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = { Text("مسح سجل العمليات؟") },
            text = {
                Text(
                    "سيتم حذف المراجع والبيانات الوصفية المسجلة فقط، ولن تتأثر ملفاتك الأصلية أو الناتجة المحفوظة على جهازك."
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearAllHistory() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("مسح السجل")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showClearConfirm(false) }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun HistoryItemCard(item: HistoryItemUi) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val record = item.record
    val dateFormat = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(record.completedAt))

    val (toolTitle, toolIcon) = when (record.toolType) {
        ToolType.MERGE -> "دمج PDF" to Icons.AutoMirrored.Filled.MergeType
        ToolType.IMAGES_TO_PDF -> "صور إلى PDF" to Icons.Default.Image
        ToolType.SPLIT -> "استخراج صفحات" to Icons.AutoMirrored.Filled.CallSplit
        ToolType.COMPRESS -> "ضغط PDF" to Icons.Default.Compress
        ToolType.WATERMARK -> "علامة مائية" to Icons.Default.WaterDamage
        ToolType.PDF_TO_IMAGES -> "PDF إلى صور" to Icons.Default.PhotoLibrary
        ToolType.EXTRACT_TEXT -> "استخراج نصوص" to Icons.Default.Description
        ToolType.REPAIR -> "إصلاح PDF" to Icons.Default.Build
        ToolType.SCANNER -> "مسح مستند" to Icons.Default.CameraAlt
        ToolType.OCR -> "تعرف ضوئي OCR" to Icons.Default.FindInPage
        ToolType.ID_CARD -> "سحب البطاقة" to Icons.Default.Badge
        ToolType.PASSPORT_MRZ -> "جواز السفر" to Icons.Default.QrCodeScanner
        ToolType.BACKUP_RESTORE -> "نسخ احتياطي" to Icons.Default.Security
        ToolType.ROTATE -> "تدوير الصفحات" to Icons.AutoMirrored.Filled.RotateRight
        ToolType.PROTECT -> "حماية وتشفير" to Icons.Default.Lock
        ToolType.UNLOCK -> "إزالة الحماية" to Icons.Default.LockOpen
        ToolType.METADATA -> "البيانات الوصفية" to Icons.Default.Info
        ToolType.FLATTEN -> "تسطيح PDF" to Icons.Default.LayersClear
        ToolType.ORGANIZE_PAGES -> "تنظيم الصفحات" to Icons.Default.GridView
        ToolType.PAGE_NUMBERS -> "أرقام الصفحات" to Icons.Default.FormatListNumbered
        ToolType.GRAYSCALE -> "أبيض وأسود" to Icons.Default.FormatPaint
        ToolType.CROP -> "قص الهوامش" to Icons.Default.Crop
        ToolType.REDACT -> "حجب البيانات" to Icons.Default.VisibilityOff
        ToolType.TXT_TO_PDF -> "نص إلى PDF" to Icons.Default.Description
        ToolType.MARKDOWN_TO_PDF -> "ماركداون إلى PDF" to Icons.Default.Description
        ToolType.HTML_TO_PDF -> "HTML إلى PDF" to Icons.Default.Description
        ToolType.PDF_TO_MARKDOWN -> "PDF إلى ماركداون" to Icons.Default.Description
        ToolType.RTF_TO_PDF -> "RTF إلى PDF" to Icons.Default.Description
        ToolType.CONVERT_IMAGE -> "تحويل صيغة الصورة" to Icons.Default.PhotoLibrary
        ToolType.COMPRESS_IMAGE -> "ضغط وتصغير صورة" to Icons.Default.Compress
        ToolType.COMPARE_PDF -> "مقارنة ملفات PDF" to Icons.Default.CompareArrows
        ToolType.SIGN_PDF -> "التوقيع الإلكتروني" to Icons.Default.Draw
        ToolType.PDF_FORMS -> "تعبئة نموذج PDF" to Icons.Default.Assignment
        else -> "أداة PDF" to Icons.Default.Description
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = toolIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = toolTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (record.status) {
                        OperationStatus.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
                        OperationStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                        OperationStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = when (record.status) {
                            OperationStatus.SUCCESS -> "ناجحة"
                            OperationStatus.FAILED -> "فشلت"
                            OperationStatus.CANCELLED -> "ملغاة"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (record.outputDocumentName != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "الملف الناتج: ${record.outputDocumentName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    if (!item.isFileAccessible) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "غير متاح (نُقل أو حُذف)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (item.isFileAccessible && record.outputDocumentUri != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ModernGradientButton(
                            text = "مشاركة الملف",
                            icon = Icons.Default.Share,
                            onClick = {
                                val uri = android.net.Uri.parse(record.outputDocumentUri)
                                shareOutputUri(context, uri)
                            },
                            height = 42.dp,
                            modifier = Modifier.weight(1f)
                        )
                        ModernSoftButton(
                            text = "فتح الملف",
                            icon = Icons.AutoMirrored.Filled.OpenInNew,
                            onClick = {
                                val uri = android.net.Uri.parse(record.outputDocumentUri)
                                openOutputUri(context, uri)
                            },
                            height = 42.dp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            val outputSize = record.outputSizeBytes
            if (outputSize != null && outputSize > 0) {
                val sizeStr = if (outputSize > 1024 * 1024) {
                    String.format("%.2f ميجابايت", outputSize / (1024f * 1024f))
                } else {
                    String.format("%.1f كيلوبايت", outputSize / 1024f)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "الحجم: $sizeStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val summaryText = record.summary
            if (summaryText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val errorMsg = record.errorMessage
            if (errorMsg != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "الخطأ: $errorMsg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
