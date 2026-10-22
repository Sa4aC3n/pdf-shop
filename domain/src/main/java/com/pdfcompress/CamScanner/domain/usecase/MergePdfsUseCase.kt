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

class MergePdfsUseCase(
    private val pdfEngine: PdfDocumentEngine,
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(
        inputUris: List<Uri>,
        inputNames: List<String>,
        destinationUri: Uri,
        destinationName: String,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> {
        val startedAt = System.currentTimeMillis()

        if (inputUris.size < 2) {
            return AppResult.Error(
                AppError.Unknown("يجب اختيار ملفين PDF على الأقل للدمج")
            )
        }

        val result = pdfEngine.merge(inputUris, destinationUri, onProgress)

        when (result) {
            is AppResult.Success -> {
                val sizeBytes = result.data
                historyRepository.recordOperation(
                    OperationRecord(
                        toolType = ToolType.MERGE,
                        status = OperationStatus.SUCCESS,
                        startedAt = startedAt,
                        completedAt = System.currentTimeMillis(),
                        inputDocumentNames = inputNames,
                        outputDocumentName = destinationName,
                        outputDocumentUri = destinationUri.toString(),
                        outputSizeBytes = sizeBytes,
                        summary = "تم دمج ${inputUris.size} ملفات بنجاح"
                    )
                )
            }
            is AppResult.Error -> {
                historyRepository.recordOperation(
                    OperationRecord(
                        toolType = ToolType.MERGE,
                        status = OperationStatus.FAILED,
                        startedAt = startedAt,
                        completedAt = System.currentTimeMillis(),
                        inputDocumentNames = inputNames,
                        outputDocumentName = destinationName,
                        outputDocumentUri = destinationUri.toString(),
                        outputSizeBytes = null,
                        errorMessage = result.error.message
                    )
                )
            }
            is AppResult.Progress -> {
                // Internal progress
            }
        }

        return result
    }
}
