package com.pdfcompress.CamScanner.domain.usecase

import android.net.Uri
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.domain.engine.EngineProgress
import com.pdfcompress.CamScanner.domain.engine.PdfDocumentEngine
import com.pdfcompress.CamScanner.domain.model.CompareResult
import com.pdfcompress.CamScanner.domain.model.OperationRecord
import com.pdfcompress.CamScanner.domain.model.OperationStatus
import com.pdfcompress.CamScanner.domain.model.ToolType
import com.pdfcompress.CamScanner.domain.repository.HistoryRepository

class ComparePdfUseCase(
    private val pdfEngine: PdfDocumentEngine,
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(
        uriA: Uri,
        nameA: String,
        uriB: Uri,
        nameB: String,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<CompareResult> {
        val startedAt = System.currentTimeMillis()
        val result = pdfEngine.comparePdfs(uriA, uriB, onProgress)

        when (result) {
            is AppResult.Success -> {
                val diffCount = result.data.diffCount
                historyRepository.recordOperation(
                    OperationRecord(
                        toolType = ToolType.COMPARE_PDF,
                        status = OperationStatus.SUCCESS,
                        startedAt = startedAt,
                        completedAt = System.currentTimeMillis(),
                        inputDocumentNames = listOf(nameA, nameB),
                        outputDocumentName = null,
                        outputDocumentUri = null,
                        outputSizeBytes = 0,
                        summary = "مقارنة مستندين: تم اكتشاف $diffCount اختلاف"
                    )
                )
            }
            is AppResult.Error -> {
                historyRepository.recordOperation(
                    OperationRecord(
                        toolType = ToolType.COMPARE_PDF,
                        status = OperationStatus.FAILED,
                        startedAt = startedAt,
                        completedAt = System.currentTimeMillis(),
                        inputDocumentNames = listOf(nameA, nameB),
                        outputDocumentName = null,
                        outputDocumentUri = null,
                        outputSizeBytes = 0,
                        summary = "فشلت مقارنة المستندين: ${result.error.message}"
                    )
                )
            }
            is AppResult.Progress -> {}
        }

        return result
    }
}
