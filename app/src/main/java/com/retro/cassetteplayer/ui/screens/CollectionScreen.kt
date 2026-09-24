package com.retro.cassetteplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.retro.cassetteplayer.ui.theme.InkRaised
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
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
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.retro.cassetteplayer.ui.components.DeletePlaylistDialog
import com.retro.cassetteplayer.ui.components.PlaylistNameDialog
import com.retro.cassetteplayer.ui.theme.InkSurface
import com.retro.cassetteplayer.ui.theme.TapeOrange
import com.retro.cassetteplayer.ui.theme.TextSecondary
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
import com.retro.cassetteplayer.ui.components.PillButton
import com.retro.cassetteplayer.ui.components.SongRow
import com.retro.cassetteplayer.ui.components.TopGlow
import com.retro.cassetteplayer.ui.theme.Ink
import com.retro.cassetteplayer.ui.theme.TextPrimary

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
    onAddToPlaylist: (Song) -> Unit,
    onSaveAll: (List<Song>) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onRemoveFromPlaylist: (String, Song) -> Unit,
    onReorderPlaylist: (String, List<Song>) -> Unit,
) {
    val isFavorites = collection?.isFavorites == true
    val playlistId = collection?.userPlaylistId

    // Local copy of the track order: updated live while dragging, saved when the drag ends.
    var tracks by remember(collection?.songs) { mutableStateOf(collection?.songs.orEmpty()) }
    val lazyListState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        // Header rows have no song key, so moves onto them are ignored.
        val fromIndex = tracks.indexOfFirst { it.id == from.key }
        val toIndex = tracks.indexOfFirst { it.id == to.key }
        if (fromIndex >= 0 && toIndex >= 0) {
            tracks = tracks.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    var menuOpen by remember { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    if (renaming && collection != null && playlistId != null) {
        PlaylistNameDialog(
            title = "Renomear playlist",
            confirmLabel = "Salvar",
            initialName = collection.title,
            onConfirm = { name ->
                onRenamePlaylist(playlistId, name)
                renaming = false
            },
            onDismiss = { renaming = false },
        )
    }
    if (confirmDelete && collection != null && playlistId != null) {
        DeletePlaylistDialog(
            name = collection.title,
            onConfirm = {
                confirmDelete = false
                onDeletePlaylist(playlistId)
                onBack()
            },
            onDismiss = { confirmDelete = false },
        )
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
        LazyColumn(state = lazyListState, contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Row(
                    Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar", tint = TextPrimary)
                    }
                    Spacer(Modifier.weight(1f))
                    if (playlistId != null) {
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "Opções da playlist", tint = TextPrimary)
                            }
                            DropdownMenu(
                                expanded = menuOpen,
                                onDismissRequest = { menuOpen = false },
                                modifier = Modifier.background(InkSurface),
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Renomear", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = TextSecondary) },
                                    onClick = { menuOpen = false; renaming = true },
                                )
                                DropdownMenuItem(
                                    text = { Text("Excluir playlist", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = TextSecondary) },
                                    onClick = { menuOpen = false; confirmDelete = true },
                                )
                            }
                        }
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
                        style = MaterialTheme.typography.headlineSmall,
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
                        PillButton("Tocar", onClick = { onPlayAll(collection.songs) }, icon = Icons.Rounded.PlayArrow, filled = true)
                        PillButton("Aleatório", onClick = { onShufflePlay(collection.songs) }, icon = Icons.Rounded.Shuffle)
                        if (playlistId == null) {
                            PillButton("Salvar", onClick = { onSaveAll(collection.songs) }, icon = Icons.AutoMirrored.Rounded.PlaylistAdd)
                        }
                    }
                }
            }
            if (collection.songs.isEmpty()) {
                item {
                    StatusMessage(
                        if (isFavorites) "Nenhuma favorita ainda. Toque no coração no player ou segure uma música e escolha \"Adicionar às favoritas\"."
                        else "Playlist vazia. Segure uma música e escolha \"Salvar na playlist\" para adicioná-la aqui."
                    )
                }
            }
            items(tracks, key = { it.id }) { song ->
                ReorderableItem(reorderState, key = song.id, enabled = playlistId != null) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "dragElevation")
                    Column(
                        Modifier
                            .shadow(elevation)
                            .background(if (isDragging) InkRaised else Color.Transparent)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SongRow(
                                song = song,
                                isCurrent = song.id.toString() == playback.mediaId,
                                isPlaying = playback.isPlaying,
                                onClick = { onSongClick(tracks, song) },
                                onPlayNext = { onPlayNext(song) },
                                onAddToQueue = { onAddToQueue(song) },
                                onAddToPlaylist = { onAddToPlaylist(song) },
                                onRemoveFromPlaylist = when {
                                    // In "Favoritas" the menu's own "Remover das favoritas" covers it
                                    isFavorites -> null
                                    playlistId != null -> { { onRemoveFromPlaylist(playlistId, song) } }
                                    else -> null
                                },
                                modifier = Modifier.weight(1f),
                            )
                            if (playlistId != null) {
                                Icon(
                                    Icons.Rounded.DragHandle,
                                    contentDescription = "Arrastar para reordenar",
                                    tint = if (isDragging) TapeOrange else TextSecondary,
                                    modifier = Modifier
                                        .draggableHandle(
                                            onDragStarted = {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                            onDragStopped = { onReorderPlaylist(playlistId, tracks) },
                                        )
                                        .padding(start = 4.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                                )
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 80.dp),
                            color = Color.White.copy(alpha = 0.06f),
                        )
                    }
                }
            }
        }
    }
}
