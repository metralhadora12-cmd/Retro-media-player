@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.retro.cassetteplayer.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.retro.cassetteplayer.R
import com.retro.cassetteplayer.data.Song
import com.retro.cassetteplayer.data.StatsSnapshot
import com.retro.cassetteplayer.ui.components.AlbumArt
import com.retro.cassetteplayer.ui.components.RetroChip
import com.retro.cassetteplayer.ui.components.artistLabel
import com.retro.cassetteplayer.ui.components.titleLabel
import com.retro.cassetteplayer.ui.theme.DisplayFont
import com.retro.cassetteplayer.ui.theme.Ink
import com.retro.cassetteplayer.ui.theme.InkRaised
import com.retro.cassetteplayer.ui.theme.InkSurface
import com.retro.cassetteplayer.ui.theme.TapeOrange
import com.retro.cassetteplayer.ui.theme.TextPrimary
import com.retro.cassetteplayer.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class Ranked(val label: String, val song: Song?, val ms: Long, val plays: Int)

/** Listening stats: time listened, last 30 days, top artists and songs, monthly recap. */
@Composable
fun StatsScreen(
    songs: List<Song>,
    stats: StatsSnapshot,
    onBack: () -> Unit,
    onSongClick: (List<Song>, Song) -> Unit,
) {
    var thisMonth by rememberSaveable { mutableStateOf(true) }
    val byId = remember(songs) { songs.associateBy { it.id } }
    val month = YearMonth.now()
    val monthMs: Map<Long, Long> = stats.monthly[month.toString()].orEmpty()

    val topSongs = remember(stats, byId, thisMonth) {
        if (thisMonth) {
            monthMs.entries.sortedByDescending { it.value }.mapNotNull { (id, ms) ->
                byId[id]?.let { Ranked(it.title, it, ms, stats.songs[id]?.plays ?: 0) }
            }
        } else {
            stats.songs.entries.filter { it.value.plays > 0 }
                .sortedWith(compareByDescending<Map.Entry<Long, com.retro.cassetteplayer.data.SongStats>> { it.value.plays }.thenByDescending { it.value.listenedMs })
                .mapNotNull { (id, s) -> byId[id]?.let { Ranked(it.title, it, s.listenedMs, s.plays) } }
        }.take(10)
    }
    val topArtists = remember(stats, byId, thisMonth) {
        val perSong: Map<Long, Long> = if (thisMonth) monthMs else stats.songs.mapValues { it.value.listenedMs }
        perSong.entries.mapNotNull { (id, ms) -> byId[id]?.let { it.artist to ms } }
            .groupBy({ it.first }, { it.second })
            .map { (artist, list) -> Ranked(artist, null, list.sum(), 0) }
            .sortedByDescending { it.ms }
            .take(5)
    }
    val listenedMs = if (thisMonth) monthMs.values.sum() else stats.totalListenedMs
    val distinctSongs = if (thisMonth) monthMs.size else stats.songs.count { it.value.listenedMs > 0 }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
    ) {
        ScreenTopBar(stringResource(R.string.stats_title), onBack)
        if (stats.daily.isEmpty()) {
            StatusMessage(stringResource(R.string.stats_empty))
            return@Column
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RetroChip(stringResource(R.string.stats_this_month), selected = thisMonth, onClick = { thisMonth = true })
                    RetroChip(stringResource(R.string.stats_all_time), selected = !thisMonth, onClick = { thisMonth = false })
                }
            }
            if (thisMonth && topArtists.isNotEmpty() && topSongs.isNotEmpty()) {
                item { MonthRecap(month, topArtists.first().label, topSongs.first()) }
            }
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatCard(stringResource(R.string.stats_listened), formatDuration(listenedMs), Modifier.weight(1f))
                    if (!thisMonth) StatCard(stringResource(R.string.stats_plays), stats.totalPlays.toString(), Modifier.weight(1f))
                    StatCard(stringResource(R.string.stats_songs), distinctSongs.toString(), Modifier.weight(1f))
                }
            }
            item { SectionLabel(stringResource(R.string.stats_last_30_days)) }
            item { DailyChart(stats.daily) }
            if (topArtists.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.stats_top_artists)) }
                topArtists.forEach { artist ->
                    item { ArtistBar(artistLabel(artist.label), artist.ms, topArtists.first().ms) }
                }
            }
            if (topSongs.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.stats_top_songs)) }
                val playable = topSongs.mapNotNull { it.song }
                topSongs.forEachIndexed { index, ranked ->
                    val song = ranked.song ?: return@forEachIndexed
                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSongClick(playable, song) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${index + 1}",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = DisplayFont),
                                color = TapeOrange,
                                modifier = Modifier.padding(end = 12.dp),
                            )
                            AlbumArt(song.artworkUri, Modifier.size(44.dp), RoundedCornerShape(4.dp))
                            Column(
                                Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp)
                            ) {
                                Text(titleLabel(song.title), style = MaterialTheme.typography.bodyLarge, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    if (thisMonth) "${artistLabel(song.artist)} · ${formatDuration(ranked.ms)}"
                                    else "${artistLabel(song.artist)} · ${pluralStringResource(R.plurals.plays_count, ranked.plays, ranked.plays)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthRecap(month: YearMonth, artist: String, song: Ranked) {
    val monthName = month.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()))
    Column(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .background(InkSurface, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            stringResource(R.string.stats_month_recap, monthName).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = DisplayFont),
            color = TapeOrange,
        )
        Text(
            stringResource(R.string.stats_top_artist_line, artistLabel(artist)),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            stringResource(R.string.stats_top_song_line, titleLabel(song.label)),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(InkSurface, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = TapeOrange,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
    )
}

/** One bar per day for the last 30 days. */
@Composable
private fun DailyChart(daily: Map<String, Long>) {
    val today = LocalDate.now()
    val values = remember(daily) { (29 downTo 0).map { daily[today.minusDays(it.toLong()).toString()] ?: 0L } }
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    val barColor = TapeOrange
    val emptyColor = InkRaised
    Canvas(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(120.dp)
    ) {
        val gap = 3.dp.toPx()
        val barWidth = (size.width - gap * (values.size - 1)) / values.size
        values.forEachIndexed { i, v ->
            val x = i * (barWidth + gap)
            val h = if (v == 0L) 2.dp.toPx() else (size.height * v / max).coerceAtLeast(3.dp.toPx())
            drawRoundRect(
                color = if (v == 0L) emptyColor else barColor,
                topLeft = Offset(x, size.height - h),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
        }
    }
}

@Composable
private fun ArtistBar(name: String, ms: Long, maxMs: Long) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(formatDuration(ms), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Box(
            Modifier
                .padding(top = 4.dp)
                .fillMaxWidth()
                .height(6.dp)
                .background(InkRaised, RoundedCornerShape(3.dp))
        ) {
            Box(
                Modifier
                    .fillMaxWidth((ms.toFloat() / maxMs.coerceAtLeast(1L)).coerceIn(0.02f, 1f))
                    .fillMaxHeight()
                    .background(TapeOrange, RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
private fun formatDuration(ms: Long): String {
    val minutes = ms / 60_000
    return if (minutes >= 60) stringResource(R.string.stats_hours_minutes, (minutes / 60).toInt(), (minutes % 60).toInt())
    else stringResource(R.string.stats_minutes, minutes.toInt())
}
