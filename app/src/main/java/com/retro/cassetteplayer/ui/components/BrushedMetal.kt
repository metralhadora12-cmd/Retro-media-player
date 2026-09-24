package com.retro.cassetteplayer.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.retro.cassetteplayer.ui.theme.AluDark
import com.retro.cassetteplayer.ui.theme.AluLight
import com.retro.cassetteplayer.ui.theme.AluMid
import kotlin.random.Random

private class Grain(
    val pos: Float,
    val start: Float,
    val length: Float,
    val alpha: Float,
    val bright: Boolean,
)

/**
 * Brushed-aluminium texture: a soft gradient plus many thin, randomly placed vertical
 * "brush" strokes and a diagonal sheen. The seed keeps it stable across recompositions.
 */
fun Modifier.brushedAluminium(
    light: Color = AluLight,
    mid: Color = AluMid,
    dark: Color = AluDark,
    grainCount: Int = 700,
    seed: Int = 3,
): Modifier = drawWithCache {
    val random = Random(seed)
    val grains = List(grainCount) {
        Grain(
            pos = random.nextFloat(),
            start = random.nextFloat() * 0.6f - 0.1f,
            length = 0.2f + random.nextFloat() * 0.7f,
            alpha = 0.03f + random.nextFloat() * 0.08f,
            bright = random.nextBoolean(),
        )
    }
    val body = Brush.verticalGradient(0f to light, 0.5f to mid, 1f to dark)
    val sheen = Brush.linearGradient(
        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.18f), Color.Transparent),
        start = Offset(0f, size.height * 0.2f),
        end = Offset(size.width, size.height * 0.6f),
    )
    onDrawBehind {
        drawRect(body)
        grains.forEach { g ->
            val x = g.pos * size.width
            drawLine(
                color = (if (g.bright) Color.White else Color.Black).copy(alpha = g.alpha),
                start = Offset(x, g.start * size.height),
                end = Offset(x, (g.start + g.length) * size.height),
                strokeWidth = 1f,
            )
        }
        drawRect(sheen)
    }
}
