package com.pdfcompress.CamScanner.presentation.forms

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.core.files.SafFileHelper
import com.pdfcompress.CamScanner.domain.model.FormFieldInfo
import com.pdfcompress.CamScanner.domain.model.FormFillRequest
import com.pdfcompress.CamScanner.domain.usecase.PdfFormsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PdfFormsUiState(
    val pdfUri: Uri? = null,
    val pdfName: String? = null,
    val isLoadingFields: Boolean = false,
    val fields: List<FormFieldInfo> = emptyList(),
    val editedValues: Map<String, String> = emptyMap(),
    val flattenAfterFill: Boolean = false,
    val isSaving: Boolean = false,
    val progressPercent: Int? = null,
    val progressMessage: String? = null,
    val errorMessage: String? = null,
    val successOutputName: String? = null,
    val outputUri: Uri? = null
)

class PdfFormsViewModel(
    private val pdfFormsUseCase: PdfFormsUseCase,
    private val safHelper: SafFileHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfFormsUiState())
    val uiState: StateFlow<PdfFormsUiState> = _uiState.asStateFlow()
    private var saveJob: Job? = null

    fun setSourcePdf(uri: Uri) {
        safHelper.takePersistableUriPermission(uri)
        val name = safHelper.getDisplayName(uri)
        _uiState.update {
            it.copy(
                pdfUri = uri,
                pdfName = name,
                isLoadingFields = true,
                fields = emptyList(),
                editedValues = emptyMap(),
                errorMessage = null,
                successOutputName = null,
                outputUri = null
            )
        }

        viewModelScope.launch {
            val result = pdfFormsUseCase.getFields(uri)
            when (result) {
                is AppResult.Success -> {
                    val fieldMap = result.data.associate { it.name to it.value }
                    _uiState.update {
                        it.copy(
                            isLoadingFields = false,
                            fields = result.data,
                            editedValues = fieldMap
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingFields = false,
                            errorMessage = result.error.message
                        )
                    }
                }
                is AppResult.Progress -> {}
            }
        }
    }

    fun updateFieldValue(name: String, value: String) {
        _uiState.update {
            val updated = it.editedValues.toMutableMap()
            updated[name] = value
            it.copy(editedValues = updated)
        }
    }

    fun setFlatten(flatten: Boolean) {
        _uiState.update { it.copy(flattenAfterFill = flatten) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun saveFilledForm(destinationUri: Uri) {
        val state = _uiState.value
        val pdf = state.pdfUri ?: return
        val name = state.pdfName ?: "form.pdf"
        val destName = safHelper.getDisplayName(destinationUri)

        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSaving = true,
                    progressPercent = 0,
                    progressMessage = "تطبيق وحفظ الحقول في نموذج الـ PDF أوفلاين…",
                    errorMessage = null
                )
            }

            val request = FormFillRequest(
                fields = state.editedValues,
                flattenAfterFill = state.flattenAfterFill
            )

            try {
                val result = pdfFormsUseCase.fillFields(
                    pdfUri = pdf,
                    pdfName = name,
                    request = request,
                    destinationUri = destinationUri,
                    destinationName = destName,
                    onProgress = { p ->
                        _uiState.update {
                            it.copy(
                                progressPercent = p.percentage,
                                progressMessage = p.stage
                            )
                        }
                    }
                )

                when (result) {
                    is AppResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                progressPercent = null,
                                progressMessage = null,
                                successOutputName = destName,
                                outputUri = destinationUri
                            )
                        }
                    }
                    is AppResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                progressPercent = null,
                                progressMessage = null,
                                errorMessage = result.error.message
                            )
                        }
                    }
                    is AppResult.Progress -> {}
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        progressPercent = null,
                        progressMessage = null,
                        errorMessage = e.message ?: "فشلت عملية حفظ النموذج"
                    )
                }
            }
        }
    }

    class Factory(
        private val pdfFormsUseCase: PdfFormsUseCase,
        private val safHelper: SafFileHelper
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PdfFormsViewModel::class.java)) {
                return PdfFormsViewModel(pdfFormsUseCase, safHelper) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
