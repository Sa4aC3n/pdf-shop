package com.pdfcompress.CamScanner.domain.usecase

import android.net.Uri
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.domain.engine.EngineProgress
import com.pdfcompress.CamScanner.domain.engine.PdfDocumentEngine
import com.pdfcompress.CamScanner.domain.model.FormFieldInfo
import com.pdfcompress.CamScanner.domain.model.FormFillRequest
import com.pdfcompress.CamScanner.domain.model.OperationRecord
import com.pdfcompress.CamScanner.domain.model.OperationStatus
import com.pdfcompress.CamScanner.domain.model.ToolType
import com.pdfcompress.CamScanner.domain.repository.HistoryRepository

class PdfFormsUseCase(
    private val pdfEngine: PdfDocumentEngine,
    private val historyRepository: HistoryRepository
) {
    suspend fun getFields(pdfUri: Uri): AppResult<List<FormFieldInfo>> {
        return pdfEngine.getFormFields(pdfUri)
    }

    suspend fun fillFields(
        pdfUri: Uri,
        pdfName: String,
        request: FormFillRequest,
        destinationUri: Uri,
        destinationName: String,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> {
        val startedAt = System.currentTimeMillis()
        val result = pdfEngine.fillFormFields(pdfUri, request, destinationUri, onProgress)

        when (result) {
            is AppResult.Success -> {
                val sizeBytes = result.data
                historyRepository.recordOperation(
                    OperationRecord(
                        toolType = ToolType.PDF_FORMS,
                        status = OperationStatus.SUCCESS,
                        startedAt = startedAt,
                        completedAt = System.currentTimeMillis(),
                        inputDocumentNames = listOf(pdfName),
                        outputDocumentName = destinationName,
                        outputDocumentUri = destinationUri.toString(),
                        outputSizeBytes = sizeBytes,
                        summary = "تعبئة وحفظ ${request.fields.size} حقول في النموذج"
                    )
                )
            }
            is AppResult.Error -> {
                historyRepository.recordOperation(
                    OperationRecord(
                        toolType = ToolType.PDF_FORMS,
                        status = OperationStatus.FAILED,
                        startedAt = startedAt,
                        completedAt = System.currentTimeMillis(),
                        inputDocumentNames = listOf(pdfName),
                        outputDocumentName = destinationName,
                        outputDocumentUri = destinationUri.toString(),
                        outputSizeBytes = 0,
                        summary = "فشل حفظ النموذج: ${result.error.message}"
                    )
                )
            }
            is AppResult.Progress -> {}
        }

        return result
    }
}
