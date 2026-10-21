package org.videopocket.probe.domain.model

data class DocumentRecord(
    val id: Long = 0,
    val displayName: String,
    val mimeType: String,
    val uriString: String,
    val sizeBytes: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isAvailable: Boolean = true
)
