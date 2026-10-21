package org.videopocket.probe.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import org.videopocket.probe.domain.model.AppLanguage
import org.videopocket.probe.domain.model.AppTheme
import org.videopocket.probe.domain.model.UserSettings
import org.videopocket.probe.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "pdf_shop_settings")

class SettingsRepositoryImpl(
    private val context: Context
) : SettingsRepository {

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("app_theme")
        val LANGUAGE = stringPreferencesKey("app_language")
        val APP_LOCK = booleanPreferencesKey("app_lock")
    }

    override val settings: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        val themeString = preferences[PreferencesKeys.THEME] ?: AppTheme.SYSTEM.name
        val langString = preferences[PreferencesKeys.LANGUAGE] ?: AppLanguage.ARABIC.name
        val lockEnabled = preferences[PreferencesKeys.APP_LOCK] ?: false

        val theme = try { AppTheme.valueOf(themeString) } catch (_: Exception) { AppTheme.SYSTEM }
        val language = try { AppLanguage.valueOf(langString) } catch (_: Exception) { AppLanguage.ARABIC }

        UserSettings(
            theme = theme,
            language = language,
            isAppLockEnabled = lockEnabled
        )
    }

    override suspend fun updateTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme.name
        }
    }

    override suspend fun updateLanguage(language: AppLanguage) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = language.name
        }
    }

    override suspend fun updateAppLock(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LOCK] = enabled
        }
    }
}
