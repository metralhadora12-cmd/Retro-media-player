package com.retro.cassetteplayer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.cassetteplayer.ui.theme.DisplayFont
import com.retro.cassetteplayer.ui.theme.TapeOrange
import com.retro.cassetteplayer.ui.theme.TextSecondary
import androidx.compose.ui.res.stringResource
import com.retro.cassetteplayer.R
import com.retro.cassetteplayer.ui.theme.TextPrimary

private val TickColor: Color get() = TextSecondary.copy(alpha = 0.5f)

private const val SCALE_MARKS = 10
/**
 * Tape-counter style seek bar: a 0–9 graduated scale above a dark groove with an orange
 * slider block. Tap or drag to seek; while dragging the counter previews the target time.
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
    val textMeasurer = rememberTextMeasurer()
    val numberStyle = TextStyle(
        color = TextSecondary,
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
    )

    val progressDescription = stringResource(R.string.player_progress)
    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .semantics { contentDescription = progressDescription }
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
            val inset = 8.dp.toPx()
            val trackWidth = size.width - inset * 2
            val grooveHeight = 6.dp.toPx()
            val grooveTop = size.height - grooveHeight - 2.dp.toPx()
            val scaleY = 7.dp.toPx()

            // Scale: numbers 0..9 with minor ticks between them
            val step = trackWidth / SCALE_MARKS
            for (i in 0 until SCALE_MARKS) {
                val cx = inset + step * (i + 0.5f)
                val layout = textMeasurer.measure(i.toString(), numberStyle)
                drawText(layout, topLeft = Offset(cx - layout.size.width / 2f, scaleY - layout.size.height / 2f))
                for (t in listOf(-0.35f, -0.2f, 0.2f, 0.35f)) {
                    val tx = cx + step * t
                    drawLine(TickColor, Offset(tx, scaleY - 4.dp.toPx()), Offset(tx, scaleY + 4.dp.toPx()), 1.dp.toPx())
                }
                drawLine(TickColor, Offset(inset + step * i, scaleY - 5.dp.toPx()), Offset(inset + step * i, scaleY + 5.dp.toPx()), 1.dp.toPx())
            }

            // Groove
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF151515), Color(0xFF2A2A2A))),
                topLeft = Offset(inset, grooveTop),
                size = Size(trackWidth, grooveHeight),
                cornerRadius = CornerRadius(grooveHeight),
            )
            // Played part, slightly lighter
            drawRoundRect(
                color = TextPrimary.copy(alpha = 0.25f),
                topLeft = Offset(inset, grooveTop),
                size = Size(trackWidth * fraction, grooveHeight),
                cornerRadius = CornerRadius(grooveHeight),
            )
            // Orange slider block
            val blockWidth = 8.dp.toPx()
            val blockX = (inset + trackWidth * fraction - blockWidth / 2f)
                .coerceIn(inset, inset + trackWidth - blockWidth)
            drawRoundRect(TapeOrange, Offset(blockX, grooveTop - 4.dp.toPx()), Size(blockWidth, grooveHeight + 8.dp.toPx()), CornerRadius(2.dp.toPx()))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatTime(shownPosition), style = MaterialTheme.typography.labelLarge.copy(fontFamily = DisplayFont), color = TextSecondary)
            Text(formatTime(durationMs), style = MaterialTheme.typography.labelLarge.copy(fontFamily = DisplayFont), color = TextSecondary)
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
