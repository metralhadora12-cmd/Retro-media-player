package com.retro.cassetteplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val WalkmanColorScheme = darkColorScheme(
    primary = HotlineOrange,
    onPrimary = Navy,
    secondary = WalkmanBlueLight,
    onSecondary = TextPrimary,
    tertiary = Silver,
    background = Navy,
    onBackground = TextPrimary,
    surface = NavySurface,
    onSurface = TextPrimary,
    surfaceVariant = NavyRaised,
    onSurfaceVariant = TextSecondary,
    outline = MetalDark,
)

@Composable
fun RetroCassetteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WalkmanColorScheme,
        typography = RetroTypography,
        content = content,
    )
}
