package org.videopocket.probe.data.repository

import org.videopocket.probe.data.db.dao.OperationDao
import org.videopocket.probe.data.db.entity.OperationEntity
import org.videopocket.probe.domain.model.OperationRecord
import org.videopocket.probe.domain.repository.HistoryRepository
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
