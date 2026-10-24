package com.pdfcompress.CamScanner.domain.usecase

import android.net.Uri
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

        val result = if (isPdf) {
            onProgress(EngineProgress(15, "بدء قراءة وتحليل نصوص مستند الـ PDF…"))
            pdfEngine.extractText(sourceUri, onProgress)
        } else {
            onProgress(EngineProgress(15, "بدء التعرف الضوئي الأوفلاين على نصوص الصورة (OCR)…"))
            pdfEngine.recognizeImageText(sourceUri, onProgress)
        }

        when (result) {
            is AppResult.Success -> {
                if (result.data.text.isNotBlank()) {
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
                            summary = "تم استخراج ${result.data.wordCount} كلمة من $sourceName"
                        )
                    )
                }
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
