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

        if (!isPdf) {
            // For standalone raster images, we do not simulate fake OCR progress or record a fake success.
            // Digital text extraction applies to PDFs; raster images can be sent to on-device Google Lens.
            return AppResult.Success(
                TextExtractionResult(
                    text = "",
                    characterCount = 0,
                    wordCount = 0,
                    pageCount = 1
                )
            )
        }

        onProgress(EngineProgress(15, "بدء تحليل واستخراج طبقة النصوص من المستند…"))
        val result = pdfEngine.extractText(sourceUri, onProgress)

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
