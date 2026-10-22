package com.pdfcompress.CamScanner.domain.usecase

import android.net.Uri
import com.pdfcompress.CamScanner.core.common.error.AppError
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.domain.model.OperationStatus
import com.pdfcompress.CamScanner.domain.model.ToolType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class SplitPdfUseCaseTest {

    private lateinit var fakeEngine: FakePdfDocumentEngine
    private lateinit var fakeHistory: FakeHistoryRepository
    private lateinit var useCase: SplitPdfUseCase

    @Before
    fun setup() {
        fakeEngine = FakePdfDocumentEngine()
        fakeHistory = FakeHistoryRepository()
        useCase = SplitPdfUseCase(fakeEngine, fakeHistory)
    }

    @Test
    fun `empty selected pages returns Error`() = runBlocking {
        val srcUri = mock(Uri::class.java)
        val destUri = mock(Uri::class.java)

        val result = useCase(
            sourceUri = srcUri,
            sourceName = "document.pdf",
            selectedPages = emptyList(),
            destinationUri = destUri,
            destinationName = "extracted.pdf",
            onProgress = {}
        )

        assertTrue(result is AppResult.Error)
        assertEquals(0, fakeEngine.splitCallCount)
    }

    @Test
    fun `successful extraction records SUCCESS in history`() = runBlocking {
        val srcUri = mock(Uri::class.java)
        val destUri = mock(Uri::class.java)
        fakeEngine.splitResult = AppResult.Success(51200L)

        val result = useCase(
            sourceUri = srcUri,
            sourceName = "document.pdf",
            selectedPages = listOf(1, 3),
            destinationUri = destUri,
            destinationName = "extracted.pdf",
            onProgress = {}
        )

        assertTrue(result is AppResult.Success)
        assertEquals(1, fakeHistory.recordedOperations.size)
        assertEquals(OperationStatus.SUCCESS, fakeHistory.recordedOperations[0].status)
        assertEquals(ToolType.SPLIT, fakeHistory.recordedOperations[0].toolType)
    }
}
