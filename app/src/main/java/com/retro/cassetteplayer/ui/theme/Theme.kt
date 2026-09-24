package com.retro.cassetteplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun RetroCassetteTheme(content: @Composable () -> Unit) {
    val p = AppTheme.palette
    val colors = if (AppTheme.isDark) {
        darkColorScheme(
            primary = TapeOrange,
            onPrimary = p.textPrimary,
            secondary = p.tapeAmber,
            onSecondary = p.ink,
            tertiary = LabelCream,
            background = p.ink,
            onBackground = p.textPrimary,
            surface = p.inkSurface,
            onSurface = p.textPrimary,
            surfaceVariant = p.inkRaised,
            onSurfaceVariant = p.textSecondary,
            outline = p.inkRaised,
        )
    } else {
        lightColorScheme(
            primary = TapeOrange,
            onPrimary = p.ink,
            secondary = p.tapeAmber,
            onSecondary = p.ink,
            tertiary = TapeBrown,
            background = p.ink,
            onBackground = p.textPrimary,
            surface = p.inkSurface,
            onSurface = p.textPrimary,
            surfaceVariant = p.inkRaised,
            onSurfaceVariant = p.textSecondary,
            outline = p.inkRaised,
        )
    }
    MaterialTheme(
        colorScheme = colors,
        typography = RetroTypography,
        content = content,
    )
}
