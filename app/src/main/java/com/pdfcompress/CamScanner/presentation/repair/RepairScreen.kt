package com.pdfcompress.CamScanner.presentation.repair

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileOpen
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
fun RepairScreen(
    viewModel: RepairViewModel,
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
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { destinationUri: Uri? ->
        if (destinationUri != null) {
            viewModel.startRepair(destinationUri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("محاولة إصلاح PDF") },
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
                        text = if (uiState.sourceUri == null) "اختيار مستند تالف" else "تغيير المستند",
                        icon = Icons.Default.FileOpen,
                        onClick = { filePickerLauncher.launch(arrayOf("application/pdf")) },
                        enabled = !uiState.isProcessing,
                        modifier = Modifier.weight(1f)
                    )

                    ModernGradientButton(
                        text = "إصلاح واسترداد",
                        icon = Icons.Default.Build,
                        onClick = {
                            savePickerLauncher.launch("repaired_${System.currentTimeMillis()}.pdf")
                        },
                        enabled = uiState.sourceUri != null && !uiState.isProcessing,
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
                    onRetry = { viewModel.retryLastRepair() },
                    onDismiss = { viewModel.dismissError() }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (uiState.sourceUri == null) {
                EmptyStateView(
                    title = "اختر ملف PDF تالف لمحاولة استرداده",
                    description = "يقوم المحرك بفحص البايتات وإعادة بناء جداول المراجع المتقاطعة (XRef Table) والمقطورات لاستخراج أكبر عدد ممكن من الصفحات الصالحة بصدق وشفافية.",
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Build
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = uiState.sourceName ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val formattedSize = if (uiState.sourceSizeBytes > 1024 * 1024) {
                            String.format("%.2f MB", uiState.sourceSizeBytes / (1024f * 1024f))
                        } else {
                            String.format("%.1f KB", uiState.sourceSizeBytes / 1024f)
                        }
                        Text(
                            text = "حجم الملف الحالي: $formattedSize",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ملاحظة الصدق الوظيفي:",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• الإصلاح مخصص لمعالجة تلف الفهارس وانقطاع الملفات المفاجئ.\n• الملفات المشفرة أو التالفة عشوائياً بنسبة تفوق 50% قد لا يتم استردادها كاملة.\n• سيتم تزويدك بتقرير شفاف بعدد الصفحات التي نجح استرجاعها فعلياً.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }

    if (uiState.isProcessing) {
        LoadingDialog(
            title = "جارٍ فحص وإصلاح المستند محلياً…",
            message = uiState.progressMessage,
            progressPercent = uiState.progressPercent,
            onCancel = { viewModel.cancelProcessing() }
        )
    }

    uiState.repairReport?.let { report ->
        val sizeStr = if (report.outputSizeBytes > 1024 * 1024) {
            String.format("%.2f MB", report.outputSizeBytes / (1024f * 1024f))
        } else {
            String.format("%.1f KB", report.outputSizeBytes / 1024f)
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
            title = { Text("اكتملت محاولة الإصلاح!") },
            text = {
                Column {
                    Text(
                        text = uiState.outputName ?: "الملف المسترد",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(report.diagnosisMessage)
                    Text("حجم الملف الناتج: $sizeStr")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "تم حفظ المستند المسترد كملف جديد مع بقاء الملف الأصلي كما هو.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutputFileActionsRow(outputUri = uiState.outputUri)
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
