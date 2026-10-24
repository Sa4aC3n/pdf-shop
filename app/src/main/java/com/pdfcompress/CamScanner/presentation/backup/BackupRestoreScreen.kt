package com.pdfcompress.CamScanner.presentation.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdfcompress.CamScanner.core.ui.components.ErrorBanner
import com.pdfcompress.CamScanner.core.ui.components.LoadingDialog
import com.pdfcompress.CamScanner.core.ui.components.ModernGradientButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    viewModel: BackupRestoreViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var showRestorePassword by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setRestoreSourceUri(it) }
    }

    val saveBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) viewModel.createBackup(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "النسخ الاحتياطي والأرشيف",
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

            // Lock Icon Header
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Text(
                text = "النسخ الاحتياطي المشفر (AES-256)",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onBackground
            )

            // Segmented Tabs
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    // Tab 0: Export
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (state.selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        TextButton(onClick = { viewModel.selectTab(0) }) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = null,
                                tint = if (state.selectedTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تصدير نسخة احتياطية",
                                fontWeight = FontWeight.Bold,
                                color = if (state.selectedTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Tab 1: Restore
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (state.selectedTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        TextButton(onClick = { viewModel.selectTab(1) }) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = null,
                                tint = if (state.selectedTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "استعادة الأرشيف",
                                fontWeight = FontWeight.Bold,
                                color = if (state.selectedTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Tab Content
            if (state.selectedTab == 0) {
                // EXPORT TAB
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "تُحفظ كل المستمسكات والوثائق المحفوظة في ملف أرشيف واحد آمن.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Switch: Protect with password
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "حماية النسخة بكلمة سر",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "مشفّرة AES-256 (موصى به لحماية المستمسكات)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Switch(
                                checked = state.isPasswordEnabled,
                                onCheckedChange = { viewModel.setPasswordEnabled(it) }
                            )
                        }

                        if (state.isPasswordEnabled) {
                            OutlinedTextField(
                                value = state.password,
                                onValueChange = { viewModel.setPassword(it) },
                                label = { Text("كلمة السر") },
                                singleLine = true,
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = state.confirmPassword,
                                onValueChange = { viewModel.setConfirmPassword(it) },
                                label = { Text("تأكيد كلمة السر") },
                                singleLine = true,
                                visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                        Icon(
                                            imageVector = if (showConfirmPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Security Hints
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "• احفظ كلمة السر في مكان آمن؛ لا يمكن فتح النسخة بدونها.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "• التشفير AES-256 ويتم بالكامل محلياً على هاتفك بدون إنترنت.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        ModernGradientButton(
                            text = "إنشاء النسخة الاحتياطية",
                            onClick = { saveBackupLauncher.launch("PDFShop_Backup_${System.currentTimeMillis()}.pdfshop") },
                            enabled = !state.isProcessing,
                            icon = Icons.Default.Lock,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                // RESTORE TAB
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "اختر ملف النسخة الاحتياطية (.pdfshop) لاستعادة جميع الوثائق وسجل المستندات.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedButton(
                            onClick = { filePickerLauncher.launch("*/*") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (state.restoreSourceUri != null) "تم اختيار ملف الأرشيف ✓" else "اختيار ملف الأرشيف (.pdfshop)",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedTextField(
                            value = state.restorePassword,
                            onValueChange = { viewModel.setRestorePassword(it) },
                            label = { Text("كلمة سر الأرشيف (إن كانت النسخة مشفرة)") },
                            singleLine = true,
                            visualTransformation = if (showRestorePassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { showRestorePassword = !showRestorePassword }) {
                                    Icon(
                                        imageVector = if (showRestorePassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        ModernGradientButton(
                            text = "استعادة الأرشيف الآن",
                            onClick = { viewModel.restoreBackup() },
                            enabled = !state.isProcessing && state.restoreSourceUri != null,
                            icon = Icons.Default.Restore,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (state.isProcessing) {
        LoadingDialog(
            title = "جارٍ المعالجة…",
            message = state.progress?.stage ?: "جارٍ المعالجة…",
            progressPercent = state.progress?.percentage
        )
    }

    state.exportReport?.let { report ->
        AlertDialog(
            onDismissRequest = { viewModel.clearResult() },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF059669),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = { Text("اكتملت النسخة الاحتياطية بنجاح") },
            text = {
                Text(
                    "تم تصدير وحفظ النسخة الاحتياطية في مجلد المستندات.\n\n" +
                        "• عدد السجلات: ${report.totalRecordsCount}\n" +
                        "• عدد الملفات: ${report.totalFilesCount}\n" +
                        "• التشفير: ${if (report.isEncrypted) "مشفر بـ AES-256 ✓" else "غير مشفر"}\n" +
                        "• حجم الملف: ${report.outputSizeBytes / 1024} كيلوبايت"
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.clearResult() }) {
                    Text("حسناً")
                }
            }
        )
    }

    state.restoredCount?.let { count ->
        AlertDialog(
            onDismissRequest = { viewModel.clearResult() },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF059669),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = { Text("تمت استعادة الأرشيف بنجاح") },
            text = {
                Text("تمت استعادة $count سجل مستند وملف بنجاح إلى التطبيق وسجل العمليات.")
            },
            confirmButton = {
                Button(onClick = { viewModel.clearResult() }) {
                    Text("حسناً")
                }
            }
        )
    }
}
