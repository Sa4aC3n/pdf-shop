package org.videopocket.probe.domain.repository

import org.videopocket.probe.domain.model.OperationRecord
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getOperations(): Flow<List<OperationRecord>>
    suspend fun recordOperation(operation: OperationRecord): Long
    suspend fun clearHistory()
    suspend fun updateAvailability(operationId: Long, isAvailable: Boolean)
}
