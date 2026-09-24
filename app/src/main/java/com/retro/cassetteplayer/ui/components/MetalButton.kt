package com.retro.cassetteplayer.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.retro.cassetteplayer.ui.theme.MetalDark
import com.retro.cassetteplayer.ui.theme.MetalHighlight
import com.retro.cassetteplayer.ui.theme.MetalLight
import com.retro.cassetteplayer.ui.theme.MetalMid
import com.retro.cassetteplayer.ui.theme.MetalShadow
import com.retro.cassetteplayer.ui.theme.RetroOrange

/**
 * A skeuomorphic, brushed-aluminium key. It sinks when pressed and can stay latched
 * down ([latched]) like the PLAY key of a cassette deck.
 */
@Composable
fun MetalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp),
    enabled: Boolean = true,
    latched: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val down = pressed || latched
    val elevation by animateDpAsState(if (down) 1.dp else 6.dp, label = "keyElevation")
    val travel by animateDpAsState(if (down) 2.dp else 0.dp, label = "keyTravel")

    val face = if (down) {
        Brush.verticalGradient(listOf(MetalMid, MetalLight, MetalMid))
    } else {
        Brush.verticalGradient(listOf(MetalHighlight, MetalLight, MetalMid))
    }
    val rim = Brush.verticalGradient(
        if (down) listOf(MetalDark, Color.White.copy(alpha = 0.6f))
        else listOf(Color.White.copy(alpha = 0.9f), MetalDark)
    )

    Box(
        modifier = modifier
            .offset(y = travel)
            .shadow(elevation, shape, ambientColor = MetalShadow, spotColor = MetalShadow)
            .clip(shape)
            .background(face)
            .drawBehind {
                // Fine horizontal brushing on the key face.
                var y = 2f
                var i = 0
                while (y < size.height) {
                    drawLine(
                        color = (if (i % 3 == 0) Color.White else Color.Black).copy(alpha = 0.05f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f,
                    )
                    y += 3f
                    i++
                }
            }
            .border(1.dp, rim, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (enabled) Color(0xFF262626) else MetalDark,
        ) {
            content()
        }
    }
}

@Composable
fun MetalIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 26.dp,
    shape: Shape = RoundedCornerShape(10.dp),
    latched: Boolean = false,
    accent: Boolean = false,
) {
    MetalButton(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        latched = latched,
        contentPadding = PaddingValues(0.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = if (accent) RetroOrange else LocalContentColor.current,
        )
    }
}
