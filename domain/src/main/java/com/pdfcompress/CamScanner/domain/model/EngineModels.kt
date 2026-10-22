package com.pdfcompress.CamScanner.domain.model

enum class CompressionLevel(val quality: Float, val titleAr: String, val maxDimension: Int) {
    LOW(quality = 0.80f, titleAr = "ضغط خفيف (جودة عالية)", maxDimension = 1800),
    MEDIUM(quality = 0.55f, titleAr = "ضغط متوازن (مستحسن)", maxDimension = 1200),
    HIGH(quality = 0.35f, titleAr = "أقصى ضغط (حجم أصغر)", maxDimension = 800)
}

data class CompressionResult(
    val originalSizeBytes: Long,
    val compressedSizeBytes: Long,
    val reductionPercentage: Int,
    val isSmaller: Boolean
)

data class WatermarkOptions(
    val text: String,
    val fontSize: Float = 42f,
    val opacity: Float = 0.25f,
    val rotationDegrees: Float = 45f
)

data class TextExtractionResult(
    val text: String,
    val characterCount: Int,
    val wordCount: Int,
    val pageCount: Int
)

data class RepairReport(
    val isSuccess: Boolean,
    val recoveredPagesCount: Int,
    val outputSizeBytes: Long,
    val diagnosisMessage: String
)

data class ExportImagesResult(
    val totalExportedPages: Int,
    val outputSizeBytes: Long,
    val isZipArchive: Boolean
)
