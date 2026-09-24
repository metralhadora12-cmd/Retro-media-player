package com.retro.cassetteplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.retro.cassetteplayer.data.Album
import com.retro.cassetteplayer.ui.components.AlbumArt
import com.retro.cassetteplayer.ui.components.darkBrushedMetal
import com.retro.cassetteplayer.ui.theme.Charcoal
import com.retro.cassetteplayer.ui.theme.Cream
import com.retro.cassetteplayer.ui.theme.CreamMuted
import com.retro.cassetteplayer.ui.theme.RetroAmber
import com.retro.cassetteplayer.ui.theme.RetroOrange

@Composable
fun CollectionsScreen(
    albums: List<Album>,
    onPlayAlbum: (Album) -> Unit,
    onShuffleAlbum: (Album) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 156.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(Charcoal)
            .statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text("COLLECTIONS", style = MaterialTheme.typography.headlineSmall, color = Cream)
                Text(
                    "${albums.size} ÁLBUNS",
                    style = MaterialTheme.typography.labelSmall,
                    color = RetroOrange,
                )
            }
        }
        if (albums.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { StatusMessage("Nenhum álbum encontrado.") }
        }
        items(albums, key = { it.id }) { album ->
            AlbumCard(album, onClick = { onPlayAlbum(album) }, onShuffle = { onShuffleAlbum(album) })
        }
    }
}

@Composable
private fun AlbumCard(album: Album, onClick: () -> Unit, onShuffle: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .darkBrushedMetal()
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        AlbumArt(
            uri = album.artworkUri,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            cornerRadius = 8.dp,
        )
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    album.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Cream,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    album.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = CreamMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${album.songs.size} faixas",
                    style = MaterialTheme.typography.labelSmall,
                    color = RetroAmber,
                )
            }
            IconButton(onClick = onShuffle, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.Shuffle, contentDescription = "Aleatório", tint = CreamMuted)
            }
        }
    }
}
