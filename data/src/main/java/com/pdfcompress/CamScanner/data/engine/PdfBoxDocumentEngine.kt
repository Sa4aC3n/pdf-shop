package com.pdfcompress.CamScanner.data.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.pdfcompress.CamScanner.core.common.dispatcher.DispatcherProvider
import com.pdfcompress.CamScanner.core.common.error.AppError
import com.pdfcompress.CamScanner.core.common.result.AppResult
import com.pdfcompress.CamScanner.core.files.SafFileHelper
import com.pdfcompress.CamScanner.core.files.TempFileManager
import com.pdfcompress.CamScanner.domain.engine.EngineProgress
import com.pdfcompress.CamScanner.domain.engine.PdfDocumentEngine
import com.pdfcompress.CamScanner.domain.model.CompressionLevel
import com.pdfcompress.CamScanner.domain.model.CompressionResult
import com.pdfcompress.CamScanner.domain.model.WatermarkOptions
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.util.Matrix
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

            merger.mergeDocuments(MemoryUsageSetting.setupTempFileOnly())

            onProgress(EngineProgress(80, "التحقق من صحة المستند المدمج"))

            if (!tempOutputFile.exists() || tempOutputFile.length() == 0L) {
                return@withContext AppResult.Error(
                    AppError.CorruptedPdf("فشل إنتاج ملف PDF مدمج صالح")
                )
            }

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

    override suspend fun compress(
        sourceUri: Uri,
        level: CompressionLevel,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<CompressionResult> = withContext(dispatchers.io) {
        var tempInputFile: File? = null
        var tempOutputFile: File? = null

        try {
            onProgress(EngineProgress(10, "تحضير المستند الأصلي للضغط"))

            tempInputFile = tempFileManager.createTempPdfFile("compress_source")
            val copied = tempFileManager.copyUriToTempFile(sourceUri, tempInputFile)
            if (!copied || !tempInputFile.exists() || tempInputFile.length() == 0L) {
                return@withContext AppResult.Error(
                    AppError.FileNotFound("تعذر فتح ملف الـ PDF الأصلي")
                )
            }

            val originalSize = tempInputFile.length()
            onProgress(EngineProgress(25, "فحص الصور والمحتوى الداخلي"))

            val doc = PDDocument.load(tempInputFile)
            tempOutputFile = tempFileManager.createTempPdfFile("compress_output")

            val totalPages = doc.numberOfPages
            for ((index, page) in doc.pages.withIndex()) {
                val resources = page.resources
                if (resources != null) {
                    for (xName in resources.xObjectNames) {
                        val xobject = resources.getXObject(xName)
                        if (xobject is PDImageXObject) {
                            try {
                                val originalBmp = xobject.image
                                if (originalBmp != null) {
                                    val maxDim = level.maxDimension
                                    val maxCurrent = maxOf(originalBmp.width, originalBmp.height)
                                    val scale = if (maxCurrent > maxDim) {
                                        maxDim.toFloat() / maxCurrent.toFloat()
                                    } else 1.0f

                                    val newW = (originalBmp.width * scale).toInt().coerceAtLeast(1)
                                    val newH = (originalBmp.height * scale).toInt().coerceAtLeast(1)

                                    val scaledBmp = if (scale < 1.0f) {
                                        Bitmap.createScaledBitmap(originalBmp, newW, newH, true)
                                    } else {
                                        originalBmp
                                    }

                                    val newImageXObject = JPEGFactory.createFromImage(doc, scaledBmp, level.quality)
                                    if (scaledBmp != originalBmp) {
                                        scaledBmp.recycle()
                                    }
                                    originalBmp.recycle()

                                    resources.put(xName, newImageXObject)
                                }
                            } catch (_: Exception) {
                                // Skip non-compressible / vector streams
                            }
                        }
                    }
                }
                val pct = 25 + ((index + 1) * 50 / totalPages.coerceAtLeast(1))
                onProgress(EngineProgress(pct, "ضغط الصفحة ${index + 1} من $totalPages"))
            }

            onProgress(EngineProgress(80, "إعادة كتابة هيكل الـ PDF وضغط الجداول"))

            FileOutputStream(tempOutputFile).use { out ->
                doc.save(out)
            }
            doc.close()

            if (!tempOutputFile.exists() || tempOutputFile.length() == 0L) {
                return@withContext AppResult.Error(
                    AppError.CorruptedPdf("فشل حفظ المستند المضغوط")
                )
            }

            val compressedSize = tempOutputFile.length()
            val isSmaller = compressedSize < originalSize
            val reductionPct = if (isSmaller && originalSize > 0) {
                (((originalSize - compressedSize) * 100) / originalSize).toInt()
            } else 0

            onProgress(EngineProgress(95, "حفظ المستند في الوجهة المحددة"))

            val saved = tempFileManager.copyFileToUri(tempOutputFile, destinationUri)
            if (!saved) {
                return@withContext AppResult.Error(
                    AppError.InsufficientStorage("تعذر حفظ الملف المضغوط في الوجهة المحددة")
                )
            }

            onProgress(EngineProgress(100, "اكتملت عملية الضغط بنجاح"))

            AppResult.Success(
                CompressionResult(
                    originalSizeBytes = originalSize,
                    compressedSizeBytes = compressedSize,
                    reductionPercentage = reductionPct,
                    isSmaller = isSmaller
                )
            )

        } catch (oom: OutOfMemoryError) {
            AppResult.Error(AppError.OutOfMemory("نفاد ذاكرة الجهاز أثناء ضغط المستند"))
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.localizedMessage ?: "حدث خطأ أثناء ضغط الملف", e))
        } finally {
            tempFileManager.deleteFileSafely(tempInputFile)
            tempFileManager.deleteFileSafely(tempOutputFile)
        }
    }

    override suspend fun addWatermark(
        sourceUri: Uri,
        options: WatermarkOptions,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> = withContext(dispatchers.io) {
        var tempInputFile: File? = null
        var tempOutputFile: File? = null

        try {
            onProgress(EngineProgress(10, "تحضير المستند الأصلي"))

            tempInputFile = tempFileManager.createTempPdfFile("watermark_source")
            val copied = tempFileManager.copyUriToTempFile(sourceUri, tempInputFile)
            if (!copied || !tempInputFile.exists() || tempInputFile.length() == 0L) {
                return@withContext AppResult.Error(
                    AppError.FileNotFound("تعذر فتح ملف الـ PDF الأصلي")
                )
            }

            onProgress(EngineProgress(30, "تجهيز خط ومعلمات العلامة المائية"))

            val doc = PDDocument.load(tempInputFile)
            tempOutputFile = tempFileManager.createTempPdfFile("watermark_output")

            val font = PDType1Font.HELVETICA_BOLD
            val fontSize = options.fontSize
            val text = options.text
            val totalPages = doc.numberOfPages

            for ((index, page) in doc.pages.withIndex()) {
                val mediaBox = page.mediaBox ?: page.cropBox ?: PDRectangle.A4
                val centerX = mediaBox.width / 2f
                val centerY = mediaBox.height / 2f

                // Estimate text center
                val textWidth = (font.getStringWidth(text) / 1000f) * fontSize

                val contentStream = PDPageContentStream(
                    doc,
                    page,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
                )

                val graphicsState = PDExtendedGraphicsState().apply {
                    nonStrokingAlphaConstant = options.opacity.coerceIn(0.05f, 0.95f)
                }
                contentStream.setGraphicsStateParameters(graphicsState)
                contentStream.setNonStrokingColor(128, 128, 128)

                contentStream.beginText()
                contentStream.setFont(font, fontSize)

                val rotationRad = Math.toRadians(options.rotationDegrees.toDouble()).toFloat()
                // Transform to rotate at center
                val matrix = Matrix.getRotateInstance(rotationRad, centerX, centerY)
                contentStream.setTextMatrix(matrix)

                contentStream.newLineAtOffset(-textWidth / 2f, 0f)
                contentStream.showText(text)
                contentStream.endText()
                contentStream.close()

                val pct = 30 + ((index + 1) * 55 / totalPages.coerceAtLeast(1))
                onProgress(EngineProgress(pct, "تطبيق العلامة المائية على الصفحة ${index + 1} من $totalPages"))
            }

            onProgress(EngineProgress(90, "حفظ المستند بعد إضافة العلامة المائية"))

            FileOutputStream(tempOutputFile).use { out ->
                doc.save(out)
            }
            doc.close()

            if (!tempOutputFile.exists() || tempOutputFile.length() == 0L) {
                return@withContext AppResult.Error(
                    AppError.CorruptedPdf("فشل حفظ المستند بالعلامة المائية")
                )
            }

            val saved = tempFileManager.copyFileToUri(tempOutputFile, destinationUri)
            if (!saved) {
                return@withContext AppResult.Error(
                    AppError.InsufficientStorage("تعذر حفظ المستند في الوجهة المحددة")
                )
            }

            val finalSize = safHelper.getFileSize(destinationUri).let {
                if (it > 0) it else tempOutputFile.length()
            }

            onProgress(EngineProgress(100, "تمت إضافة العلامة المائية بنجاح"))
            AppResult.Success(finalSize)

        } catch (oom: OutOfMemoryError) {
            AppResult.Error(AppError.OutOfMemory("نفاد ذاكرة الجهاز أثناء إضافة العلامة المائية"))
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.localizedMessage ?: "حدث خطأ أثناء تطبيق العلامة المائية", e))
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
