package com.pdfcompress.CamScanner.core.files

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import java.io.InputStream
import java.io.OutputStream

class SafFileHelper(private val context: Context) {

    fun getDisplayName(uri: Uri): String {
        var name = "document.pdf"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val queriedName = cursor.getString(nameIndex)
                    if (!queriedName.isNullOrBlank()) {
                        name = queriedName
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback to last path segment if cursor query fails
            uri.lastPathSegment?.let { segment ->
                if (segment.isNotBlank()) name = segment
            }
        }
        return name
    }

    fun getFileSize(uri: Uri): Long {
        var size = -1L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1 && cursor.moveToFirst()) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        } catch (_: Exception) {
            size = -1L
        }
        return size
    }

    fun getMimeType(uri: Uri): String {
        return context.contentResolver.getType(uri) ?: "application/pdf"
    }

    fun isUriAccessible(uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (_: Exception) {
            false
        }
    }

    fun takePersistableUriPermission(uri: Uri) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: SecurityException) {
            // Some providers do not support persistable permissions; continue safely
        }
    }

    fun openInputStream(uri: Uri): InputStream? {
        return try {
            context.contentResolver.openInputStream(uri)
        } catch (_: Exception) {
            null
        }
    }

    fun openOutputStream(uri: Uri): OutputStream? {
        return try {
            context.contentResolver.openOutputStream(uri, "wt")
        } catch (_: Exception) {
            null
        }
    }
}
