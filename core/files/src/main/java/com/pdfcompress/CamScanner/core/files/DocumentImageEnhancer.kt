package com.pdfcompress.CamScanner.core.files

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint

enum class ScanFilter(val titleAr: String) {
    ORIGINAL("الأصلية"),
    DOCUMENT_BW("مستند (أبيض وأسود)"),
    GRAYSCALE("تدرج الرمادي"),
    MAGIC_COLOR("ألوان معززة")
}

object DocumentImageEnhancer {

    fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees % 360f == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun applyFilter(source: Bitmap, filter: ScanFilter): Bitmap {
        return when (filter) {
            ScanFilter.ORIGINAL -> source
            ScanFilter.GRAYSCALE -> applyGrayscale(source)
            ScanFilter.DOCUMENT_BW -> applyDocumentBw(source)
            ScanFilter.MAGIC_COLOR -> applyMagicColor(source)
        }
    }

    private fun applyGrayscale(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    private fun applyDocumentBw(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
        val scale = 2.4f
        val translate = (-128f * scale + 128f) + 40f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        colorMatrix.postConcat(contrastMatrix)

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    private fun applyMagicColor(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        val colorMatrix = ColorMatrix().apply { setSaturation(1.35f) }
        val scale = 1.25f
        val translate = (-128f * scale + 128f) + 15f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        colorMatrix.postConcat(contrastMatrix)

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }
}
