package com.pdfcompress.CamScanner.presentation.passport

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.pdfcompress.CamScanner.core.ui.components.ErrorBanner
import com.pdfcompress.CamScanner.core.ui.components.LoadingDialog
import com.pdfcompress.CamScanner.core.ui.components.ModernGradientButton
import com.pdfcompress.CamScanner.core.ui.components.OutputFileActionsRow
import com.pdfcompress.CamScanner.domain.model.PassportMrzData
import com.pdfcompress.CamScanner.presentation.components.AdBanner
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassportScreen(
    viewModel: PassportViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraUri?.let { viewModel.processPassportImage(it) }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.processPassportImage(it) }
    }

    val saveReportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) viewModel.generateOfficialReport(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCameraInternal()
        } else {
            Toast.makeText(context, "يرجى منح إذن الكاميرا لالتقاط صورة جواز السفر", Toast.LENGTH_LONG).show()
        }
    }

    fun launchCameraInternal() {
        try {
            val cacheDir = File(context.cacheDir, "camera_passport").also { it.mkdirs() }
            val file = File(cacheDir, "passport_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            tempCameraUri = uri
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر تشغيل الكاميرا: ${e.message}", Toast.LENGTH_SHORT).show()
            galleryLauncher.launch("image/*")
        }
    }

    fun launchCamera() {
        val hasCameraPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            launchCameraInternal()
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "تم نسخ $label إلى الحافظة", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ماسح جواز السفر (MRZ / Barcode)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error banner
            state.errorMessage?.let { error ->
                ErrorBanner(
                    errorMessage = error,
                    onDismiss = { viewModel.dismissError() }
                )
            }

            // Hero Info Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlightTakeoff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "مسح وتدقيق الجواز وفق معايير ICAO 9303",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "قراءة منطقة الشيفرة المدمجة (MRZ) أو الباركود في أسفل صفحة الجواز، والتحقق من صحة الأرقام الرقابية والتواريخ 100% بدون إنترنت.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Image Picker / Preview Box
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "صورة صفحة المعلومات بجواز السفر:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (state.bitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        ) {
                            Image(
                                bitmap = state.bitmap!!.asImageBitmap(),
                                contentDescription = "جواز السفر",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { launchCamera() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إعادة التصوير", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("اختيار من الاستوديو", fontSize = 12.sp)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "وجه الكاميرا نحو صفحة بيانات الجواز",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = { launchCamera() },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("التقاط بالكاميرا", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { galleryLauncher.launch("image/*") },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("اختيار صورة", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Results Section
            state.mrzData?.let { mrz ->
                MrzResultCard(
                    mrz = mrz,
                    onCopyField = { label, text -> copyToClipboard(label, text) },
                    onCopyAll = {
                        val fullSummary = """
                            |نوع الوثيقة: ${mrz.documentType}
                            |الدولة: ${mrz.countryName}
                            |الاسم الكامل: ${mrz.fullName}
                            |رقم الجواز: ${mrz.documentNumber}
                            |الجنسية: ${mrz.nationalityName}
                            |تاريخ الميلاد: ${mrz.dateOfBirth}
                            |النوع: ${mrz.sexLabel}
                            |تاريخ الانتهاء: ${mrz.expiryDate}
                            |حالة الصلاحية: ${if (mrz.isExpired) "منتهي الصلاحية" else "ساري المفعول"}
                            |حالة فحص التدقيق ICAO: ${if (mrz.isCompleteValid) "صحيح ومطابق" else "تنبيه"}
                        """.trimMargin()
                        copyToClipboard("بيانات جواز السفر", fullSummary)
                    },
                    onExportReportPdf = {
                        val docNum = state.mrzData?.documentNumber ?: "Doc"
                        saveReportLauncher.launch("Passport_Report_$docNum.pdf")
                    },
                    isGeneratingReport = state.isGeneratingReport
                )
            }

            // Output Actions Row if report generated
            state.outputUri?.let { outUri ->
                OutputFileActionsRow(
                    outputUri = outUri,
                    defaultMimeType = "application/pdf",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Google AdMob Banner
            AdBanner(modifier = Modifier.padding(top = 10.dp))
        }
    }

    if (state.isScanning || state.isGeneratingReport) {
        LoadingDialog(
            title = if (state.isScanning) "جارٍ فحص وتحليل الجواز…" else "جارٍ إنشاء تقرير الجواز…",
            message = state.progress?.stage ?: "جارٍ المعالجة والتحقق…",
            progressPercent = state.progress?.percentage
        )
    }
}

@Composable
private fun MrzResultCard(
    mrz: PassportMrzData,
    onCopyField: (String, String) -> Unit,
    onCopyAll: () -> Unit,
    onExportReportPdf: () -> Unit,
    isGeneratingReport: Boolean
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (mrz.isCompleteValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (mrz.isCompleteValid) Color(0xFF059669) else Color(0xFFD97706),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "بيانات الجواز المستخرجة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (mrz.isExpired) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (mrz.isExpired) "منتهي الصلاحية" else "ساري المفعول ✓",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (mrz.isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            MrzFieldRow("الاسم الكامل (Full Name)", mrz.fullName) { onCopyField("الاسم", mrz.fullName) }
            MrzFieldRow("رقم الجواز (Passport No.)", mrz.documentNumber) { onCopyField("رقم الجواز", mrz.documentNumber) }
            MrzFieldRow("الدولة المصدرة (Issuing State)", mrz.countryName) { onCopyField("الدولة", mrz.countryName) }
            MrzFieldRow("الجنسية (Nationality)", mrz.nationalityName) { onCopyField("الجنسية", mrz.nationalityName) }
            MrzFieldRow("تاريخ الميلاد (Birth Date)", mrz.dateOfBirth) { onCopyField("تاريخ الميلاد", mrz.dateOfBirth) }
            MrzFieldRow("النوع (Sex)", mrz.sexLabel) { onCopyField("النوع", mrz.sexLabel) }
            MrzFieldRow("تاريخ الانتهاء (Expiry Date)", mrz.expiryDate) { onCopyField("تاريخ الانتهاء", mrz.expiryDate) }
            mrz.personalNumber?.let { pn ->
                MrzFieldRow("الرقم الوطني / الموحد", pn) { onCopyField("الرقم الوطني", pn) }
            }

            // Raw MRZ Box
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "الرمز الآلي (MRZ Zone):",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = mrz.rawMrz,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(10.dp)
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCopyAll,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("نسخ البيانات", fontSize = 12.sp)
                }

                Button(
                    onClick = onExportReportPdf,
                    enabled = !isGeneratingReport,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تصدير تقرير PDF", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun MrzFieldRow(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(
            onClick = onCopy,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "نسخ",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
