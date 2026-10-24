package com.pdfcompress.CamScanner.domain.usecase

import android.net.Uri
import com.pdfcompress.CamScanner.core.common.error.AppError
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.domain.engine.EngineProgress
import com.pdfcompress.CamScanner.domain.model.BackupReport
import com.pdfcompress.CamScanner.domain.model.OperationRecord
import com.pdfcompress.CamScanner.domain.model.OperationStatus
import com.pdfcompress.CamScanner.domain.model.ToolType
import com.pdfcompress.CamScanner.domain.repository.HistoryRepository

interface BackupArchiveEngine {
    suspend fun createArchive(
        historyRecords: List<OperationRecord>,
        password: String?,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<BackupReport>

    suspend fun restoreArchive(
        sourceUri: Uri,
        password: String?,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<List<OperationRecord>>
}

class BackupRestoreUseCase(
    private val backupEngine: BackupArchiveEngine,
    private val historyRepository: HistoryRepository
) {
    suspend fun createBackup(
        password: String?,
        destinationUri: Uri,
        destinationName: String,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<BackupReport> {
        val startedAt = System.currentTimeMillis()
        val allHistory = historyRepository.getAllHistory()

        val result = backupEngine.createArchive(
            historyRecords = allHistory,
            password = password,
            destinationUri = destinationUri,
            onProgress = onProgress
        )

        when (result) {
            is AppResult.Success -> {
                historyRepository.recordOperation(
                    OperationRecord(
                        toolType = ToolType.BACKUP_RESTORE,
                        status = OperationStatus.SUCCESS,
                        startedAt = startedAt,
                        completedAt = System.currentTimeMillis(),
                        inputDocumentNames = listOf("${allHistory.size} سجلات مستندات"),
                        outputDocumentName = destinationName,
                        outputDocumentUri = destinationUri.toString(),
                        outputSizeBytes = result.data.outputSizeBytes,
                        summary = if (result.data.isEncrypted) "نسخة احتياطية مشفرة AES-256 تحتوي على ${result.data.totalRecordsCount} مستند" else "نسخة احتياطية عادية تحتوي على ${result.data.totalRecordsCount} مستند"
                    )
                )
            }
            is AppResult.Error -> {
                historyRepository.recordOperation(
                    OperationRecord(
                        toolType = ToolType.BACKUP_RESTORE,
                        status = OperationStatus.FAILED,
                        startedAt = startedAt,
                        completedAt = System.currentTimeMillis(),
                        inputDocumentNames = listOf("أرشيف مستندات"),
                        outputDocumentName = destinationName,
                        outputDocumentUri = destinationUri.toString(),
                        outputSizeBytes = null,
                        summary = "فشل في إنشاء النسخة الاحتياطية",
                        errorMessage = result.error.message
                    )
                )
            }
            else -> {}
        }

        return result
    }

    suspend fun restoreBackup(
        sourceUri: Uri,
        password: String?,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Int> {
        val result = backupEngine.restoreArchive(sourceUri, password, onProgress)
        return when (result) {
            is AppResult.Success -> {
                val records = result.data
                for (rec in records) {
                    historyRepository.recordOperation(rec)
                }
                AppResult.Success(records.size)
            }
            is AppResult.Error -> AppResult.Error(result.error)
            else -> AppResult.Error(com.pdfcompress.CamScanner.core.common.error.AppError.Unknown("فشل في قراءة الأرشيف"))
        }
    }
}
