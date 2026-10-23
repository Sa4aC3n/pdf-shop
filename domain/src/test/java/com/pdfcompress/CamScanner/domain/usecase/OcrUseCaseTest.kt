package com.pdfcompress.CamScanner.domain.usecase

import android.net.Uri
import com.pdfcompress.CamScanner.core.common.error.AppError
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.domain.model.OperationStatus
import com.pdfcompress.CamScanner.domain.model.TextExtractionResult
import com.pdfcompress.CamScanner.domain.model.ToolType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class OcrUseCaseTest {

    private lateinit var fakeEngine: FakePdfDocumentEngine
    private lateinit var fakeHistory: FakeHistoryRepository
    private lateinit var useCase: OcrUseCase

    @Before
    fun setup() {
        fakeEngine = FakePdfDocumentEngine()
        fakeHistory = FakeHistoryRepository()
        useCase = OcrUseCase(fakeEngine, fakeHistory)
    }

    @Test
    fun `successful OCR records OCR in history and returns Success`() = runBlocking {
        val srcUri = mock(Uri::class.java)
        val extractedText = "مستند تجريبي يحتوي على نص عربي واضح"
        fakeEngine.extractTextResult = AppResult.Success(
            TextExtractionResult(
                text = extractedText,
                characterCount = extractedText.length,
                wordCount = 7,
                pageCount = 1
            )
        )

        val result = useCase(
            sourceUri = srcUri,
            sourceName = "document.pdf",
            isPdf = true,
            onProgress = {}
        )

        assertTrue(result is AppResult.Success)
        val data = (result as AppResult.Success).data
        assertEquals(7, data.wordCount)
        assertEquals(1, fakeHistory.recordedOperations.size)
        assertEquals(OperationStatus.SUCCESS, fakeHistory.recordedOperations[0].status)
        assertEquals(ToolType.OCR, fakeHistory.recordedOperations[0].toolType)
    }

    @Test
    fun `OCR engine error records FAILED in history and returns Error`() = runBlocking {
        val srcUri = mock(Uri::class.java)
        fakeEngine.extractTextResult = AppResult.Error(AppError.CorruptedPdf("Unreadable scan"))

        val result = useCase(
            sourceUri = srcUri,
            sourceName = "damaged.pdf",
            isPdf = true,
            onProgress = {}
        )

        assertTrue(result is AppResult.Error)
        assertEquals(1, fakeHistory.recordedOperations.size)
        assertEquals(OperationStatus.FAILED, fakeHistory.recordedOperations[0].status)
    }
}
