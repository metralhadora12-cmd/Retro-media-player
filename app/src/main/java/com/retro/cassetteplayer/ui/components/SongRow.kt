package com.retro.cassetteplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.retro.cassetteplayer.data.Song
import com.retro.cassetteplayer.ui.theme.Cream
import com.retro.cassetteplayer.ui.theme.CreamMuted
import com.retro.cassetteplayer.ui.theme.Graphite
import com.retro.cassetteplayer.ui.theme.RetroAmber
import com.retro.cassetteplayer.ui.theme.RetroOrange

@Composable
fun SongRow(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(song.artworkUri, Modifier.size(50.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isCurrent) RetroAmber else Cream,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = CreamMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.album,
                style = MaterialTheme.typography.labelSmall,
                color = CreamMuted.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isCurrent) {
            PlaybackLed(lit = isPlaying, modifier = Modifier.padding(start = 8.dp))
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "Opções", tint = CreamMuted)
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                modifier = Modifier.background(Graphite),
            ) {
                SongMenuItem("Tocar", Icons.Rounded.PlayArrow) { menuOpen = false; onClick() }
                SongMenuItem("Tocar a seguir", Icons.AutoMirrored.Rounded.QueueMusic) { menuOpen = false; onPlayNext() }
                SongMenuItem("Adicionar à fila", Icons.AutoMirrored.Rounded.PlaylistAdd) {
                    menuOpen = false
                    onAddToQueue()
                }
            }
        }
    }
}

@Composable
private fun SongMenuItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, color = Cream) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = RetroAmber) },
        onClick = onClick,
    )
}

/** Round orange LED, glowing when [lit]. */
@Composable
fun PlaybackLed(lit: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(10.dp)
            .shadow(if (lit) 6.dp else 0.dp, CircleShape, ambientColor = RetroOrange, spotColor = RetroOrange)
            .background(if (lit) RetroOrange else RetroOrange.copy(alpha = 0.3f), CircleShape)
    )
}
