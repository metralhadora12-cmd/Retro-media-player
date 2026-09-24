package com.retro.cassetteplayer.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.retro.cassetteplayer.ui.theme.Navy
import com.retro.cassetteplayer.ui.theme.NavyRaised
import com.retro.cassetteplayer.ui.theme.NavySurface
import com.retro.cassetteplayer.ui.theme.Silver
import com.retro.cassetteplayer.ui.theme.SilverDark
import com.retro.cassetteplayer.ui.theme.SilverLight
import com.retro.cassetteplayer.ui.theme.WalkmanBlue
import com.retro.cassetteplayer.ui.theme.WalkmanBlueDark
import com.retro.cassetteplayer.ui.theme.WalkmanBlueLight
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

/** Deep blue brushed finish used for bars and lists. */
fun Modifier.navyBrushedMetal(): Modifier =
    brushedMetal(highlight = NavyRaised, base = NavySurface, shadow = Navy, seed = 11)

/** Metallic blue body of the player, with a denser brushed grain. */
fun Modifier.blueBrushedMetal(): Modifier =
    brushedMetal(highlight = WalkmanBlueLight, base = WalkmanBlue, shadow = WalkmanBlueDark, grainCount = 520, seed = 3)

/** Silver trim band (top edge of the device). */
fun Modifier.silverBrushedMetal(): Modifier =
    brushedMetal(highlight = SilverLight, base = Silver, shadow = SilverDark, grainCount = 180, seed = 5)
