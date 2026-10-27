package com.pdfcompress.CamScanner.presentation.sign

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.core.files.SafFileHelper
import com.pdfcompress.CamScanner.core.files.TempFileManager
import com.pdfcompress.CamScanner.domain.model.SignaturePlacement
import com.pdfcompress.CamScanner.domain.usecase.SignPdfUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.FileOutputStream

data class SignUiState(
    val pdfUri: Uri? = null,
    val pdfName: String? = null,
    val totalPages: Int = 1,
    val selectedPage: Int = 0,
    val signatureBitmapUri: Uri? = null,
    val hasDrawnSignature: Boolean = false,
    val xPercent: Float = 0.65f, // Default bottom-right
    val yPercent: Float = 0.82f,
    val widthPercent: Float = 0.30f,
    val heightPercent: Float = 0.12f,
    val isSigning: Boolean = false,
    val progressPercent: Int? = null,
    val progressMessage: String? = null,
    val errorMessage: String? = null,
    val successOutputName: String? = null,
    val outputUri: Uri? = null
)

class SignViewModel(
    private val signPdfUseCase: SignPdfUseCase,
    private val safHelper: SafFileHelper,
    private val tempFileManager: TempFileManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUiState())
    val uiState: StateFlow<SignUiState> = _uiState.asStateFlow()
    private var signJob: Job? = null

    fun setSourcePdf(uri: Uri) {
        safHelper.takePersistableUriPermission(uri)
        val name = safHelper.getDisplayName(uri)
        _uiState.update {
            it.copy(
                pdfUri = uri,
                pdfName = name,
                errorMessage = null,
                successOutputName = null,
                outputUri = null,
                totalPages = 1,
                selectedPage = 0
            )
        }

        viewModelScope.launch {
            val count = signPdfUseCase.getPageCount(uri)
            if (count > 0) {
                _uiState.update { it.copy(totalPages = count) }
            }
        }
    }

    fun setSelectedPage(page: Int) {
        _uiState.update { it.copy(selectedPage = page.coerceIn(0, (it.totalPages - 1).coerceAtLeast(0))) }
    }

    fun setPlacementPreset(preset: String) {
        when (preset) {
            "bottom_right" -> _uiState.update { it.copy(xPercent = 0.65f, yPercent = 0.82f, widthPercent = 0.30f, heightPercent = 0.12f) }
            "bottom_left" -> _uiState.update { it.copy(xPercent = 0.05f, yPercent = 0.82f, widthPercent = 0.30f, heightPercent = 0.12f) }
            "center" -> _uiState.update { it.copy(xPercent = 0.35f, yPercent = 0.45f, widthPercent = 0.30f, heightPercent = 0.12f) }
            "top_right" -> _uiState.update { it.copy(xPercent = 0.65f, yPercent = 0.08f, widthPercent = 0.30f, heightPercent = 0.12f) }
        }
    }

    fun setCustomPlacement(x: Float, y: Float, w: Float, h: Float) {
        _uiState.update {
            it.copy(
                xPercent = x.coerceIn(0f, 0.85f),
                yPercent = y.coerceIn(0f, 0.85f),
                widthPercent = w.coerceIn(0.10f, 0.60f),
                heightPercent = h.coerceIn(0.04f, 0.30f)
            )
        }
    }

    fun setSignatureImageUri(uri: Uri) {
        safHelper.takePersistableUriPermission(uri)
        _uiState.update {
            it.copy(
                signatureBitmapUri = uri,
                hasDrawnSignature = true,
                errorMessage = null
            )
        }
    }

    fun setDrawnSignatureBitmap(bitmap: Bitmap) {
        try {
            val tempImg = tempFileManager.createTempImageFile("png")
            FileOutputStream(tempImg).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val uri = Uri.fromFile(tempImg)
            _uiState.update {
                it.copy(
                    signatureBitmapUri = uri,
                    hasDrawnSignature = true,
                    errorMessage = null
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "تعذر حفظ التوقيع المرسوم: ${e.message}") }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun signDocument(destinationUri: Uri) {
        val state = _uiState.value
        val pdf = state.pdfUri ?: return
        val sigUri = state.signatureBitmapUri ?: run {
            _uiState.update { it.copy(errorMessage = "يرجى رسم أو اختيار صورة التوقيع أولاً") }
            return
        }
        val name = state.pdfName ?: "document.pdf"
        val destName = safHelper.getDisplayName(destinationUri)

        signJob?.cancel()
        signJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSigning = true,
                    progressPercent = 0,
                    progressMessage = "دمج التوقيع الإلكتروني داخل مستند الـ PDF أوفلاين…",
                    errorMessage = null
                )
            }

            val placement = SignaturePlacement(
                pageIndex = state.selectedPage,
                xPercent = state.xPercent,
                yPercent = state.yPercent,
                widthPercent = state.widthPercent,
                heightPercent = state.heightPercent
            )

            try {
                val result = signPdfUseCase(
                    pdfUri = pdf,
                    pdfName = name,
                    signatureBitmapUri = sigUri,
                    placement = placement,
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
                                isSigning = false,
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
                                isSigning = false,
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
                        isSigning = false,
                        progressPercent = null,
                        progressMessage = null,
                        errorMessage = e.message ?: "فشلت عملية التوقيع"
                    )
                }
            }
        }
    }

    class Factory(
        private val signPdfUseCase: SignPdfUseCase,
        private val safHelper: SafFileHelper,
        private val tempFileManager: TempFileManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SignViewModel::class.java)) {
                return SignViewModel(signPdfUseCase, safHelper, tempFileManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
