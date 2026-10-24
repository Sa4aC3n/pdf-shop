package com.pdfcompress.CamScanner.data.repository

import com.pdfcompress.CamScanner.data.db.dao.OperationDao
import com.pdfcompress.CamScanner.data.db.entity.OperationEntity
import com.pdfcompress.CamScanner.domain.model.OperationRecord
import com.pdfcompress.CamScanner.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepositoryImpl(
    private val operationDao: OperationDao
) : HistoryRepository {

    override fun getOperations(): Flow<List<OperationRecord>> {
        return operationDao.getAllOperations().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAllHistory(): List<OperationRecord> {
        return operationDao.getOperationsList().map { it.toDomain() }
    }

    override suspend fun recordOperation(operation: OperationRecord): Long {
        return operationDao.insertOperation(OperationEntity.fromDomain(operation))
    }

    override suspend fun clearHistory() {
        operationDao.clearAllOperations()
    }

    override suspend fun updateAvailability(operationId: Long, isAvailable: Boolean) {
        operationDao.updateAvailability(operationId, isAvailable)
    }
}
