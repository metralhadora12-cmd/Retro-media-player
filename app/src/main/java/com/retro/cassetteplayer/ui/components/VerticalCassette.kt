package com.retro.cassetteplayer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.res.stringResource
import com.retro.cassetteplayer.R
import androidx.compose.ui.text.font.Font
import com.retro.cassetteplayer.data.CassetteModel

/** Width / height of the upright cassette. */
const val VERTICAL_CASSETTE_ASPECT = 0.6f
// Pack radii as fractions of the centre-window width (shared by drawing and physics).
private const val PACK_MIN = 0.26f
private const val PACK_MAX = 0.62f

/** Tape pack radius (fraction of window width) of the supply (top) and take-up (bottom) reels. */
private fun supplyPack(progress: Float) = PACK_MAX + (PACK_MIN - PACK_MAX) * progress
private fun takeUpPack(progress: Float) = PACK_MIN + (PACK_MAX - PACK_MIN) * progress

/**
 * Tape runs past the heads at constant speed, so each reel's angular speed is inversely
 * proportional to how much tape it holds: the emptier reel spins faster. Angles only
 * advance while [isPlaying] and hold still when paused.
 */
private const val TAPE_SPEED = 110f // degrees per second at a pack radius of 1 window width

private class ReelAngles {
    var top by mutableFloatStateOf(0f)
    var bottom by mutableFloatStateOf(0f)
}

@Composable
private fun rememberReelAngles(isPlaying: Boolean, progress: Float): ReelAngles {
    val angles = remember { ReelAngles() }
    val currentProgress by rememberUpdatedState(progress)
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val dt = (now - last) / 1_000_000_000f
                last = now
                val p = currentProgress.coerceIn(0f, 1f)
                angles.top = (angles.top + dt * TAPE_SPEED / supplyPack(p)) % 360f
                angles.bottom = (angles.bottom + dt * TAPE_SPEED / takeUpPack(p)) % 360f
            }
        }
    }
    return angles
}

/** Colours of each cassette type's label. */
private class Skin(
    val label: List<Color>,
    val ink: Color,
    val pinstripe: Color,
    val typeText: String,
)

private fun skinFor(model: CassetteModel) = when (model) {
    CassetteModel.NORMAL -> Skin(listOf(LabelCream, Color(0xFFDDD7CB)), Color(0xFF26282C), LabelCream, "TYPE I · NORMAL")
    CassetteModel.CHROME -> Skin(listOf(Color(0xFFE6EAEF), Color(0xFFAAB2BC)), Color(0xFF1E2A3A), Color(0xFFE6EAEF), "TYPE II · CrO₂ HIGH BIAS")
    CassetteModel.METAL -> Skin(listOf(Color(0xFF2B2C30), Color(0xFF121315)), Color(0xFFD8B56A), Color(0xFFD8B56A), "TYPE IV · METAL")
}

private val MarkerFont = FontFamily(Font(R.font.permanent_marker))

/**
 * A cassette standing upright: printed label with the title sideways, a coloured
 * stripe band, and a dark centre window where the two reels spin. Tape winds from the
 * top reel to the bottom one following [progress]. [model] picks the label style
 * (normal, chrome, metal), [handwritten] writes the title with a marker and [side]
 * prints the side letter.
 */
@Composable
fun VerticalCassette(
    isPlaying: Boolean,
    progress: Float,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    model: CassetteModel = CassetteModel.NORMAL,
    bandColor: Color = TapeOrange,
    handwritten: Boolean = false,
    side: Char? = null,
) {
    val angles = rememberReelAngles(isPlaying, progress)
    val textMeasurer = rememberTextMeasurer()
    val defaultTitle = stringResource(R.string.cassette_default_title)
    val defaultSubtitle = stringResource(R.string.cassette_default_subtitle)
    Canvas(modifier) {
        drawCassette(
            area = Rect(Offset.Zero, size),
            topAngle = angles.top,
            bottomAngle = angles.bottom,
            progress = progress.coerceIn(0f, 1f),
            title = title.ifBlank { defaultTitle },
            subtitle = subtitle.ifBlank { defaultSubtitle },
            textMeasurer = textMeasurer,
            skin = skinFor(model),
            band = bandColor,
            handwritten = handwritten,
            side = side,
        )
    }
}

