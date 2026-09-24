package com.retro.cassetteplayer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.cassetteplayer.ui.theme.Cream
import com.retro.cassetteplayer.ui.theme.DisplayFont
import com.retro.cassetteplayer.ui.theme.LcdBackground
import com.retro.cassetteplayer.ui.theme.MetalDark
import com.retro.cassetteplayer.ui.theme.RetroAmber
import com.retro.cassetteplayer.ui.theme.RetroOrange

private const val TICKS = 60

/**
 * Seek bar drawn as a graduated ruler with an orange needle. Tap or drag to seek;
 * while dragging the counter previews the target time.
 */
@Composable
fun RetroSeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val fraction = dragFraction
        ?: if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val shownPosition = dragFraction?.let { (it * durationMs).toLong() } ?: positionMs

    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .semantics { contentDescription = "Barra de progresso" }
                .pointerInput(durationMs) {
                    detectTapGestures { offset ->
                        if (durationMs > 0) {
                            onSeek(((offset.x / size.width).coerceIn(0f, 1f) * durationMs).toLong())
                        }
                    }
                }
                .pointerInput(durationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            if (durationMs > 0) dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            dragFraction?.let { onSeek((it * durationMs).toLong()) }
                            dragFraction = null
                        },
                        onDragCancel = { dragFraction = null },
                        onHorizontalDrag = { change, _ ->
                            if (dragFraction != null) {
                                change.consume()
                                dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            }
                        },
                    )
                }
        ) {
            val inset = 6.dp.toPx()
            val trackWidth = size.width - inset * 2
            val grooveTop = size.height * 0.62f
            val grooveHeight = size.height * 0.14f
            val needleX = inset + trackWidth * fraction

            // Recessed groove
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.55f),
                topLeft = Offset(inset, grooveTop),
                size = Size(trackWidth, grooveHeight),
                cornerRadius = CornerRadius(grooveHeight / 2),
            )
            // Played portion
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(RetroOrange, RetroAmber)),
                topLeft = Offset(inset, grooveTop),
                size = Size(trackWidth * fraction, grooveHeight),
                cornerRadius = CornerRadius(grooveHeight / 2),
            )

            // Graduations
            for (i in 0..TICKS) {
                val x = inset + trackWidth * i / TICKS
                val tickHeight = when {
                    i % 10 == 0 -> size.height * 0.34f
                    i % 5 == 0 -> size.height * 0.24f
                    else -> size.height * 0.13f
                }
                val passed = x <= needleX
                drawLine(
                    color = if (passed) RetroAmber else Cream.copy(alpha = 0.55f),
                    start = Offset(x, grooveTop - 3.dp.toPx()),
                    end = Offset(x, grooveTop - 3.dp.toPx() - tickHeight),
                    strokeWidth = if (i % 10 == 0) 2.dp.toPx() else 1.dp.toPx(),
                )
            }

            // Needle with glow and a pointer on top
            drawLine(
                color = RetroOrange.copy(alpha = 0.3f),
                start = Offset(needleX, 0f),
                end = Offset(needleX, size.height),
                strokeWidth = 8.dp.toPx(),
            )
            drawLine(
                color = RetroOrange,
                start = Offset(needleX, 4.dp.toPx()),
                end = Offset(needleX, size.height),
                strokeWidth = 2.5.dp.toPx(),
            )
            val pointer = Path().apply {
                val half = 6.dp.toPx()
                moveTo(needleX - half, 0f)
                lineTo(needleX + half, 0f)
                lineTo(needleX, 9.dp.toPx())
                close()
            }
            drawPath(pointer, RetroOrange)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LcdCounter(formatTime(shownPosition))
            LcdCounter(formatTime(durationMs))
        }
    }
}

/** Small amber-on-black "LCD" digit window. */
@Composable
fun LcdCounter(text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    Text(
        text = text,
        modifier = modifier
            .background(LcdBackground, shape)
            .border(1.dp, MetalDark, shape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelLarge.copy(
            fontFamily = DisplayFont,
            fontSize = 15.sp,
            letterSpacing = 2.sp,
        ),
        color = RetroAmber,
    )
}

fun formatTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
