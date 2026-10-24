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
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.util.Matrix
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.pdfcompress.CamScanner.domain.model.ExportImagesResult
import com.pdfcompress.CamScanner.domain.model.RepairReport
import com.pdfcompress.CamScanner.domain.model.TextExtractionResult
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

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
                ensureActive()
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

            ensureActive()
            onProgress(EngineProgress(45, "جارٍ دمج صفحات PDF محلياً"))

            tempOutputFile = tempFileManager.createTempPdfFile("merge_output")
            val merger = PDFMergerUtility().apply {
                destinationFileName = tempOutputFile.absolutePath
            }

            for (file in tempInputFiles) {
                merger.addSource(file)
            }

            merger.mergeDocuments(MemoryUsageSetting.setupTempFileOnly())

            ensureActive()
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

        } catch (ce: CancellationException) {
            throw ce
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
                ensureActive()
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

            ensureActive()
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

            try {
                PDDocument.load(tempOutputFile).use { checkDoc ->
                    if (checkDoc.numberOfPages == 0) {
                        return@withContext AppResult.Error(
                            AppError.CorruptedPdf("ملف الـ PDF الناتج من الصور فارغ ولا يحتوي على صفحات")
                        )
                    }
                }
            } catch (e: Exception) {
                return@withContext AppResult.Error(
                    AppError.CorruptedPdf("تعذر التحقق من سلامة ملف PDF الناتج", e)
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

        } catch (ce: CancellationException) {
            throw ce
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

            ensureActive()
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
                ensureActive()
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

        } catch (ce: CancellationException) {
            throw ce
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
                ensureActive()
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

            ensureActive()
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

            val rawCompressedSize = tempOutputFile.length()
            val isSmaller = rawCompressedSize < originalSize
            val fileToSave = if (isSmaller) tempOutputFile else tempInputFile
            val finalCompressedSize = if (isSmaller) rawCompressedSize else originalSize
            val reductionPct = if (isSmaller && originalSize > 0) {
                (((originalSize - finalCompressedSize) * 100) / originalSize).toInt()
            } else 0

            onProgress(EngineProgress(95, "حفظ المستند في الوجهة المحددة"))

            val saved = tempFileManager.copyFileToUri(fileToSave, destinationUri)
            if (!saved) {
                return@withContext AppResult.Error(
                    AppError.InsufficientStorage("تعذر حفظ الملف المضغوط في الوجهة المحددة")
                )
            }

            onProgress(EngineProgress(100, "اكتملت عملية الضغط بنجاح"))

            AppResult.Success(
                CompressionResult(
                    originalSizeBytes = originalSize,
                    compressedSizeBytes = finalCompressedSize,
                    reductionPercentage = reductionPct,
                    isSmaller = isSmaller
                )
            )

        } catch (ce: CancellationException) {
            throw ce
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
        var watermarkBitmap: Bitmap? = null

        try {
            onProgress(EngineProgress(10, "تحضير المستند الأصلي"))

            tempInputFile = tempFileManager.createTempPdfFile("watermark_source")
            val copied = tempFileManager.copyUriToTempFile(sourceUri, tempInputFile)
            if (!copied || !tempInputFile.exists() || tempInputFile.length() == 0L) {
                return@withContext AppResult.Error(
                    AppError.FileNotFound("تعذر فتح ملف الـ PDF الأصلي")
                )
            }

            ensureActive()
            onProgress(EngineProgress(30, "تجهيز خط ومعلمات العلامة المائية"))

            val doc = PDDocument.load(tempInputFile)
            tempOutputFile = tempFileManager.createTempPdfFile("watermark_output")

            val text = options.text.trim().ifEmpty { "WATERMARK" }
            val scaleFactor = 2.5f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(115, 115, 115)
                textSize = options.fontSize * scaleFactor
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            val fontMetrics = paint.fontMetrics
            val textWidth = paint.measureText(text).coerceAtLeast(20f)
            val textHeight = (fontMetrics.descent - fontMetrics.ascent).coerceAtLeast(20f)

            val rad = Math.toRadians(options.rotationDegrees.toDouble())
            val cosA = abs(cos(rad)).toFloat()
            val sinA = abs(sin(rad)).toFloat()
            val bmpW = (textWidth * cosA + textHeight * sinA + 32f).toInt().coerceAtLeast(64)
            val bmpH = (textWidth * sinA + textHeight * cosA + 32f).toInt().coerceAtLeast(64)

            watermarkBitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(watermarkBitmap)
            canvas.translate(bmpW / 2f, bmpH / 2f)
            canvas.rotate(-options.rotationDegrees)
            val textOffset = -(fontMetrics.descent + fontMetrics.ascent) / 2f
            canvas.drawText(text, 0f, textOffset, paint)

            val watermarkImage = LosslessFactory.createFromImage(doc, watermarkBitmap)
            val drawW = bmpW / scaleFactor
            val drawH = bmpH / scaleFactor

            val totalPages = doc.numberOfPages

            for ((index, page) in doc.pages.withIndex()) {
                ensureActive()
                val mediaBox = page.mediaBox ?: page.cropBox ?: PDRectangle.A4
                val centerX = mediaBox.width / 2f
                val centerY = mediaBox.height / 2f

                PDPageContentStream(
                    doc,
                    page,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
                ).use { contentStream ->
                    val graphicsState = PDExtendedGraphicsState().apply {
                        nonStrokingAlphaConstant = options.opacity.coerceIn(0.05f, 0.95f)
                        strokingAlphaConstant = options.opacity.coerceIn(0.05f, 0.95f)
                    }
                    contentStream.setGraphicsStateParameters(graphicsState)
                    contentStream.drawImage(
                        watermarkImage,
                        centerX - drawW / 2f,
                        centerY - drawH / 2f,
                        drawW,
                        drawH
                    )
                }

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

        } catch (ce: CancellationException) {
            throw ce
        } catch (oom: OutOfMemoryError) {
            AppResult.Error(AppError.OutOfMemory("نفاد ذاكرة الجهاز أثناء إضافة العلامة المائية"))
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.localizedMessage ?: "حدث خطأ أثناء تطبيق العلامة المائية", e))
        } finally {
            try { watermarkBitmap?.recycle() } catch (_: Exception) {}
            tempFileManager.deleteFileSafely(tempInputFile)
            tempFileManager.deleteFileSafely(tempOutputFile)
        }
    }

    override suspend fun pdfToImages(
        sourceUri: Uri,
        selectedPages: List<Int>,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<ExportImagesResult> = withContext(dispatchers.io) {
        var tempInputFile: File? = null
        var tempOutputFile: File? = null
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null

        try {
            onProgress(EngineProgress(10, "تحضير المستند لقراءة الصفحات"))

            tempInputFile = tempFileManager.createTempPdfFile("pdf_to_img_source")
            val copied = tempFileManager.copyUriToTempFile(sourceUri, tempInputFile)
            if (!copied || !tempInputFile.exists()) {
                return@withContext AppResult.Error(AppError.FileNotFound("تعذر فتح ملف الـ PDF الأصلي"))
            }

            pfd = ParcelFileDescriptor.open(tempInputFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)

            val totalAvailable = renderer.pageCount
            val pagesToExport = selectedPages.filter { it in 1..totalAvailable }
            if (pagesToExport.isEmpty()) {
                return@withContext AppResult.Error(AppError.Unknown("الصفحات المحددة غير موجودة في المستند"))
            }

            val isSinglePage = pagesToExport.size == 1

            if (isSinglePage) {
                tempOutputFile = tempFileManager.createTempImageFile("jpg")
                val pageIndex = pagesToExport[0] - 1
                val page = renderer.openPage(pageIndex)

                val bmpWidth = (page.width * 2).coerceAtLeast(1)
                val bmpHeight = (page.height * 2).coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)

                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                FileOutputStream(tempOutputFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                bitmap.recycle()

                onProgress(EngineProgress(90, "حفظ الصورة الناتجة"))
                val saved = tempFileManager.copyFileToUri(tempOutputFile, destinationUri)
                if (!saved) {
                    return@withContext AppResult.Error(AppError.InsufficientStorage("تعذر حفظ الصورة في الوجهة المختارة"))
                }

                onProgress(EngineProgress(100, "اكتمل تصدير الصورة"))
                AppResult.Success(
                    ExportImagesResult(
                        totalExportedPages = 1,
                        outputSizeBytes = tempOutputFile.length(),
                        isZipArchive = false
                    )
                )
            } else {
                // Export multiple pages into a ZIP archive
                tempOutputFile = File(tempFileManager.createTempPdfFile("export_images").parentFile, "pages_${System.currentTimeMillis()}.zip")
                val zipOut = ZipOutputStream(FileOutputStream(tempOutputFile))

                for ((index, pageNum) in pagesToExport.withIndex()) {
                    ensureActive()
                    val page = renderer.openPage(pageNum - 1)
                    val bmpW = (page.width * 2).coerceAtLeast(1)
                    val bmpH = (page.height * 2).coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)

                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    val entryName = String.format("page_%03d.jpg", pageNum)
                    zipOut.putNextEntry(ZipEntry(entryName))
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 88, zipOut)
                    zipOut.closeEntry()
                    bitmap.recycle()

                    val pct = 10 + ((index + 1) * 75 / pagesToExport.size)
                    onProgress(EngineProgress(pct, "تصدير الصفحة $pageNum من ${pagesToExport.size}"))
                }
                zipOut.close()

                onProgress(EngineProgress(90, "حفظ الأرشيف المضغوط للصور"))
                val saved = tempFileManager.copyFileToUri(tempOutputFile, destinationUri)
                if (!saved) {
                    return@withContext AppResult.Error(AppError.InsufficientStorage("تعذر حفظ أرشيف الصور في الوجهة المحددة"))
                }

                onProgress(EngineProgress(100, "اكتمل تصدير أرشيف الصور"))
                AppResult.Success(
                    ExportImagesResult(
                        totalExportedPages = pagesToExport.size,
                        outputSizeBytes = tempOutputFile.length(),
                        isZipArchive = true
                    )
                )
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (oom: OutOfMemoryError) {
            AppResult.Error(AppError.OutOfMemory("نفاد ذاكرة الجهاز أثناء تصدير الصور"))
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.localizedMessage ?: "حدث خطأ أثناء تصدير الصور", e))
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
            tempFileManager.deleteFileSafely(tempInputFile)
            tempFileManager.deleteFileSafely(tempOutputFile)
        }
    }

    override suspend fun extractText(
        sourceUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<TextExtractionResult> = withContext(dispatchers.io) {
        var tempInputFile: File? = null
        try {
            onProgress(EngineProgress(15, "تحضير المستند لاستخراج النصوص"))

            tempInputFile = tempFileManager.createTempPdfFile("extract_text_source")
            val copied = tempFileManager.copyUriToTempFile(sourceUri, tempInputFile)
            if (!copied || !tempInputFile.exists()) {
                return@withContext AppResult.Error(AppError.FileNotFound("تعذر فتح ملف الـ PDF الأصلي"))
            }

            ensureActive()
            onProgress(EngineProgress(40, "تحليل وفهرسة خطوط وتدفقات النصوص"))

            val doc = PDDocument.load(tempInputFile)
            val stripper = PDFTextStripper()
            val text = stripper.getText(doc) ?: ""
            val totalPages = doc.numberOfPages
            doc.close()

            ensureActive()
            onProgress(EngineProgress(90, "معالجة وإحصاء الكلمات"))

            val words = text.split("\\s+".toRegex()).filter { it.isNotBlank() }

            onProgress(EngineProgress(100, "تم استخراج النصوص بنجاح"))
            AppResult.Success(
                TextExtractionResult(
                    text = text,
                    characterCount = text.length,
                    wordCount = words.size,
                    pageCount = totalPages
                )
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.localizedMessage ?: "تعذر استخراج النصوص من هذا المستند", e))
        } finally {
            tempFileManager.deleteFileSafely(tempInputFile)
        }
    }

    override suspend fun repair(
        sourceUri: Uri,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<RepairReport> = withContext(dispatchers.io) {
        var tempInputFile: File? = null
        var tempOutputFile: File? = null

        try {
            onProgress(EngineProgress(15, "فحص بنية بايتات ملف الـ PDF والتلف"))

            tempInputFile = tempFileManager.createTempPdfFile("repair_source")
            val copied = tempFileManager.copyUriToTempFile(sourceUri, tempInputFile)
            if (!copied || !tempInputFile.exists() || tempInputFile.length() == 0L) {
                return@withContext AppResult.Error(AppError.FileNotFound("الملف الأصلي غير موجود أو فارغ تماماً"))
            }

            ensureActive()
            onProgress(EngineProgress(40, "محاولة إعادة بناء جداول المراجع والمقطورات (XRef Rebuild)"))

            // Attempt loading with ScratchFile to reconstruct corrupted xref streams
            val doc = try {
                PDDocument.load(tempInputFile, MemoryUsageSetting.setupTempFileOnly())
            } catch (e: Exception) {
                return@withContext AppResult.Error(
                    AppError.CorruptedPdf("تلف جذري في الملف يتعذر استرداده محلياً: ${e.localizedMessage}")
                )
            }

            tempOutputFile = tempFileManager.createTempPdfFile("repair_output")
            val recoveredPages = doc.numberOfPages

            if (recoveredPages == 0) {
                doc.close()
                return@withContext AppResult.Error(
                    AppError.CorruptedPdf("تم فحص الملف ولكن لم يتم العثور على أي صفحات صالحة للاسترداد")
                )
            }

            ensureActive()
            onProgress(EngineProgress(80, "إعادة كتابة وتوليد مستند PDF سليم"))

            FileOutputStream(tempOutputFile).use { out ->
                doc.save(out)
            }
            doc.close()

            val saved = tempFileManager.copyFileToUri(tempOutputFile, destinationUri)
            if (!saved) {
                return@withContext AppResult.Error(AppError.InsufficientStorage("تعذر حفظ المستند المسترد"))
            }

            val finalSize = safHelper.getFileSize(destinationUri).let {
                if (it > 0) it else tempOutputFile.length()
            }

            onProgress(EngineProgress(100, "اكتمل الإصلاح بنجاح"))

            AppResult.Success(
                RepairReport(
                    isSuccess = true,
                    recoveredPagesCount = recoveredPages,
                    outputSizeBytes = finalSize,
                    diagnosisMessage = "تمت إعادة بناء جداول المراجع واسترداد $recoveredPages صفحة بنجاح"
                )
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.localizedMessage ?: "حدث خطأ أثناء محاولة إصلاح المستند", e))
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
