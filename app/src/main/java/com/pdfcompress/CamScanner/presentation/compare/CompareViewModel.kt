package com.pdfcompress.CamScanner.presentation.compare

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.core.files.SafFileHelper
import com.pdfcompress.CamScanner.domain.model.CompareResult
import com.pdfcompress.CamScanner.domain.usecase.ComparePdfUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CompareUiState(
    val uriA: Uri? = null,
    val nameA: String? = null,
    val uriB: Uri? = null,
    val nameB: String? = null,
    val isComparing: Boolean = false,
    val progressPercent: Int? = null,
    val progressMessage: String? = null,
    val errorMessage: String? = null,
    val compareResult: CompareResult? = null,
    val selectedPageIndex: Int = 0
)

class CompareViewModel(
    private val comparePdfUseCase: ComparePdfUseCase,
    private val safHelper: SafFileHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompareUiState())
    val uiState: StateFlow<CompareUiState> = _uiState.asStateFlow()
    private var compareJob: Job? = null

    fun setFileA(uri: Uri) {
        safHelper.takePersistableUriPermission(uri)
        val name = safHelper.getDisplayName(uri)
        _uiState.update {
            it.copy(
                uriA = uri,
                nameA = name,
                errorMessage = null,
                compareResult = null
            )
        }
    }

    fun setFileB(uri: Uri) {
        safHelper.takePersistableUriPermission(uri)
        val name = safHelper.getDisplayName(uri)
        _uiState.update {
            it.copy(
                uriB = uri,
                nameB = name,
                errorMessage = null,
                compareResult = null
            )
        }
    }

    fun selectPage(index: Int) {
        _uiState.update { it.copy(selectedPageIndex = index) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun startComparison() {
        val state = _uiState.value
        val a = state.uriA ?: return
        val b = state.uriB ?: return
        val nameA = state.nameA ?: "مستند 1"
        val nameB = state.nameB ?: "مستند 2"

        compareJob?.cancel()
        compareJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isComparing = true,
                    progressPercent = 0,
                    progressMessage = "بدء فحص ومقارنة نصوص المستندين أوفلاين…",
                    errorMessage = null,
                    compareResult = null,
                    selectedPageIndex = 0
                )
            }

            try {
                val result = comparePdfUseCase(
                    uriA = a,
                    nameA = nameA,
                    uriB = b,
                    nameB = nameB,
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
                                isComparing = false,
                                progressPercent = null,
                                progressMessage = null,
                                compareResult = result.data
                            )
                        }
                    }
                    is AppResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isComparing = false,
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
                        isComparing = false,
                        progressPercent = null,
                        progressMessage = null,
                        errorMessage = e.message ?: "فشلت عملية المقارنة"
                    )
                }
            }
        }
    }

    class Factory(
        private val comparePdfUseCase: ComparePdfUseCase,
        private val safHelper: SafFileHelper
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CompareViewModel::class.java)) {
                return CompareViewModel(comparePdfUseCase, safHelper) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
