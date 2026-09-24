package com.retro.cassetteplayer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.retro.cassetteplayer.ui.theme.Cream
import com.retro.cassetteplayer.ui.theme.DisplayFont
import com.retro.cassetteplayer.ui.theme.MetalDark
import com.retro.cassetteplayer.ui.theme.MetalLight
import com.retro.cassetteplayer.ui.theme.RetroAmber
import com.retro.cassetteplayer.ui.theme.RetroOrange
import com.retro.cassetteplayer.ui.theme.TapeBrown

private const val CASSETTE_ASPECT = 1.6f
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
 * A horizontal compact cassette drawn on a Canvas. The hubs spin with playback and the
 * tape packs move from the left reel to the right one following [progress].
 */
@Composable
fun CassetteTape(
    isPlaying: Boolean,
    progress: Float,
    modifier: Modifier = Modifier,
    label: String = "",
    side: String = "A",
) {
    val rotation = rememberReelRotation(isPlaying)
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier.aspectRatio(CASSETTE_ASPECT)) {
        drawCassette(
            angle = rotation.value,
            progress = progress.coerceIn(0f, 1f),
            label = label,
            side = side,
            textMeasurer = textMeasurer,
        )
    }
}

/** Transparent acrylic case with a glass rim and a diagonal glare. */
@Composable
fun AcrylicCase(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .shadow(18.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.05f),
                        Color.White.copy(alpha = 0.12f),
                    )
                )
            )
            .border(
                1.5.dp,
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.08f))
                ),
                shape,
            )
            .drawWithContent {
                drawContent()
                val glare = Path().apply {
                    moveTo(size.width * 0.55f, 0f)
                    lineTo(size.width * 0.75f, 0f)
                    lineTo(size.width * 0.35f, size.height)
                    lineTo(size.width * 0.15f, size.height)
                    close()
                }
                drawPath(glare, Color.White.copy(alpha = 0.06f))
            }
            .padding(18.dp),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

private fun DrawScope.drawCassette(
    angle: Float,
    progress: Float,
    label: String,
    side: String,
    textMeasurer: TextMeasurer,
) {
    val w = size.width
    val h = size.height

    // --- Shell ---------------------------------------------------------------------
    val shellCorner = CornerRadius(w * 0.04f)
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF3E3E3E), Color(0xFF232323))),
        cornerRadius = shellCorner,
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.7f),
        cornerRadius = shellCorner,
        style = Stroke(w * 0.006f),
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.07f),
        topLeft = Offset(w * 0.015f, h * 0.022f),
        size = Size(w * 0.97f, h * 0.956f),
        cornerRadius = CornerRadius(w * 0.032f),
        style = Stroke(w * 0.004f),
    )

    listOf(
        Offset(w * 0.035f, h * 0.06f),
        Offset(w * 0.965f, h * 0.06f),
        Offset(w * 0.035f, h * 0.94f),
        Offset(w * 0.965f, h * 0.94f),
        Offset(w * 0.5f, h * 0.9f),
    ).forEach { drawScrew(it, w * 0.013f) }

    // --- Label -----------------------------------------------------------------------
    val labelTopLeft = Offset(w * 0.07f, h * 0.07f)
    val labelSize = Size(w * 0.86f, h * 0.62f)
    drawRoundRect(Cream, labelTopLeft, labelSize, CornerRadius(w * 0.018f))
    drawRect(RetroOrange, Offset(labelTopLeft.x, h * 0.52f), Size(labelSize.width, h * 0.06f))
    drawRect(RetroAmber, Offset(labelTopLeft.x, h * 0.595f), Size(labelSize.width, h * 0.03f))

    // Handwriting line + side badge
    drawLine(
        color = TapeBrown.copy(alpha = 0.35f),
        start = Offset(w * 0.18f, h * 0.225f),
        end = Offset(w * 0.9f, h * 0.225f),
        strokeWidth = w * 0.003f,
    )
    val badgeTopLeft = Offset(w * 0.095f, h * 0.1f)
    val badgeSize = Size(w * 0.065f, h * 0.11f)
    drawRoundRect(RetroOrange, badgeTopLeft, badgeSize, CornerRadius(w * 0.008f))

    val sideLayout = textMeasurer.measure(
        side,
        TextStyle(
            color = Cream,
            fontFamily = DisplayFont,
            fontWeight = FontWeight.Bold,
            fontSize = (h * 0.085f).toSp(),
        ),
    )
    drawText(
        sideLayout,
        topLeft = Offset(
            badgeTopLeft.x + (badgeSize.width - sideLayout.size.width) / 2f,
            badgeTopLeft.y + (badgeSize.height - sideLayout.size.height) / 2f,
        ),
    )

    if (label.isNotBlank()) {
        drawText(
            textMeasurer = textMeasurer,
            text = label,
            topLeft = Offset(w * 0.19f, h * 0.11f),
            style = TextStyle(
                color = Color(0xFF2B2016),
                fontFamily = DisplayFont,
                fontWeight = FontWeight.Bold,
                fontSize = (h * 0.075f).toSp(),
            ),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            size = Size(w * 0.7f, h * 0.11f),
        )
    }
    drawText(
        textMeasurer = textMeasurer,
        text = "C-60  STEREO",
        topLeft = Offset(w * 0.64f, h * 0.525f),
        style = TextStyle(
            color = Cream,
            fontFamily = DisplayFont,
            fontWeight = FontWeight.Bold,
            fontSize = (h * 0.038f).toSp(),
        ),
        maxLines = 1,
    )

    // --- Window with the tape packs ---------------------------------------------------
    val windowRect = Rect(Offset(w * 0.22f, h * 0.29f), Size(w * 0.56f, h * 0.2f))
    val windowPath = Path().apply {
        addRoundRect(RoundRect(windowRect, CornerRadius(windowRect.height / 2f)))
    }
    drawPath(windowPath, Color(0xFF120E0B))

    val leftHub = Offset(w * 0.32f, windowRect.center.y)
    val rightHub = Offset(w * 0.68f, windowRect.center.y)
    val minPack = w * 0.06f
    val maxPack = w * 0.16f
    // Tape winds from the supply reel (left) onto the take-up reel (right).
    val leftPack = maxPack + (minPack - maxPack) * progress
    val rightPack = minPack + (maxPack - minPack) * progress

    clipPath(windowPath) {
        drawTapePack(leftHub, leftPack)
        drawTapePack(rightHub, rightPack)
        // Glass reflection on the window
        drawRect(
            color = Color.White.copy(alpha = 0.06f),
            topLeft = windowRect.topLeft,
            size = Size(windowRect.width, windowRect.height * 0.35f),
        )
    }
    drawPath(windowPath, Color.White.copy(alpha = 0.18f), style = Stroke(w * 0.004f))

    // Hubs are visible through holes in the label and spin with playback.
    val hubRadius = w * 0.045f
    drawReelHub(leftHub, hubRadius, angle)
    drawReelHub(rightHub, hubRadius, angle)

    // --- Bottom head guide (trapezoid) -------------------------------------------------
    val trapezoid = Path().apply {
        moveTo(w * 0.2f, h)
        lineTo(w * 0.25f, h * 0.76f)
        lineTo(w * 0.75f, h * 0.76f)
        lineTo(w * 0.8f, h)
        close()
    }
    drawPath(trapezoid, Brush.verticalGradient(listOf(Color(0xFF383838), Color(0xFF2A2A2A))))
    drawPath(trapezoid, Color.Black.copy(alpha = 0.6f), style = Stroke(w * 0.004f))
    listOf(0.31f, 0.43f, 0.57f, 0.69f).forEachIndexed { i, x ->
        val r = if (i == 1 || i == 2) w * 0.018f else w * 0.012f
        drawCircle(Color(0xFF0E0E0E), r, Offset(w * x, h * 0.87f))
    }
}

