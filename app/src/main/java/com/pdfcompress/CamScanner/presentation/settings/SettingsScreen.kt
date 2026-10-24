package com.pdfcompress.CamScanner.presentation.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdfcompress.CamScanner.core.ui.components.ModernGradientButton
import com.pdfcompress.CamScanner.domain.model.AppColorTheme
import com.pdfcompress.CamScanner.domain.model.AppLanguage
import com.pdfcompress.CamScanner.domain.model.AppTheme

private const val DEVELOPER_PLAY_STORE_URL = "https://play.google.com/store/apps/dev?id=6839550047456072292"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showColorThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAboutAppDialog by remember { mutableStateOf(false) }
    var showTermsOfUseDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "الإعدادات والمظهر",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Section 1: Appearance & Color Theme
            Text(
                text = "تخصيص المظهر والألوان",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showColorThemeDialog = true }
                            .testTag("settings_color_theme_row")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "لون المظهر الأساسي",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = settings.colorTheme.titleAr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Interactive Color Swatch Picker Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppColorTheme.entries.forEach { palette ->
                            val isSelected = settings.colorTheme == palette
                            val swatchColor = Color(palette.primaryColorLong)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.setColorTheme(palette) }
                                    .padding(horizontal = 4.dp, vertical = 6.dp)
                                    .testTag("color_swatch_${palette.name.lowercase()}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = palette.titleAr,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                // Dark / Light / System Mode Row
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showThemeDialog = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DarkMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "وضع الإضاءة (فاتح / داكن)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when (settings.theme) {
                                    AppTheme.SYSTEM -> "تلقائي (حسب النظام)"
                                    AppTheme.LIGHT -> "المظهر الفاتح"
                                    AppTheme.DARK -> "المظهر الداكن"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val themeOptions = listOf(
                            AppTheme.LIGHT to "فاتح",
                            AppTheme.DARK to "داكن",
                            AppTheme.SYSTEM to "تلقائي"
                        )
                        themeOptions.forEach { (themeMode, label) ->
                            val selected = settings.theme == themeMode
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.setTheme(themeMode) },
                                label = {
                                    Text(
                                        text = label,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_mode_${themeMode.name.lowercase()}"),
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                SettingsClickableItem(
                    icon = Icons.Default.Language,
                    title = "اللغة واتجاه الواجهة",
                    subtitle = when (settings.language) {
                        AppLanguage.ARABIC -> "العربية (RTL)"
                        AppLanguage.ENGLISH -> "English (LTR)"
                    },
                    onClick = { showLanguageDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2: Security
            Text(
                text = "الأمان والقفل",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                SettingsToggleItem(
                    icon = Icons.Default.Fingerprint,
                    title = "قفل التطبيق بالبصمة",
                    subtitle = "طلب بصمة الإصبع أو شاشة قفل الجهاز الرسمية عند فتح التطبيق",
                    checked = settings.isAppLockEnabled,
                    onCheckedChange = { viewModel.setAppLock(it) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 3: App Info & Store Actions (2 small cards side-by-side: عن التطبيق + تحديث التطبيق)
            Text(
                text = "معلومات التطبيق والتحديثات",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmallActionCard(
                    icon = Icons.Default.Info,
                    title = "عن التطبيق",
                    subtitle = "الوصف والمميزات",
                    onClick = { showAboutAppDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_about_app_card")
                )

                SmallActionCard(
                    icon = Icons.Default.SystemUpdate,
                    title = "تحديث التطبيق",
                    subtitle = "أحدث إصدار بالمتجر",
                    onClick = { openAppInPlayStore(context) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_update_app_card")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 4: Our Apps on Google Play + Container Card with 2 inner small cards (مشاركة التطبيق + تقييم التطبيق)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Developer Store Banner
                    Surface(
                        onClick = { openDeveloperPageInPlayStore(context) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_our_apps_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Apps,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "تطبيقاتنا على متجر جوجل",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "تصفح جميع تطبيقاتنا وأدواتنا الرسمية على Google Play",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "فتح متجر جوجل",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Two small cards inside the parent card: مشاركة التطبيق & تقييم التطبيق
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SmallActionCard(
                            icon = Icons.Default.Share,
                            title = "مشاركة التطبيق",
                            subtitle = "شارك مع أصدقائك",
                            onClick = { shareAppLink(context) },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_share_app_card")
                        )

                        SmallActionCard(
                            icon = Icons.Default.StarRate,
                            title = "تقييم التطبيق",
                            subtitle = "ادعمنا بتقييم 5 نجوم",
                            onClick = { openAppInPlayStore(context) },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_rate_app_card")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 5: Privacy Guarantee Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "تطبيق محلي أوفلاين 100%",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "• لا يوجد إذن INTERNET في Manifest التطبيق نهائياً.\n• لا توجد خوادم وسيطة أو خدمات رفع سحابية.\n• لا يتم تسجيل بيانات الدخول أو إرسال تحليلات.\n• جميع محركات المعالجة مرخصة وفق Apache 2.0 المفتوح المعتمد.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 6 (Bottom of Settings Tab): Legal Pages (شروط الاستخدام & سياسة الخصوصية)
            Text(
                text = "الأحكام القانونية والخصوصية",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                SettingsPolicyItem(
                    icon = Icons.Default.Gavel,
                    title = "1- شروط الاستخدام",
                    subtitle = "الضوابط القانونية ومسؤوليات استخدام أدوات معالجة المستندات",
                    onClick = { showTermsOfUseDialog = true },
                    testTag = "settings_terms_of_use_item"
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                SettingsPolicyItem(
                    icon = Icons.Default.PrivacyTip,
                    title = "2- سياسة الخصوصية",
                    subtitle = "تفاصيل المعالجة المحلية 100% والصلاحيات وحماية بياناتك",
                    onClick = { showPrivacyPolicyDialog = true },
                    testTag = "settings_privacy_policy_item"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Pop-up 1: عن التطبيق (About the App & Features)
    if (showAboutAppDialog) {
        AlertDialog(
            onDismissRequest = { showAboutAppDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "عن التطبيق والمميزات",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "ClearScan Pro: PDF Scanner",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "YOUR ULTIMATE PDF SHOP",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "منصة متكاملة لمعالجة ومسح ملفات الـ PDF والوثائق محلياً بالكامل على جهازك، مصممة لتوفير أعلى مستويات السرعة والخصوصية دون الحاجة لأي اتصال بالإنترنت.",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 21.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "أبرز المميزات والأدوات الفعلية:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. ماسح المستندات بالكاميرا: التقاط الوثائق الورقية مع فلاتر توضيح النصوص وتدوير الصفحات وتصديرها كـ PDF.\n" +
                            "2. دمج ملفات PDF: جمع عدة مستندات في ملف واحد مع التحكم الكامل بترتيبها.\n" +
                            "3. تقسيم واستخراج الصفحات: تحديد صفحات معينة من أي مستند وحفظها في ملف PDF مستقل.\n" +
                            "4. ضغط ملفات PDF: تقليل حجم الملفات عبر ضغط الصور المضمنة بـ 3 مستويات مع مقارنة شفافة للحجم قبل وبعد.\n" +
                            "5. تحويل الصور إلى PDF: تحويل صور المعرض إلى مستند PDF متناسق عالي الجودة.\n" +
                            "6. تحويل PDF إلى صور: تصدير صفحات المستند كصور JPG مفردة أو مجمعة في أرشيف ZIP.\n" +
                            "7. إضافة علامة مائية: حماية المستندات بنصوص عربية أو إنجليزية مخصصة مع التحكم في الشفافية والحجم والزاوية.\n" +
                            "8. استخراج النصوص والـ OCR: قراءة النصوص الرقمية والبحث الفوري بداخلها ونسخها أو تصديرها كملف TXT، مع دعم التكامل المباشر مع عدسة Google للصور.\n" +
                            "9. إصلاح ملفات PDF: فحص الملفات المتضررة وإعادة بناء جداول المراجع لاسترداد الصفحات السليمة.\n" +
                            "10. قفل التطبيق بالبصمة وتخصيص الألوان: حماية خصوصيتك بالبصمة أو رمز الجهاز مع 6 ألوان مظهر احترافية.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                ModernGradientButton(
                    text = "حسناً",
                    onClick = { showAboutAppDialog = false },
                    height = 44.dp
                )
            }
        )
    }

    // Pop-up 2: شروط الاستخدام (Terms of Use)
    if (showTermsOfUseDialog) {
        AlertDialog(
            onDismissRequest = { showTermsOfUseDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Gavel,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "شروط الاستخدام",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "مرحباً بك في تطبيق ClearScan Pro: PDF Scanner (YOUR ULTIMATE PDF SHOP). باستخدامك لهذا التطبيق فإنك توافق على الشروط والضوابط التالية:\n\n" +
                            "1. طبيعة الخدمة المحلية:\n" +
                            "يعمل التطبيق كأداة معالجة ملفات محلية بالكامل على جهاز المستخدم. تتم جميع عمليات الدمج، التقسيم، الضغط، الترقيم، والمسح الضوئي داخل ذاكرة جهازك دون تدخل أي خادم خارجي.\n\n" +
                            "2. الحفاظ على الملفات الأصلية:\n" +
                            "يلتزم التطبيق بعدم الكتابة فوق ملفاتك الأصلية أو حذفها. جميع العمليات تنتج ملفات جديدة في المسار الذي تختاره بنفسك عبر منتقي ملفات النظام الرسمي (Android SAF).\n\n" +
                            "3. مسؤولية المحتوى وحقوق الملكية:\n" +
                            "المستخدم هو المسؤول الوحيد عن قانونية الملفات والمستندات التي يقوم بمعالجتها أو مسحها ضوئياً أو إضافة علامات مائية عليها، وعن امتلاكه الصلاحية القانونية للتعامل مع تلك الوثائق.\n\n" +
                            "4. حدود إصلاح وضغط الملفات:\n" +
                            "• تعتمد نسبة ضغط ملفات PDF على وجود صور مضمنة قابلة للضغط؛ المستندات النصية البحتة قد لا ينخفض حجمها، ويقوم التطبيق بإبلاغك بذلك بشفافية.\n" +
                            "• أداة إصلاح PDF تعيد بناء الفهارس المتضررة وتسترد الصفحات القابلة للقراءة، ولكنها لا تفك تشفير الملفات المحمية بكلمات مرور ولا تسترجع البيانات المفقودة كلياً على مستوى البايتات.\n\n" +
                            "5. التحديثات وحقوق الترخيص:\n" +
                            "يعتمد التطبيق على مكتبات مفتوحة المصدر مرخصة بموجب رخصة Apache 2.0 المعتمدة. يُحظر تفكيك التطبيق أو إعادة توزيعه لأغراض غير مشروعة.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                }
            },
            confirmButton = {
                ModernGradientButton(
                    text = "موافق وإغلاق",
                    onClick = { showTermsOfUseDialog = false },
                    height = 44.dp
                )
            }
        )
    }

    // Pop-up 3: سياسة الخصوصية (Privacy Policy)
    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.PrivacyTip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "سياسة الخصوصية",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "خصوصيتك هي الركيزة الأساسية التي بُني عليها هذا التطبيق:\n\n" +
                            "1. انعدام الاتصال بالإنترنت (100% Offline):\n" +
                            "لا يطلب التطبيق صلاحية الاتصال بالإنترنت (INTERNET Permission) في ملف التعريف البرمجي (AndroidManifest). هذا يضمن تقنياً استحالة إرسال أي مستند أو صورة أو نص خارج جهازك.\n\n" +
                            "2. عدم جمع البيانات أو التتبع:\n" +
                            "لا يقوم التطبيق بجمع أي بيانات شخصية، ولا يستخدم أي أدوات تتبع أو إعلانات أو تحليلات سحابية، ولا يطلب إنشاء حساب أو تسجيل دخول.\n\n" +
                            "3. صلاحية الكاميرا (CAMERA):\n" +
                            "تُستخدم صلاحية الكاميرا حصرياً عند فتحك لأداة «ماسح المستندات بالكاميرا» لالتقاط صور الوثائق وتحويلها إلى PDF محلياً. يتم حذف الصور المؤقتة فور انتهاء العملية.\n\n" +
                            "4. الوصول إلى الملفات والتخزين:\n" +
                            "لا يطلب التطبيق صلاحيات قراءة شاملة لذاكرة الجهاز، بل يستخدم إطار عمل الوصول للتخزين الآمن (Android Storage Access Framework) بحيث لا يقرأ التطبيق إلا الملف المحدد الذي تختاره بنفسك في كل عملية.\n\n" +
                            "5. سجل العمليات والقفل الحيوي:\n" +
                            "يتم حفظ سجل العمليات وتفضيلات المظهر محلياً داخل قاعدة بيانات التطبيق الخاصة على جهازك فقط، ويمكنك مسح السجل في أي وقت بضغطة زر. عند تفعيل قفل البصمة، يتم التحقق عبر نظام الأمان الرسمي لجهازك (Android BiometricPrompt) دون تخزين أي بيانات حيوية داخل التطبيق.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                }
            },
            confirmButton = {
                ModernGradientButton(
                    text = "قرأت وفهمت",
                    onClick = { showPrivacyPolicyDialog = false },
                    height = 44.dp
                )
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("اختر وضع الإضاءة", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    AppTheme.entries.forEach { theme ->
                        val label = when (theme) {
                            AppTheme.SYSTEM -> "تلقائي (حسب النظام)"
                            AppTheme.LIGHT -> "فاتح"
                            AppTheme.DARK -> "داكن"
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setTheme(theme)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = (settings.theme == theme),
                                onClick = {
                                    viewModel.setTheme(theme)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    if (showColorThemeDialog) {
        AlertDialog(
            onDismissRequest = { showColorThemeDialog = false },
            title = { Text("اختر لون المظهر", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    AppColorTheme.entries.forEach { colorTheme ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setColorTheme(colorTheme)
                                    showColorThemeDialog = false
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorTheme.primaryColorLong))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = colorTheme.titleAr,
                                modifier = Modifier.weight(1f),
                                fontWeight = if (settings.colorTheme == colorTheme) FontWeight.Bold else FontWeight.Normal
                            )
                            RadioButton(
                                selected = (settings.colorTheme == colorTheme),
                                onClick = {
                                    viewModel.setColorTheme(colorTheme)
                                    showColorThemeDialog = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorThemeDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("اختر اللغة", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    AppLanguage.entries.forEach { lang ->
                        val label = when (lang) {
                            AppLanguage.ARABIC -> "العربية (RTL)"
                            AppLanguage.ENGLISH -> "English (LTR)"
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setLanguage(lang)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = (settings.language == lang),
                                onClick = {
                                    viewModel.setLanguage(lang)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

private fun openAppInPlayStore(context: Context) {
    val pkg = context.packageName
    try {
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(marketIntent)
    } catch (_: Exception) {
        try {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "تعذر فتح متجر Google Play على هذا الجهاز", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun openDeveloperPageInPlayStore(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DEVELOPER_PLAY_STORE_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "تعذر فتح صفحة المطور على متجر Google Play", Toast.LENGTH_SHORT).show()
    }
}

private fun shareAppLink(context: Context) {
    val pkg = context.packageName
    val shareText = "جرب تطبيق ClearScan Pro: PDF Scanner (YOUR ULTIMATE PDF SHOP) — يعمل بدون إنترنت 100%:\n" +
        "https://play.google.com/store/apps/details?id=$pkg\n\n" +
        "وتصفح جميع تطبيقاتنا على متجر جوجل:\n$DEVELOPER_PLAY_STORE_URL"
    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "ClearScan Pro: PDF Scanner")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(sendIntent, "مشاركة التطبيق عبر"))
    } catch (_: Exception) {
        Toast.makeText(context, "تعذر فتح قائمة المشاركة", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun SmallActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsPolicyItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
