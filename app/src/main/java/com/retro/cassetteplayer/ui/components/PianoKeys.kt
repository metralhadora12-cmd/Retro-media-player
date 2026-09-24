package com.retro.cassetteplayer.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.retro.cassetteplayer.ui.theme.TapeOrange
import com.retro.cassetteplayer.ui.theme.TextPrimary

data class PianoKey(
    val icon: ImageVector,
    val description: String,
    val onClick: () -> Unit,
    val latched: Boolean = false,
    val weight: Float = 1f,
)

private val KeyTop = Color(0xFF2E2E2E)
private val KeyBottom = Color(0xFF222222)
private val KeySide = Color(0xFF121212)

/**
 * A block of joined cassette-deck keys in dark graphite. Each key sinks when pressed and
 * can stay latched down (PLAY while playing), lighting a small orange LED strip.
 */
@Composable
fun PianoKeys(keys: List<PianoKey>, modifier: Modifier = Modifier) {
    Row(modifier) {
        keys.forEachIndexed { index, key ->
            val shape = RoundedCornerShape(
                topStart = if (index == 0) 10.dp else 2.dp,
                bottomStart = if (index == 0) 10.dp else 2.dp,
                topEnd = if (index == keys.lastIndex) 10.dp else 2.dp,
                bottomEnd = if (index == keys.lastIndex) 10.dp else 2.dp,
            )
            Key(key, shape, Modifier.weight(key.weight))
            if (index != keys.lastIndex) Spacer(Modifier.width(3.dp))
        }
    }
}

@Composable
private fun Key(key: PianoKey, shape: RoundedCornerShape, modifier: Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val down = pressed || key.latched
    val travel by animateDpAsState(if (down) 3.dp else 0.dp, label = "keyTravel")

    Box(modifier.height(62.dp)) {
        // Side face: 5dp of depth when up, 2dp when pressed down
        Box(
            Modifier
                .fillMaxWidth()
                .height(57.dp)
                .offset(y = 5.dp)
                .clip(shape)
                .background(KeySide)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(57.dp)
                .offset(y = travel)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        if (down) listOf(KeyBottom, KeyTop) else listOf(KeyTop, KeyBottom)
                    )
                )
                .drawBehind {
                    // Hairline highlight on the top edge + fine ribbing near the bottom
                    drawLine(Color.White.copy(alpha = 0.12f), Offset(0f, 1f), Offset(size.width, 1f), 2f)
                    var gy = size.height * 0.78f
                    while (gy < size.height * 0.92f) {
                        drawLine(
                            Color.White.copy(alpha = 0.05f),
                            Offset(size.width * 0.2f, gy),
                            Offset(size.width * 0.8f, gy),
                            1.5f,
                        )
                        gy += 4f
                    }
                    if (key.latched) {
                        drawRect(
                            TapeOrange,
                            topLeft = Offset(size.width * 0.35f, size.height - 5.dp.toPx()),
                            size = Size(size.width * 0.3f, 2.dp.toPx()),
                        )
                    }
                }
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClick = key.onClick,
                )
                .semantics { contentDescription = key.description },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                key.icon,
                contentDescription = null,
                tint = if (key.latched) TapeOrange else TextPrimary,
                modifier = Modifier.size(if (key.weight > 1f) 30.dp else 26.dp),
            )
        }
    }
}
