package com.retro.cassetteplayer.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.retro.cassetteplayer.ui.theme.Charcoal
import com.retro.cassetteplayer.ui.theme.Graphite
import com.retro.cassetteplayer.ui.theme.SteelDark
import com.retro.cassetteplayer.ui.theme.SteelLight
import com.retro.cassetteplayer.ui.theme.SteelMid
import kotlin.random.Random

private class Grain(
    val y: Float,
    val start: Float,
    val length: Float,
    val alpha: Float,
    val bright: Boolean,
)

/**
 * Paints a brushed-metal texture: a banded gradient plus many thin, randomly placed
 * horizontal "brush" strokes and a soft diagonal sheen. The seed keeps it stable.
 */
fun Modifier.brushedMetal(
    highlight: Color,
    base: Color,
    shadow: Color,
    grainCount: Int = 260,
    seed: Int = 7,
): Modifier = drawWithCache {
    val random = Random(seed)
    val grains = List(grainCount) {
        Grain(
            y = random.nextFloat(),
            start = random.nextFloat() * 0.5f - 0.1f,
            length = 0.4f + random.nextFloat() * 0.8f,
            alpha = 0.02f + random.nextFloat() * 0.07f,
            bright = random.nextBoolean(),
        )
    }
    val body = Brush.verticalGradient(
        0f to highlight,
        0.35f to base,
        0.7f to shadow,
        1f to base,
    )
    val sheen = Brush.linearGradient(
        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.07f), Color.Transparent),
        start = Offset.Zero,
        end = Offset(size.width, size.height),
    )
    onDrawBehind {
        drawRect(body)
        grains.forEach { g ->
            val y = g.y * size.height
            drawLine(
                color = (if (g.bright) Color.White else Color.Black).copy(alpha = g.alpha),
                start = Offset(g.start * size.width, y),
                end = Offset((g.start + g.length) * size.width, y),
                strokeWidth = 1f,
            )
        }
        drawRect(sheen)
    }
}

/** Dark matte body (#1E1E1E / #2B2B2B) used for lists and bars. */
fun Modifier.darkBrushedMetal(): Modifier =
    brushedMetal(highlight = Graphite, base = Color(0xFF242424), shadow = Charcoal, seed = 11)

/** Brushed aluminium / steel backdrop used by the player view and the header card. */
fun Modifier.steelBrushedMetal(): Modifier =
    brushedMetal(highlight = SteelLight, base = SteelMid, shadow = SteelDark, grainCount = 420, seed = 3)
