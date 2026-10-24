package com.pdfcompress.CamScanner.presentation.ocr

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TravelExplore
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdfcompress.CamScanner.core.ui.components.EmptyStateView
import com.pdfcompress.CamScanner.core.ui.components.ErrorBanner
import com.pdfcompress.CamScanner.core.ui.components.LoadingDialog
import com.pdfcompress.CamScanner.core.ui.components.ModernBottomBarContainer
import com.pdfcompress.CamScanner.core.ui.components.ModernGradientButton
import com.pdfcompress.CamScanner.core.ui.components.ModernSoftButton
import com.pdfcompress.CamScanner.core.ui.components.OutputFileActionsRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScreen(
    viewModel: OcrViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.copiedToClipboard) {
        if (uiState.copiedToClipboard) {
            snackbarHostState.showSnackbar("تم نسخ النص المستخرج إلى الحافظة بنجاح")
            viewModel.dismissClipboardNotification()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setSourceFile(uri)
        }
    }

    val saveTextPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { destinationUri: Uri? ->
        if (destinationUri != null) {
            viewModel.saveAsTextFile(destinationUri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("التعرف الضوئي OCR") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "الرجوع"
                        )
                    }
                },
                actions = {
                    if (!uiState.extractedText.isNullOrBlank()) {
                        IconButton(onClick = { viewModel.copyToClipboard() }) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "نسخ النص")
                        }
                        IconButton(onClick = { viewModel.shareText(context) }) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "مشاركة النص")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            ModernBottomBarContainer {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModernSoftButton(
                        text = if (uiState.sourceUri == null) "اختيار مستند/صورة" else "تغيير المستند",
                        icon = Icons.Default.FileOpen,
                        onClick = { filePickerLauncher.launch(arrayOf("application/pdf", "image/*")) },
                        enabled = !uiState.isProcessing,
                        modifier = Modifier.weight(1f)
                    )

                    if (uiState.sourceUri != null) {
                        ModernGradientButton(
                            text = "عدسة Google",
                            icon = Icons.Default.TravelExplore,
                            onClick = { viewModel.openWithGoogleLens(context) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (!uiState.extractedText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    ModernGradientButton(
                        text = "حفظ النص كملف TXT",
                        icon = Icons.Default.Download,
                        onClick = {
                            val defaultName = "${uiState.sourceName ?: "ocr_result"}.txt"
                            saveTextPickerLauncher.launch(defaultName)
                        },
                        modifier = Modifier.fillMaxWidth()
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
                    onRetry = { viewModel.retryAnalysis() },
                    onDismiss = { viewModel.dismissError() }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (uiState.sourceUri == null) {
                EmptyStateView(
                    title = "اختر مستند PDF أو صورة للتعرف الضوئي OCR",
                    description = "استخراج كامل النصوص العربية والإنجليزية محلياً بدون إنترنت 100% عبر Tesseract المدمج، مع إمكانية البحث الفوري والنسخ والحفظ كملف TXT.",
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.FindInPage
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))

                // Document card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        uiState.previewBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "معاينة المستند",
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.sourceName ?: "",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "الكلمات المستخرجة: ${uiState.wordCount} | الحروف: ${uiState.characterCount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search bar
                val rawText = uiState.extractedText ?: ""
                val query = uiState.searchQuery.trim()
                val matchCount = remember(rawText, query) {
                    if (query.isEmpty() || rawText.isEmpty()) 0
                    else {
                        var count = 0
                        var startIdx = 0
                        while (true) {
                            val idx = rawText.indexOf(query, startIdx, ignoreCase = true)
                            if (idx < 0) break
                            count++
                            startIdx = idx + query.length
                        }
                        count
                    }
                }

                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("بحث في النص المستخرج…") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.FindInPage, contentDescription = null)
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            Text(
                                text = "$matchCount نتيجة",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (matchCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Text Display Box
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        val textToShow = uiState.extractedText
                        if (textToShow.isNullOrBlank()) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "لم يتم العثور على نصوص قابلة للقراءة في هذا الملف.\nتأكد من وضوح وجودة الصورة أو صفحات المستند الممسوح.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            val highlightBg = MaterialTheme.colorScheme.primaryContainer
                            val highlightFg = MaterialTheme.colorScheme.onPrimaryContainer
                            val annotatedText = remember(textToShow, query, highlightBg, highlightFg) {
                                androidx.compose.ui.text.buildAnnotatedString {
                                    append(textToShow)
                                    if (query.isNotEmpty()) {
                                        var startIndex = 0
                                        while (startIndex < textToShow.length) {
                                            val index = textToShow.indexOf(query, startIndex, ignoreCase = true)
                                            if (index < 0) break
                                            addStyle(
                                                style = androidx.compose.ui.text.SpanStyle(
                                                    background = highlightBg,
                                                    color = highlightFg,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                start = index,
                                                end = index + query.length
                                            )
                                            startIndex = index + query.length
                                        }
                                    }
                                }
                            }
                            SelectionContainer {
                                Text(
                                    text = annotatedText,
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.isProcessing) {
        LoadingDialog(
            title = "جارٍ التعرف الضوئي وفهرسة النصوص…",
            message = uiState.progressMessage,
            progressPercent = uiState.progressPercent,
            onCancel = { viewModel.cancelProcessing() }
        )
    }

    uiState.saveSuccessMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissSaveSuccess() },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("تم الحفظ بنجاح") },
            text = {
                Column {
                    Text(msg)
                    OutputFileActionsRow(
                        outputUri = uiState.savedTextUri,
                        defaultMimeType = "text/plain"
                    )
                }
            },
            confirmButton = {
                ModernGradientButton(
                    text = "حسناً",
                    onClick = { viewModel.dismissSaveSuccess() },
                    height = 44.dp
                )
            }
        )
    }
}
