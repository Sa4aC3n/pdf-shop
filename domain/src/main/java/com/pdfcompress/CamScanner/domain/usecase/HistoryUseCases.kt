package com.pdfcompress.CamScanner.domain.usecase

import com.pdfcompress.CamScanner.domain.model.OperationRecord
import com.pdfcompress.CamScanner.domain.repository.HistoryRepository
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