private fun DrawScope.drawTapePack(center: Offset, radius: Float) {
    drawCircle(TapeBrown, radius, center)
    var r = radius * 0.92f
    while (r > radius * 0.4f) {
        drawCircle(Color.White.copy(alpha = 0.035f), r, center, style = Stroke(1f))
        r -= radius * 0.12f
    }
}

private fun DrawScope.drawReelHub(center: Offset, radius: Float, angle: Float) {
    drawCircle(Color(0xFF0E0E0E), radius * 1.18f, center)
    drawCircle(Cream, radius, center)
    drawCircle(Color(0xFF8A7F6C), radius, center, style = Stroke(radius * 0.1f))
    drawCircle(Color(0xFF1A1A1A), radius * 0.56f, center)
    rotate(angle, pivot = center) {
        repeat(6) { i ->
            rotate(i * 60f, pivot = center) {
                drawRect(
                    color = Cream,
                    topLeft = Offset(center.x - radius * 0.09f, center.y - radius * 0.57f),
                    size = Size(radius * 0.18f, radius * 0.26f),
                )
            }
        }
        // Orange dot makes the rotation easy to see.
        drawCircle(RetroOrange, radius * 0.1f, Offset(center.x, center.y - radius * 0.78f))
    }
}

private fun DrawScope.drawScrew(center: Offset, radius: Float) {
    drawCircle(
        Brush.radialGradient(listOf(MetalLight, MetalDark), center = center, radius = radius),
        radius,
        center,
    )
    drawCircle(Color.Black.copy(alpha = 0.6f), radius, center, style = Stroke(radius * 0.18f))
    val arm = radius * 0.6f
    drawLine(Color(0xFF2A2A2A), Offset(center.x - arm, center.y), Offset(center.x + arm, center.y), radius * 0.22f)
    drawLine(Color(0xFF2A2A2A), Offset(center.x, center.y - arm), Offset(center.x, center.y + arm), radius * 0.22f)
}
