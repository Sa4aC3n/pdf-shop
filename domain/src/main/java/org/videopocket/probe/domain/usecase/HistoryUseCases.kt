package org.videopocket.probe.domain.usecase

import org.videopocket.probe.domain.model.OperationRecord
import org.videopocket.probe.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow

class GetHistoryUseCase(
    private val historyRepository: HistoryRepository
) {
    operator fun invoke(): Flow<List<OperationRecord>> {
        return historyRepository.getOperations()
    }
}

class ClearHistoryUseCase(
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke() {
        historyRepository.clearHistory()
    }
}
