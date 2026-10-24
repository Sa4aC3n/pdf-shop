package com.pdfcompress.CamScanner.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pdfcompress.CamScanner.domain.model.AppColorTheme
import com.pdfcompress.CamScanner.domain.model.AppLanguage
import com.pdfcompress.CamScanner.domain.model.AppTheme
import com.pdfcompress.CamScanner.domain.model.UserSettings
import com.pdfcompress.CamScanner.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "pdf_shop_settings")

class SettingsRepositoryImpl(
    private val context: Context
) : SettingsRepository {

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("app_theme")
        val COLOR_THEME = stringPreferencesKey("app_color_theme")
        val LANGUAGE = stringPreferencesKey("app_language")
        val APP_LOCK = booleanPreferencesKey("app_lock")
    }

    override val settings: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        val themeString = preferences[PreferencesKeys.THEME] ?: AppTheme.LIGHT.name
        val colorThemeString = preferences[PreferencesKeys.COLOR_THEME] ?: AppColorTheme.EMERALD_TEAL.name
        val langString = preferences[PreferencesKeys.LANGUAGE] ?: AppLanguage.ARABIC.name
        val lockEnabled = preferences[PreferencesKeys.APP_LOCK] ?: false

        val theme = try { AppTheme.valueOf(themeString) } catch (_: Exception) { AppTheme.LIGHT }
        val colorTheme = try { AppColorTheme.valueOf(colorThemeString) } catch (_: Exception) { AppColorTheme.EMERALD_TEAL }
        val language = try { AppLanguage.valueOf(langString) } catch (_: Exception) { AppLanguage.ARABIC }

        UserSettings(
            theme = theme,
            colorTheme = colorTheme,
            language = language,
            isAppLockEnabled = lockEnabled
        )
    }

    override suspend fun updateTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme.name
        }
    }

    override suspend fun updateColorTheme(colorTheme: AppColorTheme) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.COLOR_THEME] = colorTheme.name
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
