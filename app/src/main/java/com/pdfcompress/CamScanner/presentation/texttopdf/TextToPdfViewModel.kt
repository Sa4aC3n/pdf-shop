package com.pdfcompress.CamScanner.presentation.texttopdf

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.core.files.SafFileHelper
import com.pdfcompress.CamScanner.domain.usecase.TextToPdfUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TextToPdfUiState(
    val title: String = "",
    val textContent: String = "",
    val sourceFileName: String? = null,
    val isProcessing: Boolean = false,
    val progressPercent: Int? = null,
    val progressMessage: String? = null,
    val errorMessage: String? = null,
    val successOutputName: String? = null,
    val successSizeBytes: Long = 0,
    val outputUri: Uri? = null
)

class TextToPdfViewModel(
    private val textToPdfUseCase: TextToPdfUseCase,
    private val safHelper: SafFileHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(TextToPdfUiState())
    val uiState: StateFlow<TextToPdfUiState> = _uiState.asStateFlow()
    private var processingJob: Job? = null

    fun setTitle(value: String) { _uiState.update { it.copy(title = value) } }
    fun setTextContent(value: String) { _uiState.update { it.copy(textContent = value) } }

    fun loadFromFile(uri: Uri) {
        safHelper.takePersistableUriPermission(uri)
        val name = safHelper.getDisplayName(uri)
        viewModelScope.launch {
            try {
                val stream = safHelper.openInputStream(uri)
                val content = stream?.bufferedReader()?.use { it.readText() } ?: ""
                _uiState.update {
                    it.copy(
                        textContent = content,
                        title = it.title.ifBlank { name.substringBeforeLast(".") },
                        sourceFileName = name,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "تعذر قراءة محتوى الملف: ${e.message}") }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun convertToPdf(destinationUri: Uri) {
        val state = _uiState.value
        val text = state.textContent
        if (text.isBlank()) {
            _uiState.update { it.copy(errorMessage = "يرجى إدخال نص أو اختيار ملف نصي أولاً") }
            return
        }

        val destName = safHelper.getDisplayName(destinationUri)

        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    progressPercent = 0,
                    progressMessage = "تنسيق وتحويل النص إلى PDF…",
                    errorMessage = null
                )
            }

            try {
                val result = textToPdfUseCase(
                    text = text,
                    title = state.title.ifBlank { null },
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
        private val textToPdfUseCase: TextToPdfUseCase,
        private val safHelper: SafFileHelper
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TextToPdfViewModel(textToPdfUseCase, safHelper) as T
        }
    }
}
