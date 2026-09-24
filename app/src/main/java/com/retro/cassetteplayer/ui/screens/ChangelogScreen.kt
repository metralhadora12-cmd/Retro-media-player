package com.retro.cassetteplayer.ui.screens

import androidx.annotation.ArrayRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.retro.cassetteplayer.R
import com.retro.cassetteplayer.ui.theme.DisplayFont
import com.retro.cassetteplayer.ui.theme.Ink
import com.retro.cassetteplayer.ui.theme.InkRaised
import com.retro.cassetteplayer.ui.theme.TapeOrange
import com.retro.cassetteplayer.ui.theme.TextPrimary
import com.retro.cassetteplayer.ui.theme.TextSecondary

private data class Release(val version: String, @ArrayRes val notes: Int)

/** Newest first; the notes are string arrays so they follow the app language. */
private val releases = listOf(
    Release("2.1.0", R.array.changelog_2_1),
    Release("2.0.0", R.array.changelog_2_0),
    Release("1.9.0", R.array.changelog_1_9),
    Release("1.8.0", R.array.changelog_1_8),
    Release("1.7.0", R.array.changelog_1_7),
    Release("1.6.0", R.array.changelog_1_6),
    Release("1.5.0", R.array.changelog_1_5),
    Release("1.4.0", R.array.changelog_1_4),
    Release("1.3.0", R.array.changelog_1_3),
    Release("1.2.0", R.array.changelog_1_2),
    Release("1.1.0", R.array.changelog_1_1),
    Release("1.0.0", R.array.changelog_1_0),
)

@Composable
fun ChangelogScreen(onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
    ) {
        ScreenTopBar(stringResource(R.string.changelog_title), onBack)
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(releases, key = { it.version }) { release ->
                ReleaseNotes(release, isCurrent = release == releases.first())
            }
        }
    }
}

@Composable
private fun ReleaseNotes(release: Release, isCurrent: Boolean) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.changelog_version, release.version),
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = DisplayFont),
                color = TextPrimary,
            )
            if (isCurrent) {
                Text(
                    stringResource(R.string.changelog_current),
                    style = MaterialTheme.typography.labelSmall,
                    color = Ink,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .background(TapeOrange, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .background(InkRaised.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            stringArrayResource(release.notes).forEach { note ->
                Row {
                    Box(
                        Modifier
                            .padding(top = 7.dp, end = 10.dp)
                            .size(6.dp)
                            .background(TapeOrange, CircleShape)
                    )
                    Text(note, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }
    }
}
