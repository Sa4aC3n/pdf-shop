package com.pdfcompress.CamScanner.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WaterDamage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ToolDefinition(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isAvailable: Boolean,
    val badgeText: String,
    val route: String? = null
)

@Composable
fun HomeScreen(
    onNavigateToMerge: () -> Unit,
    onNavigateToImagesToPdf: () -> Unit,
    onNavigateToSplit: () -> Unit,
    onNavigateToCompress: () -> Unit,
    onNavigateToWatermark: () -> Unit,
    onNavigateToPdfToImages: () -> Unit,
    onNavigateToExtractText: () -> Unit,
    onNavigateToRepair: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToOcr: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dialogToolTitle by remember { mutableStateOf<String?>(null) }

    val activeTools = listOf(
        ToolDefinition(
            id = "scanner",
            title = "ماسح المستندات بالكاميرا",
            description = "التقاط الوثائق الورقية وفلاتر المستندات وتصديرها مباشرة كـ PDF",
            icon = Icons.Default.CameraAlt,
            isAvailable = true,
            badgeText = "جاهز ومكتمل",
            route = "scanner"
        ),
        ToolDefinition(
            id = "ocr",
            title = "التعرف الضوئي OCR",
            description = "استخراج النصوص والتكامل الذكي المباشر مع عدسة Google",
            icon = Icons.Default.FindInPage,
            isAvailable = true,
            badgeText = "جاهز ومكتمل",
            route = "ocr"
        ),
        ToolDefinition(
            id = "merge",
            title = "دمج ملفات PDF",
            description = "دمج عدة ملفات في مستند واحد مع إعادة الترتيب",
            icon = Icons.AutoMirrored.Filled.MergeType,
            isAvailable = true,
            badgeText = "جاهز ومكتمل",
            route = "merge"
        ),
        ToolDefinition(
            id = "images_to_pdf",
            title = "صور إلى PDF",
            description = "تحويل مجموعة من الصور إلى مستند PDF عالي الجودة",
            icon = Icons.Default.Image,
            isAvailable = true,
            badgeText = "جاهز ومكتمل",
            route = "images_to_pdf"
        ),
        ToolDefinition(
            id = "split",
            title = "تقسيم واستخراج صفحات",
            description = "استخراج صفحات محددة من مستند PDF وحفظها كملف مستقل",
            icon = Icons.AutoMirrored.Filled.CallSplit,
            isAvailable = true,
            badgeText = "جاهز ومكتمل",
            route = "split"
        ),
        ToolDefinition(
            id = "compress",
            title = "ضغط مستند PDF",
            description = "تقليل الحجم مع الحفاظ على وضوح المحتوى ومقارنة البايتات الشفافة",
            icon = Icons.Default.Compress,
            isAvailable = true,
            badgeText = "جاهز ومكتمل",
            route = "compress"
        ),
        ToolDefinition(
            id = "watermark",
            title = "علامة مائية",
            description = "إضافة نصوص مخصصة أو أختام لحماية المستند وسريته",
            icon = Icons.Default.WaterDamage,
            isAvailable = true,
            badgeText = "جاهز ومكتمل",
            route = "watermark"
        ),
        ToolDefinition(
            id = "pdf_to_images",
            title = "تحويل PDF إلى صور",
            description = "تصدير صفحات المستند بصيغة JPG عالية الدقة أو كملف ZIP مجمع",
            icon = Icons.Default.PhotoLibrary,
            isAvailable = true,
            badgeText = "جاهز ومكتمل",
            route = "pdf_to_images"
        ),
        ToolDefinition(
            id = "extract_text",
            title = "استخراج النصوص",
            description = "استخراج كامل النصوص الرقمية والبحث فيها ونسخها أو حفظها كملف TXT",
            icon = Icons.Default.Description,
            isAvailable = true,
            badgeText = "جاهز ومكتمل",
            route = "extract_text"
        ),
        ToolDefinition(
            id = "repair",
            title = "محاولة إصلاح PDF",
            description = "فحص واسترداد الملفات التالفة مع تقرير شفاف بحالة الاستعادة",
            icon = Icons.Default.Build,
            isAvailable = true,
            badgeText = "جاهز ومكتمل",
            route = "repair"
        )
    )

    val upcomingTools = emptyList<ToolDefinition>()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Privacy Banner
        item {
            PrivacyCard()
        }

        // Section: Active tools
        item {
            Text(
                text = "الأدوات المتاحة للعمل الفعلي",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        items(activeTools.size) { index ->
            val tool = activeTools[index]
            ToolCard(
                tool = tool,
                onClick = {
                    when (tool.id) {
                        "scanner" -> onNavigateToScanner()
                        "ocr" -> onNavigateToOcr()
                        "merge" -> onNavigateToMerge()
                        "images_to_pdf" -> onNavigateToImagesToPdf()
                        "split" -> onNavigateToSplit()
                        "compress" -> onNavigateToCompress()
                        "watermark" -> onNavigateToWatermark()
                        "pdf_to_images" -> onNavigateToPdfToImages()
                        "extract_text" -> onNavigateToExtractText()
                        "repair" -> onNavigateToRepair()
                    }
                }
            )
        }

        // Section: Upcoming tools (if any)
        if (upcomingTools.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "أدوات إضافية (قيد التطوير والاختبار المحلي)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(upcomingTools.size) { index ->
                val tool = upcomingTools[index]
                ToolCard(
                    tool = tool,
                    onClick = {
                        dialogToolTitle = tool.title
                    }
                )
            }
        }
    }

    if (dialogToolTitle != null) {
        AlertDialog(
            onDismissRequest = { dialogToolTitle = null },
            title = { Text(text = dialogToolTitle ?: "") },
            text = {
                Text(
                    text = "هذه الميزة تخضع للتطوير والاختبار المحلي لضمان جودتها وعملها بدون إنترنت تمامًا. التزامًا بمبدأ الصدق الوظيفي، سيتم تفعيلها فور اكتمال محركها واعتماده."
                )
            },
            confirmButton = {
                TextButton(onClick = { dialogToolTitle = null }) {
                    Text("حسناً")
                }
            }
        )
    }
}

@Composable
fun PrivacyCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "خصوصية محلية 100%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "جميع العمليات تجري على جهازك مباشرة دون اتصال بالإنترنت أو رفع لأي خادم.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun ToolCard(
    tool: ToolDefinition,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (tool.isAvailable) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (tool.isAvailable) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (tool.isAvailable) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = null,
                    tint = if (tool.isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tool.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (tool.isAvailable) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = tool.badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (tool.isAvailable) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
