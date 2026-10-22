package com.pdfcompress.CamScanner.core.common.error

sealed class AppError(
    open val message: String,
    open val cause: Throwable? = null
) {
    data class FileNotFound(override val message: String = "الملف غير موجود أو تم نقله") : AppError(message)
    data class PermissionDenied(override val message: String = "لا توجد صلاحية للوصول إلى هذا الملف") : AppError(message)
    data class CorruptedPdf(override val message: String = "الملف تالف أو غير صالح كـ PDF", override val cause: Throwable? = null) : AppError(message, cause)
    data class OutOfMemory(override val message: String = "نفاد ذاكرة الجهاز أثناء المعالجة") : AppError(message)
    data class InsufficientStorage(override val message: String = "مساحة التخزين غير كافية لحفظ الملف الناتج") : AppError(message)
    data class OperationCancelled(override val message: String = "تم إلغاء العملية بأمان") : AppError(message)
    data class UnsupportedFormat(override val message: String = "صيغة الملف غير مدعومة") : AppError(message)
    data class UnsupportedFeature(override val message: String = "هذه الخاصية غير مدعومة لهذا الملف") : AppError(message)
    data class Unknown(override val message: String, override val cause: Throwable? = null) : AppError(message, cause)
}
