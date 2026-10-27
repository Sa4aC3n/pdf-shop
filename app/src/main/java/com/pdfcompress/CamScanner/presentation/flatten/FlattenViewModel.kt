package com.pdfcompress.CamScanner.presentation.flatten

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.core.files.SafFileHelper
import com.pdfcompress.CamScanner.domain.usecase.FlattenPdfUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FlattenUiState(
    val sourceUri: Uri? = null,
    val sourceName: String? = null,
    val isProcessing: Boolean = false,
    val progressPercent: Int? = null,
    val progressMessage: String? = null,
    val errorMessage: String? = null,
    val successOutputName: String? = null,
    val successSizeBytes: Long = 0,
    val outputUri: Uri? = null
)

class FlattenViewModel(
    private val flattenUseCase: FlattenPdfUseCase,
    private val safHelper: SafFileHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlattenUiState())
    val uiState: StateFlow<FlattenUiState> = _uiState.asStateFlow()
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

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun flatten(destinationUri: Uri) {
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
                    progressMessage = "بدء تسطيح النماذج والتعليقات…",
                    errorMessage = null
                )
            }

            try {
                val result = flattenUseCase(
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
        private val flattenUseCase: FlattenPdfUseCase,
        private val safHelper: SafFileHelper
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FlattenViewModel(flattenUseCase, safHelper) as T
        }
    }
}
