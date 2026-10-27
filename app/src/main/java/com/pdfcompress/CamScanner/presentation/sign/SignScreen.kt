package com.pdfcompress.CamScanner.presentation.sign

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pdfcompress.CamScanner.core.ui.components.OutputFileActionsRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignScreen(
    viewModel: SignViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.setSourcePdf(it) }
    }

    val imgPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.setSignatureImageUri(it) }
    }

    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let { viewModel.signDocument(it) }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    // Touch Drawing Pad lines
    val lines = remember { mutableStateListOf<List<Offset>>() }
    var currentLine by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("التوقيع الإلكتروني على PDF", fontWeight = FontWeight.Bold) },
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
                    Text("المستند المراد توقيعه:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (state.pdfUri == null) {
                        OutlinedButton(
                            onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("اختيار ملف PDF")
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = state.pdfName ?: "مستند PDF",
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text("إجمالي الصفحات: ${state.totalPages}", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) }) {
                                Icon(Icons.Default.Description, contentDescription = "تغيير")
                            }
                        }
                    }
                }
            }

            if (state.pdfUri != null) {
                // Page Selector
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("اختر الصفحة التي تريد وضع التوقيع عليها:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (p in 0 until state.totalPages) {
                                FilterChip(
                                    selected = state.selectedPage == p,
                                    onClick = { viewModel.setSelectedPage(p) },
                                    label = { Text("صفحة ${p + 1}") }
                                )
                            }
                        }
                    }
                }

                // Signature Drawing Canvas / Import
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("لوحة رسم التوقيع بيدك:", fontWeight = FontWeight.Bold)
                            Row {
                                OutlinedButton(
                                    onClick = { imgPickerLauncher.launch(arrayOf("image/*")) },
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("صورة توقيع")
                                }
                                IconButton(
                                    onClick = {
                                        lines.clear()
                                        currentLine = emptyList()
                                    }
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Touch Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            currentLine = listOf(offset)
                                        },
                                        onDrag = { change, _ ->
                                            currentLine = currentLine + change.position
                                        },
                                        onDragEnd = {
                                            if (currentLine.isNotEmpty()) {
                                                lines.add(currentLine)
                                                currentLine = emptyList()
                                            }
                                        }
                                    )
                                }
                        ) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                for (line in lines) {
                                    for (i in 0 until line.size - 1) {
                                        drawLine(
                                            color = Color.Black,
                                            start = line[i],
                                            end = line[i + 1],
                                            strokeWidth = 5f,
                                            cap = StrokeCap.Round
                                        )
                                    }
                                }
                                for (i in 0 until currentLine.size - 1) {
                                    drawLine(
                                        color = Color.Black,
                                        start = currentLine[i],
                                        end = currentLine[i + 1],
                                        strokeWidth = 5f,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }

                            if (lines.isEmpty() && currentLine.isEmpty() && !state.hasDrawnSignature) {
                                Text(
                                    text = "ارسم توقيعك هنا بإصبعك، أو اختر صورة جاهزة…",
                                    color = Color.Gray,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (lines.isNotEmpty()) {
                                    val bmp = createBitmapFromLines(lines, 600, 300)
                                    viewModel.setDrawnSignatureBitmap(bmp)
                                }
                            },
                            enabled = lines.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Draw, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("اعتماد وتثبيت التوقيع المرسوم")
                        }

                        if (state.hasDrawnSignature) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "✓ تم اعتماد التوقيع وجاهز للإدراج في الصفحة",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Placement Controls Card
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("موقع التوقيع في الصفحة:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = state.xPercent > 0.5f && state.yPercent > 0.5f,
                                onClick = { viewModel.setPlacementPreset("bottom_right") },
                                label = { Text("أسفل يمين") }
                            )
                            FilterChip(
                                selected = state.xPercent < 0.3f && state.yPercent > 0.5f,
                                onClick = { viewModel.setPlacementPreset("bottom_left") },
                                label = { Text("أسفل يسار") }
                            )
                            FilterChip(
                                selected = state.xPercent in 0.3f..0.5f && state.yPercent in 0.3f..0.6f,
                                onClick = { viewModel.setPlacementPreset("center") },
                                label = { Text("الوسط") }
                            )
                            FilterChip(
                                selected = state.yPercent < 0.3f,
                                onClick = { viewModel.setPlacementPreset("top_right") },
                                label = { Text("أعلى يمين") }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("حجم التوقيع:", fontWeight = FontWeight.Medium)
                        Slider(
                            value = state.widthPercent,
                            onValueChange = { w ->
                                viewModel.setCustomPlacement(state.xPercent, state.yPercent, w, w * 0.4f)
                            },
                            valueRange = 0.15f..0.55f
                        )
                    }
                }

                // Sign Action Button
                Button(
                    onClick = {
                        val base = state.pdfName?.substringBeforeLast('.') ?: "document"
                        saveDocumentLauncher.launch("signed_$base.pdf")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !state.isSigning && state.hasDrawnSignature
                ) {
                    if (state.isSigning) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("جاري دمج التوقيع وحفظ المستند…")
                    } else {
                        Icon(Icons.Default.Draw, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("توقيع وحفظ الـ PDF الآن", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Progress
            if (state.isSigning) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(state.progressMessage ?: "جاري التوقيع…")
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
                            Text("تم توقيع المستند بنجاح بدون إنترنت!", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("الملف الموقع: ${state.successOutputName}")
                        Spacer(modifier = Modifier.height(12.dp))
                        OutputFileActionsRow(outputUri = state.outputUri)
                    }
                }
            }
        }
    }
}

private fun createBitmapFromLines(lines: List<List<Offset>>, width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    for (line in lines) {
        for (i in 0 until line.size - 1) {
            canvas.drawLine(line[i].x, line[i].y, line[i + 1].x, line[i + 1].y, paint)
        }
    }
    return bitmap
}
