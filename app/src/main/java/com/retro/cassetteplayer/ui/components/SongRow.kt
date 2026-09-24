package com.retro.cassetteplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.retro.cassetteplayer.data.Song
import com.retro.cassetteplayer.ui.theme.TextPrimary
import com.retro.cassetteplayer.ui.theme.TextSecondary
import com.retro.cassetteplayer.ui.theme.InkSurface
import com.retro.cassetteplayer.ui.theme.TapeAmber
import com.retro.cassetteplayer.ui.theme.TapeOrange

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
    onRemoveFromPlaylist: (() -> Unit)? = null,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val favorites = LocalFavorites.current
    val isFavorite = favorites.isFavorite(song)

    // Where the finger went down (row coordinates) and the row size, used to open the
    // menu right under the finger.
    var pressOffset by remember { mutableStateOf(Offset.Zero) }
    var rowSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    Box(
        modifier
            .fillMaxWidth()
            .onSizeChanged { rowSize = it }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    // Observe only: records the touch position without consuming it.
                    awaitEachGesture {
                        pressOffset = awaitFirstDown(requireUnconsumed = false).position
                    }
                }
                // Tap plays; press and hold opens the options menu.
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    },
                    onLongClickLabel = "Opções",
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArt(song.artworkUri, Modifier.size(48.dp), RoundedCornerShape(4.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isCurrent) TapeAmber else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${song.artist} • ${song.album}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isCurrent) {
                PlaybackLed(lit = isPlaying, modifier = Modifier.padding(start = 8.dp))
            }
        }

        // DropdownMenu offsets are relative to the anchor's bottom-left corner;
        // the popup is flipped/clamped automatically near the screen edges.
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            offset = with(density) {
                DpOffset(pressOffset.x.toDp(), (pressOffset.y - rowSize.height).toDp())
            },
            modifier = Modifier.background(InkSurface),
        ) {
            SongMenuItem("Tocar", Icons.Rounded.PlayArrow) { menuOpen = false; onClick() }
            SongMenuItem("Tocar a seguir", Icons.AutoMirrored.Rounded.QueueMusic) { menuOpen = false; onPlayNext() }
            SongMenuItem("Adicionar à fila", Icons.AutoMirrored.Rounded.PlaylistPlay) {
                menuOpen = false
                onAddToQueue()
            }
            SongMenuItem(
                if (isFavorite) "Remover das favoritas" else "Adicionar às favoritas",
                if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            ) {
                menuOpen = false
                favorites.toggle(song)
            }
            SongMenuItem("Salvar na playlist", Icons.AutoMirrored.Rounded.PlaylistAdd) {
                menuOpen = false
                onAddToPlaylist()
            }
            if (onRemoveFromPlaylist != null) {
                SongMenuItem("Remover da playlist", Icons.Rounded.RemoveCircleOutline) {
                    menuOpen = false
                    onRemoveFromPlaylist()
                }
            }
        }
    }
}

@Composable
private fun SongMenuItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, color = TextPrimary) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = TextSecondary) },
        onClick = onClick,
    )
}

/** Round orange LED, glowing when [lit]. */
@Composable
fun PlaybackLed(lit: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(10.dp)
            .shadow(if (lit) 6.dp else 0.dp, CircleShape, ambientColor = TapeOrange, spotColor = TapeOrange)
            .background(if (lit) TapeOrange else TapeOrange.copy(alpha = 0.3f), CircleShape)
    )
}
