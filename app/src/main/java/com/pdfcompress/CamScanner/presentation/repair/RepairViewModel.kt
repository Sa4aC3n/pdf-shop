package com.pdfcompress.CamScanner.presentation.repair

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.core.files.SafFileHelper
import com.pdfcompress.CamScanner.domain.model.RepairReport
import com.pdfcompress.CamScanner.domain.usecase.RepairPdfUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RepairUiState(
    val sourceUri: Uri? = null,
    val sourceName: String? = null,
    val sourceSizeBytes: Long = 0,
    val isProcessing: Boolean = false,
    val progressPercent: Int? = null,
    val progressMessage: String? = null,
    val errorMessage: String? = null,
    val repairReport: RepairReport? = null,
    val outputName: String? = null,
    val outputUri: Uri? = null
)

class RepairViewModel(
    private val useCase: RepairPdfUseCase,
    private val safHelper: SafFileHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepairUiState())
    val uiState: StateFlow<RepairUiState> = _uiState.asStateFlow()
    private var processingJob: Job? = null
    private var lastDestinationUri: Uri? = null

    fun setSourceFile(uri: Uri) {
        safHelper.takePersistableUriPermission(uri)
        val name = safHelper.getDisplayName(uri)
        val size = safHelper.getFileSize(uri)

        _uiState.update {
            it.copy(
                sourceUri = uri,
                sourceName = name,
                sourceSizeBytes = size,
                errorMessage = null,
                repairReport = null
            )
        }
    }

    fun startRepair(destinationUri: Uri) {
        val currentState = _uiState.value
        val srcUri = currentState.sourceUri ?: return
        val srcName = currentState.sourceName ?: "damaged.pdf"
        if (currentState.isProcessing) return

        lastDestinationUri = destinationUri
        val destName = safHelper.getDisplayName(destinationUri)

        processingJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    progressPercent = 0,
                    progressMessage = "بدء فحص البايتات التالفة…",
                    errorMessage = null,
                    repairReport = null,
                    outputUri = null
                )
            }

            try {
                val result = useCase(
                    sourceUri = srcUri,
                    sourceName = srcName,
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
                                repairReport = result.data,
                                outputName = destName,
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
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        progressPercent = null,
                        progressMessage = null,
                        errorMessage = "تم إلغاء عملية الإصلاح بناءً على طلبك."
                    )
                }
            }
        }
    }

    fun cancelProcessing() {
        processingJob?.cancel()
        processingJob = null
        _uiState.update {
            it.copy(
                isProcessing = false,
                progressPercent = null,
                progressMessage = null,
                errorMessage = "تم إلغاء عملية الإصلاح."
            )
        }
    }

    fun retryLastRepair() {
        val dest = lastDestinationUri
        if (dest != null && _uiState.value.sourceUri != null) {
            startRepair(dest)
        } else {
            dismissError()
        }
    }

    fun dismissSuccess() {
        _uiState.update { it.copy(repairReport = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        fun provideFactory(
            useCase: RepairPdfUseCase,
            safHelper: SafFileHelper
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RepairViewModel(useCase, safHelper) as T
            }
        }
    }
}
