package com.pdfcompress.CamScanner.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Sleek primary action button with a rich horizontal gradient (deep primary -> bright luminous accent),
 * soft colored elevation shadow, and crisp white typography — inspired by modern scanner/PDF pro UIs.
 */
@Composable
fun ModernGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 50.dp,
    cornerRadius: Dp = 15.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val brightEnd = lerp(primary, Color(0xFF00B4D8), 0.42f).let {
        if (secondary != primary) lerp(it, secondary, 0.35f) else it
    }
    val shape = RoundedCornerShape(cornerRadius)

    val gradientBrush = if (enabled) {
        Brush.horizontalGradient(
            colors = listOf(primary, brightEnd)
        )
    } else {
        val disabledColor = MaterialTheme.colorScheme.surfaceVariant
        Brush.horizontalGradient(
            colors = listOf(disabledColor, disabledColor)
        )
    }

    val contentColor = if (enabled) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f)
    }

    Box(
        modifier = modifier
            .heightIn(min = height)
            .then(
                if (enabled) {
                    Modifier.shadow(
                        elevation = 6.dp,
                        shape = shape,
                        ambientColor = primary,
                        spotColor = primary
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(brush = gradientBrush, shape = shape)
            .border(
                width = 1.dp,
                color = if (enabled) Color.White.copy(alpha = 0.22f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                shape = shape
            )
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Modern soft tonal secondary button ("ADD PAGE", "SAVE TO CLOUD", "EXPORT TO JPG" style)
 * with a frosted cool surface tint, subtle border, and high-contrast bold label.
 */
@Composable
fun ModernSoftButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 50.dp,
    cornerRadius: Dp = 15.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val shape = RoundedCornerShape(cornerRadius)

    val containerColor = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        isDark -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        else -> lerp(MaterialTheme.colorScheme.primaryContainer, Color(0xFFE8EEF5), 0.55f)
    }

    val borderColor = when {
        !enabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        isDark -> MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
    }

    val textColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }

    val iconColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (enabled && !isDark) 1.dp else 0.dp,
        modifier = modifier.heightIn(min = height)
    ) {
        Box(
            modifier = Modifier.padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = textColor,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Elevated rounded bottom sheet container for tool action bars.
 */
@Composable
fun ModernBottomBarContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            content = content
        )
    }
}

/**
 * Dual-tone modern filter/tool tile inspired by the "B&W PRO" / "ROTATE" / "CROP" strip.
 * When selected, displays a metallic/soft-gradient upper icon box and a vibrant primary gradient bottom label strip.
 */
@Composable
fun ModernFilterTile(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val brightEnd = lerp(primary, Color(0xFF00B4D8), 0.45f)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val shape = RoundedCornerShape(14.dp)

    if (selected) {
        val topBrush = if (isDark) {
            Brush.verticalGradient(
                colors = listOf(Color(0xFF3A4454), Color(0xFF232B38))
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(Color(0xFFDCE3EC), Color(0xFFF4F7FB))
            )
        }
        val bottomBrush = Brush.horizontalGradient(
            colors = listOf(primary, brightEnd)
        )

        Column(
            modifier = modifier
                .height(68.dp)
                .shadow(
                    elevation = 5.dp,
                    shape = shape,
                    ambientColor = primary,
                    spotColor = primary
                )
                .clip(shape)
                .border(1.5.dp, brightEnd.copy(alpha = 0.85f), shape)
                .clickable(role = Role.RadioButton, onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(topBrush),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isDark) Color.White else Color(0xFF1E293B),
                    modifier = Modifier.size(22.dp)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bottomBrush)
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .height(68.dp)
                .clip(shape)
                .background(
                    if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    shape = shape
                )
                .clickable(role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
