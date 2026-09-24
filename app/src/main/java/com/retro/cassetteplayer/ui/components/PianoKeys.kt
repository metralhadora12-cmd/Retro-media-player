package com.retro.cassetteplayer.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.retro.cassetteplayer.ui.theme.AluInk
import com.retro.cassetteplayer.ui.theme.KeyGrey
import com.retro.cassetteplayer.ui.theme.KeyShadow
import com.retro.cassetteplayer.ui.theme.KeyWhite

data class PianoKey(
    val icon: ImageVector,
    val description: String,
    val onClick: () -> Unit,
    val latched: Boolean = false,
    val weight: Float = 1f,
)

/**
 * A block of joined cassette-deck keys. Each key has its symbol printed on the metal
 * above it, sinks when pressed and can stay latched down (PLAY while playing).
 */
@Composable
fun PianoKeys(keys: List<PianoKey>, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        keys.forEachIndexed { index, key ->
            val shape = RoundedCornerShape(
                topStart = if (index == 0) 6.dp else 0.dp,
                bottomStart = if (index == 0) 6.dp else 0.dp,
                topEnd = if (index == keys.lastIndex) 6.dp else 0.dp,
                bottomEnd = if (index == keys.lastIndex) 6.dp else 0.dp,
            )
            Column(
                modifier = Modifier.weight(key.weight),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(key.icon, contentDescription = null, tint = AluInk, modifier = Modifier.size(16.dp))
                Spacer(Modifier.height(8.dp))
                Key(key, shape)
            }
            if (index != keys.lastIndex) Spacer(Modifier.width(2.dp))
        }
    }
}

@Composable
private fun Key(key: PianoKey, shape: RoundedCornerShape) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val down = pressed || key.latched
    val travel by animateDpAsState(if (down) 4.dp else 0.dp, label = "keyTravel")

    Box(
        Modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {
        // Side face of the key: 6dp shows when up, 2dp when pressed down
        Box(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .offset(y = 6.dp)
                .shadow(6.dp, shape)
                .clip(shape)
                .background(KeyShadow)
        )
        // Top face
        Box(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .offset(y = travel)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        if (down) listOf(KeyGrey, KeyWhite) else listOf(KeyWhite, KeyGrey)
                    )
                )
                .drawBehind {
                    // Ribbed grip texture across the key
                    val gripTop = size.height * 0.2f
                    val gripBottom = size.height * 0.8f
                    var gy = gripTop
                    while (gy < gripBottom) {
                        drawLine(
                            Color.Black.copy(alpha = 0.08f),
                            Offset(size.width * 0.12f, gy),
                            Offset(size.width * 0.88f, gy),
                            1.5f,
                        )
                        gy += 5f
                    }
                    drawLine(Color.White, Offset(0f, 1f), Offset(size.width, 1f), 2f)
                }
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClick = key.onClick,
                )
                .semantics { contentDescription = key.description },
        )
    }
}
