package com.pdfcompress.CamScanner.presentation.compare

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdfcompress.CamScanner.domain.model.DiffType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    viewModel: CompareViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val pickerLauncherA = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.setFileA(it) }
    }

    val pickerLauncherB = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.setFileB(it) }
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
                title = { Text("مقارنة ملفات PDF", fontWeight = FontWeight.Bold) },
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
            // Document A Picker
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("المستند الأول (الأصلي / الإصدار A):", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (state.uriA == null) {
                        OutlinedButton(
                            onClick = { pickerLauncherA.launch(arrayOf("application/pdf")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("اختيار المستند الأول (PDF)")
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.nameA ?: "مستند 1",
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(onClick = { pickerLauncherA.launch(arrayOf("application/pdf")) }) {
                                Icon(Icons.Default.Difference, contentDescription = "تغيير")
                            }
                        }
                    }
                }
            }

            // Document B Picker
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("المستند الثاني (المعدل / الإصدار B):", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (state.uriB == null) {
                        OutlinedButton(
                            onClick = { pickerLauncherB.launch(arrayOf("application/pdf")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("اختيار المستند الثاني (PDF)")
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.nameB ?: "مستند 2",
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(onClick = { pickerLauncherB.launch(arrayOf("application/pdf")) }) {
                                Icon(Icons.Default.Difference, contentDescription = "تغيير")
                            }
                        }
                    }
                }
            }

            // Start Compare Button
            if (state.uriA != null && state.uriB != null) {
                Button(
                    onClick = { viewModel.startComparison() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !state.isComparing
                ) {
                    if (state.isComparing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("جاري فحص المقارنة أوفلاين…")
                    } else {
                        Icon(Icons.Default.CompareArrows, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("بدء المقارنة الذكية الآن", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Progress
            if (state.isComparing) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(state.progressMessage ?: "جاري المقارنة…")
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

            // Results Card
            state.compareResult?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.diffCount == 0)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val icon = if (result.diffCount == 0) Icons.Default.CheckCircle else Icons.Default.Warning
                            val title = if (result.diffCount == 0)
                                "المستندان متطابقان تماماً في النصوص!"
                            else
                                "تم العثور على اختلافات في ${result.diffCount} صفحة"
                            Icon(icon, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(title, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("عدد صفحات المستند الأول: ${result.totalPagesA} | الثاني: ${result.totalPagesB}")
                    }
                }

                // Page Selector Chips
                if (result.pageDiffs.isNotEmpty()) {
                    Text("اختر الصفحة لعرض الفروقات:", fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        result.pageDiffs.forEachIndexed { index, pDiff ->
                            FilterChip(
                                selected = state.selectedPageIndex == index,
                                onClick = { viewModel.selectPage(index) },
                                label = {
                                    val badge = if (pDiff.hasDiff) "• ص ${pDiff.pageNumber}" else "ص ${pDiff.pageNumber}"
                                    Text(badge)
                                }
                            )
                        }
                    }

                    // Diff Viewer for Selected Page
                    val curDiff = result.pageDiffs.getOrNull(state.selectedPageIndex)
                    if (curDiff != null) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("تفاصيل الصفحة ${curDiff.pageNumber}", fontWeight = FontWeight.Bold)
                                    Text(
                                        text = if (curDiff.hasDiff) "يوجد اختلافات" else "متطابقة",
                                        color = if (curDiff.hasDiff) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                if (curDiff.diffLines.isEmpty()) {
                                    Text("لا توجد نصوص أو اختلافات في هذه الصفحة", style = MaterialTheme.typography.bodySmall)
                                } else {
                                    curDiff.diffLines.forEach { line ->
                                        val (bgColor, textColor, prefix) = when (line.type) {
                                            DiffType.ADDED -> Triple(Color(0xFFDCFCE7), Color(0xFF166534), "+ ")
                                            DiffType.REMOVED -> Triple(Color(0xFFFEE2E2), Color(0xFF991B1B), "- ")
                                            DiffType.UNCHANGED -> Triple(Color.Transparent, MaterialTheme.colorScheme.onSurface, "  ")
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(bgColor, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "$prefix${line.text}",
                                                color = textColor,
                                                fontSize = 13.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
