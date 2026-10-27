package com.pdfcompress.CamScanner.presentation.rotate

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.core.files.SafFileHelper
import com.pdfcompress.CamScanner.domain.usecase.RotatePdfUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RotateUiState(
    val sourceUri: Uri? = null,
    val sourceName: String? = null,
    val totalPages: Int = 0,
    val rotationAngle: Int = 90, // 90, 180, 270
    val applyToAllPages: Boolean = true,
    val pageRangeText: String = "",
    val isProcessing: Boolean = false,
    val progressPercent: Int? = null,
    val progressMessage: String? = null,
    val errorMessage: String? = null,
    val successOutputName: String? = null,
    val successSizeBytes: Long = 0,
    val outputUri: Uri? = null
)

class RotateViewModel(
    private val rotateUseCase: RotatePdfUseCase,
    private val safHelper: SafFileHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(RotateUiState())
    val uiState: StateFlow<RotateUiState> = _uiState.asStateFlow()
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
            val pages = rotateUseCase.getPageCount(uri)
            _uiState.update { it.copy(totalPages = pages) }
        }
    }

    fun setRotationAngle(angle: Int) {
        _uiState.update { it.copy(rotationAngle = angle) }
    }

    fun setApplyToAllPages(all: Boolean) {
        _uiState.update { it.copy(applyToAllPages = all) }
    }

    fun setPageRangeText(text: String) {
        _uiState.update { it.copy(pageRangeText = text) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun rotate(destinationUri: Uri) {
        val state = _uiState.value
        val source = state.sourceUri ?: return
        val sourceName = state.sourceName ?: "input.pdf"
        val destName = safHelper.getDisplayName(destinationUri)

        val selectedPages: List<Int>? = if (state.applyToAllPages) {
            null
        } else {
            parsePageRange(state.pageRangeText, state.totalPages)
        }

        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    progressPercent = 0,
                    progressMessage = "بدء تدوير صفحات الـ PDF…",
                    errorMessage = null
                )
            }

            try {
                val result = rotateUseCase(
                    sourceUri = source,
                    sourceName = sourceName,
                    rotationDegrees = state.rotationAngle,
                    selectedPages = selectedPages,
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

    private fun parsePageRange(rangeStr: String, maxPages: Int): List<Int> {
        val pages = mutableSetOf<Int>()
        val parts = rangeStr.split(",", " ").filter { it.isNotBlank() }
        for (part in parts) {
            if (part.contains("-")) {
                val bounds = part.split("-").mapNotNull { it.trim().toIntOrNull() }
                if (bounds.size == 2) {
                    val start = bounds[0].coerceAtLeast(1)
                    val end = bounds[1].coerceAtMost(maxPages)
                    for (p in start..end) pages.add(p)
                }
            } else {
                part.trim().toIntOrNull()?.let {
                    if (it in 1..maxPages) pages.add(it)
                }
            }
        }
        return if (pages.isEmpty()) (1..maxPages).toList() else pages.sorted()
    }

    class Factory(
        private val rotateUseCase: RotatePdfUseCase,
        private val safHelper: SafFileHelper
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RotateViewModel(rotateUseCase, safHelper) as T
        }
    }
}
