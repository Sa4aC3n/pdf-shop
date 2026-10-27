package com.pdfcompress.CamScanner.domain.model

enum class PageNumberPosition(val title: String) {
    BOTTOM_CENTER("أسفل الوسط"),
    BOTTOM_RIGHT("أسفل اليمين"),
    BOTTOM_LEFT("أسفل اليسار"),
    TOP_CENTER("أعلى الوسط"),
    TOP_RIGHT("أعلى اليمين"),
    TOP_LEFT("أعلى اليسار")
}

enum class PageNumberFormat(val template: String, val title: String) {
    NUMBER_ONLY("{n}", "رقم فقط (1)"),
    PAGE_X_OF_Y("{n} / {total}", "رقم من إجمالي (1 / 10)"),
    ARABIC_PAGE_X_OF_Y("صفحة {n} من {total}", "صفحة 1 من 10"),
    DASHES("- {n} -", "- 1 -")
}

data class CropMargins(
    val topPt: Float = 0f,
    val bottomPt: Float = 0f,
    val leftPt: Float = 0f,
    val rightPt: Float = 0f
)

data class RedactionRect(
    val pageIndex: Int,
    val leftRatio: Float,    // 0.0 .. 1.0 relative to page width
    val topRatio: Float,     // 0.0 .. 1.0 relative to page height
    val widthRatio: Float,   // 0.0 .. 1.0 relative to page width
    val heightRatio: Float   // 0.0 .. 1.0 relative to page height
)
