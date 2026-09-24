package com.retro.cassetteplayer.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.retro.cassetteplayer.R
import com.retro.cassetteplayer.data.CoverResult
import com.retro.cassetteplayer.data.Song
import com.retro.cassetteplayer.data.SongTags
import com.retro.cassetteplayer.data.WriteResult
import com.retro.cassetteplayer.ui.components.CassetteGlyph
import com.retro.cassetteplayer.ui.components.PillButton
import com.retro.cassetteplayer.ui.components.tracksLabel
import com.retro.cassetteplayer.ui.theme.Ink
import com.retro.cassetteplayer.ui.theme.InkRaised
import com.retro.cassetteplayer.ui.theme.InkSurface
import com.retro.cassetteplayer.ui.theme.TapeOrange
import com.retro.cassetteplayer.ui.theme.TextPrimary
import com.retro.cassetteplayer.ui.theme.TextSecondary

/**
 * Edits a song's tags and cover and writes them into the audio file.
 * [albumSongs] lets the new cover be applied to the whole album.
 */
@Composable
fun EditTagsScreen(
    song: Song,
    albumSongs: List<Song>,
    applyArtworkToAlbum: Boolean,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onFailed: () -> Unit,
    onCoverDownloadFailed: () -> Unit,
    viewModel: TagEditorViewModel = viewModel(),
) {
    LaunchedEffect(song.id) { viewModel.start(song, albumSongs, applyArtworkToAlbum) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var searchingCover by rememberSaveable { mutableStateOf(false) }

    val handleResult: (WriteResult) -> Unit = { result ->
        when (result) {
            WriteResult.Saved -> onSaved()
            WriteResult.Failed -> onFailed()
            is WriteResult.NeedsPermission -> Unit // handled by the launcher below
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) viewModel.save(handleResult)
    }
    // Android 10 asks for access while writing; relaunch the save once granted.
    val saveWithRetry: () -> Unit = {
        viewModel.save { result ->
            if (result is WriteResult.NeedsPermission) {
                permissionLauncher.launch(IntentSenderRequest.Builder(result.intentSender).build())
            } else {
                handleResult(result)
            }
        }
    }
    val onSave: () -> Unit = {
        val request = viewModel.writeRequest()
        if (request != null) permissionLauncher.launch(IntentSenderRequest.Builder(request).build())
        else saveWithRetry()
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::loadArtworkFrom)
    }

    if (searchingCover) {
        CoverSearchSheet(
            initialQuery = listOf(state.tags.albumArtist.ifBlank { state.tags.artist }, state.tags.album)
                .filter { it.isNotBlank() }.joinToString(" "),
            searching = state.searching,
            results = state.searchResults,
            failed = state.searchFailed,
            onSearch = viewModel::searchCovers,
            onPick = { result ->
                searchingCover = false
                viewModel.pickCover(result) { ok -> if (!ok) onCoverDownloadFailed() }
            },
            onDismiss = { searchingCover = false },
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
    ) {
        ScreenTopBar(stringResource(R.string.tags_title), onBack)

        if (state.loading) {
            StatusMessage(stringResource(R.string.tags_loading), showProgress = true)
            return@Column
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Cover
            Box(
                Modifier
                    .padding(top = 8.dp)
                    .size(220.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(InkRaised),
                contentAlignment = Alignment.Center,
            ) {
                val preview = state.artworkPreview
                if (preview != null) {
                    AsyncImage(
                        model = preview,
                        contentDescription = stringResource(R.string.tags_cover),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    CassetteGlyph(Modifier.fillMaxSize(0.6f))
                }
                if (state.downloadingArtwork) {
                    CircularProgressIndicator(color = TapeOrange, modifier = Modifier.size(36.dp))
                }
            }
            Row(
                modifier = Modifier.padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PillButton(stringResource(R.string.tags_cover_search), { searchingCover = true }, icon = Icons.Rounded.Search)
                PillButton(
                    stringResource(R.string.tags_cover_gallery),
                    { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    icon = Icons.Outlined.Image,
                )
                if (state.artworkPreview != null) {
                    PillButton(stringResource(R.string.tags_cover_remove), viewModel::removeArtwork, icon = Icons.Rounded.DeleteOutline)
                }
            }
            if (albumSongs.size > 1) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setApplyArtworkToAlbum(!state.applyArtworkToAlbum) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = state.applyArtworkToAlbum,
                        onCheckedChange = viewModel::setApplyArtworkToAlbum,
                        colors = CheckboxDefaults.colors(checkedColor = TapeOrange),
                    )
                    Text(
                        stringResource(R.string.tags_apply_album, tracksLabel(albumSongs.size)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                    )
                }
            }

            if (state.readFailed) {
                Text(
                    stringResource(R.string.tags_read_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            // Text fields
            val tags = state.tags
            TagField(R.string.tags_field_title, tags.title) { v -> viewModel.updateTags { it.copy(title = v) } }
            TagField(R.string.tags_field_artist, tags.artist) { v -> viewModel.updateTags { it.copy(artist = v) } }
            TagField(R.string.tags_field_album, tags.album) { v -> viewModel.updateTags { it.copy(album = v) } }
            TagField(R.string.tags_field_album_artist, tags.albumArtist) { v -> viewModel.updateTags { it.copy(albumArtist = v) } }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TagField(R.string.tags_field_year, tags.year, Modifier.weight(1f), numeric = true) { v ->
                    viewModel.updateTags { it.copy(year = v) }
                }
                TagField(R.string.tags_field_track, tags.track, Modifier.weight(1f), numeric = true) { v ->
                    viewModel.updateTags { it.copy(track = v) }
                }
            }
            TagField(R.string.tags_field_genre, tags.genre) { v -> viewModel.updateTags { it.copy(genre = v) } }

            Box(Modifier.padding(top = 20.dp), contentAlignment = Alignment.Center) {
                if (state.saving) {
                    CircularProgressIndicator(color = TapeOrange, modifier = Modifier.size(32.dp))
                } else {
                    PillButton(
                        text = stringResource(R.string.tags_save),
                        onClick = onSave,
                        filled = true,
                        enabled = !state.downloadingArtwork,
                    )
                }
            }
        }
    }
}

@Composable
private fun TagField(
    label: Int,
    value: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    numeric: Boolean = false,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(if (numeric) it.filter { c -> c.isDigit() || c == '/' }.take(9) else it) },
        label = { Text(stringResource(label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text,
            imeAction = ImeAction.Next,
        ),
        colors = tagFieldColors(),
        modifier = modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun tagFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TapeOrange,
    unfocusedBorderColor = InkRaised,
    focusedLabelColor = TapeOrange,
    unfocusedLabelColor = TextSecondary,
    cursorColor = TapeOrange,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
)

/** Bottom sheet that searches covers online and shows them in a grid. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoverSearchSheet(
    initialQuery: String,
    searching: Boolean,
    results: List<CoverResult>?,
    failed: Boolean,
    onSearch: (String) -> Unit,
    onPick: (CoverResult) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    val searchedOnce = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!searchedOnce.value && query.isNotBlank()) {
            searchedOnce.value = true
            onSearch(query)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = InkSurface,
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .height(560.dp)
        ) {
            Text(
                stringResource(R.string.cover_search_title),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.cover_search_hint), color = TextSecondary) },
                trailingIcon = {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = stringResource(R.string.action_search),
                        tint = TextSecondary,
                        modifier = Modifier.clickable { onSearch(query) },
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
                colors = tagFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
            when {
                searching -> StatusMessage("", showProgress = true)
                failed -> StatusMessage(stringResource(R.string.cover_search_failed))
                results != null && results.isEmpty() -> StatusMessage(stringResource(R.string.cover_search_empty))
                results != null -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        items(results, key = { it.imageUrl }) { result ->
                            Column(
                                Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onPick(result) }
                            ) {
                                AsyncImage(
                                    model = result.thumbnailUrl,
                                    contentDescription = result.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(InkRaised),
                                )
                                Text(
                                    result.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                                Text(
                                    result.artist,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.cover_search_source),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
