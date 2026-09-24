package com.retro.cassetteplayer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.cassetteplayer.ui.theme.DisplayFont
import com.retro.cassetteplayer.ui.theme.HotlineAmber
import com.retro.cassetteplayer.ui.theme.HotlineOrange
import com.retro.cassetteplayer.ui.theme.Silver
import com.retro.cassetteplayer.ui.theme.SilverDark
import com.retro.cassetteplayer.ui.theme.SilverLight
import com.retro.cassetteplayer.ui.theme.SmokedGlass
import com.retro.cassetteplayer.ui.theme.TapeBrown
import com.retro.cassetteplayer.ui.theme.TextPrimary
import com.retro.cassetteplayer.ui.theme.WalkmanBlueDark
import com.retro.cassetteplayer.ui.theme.WalkmanBlueLight

private const val DECK_ASPECT = 1.5f
private const val REEL_TURN_MS = 1_800

/**
 * Keeps turning while [isPlaying] is true and holds its angle when paused,
 * so the reels stop exactly where they were.
 */
@Composable
fun rememberReelRotation(isPlaying: Boolean): State<Float> {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = tween(REEL_TURN_MS, easing = LinearEasing),
            )
            rotation.snapTo(rotation.value % 360f)
        }
    }
    return rotation.asState()
}

/**
 * A portable cassette player seen from the front: brushed blue body, silver top band
 * with the headphone jack and orange hotline key, and a smoked window where the tape
 * reels spin while playing. The tape winds from left to right following [progress].
 */
@Composable
fun WalkmanDeck(
    isPlaying: Boolean,
    progress: Float,
    modifier: Modifier = Modifier,
    label: String = "",
) {
    val rotation = rememberReelRotation(isPlaying)
    val textMeasurer = rememberTextMeasurer()
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .aspectRatio(DECK_ASPECT)
            .shadow(20.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .blueBrushedMetal(),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawDeck(
                angle = rotation.value,
                progress = progress.coerceIn(0f, 1f),
                isPlaying = isPlaying,
                label = label,
                textMeasurer = textMeasurer,
            )
        }
    }
}

