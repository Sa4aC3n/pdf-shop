package com.pdfcompress.CamScanner.presentation.metadata

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.core.files.SafFileHelper
import com.pdfcompress.CamScanner.domain.model.PdfMetadata
import com.pdfcompress.CamScanner.domain.usecase.PdfMetadataUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MetadataUiState(
    val sourceUri: Uri? = null,
    val sourceName: String? = null,
    val title: String = "",
    val author: String = "",
    val subject: String = "",
    val keywords: String = "",
    val creator: String = "",
    val producer: String = "",
    val pageCount: Int = 0,
    val isLoadingMetadata: Boolean = false,
    val isProcessing: Boolean = false,
    val progressPercent: Int? = null,
    val progressMessage: String? = null,
    val errorMessage: String? = null,
    val successOutputName: String? = null,
    val successSizeBytes: Long = 0,
    val outputUri: Uri? = null
)

class MetadataViewModel(
    private val metadataUseCase: PdfMetadataUseCase,
    private val safHelper: SafFileHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(MetadataUiState())
    val uiState: StateFlow<MetadataUiState> = _uiState.asStateFlow()
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
                isLoadingMetadata = true
            )
        }

        viewModelScope.launch {
            when (val result = metadataUseCase.readMetadata(uri)) {
                is AppResult.Success -> {
                    val meta = result.data
                    _uiState.update {
                        it.copy(
                            title = meta.title ?: "",
                            author = meta.author ?: "",
                            subject = meta.subject ?: "",
                            keywords = meta.keywords ?: "",
                            creator = meta.creator ?: "",
                            producer = meta.producer ?: "",
                            pageCount = meta.pageCount,
                            isLoadingMetadata = false
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingMetadata = false,
                            errorMessage = result.error.message
                        )
                    }
                }
                is AppResult.Progress -> {}
            }
        }
    }

    fun setTitle(value: String) { _uiState.update { it.copy(title = value) } }
    fun setAuthor(value: String) { _uiState.update { it.copy(author = value) } }
    fun setSubject(value: String) { _uiState.update { it.copy(subject = value) } }
    fun setKeywords(value: String) { _uiState.update { it.copy(keywords = value) } }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun saveMetadata(destinationUri: Uri, clearAll: Boolean) {
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
                    progressMessage = if (clearAll) "محو كافة البيانات الوصفية الحساسة…" else "تحديث وحفظ البيانات الوصفية…",
                    errorMessage = null
                )
            }

            try {
                val result = metadataUseCase.updateMetadata(
                    sourceUri = source,
                    sourceName = sourceName,
                    title = state.title.ifBlank { null },
                    author = state.author.ifBlank { null },
                    subject = state.subject.ifBlank { null },
                    keywords = state.keywords.ifBlank { null },
                    clearAll = clearAll,
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
        private val metadataUseCase: PdfMetadataUseCase,
        private val safHelper: SafFileHelper
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MetadataViewModel(metadataUseCase, safHelper) as T
        }
    }
}
