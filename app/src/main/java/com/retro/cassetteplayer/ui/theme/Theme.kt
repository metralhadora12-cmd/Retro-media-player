package com.retro.cassetteplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = darkColorScheme(
    primary = TapeOrange,
    onPrimary = TextPrimary,
    secondary = TapeAmber,
    onSecondary = Ink,
    tertiary = LabelCream,
    background = Ink,
    onBackground = TextPrimary,
    surface = InkSurface,
    onSurface = TextPrimary,
    surfaceVariant = InkRaised,
    onSurfaceVariant = TextSecondary,
    outline = InkRaised,
)

@Composable
fun RetroCassetteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = RetroTypography,
        content = content,
    )
}
