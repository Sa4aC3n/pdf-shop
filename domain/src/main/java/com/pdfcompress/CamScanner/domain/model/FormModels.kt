package com.pdfcompress.CamScanner.domain.model

enum class FormFieldType {
    TEXT,
    CHECKBOX,
    RADIO,
    COMBO,
    UNKNOWN
}

data class FormFieldInfo(
    val name: String,
    val value: String,
    val type: FormFieldType,
    val isReadOnly: Boolean
)

data class FormFillRequest(
    val fields: Map<String, String>,
    val flattenAfterFill: Boolean = false
)
