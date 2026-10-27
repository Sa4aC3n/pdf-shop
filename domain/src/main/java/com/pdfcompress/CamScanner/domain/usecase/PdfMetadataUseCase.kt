package com.pdfcompress.CamScanner.domain.usecase

import android.net.Uri
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.domain.engine.EngineProgress
import com.pdfcompress.CamScanner.domain.engine.PdfDocumentEngine
import com.pdfcompress.CamScanner.domain.model.OperationRecord
import com.pdfcompress.CamScanner.domain.model.OperationStatus
import com.pdfcompress.CamScanner.domain.model.PdfMetadata
import com.pdfcompress.CamScanner.domain.model.ToolType
import com.pdfcompress.CamScanner.domain.repository.HistoryRepository

class PdfMetadataUseCase(
    private val pdfEngine: PdfDocumentEngine,
    private val historyRepository: HistoryRepository
) {
    suspend fun readMetadata(sourceUri: Uri): AppResult<PdfMetadata> {
        return pdfEngine.readMetadata(sourceUri)
    }

    suspend fun updateMetadata(
        sourceUri: Uri,
        sourceName: String,
        title: String?,
        author: String?,
        subject: String?,
        keywords: String?,
        clearAll: Boolean,
        destinationUri: Uri,
        destinationName: String,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> {
        val startedAt = System.currentTimeMillis()
        val result = pdfEngine.updateMetadata(
            sourceUri,
            title,
            author,
            subject,
            keywords,
            clearAll,
            destinationUri,
            onProgress
        )

        when (result) {
            is AppResult.Success -> {
                val sizeBytes = result.data
                historyRepository.recordOperation(
                    OperationRecord(
                        toolType = ToolType.METADATA,
                        status = OperationStatus.SUCCESS,
                        startedAt = startedAt,
                        completedAt = System.currentTimeMillis(),
                        inputDocumentNames = listOf(sourceName),
                        outputDocumentName = destinationName,
                        outputDocumentUri = destinationUri.toString(),
                        outputSizeBytes = sizeBytes,
                        summary = if (clearAll) "تم مسح كافة البيانات الوصفية (Metadata)" else "تم تحديث البيانات الوصفية للمستند"
                    )
                )
            }
            is AppResult.Error -> {
                historyRepository.recordOperation(
                    OperationRecord(
                        toolType = ToolType.METADATA,
                        status = OperationStatus.FAILED,
                        startedAt = startedAt,
                        completedAt = System.currentTimeMillis(),
                        inputDocumentNames = listOf(sourceName),
                        outputDocumentName = destinationName,
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
