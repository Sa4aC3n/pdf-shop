package com.pdfcompress.CamScanner.domain.usecase

import com.pdfcompress.CamScanner.domain.model.AppColorTheme
import com.pdfcompress.CamScanner.domain.model.AppLanguage
import com.pdfcompress.CamScanner.domain.model.AppTheme
import com.pdfcompress.CamScanner.domain.model.UserSettings
import com.pdfcompress.CamScanner.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class GetSettingsUseCase(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<UserSettings> {
        return settingsRepository.settings
    }
}

class UpdateSettingsUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend fun setTheme(theme: AppTheme) {
        settingsRepository.updateTheme(theme)
    }

    suspend fun setColorTheme(colorTheme: AppColorTheme) {
        settingsRepository.updateColorTheme(colorTheme)
    }

    suspend fun setLanguage(language: AppLanguage) {
        settingsRepository.updateLanguage(language)
    }

    suspend fun setAppLock(enabled: Boolean) {
        settingsRepository.updateAppLock(enabled)
    }
}
