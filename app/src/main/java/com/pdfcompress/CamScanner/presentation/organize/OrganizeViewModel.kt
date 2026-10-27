package com.pdfcompress.CamScanner.presentation.organize

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.core.files.SafFileHelper
import com.pdfcompress.CamScanner.domain.usecase.OrganizePagesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrganizeUiState(
    val sourceUri: Uri? = null,
    val sourceName: String? = null,
    val totalOriginalPages: Int = 0,
    val pageOrder: List<Int> = emptyList(), // 0-based page indices
    val isProcessing: Boolean = false,
    val progressPercent: Int? = null,
    val progressMessage: String? = null,
    val errorMessage: String? = null,
    val successOutputName: String? = null,
    val successSizeBytes: Long = 0,
    val outputUri: Uri? = null
)

class OrganizeViewModel(
    private val organizeUseCase: OrganizePagesUseCase,
    private val safHelper: SafFileHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrganizeUiState())
    val uiState: StateFlow<OrganizeUiState> = _uiState.asStateFlow()
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
                pageOrder = emptyList(),
                totalOriginalPages = 0
            )
        }

        viewModelScope.launch {
            val count = organizeUseCase.getPageCount(uri)
            _uiState.update {
                it.copy(
                    totalOriginalPages = count,
                    pageOrder = (0 until count).toList()
                )
            }
        }
    }

    fun removePageAt(position: Int) {
        _uiState.update { current ->
            if (position in current.pageOrder.indices && current.pageOrder.size > 1) {
                val updated = current.pageOrder.toMutableList().apply { removeAt(position) }
                current.copy(pageOrder = updated)
            } else {
                current
            }
        }
    }

    fun movePageLeft(position: Int) {
        _uiState.update { current ->
            if (position > 0 && position < current.pageOrder.size) {
                val updated = current.pageOrder.toMutableList()
                val temp = updated[position]
                updated[position] = updated[position - 1]
                updated[position - 1] = temp
                current.copy(pageOrder = updated)
            } else {
                current
            }
        }
    }

    fun movePageRight(position: Int) {
        _uiState.update { current ->
            if (position >= 0 && position < current.pageOrder.size - 1) {
                val updated = current.pageOrder.toMutableList()
                val temp = updated[position]
                updated[position] = updated[position + 1]
                updated[position + 1] = temp
                current.copy(pageOrder = updated)
            } else {
                current
            }
        }
    }

    fun duplicatePageAt(position: Int) {
        _uiState.update { current ->
            if (position in current.pageOrder.indices) {
                val updated = current.pageOrder.toMutableList()
                updated.add(position + 1, updated[position])
                current.copy(pageOrder = updated)
            } else {
                current
            }
        }
    }

    fun resetOrder() {
        _uiState.update { current ->
            current.copy(pageOrder = (0 until current.totalOriginalPages).toList())
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun saveOrganizedPdf(destinationUri: Uri) {
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
                    progressMessage = "بدء معالجة وترتيب صفحات المستند…",
                    errorMessage = null
                )
            }

            try {
                val result = organizeUseCase(
                    sourceUri = source,
                    sourceName = sourceName,
                    pageOrder = state.pageOrder,
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
        private val organizeUseCase: OrganizePagesUseCase,
        private val safHelper: SafFileHelper
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OrganizeViewModel(organizeUseCase, safHelper) as T
        }
    }
}
