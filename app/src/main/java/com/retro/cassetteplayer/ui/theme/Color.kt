package com.retro.cassetteplayer.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Colours that change between the dark and light themes. */
data class AppPalette(
    val ink: Color,
    val inkSurface: Color,
    val inkRaised: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    /** Accent for text/icons that must stay readable on the background. */
    val tapeAmber: Color,
    /** Thin dividers, ticks and outlines. */
    val hairline: Color,
    val outline: Color,
    val topGlow: List<Color>,
    val playerGlow: List<Color>,
)

val DarkPalette = AppPalette(
    ink = Color(0xFF0B0B0B),
    inkSurface = Color(0xFF1C1C1C),
    inkRaised = Color(0xFF2A2A2A),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFAAAAAA),
    tapeAmber = Color(0xFFF0913F),
    hairline = Color.White.copy(alpha = 0.08f),
    outline = Color.White.copy(alpha = 0.25f),
    topGlow = listOf(Color(0xFF3B2012), Color(0xFF1A120D), Color(0xFF0B0B0B)),
    playerGlow = listOf(Color(0xFF2E1A10), Color(0xFF120D0A), Color(0xFF0B0B0B)),
)

val LightPalette = AppPalette(
    ink = Color(0xFFFFFFFF),
    inkSurface = Color(0xFFF3F1EE),
    inkRaised = Color(0xFFE7E3DE),
    textPrimary = Color(0xFF111111),
    textSecondary = Color(0xFF5F5F5F),
    tapeAmber = Color(0xFFC4541A),
    hairline = Color.Black.copy(alpha = 0.10f),
    outline = Color.Black.copy(alpha = 0.25f),
    topGlow = listOf(Color(0xFFF8DCC8), Color(0xFFFCEFE6), Color(0xFFFFFFFF)),
    playerGlow = listOf(Color(0xFFF3D2BC), Color(0xFFFAEDE4), Color(0xFFFFFFFF)),
)

// Theme-dependent tokens. They read AppTheme's snapshot state, so composables and
// Canvas draw blocks that use them update automatically when the theme changes.
val Ink: Color get() = AppTheme.palette.ink
val InkSurface: Color get() = AppTheme.palette.inkSurface
val InkRaised: Color get() = AppTheme.palette.inkRaised
val TextPrimary: Color get() = AppTheme.palette.textPrimary
val TextSecondary: Color get() = AppTheme.palette.textSecondary
val TapeAmber: Color get() = AppTheme.palette.tapeAmber
val Hairline: Color get() = AppTheme.palette.hairline
val OutlineColor: Color get() = AppTheme.palette.outline

/** Glow at the top of Home and collection pages. */
val TopGlow: Brush get() = Brush.verticalGradient(AppTheme.palette.topGlow)

/** Player backdrop: base colour with a faint warm tape-coloured glow at the top. */
val PlayerGlow: Brush
    get() = AppTheme.palette.playerGlow.let {
        Brush.verticalGradient(0f to it[0], 0.55f to it[1], 1f to it[2])
    }

// Fixed colours (physical objects and accents look the same in both themes)
val TapeOrange = Color(0xFFD9621C)
val TapeBrown = Color(0xFF3B2618)
val LabelCream = Color(0xFFECE7DD)
