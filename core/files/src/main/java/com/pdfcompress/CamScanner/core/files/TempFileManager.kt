package com.pdfcompress.CamScanner.core.files

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class TempFileManager(private val context: Context) {

    private val tempDir: File
        get() = File(context.cacheDir, "pdf_processing_temp").apply {
            if (!exists()) mkdirs()
        }

    fun createTempPdfFile(prefix: String = "doc"): File {
        val uniqueName = "${prefix}_${UUID.randomUUID()}.pdf"
        return File(tempDir, uniqueName)
    }

    fun createTempImageFile(extension: String = "jpg"): File {
        val uniqueName = "img_${UUID.randomUUID()}.$extension"
        return File(tempDir, uniqueName)
    }

    fun copyUriToTempFile(uri: Uri, tempFile: File): Boolean {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream == null) return false
            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }
            tempFile.exists() && tempFile.length() > 0L
        } catch (_: Exception) {
            deleteFileSafely(tempFile)
            false
        }
    }

    fun copyFileToUri(sourceFile: File, destinationUri: Uri): Boolean {
        return try {
            if (!sourceFile.exists() || sourceFile.length() == 0L) return false
            val outputStream = context.contentResolver.openOutputStream(destinationUri, "wt")
                ?: context.contentResolver.openOutputStream(destinationUri)
                ?: return false
            var bytesCopied = 0L
            outputStream.use { output ->
                sourceFile.inputStream().use { input ->
                    bytesCopied = input.copyTo(output)
                    output.flush()
                }
            }
            if (bytesCopied <= 0L) return false
            val verifiedReadable = try {
                context.contentResolver.openInputStream(destinationUri)?.use { stream ->
                    stream.read() != -1
                } ?: true
            } catch (_: Exception) {
                true
            }
            verifiedReadable
        } catch (_: Exception) {
            false
        }
    }

    fun deleteFileSafely(file: File?): Boolean {
        return try {
            if (file != null && file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun cleanAllTempFiles(): Int {
        var count = 0
        try {
            val files = tempDir.listFiles() ?: return 0
            for (file in files) {
                if (file.isFile && file.delete()) {
                    count++
                }
            }
        } catch (_: Exception) {
            // Ignore error during cleanup
        }
        return count
    }
}
