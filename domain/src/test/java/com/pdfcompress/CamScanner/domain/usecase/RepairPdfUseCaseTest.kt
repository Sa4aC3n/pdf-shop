package com.pdfcompress.CamScanner.domain.usecase

import android.net.Uri
import com.pdfcompress.CamScanner.core.common.error.AppError
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.domain.model.OperationStatus
import com.pdfcompress.CamScanner.domain.model.RepairReport
import com.pdfcompress.CamScanner.domain.model.ToolType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class RepairPdfUseCaseTest {

    private lateinit var fakeEngine: FakePdfDocumentEngine
    private lateinit var fakeHistory: FakeHistoryRepository
    private lateinit var useCase: RepairPdfUseCase

    @Before
    fun setup() {
        fakeEngine = FakePdfDocumentEngine()
        fakeHistory = FakeHistoryRepository()
        useCase = RepairPdfUseCase(fakeEngine, fakeHistory)
    }

    @Test
    fun `successful repair records in history and returns Success`() = runBlocking {
        val srcUri = mock(Uri::class.java)
        val destUri = mock(Uri::class.java)
        fakeEngine.repairResult = AppResult.Success(
            RepairReport(
                isSuccess = true,
                recoveredPagesCount = 10,
                outputSizeBytes = 1048576L,
                diagnosisMessage = "تم إصلاح جدول الفهارس واسترداد 10 صفحات"
            )
        )

        val result = useCase(
            sourceUri = srcUri,
            sourceName = "damaged.pdf",
            destinationUri = destUri,
            destinationName = "repaired.pdf",
            onProgress = {}
        )

        assertTrue(result is AppResult.Success)
        val data = (result as AppResult.Success).data
        assertTrue(data.isSuccess)
        assertEquals(10, data.recoveredPagesCount)
        assertEquals(1, fakeHistory.recordedOperations.size)
        assertEquals(OperationStatus.SUCCESS, fakeHistory.recordedOperations[0].status)
        assertEquals(ToolType.REPAIR, fakeHistory.recordedOperations[0].toolType)
    }

    @Test
    fun `engine error records FAILED in history and returns Error`() = runBlocking {
        val srcUri = mock(Uri::class.java)
        val destUri = mock(Uri::class.java)
        fakeEngine.repairResult = AppResult.Error(AppError.CorruptedPdf("Fatal corruption"))

        val result = useCase(
            sourceUri = srcUri,
            sourceName = "damaged.pdf",
            destinationUri = destUri,
            destinationName = "repaired.pdf",
            onProgress = {}
        )

        assertTrue(result is AppResult.Error)
        assertEquals(1, fakeHistory.recordedOperations.size)
        assertEquals(OperationStatus.FAILED, fakeHistory.recordedOperations[0].status)
    }
}
