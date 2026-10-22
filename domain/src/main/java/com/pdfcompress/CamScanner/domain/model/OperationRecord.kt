package com.pdfcompress.CamScanner.domain.model

enum class ToolType {
    MERGE,
    IMAGES_TO_PDF,
    SPLIT,
    SCANNER,
    COMPRESS,
    WATERMARK,
    OCR,
    REPAIR
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
