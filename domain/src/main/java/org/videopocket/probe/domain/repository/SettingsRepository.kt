package org.videopocket.probe.domain.repository

import org.videopocket.probe.domain.model.AppLanguage
import org.videopocket.probe.domain.model.AppTheme
import org.videopocket.probe.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun updateTheme(theme: AppTheme)
    suspend fun updateLanguage(language: AppLanguage)
    suspend fun updateAppLock(enabled: Boolean)
}
