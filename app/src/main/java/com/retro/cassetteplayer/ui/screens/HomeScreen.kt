package com.retro.cassetteplayer.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.retro.cassetteplayer.R
import com.retro.cassetteplayer.data.Song
import com.retro.cassetteplayer.data.SongCollection
import com.retro.cassetteplayer.playback.PlaybackState
import com.retro.cassetteplayer.ui.components.CollageArt
import com.retro.cassetteplayer.ui.components.CoverTile
import com.retro.cassetteplayer.ui.components.PageDots
import com.retro.cassetteplayer.ui.components.PillButton
import com.retro.cassetteplayer.ui.components.RetroChip
import com.retro.cassetteplayer.ui.components.SectionHeader
import com.retro.cassetteplayer.ui.components.SongRow
import com.retro.cassetteplayer.ui.components.TopGlow
import com.retro.cassetteplayer.ui.theme.Ink
import com.retro.cassetteplayer.ui.theme.TextPrimary
import com.retro.cassetteplayer.ui.theme.TextSecondary
import com.retro.cassetteplayer.ui.theme.DisplayFont
import com.retro.cassetteplayer.ui.theme.InkRaised
import com.retro.cassetteplayer.ui.theme.TapeOrange
import kotlin.random.Random

private const val TILES_PER_PAGE = 9
private const val QUICK_PICK_ROWS = 4

