package com.pdfcompress.CamScanner

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.fragment.app.FragmentActivity
import com.pdfcompress.CamScanner.core.ui.theme.PDFShopTheme
import com.pdfcompress.CamScanner.domain.model.AppLanguage
import com.pdfcompress.CamScanner.domain.model.AppTheme
import com.pdfcompress.CamScanner.presentation.navigation.AppNavHost

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as PdfShopApplication
        val container = app.container

        setContent {
            val settings by container.settingsRepository.settings.collectAsState(
                initial = com.pdfcompress.CamScanner.domain.model.UserSettings()
            )

            val isDark = when (settings.theme) {
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }

            val layoutDirection = when (settings.language) {
                AppLanguage.ARABIC -> LayoutDirection.Rtl
                AppLanguage.ENGLISH -> LayoutDirection.Ltr
            }

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                PDFShopTheme(darkTheme = isDark) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNavHost(container = container)
                    }
                }
            }
        }
    }
}
