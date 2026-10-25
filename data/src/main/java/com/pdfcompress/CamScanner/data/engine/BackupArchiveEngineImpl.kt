package com.pdfcompress.CamScanner.data.engine

import android.content.Context
import android.net.Uri
import com.pdfcompress.CamScanner.core.common.dispatcher.DispatcherProvider
import com.pdfcompress.CamScanner.core.common.error.AppError
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.core.files.SafFileHelper
import com.pdfcompress.CamScanner.core.files.TempFileManager
import com.pdfcompress.CamScanner.domain.engine.EngineProgress
import com.pdfcompress.CamScanner.domain.model.BackupReport
import com.pdfcompress.CamScanner.domain.model.OperationRecord
import com.pdfcompress.CamScanner.domain.model.OperationStatus
import com.pdfcompress.CamScanner.domain.model.ToolType
import com.pdfcompress.CamScanner.domain.usecase.BackupArchiveEngine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupArchiveEngineImpl(
    private val context: Context,
    private val safHelper: SafFileHelper,
    private val tempFileManager: TempFileManager,
    private val dispatchers: DispatcherProvider
) : BackupArchiveEngine {

    companion object {
        private val MAGIC_HEADER = "PDFSHOP_AES256".toByteArray(Charsets.UTF_8)
        private const val SALT_LEN = 16
        private const val IV_LEN = 12
        private const val KEY_LEN = 256
        private const val ITERATIONS = 65536
        private const val GCM_TAG_LEN = 128
    }

    override suspend fun createArchive(
        historyRecords: List<OperationRecord>,
        password: String?,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<BackupReport> = withContext(dispatchers.io) {
        try {
            onProgress(EngineProgress(10, "تجهيز بيانات الأرشيف وسجلات المستندات…"))

            val tempZipFile = tempFileManager.createTempPdfFile("backup_staging")
            val zipOut = ZipOutputStream(FileOutputStream(tempZipFile))

            // 1. Write history.json
            val jsonArray = JSONArray()
            for (rec in historyRecords) {
                val obj = JSONObject().apply {
                    put("id", rec.id)
                    put("toolType", rec.toolType.name)
                    put("status", rec.status.name)
                    put("startedAt", rec.startedAt)
                    put("completedAt", rec.completedAt)
                    put("inputNames", JSONArray(rec.inputDocumentNames))
                    put("outputName", rec.outputDocumentName ?: "")
                    put("outputUri", rec.outputDocumentUri ?: "")
                    put("outputSizeBytes", rec.outputSizeBytes ?: 0L)
                    put("summary", rec.summary ?: "")
                    put("errorMessage", rec.errorMessage ?: "")
                }
                jsonArray.put(obj)
            }

            val historyBytes = jsonArray.toString(2).toByteArray(Charsets.UTF_8)
            zipOut.putNextEntry(ZipEntry("history.json"))
            zipOut.write(historyBytes)
            zipOut.closeEntry()

            // 2. Add available files
            onProgress(EngineProgress(35, "تجميع ملفات المستندات والمستمسكات المحفوظة…"))
            var includedFiles = 0
            for ((index, rec) in historyRecords.withIndex()) {
                val uriStr = rec.outputDocumentUri
                if (!uriStr.isNullOrBlank()) {
                    try {
                        val fileUri = Uri.parse(uriStr)
                        val fileName = rec.outputDocumentName ?: "doc_${rec.id}.pdf"
                        context.contentResolver.openInputStream(fileUri)?.use { input ->
                            zipOut.putNextEntry(ZipEntry("files/$fileName"))
                            input.copyTo(zipOut)
                            zipOut.closeEntry()
                            includedFiles++
                        }
                    } catch (_: Exception) {
                        // Skip unreadable files
                    }
                }
                val pct = 35 + ((index + 1) * 35 / maxOf(1, historyRecords.size))
                onProgress(EngineProgress(pct, "تمت إضافة $includedFiles ملف إلى الأرشيف…"))
            }
            zipOut.close()

            val rawZipBytes = tempZipFile.readBytes()
            tempFileManager.deleteFileSafely(tempZipFile)

            val finalBytes = if (!password.isNullOrBlank()) {
                onProgress(EngineProgress(80, "تشفير الأرشيف بتقنية AES-256 محلياً…"))
                encryptData(rawZipBytes, password)
            } else {
                rawZipBytes
            }

            onProgress(EngineProgress(95, "كتابة وحفظ ملف الأرشيف النهائي…"))
            val written = writeBytesToUri(destinationUri, finalBytes)
            if (!written) {
                return@withContext AppResult.Error(AppError.InsufficientStorage("تعذر حفظ ملف الأرشيف في المسار المحدد"))
            }

            onProgress(EngineProgress(100, "اكتمل إنشاء النسخة الاحتياطية بنجاح"))
            val report = BackupReport(
                totalFilesCount = includedFiles,
                totalRecordsCount = historyRecords.size,
                outputSizeBytes = finalBytes.size.toLong(),
                isEncrypted = !password.isNullOrBlank(),
                summary = "تم تجميع ${historyRecords.size} مستند و $includedFiles ملف في حزمة واحدة"
            )
            AppResult.Success(report)
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.message ?: "فشل غير متوقع في إنشاء الأرشيف"))
        }
    }

    override suspend fun restoreArchive(
        sourceUri: Uri,
        password: String?,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<List<OperationRecord>> = withContext(dispatchers.io) {
        try {
            onProgress(EngineProgress(15, "قراءة ملف النسخة الاحتياطية…"))
            val rawBytes = readBytesFromUri(sourceUri)
                ?: return@withContext AppResult.Error(AppError.CorruptedPdf("تعذر قراءة ملف النسخة الاحتياطية"))

            val zipBytes = if (isEncrypted(rawBytes)) {
                if (password.isNullOrBlank()) {
                    return@withContext AppResult.Error(AppError.CorruptedPdf("هذه النسخة الاحتياطية محمية بكلمة سر؛ يرجى إدخال كلمة السر لفك التشفير"))
                }
                onProgress(EngineProgress(40, "فك تشفير AES-256 والتحقق من كلمة السر…"))
                try {
                    decryptData(rawBytes, password)
                } catch (e: Exception) {
                    return@withContext AppResult.Error(AppError.CorruptedPdf("كلمة السر غير صحيحة أو الملف تالف"))
                }
            } else {
                rawBytes
            }

            onProgress(EngineProgress(65, "استخراج المستندات والسجلات من الأرشيف…"))
            val restoredRecords = mutableListOf<OperationRecord>()
            val zipIn = ZipInputStream(ByteArrayInputStream(zipBytes))
            var entry = zipIn.nextEntry

            while (entry != null) {
                if (entry.name == "history.json") {
                    val jsonBytes = zipIn.readBytes()
                    val jsonStr = String(jsonBytes, Charsets.UTF_8)
                    val jsonArr = JSONArray(jsonStr)
                    for (i in 0 until jsonArr.length()) {
                        val obj = jsonArr.getJSONObject(i)
                        val toolTypeStr = obj.optString("toolType", "MERGE")
                        val statusStr = obj.optString("status", "SUCCESS")
                        val inputNamesArr = obj.optJSONArray("inputNames") ?: JSONArray()
                        val inputNames = mutableListOf<String>()
                        for (k in 0 until inputNamesArr.length()) {
                            inputNames.add(inputNamesArr.getString(k))
                        }

                        val rec = OperationRecord(
                            id = 0, // new ID generated by DB
                            toolType = try { ToolType.valueOf(toolTypeStr) } catch (_: Exception) { ToolType.MERGE },
                            status = try { OperationStatus.valueOf(statusStr) } catch (_: Exception) { OperationStatus.SUCCESS },
                            startedAt = obj.optLong("startedAt", System.currentTimeMillis()),
                            completedAt = obj.optLong("completedAt", System.currentTimeMillis()),
                            inputDocumentNames = inputNames,
                            outputDocumentName = if (obj.has("outputName") && !obj.isNull("outputName")) obj.getString("outputName") else null,
                            outputDocumentUri = if (obj.has("outputUri") && !obj.isNull("outputUri")) obj.getString("outputUri") else null,
                            outputSizeBytes = if (obj.has("outputSizeBytes") && !obj.isNull("outputSizeBytes")) obj.optLong("outputSizeBytes") else null,
                            summary = if (obj.has("summary") && !obj.isNull("summary")) obj.getString("summary") else null,
                            errorMessage = if (obj.has("errorMessage") && !obj.isNull("errorMessage")) obj.getString("errorMessage") else null,
                            isOutputAvailable = true
                        )
                        restoredRecords.add(rec)
                    }
                } else if (entry.name.startsWith("files/")) {
                    val fileName = entry.name.removePrefix("files/")
                    if (fileName.isNotBlank()) {
                        val fileBytes = zipIn.readBytes()
                        saveBytesToInternalDocs(fileName, fileBytes)
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
            zipIn.close()

            onProgress(EngineProgress(100, "تمت استعادة ${restoredRecords.size} سجل مستند بنجاح"))
            AppResult.Success(restoredRecords)
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.message ?: "فشل في استعادة النسخة الاحتياطية"))
        }
    }

    private fun isEncrypted(bytes: ByteArray): Boolean {
        if (bytes.size < MAGIC_HEADER.size) return false
        for (i in MAGIC_HEADER.indices) {
            if (bytes[i] != MAGIC_HEADER[i]) return false
        }
        return true
    }

    private fun encryptData(data: ByteArray, password: String): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LEN).also { random.nextBytes(it) }
        val iv = ByteArray(IV_LEN).also { random.nextBytes(it) }

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LEN)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LEN, iv))
        val encrypted = cipher.doFinal(data)

        val out = ByteArrayOutputStream()
        out.write(MAGIC_HEADER)
        out.write(salt)
        out.write(iv)
        out.write(encrypted)
        return out.toByteArray()
    }

    private fun decryptData(data: ByteArray, password: String): ByteArray {
        var offset = MAGIC_HEADER.size
        val salt = data.copyOfRange(offset, offset + SALT_LEN)
        offset += SALT_LEN
        val iv = data.copyOfRange(offset, offset + IV_LEN)
        offset += IV_LEN
        val encrypted = data.copyOfRange(offset, data.size)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LEN)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LEN, iv))
        return cipher.doFinal(encrypted)
    }

    private fun writeBytesToUri(uri: Uri, bytes: ByteArray): Boolean {
        return try {
            safHelper.openOutputStream(uri)?.use { out ->
                out.write(bytes)
                out.flush()
                true
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun readBytesFromUri(uri: Uri): ByteArray? {
        return try {
            safHelper.openInputStream(uri)?.use { it.readBytes() }
        } catch (_: Exception) {
            null
        }
    }

    private fun saveBytesToInternalDocs(fileName: String, bytes: ByteArray): File? {
        return try {
            val restoredDir = File(context.filesDir, "restored_documents").apply { if (!exists()) mkdirs() }
            val file = File(restoredDir, fileName)
            file.writeBytes(bytes)
            file
        } catch (_: Exception) {
            null
        }
    }
}
