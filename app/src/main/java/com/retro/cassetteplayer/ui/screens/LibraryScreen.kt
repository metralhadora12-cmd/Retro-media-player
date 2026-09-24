package com.retro.cassetteplayer.ui.screens

import androidx.compose.foundation.background
import com.retro.cassetteplayer.ui.theme.InkRaised
import androidx.compose.material.icons.rounded.Add
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.cassetteplayer.data.CollectionKind
import com.retro.cassetteplayer.data.LibraryCollections
import com.retro.cassetteplayer.data.Song
import com.retro.cassetteplayer.data.SongCollection
import com.retro.cassetteplayer.playback.PlaybackState
import com.retro.cassetteplayer.ui.components.CollectionGridItem
import com.retro.cassetteplayer.ui.components.CollectionListItem
import com.retro.cassetteplayer.ui.components.PillButton
import com.retro.cassetteplayer.ui.components.RetroChip
import com.retro.cassetteplayer.ui.components.RetroIconChip
import com.retro.cassetteplayer.ui.components.SongRow
import com.retro.cassetteplayer.ui.theme.Ink
import com.retro.cassetteplayer.ui.theme.TextPrimary
import com.retro.cassetteplayer.ui.theme.InkSurface
import com.retro.cassetteplayer.ui.theme.TapeAmber

enum class LibraryFilter(val label: String) {
    PLAYLISTS("Playlists"),
    ALBUMS("Álbuns"),
    ARTISTS("Artistas"),
    SONGS("Músicas"),
}

enum class LibrarySort(val label: String) {
    RECENT("Atividade recente"),
    ALPHABETICAL("Ordem alfabética"),
    SIZE("Mais faixas"),
}

@Composable
fun LibraryScreen(
    songs: List<Song>,
    library: LibraryCollections,
    playback: PlaybackState,
    onOpenSearch: () -> Unit,
    onOpenCollection: (SongCollection) -> Unit,
    onShufflePlay: (List<Song>) -> Unit,
    onSongClick: (List<Song>, Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onCreatePlaylist: () -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf<LibraryFilter?>(null) }
    var sort by rememberSaveable { mutableStateOf(LibrarySort.RECENT) }
    var gridMode by rememberSaveable { mutableStateOf(true) }

    val collections = remember(library, filter, sort) {
        val base = when (filter) {
            LibraryFilter.PLAYLISTS -> library.playlists
            LibraryFilter.ALBUMS -> library.albums
            LibraryFilter.ARTISTS -> library.artists
            LibraryFilter.SONGS -> emptyList()
            null -> library.playlists + library.albums
        }
        // Automatic playlists stay pinned on top, like "Músicas salvas".
        val (pinned, rest) = base.partition { it.kind == CollectionKind.PLAYLIST }
        pinned + when (sort) {
            LibrarySort.RECENT -> rest.sortedByDescending { it.lastAdded }
            LibrarySort.ALPHABETICAL -> rest.sortedBy { it.title.lowercase() }
            LibrarySort.SIZE -> rest.sortedByDescending { it.songs.size }
        }
    }
    val sortedSongs = remember(songs, sort) {
        when (sort) {
            LibrarySort.RECENT -> songs.sortedByDescending { it.dateAdded }
            LibrarySort.ALPHABETICAL -> songs.sortedBy { it.title.lowercase() }
            LibrarySort.SIZE -> songs.sortedByDescending { it.durationMs }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Ink)
    ) {
        Column(Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Biblioteca",
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 30.sp),
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onCreatePlaylist) {
                    Icon(Icons.Rounded.Add, contentDescription = "Nova playlist", tint = TextPrimary)
                }
                IconButton(onClick = onOpenSearch) {
                    Icon(Icons.Rounded.Search, contentDescription = "Buscar", tint = TextPrimary)
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp),
            ) {
                if (filter != null) {
                    item {
                        RetroIconChip(Icons.Rounded.Close, "Limpar filtro", onClick = { filter = null })
                    }
                }
                items(LibraryFilter.entries) { option ->
                    RetroChip(
                        text = option.label,
                        selected = option == filter,
                        onClick = { filter = if (option == filter) null else option },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SortSelector(sort, onSortChange = { sort = it })
                Spacer(Modifier.weight(1f))
                if (filter != LibraryFilter.SONGS) {
                    IconButton(onClick = { gridMode = !gridMode }) {
                        Icon(
                            if (gridMode) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                            contentDescription = if (gridMode) "Ver em lista" else "Ver em grade",
                            tint = TextPrimary,
                        )
                    }
                }
            }

            val bottomSpace = PaddingValues(bottom = 96.dp)
            val showNewTile = filter == LibraryFilter.PLAYLISTS
            when {
                filter == LibraryFilter.SONGS -> LazyColumn(contentPadding = bottomSpace) {
                    items(sortedSongs, key = { it.id }) { song ->
                        SongRow(
                            song = song,
                            isCurrent = song.id.toString() == playback.mediaId,
                            isPlaying = playback.isPlaying,
                            onClick = { onSongClick(sortedSongs, song) },
                            onPlayNext = { onPlayNext(song) },
                            onAddToQueue = { onAddToQueue(song) },
                            onAddToPlaylist = { onAddToPlaylist(song) },
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 80.dp),
                            color = Color.White.copy(alpha = 0.06f),
                        )
                    }
                }
                collections.isEmpty() && filter != LibraryFilter.PLAYLISTS ->
                    StatusMessage("Nada na biblioteca ainda.")
                gridMode -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (showNewTile) {
                        item(key = "new-playlist") { NewPlaylistGridItem(onCreatePlaylist) }
                    }
                    items(collections, key = { it.id }) { collection ->
                        CollectionGridItem(collection, onClick = { onOpenCollection(collection) })
                    }
                }
                else -> LazyColumn(contentPadding = bottomSpace) {
                    if (showNewTile) {
                        item(key = "new-playlist") { NewPlaylistListItem(onCreatePlaylist) }
                    }
                    items(collections, key = { it.id }) { collection ->
                        CollectionListItem(collection, onClick = { onOpenCollection(collection) })
                    }
                }
            }
        }

        PillButton(
            onClick = { onShufflePlay(songs) },
            enabled = songs.isNotEmpty(),
            filled = true,
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Rounded.Shuffle, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text("Modo aleatório", style = MaterialTheme.typography.titleMedium, color = Ink)
        }
    }
}

@Composable
private fun SortSelector(sort: LibrarySort, onSortChange: (LibrarySort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clickable { open = true }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(sort.label, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Ordenar", tint = TextPrimary)
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.background(InkSurface),
        ) {
            LibrarySort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label, color = if (option == sort) TapeAmber else TextPrimary) },
                    onClick = {
                        open = false
                        onSortChange(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun NewPlaylistGridItem(onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(bottom = 8.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(InkRaised, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(40.dp))
        }
        Text(
            "Nova playlist",
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun NewPlaylistListItem(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .background(InkRaised, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = TextPrimary)
        }
        Text(
            "Nova playlist",
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            modifier = Modifier.padding(start = 14.dp),
        )
    }
}
