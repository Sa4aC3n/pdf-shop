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

class ScanDocumentUseCaseTest {

    private lateinit var fakeEngine: FakePdfDocumentEngine
    private lateinit var fakeHistory: FakeHistoryRepository
    private lateinit var useCase: ScanDocumentUseCase

    @Before
    fun setup() {
        fakeEngine = FakePdfDocumentEngine()
        fakeHistory = FakeHistoryRepository()
        useCase = ScanDocumentUseCase(fakeEngine, fakeHistory)
    }

    @Test
    fun `empty scanned pages returns Error without calling engine`() = runBlocking {
        val destUri = mock(Uri::class.java)

        val result = useCase(
            scannedPageUris = emptyList(),
            pageNames = emptyList(),
            destinationUri = destUri,
            destinationName = "scan.pdf",
            onProgress = {}
        )

        assertTrue(result is AppResult.Error)
        assertEquals(0, fakeEngine.imagesToPdfCallCount)
        assertEquals(0, fakeHistory.recordedOperations.size)
    }

    @Test
    fun `successful scan generation records SCANNER in history and returns Success`() = runBlocking {
        val page1 = mock(Uri::class.java)
        val page2 = mock(Uri::class.java)
        val destUri = mock(Uri::class.java)
        fakeEngine.imagesToPdfResult = AppResult.Success(256000L)

        val result = useCase(
            scannedPageUris = listOf(page1, page2),
            pageNames = listOf("page_1.jpg", "page_2.jpg"),
            destinationUri = destUri,
            destinationName = "scanned_doc.pdf",
            onProgress = {}
        )

        assertTrue(result is AppResult.Success)
        assertEquals(256000L, (result as AppResult.Success).data)
        assertEquals(1, fakeEngine.imagesToPdfCallCount)
        assertEquals(1, fakeHistory.recordedOperations.size)
        assertEquals(OperationStatus.SUCCESS, fakeHistory.recordedOperations[0].status)
        assertEquals(ToolType.SCANNER, fakeHistory.recordedOperations[0].toolType)
    }

    @Test
    fun `engine error records FAILED in history and returns Error`() = runBlocking {
        val page1 = mock(Uri::class.java)
        val destUri = mock(Uri::class.java)
        fakeEngine.imagesToPdfResult = AppResult.Error(AppError.CorruptedPdf("Encoding error"))

        val result = useCase(
            scannedPageUris = listOf(page1),
            pageNames = listOf("page_1.jpg"),
            destinationUri = destUri,
            destinationName = "scanned_doc.pdf",
            onProgress = {}
        )

        assertTrue(result is AppResult.Error)
        assertEquals(1, fakeHistory.recordedOperations.size)
        assertEquals(OperationStatus.FAILED, fakeHistory.recordedOperations[0].status)
    }
}