@Composable
fun HomeScreen(
    songs: List<Song>,
    albums: List<SongCollection>,
    playlists: List<SongCollection>,
    playback: PlaybackState,
    isLoading: Boolean,
    hasPermission: Boolean?,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onReload: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenCollection: (SongCollection) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onSongClick: (List<Song>, Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
) {
    // Artist chips act like YouTube Music's mood chips: they filter the whole page.
    var selectedArtist by rememberSaveable { mutableStateOf<String?>(null) }
    val topArtists = remember(songs) {
        songs.groupingBy { it.artist }.eachCount().entries
            .sortedByDescending { it.value }
            .take(8)
            .map { it.key }
    }
    val visibleSongs = remember(songs, selectedArtist) {
        selectedArtist?.let { artist -> songs.filter { it.artist == artist } } ?: songs
    }
    val visibleAlbums = remember(albums, selectedArtist) {
        selectedArtist?.let { artist -> albums.filter { a -> a.songs.any { it.artist == artist } } } ?: albums
    }
    val jukebox = remember(playlists, visibleAlbums, selectedArtist) {
        val base = if (selectedArtist == null) playlists + visibleAlbums else visibleAlbums
        base.take(TILES_PER_PAGE * 3)
    }
    val quickPicks = remember(visibleSongs) {
        visibleSongs.shuffled(Random(visibleSongs.size)).take(QUICK_PICK_ROWS * 5)
    }
    val recentAlbums = remember(visibleAlbums) {
        visibleAlbums.sortedByDescending { it.lastAdded }.take(12)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Ink)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(420.dp)
                .background(TopGlow)
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item { HomeTopBar(onReload = onReload, onOpenSearch = onOpenSearch) }

            if (topArtists.isNotEmpty()) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp),
                    ) {
                        item {
                            RetroChip("Tudo", selectedArtist == null, onClick = { selectedArtist = null })
                        }
                        items(topArtists) { artist ->
                            RetroChip(
                                text = artist,
                                selected = artist == selectedArtist,
                                onClick = { selectedArtist = if (artist == selectedArtist) null else artist },
                            )
                        }
                    }
                }
            }

            when {
                hasPermission == false -> item { PermissionCard(onRequestPermission, onOpenSettings) }
                isLoading -> item { StatusMessage("REBOBINANDO…", showProgress = true) }
                hasPermission == true && songs.isEmpty() -> item {
                    StatusMessage("Nenhuma música encontrada no dispositivo.")
                }
                songs.isNotEmpty() -> {
                    if (jukebox.isNotEmpty()) {
                        item {
                            Column {
                                SectionHeader(
                                    title = "Jukebox de fitas",
                                    overline = selectedArtist ?: "Sua coleção",
                                    leading = { CassetteAvatar() },
                                    action = {
                                        IconButton(onClick = onOpenLibrary) {
                                            Icon(
                                                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                                contentDescription = "Abrir biblioteca",
                                                tint = TextPrimary,
                                            )
                                        }
                                    },
                                    modifier = Modifier.padding(top = 12.dp),
                                )
                                JukeboxPager(jukebox, onOpenCollection)
                            }
                        }
                    }

                    if (quickPicks.isNotEmpty()) {
                        item {
                            Column {
                                SectionHeader(
                                    title = "Escolha a dedo",
                                    action = { PillButton("Tocar tudo", onClick = { onPlayAll(quickPicks) }) },
                                    modifier = Modifier.padding(top = 20.dp),
                                )
                                QuickPicks(
                                    picks = quickPicks,
                                    playback = playback,
                                    onSongClick = { onSongClick(quickPicks, it) },
                                    onPlayNext = onPlayNext,
                                    onAddToQueue = onAddToQueue,
                                )
                            }
                        }
                    }

                    if (recentAlbums.isNotEmpty()) {
                        item {
                            Column {
                                SectionHeader(
                                    title = "Adicionadas recentemente",
                                    modifier = Modifier.padding(top = 20.dp),
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(recentAlbums, key = { it.id }) { album ->
                                        AlbumCard(album, onClick = { onOpenCollection(album) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(onReload: () -> Unit, onOpenSearch: () -> Unit) {
    Row(
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(start = 8.dp, end = 4.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(52.dp),
        )
        Text(
            text = "Cassette",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onReload) {
            Icon(Icons.Rounded.Refresh, contentDescription = "Recarregar biblioteca", tint = TextPrimary)
        }
        IconButton(onClick = onOpenSearch) {
            Icon(Icons.Rounded.Search, contentDescription = "Buscar", tint = TextPrimary)
        }
    }
}

@Composable
private fun CassetteAvatar() {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(InkRaised),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.requiredSize(64.dp),
        )
    }
}

/** 3x3 pages of cover tiles, swiped horizontally, with LED page dots. */
@Composable
private fun JukeboxPager(tiles: List<SongCollection>, onOpenCollection: (SongCollection) -> Unit) {
    val pages = remember(tiles) { tiles.chunked(TILES_PER_PAGE) }
    val pagerState = rememberPagerState { pages.size }
    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        pageSpacing = 12.dp,
        verticalAlignment = Alignment.Top,
    ) { page ->
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            pages[page].chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { tile ->
                        CoverTile(tile, onClick = { onOpenCollection(tile) }, modifier = Modifier.weight(1f))
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
    if (pages.size > 1) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            PageDots(pages.size, pagerState.currentPage)
        }
    }
}

/** Columns of four songs that scroll sideways, like "Escolha a dedo". */
@Composable
private fun QuickPicks(
    picks: List<Song>,
    playback: PlaybackState,
    onSongClick: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
) {
    val rowHeight = 72.dp
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val itemWidth = maxWidth * 0.88f
        LazyHorizontalGrid(
            rows = GridCells.Fixed(QUICK_PICK_ROWS),
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight * QUICK_PICK_ROWS),
        ) {
            items(picks, key = { it.id }) { song ->
                SongRow(
                    song = song,
                    isCurrent = song.id.toString() == playback.mediaId,
                    isPlaying = playback.isPlaying,
                    onClick = { onSongClick(song) },
                    onPlayNext = { onPlayNext(song) },
                    onAddToQueue = { onAddToQueue(song) },
                    modifier = Modifier.width(itemWidth),
                )
            }
        }
    }
}

@Composable
private fun AlbumCard(album: SongCollection, onClick: () -> Unit) {
    Column(
        Modifier
            .width(148.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        CollageArt(
            album.artworkUris,
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            RoundedCornerShape(8.dp),
        )
        Text(
            album.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            album.songs.first().artist,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )
        PillButton("Permitir acesso", onClick = onRequestPermission, filled = true)
        TextButton(onClick = onOpenSettings) {
            Text("Abrir configurações do app", color = TextSecondary)
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
                CircularProgressIndicator(color = TapeOrange, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(12.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontFamily = DisplayFont,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
