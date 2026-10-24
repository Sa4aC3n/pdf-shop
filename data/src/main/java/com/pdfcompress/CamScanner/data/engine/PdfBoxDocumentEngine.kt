package com.pdfcompress.CamScanner.data.engine

import com.googlecode.tesseract.android.TessBaseAPI

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
import com.pdfcompress.CamScanner.domain.model.IdCardLayoutMode
import com.pdfcompress.CamScanner.domain.model.IdCardResult
import com.pdfcompress.CamScanner.domain.model.PassportMrzData
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

                                    val newImageXObject = if (scaledBmp.hasAlpha()) {
                                        LosslessFactory.createFromImage(doc, scaledBmp)
                                    } else {
                                        JPEGFactory.createFromImage(doc, scaledBmp, level.quality)
                                    }
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
                val centerX = mediaBox.lowerLeftX + mediaBox.width / 2f
                val centerY = mediaBox.lowerLeftY + mediaBox.height / 2f

                PDPageContentStream(
                    doc,
                    page,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
                ).use { contentStream ->
                    val pageRotation = page.rotation
                    if (pageRotation != 0) {
                        contentStream.transform(
                            Matrix.getRotateInstance(Math.toRadians(-pageRotation.toDouble()), centerX, centerY)
                        )
                    }
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
            val trimmedText = text.trim()
            if (trimmedText.isNotBlank()) {
                onProgress(EngineProgress(90, "معالجة وإحصاء الكلمات"))
                val words = trimmedText.split("\\s+".toRegex()).filter { it.isNotBlank() }
                onProgress(EngineProgress(100, "تم استخراج النصوص بنجاح"))
                return@withContext AppResult.Success(
                    TextExtractionResult(
                        text = trimmedText,
                        characterCount = trimmedText.length,
                        wordCount = words.size,
                        pageCount = totalPages
                    )
                )
            }

            // Scanned PDF fallback: render pages and run offline Tesseract OCR
            onProgress(EngineProgress(45, "المستند ممسوح ضوئياً؛ بدء التعرف الضوئي (OCR) على الصفحات…"))
            val datapath = ensureTessDataReady()
            val ocrBuilder = StringBuilder()

            context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val pagesCount = renderer.pageCount
                    for (i in 0 until pagesCount) {
                        ensureActive()
                        val page = renderer.openPage(i)
                        val scale = 2.0f
                        val w = (page.width * scale).toInt().coerceAtLeast(1)
                        val h = (page.height * scale).toInt().coerceAtLeast(1)
                        val pageBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(pageBmp)
                        canvas.drawColor(Color.WHITE)
                        page.render(pageBmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()

                        val optBmp = optimizeBitmapForOcr(pageBmp)
                        val pageText = runTesseractOnBitmap(optBmp, datapath)
                        if (optBmp != pageBmp) {
                            optBmp.recycle()
                        }
                        pageBmp.recycle()

                        if (pageText.isNotBlank()) {
                            if (ocrBuilder.isNotEmpty()) ocrBuilder.append("\n\n--- صفحة ${i + 1} ---\n\n")
                            ocrBuilder.append(pageText)
                        }

                        val progressPct = 45 + ((i + 1) * 45 / pagesCount.coerceAtLeast(1))
                        onProgress(EngineProgress(progressPct, "تم استخراج نصوص الصفحة ${i + 1} من $pagesCount"))
                    }
                }
            }

            val finalOcrText = ocrBuilder.toString().trim()
            val ocrWords = finalOcrText.split("\\s+".toRegex()).filter { it.isNotBlank() }
            onProgress(EngineProgress(100, "اكتمل استخراج نصوص المستند بالـ OCR بنجاح"))
            AppResult.Success(
                TextExtractionResult(
                    text = finalOcrText,
                    characterCount = finalOcrText.length,
                    wordCount = ocrWords.size,
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

    private fun ensureTessDataReady(): String {
        val baseDir = context.filesDir
        val tessDataDir = File(baseDir, "tessdata")
        if (!tessDataDir.exists()) {
            tessDataDir.mkdirs()
        }
        val models = listOf("ara.traineddata", "eng.traineddata")
        for (model in models) {
            val target = File(tessDataDir, model)
            if (!target.exists() || target.length() == 0L) {
                try {
                    context.assets.open("tessdata/$model").use { input ->
                        FileOutputStream(target).use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        return baseDir.absolutePath
    }

    private fun optimizeBitmapForOcr(bitmap: Bitmap): Bitmap {
        val maxDim = maxOf(bitmap.width, bitmap.height)
        return if (maxDim > 2048) {
            val scale = 2048f / maxDim.toFloat()
            val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
            val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(bitmap, w, h, true)
        } else {
            bitmap
        }
    }

    private fun runTesseractOnBitmap(bitmap: Bitmap, datapath: String): String {
        val tess = TessBaseAPI()
        return try {
            val inited = try {
                tess.init(datapath, "ara+eng")
            } catch (_: Exception) {
                false
            }
            val ready = if (inited) true else try { tess.init(datapath, "eng") } catch (_: Exception) { false }
            if (!ready) return ""
            tess.setImage(bitmap)
            val text = tess.utF8Text ?: ""
            tess.clear()
            text.trim()
        } catch (_: Exception) {
            ""
        } finally {
            try { tess.recycle() } catch (_: Exception) {}
        }
    }

    override suspend fun recognizeImageText(
        imageUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<TextExtractionResult> = withContext(dispatchers.io) {
        onProgress(EngineProgress(10, "تحضير نماذج التعرف الضوئي الأوفلاين (Tesseract)…"))
        val datapath = ensureTessDataReady()

        ensureActive()
        onProgress(EngineProgress(30, "قراءة ومعالجة أبعاد الصورة…"))
        val originalBmp = try {
            context.contentResolver.openInputStream(imageUri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            null
        }

        if (originalBmp == null) {
            return@withContext AppResult.Error(AppError.CorruptedPdf("تعذر قراءة أو فك تشفير ملف الصورة المختار"))
        }

        ensureActive()
        onProgress(EngineProgress(50, "معالجة واستخراج النصوص العربية والإنجليزية (OCR)…"))
        val optimized = optimizeBitmapForOcr(originalBmp)
        val recognizedText = runTesseractOnBitmap(optimized, datapath)

        if (optimized != originalBmp) {
            optimized.recycle()
        }
        originalBmp.recycle()

        ensureActive()
        onProgress(EngineProgress(90, "إحصاء الكلمات والرموز المستخرجة…"))
        val words = recognizedText.split("\\s+".toRegex()).filter { it.isNotBlank() }

        onProgress(EngineProgress(100, "اكتمل التعرف الضوئي على الصورة بنجاح"))
        AppResult.Success(
            TextExtractionResult(
                text = recognizedText,
                characterCount = recognizedText.length,
                wordCount = words.size,
                pageCount = 1
            )
        )
    }


    override suspend fun createIdCardPdf(
        frontCardUri: Uri?,
        backCardUri: Uri?,
        layoutMode: IdCardLayoutMode,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<IdCardResult> = withContext(dispatchers.io) {
        try {
            onProgress(EngineProgress(10, "تحضير وقراءة صور البطاقة الشخصية…"))

            val frontBmp = frontCardUri?.let { uri ->
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }
            val backBmp = backCardUri?.let { uri ->
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }

            if (frontBmp == null && backBmp == null) {
                return@withContext AppResult.Error(AppError.CorruptedPdf("تعذر قراءة صور البطاقة"))
            }

            onProgress(EngineProgress(35, "تجهيز وتنسيق صفحة الطباعة الرسمية…"))
            val tempPdf = tempFileManager.createTempPdfFile("id_card_out")
            val pdfDoc = android.graphics.pdf.PdfDocument()
            var pageCount = 0

            when (layoutMode) {
                IdCardLayoutMode.A4_VERTICAL -> {
                    // Standard A4 portrait: 595 x 842 points
                    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
                    val page = pdfDoc.startPage(pageInfo)
                    val canvas = page.canvas

                    val bgPaint = Paint().apply { color = Color.WHITE }
                    canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

                    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.rgb(100, 116, 139)
                        textSize = 11f
                        textAlign = Paint.Align.CENTER
                    }
                    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.rgb(203, 213, 225)
                        style = Paint.Style.STROKE
                        strokeWidth = 1.5f
                    }

                    val cardW = 390f
                    val cardH = 246f
                    val cardX = (595f - cardW) / 2f

                    if (frontBmp != null) {
                        val frontY = 100f
                        canvas.drawText("الوجه الأمامي للبطاقة (Front Side)", 595f / 2f, frontY - 12f, labelPaint)
                        val dstRect = android.graphics.RectF(cardX, frontY, cardX + cardW, frontY + cardH)
                        canvas.drawBitmap(frontBmp, null, dstRect, paint)
                        canvas.drawRoundRect(dstRect, 8f, 8f, borderPaint)
                    }

                    if (backBmp != null) {
                        val backY = 460f
                        canvas.drawText("الوجه الخلفي للبطاقة (Back Side)", 595f / 2f, backY - 12f, labelPaint)
                        val dstRect = android.graphics.RectF(cardX, backY, cardX + cardW, backY + cardH)
                        canvas.drawBitmap(backBmp, null, dstRect, paint)
                        canvas.drawRoundRect(dstRect, 8f, 8f, borderPaint)
                    }

                    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.rgb(148, 163, 184)
                        textSize = 9f
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText("تم المسح والدمج محلياً باستخدام PDF Shop Scanner", 595f / 2f, 815f, footerPaint)

                    pdfDoc.finishPage(page)
                    pageCount = 1
                }
                IdCardLayoutMode.SIDE_BY_SIDE -> {
                    // A4 Landscape: 842 x 595 points
                    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(842, 595, 1).create()
                    val page = pdfDoc.startPage(pageInfo)
                    val canvas = page.canvas
                    canvas.drawColor(Color.WHITE)

                    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.rgb(203, 213, 225)
                        style = Paint.Style.STROKE
                        strokeWidth = 1.5f
                    }
                    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.rgb(100, 116, 139)
                        textSize = 11f
                        textAlign = Paint.Align.CENTER
                    }

                    val cardW = 360f
                    val cardH = 227f
                    val cardY = (595f - cardH) / 2f

                    if (frontBmp != null) {
                        val frontX = 40f
                        canvas.drawText("الوجه الأمامي (Front)", frontX + cardW / 2f, cardY - 12f, labelPaint)
                        val dstRect = android.graphics.RectF(frontX, cardY, frontX + cardW, cardY + cardH)
                        canvas.drawBitmap(frontBmp, null, dstRect, paint)
                        canvas.drawRoundRect(dstRect, 8f, 8f, borderPaint)
                    }

                    if (backBmp != null) {
                        val backX = 842f - cardW - 40f
                        canvas.drawText("الوجه الخلفي (Back)", backX + cardW / 2f, cardY - 12f, labelPaint)
                        val dstRect = android.graphics.RectF(backX, cardY, backX + cardW, cardY + cardH)
                        canvas.drawBitmap(backBmp, null, dstRect, paint)
                        canvas.drawRoundRect(dstRect, 8f, 8f, borderPaint)
                    }

                    pdfDoc.finishPage(page)
                    pageCount = 1
                }
                IdCardLayoutMode.TWO_PAGES -> {
                    var pageNum = 1
                    val bmps = listOfNotNull(frontBmp, backBmp)
                    for (bmp in bmps) {
                        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                        val page = pdfDoc.startPage(pageInfo)
                        val canvas = page.canvas
                        canvas.drawColor(Color.WHITE)

                        val cardW = 460f
                        val cardH = 290f
                        val cardX = (595f - cardW) / 2f
                        val cardY = (842f - cardH) / 2f
                        val dstRect = android.graphics.RectF(cardX, cardY, cardX + cardW, cardY + cardH)
                        canvas.drawBitmap(bmp, null, dstRect, Paint(Paint.ANTI_ALIAS_FLAG))

                        pdfDoc.finishPage(page)
                        pageNum++
                    }
                    pageCount = bmps.size
                }
                IdCardLayoutMode.FRONT_ONLY -> {
                    val bmp = frontBmp ?: backBmp!!
                    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
                    val page = pdfDoc.startPage(pageInfo)
                    val canvas = page.canvas
                    canvas.drawColor(Color.WHITE)

                    val cardW = 460f
                    val cardH = 290f
                    val cardX = (595f - cardW) / 2f
                    val cardY = (842f - cardH) / 2f
                    val dstRect = android.graphics.RectF(cardX, cardY, cardX + cardW, cardY + cardH)
                    canvas.drawBitmap(bmp, null, dstRect, Paint(Paint.ANTI_ALIAS_FLAG))

                    pdfDoc.finishPage(page)
                    pageCount = 1
                }
                IdCardLayoutMode.BACK_ONLY -> {
                    val bmp = backBmp ?: frontBmp!!
                    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
                    val page = pdfDoc.startPage(pageInfo)
                    val canvas = page.canvas
                    canvas.drawColor(Color.WHITE)

                    val cardW = 460f
                    val cardH = 290f
                    val cardX = (595f - cardW) / 2f
                    val cardY = (842f - cardH) / 2f
                    val dstRect = android.graphics.RectF(cardX, cardY, cardX + cardW, cardY + cardH)
                    canvas.drawBitmap(bmp, null, dstRect, Paint(Paint.ANTI_ALIAS_FLAG))

                    pdfDoc.finishPage(page)
                    pageCount = 1
                }
            }

            onProgress(EngineProgress(85, "حفظ وتصدير ملف PDF النهائي…"))
            FileOutputStream(tempPdf).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()

            val success = tempFileManager.copyFileToUri(tempPdf, destinationUri)
            val outputSize = tempPdf.length()
            tempFileManager.deleteFileSafely(tempPdf)

            frontBmp?.recycle()
            backBmp?.recycle()

            if (!success) {
                return@withContext AppResult.Error(AppError.InsufficientStorage("تعذر نسخ المستند النهائي إلى مجلد الوجهة"))
            }

            onProgress(EngineProgress(100, "اكتمل تصدير البطاقة الشخصية كـ PDF"))
            AppResult.Success(IdCardResult(outputSize, pageCount, layoutMode))
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.message ?: "فشل في إنشاء مستند البطاقة الشخصية"))
        }
    }

    override suspend fun createIdCardImage(
        frontCardUri: Uri?,
        backCardUri: Uri?,
        layoutMode: IdCardLayoutMode,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> = withContext(dispatchers.io) {
        try {
            onProgress(EngineProgress(15, "قراءة صور البطاقة لدمجها كصورة…"))
            val frontBmp = frontCardUri?.let { uri ->
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }
            val backBmp = backCardUri?.let { uri ->
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }

            if (frontBmp == null && backBmp == null) {
                return@withContext AppResult.Error(AppError.CorruptedPdf("تعذر قراءة صور البطاقة"))
            }

            onProgress(EngineProgress(45, "دمج ومعالجة الصورة المجمعة…"))
            val width = maxOf(frontBmp?.width ?: 0, backBmp?.width ?: 0)
            val frontH = frontBmp?.height ?: 0
            val backH = backBmp?.height ?: 0
            val spacing = 40
            val height = if (frontBmp != null && backBmp != null) frontH + backH + spacing else frontH + backH

            val combined = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(combined)
            canvas.drawColor(Color.WHITE)

            var curY = 0f
            if (frontBmp != null) {
                val x = (width - frontBmp.width) / 2f
                canvas.drawBitmap(frontBmp, x, curY, null)
                curY += frontBmp.height + spacing
            }
            if (backBmp != null) {
                val x = (width - backBmp.width) / 2f
                canvas.drawBitmap(backBmp, x, curY, null)
            }

            onProgress(EngineProgress(80, "ضغط وحفظ ملف الصورة…"))
            val tempImg = tempFileManager.createTempPdfFile("id_card_img.jpg")
            FileOutputStream(tempImg).use { out ->
                combined.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            combined.recycle()
            frontBmp?.recycle()
            backBmp?.recycle()

            val success = tempFileManager.copyFileToUri(tempImg, destinationUri)
            val size = tempImg.length()
            tempFileManager.deleteFileSafely(tempImg)

            if (!success) {
                return@withContext AppResult.Error(AppError.InsufficientStorage("تعذر حفظ الصورة في المسار المحدد"))
            }

            onProgress(EngineProgress(100, "اكتمل حفظ صورة البطاقة المدمجة بنجاح"))
            AppResult.Success(size)
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.message ?: "فشل في حفظ صورة البطاقة"))
        }
    }

    override suspend fun generatePassportReportPdf(
        photoUri: Uri?,
        mrzData: PassportMrzData,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> = withContext(dispatchers.io) {
        try {
            onProgress(EngineProgress(15, "إعداد تقرير فحص وتدقيق جواز السفر…"))
            val photoBmp = photoUri?.let { uri ->
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }

            onProgress(EngineProgress(40, "تنسيق وثيقة التحقق الرسمية (PDF)…"))
            val tempPdf = tempFileManager.createTempPdfFile("passport_report")
            val pdfDoc = android.graphics.pdf.PdfDocument()
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            // Header Banner
            val headerPaint = Paint().apply { color = Color.rgb(5, 150, 105) } // Emerald Green
            canvas.drawRect(0f, 0f, 595f, 60f, headerPaint)

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("تقرير فحص جواز السفر الإلكتروني (ICAO 9303)", 595f / 2f, 38f, titlePaint)

            var curY = 85f

            // Passport photo
            if (photoBmp != null) {
                val photoW = 280f
                val photoH = (photoW * photoBmp.height / photoBmp.width).coerceAtMost(160f)
                val photoX = (595f - photoW) / 2f
                val dstRect = android.graphics.RectF(photoX, curY, photoX + photoW, curY + photoH)
                canvas.drawBitmap(photoBmp, null, dstRect, Paint(Paint.ANTI_ALIAS_FLAG))
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(203, 213, 225)
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }
                canvas.drawRoundRect(dstRect, 6f, 6f, borderPaint)
                curY += photoH + 20f
                photoBmp.recycle()
            }

            // Table of fields
            val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 41, 59)
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("البيانات الشخصية والوثائقية المستخرجة:", 40f, curY, sectionPaint)
            curY += 15f

            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(100, 116, 139)
                textSize = 11f
            }
            val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
            }
            val linePaint = Paint().apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 1f
            }

            val fields = listOf(
                Pair("الاسم الكامل / Full Name", mrzData.fullName),
                Pair("رقم الجواز / Passport No.", "${mrzData.documentNumber}  " + if (mrzData.documentNumberCheckOk) "[صحيح ✓]" else "[فحص التدقيق غير متطابق ✗]"),
                Pair("الدولة المصدرة / Issuing State", mrzData.countryName),
                Pair("الجنسية / Nationality", mrzData.nationalityName),
                Pair("تاريخ الميلاد / Date of Birth", mrzData.dateOfBirth),
                Pair("النوع / Gender", mrzData.sexLabel),
                Pair("تاريخ الانتهاء / Expiry Date", "${mrzData.expiryDate}  " + if (mrzData.isExpired) "[منتهي الصلاحية ⚠️]" else "[ساري المفعول ✓]"),
                Pair("نوع الوثيقة / Document Type", mrzData.documentType),
                Pair("حالة فحص ICAO 9303 الكلي", if (mrzData.isCompleteValid) "مطابق وموثوق بالكامل (100% Valid) ✓" else "تمت القراءة مع تنبيهات في الأرقام الرقابية")
            )

            for (field in fields) {
                curY += 18f
                canvas.drawText(field.first, 45f, curY, labelPaint)
                canvas.drawText(field.second, 220f, curY, valuePaint)
                curY += 8f
                canvas.drawLine(40f, curY, 555f, curY, linePaint)
            }

            // MRZ Raw Box
            curY += 25f
            canvas.drawText("الشيفرة المقروءة آلياً (Machine Readable Zone - MRZ):", 40f, curY, sectionPaint)
            curY += 12f

            val mrzBoxPaint = Paint().apply { color = Color.rgb(241, 245, 249) }
            val mrzBorder = Paint().apply {
                color = Color.rgb(203, 213, 225)
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            val mrzRect = android.graphics.RectF(40f, curY, 555f, curY + 60f)
            canvas.drawRoundRect(mrzRect, 8f, 8f, mrzBoxPaint)
            canvas.drawRoundRect(mrzRect, 8f, 8f, mrzBorder)

            val mrzFontPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 41, 59)
                textSize = 11f
                typeface = Typeface.MONOSPACE
            }
            val mrzLines = mrzData.rawMrz.lines()
            var mrzTextY = curY + 24f
            for (line in mrzLines) {
                canvas.drawText(line, 55f, mrzTextY, mrzFontPaint)
                mrzTextY += 18f
            }

            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(148, 163, 184)
                textSize = 9f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("تقرير إلكتروني غير متصل بالإنترنت صادر من تطبيق PDF Shop Scanner", 595f / 2f, 815f, footerPaint)

            pdfDoc.finishPage(page)

            onProgress(EngineProgress(85, "كتابة ملف التقرير إلى الذاكرة…"))
            FileOutputStream(tempPdf).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()

            val success = tempFileManager.copyFileToUri(tempPdf, destinationUri)
            val size = tempPdf.length()
            tempFileManager.deleteFileSafely(tempPdf)

            if (!success) {
                return@withContext AppResult.Error(AppError.InsufficientStorage("تعذر حفظ التقرير في المجلد المحدد"))
            }

            onProgress(EngineProgress(100, "اكتمل إنشاء تقرير جواز السفر بنجاح"))
            AppResult.Success(size)
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.message ?: "فشل في إنشاء تقرير جواز السفر"))
        }
    }

    override suspend fun getPageCount(sourceUri: Uri): Int = withContext(dispatchers.io) {
        val quickCount = try { context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { pfd -> PdfRenderer(pfd).use { it.pageCount } } } catch (_: Exception) { null }
        if (quickCount != null && quickCount > 0) return@withContext quickCount
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
