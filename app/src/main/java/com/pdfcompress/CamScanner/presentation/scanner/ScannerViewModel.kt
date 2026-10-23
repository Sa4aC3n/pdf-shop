package com.pdfcompress.CamScanner.presentation.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.core.files.DocumentImageEnhancer
import com.pdfcompress.CamScanner.core.files.SafFileHelper
import com.pdfcompress.CamScanner.core.files.ScanFilter
import com.pdfcompress.CamScanner.core.files.TempFileManager
import com.pdfcompress.CamScanner.domain.usecase.ScanDocumentUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class ScannedPage(
    val id: String = UUID.randomUUID().toString(),
    val rawFile: File,
    val rotationDegrees: Float = 0f,
    val filter: ScanFilter = ScanFilter.ORIGINAL
)

data class ScannerUiState(
    val capturedPages: List<ScannedPage> = emptyList(),
    val activePageIndex: Int = 0,
    val isCameraMode: Boolean = true,
    val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    val isProcessing: Boolean = false,
    val progressPercent: Int? = null,
    val progressMessage: String? = null,
    val errorMessage: String? = null,
    val successSizeBytes: Long? = null,
    val outputName: String? = null
)

class ScannerViewModel(
    private val scanDocumentUseCase: ScanDocumentUseCase,
    private val safHelper: SafFileHelper,
    private val tempFileManager: TempFileManager,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun onPageCaptured(file: File) {
        val newPage = ScannedPage(rawFile = file)
        _uiState.update { current ->
            val updatedList = current.capturedPages + newPage
            current.copy(
                capturedPages = updatedList,
                activePageIndex = updatedList.size - 1,
                isCameraMode = false
            )
        }
    }

    fun onImageImported(uri: Uri) {
        viewModelScope.launch {
            val tempFile = tempFileManager.createTempImageFile("jpg")
            val success = tempFileManager.copyUriToTempFile(uri, tempFile)
            if (success) {
                onPageCaptured(tempFile)
            } else {
                _uiState.update { it.copy(errorMessage = "تعذر قراءة الصورة المستوردة") }
            }
        }
    }

    fun setActivePageFilter(filter: ScanFilter) {
        val state = _uiState.value
        val index = state.activePageIndex
        if (index in state.capturedPages.indices) {
            val updated = state.capturedPages.toMutableList()
            updated[index] = updated[index].copy(filter = filter)
            _uiState.update { it.copy(capturedPages = updated) }
        }
    }

    fun rotateActivePage() {
        val state = _uiState.value
        val index = state.activePageIndex
        if (index in state.capturedPages.indices) {
            val updated = state.capturedPages.toMutableList()
            val newRotation = (updated[index].rotationDegrees + 90f) % 360f
            updated[index] = updated[index].copy(rotationDegrees = newRotation)
            _uiState.update { it.copy(capturedPages = updated) }
        }
    }

    fun removePage(index: Int) {
        val state = _uiState.value
        if (index in state.capturedPages.indices) {
            val updated = state.capturedPages.toMutableList()
            val removed = updated.removeAt(index)
            removed.rawFile.delete()

            val newIndex = if (updated.isEmpty()) 0 else (index.coerceAtMost(updated.size - 1))
            val backToCamera = updated.isEmpty()
            _uiState.update {
                it.copy(
                    capturedPages = updated,
                    activePageIndex = newIndex,
                    isCameraMode = backToCamera
                )
            }
        }
    }

    fun switchToCamera() {
        _uiState.update { it.copy(isCameraMode = true) }
    }

    fun switchToReview(index: Int = -1) {
        _uiState.update { current ->
            val targetIdx = if (index >= 0) index else current.activePageIndex
            current.copy(
                isCameraMode = false,
                activePageIndex = targetIdx.coerceIn(0, (current.capturedPages.size - 1).coerceAtLeast(0))
            )
        }
    }

    fun toggleFlash() {
        _uiState.update {
            val nextFlash = when (it.flashMode) {
                ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                else -> ImageCapture.FLASH_MODE_OFF
            }
            it.copy(flashMode = nextFlash)
        }
    }

    fun saveAsPdf(destinationUri: Uri) {
        val pages = _uiState.value.capturedPages
        if (pages.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "يجب مسح صفحة واحدة على الأقل قبل الحفظ") }
            return
        }

        val destName = safHelper.getDisplayName(destinationUri)

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    progressPercent = 0,
                    progressMessage = "تطبيق فلاتر المستندات وتجهيز الصفحات…",
                    errorMessage = null,
                    successSizeBytes = null
                )
            }

            val processedUris = mutableListOf<Uri>()
            val tempProcessedFiles = mutableListOf<File>()

            try {
                withContext(Dispatchers.IO) {
                    pages.forEachIndexed { i, page ->
                        _uiState.update {
                            it.copy(
                                progressPercent = (i * 30 / pages.size),
                                progressMessage = "معالجة وتحسين الصفحة ${i + 1} من ${pages.size}…"
                            )
                        }

                        var bitmap: Bitmap = BitmapFactory.decodeFile(page.rawFile.absolutePath)
                        if (page.rotationDegrees % 360f != 0f) {
                            bitmap = DocumentImageEnhancer.rotate(bitmap, page.rotationDegrees)
                        }
                        if (page.filter != ScanFilter.ORIGINAL) {
                            bitmap = DocumentImageEnhancer.applyFilter(bitmap, page.filter)
                        }

                        val outFile = tempFileManager.createTempImageFile("jpg")
                        tempProcessedFiles.add(outFile)
                        FileOutputStream(outFile).use { fos ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
                        }
                        processedUris.add(Uri.fromFile(outFile))
                    }
                }

                val pageNames = pages.mapIndexed { idx, _ -> "scanned_page_${idx + 1}.jpg" }
                val result = scanDocumentUseCase(
                    scannedPageUris = processedUris,
                    pageNames = pageNames,
                    destinationUri = destinationUri,
                    destinationName = destName,
                    onProgress = { progress ->
                        _uiState.update {
                            it.copy(
                                progressPercent = 30 + (progress.percentage * 70 / 100),
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
                                successSizeBytes = result.data,
                                outputName = destName
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
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        progressPercent = null,
                        progressMessage = null,
                        errorMessage = "حدث خطأ أثناء معالجة المستند: ${e.localizedMessage}"
                    )
                }
            } finally {
                tempProcessedFiles.forEach { it.delete() }
            }
        }
    }

    fun dismissSuccess() {
        _uiState.update { it.copy(successSizeBytes = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        fun provideFactory(
            scanDocumentUseCase: ScanDocumentUseCase,
            safHelper: SafFileHelper,
            tempFileManager: TempFileManager,
            context: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ScannerViewModel(scanDocumentUseCase, safHelper, tempFileManager, context) as T
            }
        }
    }
}
