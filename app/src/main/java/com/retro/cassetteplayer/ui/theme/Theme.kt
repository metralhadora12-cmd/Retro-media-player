package com.retro.cassetteplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val RetroColorScheme = darkColorScheme(
    primary = RetroOrange,
    onPrimary = Cream,
    secondary = RetroAmber,
    onSecondary = Charcoal,
    tertiary = Cream,
    background = Charcoal,
    onBackground = Cream,
    surface = Graphite,
    onSurface = Cream,
    surfaceVariant = Gunmetal,
    onSurfaceVariant = CreamMuted,
    outline = MetalDark,
)

@Composable
fun RetroCassetteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RetroColorScheme,
        typography = RetroTypography,
        content = content,
    )
}
