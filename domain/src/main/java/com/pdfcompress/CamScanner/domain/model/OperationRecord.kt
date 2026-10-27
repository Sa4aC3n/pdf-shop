package com.pdfcompress.CamScanner.domain.model

enum class ToolType {
    MERGE,
    IMAGES_TO_PDF,
    SPLIT,
    SCANNER,
    COMPRESS,
    WATERMARK,
    PDF_TO_IMAGES,
    EXTRACT_TEXT,
    OCR,
    REPAIR,
    ID_CARD,
    PASSPORT_MRZ,
    BACKUP_RESTORE,
    ROTATE,
    PROTECT,
    UNLOCK,
    METADATA,
    FLATTEN,
    ORGANIZE_PAGES,
    PAGE_NUMBERS,
    GRAYSCALE,
    CROP,
    REDACT,
    TXT_TO_PDF,
    MARKDOWN_TO_PDF,
    HTML_TO_PDF,
    PDF_TO_MARKDOWN,
    RTF_TO_PDF
}

enum class OperationStatus {
    SUCCESS,
    FAILED,
    CANCELLED
}

data class OperationRecord(
    val id: Long = 0,
    val toolType: ToolType,
    val status: OperationStatus,
    val startedAt: Long,
    val completedAt: Long = System.currentTimeMillis(),
    val inputDocumentNames: List<String>,
    val outputDocumentName: String?,
    val outputDocumentUri: String?,
    val outputSizeBytes: Long?,
    val summary: String? = null,
    val errorMessage: String? = null,
    val isOutputAvailable: Boolean = true
)
