package com.pdfcompress.CamScanner.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pdfcompress.CamScanner.domain.model.OperationRecord
import com.pdfcompress.CamScanner.domain.model.OperationStatus
import com.pdfcompress.CamScanner.domain.model.ToolType

@Entity(tableName = "operations")
data class OperationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val toolType: String,
    val status: String,
    val startedAt: Long,
    val completedAt: Long,
    val inputDocumentNames: String, // comma-separated or JSON
    val outputDocumentName: String?,
    val outputDocumentUri: String?,
    val outputSizeBytes: Long?,
    val summary: String?,
    val errorMessage: String?,
    val isOutputAvailable: Boolean = true
) {
    fun toDomain(): OperationRecord {
        return OperationRecord(
            id = id,
            toolType = try { ToolType.valueOf(toolType) } catch (_: Exception) { ToolType.MERGE },
            status = try { OperationStatus.valueOf(status) } catch (_: Exception) { OperationStatus.SUCCESS },
            startedAt = startedAt,
            completedAt = completedAt,
            inputDocumentNames = if (inputDocumentNames.isBlank()) emptyList() else inputDocumentNames.split(";;;"),
            outputDocumentName = outputDocumentName,
            outputDocumentUri = outputDocumentUri,
            outputSizeBytes = outputSizeBytes,
            summary = summary,
            errorMessage = errorMessage,
            isOutputAvailable = isOutputAvailable
        )
    }

    companion object {
        fun fromDomain(record: OperationRecord): OperationEntity {
            return OperationEntity(
                id = record.id,
                toolType = record.toolType.name,
                status = record.status.name,
                startedAt = record.startedAt,
                completedAt = record.completedAt,
                inputDocumentNames = record.inputDocumentNames.joinToString(";;;"),
                outputDocumentName = record.outputDocumentName,
                outputDocumentUri = record.outputDocumentUri,
                outputSizeBytes = record.outputSizeBytes,
                summary = record.summary,
                errorMessage = record.errorMessage,
                isOutputAvailable = record.isOutputAvailable
            )
        }
    }
}
