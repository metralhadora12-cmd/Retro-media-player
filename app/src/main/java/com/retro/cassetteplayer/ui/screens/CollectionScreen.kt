package com.retro.cassetteplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.retro.cassetteplayer.data.CollectionKind
import com.retro.cassetteplayer.data.Song
import com.retro.cassetteplayer.data.SongCollection
import com.retro.cassetteplayer.playback.PlaybackState
import com.retro.cassetteplayer.ui.components.CollageArt
import com.retro.cassetteplayer.ui.components.MetalButton
import com.retro.cassetteplayer.ui.components.SongRow
import com.retro.cassetteplayer.ui.components.TopGlow
import com.retro.cassetteplayer.ui.theme.Navy
import com.retro.cassetteplayer.ui.theme.TextPrimary
import com.retro.cassetteplayer.ui.theme.TextSecondary

/** Album / artist / playlist page: big cover, play & shuffle keys and the track list. */
@Composable
fun CollectionScreen(
    collection: SongCollection?,
    playback: PlaybackState,
    onBack: () -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onShufflePlay: (List<Song>) -> Unit,
    onSongClick: (List<Song>, Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Navy)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(420.dp)
                .background(TopGlow)
        )
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Row(Modifier.statusBarsPadding().padding(4.dp)) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar", tint = TextPrimary)
                    }
                }
            }
            if (collection == null) {
                item { StatusMessage("Coleção não encontrada.") }
                return@LazyColumn
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CollageArt(
                        uris = collection.artworkUris,
                        modifier = Modifier.size(220.dp),
                        shape = if (collection.kind == CollectionKind.ARTIST) CircleShape else RoundedCornerShape(10.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        collection.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        collection.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Row(
                        modifier = Modifier.padding(vertical = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        KeyButton("TOCAR", Icons.Rounded.PlayArrow) { onPlayAll(collection.songs) }
                        KeyButton("ALEATÓRIO", Icons.Rounded.Shuffle) { onShufflePlay(collection.songs) }
                    }
                }
            }
            items(collection.songs, key = { it.id }) { song ->
                SongRow(
                    song = song,
                    isCurrent = song.id.toString() == playback.mediaId,
                    isPlaying = playback.isPlaying,
                    onClick = { onSongClick(collection.songs, song) },
                    onPlayNext = { onPlayNext(song) },
                    onAddToQueue = { onAddToQueue(song) },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 80.dp),
                    color = Color.White.copy(alpha = 0.06f),
                )
            }
        }
    }
}

@Composable
private fun KeyButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    MetalButton(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
