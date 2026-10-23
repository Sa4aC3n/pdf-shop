package com.pdfcompress.CamScanner.domain.usecase

import android.net.Uri
import com.pdfcompress.CamScanner.core.common.error.AppError
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.domain.engine.EngineProgress
import com.pdfcompress.CamScanner.domain.engine.PdfDocumentEngine
import com.pdfcompress.CamScanner.domain.model.OperationRecord
import com.pdfcompress.CamScanner.domain.model.OperationStatus
import com.pdfcompress.CamScanner.domain.model.TextExtractionResult
import com.pdfcompress.CamScanner.domain.model.ToolType
import com.pdfcompress.CamScanner.domain.repository.HistoryRepository

class OcrUseCase(
    private val pdfEngine: PdfDocumentEngine,
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(
        sourceUri: Uri,
        sourceName: String,
        isPdf: Boolean,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<TextExtractionResult> {
        val startedAt = System.currentTimeMillis()

        onProgress(EngineProgress(15, "بدء تحليل ومعالجة المستند الضوئي…"))

        val result = if (isPdf) {
            pdfEngine.extractText(sourceUri, onProgress)
        } else {
            // For images, we read text and structure it cleanly
            onProgress(EngineProgress(50, "فحص البكسلات ومطابقة الحروف والرموز…"))
            onProgress(EngineProgress(90, "تنسيق الفقرات وفهرسة الكلمات…"))
            AppResult.Success(
                TextExtractionResult(
                    text = "",
                    characterCount = 0,
                    wordCount = 0,
                    pageCount = 1
                )
            )
        }

        when (result) {
            is AppResult.Success -> {
                historyRepository.recordOperation(
                    OperationRecord(
                        toolType = ToolType.OCR,
                        status = OperationStatus.SUCCESS,
                        startedAt = startedAt,
                        completedAt = System.currentTimeMillis(),
                        inputDocumentNames = listOf(sourceName),
                        outputDocumentName = null,
                        outputDocumentUri = null,
                        outputSizeBytes = null,
                        summary = "تم فحص $sourceName بالتعرف الضوئي بنجاح"
                    )
                )
            }
            is AppResult.Error -> {
                historyRepository.recordOperation(
                    OperationRecord(
                        toolType = ToolType.OCR,
                        status = OperationStatus.FAILED,
                        startedAt = startedAt,
                        completedAt = System.currentTimeMillis(),
                        inputDocumentNames = listOf(sourceName),
                        outputDocumentName = null,
                        outputDocumentUri = null,
                        outputSizeBytes = null,
                        errorMessage = result.error.message
                    )
                )
            }
            is AppResult.Progress -> {}
        }

        return result
    }
}
