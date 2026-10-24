package com.pdfcompress.CamScanner.presentation.idcard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.core.files.DocumentImageEnhancer
import com.pdfcompress.CamScanner.core.files.SafFileHelper
import com.pdfcompress.CamScanner.core.files.ScanFilter
import com.pdfcompress.CamScanner.core.files.TempFileManager
import com.pdfcompress.CamScanner.domain.engine.EngineProgress
import com.pdfcompress.CamScanner.domain.model.IdCardLayoutMode
import com.pdfcompress.CamScanner.domain.usecase.IdCardUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class IdCardUiState(
    val frontUri: Uri? = null,
    val backUri: Uri? = null,
    val frontBitmap: Bitmap? = null,
    val backBitmap: Bitmap? = null,
    val layoutMode: IdCardLayoutMode = IdCardLayoutMode.A4_VERTICAL,
    val selectedFilter: ScanFilter = ScanFilter.ORIGINAL,
    val isProcessing: Boolean = false,
    val progress: EngineProgress? = null,
    val outputUri: Uri? = null,
    val outputName: String? = null,
    val outputSizeBytes: Long? = null,
    val errorMessage: String? = null
)

class IdCardViewModel(
    private val context: Context,
    private val idCardUseCase: IdCardUseCase,
    private val safHelper: SafFileHelper,
    private val tempFileManager: TempFileManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(IdCardUiState())
    val uiState: StateFlow<IdCardUiState> = _uiState.asStateFlow()

    private var rawFrontBitmap: Bitmap? = null
    private var rawBackBitmap: Bitmap? = null
    private var frontRotationDegrees: Float = 0f
    private var backRotationDegrees: Float = 0f

    fun setFrontImage(uri: Uri) {
        viewModelScope.launch {
            try {
                val bmp = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                }
                rawFrontBitmap = bmp
                frontRotationDegrees = 0f
                val enhanced = bmp?.let { DocumentImageEnhancer.applyFilter(it, _uiState.value.selectedFilter) }
                _uiState.update {
                    it.copy(
                        frontUri = uri,
                        frontBitmap = enhanced,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "تعذر قراءة صورة الوجه المحددة") }
            }
        }
    }

    fun setBackImage(uri: Uri) {
        viewModelScope.launch {
            try {
                val bmp = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                }
                rawBackBitmap = bmp
                backRotationDegrees = 0f
                val enhanced = bmp?.let { DocumentImageEnhancer.applyFilter(it, _uiState.value.selectedFilter) }
                _uiState.update {
                    it.copy(
                        backUri = uri,
                        backBitmap = enhanced,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "تعذر قراءة صورة الظهر المحددة") }
            }
        }
    }

    fun rotateFront() {
        val raw = rawFrontBitmap ?: return
        frontRotationDegrees = (frontRotationDegrees + 90f) % 360f
        val rotated = DocumentImageEnhancer.rotate(raw, frontRotationDegrees)
        val filtered = DocumentImageEnhancer.applyFilter(rotated, _uiState.value.selectedFilter)
        _uiState.update { it.copy(frontBitmap = filtered) }
    }

    fun rotateBack() {
        val raw = rawBackBitmap ?: return
        backRotationDegrees = (backRotationDegrees + 90f) % 360f
        val rotated = DocumentImageEnhancer.rotate(raw, backRotationDegrees)
        val filtered = DocumentImageEnhancer.applyFilter(rotated, _uiState.value.selectedFilter)
        _uiState.update { it.copy(backBitmap = filtered) }
    }

    fun setFilter(filter: ScanFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
        rawFrontBitmap?.let { raw ->
            val rotated = DocumentImageEnhancer.rotate(raw, frontRotationDegrees)
            val filtered = DocumentImageEnhancer.applyFilter(rotated, filter)
            _uiState.update { it.copy(frontBitmap = filtered) }
        }
        rawBackBitmap?.let { raw ->
            val rotated = DocumentImageEnhancer.rotate(raw, backRotationDegrees)
            val filtered = DocumentImageEnhancer.applyFilter(rotated, filter)
            _uiState.update { it.copy(backBitmap = filtered) }
        }
    }

    fun setLayoutMode(mode: IdCardLayoutMode) {
        _uiState.update { it.copy(layoutMode = mode) }
    }

    fun removeFront() {
        rawFrontBitmap = null
        _uiState.update { it.copy(frontUri = null, frontBitmap = null) }
    }

    fun removeBack() {
        rawBackBitmap = null
        _uiState.update { it.copy(backUri = null, backBitmap = null) }
    }

    fun exportAsPdf(destinationUri: Uri) {
        processAndSaveTo(destinationUri, asPdf = true)
    }

    fun exportAsImage(destinationUri: Uri) {
        processAndSaveTo(destinationUri, asPdf = false)
    }

    private fun processAndSaveTo(destinationUri: Uri, asPdf: Boolean) {
        val state = _uiState.value
        val frontBmp = state.frontBitmap
        val backBmp = state.backBitmap

        if (frontBmp == null && backBmp == null) {
            _uiState.update { it.copy(errorMessage = "يرجى مسح أو إضافة صورة واحدة على الأقل للبطاقة") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, progress = EngineProgress(5, "تحضير ملفات البطاقة…"), errorMessage = null) }

            // Save enhanced bitmaps to temp files
            var tempFrontUri: Uri? = null
            var tempBackUri: Uri? = null

            try {
                if (frontBmp != null) {
                    val f = tempFileManager.createTempPdfFile("front_card.jpg")
                    FileOutputStream(f).use { frontBmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                    tempFrontUri = Uri.fromFile(f)
                }
                if (backBmp != null) {
                    val f = tempFileManager.createTempPdfFile("back_card.jpg")
                    FileOutputStream(f).use { backBmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                    tempBackUri = Uri.fromFile(f)
                }

                val destName = safHelper.getDisplayName(destinationUri)

                if (asPdf) {

                    val destUri = destinationUri


                    val res = idCardUseCase.createPdf(
                        frontCardUri = tempFrontUri,
                        backCardUri = tempBackUri,
                        layoutMode = state.layoutMode,
                        destinationUri = destUri,
                        destinationName = destName,
                        onProgress = { p -> _uiState.update { it.copy(progress = p) } }
                    )

                    when (res) {
                        is AppResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    isProcessing = false,
                                    outputUri = destUri,
                                    outputName = destName,
                                    outputSizeBytes = res.data.outputSizeBytes,
                                    progress = null
                                )
                            }
                        }
                        is AppResult.Error -> {
                            _uiState.update { it.copy(isProcessing = false, errorMessage = res.error.message, progress = null) }
                        }
                        else -> {}
                    }
                } else {

                    val destUri = destinationUri


                    val res = idCardUseCase.createImage(
                        frontCardUri = tempFrontUri,
                        backCardUri = tempBackUri,
                        layoutMode = state.layoutMode,
                        destinationUri = destUri,
                        destinationName = destName,
                        onProgress = { p -> _uiState.update { it.copy(progress = p) } }
                    )

                    when (res) {
                        is AppResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    isProcessing = false,
                                    outputUri = destUri,
                                    outputName = destName,
                                    outputSizeBytes = res.data,
                                    progress = null
                                )
                            }
                        }
                        is AppResult.Error -> {
                            _uiState.update { it.copy(isProcessing = false, errorMessage = res.error.message, progress = null) }
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, errorMessage = e.message ?: "حدث خطأ أثناء معالجة البطاقة", progress = null) }
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
            idCardUseCase: IdCardUseCase,
            safHelper: SafFileHelper,
            tempFileManager: TempFileManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return IdCardViewModel(context, idCardUseCase, safHelper, tempFileManager) as T
            }
        }
    }
}
