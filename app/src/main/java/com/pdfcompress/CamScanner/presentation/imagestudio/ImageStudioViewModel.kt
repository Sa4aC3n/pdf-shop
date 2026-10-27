package com.pdfcompress.CamScanner.presentation.imagestudio

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.core.files.SafFileHelper
import com.pdfcompress.CamScanner.domain.model.ImageOutputFormat
import com.pdfcompress.CamScanner.domain.model.ImageProcessOptions
import com.pdfcompress.CamScanner.domain.usecase.ImageStudioUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImageStudioUiState(
    val sourceUri: Uri? = null,
    val sourceName: String? = null,
    val sourceSizeBytes: Long = 0,
    val isCompressMode: Boolean = false,
    val selectedFormat: ImageOutputFormat = ImageOutputFormat.WEBP,
    val qualityPercent: Int = 80,
    val scalePercent: Int = 100,
    val isProcessing: Boolean = false,
    val progressPercent: Int? = null,
    val progressMessage: String? = null,
    val errorMessage: String? = null,
    val successOutputName: String? = null,
    val successSizeBytes: Long = 0,
    val outputUri: Uri? = null
)

class ImageStudioViewModel(
    private val imageStudioUseCase: ImageStudioUseCase,
    private val safHelper: SafFileHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageStudioUiState())
    val uiState: StateFlow<ImageStudioUiState> = _uiState.asStateFlow()
    private var processingJob: Job? = null

    fun setSourceImage(uri: Uri) {
        safHelper.takePersistableUriPermission(uri)
        val name = safHelper.getDisplayName(uri)
        val size = safHelper.getFileSize(uri)
        _uiState.update {
            it.copy(
                sourceUri = uri,
                sourceName = name,
                sourceSizeBytes = size,
                errorMessage = null,
                successOutputName = null,
                outputUri = null
            )
        }
    }

    fun setMode(isCompress: Boolean) {
        _uiState.update { it.copy(isCompressMode = isCompress) }
    }

    fun setFormat(format: ImageOutputFormat) {
        _uiState.update { it.copy(selectedFormat = format) }
    }

    fun setQuality(quality: Int) {
        _uiState.update { it.copy(qualityPercent = quality.coerceIn(5, 100)) }
    }

    fun setScale(scale: Int) {
        _uiState.update { it.copy(scalePercent = scale.coerceIn(10, 100)) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun processImage(destinationUri: Uri) {
        val state = _uiState.value
        val source = state.sourceUri ?: return
        val sourceName = state.sourceName ?: "image"
        val destName = safHelper.getDisplayName(destinationUri)

        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    progressPercent = 0,
                    progressMessage = if (state.isCompressMode) "جاري ضغط وتصغير الصورة بدون إنترنت…" else "جاري تحويل صيغة الصورة…",
                    errorMessage = null
                )
            }

            val options = ImageProcessOptions(
                outputFormat = state.selectedFormat,
                qualityPercent = state.qualityPercent,
                scalePercent = state.scalePercent
            )

            try {
                val result = imageStudioUseCase(
                    sourceUri = source,
                    sourceName = sourceName,
                    options = options,
                    destinationUri = destinationUri,
                    destinationName = destName,
                    isCompressMode = state.isCompressMode,
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
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        progressPercent = null,
                        progressMessage = null,
                        errorMessage = e.message ?: "فشلت العملية"
                    )
                }
            }
        }
    }

    class Factory(
        private val imageStudioUseCase: ImageStudioUseCase,
        private val safHelper: SafFileHelper
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ImageStudioViewModel::class.java)) {
                return ImageStudioViewModel(imageStudioUseCase, safHelper) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
