package org.videopocket.probe.data.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.videopocket.probe.core.common.dispatcher.DispatcherProvider
import org.videopocket.probe.core.common.error.AppError
import org.videopocket.probe.core.common.result.AppResult
import org.videopocket.probe.core.files.SafFileHelper
import org.videopocket.probe.core.files.TempFileManager
import org.videopocket.probe.domain.engine.EngineProgress
import org.videopocket.probe.domain.engine.PdfDocumentEngine
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfBoxDocumentEngine(
    private val context: Context,
    private val safHelper: SafFileHelper,
    private val tempFileManager: TempFileManager,
    private val dispatchers: DispatcherProvider
) : PdfDocumentEngine {

    override suspend fun merge(
        inputUris: List<Uri>,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> = withContext(dispatchers.io) {
        val tempInputFiles = mutableListOf<File>()
        var tempOutputFile: File? = null

        try {
            onProgress(EngineProgress(5, "تحضير الملفات المؤقتة"))

            val totalFiles = inputUris.size
            for ((index, uri) in inputUris.withIndex()) {
                val tempFile = tempFileManager.createTempPdfFile("merge_input_$index")
                val copied = tempFileManager.copyUriToTempFile(uri, tempFile)
                if (!copied || !tempFile.exists() || tempFile.length() == 0L) {
                    return@withContext AppResult.Error(
                        AppError.FileNotFound("تعذر قراءة الملف رقم ${index + 1} أو الملف فارغ")
                    )
                }
                tempInputFiles.add(tempFile)
                val progressPercent = 5 + ((index + 1) * 35 / totalFiles)
                onProgress(EngineProgress(progressPercent, "قراءة الملف ${index + 1} من $totalFiles"))
            }

            onProgress(EngineProgress(45, "جارٍ دمج صفحات PDF محلياً"))

            tempOutputFile = tempFileManager.createTempPdfFile("merge_output")
            val merger = PDFMergerUtility().apply {
                destinationFileName = tempOutputFile.absolutePath
            }

            for (file in tempInputFiles) {
                merger.addSource(file)
            }

            // Merge with temporary scratch memory to prevent OutOfMemory on large files
            merger.mergeDocuments(MemoryUsageSetting.setupTempFileOnly())

            onProgress(EngineProgress(80, "التحقق من صحة المستند المدمج"))

            if (!tempOutputFile.exists() || tempOutputFile.length() == 0L) {
                return@withContext AppResult.Error(
                    AppError.CorruptedPdf("فشل إنتاج ملف PDF مدمج صالح")
                )
            }

            // Verify the generated PDF by opening it
            try {
                PDDocument.load(tempOutputFile).use { doc ->
                    if (doc.numberOfPages == 0) {
                        return@withContext AppResult.Error(
                            AppError.CorruptedPdf("المستند الناتج لا يحتوي على صفحات صالحة")
                        )
                    }
                }
            } catch (e: Exception) {
                return@withContext AppResult.Error(
                    AppError.CorruptedPdf("تلف في هيكل الـ PDF الناتج", e)
                )
            }

            onProgress(EngineProgress(90, "حفظ المستند في الوجهة المحددة"))

            val saved = tempFileManager.copyFileToUri(tempOutputFile, destinationUri)
            if (!saved) {
                return@withContext AppResult.Error(
                    AppError.InsufficientStorage("تعذر حفظ الملف في الوجهة المختارة")
                )
            }

            val finalSize = safHelper.getFileSize(destinationUri).let {
                if (it > 0) it else tempOutputFile.length()
            }

            onProgress(EngineProgress(100, "اكتمل الدمج بنجاح"))
            AppResult.Success(finalSize)

        } catch (oom: OutOfMemoryError) {
            AppResult.Error(AppError.OutOfMemory("نفاد ذاكرة الجهاز أثناء دمج الملفات"))
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.localizedMessage ?: "حدث خطأ غير متوقع أثناء الدمج", e))
        } finally {
            for (file in tempInputFiles) {
                tempFileManager.deleteFileSafely(file)
            }
            tempFileManager.deleteFileSafely(tempOutputFile)
        }
    }

    override suspend fun imagesToPdf(
        imageUris: List<Uri>,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> = withContext(dispatchers.io) {
        var tempOutputFile: File? = null
        val totalImages = imageUris.size

        try {
            onProgress(EngineProgress(5, "بدء إنشاء مستند PDF جديد"))

            val document = PDDocument()
            tempOutputFile = tempFileManager.createTempPdfFile("images_output")

            for ((index, uri) in imageUris.withIndex()) {
                val inputStream = safHelper.openInputStream(uri)
                    ?: return@withContext AppResult.Error(
                        AppError.FileNotFound("تعذر قراءة الصورة رقم ${index + 1}")
                    )

                val bitmap = inputStream.use { stream ->
                    BitmapFactory.decodeStream(stream)
                } ?: return@withContext AppResult.Error(
                    AppError.UnsupportedFormat("تعذر فك ترميز الصورة رقم ${index + 1}")
                )

                val pageWidth = bitmap.width.toFloat()
                val pageHeight = bitmap.height.toFloat()
                val page = PDPage(PDRectangle(pageWidth, pageHeight))
                document.addPage(page)

                val pdImage = JPEGFactory.createFromImage(document, bitmap, 0.85f)
                bitmap.recycle()

                PDPageContentStream(document, page).use { contentStream ->
                    contentStream.drawImage(pdImage, 0f, 0f, pageWidth, pageHeight)
                }

                val currentProgress = 5 + ((index + 1) * 75 / totalImages)
                onProgress(EngineProgress(currentProgress, "إضافة الصورة ${index + 1} من $totalImages"))
            }

            onProgress(EngineProgress(85, "حفظ صفحات PDF وتنسيقها"))

            FileOutputStream(tempOutputFile).use { output ->
                document.save(output)
            }
            document.close()

            if (!tempOutputFile.exists() || tempOutputFile.length() == 0L) {
                return@withContext AppResult.Error(
                    AppError.CorruptedPdf("فشل إنشاء ملف PDF من الصور")
                )
            }

            onProgress(EngineProgress(95, "نسخ الملف النهائي إلى وجهة الحفظ"))

            val copied = tempFileManager.copyFileToUri(tempOutputFile, destinationUri)
            if (!copied) {
                return@withContext AppResult.Error(
                    AppError.InsufficientStorage("تعذر حفظ المستند النهائي في الوجهة المختارة")
                )
            }

            val finalSize = safHelper.getFileSize(destinationUri).let {
                if (it > 0) it else tempOutputFile.length()
            }

            onProgress(EngineProgress(100, "تم التحويل بنجاح"))
            AppResult.Success(finalSize)

        } catch (oom: OutOfMemoryError) {
            AppResult.Error(AppError.OutOfMemory("نفاد ذاكرة الجهاز أثناء معالجة الصور"))
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.localizedMessage ?: "حدث خطأ أثناء تحويل الصور", e))
        } finally {
            tempFileManager.deleteFileSafely(tempOutputFile)
        }
    }

    override suspend fun splitOrExtract(
        sourceUri: Uri,
        selectedPages: List<Int>,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> = withContext(dispatchers.io) {
        var tempInputFile: File? = null
        var tempOutputFile: File? = null

        try {
            onProgress(EngineProgress(10, "تحضير المستند الأصلي"))

            tempInputFile = tempFileManager.createTempPdfFile("split_source")
            val copied = tempFileManager.copyUriToTempFile(sourceUri, tempInputFile)
            if (!copied || !tempInputFile.exists() || tempInputFile.length() == 0L) {
                return@withContext AppResult.Error(
                    AppError.FileNotFound("تعذر فتح ملف الـ PDF الأصلي")
                )
            }

            onProgress(EngineProgress(30, "قراءة الصفحات المحددة"))

            val sourceDoc = PDDocument.load(tempInputFile)
            val newDoc = PDDocument()
            tempOutputFile = tempFileManager.createTempPdfFile("split_output")

            val totalSourcePages = sourceDoc.numberOfPages
            val validPages = selectedPages.filter { it in 1..totalSourcePages }

            if (validPages.isEmpty()) {
                sourceDoc.close()
                newDoc.close()
                return@withContext AppResult.Error(
                    AppError.Unknown("أرقام الصفحات المحددة غير موجودة في هذا المستند (إجمالي الصفحات: $totalSourcePages)")
                )
            }

            for ((index, pageNum) in validPages.withIndex()) {
                val page = sourceDoc.getPage(pageNum - 1)
                newDoc.addPage(page)
                val progressPercent = 30 + ((index + 1) * 45 / validPages.size)
                onProgress(EngineProgress(progressPercent, "استخراج الصفحة $pageNum"))
            }

            onProgress(EngineProgress(80, "كتابة المستند المستخرج"))

            FileOutputStream(tempOutputFile).use { output ->
                newDoc.save(output)
            }
            newDoc.close()
            sourceDoc.close()

            if (!tempOutputFile.exists() || tempOutputFile.length() == 0L) {
                return@withContext AppResult.Error(
                    AppError.CorruptedPdf("فشل حفظ الصفحات المستخرجة")
                )
            }

            onProgress(EngineProgress(95, "حفظ المستند في الوجهة المختارة"))

            val saved = tempFileManager.copyFileToUri(tempOutputFile, destinationUri)
            if (!saved) {
                return@withContext AppResult.Error(
                    AppError.InsufficientStorage("تعذر حفظ المستند المستخرج")
                )
            }

            val finalSize = safHelper.getFileSize(destinationUri).let {
                if (it > 0) it else tempOutputFile.length()
            }

            onProgress(EngineProgress(100, "اكتمل استخراج الصفحات بنجاح"))
            AppResult.Success(finalSize)

        } catch (oom: OutOfMemoryError) {
            AppResult.Error(AppError.OutOfMemory("نفاد ذاكرة الجهاز أثناء استخراج الصفحات"))
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.localizedMessage ?: "حدث خطأ أثناء استخراج الصفحات", e))
        } finally {
            tempFileManager.deleteFileSafely(tempInputFile)
            tempFileManager.deleteFileSafely(tempOutputFile)
        }
    }

    override suspend fun getPageCount(sourceUri: Uri): Int = withContext(dispatchers.io) {
        var tempFile: File? = null
        try {
            tempFile = tempFileManager.createTempPdfFile("count_check")
            val copied = tempFileManager.copyUriToTempFile(sourceUri, tempFile)
            if (copied && tempFile.exists()) {
                PDDocument.load(tempFile).use { doc ->
                    doc.numberOfPages
                }
            } else {
                0
            }
        } catch (_: Exception) {
            0
        } finally {
            tempFileManager.deleteFileSafely(tempFile)
        }
    }
}
