package com.pdfcompress.CamScanner.domain.usecase

import android.net.Uri
import com.pdfcompress.CamScanner.core.common.error.AppError
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.domain.engine.EngineProgress
import com.pdfcompress.CamScanner.domain.engine.PdfDocumentEngine
import com.pdfcompress.CamScanner.domain.model.CompressionLevel
import com.pdfcompress.CamScanner.domain.model.CompressionResult
import com.pdfcompress.CamScanner.domain.model.OperationRecord
import com.pdfcompress.CamScanner.domain.model.OperationStatus
import com.pdfcompress.CamScanner.domain.model.ToolType
import com.pdfcompress.CamScanner.domain.model.WatermarkOptions
import com.pdfcompress.CamScanner.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class MergePdfsUseCaseTest {

    private lateinit var fakeEngine: FakePdfDocumentEngine
    private lateinit var fakeHistory: FakeHistoryRepository
    private lateinit var useCase: MergePdfsUseCase

    @Before
    fun setup() {
        fakeEngine = FakePdfDocumentEngine()
        fakeHistory = FakeHistoryRepository()
        useCase = MergePdfsUseCase(fakeEngine, fakeHistory)
    }

    @Test
    fun `merge with less than 2 files returns Error and does not call engine`() = runBlocking {
        val dummyUri = mock(Uri::class.java)

        val result = useCase(
            inputUris = listOf(dummyUri),
            inputNames = listOf("file1.pdf"),
            destinationUri = dummyUri,
            destinationName = "merged.pdf",
            onProgress = {}
        )

        assertTrue(result is AppResult.Error)
        assertEquals(0, fakeEngine.mergeCallCount)
        assertEquals(0, fakeHistory.recordedOperations.size)
    }

    @Test
    fun `merge with valid files returns Success and records in history`() = runBlocking {
        val dummyUri1 = mock(Uri::class.java)
        val dummyUri2 = mock(Uri::class.java)
        val destUri = mock(Uri::class.java)
        fakeEngine.mergeResult = AppResult.Success(102400L) // 100 KB

        val result = useCase(
            inputUris = listOf(dummyUri1, dummyUri2),
            inputNames = listOf("doc1.pdf", "doc2.pdf"),
            destinationUri = destUri,
            destinationName = "merged.pdf",
            onProgress = {}
        )

        assertTrue(result is AppResult.Success)
        assertEquals(102400L, (result as AppResult.Success).data)
        assertEquals(1, fakeEngine.mergeCallCount)
        assertEquals(1, fakeHistory.recordedOperations.size)
        assertEquals(OperationStatus.SUCCESS, fakeHistory.recordedOperations[0].status)
        assertEquals(ToolType.MERGE, fakeHistory.recordedOperations[0].toolType)
    }

    @Test
    fun `merge failure records FAILED in history and returns Error`() = runBlocking {
        val dummyUri1 = mock(Uri::class.java)
        val dummyUri2 = mock(Uri::class.java)
        val destUri = mock(Uri::class.java)
        fakeEngine.mergeResult = AppResult.Error(AppError.CorruptedPdf("Corrupt file"))

        val result = useCase(
            inputUris = listOf(dummyUri1, dummyUri2),
            inputNames = listOf("doc1.pdf", "doc2.pdf"),
            destinationUri = destUri,
            destinationName = "merged.pdf",
            onProgress = {}
        )

        assertTrue(result is AppResult.Error)
        assertEquals(1, fakeHistory.recordedOperations.size)
        assertEquals(OperationStatus.FAILED, fakeHistory.recordedOperations[0].status)
    }
}

class FakePdfDocumentEngine : PdfDocumentEngine {
    var mergeCallCount = 0
    var imagesToPdfCallCount = 0
    var splitCallCount = 0

    var mergeResult: AppResult<Long> = AppResult.Success(1024L)
    var imagesToPdfResult: AppResult<Long> = AppResult.Success(2048L)
    var splitResult: AppResult<Long> = AppResult.Success(512L)
    var compressResult: AppResult<CompressionResult> = AppResult.Success(CompressionResult(2048, 1024, 50, true))
    var watermarkResult: AppResult<Long> = AppResult.Success(1024L)
    var pageCountToReturn: Int = 5

    override suspend fun merge(
        inputUris: List<Uri>,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> {
        mergeCallCount++
        return mergeResult
    }

    override suspend fun imagesToPdf(
        imageUris: List<Uri>,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> {
        imagesToPdfCallCount++
        return imagesToPdfResult
    }

    override suspend fun splitOrExtract(
        sourceUri: Uri,
        selectedPages: List<Int>,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> {
        splitCallCount++
        return splitResult
    }

    override suspend fun compress(
        sourceUri: Uri,
        level: CompressionLevel,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<CompressionResult> = compressResult

    override suspend fun addWatermark(
        sourceUri: Uri,
        options: WatermarkOptions,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> = watermarkResult

    override suspend fun getPageCount(sourceUri: Uri): Int = pageCountToReturn
}

class FakeHistoryRepository : HistoryRepository {
    val recordedOperations = mutableListOf<OperationRecord>()

    override fun getOperations(): Flow<List<OperationRecord>> = flowOf(recordedOperations)

    override suspend fun recordOperation(operation: OperationRecord): Long {
        recordedOperations.add(operation)
        return recordedOperations.size.toLong()
    }

    override suspend fun clearHistory() {
        recordedOperations.clear()
    }

    override suspend fun updateAvailability(operationId: Long, isAvailable: Boolean) {}
}
