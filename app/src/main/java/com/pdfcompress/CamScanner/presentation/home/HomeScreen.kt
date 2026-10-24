package com.pdfcompress.CamScanner.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WaterDamage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdfcompress.CamScanner.core.ui.components.ModernGradientButton
import com.pdfcompress.CamScanner.core.ui.components.ModernSoftButton
import com.pdfcompress.CamScanner.domain.model.AppColorTheme
import com.pdfcompress.CamScanner.domain.model.AppTheme
import com.pdfcompress.CamScanner.domain.model.UserSettings

enum class ToolCategory(val titleAr: String) {
    ALL("الكل"),
    SCAN_OCR("المسح والـ OCR"),
    ORGANIZE("دمج وتقسيم"),
    CONVERT_COMPRESS("ضغط وتحويل"),
    PROTECT_REPAIR("حماية وإصلاح")
}

data class ToolDefinition(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isAvailable: Boolean,
    val badgeText: String,
    val category: ToolCategory,
    val accentLightColor: Color,
    val accentDarkColor: Color,
    val cardBgLight: Color,
    val cardBgDark: Color,
    val cardBorderLight: Color,
    val cardBorderDark: Color,
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
    currentSettings: UserSettings = UserSettings(),
    onSelectColorTheme: (AppColorTheme) -> Unit = {},
    onSelectThemeMode: (AppTheme) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(ToolCategory.ALL) }
    var showAppearanceSheet by remember { mutableStateOf(false) }

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val activeTools = remember {
        listOf(
            ToolDefinition(
                id = "scanner",
                title = "ماسح المستندات بالكاميرا",
                description = "التقاط الوثائق الورقية وفلاتر المستندات وتصديرها مباشرة كـ PDF",
                icon = Icons.Default.CameraAlt,
                isAvailable = true,
                badgeText = "جاهز ومكتمل",
                category = ToolCategory.SCAN_OCR,
                accentLightColor = Color(0xFFDC2626),
                accentDarkColor = Color(0xFFF87171),
                cardBgLight = Color(0xFFFFF5F5),
                cardBgDark = Color(0xFF28181B),
                cardBorderLight = Color(0xFFFECACA),
                cardBorderDark = Color(0xFF4C2228),
                route = "scanner"
            ),
            ToolDefinition(
                id = "ocr",
                title = "التعرف الضوئي OCR",
                description = "استخراج النصوص من الصور والمستندات محلياً بدون إنترنت (عربي وإنجليزي)",
                icon = Icons.Default.FindInPage,
                isAvailable = true,
                badgeText = "جاهز ومكتمل",
                category = ToolCategory.SCAN_OCR,
                accentLightColor = Color(0xFF2563EB),
                accentDarkColor = Color(0xFF60A5FA),
                cardBgLight = Color(0xFFF0F7FF),
                cardBgDark = Color(0xFF162235),
                cardBorderLight = Color(0xFFBFDBFE),
                cardBorderDark = Color(0xFF233B5E),
                route = "ocr"
            ),
            ToolDefinition(
                id = "merge",
                title = "دمج ملفات PDF",
                description = "دمج عدة ملفات في مستند واحد مع إعادة الترتيب",
                icon = Icons.AutoMirrored.Filled.MergeType,
                isAvailable = true,
                badgeText = "جاهز ومكتمل",
                category = ToolCategory.ORGANIZE,
                accentLightColor = Color(0xFF0D9488),
                accentDarkColor = Color(0xFF34D399),
                cardBgLight = Color(0xFFF0FDFA),
                cardBgDark = Color(0xFF142826),
                cardBorderLight = Color(0xFF99F6E4),
                cardBorderDark = Color(0xFF1E443E),
                route = "merge"
            ),
            ToolDefinition(
                id = "images_to_pdf",
                title = "صور إلى PDF",
                description = "تحويل مجموعة من الصور إلى مستند PDF عالي الجودة",
                icon = Icons.Default.Image,
                isAvailable = true,
                badgeText = "جاهز ومكتمل",
                category = ToolCategory.CONVERT_COMPRESS,
                accentLightColor = Color(0xFF7C3AED),
                accentDarkColor = Color(0xFFA78BFA),
                cardBgLight = Color(0xFFF8F5FF),
                cardBgDark = Color(0xFF221933),
                cardBorderLight = Color(0xFFDDD6FE),
                cardBorderDark = Color(0xFF3B2859),
                route = "images_to_pdf"
            ),
            ToolDefinition(
                id = "split",
                title = "تقسيم واستخراج صفحات",
                description = "استخراج صفحات محددة من مستند PDF وحفظها كملف مستقل",
                icon = Icons.AutoMirrored.Filled.CallSplit,
                isAvailable = true,
                badgeText = "جاهز ومكتمل",
                category = ToolCategory.ORGANIZE,
                accentLightColor = Color(0xFFEA580C),
                accentDarkColor = Color(0xFFFB923C),
                cardBgLight = Color(0xFFFFF8F1),
                cardBgDark = Color(0xFF2B1D14),
                cardBorderLight = Color(0xFFFED7AA),
                cardBorderDark = Color(0xFF52321C),
                route = "split"
            ),
            ToolDefinition(
                id = "compress",
                title = "ضغط مستند PDF",
                description = "تقليل الحجم مع الحفاظ على وضوح المحتوى ومقارنة البايتات الشفافة",
                icon = Icons.Default.Compress,
                isAvailable = true,
                badgeText = "جاهز ومكتمل",
                category = ToolCategory.CONVERT_COMPRESS,
                accentLightColor = Color(0xFF0284C7),
                accentDarkColor = Color(0xFF38BDF8),
                cardBgLight = Color(0xFFF0F9FF),
                cardBgDark = Color(0xFF142433),
                cardBorderLight = Color(0xFFBAE6FD),
                cardBorderDark = Color(0xFF213F59),
                route = "compress"
            ),
            ToolDefinition(
                id = "watermark",
                title = "علامة مائية",
                description = "إضافة نصوص مخصصة أو أختام لحماية المستند وسريته",
                icon = Icons.Default.WaterDamage,
                isAvailable = true,
                badgeText = "جاهز ومكتمل",
                category = ToolCategory.PROTECT_REPAIR,
                accentLightColor = Color(0xFF4F46E5),
                accentDarkColor = Color(0xFF818CF8),
                cardBgLight = Color(0xFFEEF2FF),
                cardBgDark = Color(0xFF1B1D36),
                cardBorderLight = Color(0xFFC7D2FE),
                cardBorderDark = Color(0xFF2E335C),
                route = "watermark"
            ),
            ToolDefinition(
                id = "pdf_to_images",
                title = "تحويل PDF إلى صور",
                description = "تصدير صفحات المستند بصيغة JPG عالية الدقة أو كملف ZIP مجمع",
                icon = Icons.Default.PhotoLibrary,
                isAvailable = true,
                badgeText = "جاهز ومكتمل",
                category = ToolCategory.CONVERT_COMPRESS,
                accentLightColor = Color(0xFFDB2777),
                accentDarkColor = Color(0xFFF472B6),
                cardBgLight = Color(0xFFFDF2F8),
                cardBgDark = Color(0xFF2C1724),
                cardBorderLight = Color(0xFFFBCFE8),
                cardBorderDark = Color(0xFF522540),
                route = "pdf_to_images"
            ),
            ToolDefinition(
                id = "extract_text",
                title = "استخراج النصوص",
                description = "استخراج كامل النصوص الرقمية والبحث فيها ونسخها أو حفظها كملف TXT",
                icon = Icons.Default.Description,
                isAvailable = true,
                badgeText = "جاهز ومكتمل",
                category = ToolCategory.SCAN_OCR,
                accentLightColor = Color(0xFF059669),
                accentDarkColor = Color(0xFF34D399),
                cardBgLight = Color(0xFFECFDF5),
                cardBgDark = Color(0xFF132820),
                cardBorderLight = Color(0xFFA7F3D0),
                cardBorderDark = Color(0xFF1E4635),
                route = "extract_text"
            ),
            ToolDefinition(
                id = "repair",
                title = "محاولة إصلاح PDF",
                description = "فحص واسترداد الملفات التالفة مع تقرير شفاف بحالة الاستعادة",
                icon = Icons.Default.Build,
                isAvailable = true,
                badgeText = "جاهز ومكتمل",
                category = ToolCategory.PROTECT_REPAIR,
                accentLightColor = Color(0xFFD97706),
                accentDarkColor = Color(0xFFFBBF24),
                cardBgLight = Color(0xFFFFFBEB),
                cardBgDark = Color(0xFF292112),
                cardBorderLight = Color(0xFFFDE68A),
                cardBorderDark = Color(0xFF4F3D1B),
                route = "repair"
            )
        )
    }

    val filteredTools = remember(selectedCategory, activeTools) {
        if (selectedCategory == ToolCategory.ALL) {
            activeTools
        } else {
            activeTools.filter { it.category == selectedCategory }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 156.dp),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Top Header Bar (matching Image 3 header + quick Theme Color switcher)
        item(span = { GridItemSpan(maxLineSpan) }) {
            TopBrandBar(
                isDarkTheme = isDarkTheme,
                onOpenAppearanceDialog = { showAppearanceSheet = true },
                onToggleDarkLight = {
                    val nextTheme = if (isDarkTheme) AppTheme.LIGHT else AppTheme.DARK
                    onSelectThemeMode(nextTheme)
                }
            )
        }

        // 2. Hero Action Section + Quick Scanner CTA (matching Image 3 Hero + Image 2 Privacy Banner)
        item(span = { GridItemSpan(maxLineSpan) }) {
            HeroBannerSection(
                onQuickScanClick = onNavigateToScanner,
                onQuickImagesToPdfClick = onNavigateToImagesToPdf
            )
        }

        // 3. 100% Local Privacy Guarantee Banner (matching Image 1 & Image 2)
        item(span = { GridItemSpan(maxLineSpan) }) {
            PrivacyCard()
        }

        // 4. Section Title & Category Filter Chips
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الأدوات المتاحة للعمل الفعلي",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "${filteredTools.size} أدوات",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ToolCategory.entries.forEach { category ->
                        val selected = selectedCategory == category
                        FilterChip(
                            selected = selected,
                            onClick = { selectedCategory = category },
                            label = {
                                Text(
                                    text = category.titleAr,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            )
                        )
                    }
                }
            }
        }

        // 5. 2-Column Responsive Grid of Rich Tool Cards (combining Image 1/2 badges + Image 3 floating action circle & watermark icon)
        items(filteredTools, key = { it.id }) { tool ->
            ToolGridCard(
                tool = tool,
                isDarkTheme = isDarkTheme,
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
    }

    if (showAppearanceSheet) {
        QuickAppearanceDialog(
            currentSettings = currentSettings,
            onSelectColorTheme = onSelectColorTheme,
            onSelectThemeMode = onSelectThemeMode,
            onDismiss = { showAppearanceSheet = false }
        )
    }
}

