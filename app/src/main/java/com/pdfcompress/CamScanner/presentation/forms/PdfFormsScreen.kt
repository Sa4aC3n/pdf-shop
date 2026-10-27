package com.pdfcompress.CamScanner.presentation.forms

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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import com.pdfcompress.CamScanner.domain.model.FormFieldType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfFormsScreen(
    viewModel: PdfFormsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.setSourcePdf(it) }
    }

    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let { viewModel.saveFilledForm(it) }
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
                title = { Text("تعبئة نماذج PDF (Forms)", fontWeight = FontWeight.Bold) },
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
            // PDF Picker Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("اختر نموذج PDF التفاعلي:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (state.pdfUri == null) {
                        OutlinedButton(
                            onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("اختيار ملف الـ PDF")
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = state.pdfName ?: "نموذج PDF",
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text("عدد الحقول المكتشفة: ${state.fields.size}", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) }) {
                                Icon(Icons.Default.Description, contentDescription = "تغيير")
                            }
                        }
                    }
                }
            }

            if (state.isLoadingFields) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("جاري فحص واستخراج حقول النموذج…")
                }
            }

            if (state.pdfUri != null && !state.isLoadingFields) {
                if (state.fields.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "المستند لا يحتوي على حقول نماذج تفاعلية رسمية (AcroForms)، يمكنك استخدام أدوات تعديل أو توقيع الـ PDF لإضافة نصوص وتوقيعات بحرية.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    Text("حقول النموذج المتاحة للتعبئة:", fontWeight = FontWeight.Bold)

                    state.fields.forEach { field ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = field.name,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                when (field.type) {
                                    FormFieldType.CHECKBOX -> {
                                        val isChecked = state.editedValues[field.name]?.equals("Yes", ignoreCase = true) == true ||
                                                state.editedValues[field.name]?.equals("true", ignoreCase = true) == true
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { chk ->
                                                    viewModel.updateFieldValue(field.name, if (chk) "Yes" else "Off")
                                                },
                                                enabled = !field.isReadOnly
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(if (isChecked) "محدد (موافق)" else "غير محدد")
                                        }
                                    }
                                    else -> {
                                        OutlinedTextField(
                                            value = state.editedValues[field.name] ?: "",
                                            onValueChange = { viewModel.updateFieldValue(field.name, it) },
                                            modifier = Modifier.fillMaxWidth(),
                                            enabled = !field.isReadOnly,
                                            placeholder = { Text("أدخل القيمة…") }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Flatten Option Card
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("تسطيح النموذج (Flatten)", fontWeight = FontWeight.Bold)
                                Text("تثبيت البيانات كعناصر غير قابلة للتعديل بعد الحفظ", style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(
                                checked = state.flattenAfterFill,
                                onCheckedChange = { viewModel.setFlatten(it) }
                            )
                        }
                    }

                    // Save Button
                    Button(
                        onClick = {
                            val base = state.pdfName?.substringBeforeLast('.') ?: "form"
                            saveDocumentLauncher.launch("filled_$base.pdf")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !state.isSaving
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("جاري حفظ النموذج أوفلاين…")
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("حفظ النموذج المكتمل الآن", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Progress
            if (state.isSaving) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(state.progressMessage ?: "جاري الحفظ…")
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
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تم تعبئة وحفظ النموذج بنجاح!", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("الملف المحفوظ: ${state.successOutputName}")
                        Spacer(modifier = Modifier.height(12.dp))
                        OutputFileActionsRow(outputUri = state.outputUri)
                    }
                }
            }
        }
    }
}
