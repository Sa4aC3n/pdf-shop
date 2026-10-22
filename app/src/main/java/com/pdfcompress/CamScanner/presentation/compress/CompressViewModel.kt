package com.pdfcompress.CamScanner.presentation.compress

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.core.files.SafFileHelper
import com.pdfcompress.CamScanner.domain.model.CompressionLevel
import com.pdfcompress.CamScanner.domain.model.CompressionResult
import com.pdfcompress.CamScanner.domain.usecase.CompressPdfUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CompressUiState(
    val sourceUri: Uri? = null,
    val sourceName: String? = null,
    val sourceSizeBytes: Long = 0,
    val selectedLevel: CompressionLevel = CompressionLevel.MEDIUM,
    val isProcessing: Boolean = false,
    val progressPercent: Int? = null,
    val progressMessage: String? = null,
    val errorMessage: String? = null,
    val successResult: CompressionResult? = null,
    val outputName: String? = null
)

class CompressViewModel(
    private val compressPdfUseCase: CompressPdfUseCase,
    private val safHelper: SafFileHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompressUiState())
    val uiState: StateFlow<CompressUiState> = _uiState.asStateFlow()

    fun setSourceFile(uri: Uri) {
        safHelper.takePersistableUriPermission(uri)
        val name = safHelper.getDisplayName(uri)
        val size = safHelper.getFileSize(uri)

        _uiState.update {
            it.copy(
                sourceUri = uri,
                sourceName = name,
                sourceSizeBytes = size,
                errorMessage = null,
                successResult = null
            )
        }
    }

    fun setLevel(level: CompressionLevel) {
        _uiState.update { it.copy(selectedLevel = level) }
    }

    fun startCompression(destinationUri: Uri) {
        val currentState = _uiState.value
        val srcUri = currentState.sourceUri ?: return
        val srcName = currentState.sourceName ?: "document.pdf"
        if (currentState.isProcessing) return

        val destName = safHelper.getDisplayName(destinationUri)

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    progressPercent = 0,
                    progressMessage = "بدء ضغط المستند محلياً…",
                    errorMessage = null,
                    successResult = null
                )
            }

            val result = compressPdfUseCase(
                sourceUri = srcUri,
                sourceName = srcName,
                level = currentState.selectedLevel,
                destinationUri = destinationUri,
                destinationName = destName,
                onProgress = { progress ->
                    _uiState.update {
                        it.copy(
                            progressPercent = progress.percentage,
                            progressMessage = progress.stage
                        )
                    }
                }
            )

            when (result) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            progressPercent = null,
                            progressMessage = null,
                            successResult = result.data,
                            outputName = destName
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            progressPercent = null,
                            progressMessage = null,
                            errorMessage = result.error.message
                        )
                    }
                }
                is AppResult.Progress -> {}
            }
        }
    }

    fun dismissSuccess() {
        _uiState.update { it.copy(successResult = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        fun provideFactory(
            useCase: CompressPdfUseCase,
            safHelper: SafFileHelper
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CompressViewModel(useCase, safHelper) as T
            }
        }
    }
}
