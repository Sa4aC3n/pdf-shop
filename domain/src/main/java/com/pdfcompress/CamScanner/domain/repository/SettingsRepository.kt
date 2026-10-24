package com.pdfcompress.CamScanner.domain.repository

import com.pdfcompress.CamScanner.domain.model.AppColorTheme
import com.pdfcompress.CamScanner.domain.model.AppLanguage
import com.pdfcompress.CamScanner.domain.model.AppTheme
import com.pdfcompress.CamScanner.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun updateTheme(theme: AppTheme)
    suspend fun updateColorTheme(colorTheme: AppColorTheme)
    suspend fun updateLanguage(language: AppLanguage)
    suspend fun updateAppLock(enabled: Boolean)
}
