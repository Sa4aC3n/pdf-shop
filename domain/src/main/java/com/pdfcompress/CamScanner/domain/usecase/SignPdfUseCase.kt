package com.pdfcompress.CamScanner.domain.usecase

import android.net.Uri
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.domain.engine.EngineProgress
import com.pdfcompress.CamScanner.domain.engine.PdfDocumentEngine
import com.pdfcompress.CamScanner.domain.model.OperationRecord
import com.pdfcompress.CamScanner.domain.model.OperationStatus
import com.pdfcompress.CamScanner.domain.model.SignaturePlacement
import com.pdfcompress.CamScanner.domain.model.ToolType
import com.pdfcompress.CamScanner.domain.repository.HistoryRepository

class SignPdfUseCase(
    private val pdfEngine: PdfDocumentEngine,
    private val historyRepository: HistoryRepository
) {
    suspend fun getPageCount(uri: Uri): Int {
        return pdfEngine.getPageCount(uri)
    }

    suspend operator fun invoke(
        pdfUri: Uri,
        pdfName: String,
        signatureBitmapUri: Uri,
        placement: SignaturePlacement,
        destinationUri: Uri,
        destinationName: String,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> {
        val startedAt = System.currentTimeMillis()
        val result = pdfEngine.signPdf(pdfUri, signatureBitmapUri, placement, destinationUri, onProgress)

        when (result) {
            is AppResult.Success -> {
                val sizeBytes = result.data
                historyRepository.recordOperation(
                    OperationRecord(
                        toolType = ToolType.SIGN_PDF,
                        status = OperationStatus.SUCCESS,
                        startedAt = startedAt,
                        completedAt = System.currentTimeMillis(),
                        inputDocumentNames = listOf(pdfName),
                        outputDocumentName = destinationName,
                        outputDocumentUri = destinationUri.toString(),
                        outputSizeBytes = sizeBytes,
                        summary = "توقيع المستند إلكترونياً على الصفحة ${placement.pageIndex + 1}"
                    )
                )
            }
            is AppResult.Error -> {
                historyRepository.recordOperation(
                    OperationRecord(
                        toolType = ToolType.SIGN_PDF,
                        status = OperationStatus.FAILED,
                        startedAt = startedAt,
                        completedAt = System.currentTimeMillis(),
                        inputDocumentNames = listOf(pdfName),
                        outputDocumentName = destinationName,
                        outputDocumentUri = destinationUri.toString(),
                        outputSizeBytes = 0,
                        summary = "فشل توقيع المستند: ${result.error.message}"
                    )
                )
            }
            is AppResult.Progress -> {}
        }

        return result
    }
}
