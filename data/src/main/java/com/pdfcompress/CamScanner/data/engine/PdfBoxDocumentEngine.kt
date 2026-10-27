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
import com.pdfcompress.CamScanner.domain.model.PdfMetadata
import com.pdfcompress.CamScanner.domain.model.PageNumberPosition
import com.pdfcompress.CamScanner.domain.model.PageNumberFormat
import com.pdfcompress.CamScanner.domain.model.CropMargins
import com.pdfcompress.CamScanner.domain.model.RedactionRect
import com.pdfcompress.CamScanner.domain.model.ImageProcessOptions
import com.pdfcompress.CamScanner.domain.model.ImageOutputFormat
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
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

    override suspend fun recognizePassportMrz(
        imageUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<String> = withContext(dispatchers.io) {
        onProgress(EngineProgress(15, "تحضير نموذج قراءة الجوازات والـ MRZ…"))
        val datapath = ensureTessDataReady()

        ensureActive()
        onProgress(EngineProgress(30, "قراءة وتعديل زوايا دوران الصورة (EXIF)…"))
        val originalBmp = decodeBitmapWithExif(imageUri)
            ?: return@withContext AppResult.Error(AppError.CorruptedPdf("تعذر قراءة أو فك تشفير صورة جواز السفر"))

        try {
            ensureActive()
            onProgress(EngineProgress(45, "استخراج منطقة الـ MRZ ومعالجة التباين…"))

            val tess = TessBaseAPI()
            val initOk = try {
                tess.init(datapath, "eng")
            } catch (_: Exception) {
                false
            }
            if (!initOk) {
                return@withContext AppResult.Error(AppError.Unknown("تعذر تهيئة محرك قراءة الشيفرات"))
            }

            try {
                tess.setVariable("tessedit_char_whitelist", "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789<")
                tess.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO

                val passes = listOf(
                    CropStrategy.BOTTOM_30,
                    CropStrategy.BOTTOM_45,
                    CropStrategy.FULL,
                    CropStrategy.ROTATE_90,
                    CropStrategy.ROTATE_270
                )

                var bestText = ""
                for ((idx, strategy) in passes.withIndex()) {
                    ensureActive()
                    val progressPct = 50 + (idx * 8)
                    onProgress(EngineProgress(progressPct, "فحص منطقة الشيفرة المدمجة (محاولة ${idx + 1} من ${passes.size})…"))

                    val candidateBmp = prepareMrzCandidateBitmap(originalBmp, strategy) ?: continue
                    try {
                        tess.setImage(candidateBmp)
                        val text = tess.utF8Text ?: ""
                        tess.clear()

                        if (text.isNotBlank()) {
                            val parsed = com.pdfcompress.CamScanner.domain.parser.MrzParser.parse(text)
                            if (parsed != null) {
                                bestText = text
                                break
                            }
                            if (bestText.isBlank() || text.length > bestText.length) {
                                bestText = text
                            }
                        }
                    } finally {
                        if (candidateBmp != originalBmp) {
                            candidateBmp.recycle()
                        }
                    }
                }

                onProgress(EngineProgress(100, "اكتمل تحليل منطقة الشيفرة بنجاح"))
                AppResult.Success(bestText)
            } finally {
                try { tess.recycle() } catch (_: Exception) {}
            }
        } finally {
            originalBmp.recycle()
        }
    }

    private enum class CropStrategy {
        BOTTOM_30,
        BOTTOM_45,
        FULL,
        ROTATE_90,
        ROTATE_270
    }

    private fun decodeBitmapWithExif(uri: Uri): Bitmap? {
        return try {
            val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
                try {
                    val exif = android.media.ExifInterface(stream)
                    exif.getAttributeInt(
                        android.media.ExifInterface.TAG_ORIENTATION,
                        android.media.ExifInterface.ORIENTATION_NORMAL
                    )
                } catch (_: Exception) {
                    android.media.ExifInterface.ORIENTATION_NORMAL
                }
            } ?: android.media.ExifInterface.ORIENTATION_NORMAL

            val rawBmp = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return null

            val rotationDegrees = when (orientation) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            if (rotationDegrees != 0f) {
                val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees) }
                val rotated = Bitmap.createBitmap(rawBmp, 0, 0, rawBmp.width, rawBmp.height, matrix, true)
                if (rotated != rawBmp) rawBmp.recycle()
                rotated
            } else {
                rawBmp
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun prepareMrzCandidateBitmap(source: Bitmap, strategy: CropStrategy): Bitmap? {
        return try {
            val cropped = when (strategy) {
                CropStrategy.BOTTOM_30 -> {
                    val startY = (source.height * 0.70f).toInt().coerceIn(0, source.height - 1)
                    val h = source.height - startY
                    Bitmap.createBitmap(source, 0, startY, source.width, h)
                }
                CropStrategy.BOTTOM_45 -> {
                    val startY = (source.height * 0.55f).toInt().coerceIn(0, source.height - 1)
                    val h = source.height - startY
                    Bitmap.createBitmap(source, 0, startY, source.width, h)
                }
                CropStrategy.FULL -> {
                    source
                }
                CropStrategy.ROTATE_90 -> {
                    val matrix = android.graphics.Matrix().apply { postRotate(90f) }
                    val rot = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
                    val startY = (rot.height * 0.70f).toInt().coerceIn(0, rot.height - 1)
                    val h = rot.height - startY
                    val sub = Bitmap.createBitmap(rot, 0, startY, rot.width, h)
                    rot.recycle()
                    sub
                }
                CropStrategy.ROTATE_270 -> {
                    val matrix = android.graphics.Matrix().apply { postRotate(270f) }
                    val rot = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
                    val startY = (rot.height * 0.70f).toInt().coerceIn(0, rot.height - 1)
                    val h = rot.height - startY
                    val sub = Bitmap.createBitmap(rot, 0, startY, rot.width, h)
                    rot.recycle()
                    sub
                }
            }

            val targetWidth = 1600
            val scale = targetWidth.toFloat() / cropped.width.toFloat()
            val scaled = if (cropped.width != targetWidth && cropped.width > 300) {
                val targetHeight = (cropped.height * scale).toInt().coerceAtLeast(1)
                val s = Bitmap.createScaledBitmap(cropped, targetWidth, targetHeight, true)
                if (cropped != source) cropped.recycle()
                s
            } else {
                cropped
            }

            val gray = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(gray)
            val paint = Paint()
            val colorMatrix = android.graphics.ColorMatrix().apply {
                setSaturation(0f)
                val contrast = 1.5f
                val translate = (-0.5f * contrast + 0.5f) * 255f
                postConcat(android.graphics.ColorMatrix(floatArrayOf(
                    contrast, 0f, 0f, 0f, translate,
                    0f, contrast, 0f, 0f, translate,
                    0f, 0f, contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            paint.colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
            canvas.drawBitmap(scaled, 0f, 0f, paint)

            if (scaled != source) scaled.recycle()
            gray
        } catch (_: Exception) {
            null
        }
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

    override suspend fun rotate(
        sourceUri: Uri,
        rotationDegrees: Int,
        selectedPages: List<Int>?,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> = withContext(dispatchers.io) {
        var tempSource: File? = null
        var tempDest: File? = null
        try {
            onProgress(EngineProgress(10, "قراءة مستند PDF…"))
            tempSource = tempFileManager.createTempPdfFile("rotate_src")
            if (!tempFileManager.copyUriToTempFile(sourceUri, tempSource)) {
                return@withContext AppResult.Error(AppError.CorruptedPdf("تعذر فتح ملف الـ PDF"))
            }

            onProgress(EngineProgress(35, "تطبيق تدوير الصفحات…"))
            tempDest = tempFileManager.createTempPdfFile("rotate_out")
            PDDocument.load(tempSource).use { doc ->
                val total = doc.numberOfPages
                for (i in 0 until total) {
                    val pageNum = i + 1
                    if (selectedPages == null || selectedPages.contains(pageNum)) {
                        val page = doc.getPage(i)
                        val newRotation = (page.rotation + rotationDegrees) % 360
                        page.rotation = if (newRotation < 0) newRotation + 360 else newRotation
                    }
                    val percent = 35 + ((i + 1) * 45 / total)
                    onProgress(EngineProgress(percent, "تدوير الصفحة ${i + 1} من $total…"))
                }
                onProgress(EngineProgress(85, "حفظ المستند المُدوّر…"))
                doc.save(tempDest)
            }

            val success = tempFileManager.copyFileToUri(tempDest, destinationUri)
            val size = tempDest.length()
            if (!success) {
                return@withContext AppResult.Error(AppError.InsufficientStorage("تعذر حفظ الملف المُدوّر"))
            }

            onProgress(EngineProgress(100, "اكتمل التدوير بنجاح"))
            AppResult.Success(size)
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.message ?: "فشل في تدوير صفحات الـ PDF"))
        } finally {
            tempFileManager.deleteFileSafely(tempSource)
            tempFileManager.deleteFileSafely(tempDest)
        }
    }

    override suspend fun protect(
        sourceUri: Uri,
        userPassword: String,
        ownerPassword: String?,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> = withContext(dispatchers.io) {
        var tempSource: File? = null
        var tempDest: File? = null
        try {
            onProgress(EngineProgress(10, "قراءة المستند الأصلي…"))
            tempSource = tempFileManager.createTempPdfFile("protect_src")
            if (!tempFileManager.copyUriToTempFile(sourceUri, tempSource)) {
                return@withContext AppResult.Error(AppError.CorruptedPdf("تعذر فتح ملف الـ PDF"))
            }

            onProgress(EngineProgress(40, "تشفير وحماية الـ PDF…"))
            tempDest = tempFileManager.createTempPdfFile("protect_out")
            PDDocument.load(tempSource).use { doc ->
                val ap = com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission()
                ap.setCanPrint(true)
                ap.setCanExtractContent(true)
                val spp = com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy(
                    ownerPassword?.ifBlank { null } ?: userPassword,
                    userPassword,
                    ap
                )
                spp.encryptionKeyLength = 128
                doc.protect(spp)
                onProgress(EngineProgress(80, "حفظ الملف المشفر…"))
                doc.save(tempDest)
            }

            val success = tempFileManager.copyFileToUri(tempDest, destinationUri)
            val size = tempDest.length()
            if (!success) {
                return@withContext AppResult.Error(AppError.InsufficientStorage("تعذر حفظ الملف المشفر"))
            }

            onProgress(EngineProgress(100, "تم تشفير وتأمين الملف بنجاح"))
            AppResult.Success(size)
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.message ?: "فشل في تشفير ملف الـ PDF"))
        } finally {
            tempFileManager.deleteFileSafely(tempSource)
            tempFileManager.deleteFileSafely(tempDest)
        }
    }

    override suspend fun unlock(
        sourceUri: Uri,
        password: String,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> = withContext(dispatchers.io) {
        var tempSource: File? = null
        var tempDest: File? = null
        try {
            onProgress(EngineProgress(15, "التحقق من كلمة المرور وفك القفل…"))
            tempSource = tempFileManager.createTempPdfFile("unlock_src")
            if (!tempFileManager.copyUriToTempFile(sourceUri, tempSource)) {
                return@withContext AppResult.Error(AppError.CorruptedPdf("تعذر فتح ملف الـ PDF"))
            }

            tempDest = tempFileManager.createTempPdfFile("unlock_out")
            try {
                PDDocument.load(tempSource, password).use { doc ->
                    onProgress(EngineProgress(60, "إزالة التشفير والقيود الأمنية…"))
                    if (doc.isEncrypted) {
                        doc.isAllSecurityToBeRemoved = true
                    }
                    onProgress(EngineProgress(85, "حفظ النسخة غير المقفلة…"))
                    doc.save(tempDest)
                }
            } catch (e: com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException) {
                return@withContext AppResult.Error(AppError.PermissionDenied("كلمة المرور غير صحيحة"))
            }

            val success = tempFileManager.copyFileToUri(tempDest, destinationUri)
            val size = tempDest.length()
            if (!success) {
                return@withContext AppResult.Error(AppError.InsufficientStorage("تعذر حفظ الملف غير المقفل"))
            }

            onProgress(EngineProgress(100, "تم فك قفل الملف بنجاح"))
            AppResult.Success(size)
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.message ?: "فشل في فك قفل ملف الـ PDF"))
        } finally {
            tempFileManager.deleteFileSafely(tempSource)
            tempFileManager.deleteFileSafely(tempDest)
        }
    }

    override suspend fun readMetadata(sourceUri: Uri): AppResult<PdfMetadata> = withContext(dispatchers.io) {
        var tempSource: File? = null
        try {
            tempSource = tempFileManager.createTempPdfFile("meta_read")
            if (!tempFileManager.copyUriToTempFile(sourceUri, tempSource)) {
                return@withContext AppResult.Error(AppError.CorruptedPdf("تعذر فتح ملف الـ PDF"))
            }
            PDDocument.load(tempSource).use { doc ->
                val info = doc.documentInformation
                val metadata = PdfMetadata(
                    title = info.title,
                    author = info.author,
                    subject = info.subject,
                    keywords = info.keywords,
                    creator = info.creator,
                    producer = info.producer,
                    creationDate = info.creationDate?.time?.toString(),
                    modificationDate = info.modificationDate?.time?.toString(),
                    pageCount = doc.numberOfPages
                )
                AppResult.Success(metadata)
            }
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.message ?: "فشل في قراءة بيانات المستند"))
        } finally {
            tempFileManager.deleteFileSafely(tempSource)
        }
    }

    override suspend fun updateMetadata(
        sourceUri: Uri,
        title: String?,
        author: String?,
        subject: String?,
        keywords: String?,
        clearAll: Boolean,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> = withContext(dispatchers.io) {
        var tempSource: File? = null
        var tempDest: File? = null
        try {
            onProgress(EngineProgress(20, "قراءة بيانات المستند…"))
            tempSource = tempFileManager.createTempPdfFile("meta_src")
            if (!tempFileManager.copyUriToTempFile(sourceUri, tempSource)) {
                return@withContext AppResult.Error(AppError.CorruptedPdf("تعذر فتح ملف الـ PDF"))
            }

            tempDest = tempFileManager.createTempPdfFile("meta_out")
            PDDocument.load(tempSource).use { doc ->
                onProgress(EngineProgress(50, if (clearAll) "محو كافة البيانات الوصفية الحساسة…" else "تحديث البيانات الوصفية…"))
                if (clearAll) {
                    doc.documentInformation = com.tom_roush.pdfbox.pdmodel.PDDocumentInformation()
                } else {
                    val info = doc.documentInformation
                    title?.let { info.title = it }
                    author?.let { info.author = it }
                    subject?.let { info.subject = it }
                    keywords?.let { info.keywords = it }
                }
                onProgress(EngineProgress(85, "حفظ المستند…"))
                doc.save(tempDest)
            }

            val success = tempFileManager.copyFileToUri(tempDest, destinationUri)
            val size = tempDest.length()
            if (!success) {
                return@withContext AppResult.Error(AppError.InsufficientStorage("تعذر حفظ المستند المحدث"))
            }

            onProgress(EngineProgress(100, "اكتمل تحديث البيانات الوصفية بنجاح"))
            AppResult.Success(size)
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.message ?: "فشل في معالجة البيانات الوصفية"))
        } finally {
            tempFileManager.deleteFileSafely(tempSource)
            tempFileManager.deleteFileSafely(tempDest)
        }
    }

    override suspend fun flatten(
        sourceUri: Uri,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> = withContext(dispatchers.io) {
        var tempSource: File? = null
        var tempDest: File? = null
        try {
            onProgress(EngineProgress(20, "قراءة مستند الـ PDF وحقول النماذج…"))
            tempSource = tempFileManager.createTempPdfFile("flatten_src")
            if (!tempFileManager.copyUriToTempFile(sourceUri, tempSource)) {
                return@withContext AppResult.Error(AppError.CorruptedPdf("تعذر فتح ملف الـ PDF"))
            }

            tempDest = tempFileManager.createTempPdfFile("flatten_out")
            PDDocument.load(tempSource).use { doc ->
                onProgress(EngineProgress(55, "تسطيح وتثبيت الحقول والتعليقات بالصفحات…"))
                val acroForm = doc.documentCatalog.acroForm
                if (acroForm != null) {
                    acroForm.flatten()
                }
                onProgress(EngineProgress(85, "حفظ المستند المسطّح…"))
                doc.save(tempDest)
            }

            val success = tempFileManager.copyFileToUri(tempDest, destinationUri)
            val size = tempDest.length()
            if (!success) {
                return@withContext AppResult.Error(AppError.InsufficientStorage("تعذر حفظ المستند المسطح"))
            }

            onProgress(EngineProgress(100, "اكتمل تسطيح النماذج والتعليقات بنجاح"))
            AppResult.Success(size)
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.message ?: "فشل في تسطيح مستند الـ PDF"))
        } finally {
            tempFileManager.deleteFileSafely(tempSource)
            tempFileManager.deleteFileSafely(tempDest)
        }
    }

    override suspend fun organizePages(
        sourceUri: Uri,
        pageOrder: List<Int>,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> = withContext(dispatchers.io) {
        var tempSource: File? = null
        var tempDest: File? = null
        try {
            onProgress(EngineProgress(10, "فتح مستند الـ PDF المصدر…"))
            tempSource = tempFileManager.createTempPdfFile("organize_src")
            if (!tempFileManager.copyUriToTempFile(sourceUri, tempSource)) {
                return@withContext AppResult.Error(AppError.CorruptedPdf("تعذر فتح ملف الـ PDF"))
            }

            tempDest = tempFileManager.createTempPdfFile("organize_out")
            PDDocument.load(tempSource).use { sourceDoc ->
                val totalSourcePages = sourceDoc.numberOfPages
                if (totalSourcePages == 0) {
                    return@withContext AppResult.Error(AppError.CorruptedPdf("المستند لا يحتوي على أي صفحات"))
                }

                PDDocument().use { targetDoc ->
                    val totalToExport = pageOrder.size
                    pageOrder.forEachIndexed { idx, pageIdx ->
                        ensureActive()
                        if (pageIdx in 0 until totalSourcePages) {
                            val page = sourceDoc.getPage(pageIdx)
                            targetDoc.importPage(page)
                        }
                        val pct = 15 + ((idx + 1).toFloat() / totalToExport * 70).toInt()
                        onProgress(EngineProgress(pct, "معالجة وترتيب صفحة ${idx + 1} من $totalToExport…"))
                    }

                    onProgress(EngineProgress(88, "حفظ المستند المرتب…"))
                    targetDoc.save(tempDest)
                }
            }

            val success = tempFileManager.copyFileToUri(tempDest, destinationUri)
            val size = tempDest.length()
            if (!success) {
                return@withContext AppResult.Error(AppError.InsufficientStorage("تعذر حفظ المستند المرتب"))
            }

            onProgress(EngineProgress(100, "اكتمل ترتيب وحفظ الصفحات بنجاح"))
            AppResult.Success(size)
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.message ?: "فشل في إعادة ترتيب الصفحات"))
        } finally {
            tempFileManager.deleteFileSafely(tempSource)
            tempFileManager.deleteFileSafely(tempDest)
        }
    }

    override suspend fun addPageNumbers(
        sourceUri: Uri,
        position: PageNumberPosition,
        format: PageNumberFormat,
        startNumber: Int,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> = withContext(dispatchers.io) {
        var tempSource: File? = null
        var tempDest: File? = null
        try {
            onProgress(EngineProgress(10, "فتح مستند الـ PDF للترقيم…"))
            tempSource = tempFileManager.createTempPdfFile("page_num_src")
            if (!tempFileManager.copyUriToTempFile(sourceUri, tempSource)) {
                return@withContext AppResult.Error(AppError.CorruptedPdf("تعذر فتح ملف الـ PDF"))
            }

            tempDest = tempFileManager.createTempPdfFile("page_num_out")
            PDDocument.load(tempSource).use { doc ->
                val totalPages = doc.numberOfPages
                val font = PDType1Font.HELVETICA
                val fontSize = 10f
                val margin = 36f

                for (i in 0 until totalPages) {
                    ensureActive()
                    val currentNumber = startNumber + i
                    val text = format.template
                        .replace("{n}", currentNumber.toString())
                        .replace("{total}", totalPages.toString())

                    val page = doc.getPage(i)
                    val box = page.cropBox ?: page.mediaBox
                    val textWidth = font.getStringWidth(text) / 1000f * fontSize

                    val x = when (position) {
                        PageNumberPosition.BOTTOM_LEFT, PageNumberPosition.TOP_LEFT -> box.lowerLeftX + margin
                        PageNumberPosition.BOTTOM_CENTER, PageNumberPosition.TOP_CENTER -> box.lowerLeftX + (box.width - textWidth) / 2f
                        PageNumberPosition.BOTTOM_RIGHT, PageNumberPosition.TOP_RIGHT -> box.upperRightX - margin - textWidth
                    }

                    val y = when (position) {
                        PageNumberPosition.BOTTOM_LEFT, PageNumberPosition.BOTTOM_CENTER, PageNumberPosition.BOTTOM_RIGHT -> box.lowerLeftY + margin
                        PageNumberPosition.TOP_LEFT, PageNumberPosition.TOP_CENTER, PageNumberPosition.TOP_RIGHT -> box.upperRightY - margin - fontSize
                    }

                    val contentStream = PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)
                    contentStream.beginText()
                    contentStream.setFont(font, fontSize)
                    contentStream.setNonStrokingColor(0.2f, 0.2f, 0.2f)
                    contentStream.newLineAtOffset(x, y)
                    contentStream.showText(text)
                    contentStream.endText()
                    contentStream.close()

                    val pct = 15 + ((i + 1).toFloat() / totalPages * 70).toInt()
                    onProgress(EngineProgress(pct, "إضافة الترقيم لصفحة ${i + 1} من $totalPages…"))
                }

                onProgress(EngineProgress(88, "حفظ المستند المرقم…"))
                doc.save(tempDest)
            }

            val success = tempFileManager.copyFileToUri(tempDest, destinationUri)
            val size = tempDest.length()
            if (!success) {
                return@withContext AppResult.Error(AppError.InsufficientStorage("تعذر حفظ المستند المرقم"))
            }

            onProgress(EngineProgress(100, "تم ترقيم جميع الصفحات بنجاح"))
            AppResult.Success(size)
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.message ?: "فشل في إضافة أرقام الصفحات"))
        } finally {
            tempFileManager.deleteFileSafely(tempSource)
            tempFileManager.deleteFileSafely(tempDest)
        }
    }

    override suspend fun convertToGrayscale(
        sourceUri: Uri,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> = withContext(dispatchers.io) {
        var tempSource: File? = null
        var tempDest: File? = null
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            onProgress(EngineProgress(10, "تجهيز مستند PDF للتحويل…"))
            tempSource = tempFileManager.createTempPdfFile("gray_src")
            if (!tempFileManager.copyUriToTempFile(sourceUri, tempSource)) {
                return@withContext AppResult.Error(AppError.CorruptedPdf("تعذر فتح ملف الـ PDF"))
            }

            tempDest = tempFileManager.createTempPdfFile("gray_out")
            pfd = ParcelFileDescriptor.open(tempSource, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val totalPages = renderer.pageCount

            PDDocument().use { targetDoc ->
                val grayMatrix = ColorMatrix().apply { setSaturation(0f) }
                val paint = Paint().apply {
                    colorFilter = ColorMatrixColorFilter(grayMatrix)
                }

                for (i in 0 until totalPages) {
                    ensureActive()
                    val page = renderer.openPage(i)
                    val width = (page.width * 1.5f).toInt()
                    val height = (page.height * 1.5f).toInt()

                    val colorBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(colorBmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    val pageWidth = page.width.toFloat()
                    val pageHeight = page.height.toFloat()
                    page.close()

                    val grayBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(grayBmp)
                    canvas.drawBitmap(colorBmp, 0f, 0f, paint)
                    colorBmp.recycle()

                    val pdPage = PDPage(PDRectangle(pageWidth, pageHeight))
                    targetDoc.addPage(pdPage)
                    val pdImage = JPEGFactory.createFromImage(targetDoc, grayBmp, 0.85f)
                    grayBmp.recycle()

                    PDPageContentStream(targetDoc, pdPage).use { cs ->
                        cs.drawImage(pdImage, 0f, 0f, pageWidth, pageHeight)
                    }

                    val pct = 15 + ((i + 1).toFloat() / totalPages * 70).toInt()
                    onProgress(EngineProgress(pct, "تحويل صفحة ${i + 1} من $totalPages لأبيض وأسود…"))
                }

                onProgress(EngineProgress(88, "حفظ المستند الرمادي النهائي…"))
                targetDoc.save(tempDest)
            }

            renderer.close()
            renderer = null
            pfd.close()
            pfd = null

            val success = tempFileManager.copyFileToUri(tempDest, destinationUri)
            val size = tempDest.length()
            if (!success) {
                return@withContext AppResult.Error(AppError.InsufficientStorage("تعذر حفظ المستند الرمادي"))
            }

            onProgress(EngineProgress(100, "اكتمل تحويل المستند لتدرج رمادي بنجاح"))
            AppResult.Success(size)
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.message ?: "فشل في تحويل المستند لتدرج رمادي"))
        } finally {
            renderer?.close()
            pfd?.close()
            tempFileManager.deleteFileSafely(tempSource)
            tempFileManager.deleteFileSafely(tempDest)
        }
    }

    override suspend fun cropPages(
        sourceUri: Uri,
        margins: CropMargins,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> = withContext(dispatchers.io) {
        var tempSource: File? = null
        var tempDest: File? = null
        try {
            onProgress(EngineProgress(10, "فتح ملف الـ PDF لقص الهوامش…"))
            tempSource = tempFileManager.createTempPdfFile("crop_src")
            if (!tempFileManager.copyUriToTempFile(sourceUri, tempSource)) {
                return@withContext AppResult.Error(AppError.CorruptedPdf("تعذر فتح ملف الـ PDF"))
            }

            tempDest = tempFileManager.createTempPdfFile("crop_out")
            PDDocument.load(tempSource).use { doc ->
                val totalPages = doc.numberOfPages

                for (i in 0 until totalPages) {
                    ensureActive()
                    val page = doc.getPage(i)
                    val mediaBox = page.mediaBox
                    val newLowerLeftX = mediaBox.lowerLeftX + margins.leftPt
                    val newLowerLeftY = mediaBox.lowerLeftY + margins.bottomPt
                    val newWidth = mediaBox.width - margins.leftPt - margins.rightPt
                    val newHeight = mediaBox.height - margins.topPt - margins.bottomPt

                    if (newWidth > 20f && newHeight > 20f) {
                        page.cropBox = PDRectangle(newLowerLeftX, newLowerLeftY, newWidth, newHeight)
                    }

                    val pct = 15 + ((i + 1).toFloat() / totalPages * 70).toInt()
                    onProgress(EngineProgress(pct, "تطبيق قص الهوامش على صفحة ${i + 1} من $totalPages…"))
                }

                onProgress(EngineProgress(88, "حفظ المستند المقصوص…"))
                doc.save(tempDest)
            }

            val success = tempFileManager.copyFileToUri(tempDest, destinationUri)
            val size = tempDest.length()
            if (!success) {
                return@withContext AppResult.Error(AppError.InsufficientStorage("تعذر حفظ المستند المقصوص"))
            }

            onProgress(EngineProgress(100, "اكتمل قص الهوامش بنجاح"))
            AppResult.Success(size)
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.message ?: "فشل في قص هوامش المستند"))
        } finally {
            tempFileManager.deleteFileSafely(tempSource)
            tempFileManager.deleteFileSafely(tempDest)
        }
    }

    override suspend fun redactAreas(
        sourceUri: Uri,
        redactions: List<RedactionRect>,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> = withContext(dispatchers.io) {
        var tempSource: File? = null
        var tempDest: File? = null
        try {
            onProgress(EngineProgress(10, "فتح مستند الـ PDF لتطبيق الحجب…"))
            tempSource = tempFileManager.createTempPdfFile("redact_src")
            if (!tempFileManager.copyUriToTempFile(sourceUri, tempSource)) {
                return@withContext AppResult.Error(AppError.CorruptedPdf("تعذر فتح ملف الـ PDF"))
            }

            tempDest = tempFileManager.createTempPdfFile("redact_out")
            PDDocument.load(tempSource).use { doc ->
                val totalPages = doc.numberOfPages
                val redactionsByPage = redactions.groupBy { it.pageIndex }

                redactionsByPage.forEach { (pageIdx, rects) ->
                    ensureActive()
                    if (pageIdx in 0 until totalPages) {
                        val page = doc.getPage(pageIdx)
                        val box = page.cropBox ?: page.mediaBox
                        val contentStream = PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)
                        contentStream.setNonStrokingColor(0f, 0f, 0f)

                        for (rect in rects) {
                            val x = box.lowerLeftX + (rect.leftRatio * box.width)
                            val width = rect.widthRatio * box.width
                            val height = rect.heightRatio * box.height
                            val y = box.upperRightY - (rect.topRatio * box.height) - height

                            contentStream.addRect(x, y, width, height)
                            contentStream.fill()
                        }
                        contentStream.close()
                    }
                }

                onProgress(EngineProgress(88, "حفظ المستند المحجوب…"))
                doc.save(tempDest)
            }

            val success = tempFileManager.copyFileToUri(tempDest, destinationUri)
            val size = tempDest.length()
            if (!success) {
                return@withContext AppResult.Error(AppError.InsufficientStorage("تعذر حفظ المستند المحجوب"))
            }

            onProgress(EngineProgress(100, "تم حجب وتعتيم المناطق المحددة بنجاح"))
            AppResult.Success(size)
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.message ?: "فشل في حجب المناطق المحددة"))
        } finally {
            tempFileManager.deleteFileSafely(tempSource)
            tempFileManager.deleteFileSafely(tempDest)
        }
    }

    private fun renderSpannedToPdfFile(
        spanned: CharSequence,
        outputFile: File,
        onProgress: (EngineProgress) -> Unit
    ) {
        val pdfDoc = android.graphics.pdf.PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 45
        val contentWidth = pageWidth - (margin * 2)
        val contentHeight = pageHeight - (margin * 2)

        val textPaint = android.text.TextPaint().apply {
            isAntiAlias = true
            textSize = 13f
            color = android.graphics.Color.BLACK
        }

        val staticLayout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.text.StaticLayout.Builder.obtain(spanned, 0, spanned.length, textPaint, contentWidth)
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.25f)
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            android.text.StaticLayout(
                spanned,
                textPaint,
                contentWidth,
                android.text.Layout.Alignment.ALIGN_NORMAL,
                1.25f,
                0f,
                false
            )
        }

        var lineIndex = 0
        var pageNumber = 1
        val totalLines = staticLayout.lineCount

        while (lineIndex < totalLines) {
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            canvas.save()
            canvas.translate(margin.toFloat(), margin.toFloat())

            val startY = staticLayout.getLineTop(lineIndex)
            var endLine = lineIndex
            while (endLine < totalLines && (staticLayout.getLineBottom(endLine) - startY) <= contentHeight) {
                endLine++
            }

            canvas.clipRect(0, 0, contentWidth, contentHeight)
            canvas.translate(0f, -startY.toFloat())
            staticLayout.draw(canvas)
            canvas.restore()

            val numPaint = android.text.TextPaint().apply {
                textSize = 9f
                color = android.graphics.Color.GRAY
                textAlign = android.graphics.Paint.Align.CENTER
            }
            page.canvas.drawText("- $pageNumber -", (pageWidth / 2).toFloat(), (pageHeight - 20).toFloat(), numPaint)

            pdfDoc.finishPage(page)
            val pct = 20 + ((lineIndex.toFloat() / maxOf(1, totalLines)) * 60).toInt()
            onProgress(EngineProgress(pct, "توليد صفحة $pageNumber من المستند…"))

            pageNumber++
            lineIndex = if (endLine > lineIndex) endLine else lineIndex + 1
        }

        outputFile.outputStream().use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()
    }

    override suspend fun textToPdf(
        text: String,
        title: String?,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> = withContext(dispatchers.io) {
        var tempDest: File? = null
        try {
            onProgress(EngineProgress(10, "تجهيز وتنسيق النصوص للـ PDF…"))
            tempDest = tempFileManager.createTempPdfFile("text_to_pdf")
            val fullText = if (!title.isNullOrBlank()) "$title\n\n$text" else text
            renderSpannedToPdfFile(fullText, tempDest, onProgress)

            onProgress(EngineProgress(90, "حفظ ملف الـ PDF النهائي…"))
            val success = tempFileManager.copyFileToUri(tempDest, destinationUri)
            val size = tempDest.length()
            if (!success) {
                return@withContext AppResult.Error(AppError.InsufficientStorage("تعذر حفظ ملف الـ PDF"))
            }

            onProgress(EngineProgress(100, "اكتمل تحويل النص إلى PDF بنجاح"))
            AppResult.Success(size)
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.message ?: "فشل في تحويل النص إلى PDF"))
        } finally {
            tempFileManager.deleteFileSafely(tempDest)
        }
    }

    override suspend fun markdownToPdf(
        markdownText: String,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> = withContext(dispatchers.io) {
        var tempDest: File? = null
        try {
            onProgress(EngineProgress(10, "تحليل شفرة الماركداون وتنسيق العناصر…"))
            tempDest = tempFileManager.createTempPdfFile("md_to_pdf")

            var html = markdownText
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
            html = html.replace(Regex("(?m)^### (.*)$"), "<h3>$1</h3>")
            html = html.replace(Regex("(?m)^## (.*)$"), "<h2>$1</h2>")
            html = html.replace(Regex("(?m)^# (.*)$"), "<h1>$1</h1>")
            html = html.replace(Regex("\\*\\*(.*?)\\*\\*"), "<b>$1</b>")
            html = html.replace(Regex("\\*(.*?)\\*"), "<i>$1</i>")
            html = html.replace(Regex("(?m)^[-*] (.*)$"), "• $1<br/>")
            html = html.replace("\n", "<br/>")

            val spanned = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_LEGACY)
            } else {
                @Suppress("DEPRECATION")
                android.text.Html.fromHtml(html)
            }

            renderSpannedToPdfFile(spanned, tempDest, onProgress)

            val success = tempFileManager.copyFileToUri(tempDest, destinationUri)
            val size = tempDest.length()
            if (!success) {
                return@withContext AppResult.Error(AppError.InsufficientStorage("تعذر حفظ ملف الـ PDF"))
            }

            onProgress(EngineProgress(100, "تم تحويل الماركداون إلى PDF بنجاح"))
            AppResult.Success(size)
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.message ?: "فشل في تحويل الماركداون إلى PDF"))
        } finally {
            tempFileManager.deleteFileSafely(tempDest)
        }
    }

    override suspend fun htmlToPdf(
        htmlText: String,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> = withContext(dispatchers.io) {
        var tempDest: File? = null
        try {
            onProgress(EngineProgress(10, "تحليل وتنسيق عناصر HTML…"))
            tempDest = tempFileManager.createTempPdfFile("html_to_pdf")

            val spanned = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.text.Html.fromHtml(htmlText, android.text.Html.FROM_HTML_MODE_LEGACY)
            } else {
                @Suppress("DEPRECATION")
                android.text.Html.fromHtml(htmlText)
            }

            renderSpannedToPdfFile(spanned, tempDest, onProgress)

            val success = tempFileManager.copyFileToUri(tempDest, destinationUri)
            val size = tempDest.length()
            if (!success) {
                return@withContext AppResult.Error(AppError.InsufficientStorage("تعذر حفظ ملف الـ PDF"))
            }

            onProgress(EngineProgress(100, "تم تحويل كود HTML إلى PDF بنجاح"))
            AppResult.Success(size)
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.message ?: "فشل في تحويل HTML إلى PDF"))
        } finally {
            tempFileManager.deleteFileSafely(tempDest)
        }
    }

    override suspend fun rtfToPdf(
        rtfText: String,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> = withContext(dispatchers.io) {
        var tempDest: File? = null
        try {
            onProgress(EngineProgress(10, "استخراج النصوص والتنسيق من ملف RTF…"))
            tempDest = tempFileManager.createTempPdfFile("rtf_to_pdf")

            val plain = rtfText
                .replace(Regex("\\[a-zA-Z0-9]+ ?"), "")
                .replace("{", "")
                .replace("}", "")
                .trim()

            renderSpannedToPdfFile(plain, tempDest, onProgress)

            val success = tempFileManager.copyFileToUri(tempDest, destinationUri)
            val size = tempDest.length()
            if (!success) {
                return@withContext AppResult.Error(AppError.InsufficientStorage("تعذر حفظ ملف الـ PDF"))
            }

            onProgress(EngineProgress(100, "تم تحويل ملف RTF إلى PDF بنجاح"))
            AppResult.Success(size)
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.message ?: "فشل في تحويل RTF إلى PDF"))
        } finally {
            tempFileManager.deleteFileSafely(tempDest)
        }
    }

    override suspend fun pdfToMarkdown(
        sourceUri: Uri,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> = withContext(dispatchers.io) {
        var tempSource: File? = null
        var tempDest: File? = null
        try {
            onProgress(EngineProgress(15, "فتح مستند PDF واستخراج النصوص…"))
            tempSource = tempFileManager.createTempPdfFile("pdf_to_md_src")
            if (!tempFileManager.copyUriToTempFile(sourceUri, tempSource)) {
                return@withContext AppResult.Error(AppError.CorruptedPdf("تعذر فتح ملف الـ PDF"))
            }

            tempDest = tempFileManager.createTempPdfFile("converted_md")
            val stripper = PDFTextStripper()
            val text = PDDocument.load(tempSource).use { doc ->
                stripper.getText(doc)
            }

            onProgress(EngineProgress(60, "معالجة وهيكلة العناوين والفقرات كماركداون…"))
            val lines = text.lines()
            val mdBuilder = StringBuilder()
            var isFirstLine = true

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isBlank()) {
                    mdBuilder.append("\n\n")
                    continue
                }

                if (isFirstLine && trimmed.length < 80) {
                    mdBuilder.append("# ").append(trimmed).append("\n\n")
                    isFirstLine = false
                } else if (trimmed.length < 50 && (trimmed.startsWith("فصل") || trimmed.startsWith("باب") || trimmed.startsWith("Section") || trimmed.startsWith("Chapter") || (trimmed.all { it.isUpperCase() || it.isWhitespace() } && trimmed.length > 3))) {
                    mdBuilder.append("## ").append(trimmed).append("\n\n")
                } else if (trimmed.startsWith("•") || trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                    mdBuilder.append("* ").append(trimmed.removePrefix("•").removePrefix("-").removePrefix("*").trim()).append("\n")
                } else {
                    mdBuilder.append(trimmed).append(" ")
                }
            }

            tempDest.writeText(mdBuilder.toString(), Charsets.UTF_8)
            val success = tempFileManager.copyFileToUri(tempDest, destinationUri)
            val size = tempDest.length()
            if (!success) {
                return@withContext AppResult.Error(AppError.InsufficientStorage("تعذر حفظ ملف الماركداون"))
            }

            onProgress(EngineProgress(100, "تم استخراج وحفظ ملف الماركداون بنجاح!"))
            AppResult.Success(size)
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.message ?: "فشل في تحويل الـ PDF لماركداون"))
        } finally {
            tempFileManager.deleteFileSafely(tempSource)
            tempFileManager.deleteFileSafely(tempDest)
        }
    }

    override suspend fun convertOrCompressImage(
        sourceUri: Uri,
        options: ImageProcessOptions,
        destinationUri: Uri,
        onProgress: (EngineProgress) -> Unit
    ): AppResult<Long> = withContext(dispatchers.io) {
        var tempDest: File? = null
        try {
            onProgress(EngineProgress(10, "قراءة وفك ترميز الصورة الأصلية…"))
            val inStream = safHelper.openInputStream(sourceUri)
                ?: return@withContext AppResult.Error(AppError.FileNotFound("تعذر فتح ملف الصورة"))

            val originalBmp = inStream.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return@withContext AppResult.Error(AppError.UnsupportedFormat("تعذر فك ترميز ملف الصورة المحدد"))

            onProgress(EngineProgress(35, "تطبيق تغيير الأبعاد والضغط…"))
            val finalBmp = if (options.scalePercent in 10..99) {
                val newW = (originalBmp.width * options.scalePercent / 100).coerceAtLeast(1)
                val newH = (originalBmp.height * options.scalePercent / 100).coerceAtLeast(1)
                Bitmap.createScaledBitmap(originalBmp, newW, newH, true).also {
                    if (it != originalBmp) originalBmp.recycle()
                }
            } else {
                originalBmp
            }

            if (options.outputFormat == ImageOutputFormat.PDF) {
                onProgress(EngineProgress(65, "توليد مستند PDF من الصورة…"))
                tempDest = tempFileManager.createTempPdfFile("img_to_pdf")
                PDDocument().use { doc ->
                    val page = PDPage(PDRectangle(finalBmp.width.toFloat(), finalBmp.height.toFloat()))
                    doc.addPage(page)
                    val pdImage = JPEGFactory.createFromImage(doc, finalBmp, (options.qualityPercent / 100f).coerceIn(0.1f, 1.0f))
                    PDPageContentStream(doc, page).use { cs ->
                        cs.drawImage(pdImage, 0f, 0f, finalBmp.width.toFloat(), finalBmp.height.toFloat())
                    }
                    doc.save(tempDest)
                }
                finalBmp.recycle()
            } else {
                onProgress(EngineProgress(65, "ضغط وترميز الصورة إلى صيغة ${options.outputFormat.extension.uppercase()}…"))
                val imgFile = tempFileManager.createTempImageFile(options.outputFormat.extension)
                tempDest = imgFile
                val compressFormat = when (options.outputFormat) {
                    ImageOutputFormat.PNG -> Bitmap.CompressFormat.PNG
                    ImageOutputFormat.JPEG -> Bitmap.CompressFormat.JPEG
                    ImageOutputFormat.WEBP -> {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            Bitmap.CompressFormat.WEBP_LOSSY
                        } else {
                            @Suppress("DEPRECATION")
                            Bitmap.CompressFormat.WEBP
                        }
                    }
                    ImageOutputFormat.PDF -> Bitmap.CompressFormat.PNG
                }

                imgFile.outputStream().use { out ->
                    finalBmp.compress(compressFormat, options.qualityPercent.coerceIn(1, 100), out)
                }
                finalBmp.recycle()
            }

            val readyFile = tempDest ?: return@withContext AppResult.Error(AppError.Unknown("تعذر إنشاء ملف المخرجات"))
            onProgress(EngineProgress(90, "حفظ الصورة النهائية…"))
            val success = tempFileManager.copyFileToUri(readyFile, destinationUri)
            val size = readyFile.length()
            if (!success) {
                return@withContext AppResult.Error(AppError.InsufficientStorage("تعذر حفظ الملف النهائي"))
            }

            onProgress(EngineProgress(100, "اكتملت المعالجة بنجاح!"))
            AppResult.Success(size)
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.message ?: "فشل في معالجة وتحويل الصورة"))
        } finally {
            tempFileManager.deleteFileSafely(tempDest)
        }
    }
}
