package com.pdfcompress.CamScanner.presentation.htmltopdf

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.core.files.SafFileHelper
import com.pdfcompress.CamScanner.domain.usecase.HtmlToPdfUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HtmlToPdfUiState(
    val htmlContent: String = "<h1>عنوان الصفحة</h1>\n<p>مرحباً بك! هذه فقرة نصية بتنسيق <b>عريض</b> و<i>مائل</i>.</p>\n<ul>\n  <li>عنصر رقم 1</li>\n  <li>عنصر رقم 2</li>\n</ul>",
    val sourceFileName: String? = null,
    val isProcessing: Boolean = false,
    val progressPercent: Int? = null,
    val progressMessage: String? = null,
    val errorMessage: String? = null,
    val successOutputName: String? = null,
    val successSizeBytes: Long = 0,
    val outputUri: Uri? = null
)

class HtmlToPdfViewModel(
    private val htmlToPdfUseCase: HtmlToPdfUseCase,
    private val safHelper: SafFileHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(HtmlToPdfUiState())
    val uiState: StateFlow<HtmlToPdfUiState> = _uiState.asStateFlow()
    private var processingJob: Job? = null

    fun setHtmlContent(value: String) {
        _uiState.update { it.copy(htmlContent = value) }
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
                        htmlContent = content,
                        sourceFileName = name,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "تعذر قراءة ملف HTML: ${e.message}") }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun convertToPdf(destinationUri: Uri) {
        val state = _uiState.value
        val html = state.htmlContent
        if (html.isBlank()) {
            _uiState.update { it.copy(errorMessage = "يرجى إدخال شفرة HTML أو اختيار ملف") }
            return
        }

        val destName = safHelper.getDisplayName(destinationUri)

        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    progressPercent = 0,
                    progressMessage = "توليد ملف PDF من عناصر HTML…",
                    errorMessage = null
                )
            }

            try {
                val result = htmlToPdfUseCase(
                    htmlText = html,
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
        private val htmlToPdfUseCase: HtmlToPdfUseCase,
        private val safHelper: SafFileHelper
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HtmlToPdfViewModel(htmlToPdfUseCase, safHelper) as T
        }
    }
}
