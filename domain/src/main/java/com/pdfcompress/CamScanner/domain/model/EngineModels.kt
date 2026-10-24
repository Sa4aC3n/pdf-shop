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

enum class IdCardLayoutMode(val titleAr: String, val titleEn: String) {
    A4_VERTICAL(
        titleAr = "صفحة A4 مجمعة (طباعة رسمية)",
        titleEn = "Merged A4 (Official Copy)"
    ),
    SIDE_BY_SIDE(
        titleAr = "جنباً إلى جنب في صفحة واحدة",
        titleEn = "Side-by-Side (Single Page)"
    ),
    TWO_PAGES(
        titleAr = "صفحتين منفصلتين (PDF)",
        titleEn = "Two Pages (PDF)"
    ),
    FRONT_ONLY(
        titleAr = "حفظ الوجه فقط",
        titleEn = "Front Side Only"
    ),
    BACK_ONLY(
        titleAr = "حفظ الظهر فقط",
        titleEn = "Back Side Only"
    )
}

data class IdCardResult(
    val outputSizeBytes: Long,
    val pageCount: Int,
    val layoutMode: IdCardLayoutMode
)

data class PassportMrzData(
    val rawMrz: String,
    val documentType: String,
    val countryCode: String,
    val countryName: String,
    val documentNumber: String,
    val documentNumberCheckOk: Boolean,
    val nationality: String,
    val nationalityName: String,
    val dateOfBirth: String,
    val dateOfBirthRaw: String,
    val dateOfBirthCheckOk: Boolean,
    val sex: String,
    val sexLabel: String,
    val expiryDate: String,
    val expiryDateRaw: String,
    val expiryDateCheckOk: Boolean,
    val isExpired: Boolean,
    val personalNumber: String?,
    val surname: String,
    val givenNames: String,
    val fullName: String,
    val isCompositeCheckOk: Boolean,
    val isCompleteValid: Boolean
)

data class BackupReport(
    val totalFilesCount: Int,
    val totalRecordsCount: Int,
    val outputSizeBytes: Long,
    val isEncrypted: Boolean,
    val summary: String
)
