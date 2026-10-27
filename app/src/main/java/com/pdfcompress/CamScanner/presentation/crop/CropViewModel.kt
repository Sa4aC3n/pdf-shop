package com.pdfcompress.CamScanner.presentation.crop

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.core.files.SafFileHelper
import com.pdfcompress.CamScanner.domain.model.CropMargins
import com.pdfcompress.CamScanner.domain.usecase.CropPdfUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CropUiState(
    val sourceUri: Uri? = null,
    val sourceName: String? = null,
    val totalPages: Int = 0,
    val topMarginPt: Float = 20f,
    val bottomMarginPt: Float = 20f,
    val leftMarginPt: Float = 20f,
    val rightMarginPt: Float = 20f,
    val isProcessing: Boolean = false,
    val progressPercent: Int? = null,
    val progressMessage: String? = null,
    val errorMessage: String? = null,
    val successOutputName: String? = null,
    val successSizeBytes: Long = 0,
    val outputUri: Uri? = null
)

class CropViewModel(
    private val cropUseCase: CropPdfUseCase,
    private val safHelper: SafFileHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(CropUiState())
    val uiState: StateFlow<CropUiState> = _uiState.asStateFlow()
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
                outputUri = null,
                totalPages = 0
            )
        }

        viewModelScope.launch {
            val pages = cropUseCase.getPageCount(uri)
            _uiState.update { it.copy(totalPages = pages) }
        }
    }

    fun setAllMargins(points: Float) {
        _uiState.update {
            it.copy(
                topMarginPt = points,
                bottomMarginPt = points,
                leftMarginPt = points,
                rightMarginPt = points
            )
        }
    }

    fun setTopMargin(points: Float) { _uiState.update { it.copy(topMarginPt = points) } }
    fun setBottomMargin(points: Float) { _uiState.update { it.copy(bottomMarginPt = points) } }
    fun setLeftMargin(points: Float) { _uiState.update { it.copy(leftMarginPt = points) } }
    fun setRightMargin(points: Float) { _uiState.update { it.copy(rightMarginPt = points) } }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun cropPdf(destinationUri: Uri) {
        val state = _uiState.value
        val source = state.sourceUri ?: return
        val sourceName = state.sourceName ?: "document.pdf"
        val destName = safHelper.getDisplayName(destinationUri)

        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    progressPercent = 0,
                    progressMessage = "بدء قص هوامش صفحات المستند…",
                    errorMessage = null
                )
            }

            try {
                val margins = CropMargins(
                    topPt = state.topMarginPt,
                    bottomPt = state.bottomMarginPt,
                    leftPt = state.leftMarginPt,
                    rightPt = state.rightMarginPt
                )

                val result = cropUseCase(
                    sourceUri = source,
                    sourceName = sourceName,
                    margins = margins,
                    destinationUri = destinationUri,
                    destinationName = destName
                ) { progress ->
                    _uiState.update {
                        it.copy(
                            progressPercent = progress.percentage,
                            progressMessage = progress.stage
                        )
                    }
                }

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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        progressPercent = null,
                        progressMessage = null,
                        errorMessage = e.message ?: "حدث خطأ غير متوقع"
                    )
                }
            }
        }
    }

    class Factory(
        private val cropUseCase: CropPdfUseCase,
        private val safHelper: SafFileHelper
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CropViewModel(cropUseCase, safHelper) as T
        }
    }
}
