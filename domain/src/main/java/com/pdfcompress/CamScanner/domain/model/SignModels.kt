package com.pdfcompress.CamScanner.domain.model

data class SignaturePlacement(
    val pageIndex: Int,
    val xPercent: Float,      // 0f..1f from left of page
    val yPercent: Float,      // 0f..1f from top of page
    val widthPercent: Float,  // 0.1f..0.8f
    val heightPercent: Float  // 0.05f..0.5f
)
