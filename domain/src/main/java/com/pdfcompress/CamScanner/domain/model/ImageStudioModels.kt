package com.pdfcompress.CamScanner.domain.model

enum class ImageOutputFormat(val extension: String, val mimeType: String, val title: String) {
    PNG("png", "image/png", "PNG (جودة أصلية مع شفافية)"),
    JPEG("jpg", "image/jpeg", "JPEG (صورة قياسية مضغوطة)"),
    WEBP("webp", "image/webp", "WEBP (صيغة عصرية فائقة الضغط)"),
    PDF("pdf", "application/pdf", "مستند PDF عالي الجودة")
}

data class ImageProcessOptions(
    val outputFormat: ImageOutputFormat = ImageOutputFormat.PNG,
    val qualityPercent: Int = 85,
    val scalePercent: Int = 100
)
