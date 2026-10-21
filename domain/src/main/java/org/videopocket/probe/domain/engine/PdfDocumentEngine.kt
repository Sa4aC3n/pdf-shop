package org.videopocket.probe.domain.engine

import android.net.Uri
import org.videopocket.probe.core.common.result.AppResult

data class EngineProgress(
    val percentage: Int,
    val stage: String? = null
)

interface PdfDocumentEngine {

    suspend fun merge(
        inputUris: List<Uri>,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long>

    suspend fun imagesToPdf(
        imageUris: List<Uri>,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long>

    suspend fun splitOrExtract(
        sourceUri: Uri,
        selectedPages: List<Int>,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long>

    suspend fun getPageCount(sourceUri: Uri): Int
}
