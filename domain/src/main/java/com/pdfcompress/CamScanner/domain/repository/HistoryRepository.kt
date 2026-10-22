package com.pdfcompress.CamScanner.domain.repository

import com.pdfcompress.CamScanner.domain.model.OperationRecord
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getOperations(): Flow<List<OperationRecord>>
    suspend fun recordOperation(operation: OperationRecord): Long
    suspend fun clearHistory()
    suspend fun updateAvailability(operationId: Long, isAvailable: Boolean)
}
