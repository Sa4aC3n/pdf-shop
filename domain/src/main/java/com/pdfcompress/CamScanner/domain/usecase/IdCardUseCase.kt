package com.pdfcompress.CamScanner.domain.usecase

import android.net.Uri
import com.pdfcompress.CamScanner.core.common.error.AppError
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.domain.engine.EngineProgress
import com.pdfcompress.CamScanner.domain.engine.PdfDocumentEngine
import com.pdfcompress.CamScanner.domain.model.IdCardLayoutMode
import com.pdfcompress.CamScanner.domain.model.IdCardResult
import com.pdfcompress.CamScanner.domain.model.OperationRecord
import com.pdfcompress.CamScanner.domain.model.OperationStatus
import com.pdfcompress.CamScanner.domain.model.ToolType
import com.pdfcompress.CamScanner.domain.repository.HistoryRepository

class IdCardUseCase(
    private val pdfEngine: PdfDocumentEngine,
    private val historyRepository: HistoryRepository
) {
    suspend fun createPdf(
        frontCardUri: Uri?,
        backCardUri: Uri?,
        layoutMode: IdCardLayoutMode,
        destinationUri: Uri,
        destinationName: String,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<IdCardResult> {
        val startedAt = System.currentTimeMillis()

        if (frontCardUri == null && backCardUri == null) {
            return AppResult.Error(AppError.Unknown("يجب التقاط أو اختيار صورة الوجه أو الظهر على الأقل"))
        }

        val result = pdfEngine.createIdCardPdf(
            frontCardUri = frontCardUri,
            backCardUri = backCardUri,
            layoutMode = layoutMode,
            destinationUri = destinationUri,
            onProgress = onProgress
        )

        when (result) {
            is AppResult.Success -> {
                val inputNames = mutableListOf<String>()
                if (frontCardUri != null) inputNames.add("صورة الوجه")
                if (backCardUri != null) inputNames.add("صورة الظهر")

                historyRepository.recordOperation(
                    OperationRecord(
                        toolType = ToolType.ID_CARD,
                        status = OperationStatus.SUCCESS,
                        startedAt = startedAt,
                        completedAt = System.currentTimeMillis(),
                        inputDocumentNames = inputNames,
                        outputDocumentName = destinationName,
                        outputDocumentUri = destinationUri.toString(),
                        outputSizeBytes = result.data.outputSizeBytes,
                        summary = "تم مسح وتصدير بطاقة شخصية (${layoutMode.titleAr}) - حجم ${result.data.outputSizeBytes} بايت"
                    )
                )
            }
            is AppResult.Error -> {
                historyRepository.recordOperation(
                    OperationRecord(
                        toolType = ToolType.ID_CARD,
                        status = OperationStatus.FAILED,
                        startedAt = startedAt,
                        completedAt = System.currentTimeMillis(),
                        inputDocumentNames = listOf("بطاقة شخصية"),
                        outputDocumentName = destinationName,
                        outputDocumentUri = destinationUri.toString(),
                        outputSizeBytes = null,
                        summary = "فشل في معالجة أو دمج البطاقة الشخصية",
                        errorMessage = result.error.message
                    )
                )
            }
            else -> {}
        }

        return result
    }

    suspend fun createImage(
        frontCardUri: Uri?,
        backCardUri: Uri?,
        layoutMode: IdCardLayoutMode,
        destinationUri: Uri,
        destinationName: String,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> {
        val startedAt = System.currentTimeMillis()

        if (frontCardUri == null && backCardUri == null) {
            return AppResult.Error(AppError.Unknown("يجب التقاط أو اختيار صورة الوجه أو الظهر على الأقل"))
        }

        val result = pdfEngine.createIdCardImage(
            frontCardUri = frontCardUri,
            backCardUri = backCardUri,
            layoutMode = layoutMode,
            destinationUri = destinationUri,
            onProgress = onProgress
        )

        when (result) {
            is AppResult.Success -> {
                val inputNames = mutableListOf<String>()
                if (frontCardUri != null) inputNames.add("صورة الوجه")
                if (backCardUri != null) inputNames.add("صورة الظهر")

                historyRepository.recordOperation(
                    OperationRecord(
                        toolType = ToolType.ID_CARD,
                        status = OperationStatus.SUCCESS,
                        startedAt = startedAt,
                        completedAt = System.currentTimeMillis(),
                        inputDocumentNames = inputNames,
                        outputDocumentName = destinationName,
                        outputDocumentUri = destinationUri.toString(),
                        outputSizeBytes = result.data,
                        summary = "تم حفظ صورة البطاقة الشخصية المدمجة بنجاح"
                    )
                )
            }
            is AppResult.Error -> {
                historyRepository.recordOperation(
                    OperationRecord(
                        toolType = ToolType.ID_CARD,
                        status = OperationStatus.FAILED,
                        startedAt = startedAt,
                        completedAt = System.currentTimeMillis(),
                        inputDocumentNames = listOf("بطاقة شخصية"),
                        outputDocumentName = destinationName,
                        outputDocumentUri = destinationUri.toString(),
                        outputSizeBytes = null,
                        summary = "فشل في حفظ صورة البطاقة",
                        errorMessage = result.error.message
                    )
                )
            }
            else -> {}
        }

        return result
    }
}
