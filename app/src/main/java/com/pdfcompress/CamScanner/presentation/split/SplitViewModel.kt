package com.pdfcompress.CamScanner.presentation.split

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.core.files.SafFileHelper
import com.pdfcompress.CamScanner.domain.usecase.SplitPdfUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SplitSuccessInfo(
    val outputName: String,
    val outputSizeBytes: Long,
    val outputUri: Uri,
    val extractedPagesCount: Int
)

data class SplitUiState(
    val sourceUri: Uri? = null,
    val sourceName: String? = null,
    val totalPages: Int = 0,
    val selectedPages: Set<Int> = emptySet(),
    val isReadingPages: Boolean = false,
    val isProcessing: Boolean = false,
    val progressPercent: Int? = null,
    val progressMessage: String? = null,
    val errorMessage: String? = null,
    val lastDestinationUri: Uri? = null,
    val successInfo: SplitSuccessInfo? = null
)

class SplitViewModel(
    private val splitPdfUseCase: SplitPdfUseCase,
    private val safHelper: SafFileHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplitUiState())
    val uiState: StateFlow<SplitUiState> = _uiState.asStateFlow()
    private var processingJob: Job? = null

    fun setSourceFile(uri: Uri) {
        safHelper.takePersistableUriPermission(uri)
        val name = safHelper.getDisplayName(uri)

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    sourceUri = uri,
                    sourceName = name,
                    isReadingPages = true,
                    selectedPages = emptySet(),
                    errorMessage = null,
                    successInfo = null
                )
            }

            val pageCount = splitPdfUseCase.getPageCount(uri)

            _uiState.update {
                if (pageCount <= 0) {
                    it.copy(
                        isReadingPages = false,
                        totalPages = 0,
                        errorMessage = "تعذر قراءة صفحات المستند أو المستند فارغ"
                    )
                } else {
                    it.copy(
                        isReadingPages = false,
                        totalPages = pageCount,
                        selectedPages = setOf(1)
                    )
                }
            }
        }
    }

    fun togglePage(pageNumber: Int) {
        _uiState.update { current ->
            val updated = if (current.selectedPages.contains(pageNumber)) {
                current.selectedPages - pageNumber
            } else {
                current.selectedPages + pageNumber
            }
            current.copy(selectedPages = updated)
        }
    }

    fun selectAll() {
        val total = _uiState.value.totalPages
        if (total > 0) {
            _uiState.update { it.copy(selectedPages = (1..total).toSet()) }
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedPages = emptySet()) }
    }

    fun startExtraction(destinationUri: Uri) {
        val currentState = _uiState.value
        val sourceUri = currentState.sourceUri
        val sourceName = currentState.sourceName ?: "source.pdf"

        if (currentState.isProcessing) return
        if (sourceUri == null || currentState.selectedPages.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "يجب تحديد صفحة واحدة على الأقل للاستخراج") }
            return
        }

        val destName = safHelper.getDisplayName(destinationUri)
        val sortedPages = currentState.selectedPages.toList().sorted()

        processingJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    progressPercent = 0,
                    progressMessage = "بدء استخراج الصفحات…",
                    errorMessage = null,
                    lastDestinationUri = destinationUri,
                    successInfo = null
                )
            }

            try {
                val result = splitPdfUseCase(
                    sourceUri = sourceUri,
                    sourceName = sourceName,
                    selectedPages = sortedPages,
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
                                successInfo = SplitSuccessInfo(
                                    outputName = destName,
                                    outputSizeBytes = result.data,
                                    outputUri = destinationUri,
                                    extractedPagesCount = sortedPages.size
                                )
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
                        errorMessage = "تم إلغاء عملية استخراج الصفحات بناءً على طلبك"
                    )
                }
            }
        }
    }

    fun cancelProcessing() {
        processingJob?.cancel()
        processingJob = null
    }

    fun retryLastExtraction() {
        val dest = _uiState.value.lastDestinationUri
        if (dest != null) {
            startExtraction(dest)
        } else {
            dismissError()
        }
    }

    fun dismissSuccess() {
        _uiState.update { it.copy(successInfo = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        fun provideFactory(
            useCase: SplitPdfUseCase,
            safHelper: SafFileHelper
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SplitViewModel(useCase, safHelper) as T
            }
        }
    }
}
