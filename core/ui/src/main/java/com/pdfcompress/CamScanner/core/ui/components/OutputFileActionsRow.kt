package com.pdfcompress.CamScanner.core.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

fun openOutputUri(context: Context, uri: Uri, defaultMimeType: String = "application/pdf") {
    try {
        val mime = context.contentResolver.getType(uri) ?: defaultMimeType
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(openIntent, "فتح الملف"))
    } catch (_: Exception) {
        Toast.makeText(context, "لا يوجد تطبيق متاح لفتح هذا النوع من الملفات على جهازك", Toast.LENGTH_SHORT).show()
    }
}

fun shareOutputUri(context: Context, uri: Uri, defaultMimeType: String = "application/pdf") {
    try {
        val mime = context.contentResolver.getType(uri) ?: defaultMimeType
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "مشاركة الملف الناتج"))
    } catch (_: Exception) {
        Toast.makeText(context, "تعذر فتح قائمة المشاركة", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun OutputFileActionsRow(
    outputUri: Uri?,
    defaultMimeType: String = "application/pdf",
    modifier: Modifier = Modifier
) {
    val targetUri = outputUri ?: return
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ModernGradientButton(
            text = "مشاركة الملف",
            icon = Icons.Default.Share,
            onClick = { shareOutputUri(context, targetUri, defaultMimeType) },
            height = 46.dp,
            modifier = Modifier.weight(1f)
        )

        ModernSoftButton(
            text = "فتح الملف",
            icon = Icons.AutoMirrored.Filled.OpenInNew,
            onClick = { openOutputUri(context, targetUri, defaultMimeType) },
            height = 46.dp,
            modifier = Modifier.weight(1f)
        )
    }
}
