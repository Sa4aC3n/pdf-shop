package com.pdfcompress.CamScanner.presentation.markdowntopdf

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.core.files.SafFileHelper
import com.pdfcompress.CamScanner.domain.usecase.MarkdownToPdfUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MarkdownToPdfUiState(
    val markdownContent: String = "# عنوان المستند\n\nهذا نص تجريبي يمكن كتابته بصيغة **ماركداون** الأنيقة.\n\n## قسم فرعي\n\n* عنصر قائمة أول\n* عنصر قائمة ثاني\n* عنصر قائمة ثالث\n\nنص عادي مع تنسيق *مائل* و **عريض**.",
    val sourceFileName: String? = null,
    val isProcessing: Boolean = false,
    val progressPercent: Int? = null,
    val progressMessage: String? = null,
    val errorMessage: String? = null,
    val successOutputName: String? = null,
    val successSizeBytes: Long = 0,
    val outputUri: Uri? = null
)

class MarkdownToPdfViewModel(
    private val markdownToPdfUseCase: MarkdownToPdfUseCase,
    private val safHelper: SafFileHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarkdownToPdfUiState())
    val uiState: StateFlow<MarkdownToPdfUiState> = _uiState.asStateFlow()
    private var processingJob: Job? = null

    fun setMarkdownContent(value: String) {
        _uiState.update { it.copy(markdownContent = value) }
    }

    fun loadFromFile(uri: Uri) {
        safHelper.takePersistableUriPermission(uri)
        val name = safHelper.getDisplayName(uri)
        viewModelScope.launch {
            try {
                val stream = safHelper.openInputStream(uri)
                val content = stream?.bufferedReader()?.use { it.readText() } ?: ""
                _uiState.update {
                    it.copy(
                        markdownContent = content,
                        sourceFileName = name,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "تعذر قراءة ملف الماركداون: ${e.message}") }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun convertToPdf(destinationUri: Uri) {
        val state = _uiState.value
        val md = state.markdownContent
        if (md.isBlank()) {
            _uiState.update { it.copy(errorMessage = "يرجى إدخال شفرة ماركداون أو تحميل ملف .md") }
            return
        }

        val destName = safHelper.getDisplayName(destinationUri)

        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    progressPercent = 0,
                    progressMessage = "توليد ملف PDF من شفرة الماركداون…",
                    errorMessage = null
                )
            }

            try {
                val result = markdownToPdfUseCase(
                    markdownText = md,
                    sourceTitle = state.sourceFileName,
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
        private val markdownToPdfUseCase: MarkdownToPdfUseCase,
        private val safHelper: SafFileHelper
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MarkdownToPdfViewModel(markdownToPdfUseCase, safHelper) as T
        }
    }
}
