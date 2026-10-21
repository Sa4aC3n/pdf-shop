package org.videopocket.probe.core.common

import org.videopocket.probe.core.common.error.AppError
import org.videopocket.probe.core.common.result.AppResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppResultTest {

    @Test
    fun `test Success state`() {
        val result: AppResult<String> = AppResult.Success("PDF Processed")
        assertTrue(result.isSuccess)
        assertEquals("PDF Processed", result.getOrNull())
    }

    @Test
    fun `test Error state`() {
        val error = AppError.CorruptedPdf("Invalid PDF trailer")
        val result: AppResult<String> = AppResult.Error(error)
        assertFalse(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `test Progress state`() {
        val result: AppResult<String> = AppResult.Progress(50, "Merging")
        assertFalse(result.isSuccess)
        assertNull(result.getOrNull())
    }
}
