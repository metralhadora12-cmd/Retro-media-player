package com.retro.cassetteplayer.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import com.retro.cassetteplayer.ui.theme.InkSurface
import com.retro.cassetteplayer.ui.theme.TextPrimary
import com.retro.cassetteplayer.ui.theme.TextSecondary
import androidx.compose.ui.res.stringResource
import com.retro.cassetteplayer.R

/**
 * Clickable container whose long press opens [menu] right under the finger.
 * With [menu] = null it behaves like a plain clickable box.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LongPressMenuBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    menu: (@Composable ColumnScope.(dismiss: () -> Unit) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    Box(
        modifier
            .onSizeChanged { boxSize = it }
            .pointerInput(Unit) {
                // Observe only: records the touch position without consuming it.
                awaitEachGesture {
                    pressOffset = awaitFirstDown(requireUnconsumed = false).position
                }
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = menu?.let {
                    {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        open = true
                    }
                },
                onLongClickLabel = if (menu != null) stringResource(R.string.action_options) else null,
            )
    ) {
        content()
        if (menu != null) {
            // Offsets are relative to the anchor's bottom-left; the popup flips near edges.
            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                offset = with(density) {
                    DpOffset(pressOffset.x.toDp(), (pressOffset.y - boxSize.height).toDp())
                },
                modifier = Modifier.background(InkSurface),
            ) {
                menu { open = false }
            }
        }
    }
}

/** Menu row with a leading icon, styled like the rest of the app's menus. */
@Composable
fun MenuItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, color = TextPrimary) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = TextSecondary) },
        onClick = onClick,
    )
}
