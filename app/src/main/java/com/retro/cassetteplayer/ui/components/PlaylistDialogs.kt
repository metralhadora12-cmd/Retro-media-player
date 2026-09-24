package com.retro.cassetteplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.retro.cassetteplayer.data.SongCollection
import com.retro.cassetteplayer.ui.theme.InkRaised
import com.retro.cassetteplayer.ui.theme.InkSurface
import com.retro.cassetteplayer.ui.theme.TapeOrange
import com.retro.cassetteplayer.ui.theme.TextPrimary
import com.retro.cassetteplayer.ui.theme.TextSecondary
import androidx.compose.ui.res.stringResource
import com.retro.cassetteplayer.R
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults

/** Dialog asking for a playlist name; used to create and to rename. */
@Composable
fun PlaylistNameDialog(
    title: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    initialName: String = "",
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    val focusRequester = remember { FocusRequester() }
    val canConfirm = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = InkSurface,
        title = { Text(title, color = TextPrimary) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(MAX_NAME_LENGTH) },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.playlist_name_hint), color = TextSecondary) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { if (canConfirm) onConfirm(name) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = InkRaised,
                    unfocusedContainerColor = InkRaised,
                    focusedBorderColor = TapeOrange,
                    unfocusedBorderColor = InkRaised,
                    cursorColor = TapeOrange,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = canConfirm) {
                Text(confirmLabel, color = if (canConfirm) TapeOrange else TextSecondary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = TextPrimary) }
        },
    )
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
}

/** "Salvar na playlist": pick one of the user's playlists or start a new one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    playlists: List<SongCollection>,
    onSelect: (SongCollection) -> Unit,
    onNewPlaylist: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = InkSurface,
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Text(
                stringResource(R.string.menu_save_to_playlist),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNewPlaylist)
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
                            stringResource(R.string.new_playlist),
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            modifier = Modifier.padding(start = 14.dp),
                        )
                    }
                }
                items(playlists, key = { it.id }) { playlist ->
                    CollectionListItem(playlist, onClick = { onSelect(playlist) })
                }
            }
        }
    }
}

/** Confirmation before deleting a user playlist (the songs themselves stay on the device). */
@Composable
fun DeletePlaylistDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = InkSurface,
        title = { Text(stringResource(R.string.delete_playlist_title), color = TextPrimary) },
        text = { Text(stringResource(R.string.delete_playlist_message, name), color = TextSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_delete), color = TapeOrange) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = TextPrimary) }
        },
    )
}

/** Rename a song: new title (written to the tags) and, optionally, the file name. */
@Composable
fun RenameSongDialog(
    initialName: String,
    onConfirm: (name: String, renameFile: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var renameFile by rememberSaveable { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }
    val canConfirm = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = InkSurface,
        title = { Text(stringResource(R.string.rename_song_title), color = TextPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(120) },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.rename_song_hint), color = TextSecondary) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { if (canConfirm) onConfirm(name, renameFile) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = InkRaised,
                        unfocusedContainerColor = InkRaised,
                        focusedBorderColor = TapeOrange,
                        unfocusedBorderColor = InkRaised,
                        cursorColor = TapeOrange,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clickable { renameFile = !renameFile },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = renameFile,
                        onCheckedChange = { renameFile = it },
                        colors = CheckboxDefaults.colors(checkedColor = TapeOrange),
                    )
                    Text(stringResource(R.string.rename_song_file), color = TextPrimary)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, renameFile) }, enabled = canConfirm) {
                Text(stringResource(R.string.action_rename), color = if (canConfirm) TapeOrange else TextSecondary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = TextPrimary) }
        },
    )
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
}

private const val MAX_NAME_LENGTH = 60
