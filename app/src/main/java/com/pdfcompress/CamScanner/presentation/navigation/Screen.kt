package com.pdfcompress.CamScanner.presentation.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object History : Screen("history")
    data object Settings : Screen("settings")
    data object Merge : Screen("merge")
    data object ImagesToPdf : Screen("images_to_pdf")
    data object Split : Screen("split")
    data object Compress : Screen("compress")
    data object Watermark : Screen("watermark")
    data object PdfToImages : Screen("pdf_to_images")
    data object ExtractText : Screen("extract_text")
    data object Repair : Screen("repair")
    data object Scanner : Screen("scanner")
    data object Ocr : Screen("ocr")
}
