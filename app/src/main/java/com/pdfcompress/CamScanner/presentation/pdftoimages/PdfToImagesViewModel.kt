package com.pdfcompress.CamScanner.presentation.pdftoimages

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.core.files.SafFileHelper
import com.pdfcompress.CamScanner.domain.model.ExportImagesResult
import com.pdfcompress.CamScanner.domain.usecase.PdfToImagesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PdfToImagesUiState(
    val sourceUri: Uri? = null,
    val sourceName: String? = null,
    val totalPages: Int = 0,
    val selectedPages: Set<Int> = emptySet(),
    val isReadingPages: Boolean = false,
    val isProcessing: Boolean = false,
    val progressPercent: Int? = null,
    val progressMessage: String? = null,
    val errorMessage: String? = null,
    val successResult: ExportImagesResult? = null,
    val outputName: String? = null
)

class PdfToImagesViewModel(
    private val useCase: PdfToImagesUseCase,
    private val safHelper: SafFileHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfToImagesUiState())
    val uiState: StateFlow<PdfToImagesUiState> = _uiState.asStateFlow()

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
                    successResult = null
                )
            }

            val count = useCase.getPageCount(uri)
            _uiState.update {
                if (count <= 0) {
                    it.copy(
                        isReadingPages = false,
                        totalPages = 0,
                        errorMessage = "تعذر قراءة صفحات المستند أو المستند فارغ"
                    )
                } else {
                    it.copy(
                        isReadingPages = false,
                        totalPages = count,
                        selectedPages = (1..count).toSet() // Select all by default
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

    fun startExport(destinationUri: Uri) {
        val currentState = _uiState.value
        val srcUri = currentState.sourceUri ?: return
        val srcName = currentState.sourceName ?: "document.pdf"
        if (currentState.isProcessing) return
        if (currentState.selectedPages.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "يجب اختيار صفحة واحدة على الأقل للتصدير") }
            return
        }

        val destName = safHelper.getDisplayName(destinationUri)
        val sortedPages = currentState.selectedPages.toList().sorted()

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    progressPercent = 0,
                    progressMessage = "بدء تصدير الصفحات كصور…",
                    errorMessage = null,
                    successResult = null
                )
            }

            val result = useCase(
                sourceUri = srcUri,
                sourceName = srcName,
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
            useCase: PdfToImagesUseCase,
            safHelper: SafFileHelper
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PdfToImagesViewModel(useCase, safHelper) as T
            }
        }
    }
}
