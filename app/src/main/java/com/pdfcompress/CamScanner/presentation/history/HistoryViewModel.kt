package com.pdfcompress.CamScanner.presentation.history

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfcompress.CamScanner.core.files.SafFileHelper
import com.pdfcompress.CamScanner.domain.model.OperationRecord
import com.pdfcompress.CamScanner.domain.usecase.ClearHistoryUseCase
import com.pdfcompress.CamScanner.domain.usecase.GetHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryItemUi(
    val record: OperationRecord,
    val isFileAccessible: Boolean
)

data class HistoryUiState(
    val items: List<HistoryItemUi> = emptyList(),
    val isLoading: Boolean = true,
    val showClearDialog: Boolean = false
)

class HistoryViewModel(
    private val getHistoryUseCase: GetHistoryUseCase,
    private val clearHistoryUseCase: ClearHistoryUseCase,
    private val safHelper: SafFileHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        getHistoryUseCase().onEach { records ->
            val uiItems = records.map { record ->
                val accessible = if (record.outputDocumentUri != null) {
                    try {
                        safHelper.isUriAccessible(Uri.parse(record.outputDocumentUri))
                    } catch (_: Exception) {
                        false
                    }
                } else false

                HistoryItemUi(
                    record = record,
                    isFileAccessible = accessible
                )
            }
            _uiState.update { it.copy(items = uiItems, isLoading = false) }
        }.launchIn(viewModelScope)
    }

    fun showClearConfirm(show: Boolean) {
        _uiState.update { it.copy(showClearDialog = show) }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            clearHistoryUseCase()
            _uiState.update { it.copy(showClearDialog = false) }
        }
    }

    companion object {
        fun provideFactory(
            getHistory: GetHistoryUseCase,
            clearHistory: ClearHistoryUseCase,
            safHelper: SafFileHelper
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HistoryViewModel(getHistory, clearHistory, safHelper) as T
            }
        }
    }
}
