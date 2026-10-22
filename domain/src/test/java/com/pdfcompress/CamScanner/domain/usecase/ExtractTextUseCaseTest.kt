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

class ExtractTextUseCaseTest {

    private lateinit var fakeEngine: FakePdfDocumentEngine
    private lateinit var fakeHistory: FakeHistoryRepository
    private lateinit var useCase: ExtractTextUseCase

    @Before
    fun setup() {
        fakeEngine = FakePdfDocumentEngine()
        fakeHistory = FakeHistoryRepository()
        useCase = ExtractTextUseCase(fakeEngine, fakeHistory)
    }

    @Test
    fun `successful text extraction records in history and returns Success`() = runBlocking {
        val srcUri = mock(Uri::class.java)
        val sampleText = "بسم الله الرحمن الرحيم - مستند تجريبي"
        fakeEngine.extractTextResult = AppResult.Success(
            TextExtractionResult(
                text = sampleText,
                pageCount = 1,
                wordCount = 7,
                characterCount = sampleText.length
            )
        )

        val result = useCase(
            sourceUri = srcUri,
            sourceName = "arabic_doc.pdf",
            onProgress = {}
        )

        assertTrue(result is AppResult.Success)
        val data = (result as AppResult.Success).data
        assertEquals(7, data.wordCount)
        assertEquals(1, data.pageCount)
        assertEquals(1, fakeHistory.recordedOperations.size)
        assertEquals(OperationStatus.SUCCESS, fakeHistory.recordedOperations[0].status)
        assertEquals(ToolType.EXTRACT_TEXT, fakeHistory.recordedOperations[0].toolType)
    }

    @Test
    fun `engine error records FAILED in history and returns Error`() = runBlocking {
        val srcUri = mock(Uri::class.java)
        fakeEngine.extractTextResult = AppResult.Error(AppError.CorruptedPdf("Unreadable stream"))

        val result = useCase(
            sourceUri = srcUri,
            sourceName = "corrupt.pdf",
            onProgress = {}
        )

        assertTrue(result is AppResult.Error)
        assertEquals(1, fakeHistory.recordedOperations.size)
        assertEquals(OperationStatus.FAILED, fakeHistory.recordedOperations[0].status)
    }
}
