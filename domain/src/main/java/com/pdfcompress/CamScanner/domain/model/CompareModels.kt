package com.pdfcompress.CamScanner.domain.model

enum class DiffType {
    UNCHANGED,
    ADDED,
    REMOVED
}

data class DiffLine(
    val type: DiffType,
    val text: String
)

data class PageDiff(
    val pageNumber: Int,
    val hasDiff: Boolean,
    val textA: String,
    val textB: String,
    val diffLines: List<DiffLine>
)

data class CompareResult(
    val totalPagesA: Int,
    val totalPagesB: Int,
    val diffCount: Int,
    val pageDiffs: List<PageDiff>
)
