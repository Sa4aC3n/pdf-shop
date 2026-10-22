package com.pdfcompress.CamScanner.domain.usecase

import android.net.Uri
import com.pdfcompress.CamScanner.core.common.error.AppError
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.domain.model.CompressionLevel
import com.pdfcompress.CamScanner.domain.model.CompressionResult
import com.pdfcompress.CamScanner.domain.model.OperationStatus
import com.pdfcompress.CamScanner.domain.model.ToolType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class CompressPdfUseCaseTest {

    private lateinit var fakeEngine: FakePdfDocumentEngine
    private lateinit var fakeHistory: FakeHistoryRepository
    private lateinit var useCase: CompressPdfUseCase

    @Before
    fun setup() {
        fakeEngine = FakePdfDocumentEngine()
        fakeHistory = FakeHistoryRepository()
        useCase = CompressPdfUseCase(fakeEngine, fakeHistory)
    }

    @Test
    fun `successful compression records SUCCESS in history`() = runBlocking {
        val srcUri = mock(Uri::class.java)
        val destUri = mock(Uri::class.java)

        fakeEngine.compressResult = AppResult.Success(
            CompressionResult(
                originalSizeBytes = 2000000L,
                compressedSizeBytes = 1000000L,
                reductionPercentage = 50,
                isSmaller = true
            )
        )

        val result = useCase(
            sourceUri = srcUri,
            sourceName = "large_doc.pdf",
            level = CompressionLevel.MEDIUM,
            destinationUri = destUri,
            destinationName = "compressed.pdf",
            onProgress = {}
        )

        assertTrue(result is AppResult.Success)
        assertEquals(1, fakeHistory.recordedOperations.size)
        assertEquals(OperationStatus.SUCCESS, fakeHistory.recordedOperations[0].status)
        assertEquals(ToolType.COMPRESS, fakeHistory.recordedOperations[0].toolType)
    }

    @Test
    fun `failed compression records FAILED in history`() = runBlocking {
        val srcUri = mock(Uri::class.java)
        val destUri = mock(Uri::class.java)

        fakeEngine.compressResult = AppResult.Error(AppError.OutOfMemory())

        val result = useCase(
            sourceUri = srcUri,
            sourceName = "corrupt.pdf",
            level = CompressionLevel.HIGH,
            destinationUri = destUri,
            destinationName = "compressed.pdf",
            onProgress = {}
        )

        assertTrue(result is AppResult.Error)
        assertEquals(1, fakeHistory.recordedOperations.size)
        assertEquals(OperationStatus.FAILED, fakeHistory.recordedOperations[0].status)
    }
}
