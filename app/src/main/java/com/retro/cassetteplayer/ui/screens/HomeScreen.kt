package com.retro.cassetteplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.retro.cassetteplayer.data.Song
import com.retro.cassetteplayer.playback.PlaybackState
import com.retro.cassetteplayer.ui.components.CassetteTape
import com.retro.cassetteplayer.ui.components.MetalButton
import com.retro.cassetteplayer.ui.components.SongRow
import com.retro.cassetteplayer.ui.components.steelBrushedMetal
import com.retro.cassetteplayer.ui.theme.Charcoal
import com.retro.cassetteplayer.ui.theme.Cream
import com.retro.cassetteplayer.ui.theme.CreamMuted
import com.retro.cassetteplayer.ui.theme.RetroOrange

@Composable
fun HomeScreen(
    songs: List<Song>,
    playback: PlaybackState,
    isLoading: Boolean,
    hasPermission: Boolean?,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onReload: () -> Unit,
    onShufflePlay: () -> Unit,
    onSongClick: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Charcoal),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            HomeHeader(
                playback = playback,
                canShuffle = songs.isNotEmpty(),
                onShufflePlay = onShufflePlay,
                onReload = onReload,
            )
        }

        when {
            hasPermission == false -> item {
                PermissionCard(onRequestPermission, onOpenSettings)
            }
            isLoading -> item { StatusMessage("REBOBINANDO…", showProgress = true) }
            hasPermission == true && songs.isEmpty() -> item {
                StatusMessage("Nenhuma música encontrada no dispositivo.")
            }
            else -> {
                item {
                    Text(
                        text = "SIDE A · ${songs.size} FAIXAS",
                        style = MaterialTheme.typography.labelLarge,
                        color = RetroOrange,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
                items(songs, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        isCurrent = song.id.toString() == playback.mediaId,
                        isPlaying = playback.isPlaying,
                        onClick = { onSongClick(song) },
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
}

@Composable
private fun HomeHeader(
    playback: PlaybackState,
    canShuffle: Boolean,
    onShufflePlay: () -> Unit,
    onReload: () -> Unit,
) {
    Column(
        Modifier
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "RETRO·CASSETTE",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Cream,
                )
                Text(
                    text = "HI-FI STEREO · TYPE I NORMAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = RetroOrange,
                )
            }
            IconButton(onClick = onReload) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Recarregar biblioteca", tint = CreamMuted)
            }
        }
        Spacer(Modifier.height(16.dp))

        val cardShape = RoundedCornerShape(18.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, cardShape)
                .clip(cardShape)
                .steelBrushedMetal()
                .border(1.dp, Color.White.copy(alpha = 0.15f), cardShape)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CassetteTape(
                isPlaying = playback.isPlaying,
                progress = playback.progress,
                label = playback.title.ifBlank { "MIX TAPE VOL.1" },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            MetalButton(
                onClick = onShufflePlay,
                enabled = canShuffle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text("SHUFFLE PLAY", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(onRequestPermission: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Para tocar suas fitas, o app precisa de acesso aos arquivos de áudio do dispositivo.",
            style = MaterialTheme.typography.bodyLarge,
            color = Cream,
            textAlign = TextAlign.Center,
        )
        MetalButton(onClick = onRequestPermission, modifier = Modifier.height(50.dp)) {
            Text("PERMITIR ACESSO", style = MaterialTheme.typography.labelLarge)
        }
        TextButton(onClick = onOpenSettings) {
            Text("Abrir configurações do app", color = RetroOrange)
        }
    }
}

@Composable
fun StatusMessage(text: String, showProgress: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (showProgress) {
                CircularProgressIndicator(color = RetroOrange, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(12.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = CreamMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
