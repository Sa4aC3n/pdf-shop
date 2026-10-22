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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val saveSuccessMessage: String? = null
)

class ExtractTextViewModel(
    private val useCase: ExtractTextUseCase,
    private val safHelper: SafFileHelper,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExtractTextUiState())
    val uiState: StateFlow<ExtractTextUiState> = _uiState.asStateFlow()

    fun setSourceFile(uri: Uri) {
        safHelper.takePersistableUriPermission(uri)
        val name = safHelper.getDisplayName(uri)

        viewModelScope.launch {
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
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun copyToClipboard() {
        val text = _uiState.value.extractionResult?.text ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Extracted PDF Text", text)
        clipboard.setPrimaryClip(clip)
        _uiState.update { it.copy(copiedToClipboard = true) }
    }

    fun saveAsTextFile(destinationUri: Uri) {
        val text = _uiState.value.extractionResult?.text ?: return
        viewModelScope.launch {
            try {
                context.contentResolver.openOutputStream(destinationUri, "wt")?.use { out ->
                    out.write(text.toByteArray(Charsets.UTF_8))
                }
                _uiState.update { it.copy(saveSuccessMessage = "تم حفظ الملف النصي بنجاح") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "تعذر حفظ الملف النصي: ${e.localizedMessage}") }
            }
        }
    }

    fun dismissClipboardNotification() {
        _uiState.update { it.copy(copiedToClipboard = false) }
    }

    fun dismissSaveSuccess() {
        _uiState.update { it.copy(saveSuccessMessage = null) }
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