private fun DrawScope.drawDeck(
    angle: Float,
    progress: Float,
    isPlaying: Boolean,
    label: String,
    textMeasurer: TextMeasurer,
) {
    val w = size.width
    val h = size.height
    val bandHeight = h * 0.16f

    // --- Body edges: bevel highlight and a darker lower rim -----------------------
    drawRect(
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.10f),
            0.3f to Color.Transparent,
            0.85f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.35f),
        )
    )

    // --- Silver band along the top ------------------------------------------------
    drawRect(
        brush = Brush.verticalGradient(listOf(SilverLight, Silver, SilverDark)),
        size = Size(w, bandHeight),
    )
    var y = 2f
    while (y < bandHeight) {
        drawLine(Color.Black.copy(alpha = 0.04f), Offset(0f, y), Offset(w, y), 1f)
        y += 3f
    }
    drawLine(Color.Black.copy(alpha = 0.45f), Offset(0f, bandHeight), Offset(w, bandHeight), 2f)
    drawLine(Color.White.copy(alpha = 0.25f), Offset(0f, bandHeight + 2f), Offset(w, bandHeight + 2f), 1f)

    // Headphone jack
    val jack = Offset(w * 0.06f, bandHeight / 2f)
    drawCircle(SilverDark, bandHeight * 0.30f, jack)
    drawCircle(Color(0xFF111111), bandHeight * 0.20f, jack)

    // Orange hotline key
    val keyTopLeft = Offset(w * 0.12f, bandHeight * 0.22f)
    val keySize = Size(w * 0.075f, bandHeight * 0.56f)
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(HotlineAmber, HotlineOrange)),
        topLeft = keyTopLeft,
        size = keySize,
        cornerRadius = CornerRadius(bandHeight * 0.12f),
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.35f),
        topLeft = keyTopLeft,
        size = keySize,
        cornerRadius = CornerRadius(bandHeight * 0.12f),
        style = Stroke(1.5f),
    )

    // Track title printed on the band, like a label strip
    if (label.isNotBlank()) {
        drawText(
            textMeasurer = textMeasurer,
            text = label.uppercase(),
            topLeft = Offset(w * 0.24f, bandHeight * 0.26f),
            style = TextStyle(
                color = Color(0xFF3A4150),
                fontFamily = DisplayFont,
                fontWeight = FontWeight.Bold,
                fontSize = (bandHeight * 0.36f).toSp(),
            ),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            size = Size(w * 0.6f, bandHeight * 0.6f),
        )
    }

    // Status LED at the end of the band
    val led = Offset(w * 0.93f, bandHeight / 2f)
    if (isPlaying) drawCircle(HotlineOrange.copy(alpha = 0.35f), bandHeight * 0.3f, led)
    drawCircle(if (isPlaying) HotlineOrange else Color(0xFF6B4A36), bandHeight * 0.13f, led)

    // --- Lid seam (left panel) --------------------------------------------------------
    val seamX = w * 0.2f
    drawLine(Color.Black.copy(alpha = 0.4f), Offset(seamX, bandHeight), Offset(seamX, h), 2.5f)
    drawLine(Color.White.copy(alpha = 0.18f), Offset(seamX + 2.5f, bandHeight), Offset(seamX + 2.5f, h), 1f)

    // Vertical lettering: "STEREO" on the left panel, "CASSETTE PLAYER" on the right edge
    drawVerticalText(
        textMeasurer = textMeasurer,
        text = "STEREO",
        center = Offset(w * 0.1f, bandHeight + (h - bandHeight) * 0.5f),
        fontSize = (h * 0.075f).toSp(),
    )
    drawVerticalText(
        textMeasurer = textMeasurer,
        text = "CASSETTE PLAYER",
        center = Offset(w * 0.925f, bandHeight + (h - bandHeight) * 0.55f),
        fontSize = (h * 0.045f).toSp(),
        italic = true,
    )

    // --- Direction arrow ----------------------------------------------------------------
    val arrowY = bandHeight + h * 0.13f
    val arrow = Path().apply {
        val left = w * 0.5f
        val right = w * 0.64f
        val head = h * 0.055f
        moveTo(left, arrowY)
        lineTo(left + head, arrowY - head)
        lineTo(left + head, arrowY - head * 0.45f)
        lineTo(right, arrowY - head * 0.45f)
        lineTo(right, arrowY + head * 0.45f)
        lineTo(left + head, arrowY + head * 0.45f)
        lineTo(left + head, arrowY + head)
        close()
    }
    drawPath(arrow, TextPrimary.copy(alpha = 0.9f), style = Stroke(w * 0.005f, join = StrokeJoin.Round))

    // --- Smoked cassette window ------------------------------------------------------------
    val window = Rect(Offset(w * 0.3f, h * 0.45f), Size(w * 0.54f, h * 0.4f))
    val windowPath = Path().apply {
        addRoundRect(RoundRect(window, CornerRadius(h * 0.05f)))
    }
    // Recessed frame
    drawPath(windowPath, Color.Black.copy(alpha = 0.55f), style = Stroke(w * 0.014f))
    drawPath(windowPath, SmokedGlass)

    val leftHub = Offset(window.left + window.width * 0.27f, window.center.y)
    val rightHub = Offset(window.left + window.width * 0.73f, window.center.y)
    val hubRadius = window.height * 0.2f

    clipPath(windowPath) {
        // Cassette shell seen through the glass
        drawRect(
            color = Color(0xFF232A36),
            topLeft = Offset(window.left, window.top + window.height * 0.12f),
            size = Size(window.width, window.height * 0.76f),
        )
        // Tape packs: supply (left) shrinks, take-up (right) grows
        val minPack = hubRadius * 1.25f
        val maxPack = window.width * 0.3f
        drawTapePack(leftHub, maxPack + (minPack - maxPack) * progress)
        drawTapePack(rightHub, minPack + (maxPack - minPack) * progress)
        // Tape running along the bottom of the shell
        drawLine(
            color = TapeBrown,
            start = Offset(window.left, window.bottom - window.height * 0.14f),
            end = Offset(window.right, window.bottom - window.height * 0.14f),
            strokeWidth = window.height * 0.035f,
        )
        drawReelHub(leftHub, hubRadius, angle)
        drawReelHub(rightHub, hubRadius, angle)
        // Glass glare
        val glare = Path().apply {
            moveTo(window.left + window.width * 0.55f, window.top)
            lineTo(window.left + window.width * 0.72f, window.top)
            lineTo(window.left + window.width * 0.42f, window.bottom)
            lineTo(window.left + window.width * 0.25f, window.bottom)
            close()
        }
        drawPath(glare, Color.White.copy(alpha = 0.07f))
        drawRect(
            color = Color.White.copy(alpha = 0.05f),
            topLeft = window.topLeft,
            size = Size(window.width, window.height * 0.3f),
        )
    }
    drawPath(windowPath, SilverLight.copy(alpha = 0.35f), style = Stroke(w * 0.003f))

    // Small screws in the lower corners of the body
    listOf(
        Offset(w * 0.035f, h * 0.93f),
        Offset(w * 0.965f, h * 0.93f),
    ).forEach { drawScrew(it, h * 0.02f) }
}

