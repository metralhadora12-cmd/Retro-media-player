package com.retro.cassetteplayer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.retro.cassetteplayer.ui.theme.LabelCream
import com.retro.cassetteplayer.ui.theme.TapeBrown
import com.retro.cassetteplayer.ui.theme.TapeOrange

/** Width / height of the upright cassette. */
const val VERTICAL_CASSETTE_ASPECT = 0.6f
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
 * A cassette standing upright: cream label with
 * the title printed sideways, an orange stripe band, and a dark centre window where the
 * two reels spin. Tape winds from the top reel to the bottom one following [progress].
 */
@Composable
fun VerticalCassette(
    isPlaying: Boolean,
    progress: Float,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val rotation = rememberReelRotation(isPlaying)
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier) {
        drawCassette(
            area = Rect(Offset.Zero, size),
            angle = rotation.value,
            progress = progress.coerceIn(0f, 1f),
            title = title,
            subtitle = subtitle,
            textMeasurer = textMeasurer,
        )
    }
}

private fun DrawScope.drawCassette(
    area: Rect,
    angle: Float,
    progress: Float,
    title: String,
    subtitle: String,
    textMeasurer: TextMeasurer,
) {
    val w = area.width
    val h = area.height
    fun x(f: Float) = area.left + w * f
    fun y(f: Float) = area.top + h * f

    // --- Label -------------------------------------------------------------------
    drawRoundRect(
        brush = Brush.horizontalGradient(listOf(LabelCream, Color(0xFFDDD7CB))),
        topLeft = area.topLeft,
        size = area.size,
        cornerRadius = CornerRadius(w * 0.03f),
    )

    // Orange band on the right with two thin cream pinstripes
    val band = Rect(x(0.56f), area.top, x(0.9f), area.bottom)
    drawRect(TapeOrange, band.topLeft, band.size)
    drawRect(LabelCream.copy(alpha = 0.7f), Offset(band.left + w * 0.035f, area.top), Size(w * 0.01f, h))
    drawRect(LabelCream.copy(alpha = 0.7f), Offset(band.right - w * 0.045f, area.top), Size(w * 0.01f, h))

    // Thin orange rule next to the title
    drawLine(TapeOrange, Offset(x(0.2f), y(0.2f)), Offset(x(0.2f), y(0.72f)), w * 0.012f)

    // Title printed sideways on the cream part, artist sideways on the band
    drawSidewaysText(
        textMeasurer = textMeasurer,
        text = title.ifBlank { "MIX TAPE" }.uppercase(),
        center = Offset(x(0.1f), y(0.5f)),
        maxLength = h * 0.86f,
        fontSize = (w * 0.1f).toSp(),
        color = Color(0xFF26282C),
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
    )
    drawSidewaysText(
        textMeasurer = textMeasurer,
        text = subtitle.ifBlank { "PLAYLIST" }.uppercase(),
        center = Offset(band.center.x + w * 0.005f, y(0.5f)),
        maxLength = h * 0.7f,
        fontSize = (w * 0.055f).toSp(),
        color = LabelCream,
        fontWeight = FontWeight.Bold,
        letterSpacing = 4.sp,
    )

    // --- Dark centre window with the reels ---------------------------------------------
    val window = Rect(x(0.29f), y(0.08f), x(0.67f), y(0.92f))
    val windowPath = Path().apply {
        addRoundRect(RoundRect(window, CornerRadius(window.width / 2f)))
    }
    drawPath(windowPath, Brush.horizontalGradient(listOf(Color(0xFF26272B), Color(0xFF111214), Color(0xFF26272B))))

    val hubRadius = window.width * 0.2f
    val topHub = Offset(window.center.x, window.top + window.width * 0.52f)
    val bottomHub = Offset(window.center.x, window.bottom - window.width * 0.52f)

    clipPath(windowPath) {
        // Tape packs: supply (top) shrinks, take-up (bottom) grows
        val minPack = hubRadius * 1.3f
        val maxPack = window.width * 0.62f
        drawCircle(TapeBrown, maxPack + (minPack - maxPack) * progress, topHub)
        drawCircle(TapeBrown, minPack + (maxPack - minPack) * progress, bottomHub)
        // Small viewing slot in the middle with the tape running through
        val slot = Rect(
            Offset(window.center.x - window.width * 0.17f, window.center.y - h * 0.09f),
            Size(window.width * 0.34f, h * 0.18f),
        )
        drawRect(Color(0xFF0A0A0B), slot.topLeft, slot.size)
        drawLine(
            TapeBrown.copy(alpha = 0.9f),
            Offset(slot.center.x, slot.top),
            Offset(slot.center.x, slot.bottom),
            slot.width * 0.2f,
        )
        drawReelHub(topHub, hubRadius, angle)
        drawReelHub(bottomHub, hubRadius, angle)
    }
    drawPath(windowPath, Color.Black.copy(alpha = 0.5f), style = Stroke(w * 0.012f))

    // --- Glass glare across the cassette ---------------------------------------------------
    val glare = Path().apply {
        moveTo(x(0.42f), area.top)
        lineTo(x(1f), area.top)
        lineTo(x(1f), y(0.2f))
        lineTo(x(0.42f), y(0.5f))
        close()
    }
    drawPath(glare, Color.White.copy(alpha = 0.12f))
}

private fun DrawScope.drawSidewaysText(
    textMeasurer: TextMeasurer,
    text: String,
    center: Offset,
    maxLength: Float,
    fontSize: TextUnit,
    color: Color,
    fontWeight: FontWeight,
    letterSpacing: TextUnit,
) {
    val layout = textMeasurer.measure(
        text = text,
        style = TextStyle(
            color = color,
            fontFamily = FontFamily.SansSerif,
            fontWeight = fontWeight,
            fontSize = fontSize,
            letterSpacing = letterSpacing,
        ),
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        constraints = Constraints(maxWidth = maxLength.toInt().coerceAtLeast(1)),
    )
    // Reads top-to-bottom, like the spine of a tape.
    rotate(90f, pivot = center) {
        drawText(
            layout,
            topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f),
        )
    }
}

/** Dark reel with a three-spoke hub, like the one on the reference cassette. */
private fun DrawScope.drawReelHub(center: Offset, radius: Float, angle: Float) {
    drawCircle(Color(0xFF3B3D42), radius, center)
    drawCircle(Color(0xFF0D0E10), radius * 0.78f, center)
    rotate(angle, pivot = center) {
        repeat(3) { i ->
            rotate(i * 120f, pivot = center) {
                drawLine(
                    color = Color(0xFFBFC2C8),
                    start = center,
                    end = Offset(center.x, center.y - radius * 0.7f),
                    strokeWidth = radius * 0.16f,
                    cap = StrokeCap.Round,
                )
            }
        }
        // Tooth on the rim makes the rotation visible
        drawCircle(TapeOrange, radius * 0.1f, Offset(center.x, center.y - radius * 0.9f))
    }
    drawCircle(Color(0xFF8E9197), radius * 0.16f, center)
    drawCircle(Color.White.copy(alpha = 0.15f), radius, center, style = Stroke(1.5f))
}