@Composable
private fun TopBrandBar(
    isDarkTheme: Boolean,
    onOpenAppearanceDialog: () -> Unit,
    onToggleDarkLight: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DocumentScanner,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "ClearScan Pro: PDF Scanner",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "YOUR ULTIMATE PDF SHOP",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quick Theme Color Picker Button
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier
                    .size(42.dp)
                    .testTag("home_color_palette_button"),
                onClick = onOpenAppearanceDialog
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "تغيير لون المظهر",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Quick Dark/Light Mode Toggle Button
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier
                    .size(42.dp)
                    .testTag("home_theme_mode_button"),
                onClick = onToggleDarkLight
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "تبديل الوضع الليلي",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroBannerSection(
    onQuickScanClick: () -> Unit,
    onQuickImagesToPdfClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "إدارة ومسح ملفاتك بذكاء",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "بأعلى درجات الأمان والاحترافية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "امسح الوثائق بالكاميرا، استخرج النصوص، وادمج أو اضغط ملفات PDF في ثوانٍ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModernGradientButton(
                        text = "مسح مستند الآن",
                        icon = Icons.Default.CameraAlt,
                        onClick = onQuickScanClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hero_quick_scan_button")
                    )

                    ModernSoftButton(
                        text = "صور إلى PDF",
                        icon = Icons.Default.Image,
                        onClick = onQuickImagesToPdfClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hero_images_to_pdf_button")
                    )
                }
            }
        }
    }
}

