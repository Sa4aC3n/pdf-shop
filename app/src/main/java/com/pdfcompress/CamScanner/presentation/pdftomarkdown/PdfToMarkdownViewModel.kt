package com.pdfcompress.CamScanner.presentation.pdftomarkdown

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.core.files.SafFileHelper
import com.pdfcompress.CamScanner.domain.usecase.PdfToMarkdownUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PdfToMarkdownUiState(
    val sourceUri: Uri? = null,
    val sourceName: String? = null,
    val totalPages: Int = 0,
    val isProcessing: Boolean = false,
    val progressPercent: Int? = null,
    val progressMessage: String? = null,
    val errorMessage: String? = null,
    val successOutputName: String? = null,
    val successSizeBytes: Long = 0,
    val outputUri: Uri? = null
)

class PdfToMarkdownViewModel(
    private val pdfToMarkdownUseCase: PdfToMarkdownUseCase,
    private val safHelper: SafFileHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfToMarkdownUiState())
    val uiState: StateFlow<PdfToMarkdownUiState> = _uiState.asStateFlow()
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
            val pages = pdfToMarkdownUseCase.getPageCount(uri)
            _uiState.update { it.copy(totalPages = pages) }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun convertToMarkdown(destinationUri: Uri) {
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
                    progressMessage = "استخراج وهيكلة نصوص الـ PDF إلى ماركداون…",
                    errorMessage = null
                )
            }

            try {
                val result = pdfToMarkdownUseCase(
                    sourceUri = source,
                    sourceName = sourceName,
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
        private val pdfToMarkdownUseCase: PdfToMarkdownUseCase,
        private val safHelper: SafFileHelper
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PdfToMarkdownViewModel(pdfToMarkdownUseCase, safHelper) as T
        }
    }
}
