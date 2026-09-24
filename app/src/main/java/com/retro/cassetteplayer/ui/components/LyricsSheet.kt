package com.retro.cassetteplayer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retro.cassetteplayer.LyricsState
import com.retro.cassetteplayer.R
import com.retro.cassetteplayer.data.LyricLine
import com.retro.cassetteplayer.data.LyricsSource
import com.retro.cassetteplayer.ui.screens.StatusMessage
import com.retro.cassetteplayer.ui.theme.InkSurface
import com.retro.cassetteplayer.ui.theme.TapeOrange
import com.retro.cassetteplayer.ui.theme.TextPrimary
import com.retro.cassetteplayer.ui.theme.TextSecondary

/** Lead time so a line lights up just as it is sung. */
private const val LYRIC_LEAD_MS = 300L

/**
 * "LETRA": synced lyrics highlight and follow the song (tap a line to jump there);
 * plain lyrics are shown as scrollable text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSheet(
    title: String,
    state: LyricsState,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetHeight = (LocalConfiguration.current.screenHeightDp * 0.8f).dp
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = InkSurface,
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .height(sheetHeight)
        ) {
            Text(
                text = stringResource(R.string.lyrics_title),
                style = MaterialTheme.typography.labelLarge,
                color = TapeOrange,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )

            when (state) {
                LyricsState.Idle, is LyricsState.Loading ->
                    StatusMessage(stringResource(R.string.lyrics_loading), showProgress = true)
                is LyricsState.NotFound -> RetryMessage(stringResource(R.string.lyrics_not_found), onRetry)
                is LyricsState.Offline -> RetryMessage(stringResource(R.string.lyrics_offline), onRetry)
                is LyricsState.Found -> {
                    val lyrics = state.lyrics
                    Column(Modifier.weight(1f)) {
                        val synced = lyrics.synced
                        if (synced != null) {
                            SyncedLyrics(synced, positionMs, onSeek)
                        } else {
                            Text(
                                text = lyrics.plain.orEmpty(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 24.dp, vertical = 16.dp),
                            )
                        }
                    }
                    Text(
                        text = stringResource(
                            when (lyrics.source) {
                                LyricsSource.FILE -> R.string.lyrics_source_file
                                LyricsSource.LRC_FILE -> R.string.lyrics_source_lrc
                                LyricsSource.ONLINE -> R.string.lyrics_source_online
                            }
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncedLyrics(lines: List<LyricLine>, positionMs: Long, onSeek: (Long) -> Unit) {
    val listState = rememberLazyListState()
    val current = remember(lines, positionMs) {
        lines.indexOfLast { it.timeMs <= positionMs + LYRIC_LEAD_MS }
    }
    // Keep the current line in view (a bit below the top), unless the user is scrolling.
    LaunchedEffect(current) {
        if (current >= 0 && !listState.isScrollInProgress) {
            listState.animateScrollToItem((current - 2).coerceAtLeast(0))
        }
    }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        itemsIndexed(lines) { index, line ->
            val color by animateColorAsState(
                when {
                    index == current -> TextPrimary
                    index < current -> TextSecondary.copy(alpha = 0.45f)
                    else -> TextSecondary
                },
                label = "lyricColor",
            )
            Text(
                text = line.text.ifBlank { "♪" },
                style = if (index == current) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                fontWeight = if (index == current) FontWeight.Bold else FontWeight.SemiBold,
                color = color,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onSeek(line.timeMs) }
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun RetryMessage(text: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        StatusMessage(text)
        PillButton(stringResource(R.string.lyrics_retry), onClick = onRetry)
    }
}
