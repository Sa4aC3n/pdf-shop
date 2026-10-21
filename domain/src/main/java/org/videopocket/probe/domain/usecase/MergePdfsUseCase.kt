package org.videopocket.probe.domain.usecase

import android.net.Uri
import org.videopocket.probe.core.common.error.AppError
import org.videopocket.probe.core.common.result.AppResult
import org.videopocket.probe.domain.engine.EngineProgress
import org.videopocket.probe.domain.engine.PdfDocumentEngine
import org.videopocket.probe.domain.model.OperationRecord
import org.videopocket.probe.domain.model.OperationStatus
import org.videopocket.probe.domain.model.ToolType
import org.videopocket.probe.domain.repository.HistoryRepository

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
