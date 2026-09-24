@file:OptIn(ExperimentalComposeUiApi::class)

package com.retro.cassetteplayer.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.retro.cassetteplayer.R
import com.retro.cassetteplayer.data.AutoPlaylist
import com.retro.cassetteplayer.data.CollectionKind
import com.retro.cassetteplayer.data.Song
import com.retro.cassetteplayer.data.SongCollection

// Localised display text for library data (the data layer keeps raw values only).

@Composable
fun titleLabel(title: String): String = title.ifBlank { stringResource(R.string.unknown_title) }

@Composable
fun artistLabel(artist: String): String = artist.ifBlank { stringResource(R.string.unknown_artist) }

@Composable
fun albumLabel(album: String): String = album.ifBlank { stringResource(R.string.unknown_album) }

@Composable
fun Song.titleLabel(): String = titleLabel(title)

@Composable
fun Song.artistLabel(): String = artistLabel(artist)

@Composable
fun Song.albumLabel(): String = albumLabel(album)

@Composable
fun tracksLabel(count: Int): String = pluralStringResource(R.plurals.tracks_count, count, count)

@Composable
fun SongCollection.titleLabel(): String = when (auto) {
    AutoPlaylist.FAVORITES -> stringResource(R.string.playlist_favorites)
    AutoPlaylist.ALL_SONGS -> stringResource(R.string.playlist_all_songs)
    AutoPlaylist.RECENTLY_ADDED -> stringResource(R.string.playlist_recently_added)
    AutoPlaylist.MOST_PLAYED -> stringResource(R.string.playlist_most_played)
    AutoPlaylist.FORGOTTEN -> stringResource(R.string.playlist_forgotten)
    null -> when (kind) {
        CollectionKind.ALBUM -> albumLabel(title)
        CollectionKind.ARTIST -> artistLabel(title)
        CollectionKind.PLAYLIST -> title
        CollectionKind.FOLDER -> title.substringAfterLast('/')
    }
}

@Composable
fun SongCollection.subtitleLabel(): String {
    val tracks = tracksLabel(songs.size)
    return when {
        auto != null -> stringResource(R.string.subtitle_auto_playlist, tracks)
        kind == CollectionKind.ALBUM -> stringResource(R.string.subtitle_album, artistLabel(artist), tracks)
        kind == CollectionKind.ARTIST -> stringResource(R.string.subtitle_artist, tracks)
        kind == CollectionKind.FOLDER -> stringResource(R.string.subtitle_folder, title, tracks)
        else -> stringResource(R.string.subtitle_playlist, tracks)
    }
}
