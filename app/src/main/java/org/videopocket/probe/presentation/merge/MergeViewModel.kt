package org.videopocket.probe.presentation.merge

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import org.videopocket.probe.core.common.result.AppResult
import org.videopocket.probe.core.files.SafFileHelper
import org.videopocket.probe.domain.usecase.MergePdfsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SelectedPdfItem(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long
)

data class MergeSuccessInfo(
    val outputName: String,
    val outputSizeBytes: Long,
    val outputUri: Uri
)

data class MergeUiState(
    val selectedFiles: List<SelectedPdfItem> = emptyList(),
    val isProcessing: Boolean = false,
    val progressPercent: Int? = null,
    val progressMessage: String? = null,
    val errorMessage: String? = null,
    val successInfo: MergeSuccessInfo? = null
)

class MergeViewModel(
    private val mergePdfsUseCase: MergePdfsUseCase,
    private val safHelper: SafFileHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(MergeUiState())
    val uiState: StateFlow<MergeUiState> = _uiState.asStateFlow()

    fun addFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return

        val newItems = uris.map { uri ->
            safHelper.takePersistableUriPermission(uri)
            val name = safHelper.getDisplayName(uri)
            val size = safHelper.getFileSize(uri)
            SelectedPdfItem(uri = uri, displayName = name, sizeBytes = size)
        }

        _uiState.update { current ->
            current.copy(
                selectedFiles = current.selectedFiles + newItems,
                errorMessage = null,
                successInfo = null
            )
        }
    }

    fun removeFile(index: Int) {
        _uiState.update { current ->
            if (index in current.selectedFiles.indices) {
                val updated = current.selectedFiles.toMutableList().apply { removeAt(index) }
                current.copy(selectedFiles = updated)
            } else {
                current
            }
        }
    }

    fun moveUp(index: Int) {
        if (index <= 0) return
        _uiState.update { current ->
            val list = current.selectedFiles.toMutableList()
            val item = list.removeAt(index)
            list.add(index - 1, item)
            current.copy(selectedFiles = list)
        }
    }

    fun moveDown(index: Int) {
        _uiState.update { current ->
            if (index >= current.selectedFiles.size - 1) return@update current
            val list = current.selectedFiles.toMutableList()
            val item = list.removeAt(index)
            list.add(index + 1, item)
            current.copy(selectedFiles = list)
        }
    }

    fun startMerge(destinationUri: Uri) {
        val currentState = _uiState.value
        if (currentState.isProcessing) return // Prevent double click
        if (currentState.selectedFiles.size < 2) {
            _uiState.update { it.copy(errorMessage = "يجب اختيار ملفين على الأقل للدمج") }
            return
        }

        val destName = safHelper.getDisplayName(destinationUri)

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    progressPercent = 0,
                    progressMessage = "بدء التحضير للدمج…",
                    errorMessage = null,
                    successInfo = null
                )
            }

            val inputUris = currentState.selectedFiles.map { it.uri }
            val inputNames = currentState.selectedFiles.map { it.displayName }

            val result = mergePdfsUseCase(
                inputUris = inputUris,
                inputNames = inputNames,
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
                            successInfo = MergeSuccessInfo(
                                outputName = destName,
                                outputSizeBytes = result.data,
                                outputUri = destinationUri
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
        }
    }

    fun dismissSuccess() {
        _uiState.update { it.copy(successInfo = null, selectedFiles = emptyList()) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        fun provideFactory(
            mergeUseCase: MergePdfsUseCase,
            safHelper: SafFileHelper
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MergeViewModel(mergeUseCase, safHelper) as T
            }
        }
    }
}
