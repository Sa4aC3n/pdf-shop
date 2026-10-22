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

data class UserSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: AppLanguage = AppLanguage.ARABIC,
    val isAppLockEnabled: Boolean = false
)
