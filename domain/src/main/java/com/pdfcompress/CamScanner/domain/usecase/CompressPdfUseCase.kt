package com.pdfcompress.CamScanner.domain.usecase

import android.net.Uri
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.domain.engine.EngineProgress
import com.pdfcompress.CamScanner.domain.engine.PdfDocumentEngine
import com.pdfcompress.CamScanner.domain.model.CompressionLevel
import com.pdfcompress.CamScanner.domain.model.CompressionResult
import com.pdfcompress.CamScanner.domain.model.OperationRecord
import com.pdfcompress.CamScanner.domain.model.OperationStatus
import com.pdfcompress.CamScanner.domain.model.ToolType
import com.pdfcompress.CamScanner.domain.repository.HistoryRepository

class CompressPdfUseCase(
    private val pdfEngine: PdfDocumentEngine,
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(
        sourceUri: Uri,
        sourceName: String,
        level: CompressionLevel,
        destinationUri: Uri,
        destinationName: String,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<CompressionResult> {
        val startedAt = System.currentTimeMillis()

        val result = pdfEngine.compress(sourceUri, level, destinationUri, onProgress)

        when (result) {
            is AppResult.Success -> {
                val data = result.data
                val summaryText = if (data.isSmaller) {
                    "تم تقليل الحجم بنسبة ${data.reductionPercentage}%"
                } else {
                    "اكتملت المعالجة (الملف الأصلي محسن بالفعل)"
                }

                historyRepository.recordOperation(
                    OperationRecord(
                        toolType = ToolType.COMPRESS,
                        status = OperationStatus.SUCCESS,
                        startedAt = startedAt,
                        completedAt = System.currentTimeMillis(),
                        inputDocumentNames = listOf(sourceName),
                        outputDocumentName = destinationName,
                        outputDocumentUri = destinationUri.toString(),
                        outputSizeBytes = data.compressedSizeBytes,
                        summary = summaryText
                    )
                )
            }
            is AppResult.Error -> {
                historyRepository.recordOperation(
                    OperationRecord(
                        toolType = ToolType.COMPRESS,
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
