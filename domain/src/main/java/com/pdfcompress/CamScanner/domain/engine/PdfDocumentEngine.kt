package com.pdfcompress.CamScanner.domain.engine

import android.net.Uri
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.domain.model.CompressionLevel
import com.pdfcompress.CamScanner.domain.model.CompressionResult
import com.pdfcompress.CamScanner.domain.model.ExportImagesResult
import com.pdfcompress.CamScanner.domain.model.IdCardLayoutMode
import com.pdfcompress.CamScanner.domain.model.IdCardResult
import com.pdfcompress.CamScanner.domain.model.PassportMrzData
import com.pdfcompress.CamScanner.domain.model.RepairReport
import com.pdfcompress.CamScanner.domain.model.TextExtractionResult
import com.pdfcompress.CamScanner.domain.model.WatermarkOptions
import com.pdfcompress.CamScanner.domain.model.PdfMetadata

data class EngineProgress(
    val percentage: Int,
    val stage: String? = null
)

interface PdfDocumentEngine {

    suspend fun merge(
        inputUris: List<Uri>,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long>

    suspend fun imagesToPdf(
        imageUris: List<Uri>,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long>

    suspend fun splitOrExtract(
        sourceUri: Uri,
        selectedPages: List<Int>,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long>

    suspend fun compress(
        sourceUri: Uri,
        level: CompressionLevel,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<CompressionResult>

    suspend fun addWatermark(
        sourceUri: Uri,
        options: WatermarkOptions,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long>

    suspend fun pdfToImages(
        sourceUri: Uri,
        selectedPages: List<Int>,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<ExportImagesResult>

    suspend fun extractText(
        sourceUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<TextExtractionResult>

    suspend fun recognizeImageText(
        imageUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<TextExtractionResult>

    suspend fun createIdCardPdf(
        frontCardUri: Uri?,
        backCardUri: Uri?,
        layoutMode: IdCardLayoutMode,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<IdCardResult>

    suspend fun createIdCardImage(
        frontCardUri: Uri?,
        backCardUri: Uri?,
        layoutMode: IdCardLayoutMode,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long>

    suspend fun generatePassportReportPdf(
        photoUri: Uri?,
        mrzData: PassportMrzData,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long>

    suspend fun recognizePassportMrz(
        imageUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<String>

    suspend fun repair(
        sourceUri: Uri,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<RepairReport>

    suspend fun getPageCount(sourceUri: Uri): Int

    suspend fun rotate(
        sourceUri: Uri,
        rotationDegrees: Int,
        selectedPages: List<Int>?,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long>

    suspend fun protect(
        sourceUri: Uri,
        userPassword: String,
        ownerPassword: String?,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long>

    suspend fun unlock(
        sourceUri: Uri,
        password: String,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long>

    suspend fun updateMetadata(
        sourceUri: Uri,
        title: String?,
        author: String?,
        subject: String?,
        keywords: String?,
        clearAll: Boolean,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long>

    suspend fun readMetadata(
        sourceUri: Uri
    ): AppResult<PdfMetadata>

    suspend fun flatten(
        sourceUri: Uri,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long>
}