private fun DrawScope.drawVerticalText(
    textMeasurer: TextMeasurer,
    text: String,
    center: Offset,
    fontSize: TextUnit,
    italic: Boolean = false,
) {
    val layout = textMeasurer.measure(
        text,
        TextStyle(
            color = TextPrimary.copy(alpha = 0.92f),
            fontFamily = DisplayFont,
            fontWeight = FontWeight.Black,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            fontSize = fontSize,
            letterSpacing = 2.sp,
        ),
    )
    rotate(-90f, pivot = center) {
        drawText(
            layout,
            topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f),
        )
    }
}

private fun DrawScope.drawTapePack(center: Offset, radius: Float) {
    drawCircle(TapeBrown, radius, center)
    var r = radius * 0.92f
    while (r > radius * 0.5f) {
        drawCircle(Color.White.copy(alpha = 0.035f), r, center, style = Stroke(1f))
        r -= radius * 0.1f
    }
}

/** White translucent reel ring with a dark hub and six teeth, as seen through the window. */
private fun DrawScope.drawReelHub(center: Offset, radius: Float, angle: Float) {
    drawCircle(Color.White.copy(alpha = 0.78f), radius * 1.35f, center, style = Stroke(radius * 0.45f))
    drawCircle(Color(0xFF0B0F16), radius * 0.95f, center)
    rotate(angle, pivot = center) {
        repeat(6) { i ->
            rotate(i * 60f, pivot = center) {
                drawLine(
                    color = Color.White.copy(alpha = 0.9f),
                    start = Offset(center.x, center.y - radius * 0.9f),
                    end = Offset(center.x, center.y - radius * 0.55f),
                    strokeWidth = radius * 0.16f,
                    cap = StrokeCap.Round,
                )
            }
        }
        // Orange dot makes the rotation easy to see.
        drawCircle(HotlineOrange, radius * 0.1f, Offset(center.x, center.y - radius * 1.35f))
    }
}

private fun DrawScope.drawScrew(center: Offset, radius: Float) {
    drawCircle(
        Brush.radialGradient(listOf(WalkmanBlueLight, WalkmanBlueDark), center = center, radius = radius),
        radius,
        center,
    )
    drawCircle(Color.Black.copy(alpha = 0.5f), radius, center, style = Stroke(radius * 0.2f))
    val arm = radius * 0.6f
    drawLine(
        Color.Black.copy(alpha = 0.6f),
        Offset(center.x - arm, center.y),
        Offset(center.x + arm, center.y),
        radius * 0.25f,
    )
}
