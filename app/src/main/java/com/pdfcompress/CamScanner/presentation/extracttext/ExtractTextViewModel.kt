package com.pdfcompress.CamScanner.presentation.extracttext

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.core.files.SafFileHelper
import com.pdfcompress.CamScanner.domain.model.TextExtractionResult
import com.pdfcompress.CamScanner.domain.usecase.ExtractTextUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ExtractTextUiState(
    val sourceUri: Uri? = null,
    val sourceName: String? = null,
    val isProcessing: Boolean = false,
    val progressPercent: Int? = null,
    val progressMessage: String? = null,
    val errorMessage: String? = null,
    val extractionResult: TextExtractionResult? = null,
    val searchQuery: String = "",
    val copiedToClipboard: Boolean = false,
    val saveSuccessMessage: String? = null,
    val savedTextUri: Uri? = null
)

class ExtractTextViewModel(
    private val useCase: ExtractTextUseCase,
    private val safHelper: SafFileHelper,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExtractTextUiState())
    val uiState: StateFlow<ExtractTextUiState> = _uiState.asStateFlow()
    private var processingJob: Job? = null

    fun setSourceFile(uri: Uri) {
        safHelper.takePersistableUriPermission(uri)
        val name = safHelper.getDisplayName(uri)

        processingJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    sourceUri = uri,
                    sourceName = name,
                    isProcessing = true,
                    progressPercent = 0,
                    progressMessage = "بدء قراءة وفهرسة نصوص المستند…",
                    errorMessage = null,
                    extractionResult = null
                )
            }

            try {
                val result = useCase(
                    sourceUri = uri,
                    sourceName = name,
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
                                extractionResult = result.data
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
                        errorMessage = "تم إلغاء عملية استخراج النصوص بناءً على طلبك."
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
                errorMessage = "تم إلغاء عملية استخراج النصوص."
            )
        }
    }

    fun retryExtraction() {
        val uri = _uiState.value.sourceUri
        if (uri != null) {
            setSourceFile(uri)
        } else {
            dismissError()
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun copyToClipboard() {
        val text = _uiState.value.extractionResult?.text ?: return
        if (text.isBlank()) {
            _uiState.update { it.copy(errorMessage = "لا يوجد نص مستخرج لنسخه") }
            return
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Extracted PDF Text", text)
        clipboard.setPrimaryClip(clip)
        _uiState.update { it.copy(copiedToClipboard = true) }
    }

    fun saveAsTextFile(destinationUri: Uri) {
        val text = _uiState.value.extractionResult?.text ?: return
        if (text.isBlank()) {
            _uiState.update { it.copy(errorMessage = "لا يوجد نص مستخرج لحفظه") }
            return
        }
        viewModelScope.launch {
            try {
                val written = withContext(Dispatchers.IO) {
                    val out = context.contentResolver.openOutputStream(destinationUri, "wt")
                        ?: return@withContext false
                    out.use { stream ->
                        val bytes = text.toByteArray(Charsets.UTF_8)
                        stream.write(bytes)
                        stream.flush()
                    }
                    safHelper.isUriAccessible(destinationUri)
                }
                if (written) {
                    _uiState.update {
                        it.copy(
                            saveSuccessMessage = "تم حفظ الملف النصي بنجاح (${text.length} حرفاً)",
                            savedTextUri = destinationUri
                        )
                    }
                } else {
                    _uiState.update { it.copy(errorMessage = "تعذر فتح مسار الحفظ المختار لكتابة الملف النصي") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "تعذر حفظ الملف النصي: ${e.localizedMessage}") }
            }
        }
    }

    fun dismissClipboardNotification() {
        _uiState.update { it.copy(copiedToClipboard = false) }
    }

    fun dismissSaveSuccess() {
        _uiState.update { it.copy(saveSuccessMessage = null, savedTextUri = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        fun provideFactory(
            useCase: ExtractTextUseCase,
            safHelper: SafFileHelper,
            context: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ExtractTextViewModel(useCase, safHelper, context) as T
            }
        }
    }
}
