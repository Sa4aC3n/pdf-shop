package com.pdfcompress.CamScanner.domain.usecase

import android.net.Uri
import com.pdfcompress.CamScanner.core.common.error.AppError
import com.pdfcompress.CamScanner.domain.parser.MrzParser
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.domain.engine.EngineProgress
import com.pdfcompress.CamScanner.domain.engine.PdfDocumentEngine
import com.pdfcompress.CamScanner.domain.model.OperationRecord
import com.pdfcompress.CamScanner.domain.model.OperationStatus
import com.pdfcompress.CamScanner.domain.model.PassportMrzData
import com.pdfcompress.CamScanner.domain.model.ToolType
import com.pdfcompress.CamScanner.domain.repository.HistoryRepository

class PassportMrzUseCase(
    private val pdfEngine: PdfDocumentEngine,
    private val historyRepository: HistoryRepository
) {
    suspend fun scanPassportImage(
        imageUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<PassportMrzData> {
        onProgress(EngineProgress(20, "قراءة منطقة الشيفرة المدمجة (MRZ)…"))
        val ocrResult = pdfEngine.recognizePassportMrz(imageUri) { p ->
            onProgress(EngineProgress(20 + (p.percentage * 0.6).toInt(), p.stage))
        }

        return when (ocrResult) {
            is AppResult.Success -> {
                onProgress(EngineProgress(85, "تحليل وقراءة بيانات جواز السفر وفق معايير ICAO 9303…"))
                val mrzData = MrzParser.parse(ocrResult.data)
                if (mrzData != null) {
                    onProgress(EngineProgress(100, "تم استخراج وقراءة بيانات الجواز بنجاح"))
                    AppResult.Success(mrzData)
                } else {
                    AppResult.Error(
                        AppError.CorruptedPdf(
                            "تعذر اكتشاف خطوط الـ MRZ في الصورة الملتقطة. يرجى التأكد من وضوح السطرين في أسفل صفحة الجواز وإعادة المحاولة."
                        )
                    )
                }
            }
            is AppResult.Error -> {
                AppResult.Error(ocrResult.error)
            }
            else -> {
                AppResult.Error(AppError.Unknown("عملية غير مكتملة"))
            }
        }
    }

    suspend fun generateReport(
        photoUri: Uri?,
        mrzData: PassportMrzData,
        destinationUri: Uri,
        destinationName: String,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> {
        val startedAt = System.currentTimeMillis()
        val result = pdfEngine.generatePassportReportPdf(
            photoUri = photoUri,
            mrzData = mrzData,
            destinationUri = destinationUri,
            onProgress = onProgress
        )

        when (result) {
            is AppResult.Success -> {
                historyRepository.recordOperation(
                    OperationRecord(
                        toolType = ToolType.PASSPORT_MRZ,
                        status = OperationStatus.SUCCESS,
                        startedAt = startedAt,
                        completedAt = System.currentTimeMillis(),
                        inputDocumentNames = listOf("جواز سفر: ${mrzData.fullName}"),
                        outputDocumentName = destinationName,
                        outputDocumentUri = destinationUri.toString(),
                        outputSizeBytes = result.data,
                        summary = "تقرير فحص جواز سفر (${mrzData.countryName}) - رقم: ${mrzData.documentNumber}"
                    )
                )
            }
            is AppResult.Error -> {
                historyRepository.recordOperation(
                    OperationRecord(
                        toolType = ToolType.PASSPORT_MRZ,
                        status = OperationStatus.FAILED,
                        startedAt = startedAt,
                        completedAt = System.currentTimeMillis(),
                        inputDocumentNames = listOf("جواز سفر"),
                        outputDocumentName = destinationName,
                        outputDocumentUri = destinationUri.toString(),
                        outputSizeBytes = null,
                        summary = "فشل في توليد تقرير فحص الجواز",
                        errorMessage = result.error.message
                    )
                )
            }
            else -> {}
        }

        return result
    }
}
