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
