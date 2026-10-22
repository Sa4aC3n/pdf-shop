package com.pdfcompress.CamScanner.domain.usecase

import android.net.Uri
import com.pdfcompress.CamScanner.core.common.error.AppError
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.domain.engine.EngineProgress
import com.pdfcompress.CamScanner.domain.engine.PdfDocumentEngine
import com.pdfcompress.CamScanner.domain.model.OperationRecord
import com.pdfcompress.CamScanner.domain.model.OperationStatus
import com.pdfcompress.CamScanner.domain.model.ToolType
import com.pdfcompress.CamScanner.domain.repository.HistoryRepository

class SplitPdfUseCase(
    private val pdfEngine: PdfDocumentEngine,
    private val historyRepository: HistoryRepository
) {
    suspend fun getPageCount(sourceUri: Uri): Int {
        return pdfEngine.getPageCount(sourceUri)
    }

    suspend operator fun invoke(
        sourceUri: Uri,
        sourceName: String,
        selectedPages: List<Int>,
        destinationUri: Uri,
        destinationName: String,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> {
        val startedAt = System.currentTimeMillis()

        if (selectedPages.isEmpty()) {
            return AppResult.Error(
                AppError.Unknown("يجب تحديد صفحة واحدة على الأقل للاستخراج")
            )
        }

        val result = pdfEngine.splitOrExtract(sourceUri, selectedPages, destinationUri, onProgress)

        when (result) {
            is AppResult.Success -> {
                val sizeBytes = result.data
                historyRepository.recordOperation(
                    OperationRecord(
                        toolType = ToolType.SPLIT,
                        status = OperationStatus.SUCCESS,
                        startedAt = startedAt,
                        completedAt = System.currentTimeMillis(),
                        inputDocumentNames = listOf(sourceName),
                        outputDocumentName = destinationName,
                        outputDocumentUri = destinationUri.toString(),
                        outputSizeBytes = sizeBytes,
                        summary = "تم استخراج ${selectedPages.size} صفحات بنجاح"
                    )
                )
            }
            is AppResult.Error -> {
                historyRepository.recordOperation(
                    OperationRecord(
                        toolType = ToolType.SPLIT,
                        status = OperationStatus.FAILED,
                        startedAt = startedAt,
                        completedAt = System.currentTimeMillis(),
                        inputDocumentNames = listOf(sourceName),
                        outputDocumentName = destinationName,
                        outputDocumentUri = destinationUri.toString(),
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
