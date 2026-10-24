package com.pdfcompress.CamScanner.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun PDFShopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    primaryColorLong: Long = 0xFFD32F2FL,
    primaryContainerLightLong: Long = 0xFFFFEBEE,
    primaryContainerDarkLong: Long = 0xFF4A1215,
    secondaryColorLong: Long = 0xFFB71C1CL,
    content: @Composable () -> Unit
) {
    val primaryColor = Color(primaryColorLong)
    val secondaryColor = Color(secondaryColorLong)
    val primaryContainerLight = Color(primaryContainerLightLong)
    val primaryContainerDark = Color(primaryContainerDarkLong)

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = primaryContainerDark,
            onPrimaryContainer = Color(0xFFF5F7FA),
            secondary = secondaryColor,
            onSecondary = Color.White,
            secondaryContainer = primaryContainerDark.copy(alpha = 0.7f),
            onSecondaryContainer = Color(0xFFE8ECF4),
            tertiary = AccentAmber,
            background = DarkBackground,
            surface = DarkSurface,
            onBackground = DarkOnSurface,
            onSurface = DarkOnSurface,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = DarkOnSurfaceVariant,
            outline = Color(0xFF333B4D),
            outlineVariant = Color(0xFF262D3B)
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = primaryContainerLight,
            onPrimaryContainer = primaryColor,
            secondary = secondaryColor,
            onSecondary = Color.White,
            secondaryContainer = primaryContainerLight.copy(alpha = 0.75f),
            onSecondaryContainer = secondaryColor,
            tertiary = AccentAmber,
            background = LightBackground,
            surface = LightSurface,
            onBackground = LightOnSurface,
            onSurface = LightOnSurface,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = LightOnSurfaceVariant,
            outline = Color(0xFFDCE1EC),
            outlineVariant = Color(0xFFE7ECF5)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
