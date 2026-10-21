package org.videopocket.probe.domain.usecase

import android.net.Uri
import org.videopocket.probe.core.common.error.AppError
import org.videopocket.probe.core.common.result.AppResult
import org.videopocket.probe.domain.model.OperationStatus
import org.videopocket.probe.domain.model.ToolType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class ImagesToPdfUseCaseTest {

    private lateinit var fakeEngine: FakePdfDocumentEngine
    private lateinit var fakeHistory: FakeHistoryRepository
    private lateinit var useCase: ImagesToPdfUseCase

    @Before
    fun setup() {
        fakeEngine = FakePdfDocumentEngine()
        fakeHistory = FakeHistoryRepository()
        useCase = ImagesToPdfUseCase(fakeEngine, fakeHistory)
    }

    @Test
    fun `empty images list returns Error`() = runBlocking {
        val destUri = mock(Uri::class.java)

        val result = useCase(
            imageUris = emptyList(),
            imageNames = emptyList(),
            destinationUri = destUri,
            destinationName = "out.pdf",
            onProgress = {}
        )

        assertTrue(result is AppResult.Error)
        assertEquals(0, fakeEngine.imagesToPdfCallCount)
    }

    @Test
    fun `successful image conversion records SUCCESS in history`() = runBlocking {
        val imgUri = mock(Uri::class.java)
        val destUri = mock(Uri::class.java)
        fakeEngine.imagesToPdfResult = AppResult.Success(204800L)

        val result = useCase(
            imageUris = listOf(imgUri),
            imageNames = listOf("photo1.jpg"),
            destinationUri = destUri,
            destinationName = "out.pdf",
            onProgress = {}
        )

        assertTrue(result is AppResult.Success)
        assertEquals(1, fakeHistory.recordedOperations.size)
        assertEquals(OperationStatus.SUCCESS, fakeHistory.recordedOperations[0].status)
        assertEquals(ToolType.IMAGES_TO_PDF, fakeHistory.recordedOperations[0].toolType)
    }
}
