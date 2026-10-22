package com.pdfcompress.CamScanner.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfcompress.CamScanner.domain.model.AppLanguage
import com.pdfcompress.CamScanner.domain.model.AppTheme
import com.pdfcompress.CamScanner.domain.model.UserSettings
import com.pdfcompress.CamScanner.domain.usecase.GetSettingsUseCase
import com.pdfcompress.CamScanner.domain.usecase.UpdateSettingsUseCase
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
