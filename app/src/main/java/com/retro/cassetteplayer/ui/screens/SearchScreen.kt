package com.retro.cassetteplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.retro.cassetteplayer.data.Song
import com.retro.cassetteplayer.playback.PlaybackState
import com.retro.cassetteplayer.ui.components.SongRow
import com.retro.cassetteplayer.ui.theme.Ink
import com.retro.cassetteplayer.ui.theme.InkRaised
import com.retro.cassetteplayer.ui.theme.TextPrimary
import com.retro.cassetteplayer.ui.theme.TextSecondary
import com.retro.cassetteplayer.ui.theme.TapeOrange
import androidx.compose.ui.res.stringResource
import com.retro.cassetteplayer.R
import com.retro.cassetteplayer.ui.theme.Hairline
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import com.retro.cassetteplayer.LyricsMatch
import com.retro.cassetteplayer.ui.components.AlbumArt
import com.retro.cassetteplayer.ui.components.PillButton
import com.retro.cassetteplayer.ui.components.RetroChip
import com.retro.cassetteplayer.ui.components.artistLabel
import com.retro.cassetteplayer.ui.components.titleLabel

@Composable
fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<Song>,
    playback: PlaybackState,
    onSongClick: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    lyricsQuery: String,
    onLyricsQueryChange: (String) -> Unit,
    lyricsResults: List<LyricsMatch>,
    lyricsIndexed: Int,
    totalSongs: Int,
    lyricsDownload: Pair<Int, Int>?,
    onToggleLyricsDownload: () -> Unit,
    onEnterLyricsMode: () -> Unit,
    onLyricsResultClick: (Song) -> Unit,
) {
    var lyricsMode by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(lyricsMode) { if (lyricsMode) onEnterLyricsMode() }
    val currentQuery = if (lyricsMode) lyricsQuery else query
    val changeQuery = if (lyricsMode) onLyricsQueryChange else onQueryChange
    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
    ) {
        Text(
            text = stringResource(R.string.search_title),
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )
        OutlinedTextField(
            value = currentQuery,
            onValueChange = changeQuery,
            singleLine = true,
            placeholder = {
                Text(stringResource(if (lyricsMode) R.string.search_lyrics_hint else R.string.search_hint), color = TextSecondary)
            },
            leadingIcon = { Icon(if (lyricsMode) Icons.Rounded.Lyrics else Icons.Rounded.Search, contentDescription = null, tint = TextSecondary) },
            trailingIcon = {
                if (currentQuery.isNotEmpty()) {
                    IconButton(onClick = { changeQuery("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_clear), tint = TextSecondary)
                    }
                }
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
            shape = RoundedCornerShape(50),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = InkRaised,
                unfocusedContainerColor = InkRaised,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = TapeOrange,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        )
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RetroChip(stringResource(R.string.search_mode_songs), selected = !lyricsMode, onClick = { lyricsMode = false })
            RetroChip(stringResource(R.string.search_mode_lyrics), selected = lyricsMode, onClick = { lyricsMode = true })
        }

        if (lyricsMode) {
            LyricsSearchResults(
                query = lyricsQuery,
                results = lyricsResults,
                indexed = lyricsIndexed,
                total = totalSongs,
                download = lyricsDownload,
                onToggleDownload = onToggleLyricsDownload,
                onClick = onLyricsResultClick,
            )
        } else if (results.isEmpty()) {
            StatusMessage(
                if (query.isBlank()) stringResource(R.string.search_empty_library)
                else stringResource(R.string.search_no_results, query)
            )
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(results, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        isCurrent = song.id.toString() == playback.mediaId,
                        isPlaying = playback.isPlaying,
                        onClick = { onSongClick(song) },
                        onPlayNext = { onPlayNext(song) },
                        onAddToQueue = { onAddToQueue(song) },
                        onAddToPlaylist = { onAddToPlaylist(song) },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 80.dp),
                        color = Hairline,
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricsSearchResults(
    query: String,
    results: List<LyricsMatch>,
    indexed: Int,
    total: Int,
    download: Pair<Int, Int>?,
    onToggleDownload: () -> Unit,
    onClick: (Song) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (download != null) stringResource(R.string.settings_lyrics_index_running, download.first, download.second)
            else stringResource(R.string.search_lyrics_indexed, indexed, total),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.weight(1f),
        )
        if (download != null || indexed < total) {
            PillButton(
                stringResource(if (download != null) R.string.search_lyrics_stop else R.string.search_lyrics_download),
                onClick = onToggleDownload,
            )
        }
    }
    when {
        query.trim().length < 2 -> StatusMessage(stringResource(R.string.search_lyrics_prompt))
        results.isEmpty() -> StatusMessage(stringResource(R.string.search_lyrics_empty, query.trim()))
        else -> LazyColumn(Modifier.fillMaxSize()) {
            items(results, key = { it.song.id }) { match ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClick(match.song) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AlbumArt(match.song.artworkUri, Modifier.size(48.dp), RoundedCornerShape(4.dp))
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(start = 14.dp)
                    ) {
                        Text(match.song.titleLabel(), style = MaterialTheme.typography.bodyLarge, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(match.song.artistLabel(), style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = highlight(match.line, query.trim()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(start = 78.dp), color = Hairline)
            }
        }
    }
}

/** The matched words in bold orange. */
private fun highlight(line: String, query: String): AnnotatedString = buildAnnotatedString {
    val at = line.indexOf(query, ignoreCase = true)
    if (at < 0 || query.isEmpty()) {
        append("“$line”")
        return@buildAnnotatedString
    }
    append("“")
    append(line.substring(0, at))
    withStyle(SpanStyle(color = TapeOrange, fontWeight = FontWeight.Bold)) { append(line.substring(at, at + query.length)) }
    append(line.substring(at + query.length))
    append("”")
}
