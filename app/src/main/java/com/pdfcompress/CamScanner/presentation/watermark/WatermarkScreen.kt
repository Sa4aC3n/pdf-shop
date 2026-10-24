package com.pdfcompress.CamScanner.presentation.watermark

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.WaterDamage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdfcompress.CamScanner.core.ui.components.EmptyStateView
import com.pdfcompress.CamScanner.core.ui.components.ErrorBanner
import com.pdfcompress.CamScanner.core.ui.components.LoadingDialog
import com.pdfcompress.CamScanner.core.ui.components.ModernBottomBarContainer
import com.pdfcompress.CamScanner.core.ui.components.ModernGradientButton
import com.pdfcompress.CamScanner.core.ui.components.ModernSoftButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatermarkScreen(
    viewModel: WatermarkViewModel,
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
            viewModel.startWatermarking(destinationUri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("علامة مائية") },
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
                        text = "تطبيق وحفظ",
                        icon = Icons.Default.WaterDamage,
                        onClick = {
                            savePickerLauncher.launch("watermarked_${System.currentTimeMillis()}.pdf")
                        },
                        enabled = uiState.sourceUri != null && uiState.watermarkText.isNotBlank() && !uiState.isProcessing,
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            uiState.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                ErrorBanner(
                    errorMessage = error,
                    onRetry = if (uiState.lastDestinationUri != null && uiState.sourceUri != null) {
                        { viewModel.retryLastWatermarking() }
                    } else null,
                    onDismiss = { viewModel.dismissError() }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (uiState.sourceUri == null) {
                EmptyStateView(
                    title = "اختر ملف PDF لتطبيق العلامة المائية",
                    description = "أضف نص حماية مخصصاً أو ختماً شفافاً على كافة صفحات المستند لحفظ حقوق الملكية والسرية محلياً.",
                    modifier = Modifier.padding(vertical = 32.dp),
                    icon = Icons.Default.WaterDamage
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))

                // Source File Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.sourceName ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "المستند جاهز لتطبيق العلامة المائية",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Watermark Text Input
                OutlinedTextField(
                    value = uiState.watermarkText,
                    onValueChange = { viewModel.setText(it) },
                    label = { Text("نص العلامة المائية") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Preset Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf("سري للغاية", "نسخة مسودة", "خاص", "أصلي")
                    presets.forEach { preset ->
                        FilterChip(
                            selected = uiState.watermarkText == preset,
                            onClick = { viewModel.setText(preset) },
                            label = { Text(preset) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Visual Preview Card
                Text(
                    text = "معاينة افتراضية للعلامة المائية:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        repeat(5) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .height(8.dp)
                                    .background(Color.LightGray.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            )
                        }
                    }

                    Text(
                        text = uiState.watermarkText,
                        fontSize = (uiState.fontSize * 0.7f).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray.copy(alpha = uiState.opacity),
                        modifier = Modifier.rotate(uiState.rotation)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sliders
                Text(
                    text = "درجة الشفافية: ${(uiState.opacity * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = uiState.opacity,
                    onValueChange = { viewModel.setOpacity(it) },
                    valueRange = 0.1f..0.8f
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "حجم الخط: ${uiState.fontSize.toInt()} نقطة",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = uiState.fontSize,
                    onValueChange = { viewModel.setFontSize(it) },
                    valueRange = 24f..72f
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "زاوية الدوران: ${uiState.rotation.toInt()}°",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = uiState.rotation == 45f,
                        onClick = { viewModel.setRotation(45f) },
                        label = { Text("45° مائل (مستحسن)") }
                    )
                    FilterChip(
                        selected = uiState.rotation == 0f,
                        onClick = { viewModel.setRotation(0f) },
                        label = { Text("0° أفقي") }
                    )
                }
            }
        }
    }

    if (uiState.isProcessing) {
        LoadingDialog(
            title = "جارٍ تطبيق العلامة المائية محلياً…",
            message = uiState.progressMessage,
            progressPercent = uiState.progressPercent,
            onCancel = { viewModel.cancelProcessing() }
        )
    }

    uiState.successOutputName?.let { outputName ->
        val formattedSize = if (uiState.successSizeBytes > 1024 * 1024) {
            String.format("%.2f ميجابايت", uiState.successSizeBytes / (1024f * 1024f))
        } else {
            String.format("%.1f كيلوبايت", uiState.successSizeBytes / 1024f)
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
            title = { Text("تمت إضافة العلامة المائية بنجاح!") },
            text = {
                Column {
                    Text(
                        text = outputName,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("حجم الملف الناتج: $formattedSize")
                    Text("النص: «${uiState.watermarkText}»")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "تم حفظ الملف الجديد في موقعك المختار مع الحفاظ على المستند الأصلي.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    uiState.outputUri?.let { outUri ->
                        Spacer(modifier = Modifier.height(12.dp))
                        com.pdfcompress.CamScanner.core.ui.components.OutputFileActionsRow(
                            outputUri = outUri
                        )
                    }
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
