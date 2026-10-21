package org.videopocket.probe.domain.usecase

import org.videopocket.probe.domain.model.AppLanguage
import org.videopocket.probe.domain.model.AppTheme
import org.videopocket.probe.domain.model.UserSettings
import org.videopocket.probe.domain.repository.SettingsRepository
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

    suspend fun setLanguage(language: AppLanguage) {
        settingsRepository.updateLanguage(language)
    }

    suspend fun setAppLock(enabled: Boolean) {
        settingsRepository.updateAppLock(enabled)
    }
}
