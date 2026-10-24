package com.pdfcompress.CamScanner.presentation.passport

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.core.files.SafFileHelper
import com.pdfcompress.CamScanner.domain.engine.EngineProgress
import com.pdfcompress.CamScanner.domain.model.PassportMrzData
import com.pdfcompress.CamScanner.domain.usecase.PassportMrzUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PassportUiState(
    val imageUri: Uri? = null,
    val bitmap: Bitmap? = null,
    val mrzData: PassportMrzData? = null,
    val isScanning: Boolean = false,
    val isGeneratingReport: Boolean = false,
    val progress: EngineProgress? = null,
    val outputUri: Uri? = null,
    val outputName: String? = null,
    val outputSizeBytes: Long? = null,
    val errorMessage: String? = null
)

class PassportViewModel(
    private val context: Context,
    private val passportMrzUseCase: PassportMrzUseCase,
    private val safHelper: SafFileHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(PassportUiState())
    val uiState: StateFlow<PassportUiState> = _uiState.asStateFlow()

    fun processPassportImage(uri: Uri) {
        viewModelScope.launch {
            try {
                val bmp = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                }
                _uiState.update {
                    it.copy(
                        imageUri = uri,
                        bitmap = bmp,
                        isScanning = true,
                        errorMessage = null,
                        mrzData = null,
                        outputUri = null
                    )
                }

                val result = passportMrzUseCase.scanPassportImage(uri) { p ->
                    _uiState.update { it.copy(progress = p) }
                }

                when (result) {
                    is AppResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isScanning = false,
                                mrzData = result.data,
                                progress = null
                            )
                        }
                    }
                    is AppResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isScanning = false,
                                errorMessage = result.error.message,
                                progress = null
                            )
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        errorMessage = e.message ?: "تعذر قراءة صورة جواز السفر",
                        progress = null
                    )
                }
            }
        }
    }

    fun generateOfficialReport(destinationUri: Uri) {
        val state = _uiState.value
        val mrz = state.mrzData ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGeneratingReport = true,
                    progress = EngineProgress(10, "تجهيز تقرير فحص جواز السفر…"),
                    errorMessage = null
                )
            }

            try {
                val destName = safHelper.getDisplayName(destinationUri)
                val destUri = destinationUri

                val res = passportMrzUseCase.generateReport(
                    photoUri = state.imageUri,
                    mrzData = mrz,
                    destinationUri = destUri,
                    destinationName = destName,
                    onProgress = { p -> _uiState.update { it.copy(progress = p) } }
                )

                when (res) {
                    is AppResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isGeneratingReport = false,
                                outputUri = destUri,
                                outputName = destName,
                                outputSizeBytes = res.data,
                                progress = null
                            )
                        }
                    }
                    is AppResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isGeneratingReport = false,
                                errorMessage = res.error.message,
                                progress = null
                            )
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGeneratingReport = false,
                        errorMessage = e.message ?: "حدث خطأ أثناء إنشاء التقرير",
                        progress = null
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetOutput() {
        _uiState.update { it.copy(outputUri = null, outputName = null, outputSizeBytes = null) }
    }

    companion object {
        fun provideFactory(
            context: Context,
            passportMrzUseCase: PassportMrzUseCase,
            safHelper: SafFileHelper
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PassportViewModel(context, passportMrzUseCase, safHelper) as T
            }
        }
    }
}
