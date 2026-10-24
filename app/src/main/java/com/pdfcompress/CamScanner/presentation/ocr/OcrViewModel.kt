package com.pdfcompress.CamScanner.presentation.ocr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.core.files.SafFileHelper
import com.pdfcompress.CamScanner.core.files.TempFileManager
import com.pdfcompress.CamScanner.domain.usecase.OcrUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class OcrUiState(
    val sourceUri: Uri? = null,
    val sourceName: String? = null,
    val isPdf: Boolean = false,
    val previewBitmap: Bitmap? = null,
    val isProcessing: Boolean = false,
    val progressPercent: Int? = null,
    val progressMessage: String? = null,
    val errorMessage: String? = null,
    val extractedText: String? = null,
    val wordCount: Int = 0,
    val characterCount: Int = 0,
    val searchQuery: String = "",
    val copiedToClipboard: Boolean = false,
    val saveSuccessMessage: String? = null,
    val savedTextUri: Uri? = null
)

class OcrViewModel(
    private val ocrUseCase: OcrUseCase,
    private val safHelper: SafFileHelper,
    private val tempFileManager: TempFileManager,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(OcrUiState())
    val uiState: StateFlow<OcrUiState> = _uiState.asStateFlow()
    private var processingJob: Job? = null

    fun setSourceFile(uri: Uri) {
        safHelper.takePersistableUriPermission(uri)
        val name = safHelper.getDisplayName(uri)
        val mimeType = safHelper.getMimeType(uri) ?: ""
        val isPdf = name.lowercase().endsWith(".pdf") || mimeType.equals("application/pdf", ignoreCase = true)

        processingJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    sourceUri = uri,
                    sourceName = name,
                    isPdf = isPdf,
                    isProcessing = true,
                    progressPercent = 10,
                    progressMessage = "قراءة ومعاينة المستند…",
                    errorMessage = null,
                    extractedText = null
                )
            }

            try {
                var preview: Bitmap? = null
                withContext(Dispatchers.IO) {
                    try {
                        if (isPdf) {
                            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                                val renderer = PdfRenderer(pfd)
                                if (renderer.pageCount > 0) {
                                    val page = renderer.openPage(0)
                                    val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    page.close()
                                    preview = bmp
                                }
                                renderer.close()
                            }
                        } else {
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                preview = BitmapFactory.decodeStream(stream)
                            }
                        }
                    } catch (_: Exception) {}
                }

                _uiState.update {
                    it.copy(
                        previewBitmap = preview,
                        progressPercent = 20,
                        progressMessage = if (isPdf) "بدء استخراج النصوص الرقمية من PDF…" else "تجهيز الصورة للتعرف البصري…"
                    )
                }

                val result = ocrUseCase(
                    sourceUri = uri,
                    sourceName = name,
                    isPdf = isPdf,
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
                        val data = result.data
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                progressPercent = null,
                                progressMessage = null,
                                extractedText = data.text,
                                wordCount = data.wordCount,
                                characterCount = data.characterCount
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
                        errorMessage = "تم إلغاء عملية التحليل بناءً على طلبك."
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
                errorMessage = "تم إلغاء عملية التحليل."
            )
        }
    }

    fun retryAnalysis() {
        val uri = _uiState.value.sourceUri
        if (uri != null) {
            setSourceFile(uri)
        } else {
            dismissError()
        }
    }

    fun openWithGoogleLens(ctx: Context) {
        val uri = _uiState.value.sourceUri ?: return
        val mime = if (_uiState.value.isPdf) "application/pdf" else "image/*"
        try {
            val lensIntent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                setPackage("com.google.ar.lens")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (lensIntent.resolveActivity(ctx.packageManager) != null) {
                ctx.startActivity(lensIntent)
            } else {
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mime
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(
                    Intent.createChooser(fallbackIntent, "التعرف الضوئي عبر تطبيق خارجي").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "تعذر فتح عدسة Google: ${e.localizedMessage}") }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun copyToClipboard() {
        val text = _uiState.value.extractedText ?: return
        if (text.isBlank()) {
            _uiState.update { it.copy(errorMessage = "لا يوجد نص مستخرج لنسخه") }
            return
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("OCR Text", text)
        clipboard.setPrimaryClip(clip)
        _uiState.update { it.copy(copiedToClipboard = true) }
    }

    fun shareText(ctx: Context) {
        val text = _uiState.value.extractedText ?: return
        if (text.isBlank()) {
            _uiState.update { it.copy(errorMessage = "لا يوجد نص مستخرج لمشاركته") }
            return
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        ctx.startActivity(Intent.createChooser(shareIntent, "مشاركة النص المستخرج"))
    }

    fun saveAsTextFile(destinationUri: Uri) {
        val text = _uiState.value.extractedText ?: return
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
            ocrUseCase: OcrUseCase,
            safHelper: SafFileHelper,
            tempFileManager: TempFileManager,
            context: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return OcrViewModel(ocrUseCase, safHelper, tempFileManager, context) as T
            }
        }
    }
}
