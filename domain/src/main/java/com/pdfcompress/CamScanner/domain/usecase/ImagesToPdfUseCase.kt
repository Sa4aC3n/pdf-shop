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

class ImagesToPdfUseCase(
    private val pdfEngine: PdfDocumentEngine,
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(
        imageUris: List<Uri>,
        imageNames: List<String>,
        destinationUri: Uri,
        destinationName: String,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> {
        val startedAt = System.currentTimeMillis()

        if (imageUris.isEmpty()) {
            return AppResult.Error(
                AppError.Unknown("يجب اختيار صورة واحدة على الأقل للتحويل")
            )
        }

        val result = pdfEngine.imagesToPdf(imageUris, destinationUri, onProgress)

        when (result) {
            is AppResult.Success -> {
                val sizeBytes = result.data
                historyRepository.recordOperation(
                    OperationRecord(
                        toolType = ToolType.IMAGES_TO_PDF,
                        status = OperationStatus.SUCCESS,
                        startedAt = startedAt,
                        completedAt = System.currentTimeMillis(),
                        inputDocumentNames = imageNames,
                        outputDocumentName = destinationName,
                        outputDocumentUri = destinationUri.toString(),
                        outputSizeBytes = sizeBytes,
                        summary = "تم تحويل ${imageUris.size} صور إلى مستند PDF واحد"
                    )
                )
            }
            is AppResult.Error -> {
                historyRepository.recordOperation(
                    OperationRecord(
                        toolType = ToolType.IMAGES_TO_PDF,
                        status = OperationStatus.FAILED,
                        startedAt = startedAt,
                        completedAt = System.currentTimeMillis(),
                        inputDocumentNames = imageNames,
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
