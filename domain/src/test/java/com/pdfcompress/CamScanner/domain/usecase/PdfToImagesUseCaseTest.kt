package com.pdfcompress.CamScanner.domain.usecase

import android.net.Uri
import com.pdfcompress.CamScanner.core.common.error.AppError
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.domain.model.ExportImagesResult
import com.pdfcompress.CamScanner.domain.model.OperationStatus
import com.pdfcompress.CamScanner.domain.model.ToolType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class PdfToImagesUseCaseTest {

    private lateinit var fakeEngine: FakePdfDocumentEngine
    private lateinit var fakeHistory: FakeHistoryRepository
    private lateinit var useCase: PdfToImagesUseCase

    @Before
    fun setup() {
        fakeEngine = FakePdfDocumentEngine()
        fakeHistory = FakeHistoryRepository()
        useCase = PdfToImagesUseCase(fakeEngine, fakeHistory)
    }

    @Test
    fun `empty selected pages returns Error without calling engine`() = runBlocking {
        val srcUri = mock(Uri::class.java)
        val destUri = mock(Uri::class.java)

        val result = useCase(
            sourceUri = srcUri,
            sourceName = "doc.pdf",
            selectedPages = emptyList(),
            destinationUri = destUri,
            destinationName = "out.zip",
            onProgress = {}
        )

        assertTrue(result is AppResult.Error)
        assertEquals(0, fakeHistory.recordedOperations.size)
    }

    @Test
    fun `successful export records in history and returns Success`() = runBlocking {
        val srcUri = mock(Uri::class.java)
        val destUri = mock(Uri::class.java)
        fakeEngine.pdfToImagesResult = AppResult.Success(ExportImagesResult(totalExportedPages = 3, outputSizeBytes = 50000L, isZipArchive = true))

        val result = useCase(
            sourceUri = srcUri,
            sourceName = "doc.pdf",
            selectedPages = listOf(1, 2, 3),
            destinationUri = destUri,
            destinationName = "out.zip",
            onProgress = {}
        )

        assertTrue(result is AppResult.Success)
        val successData = (result as AppResult.Success).data
        assertEquals(3, successData.totalExportedPages)
        assertEquals(1, fakeHistory.recordedOperations.size)
        assertEquals(OperationStatus.SUCCESS, fakeHistory.recordedOperations[0].status)
        assertEquals(ToolType.PDF_TO_IMAGES, fakeHistory.recordedOperations[0].toolType)
    }

    @Test
    fun `engine error records FAILED in history and returns Error`() = runBlocking {
        val srcUri = mock(Uri::class.java)
        val destUri = mock(Uri::class.java)
        fakeEngine.pdfToImagesResult = AppResult.Error(AppError.CorruptedPdf("Render error"))

        val result = useCase(
            sourceUri = srcUri,
            sourceName = "doc.pdf",
            selectedPages = listOf(1),
            destinationUri = destUri,
            destinationName = "page_1.jpg",
            onProgress = {}
        )

        assertTrue(result is AppResult.Error)
        assertEquals(1, fakeHistory.recordedOperations.size)
        assertEquals(OperationStatus.FAILED, fakeHistory.recordedOperations[0].status)
    }
}
