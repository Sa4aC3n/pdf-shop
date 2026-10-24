package com.pdfcompress.CamScanner.domain.model

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AppLanguage {
    ARABIC,
    ENGLISH
}

enum class AppColorTheme(
    val titleAr: String,
    val titleEn: String,
    val primaryColorLong: Long,
    val primaryContainerLightLong: Long,
    val primaryContainerDarkLong: Long,
    val secondaryColorLong: Long
) {
    CRIMSON_RED(
        titleAr = "أحمر ياقوتي (PDF)",
        titleEn = "Crimson Red",
        primaryColorLong = 0xFFD32F2FL,
        primaryContainerLightLong = 0xFFFFEBEE,
        primaryContainerDarkLong = 0xFF4A1215,
        secondaryColorLong = 0xFFB71C1CL
    ),
    ROYAL_BLUE(
        titleAr = "أزرق ملكي",
        titleEn = "Royal Blue",
        primaryColorLong = 0xFF1565C0L,
        primaryContainerLightLong = 0xFFE3F2FD,
        primaryContainerDarkLong = 0xFF0D2B4E,
        secondaryColorLong = 0xFF0277BDL
    ),
    EMERALD_TEAL(
        titleAr = "أخضر زمردي",
        titleEn = "Emerald Teal",
        primaryColorLong = 0xFF00796BL,
        primaryContainerLightLong = 0xFFE0F2F1,
        primaryContainerDarkLong = 0xFF003831,
        secondaryColorLong = 0xFF00897BL
    ),
    ROYAL_PURPLE(
        titleAr = "بنفسجي عصري",
        titleEn = "Royal Purple",
        primaryColorLong = 0xFF6A1B9AL,
        primaryContainerLightLong = 0xFFF3E5F5,
        primaryContainerDarkLong = 0xFF321048,
        secondaryColorLong = 0xFF8E24AAL
    ),
    AMBER_ORANGE(
        titleAr = "برتقالي دافئ",
        titleEn = "Amber Orange",
        primaryColorLong = 0xFFE65100L,
        primaryContainerLightLong = 0xFFFFF3E0,
        primaryContainerDarkLong = 0xFF4E2004,
        secondaryColorLong = 0xFFF57C00L
    ),
    SLATE_INDIGO(
        titleAr = "كحلي احترافي",
        titleEn = "Slate Indigo",
        primaryColorLong = 0xFF283593L,
        primaryContainerLightLong = 0xFFE8EAF6,
        primaryContainerDarkLong = 0xFF151C4D,
        secondaryColorLong = 0xFF3949ABL
    )
}

data class UserSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val colorTheme: AppColorTheme = AppColorTheme.CRIMSON_RED,
    val language: AppLanguage = AppLanguage.ARABIC,
    val isAppLockEnabled: Boolean = false
)
