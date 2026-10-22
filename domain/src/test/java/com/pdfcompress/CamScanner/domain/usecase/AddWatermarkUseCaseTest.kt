package com.pdfcompress.CamScanner.domain.usecase

import android.net.Uri
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.domain.model.OperationStatus
import com.pdfcompress.CamScanner.domain.model.ToolType
import com.pdfcompress.CamScanner.domain.model.WatermarkOptions
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class AddWatermarkUseCaseTest {

    private lateinit var fakeEngine: FakePdfDocumentEngine
    private lateinit var fakeHistory: FakeHistoryRepository
    private lateinit var useCase: AddWatermarkUseCase

    @Before
    fun setup() {
        fakeEngine = FakePdfDocumentEngine()
        fakeHistory = FakeHistoryRepository()
        useCase = AddWatermarkUseCase(fakeEngine, fakeHistory)
    }

    @Test
    fun `blank watermark text returns Error`() = runBlocking {
        val srcUri = mock(Uri::class.java)
        val destUri = mock(Uri::class.java)

        val result = useCase(
            sourceUri = srcUri,
            sourceName = "doc.pdf",
            options = WatermarkOptions(text = "   "),
            destinationUri = destUri,
            destinationName = "watermarked.pdf",
            onProgress = {}
        )

        assertTrue(result is AppResult.Error)
        assertEquals(0, fakeHistory.recordedOperations.size)
    }

    @Test
    fun `successful watermarking records SUCCESS in history`() = runBlocking {
        val srcUri = mock(Uri::class.java)
        val destUri = mock(Uri::class.java)

        fakeEngine.watermarkResult = AppResult.Success(102400L)

        val result = useCase(
            sourceUri = srcUri,
            sourceName = "doc.pdf",
            options = WatermarkOptions(text = "سري للغاية"),
            destinationUri = destUri,
            destinationName = "watermarked.pdf",
            onProgress = {}
        )

        assertTrue(result is AppResult.Success)
        assertEquals(1, fakeHistory.recordedOperations.size)
        assertEquals(OperationStatus.SUCCESS, fakeHistory.recordedOperations[0].status)
        assertEquals(ToolType.WATERMARK, fakeHistory.recordedOperations[0].toolType)
    }
}