private fun DrawScope.drawCassette(
    area: Rect,
    topAngle: Float,
    bottomAngle: Float,
    progress: Float,
    title: String,
    subtitle: String,
    textMeasurer: TextMeasurer,
    skin: Skin,
    band: Color,
    handwritten: Boolean,
    side: Char?,
) {
    val w = area.width
    val h = area.height
    fun x(f: Float) = area.left + w * f
    fun y(f: Float) = area.top + h * f

    // --- Label -------------------------------------------------------------------
    drawRoundRect(
        brush = Brush.horizontalGradient(skin.label),
        topLeft = area.topLeft,
        size = area.size,
        cornerRadius = CornerRadius(w * 0.03f),
    )

    // Coloured band on the right with two thin pinstripes
    val bandRect = Rect(x(0.56f), area.top, x(0.9f), area.bottom)
    drawRect(band, bandRect.topLeft, bandRect.size)
    drawRect(skin.pinstripe.copy(alpha = 0.7f), Offset(bandRect.left + w * 0.035f, area.top), Size(w * 0.01f, h))
    drawRect(skin.pinstripe.copy(alpha = 0.7f), Offset(bandRect.right - w * 0.045f, area.top), Size(w * 0.01f, h))

    // Thin rule next to the title
    drawLine(band, Offset(x(0.2f), y(0.2f)), Offset(x(0.2f), y(0.72f)), w * 0.012f)

    // Side letter in the top corner of the label
    if (side != null) {
        val letter = textMeasurer.measure(
            side.toString(),
            TextStyle(color = skin.ink, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = (w * 0.13f).toSp()),
        )
        val center = Offset(x(0.1f), y(0.075f))
        drawCircle(skin.ink, w * 0.085f, center, style = Stroke(w * 0.012f))
        drawText(letter, topLeft = Offset(center.x - letter.size.width / 2f, center.y - letter.size.height / 2f))
    }

    // Title sideways on the label (printed, or written with a marker), artist / tape name on the band
    val titleTop = if (side != null) 0.17f else 0.07f
    drawSidewaysText(
        textMeasurer = textMeasurer,
        text = if (handwritten) title else title.uppercase(),
        center = Offset(x(0.1f), y((titleTop + 0.93f) / 2f)),
        maxLength = h * (0.93f - titleTop),
        fontSize = (w * if (handwritten) 0.115f else 0.1f).toSp(),
        color = skin.ink,
        fontWeight = if (handwritten) FontWeight.Normal else FontWeight.Black,
        letterSpacing = if (handwritten) 0.sp else 1.sp,
        fontFamily = if (handwritten) MarkerFont else FontFamily.SansSerif,
    )
    drawSidewaysText(
        textMeasurer = textMeasurer,
        text = subtitle.uppercase(),
        center = Offset(bandRect.center.x + w * 0.005f, y(0.5f)),
        maxLength = h * 0.7f,
        fontSize = (w * 0.055f).toSp(),
        color = LabelCream,
        fontWeight = FontWeight.Bold,
        letterSpacing = 4.sp,
    )
    // Tape type printed small along the right edge
    drawSidewaysText(
        textMeasurer = textMeasurer,
        text = skin.typeText,
        center = Offset(x(0.95f), y(0.5f)),
        maxLength = h * 0.8f,
        fontSize = (w * 0.035f).toSp(),
        color = skin.ink.copy(alpha = 0.7f),
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
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
        // Tape packs: supply (top) shrinks, take-up (bottom) grows with the song position
        drawTapePack(topHub, window.width * supplyPack(progress), hubRadius)
        drawTapePack(bottomHub, window.width * takeUpPack(progress), hubRadius)
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
        drawReelHub(topHub, hubRadius, topAngle)
        drawReelHub(bottomHub, hubRadius, bottomAngle)
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
    fontFamily: FontFamily = FontFamily.SansSerif,
) {
    val layout = textMeasurer.measure(
        text = text,
        style = TextStyle(
            color = color,
            fontFamily = fontFamily,
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

/**
 * Wound tape seen from above: a drop shadow on the window floor, a radial gradient for
 * the curved edge, faint winding grooves and a fixed specular highlight (light from the
 * top-left), so the pack reads as a solid disc.
 */
private fun DrawScope.drawTapePack(center: Offset, radius: Float, hubRadius: Float) {
    drawCircle(Color.Black.copy(alpha = 0.55f), radius * 1.02f, center + Offset(radius * 0.05f, radius * 0.07f))
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0xFF1C120B),
            (hubRadius / radius).coerceIn(0f, 0.9f) to Color(0xFF2A1A10),
            0.8f to Color(0xFF4A2E1C),
            0.95f to Color(0xFF3A2416),
            1f to Color(0xFF1A100A),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
    var r = radius * 0.94f
    while (r > hubRadius * 1.1f) {
        drawCircle(Color.Black.copy(alpha = 0.12f), r, center, style = Stroke(1f))
        r -= radius * 0.06f
    }
    drawArc(
        brush = Brush.sweepGradient(
            listOf(Color.Transparent, Color.White.copy(alpha = 0.16f), Color.Transparent),
            center = center,
        ),
        startAngle = 190f,
        sweepAngle = 80f,
        useCenter = false,
        topLeft = center - Offset(radius * 0.86f, radius * 0.86f),
        size = Size(radius * 1.72f, radius * 1.72f),
        style = Stroke(radius * 0.08f, cap = StrokeCap.Round),
    )
}

/**
 * Plastic take-up spool with depth: a raised glossy flange, a recessed dark well with
 * teeth, three bevelled spokes and a metal cap. Only the spool itself rotates; lighting
 * (highlights and shadows) stays fixed, which is what sells the 3D look.
 */
private fun DrawScope.drawReelHub(center: Offset, radius: Float, angle: Float) {
    // Shadow cast by the flange
    drawCircle(Color.Black.copy(alpha = 0.5f), radius * 1.02f, center + Offset(radius * 0.08f, radius * 0.12f))
    // Convex flange
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFE4E6EA), Color(0xFFA3A7AE), Color(0xFF5E6269)),
            center = center - Offset(radius * 0.35f, radius * 0.35f),
            radius = radius * 1.6f,
        ),
        radius = radius,
        center = center,
    )
    // Recessed well (darker towards the centre, lit edge at the bottom-right)
    val well = radius * 0.74f
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFF050506), Color(0xFF15161A), Color(0xFF34363C)),
            center = center + Offset(radius * 0.08f, radius * 0.08f),
            radius = well,
        ),
        radius = well,
        center = center,
    )
    drawArc(
        color = Color.Black.copy(alpha = 0.6f),
        startAngle = 180f,
        sweepAngle = 110f,
        useCenter = false,
        topLeft = center - Offset(well, well),
        size = Size(well * 2, well * 2),
        style = Stroke(radius * 0.1f),
    )

    rotate(angle, pivot = center) {
        // Teeth around the inside of the well
        repeat(6) { i ->
            rotate(i * 60f + 30f, pivot = center) {
                drawRoundRect(
                    color = Color(0xFF8B8F96),
                    topLeft = Offset(center.x - radius * 0.07f, center.y - well),
                    size = Size(radius * 0.14f, radius * 0.16f),
                    cornerRadius = CornerRadius(radius * 0.04f),
                )
            }
        }
        // Three bevelled spokes: dark shadow side, then a lighter face
        repeat(3) { i ->
            rotate(i * 120f, pivot = center) {
                drawLine(
                    color = Color.Black.copy(alpha = 0.7f),
                    start = center + Offset(radius * 0.04f, radius * 0.04f),
                    end = Offset(center.x + radius * 0.04f, center.y - well * 0.92f + radius * 0.04f),
                    strokeWidth = radius * 0.2f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFFD8DADF), Color(0xFF8A8E95)),
                        start = center,
                        end = Offset(center.x, center.y - well),
                    ),
                    start = center,
                    end = Offset(center.x, center.y - well * 0.92f),
                    strokeWidth = radius * 0.16f,
                    cap = StrokeCap.Round,
                )
            }
        }
        // Orange marker on the flange makes the rotation easy to follow
        drawCircle(TapeOrange, radius * 0.08f, Offset(center.x, center.y - radius * 0.87f))
    }

    // Metal cap with a specular dot
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFF2F3F5), Color(0xFF9A9EA5), Color(0xFF4A4D52)),
            center = center - Offset(radius * 0.06f, radius * 0.06f),
            radius = radius * 0.24f,
        ),
        radius = radius * 0.2f,
        center = center,
    )
    drawCircle(Color.White.copy(alpha = 0.8f), radius * 0.05f, center - Offset(radius * 0.07f, radius * 0.07f))

    // Fixed glossy highlight on the flange (top-left)
    drawArc(
        color = Color.White.copy(alpha = 0.55f),
        startAngle = 200f,
        sweepAngle = 70f,
        useCenter = false,
        topLeft = center - Offset(radius * 0.87f, radius * 0.87f),
        size = Size(radius * 1.74f, radius * 1.74f),
        style = Stroke(radius * 0.09f, cap = StrokeCap.Round),
    )
}
