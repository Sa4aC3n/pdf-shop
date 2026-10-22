package com.pdfcompress.CamScanner.presentation.imagestopdf

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.core.files.SafFileHelper
import com.pdfcompress.CamScanner.domain.usecase.ImagesToPdfUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SelectedImageItem(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long
)

data class ImagesToPdfSuccessInfo(
    val outputName: String,
    val outputSizeBytes: Long,
    val outputUri: Uri
)

data class ImagesToPdfUiState(
    val selectedImages: List<SelectedImageItem> = emptyList(),
    val isProcessing: Boolean = false,
    val progressPercent: Int? = null,
    val progressMessage: String? = null,
    val errorMessage: String? = null,
    val successInfo: ImagesToPdfSuccessInfo? = null
)

class ImagesToPdfViewModel(
    private val imagesToPdfUseCase: ImagesToPdfUseCase,
    private val safHelper: SafFileHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImagesToPdfUiState())
    val uiState: StateFlow<ImagesToPdfUiState> = _uiState.asStateFlow()

    fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return

        val newItems = uris.map { uri ->
            safHelper.takePersistableUriPermission(uri)
            val name = safHelper.getDisplayName(uri)
            val size = safHelper.getFileSize(uri)
            SelectedImageItem(uri = uri, displayName = name, sizeBytes = size)
        }

        _uiState.update { current ->
            current.copy(
                selectedImages = current.selectedImages + newItems,
                errorMessage = null,
                successInfo = null
            )
        }
    }

    fun removeImage(index: Int) {
        _uiState.update { current ->
            if (index in current.selectedImages.indices) {
                val updated = current.selectedImages.toMutableList().apply { removeAt(index) }
                current.copy(selectedImages = updated)
            } else current
        }
    }

    fun moveUp(index: Int) {
        if (index <= 0) return
        _uiState.update { current ->
            val list = current.selectedImages.toMutableList()
            val item = list.removeAt(index)
            list.add(index - 1, item)
            current.copy(selectedImages = list)
        }
    }

    fun moveDown(index: Int) {
        _uiState.update { current ->
            if (index >= current.selectedImages.size - 1) return@update current
            val list = current.selectedImages.toMutableList()
            val item = list.removeAt(index)
            list.add(index + 1, item)
            current.copy(selectedImages = list)
        }
    }

    fun startConversion(destinationUri: Uri) {
        val currentState = _uiState.value
        if (currentState.isProcessing) return
        if (currentState.selectedImages.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "يجب اختيار صورة واحدة على الأقل") }
            return
        }

        val destName = safHelper.getDisplayName(destinationUri)

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    progressPercent = 0,
                    progressMessage = "بدء معالجة الصور…",
                    errorMessage = null,
                    successInfo = null
                )
            }

            val imageUris = currentState.selectedImages.map { it.uri }
            val imageNames = currentState.selectedImages.map { it.displayName }

            val result = imagesToPdfUseCase(
                imageUris = imageUris,
                imageNames = imageNames,
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
                            successInfo = ImagesToPdfSuccessInfo(
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
        _uiState.update { it.copy(successInfo = null, selectedImages = emptyList()) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        fun provideFactory(
            useCase: ImagesToPdfUseCase,
            safHelper: SafFileHelper
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ImagesToPdfViewModel(useCase, safHelper) as T
            }
        }
    }
}
