package com.pdfcompress.CamScanner.presentation.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.core.files.SafFileHelper
import com.pdfcompress.CamScanner.domain.engine.EngineProgress
import com.pdfcompress.CamScanner.domain.model.BackupReport
import com.pdfcompress.CamScanner.domain.usecase.BackupRestoreUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupRestoreUiState(
    val selectedTab: Int = 0, // 0 = Export, 1 = Restore
    val isPasswordEnabled: Boolean = true,
    val password: String = "",
    val confirmPassword: String = "",
    val restoreSourceUri: Uri? = null,
    val restorePassword: String = "",
    val isProcessing: Boolean = false,
    val progress: EngineProgress? = null,
    val exportReport: BackupReport? = null,
    val restoredCount: Int? = null,
    val errorMessage: String? = null
)

class BackupRestoreViewModel(
    private val context: Context,
    private val backupRestoreUseCase: BackupRestoreUseCase,
    private val safHelper: SafFileHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupRestoreUiState())
    val uiState: StateFlow<BackupRestoreUiState> = _uiState.asStateFlow()

    fun selectTab(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab, errorMessage = null) }
    }

    fun setPasswordEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isPasswordEnabled = enabled) }
    }

    fun setPassword(pwd: String) {
        _uiState.update { it.copy(password = pwd) }
    }

    fun setConfirmPassword(pwd: String) {
        _uiState.update { it.copy(confirmPassword = pwd) }
    }

    fun setRestoreSourceUri(uri: Uri) {
        _uiState.update { it.copy(restoreSourceUri = uri, errorMessage = null) }
    }

    fun setRestorePassword(pwd: String) {
        _uiState.update { it.copy(restorePassword = pwd) }
    }

    fun createBackup(destinationUri: Uri) {
        val state = _uiState.value
        if (state.isPasswordEnabled) {
            if (state.password.isBlank()) {
                _uiState.update { it.copy(errorMessage = "يرجى إدخال كلمة سر لحماية النسخة الاحتياطية") }
                return
            }
            if (state.password != state.confirmPassword) {
                _uiState.update { it.copy(errorMessage = "كلمتا السر غير متطابقتين") }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    progress = EngineProgress(10, "بدء إنشاء النسخة الاحتياطية…"),
                    errorMessage = null,
                    exportReport = null
                )
            }

            try {
                val destName = safHelper.getDisplayName(destinationUri)
                val destUri = destinationUri

                val finalPassword = if (state.isPasswordEnabled) state.password else null
                val result = backupRestoreUseCase.createBackup(
                    password = finalPassword,
                    destinationUri = destUri,
                    destinationName = destName,
                    onProgress = { p -> _uiState.update { it.copy(progress = p) } }
                )

                when (result) {
                    is AppResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                exportReport = result.data,
                                progress = null
                            )
                        }
                    }
                    is AppResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                errorMessage = result.error.message,
                                progress = null
                            )
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = e.message ?: "حدث خطأ أثناء إنشاء النسخة الاحتياطية",
                        progress = null
                    )
                }
            }
        }
    }

    fun restoreBackup() {
        val state = _uiState.value
        val uri = state.restoreSourceUri ?: run {
            _uiState.update { it.copy(errorMessage = "يرجى اختيار ملف النسخة الاحتياطية أولاً") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    progress = EngineProgress(10, "استعادة الأرشيف…"),
                    errorMessage = null,
                    restoredCount = null
                )
            }

            val finalPassword = if (state.restorePassword.isNotBlank()) state.restorePassword else null
            val result = backupRestoreUseCase.restoreBackup(
                sourceUri = uri,
                password = finalPassword,
                onProgress = { p -> _uiState.update { it.copy(progress = p) } }
            )

            when (result) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            restoredCount = result.data,
                            progress = null
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = result.error.message,
                            progress = null
                        )
                    }
                }
                else -> {}
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearResult() {
        _uiState.update { it.copy(exportReport = null, restoredCount = null) }
    }

    companion object {
        fun provideFactory(
            context: Context,
            backupRestoreUseCase: BackupRestoreUseCase,
            safHelper: SafFileHelper
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BackupRestoreViewModel(context, backupRestoreUseCase, safHelper) as T
            }
        }
    }
}
