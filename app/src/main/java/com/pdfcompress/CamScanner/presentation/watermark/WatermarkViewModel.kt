package com.pdfcompress.CamScanner.presentation.watermark

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.core.files.SafFileHelper
import com.pdfcompress.CamScanner.domain.model.WatermarkOptions
import com.pdfcompress.CamScanner.domain.usecase.AddWatermarkUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WatermarkUiState(
    val sourceUri: Uri? = null,
    val sourceName: String? = null,
    val watermarkText: String = "سري للغاية",
    val fontSize: Float = 42f,
    val opacity: Float = 0.25f,
    val rotation: Float = 45f,
    val isProcessing: Boolean = false,
    val progressPercent: Int? = null,
    val progressMessage: String? = null,
    val errorMessage: String? = null,
    val lastDestinationUri: Uri? = null,
    val successOutputName: String? = null,
    val successSizeBytes: Long = 0,
    val outputUri: Uri? = null
)

class WatermarkViewModel(
    private val watermarkUseCase: AddWatermarkUseCase,
    private val safHelper: SafFileHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(WatermarkUiState())
    val uiState: StateFlow<WatermarkUiState> = _uiState.asStateFlow()
    private var processingJob: Job? = null

    fun setSourceFile(uri: Uri) {
        safHelper.takePersistableUriPermission(uri)
        val name = safHelper.getDisplayName(uri)
        _uiState.update {
            it.copy(
                sourceUri = uri,
                sourceName = name,
                errorMessage = null,
                successOutputName = null,
                outputUri = null
            )
        }
    }

    fun setText(text: String) {
        _uiState.update { it.copy(watermarkText = text) }
    }

    fun setFontSize(size: Float) {
        _uiState.update { it.copy(fontSize = size) }
    }

    fun setOpacity(opacity: Float) {
        _uiState.update { it.copy(opacity = opacity) }
    }

    fun setRotation(rotation: Float) {
        _uiState.update { it.copy(rotation = rotation) }
    }

    fun startWatermarking(destinationUri: Uri) {
        val currentState = _uiState.value
        val srcUri = currentState.sourceUri ?: return
        val srcName = currentState.sourceName ?: "document.pdf"
        if (currentState.isProcessing) return

        val destName = safHelper.getDisplayName(destinationUri)
        val options = WatermarkOptions(
            text = currentState.watermarkText,
            fontSize = currentState.fontSize,
            opacity = currentState.opacity,
            rotationDegrees = currentState.rotation
        )

        processingJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    progressPercent = 0,
                    progressMessage = "بدء إضافة العلامة المائية محلياً…",
                    errorMessage = null,
                    lastDestinationUri = destinationUri,
                    successOutputName = null,
                    outputUri = null
                )
            }

            try {
                val result = watermarkUseCase(
                    sourceUri = srcUri,
                    sourceName = srcName,
                    options = options,
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
                                successOutputName = destName,
                                successSizeBytes = result.data,
                                outputUri = destinationUri
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
            } catch (_: CancellationException) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        progressPercent = null,
                        progressMessage = null,
                        errorMessage = "تم إلغاء إضافة العلامة المائية بناءً على طلبك"
                    )
                }
            }
        }
    }

    fun cancelProcessing() {
        processingJob?.cancel()
        processingJob = null
    }

    fun retryLastWatermarking() {
        val dest = _uiState.value.lastDestinationUri
        if (dest != null) {
            startWatermarking(dest)
        } else {
            dismissError()
        }
    }

    fun dismissSuccess() {
        _uiState.update { it.copy(successOutputName = null, outputUri = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        fun provideFactory(
            useCase: AddWatermarkUseCase,
            safHelper: SafFileHelper
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WatermarkViewModel(useCase, safHelper) as T
            }
        }
    }
}
