package com.pdfcompress.CamScanner.presentation.redact

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.core.files.SafFileHelper
import com.pdfcompress.CamScanner.domain.model.RedactionRect
import com.pdfcompress.CamScanner.domain.usecase.RedactPdfUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RedactUiState(
    val sourceUri: Uri? = null,
    val sourceName: String? = null,
    val totalPages: Int = 0,
    val selectedPageIndex: Int = 0,
    val redactions: List<RedactionRect> = emptyList(),
    val isProcessing: Boolean = false,
    val progressPercent: Int? = null,
    val progressMessage: String? = null,
    val errorMessage: String? = null,
    val successOutputName: String? = null,
    val successSizeBytes: Long = 0,
    val outputUri: Uri? = null
)

class RedactViewModel(
    private val redactUseCase: RedactPdfUseCase,
    private val safHelper: SafFileHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(RedactUiState())
    val uiState: StateFlow<RedactUiState> = _uiState.asStateFlow()
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
                redactions = emptyList(),
                selectedPageIndex = 0,
                totalPages = 0
            )
        }

        viewModelScope.launch {
            val pages = redactUseCase.getPageCount(uri)
            _uiState.update { it.copy(totalPages = pages) }
        }
    }

    fun setSelectedPage(index: Int) {
        _uiState.update { it.copy(selectedPageIndex = index) }
    }

    fun addHeaderRedaction() {
        val page = _uiState.value.selectedPageIndex
        val rect = RedactionRect(
            pageIndex = page,
            leftRatio = 0.05f,
            topRatio = 0.03f,
            widthRatio = 0.90f,
            heightRatio = 0.12f
        )
        _uiState.update { it.copy(redactions = it.redactions + rect) }
    }

    fun addFooterRedaction() {
        val page = _uiState.value.selectedPageIndex
        val rect = RedactionRect(
            pageIndex = page,
            leftRatio = 0.05f,
            topRatio = 0.85f,
            widthRatio = 0.90f,
            heightRatio = 0.12f
        )
        _uiState.update { it.copy(redactions = it.redactions + rect) }
    }

    fun addCenterRedaction() {
        val page = _uiState.value.selectedPageIndex
        val rect = RedactionRect(
            pageIndex = page,
            leftRatio = 0.15f,
            topRatio = 0.40f,
            widthRatio = 0.70f,
            heightRatio = 0.20f
        )
        _uiState.update { it.copy(redactions = it.redactions + rect) }
    }

    fun removeRedactionAt(index: Int) {
        _uiState.update { current ->
            if (index in current.redactions.indices) {
                val updated = current.redactions.toMutableList().apply { removeAt(index) }
                current.copy(redactions = updated)
            } else {
                current
            }
        }
    }

    fun clearAllRedactions() {
        _uiState.update { it.copy(redactions = emptyList()) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun redactPdf(destinationUri: Uri) {
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
                    progressMessage = "تطبيق حجب وتعتيم المناطق الحساسة…",
                    errorMessage = null
                )
            }

            try {
                val result = redactUseCase(
                    sourceUri = source,
                    sourceName = sourceName,
                    redactions = state.redactions,
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
        private val redactUseCase: RedactPdfUseCase,
        private val safHelper: SafFileHelper
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RedactViewModel(redactUseCase, safHelper) as T
        }
    }
}