@Composable
fun PrivacyCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "خصوصية محلية 100%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "جميع العمليات تجري على جهازك مباشرة دون اتصال بالإنترنت أو رفع لأي خادم.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun ToolGridCard(
    tool: ToolDefinition,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val accentColor = if (isDarkTheme) tool.accentDarkColor else tool.accentLightColor
    val containerColor = if (isDarkTheme) tool.cardBgDark else tool.cardBgLight
    val borderColor = if (isDarkTheme) tool.cardBorderDark else tool.cardBorderLight

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(192.dp)
            .clickable(onClick = onClick)
            .testTag("tool_card_${tool.id}"),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(width = 1.5.dp, color = borderColor),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle large background watermark icon in bottom-end (inspired by Image 3)
            Icon(
                imageVector = tool.icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier
                    .size(92.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-10).dp, y = 14.dp)
                    .alpha(if (isDarkTheme) 0.12f else 0.09f)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Tool Icon Badge + "جاهز ومكتمل" Pill (matching Images 1 & 2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = if (isDarkTheme) 0.22f else 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tool.icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = if (isDarkTheme) 0.22f else 0.15f)
                    ) {
                        Text(
                            text = tool.badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                // Middle: Title & Description
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = tool.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = tool.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 17.sp
                    )
                }

                // Bottom Row: PDF Badge + Circular Floating Action Button with white border (matching Image 3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, borderColor)
                    ) {
                        Text(
                            text = "PDF",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                            .border(
                                width = 2.dp,
                                color = Color.White.copy(alpha = 0.9f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "فتح ${tool.title}",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAppearanceDialog(
    currentSettings: UserSettings,
    onSelectColorTheme: (AppColorTheme) -> Unit,
    onSelectThemeMode: (AppTheme) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تخصيص لون ومظهر التطبيق",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "اختر لون المظهر المفضل لديك:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppColorTheme.entries.chunked(2).forEach { rowPalettes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowPalettes.forEach { palette ->
                                val isSelected = currentSettings.colorTheme == palette
                                val primaryColor = Color(palette.primaryColorLong)

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) primaryColor.copy(alpha = 0.14f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) primaryColor
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSelectColorTheme(palette) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(primaryColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = palette.titleAr,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "وضع الإضاءة:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf(
                        AppTheme.LIGHT to "فاتح",
                        AppTheme.DARK to "داكن",
                        AppTheme.SYSTEM to "تلقائي"
                    )
                    modes.forEach { (mode, label) ->
                        val selected = currentSettings.theme == mode
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectThemeMode(mode) },
                            label = {
                                Text(
                                    text = label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("تم", fontWeight = FontWeight.Bold)
            }
        }
    )
}
