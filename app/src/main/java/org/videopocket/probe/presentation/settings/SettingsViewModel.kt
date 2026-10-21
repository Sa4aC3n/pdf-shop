package org.videopocket.probe.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import org.videopocket.probe.domain.model.AppLanguage
import org.videopocket.probe.domain.model.AppTheme
import org.videopocket.probe.domain.model.UserSettings
import org.videopocket.probe.domain.usecase.GetSettingsUseCase
import org.videopocket.probe.domain.usecase.UpdateSettingsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase
) : ViewModel() {

    val settings: StateFlow<UserSettings> = getSettingsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            updateSettingsUseCase.setTheme(theme)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            updateSettingsUseCase.setLanguage(language)
        }
    }

    fun setAppLock(enabled: Boolean) {
        viewModelScope.launch {
            updateSettingsUseCase.setAppLock(enabled)
        }
    }

    companion object {
        fun provideFactory(
            getSettings: GetSettingsUseCase,
            updateSettings: UpdateSettingsUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(getSettings, updateSettings) as T
            }
        }
    }
}
