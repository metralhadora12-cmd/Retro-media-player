package com.retro.cassetteplayer.ui.components

import androidx.compose.runtime.compositionLocalOf
import com.retro.cassetteplayer.data.Song

/** Favourite ids plus the toggle action, available to every song row. */
class FavoritesState(
    val ids: Set<Long>,
    val toggle: (Song) -> Unit,
) {
    fun isFavorite(song: Song) = song.id in ids
}

val LocalFavorites = compositionLocalOf { FavoritesState(emptySet()) {} }

/** Asks to delete a song from the device (confirmation handled by the app shell). */
val LocalDeleteSong = compositionLocalOf<(Song) -> Unit> { {} }

/** Opens the tag editor for a song. */
val LocalEditSong = compositionLocalOf<(Song) -> Unit> { {} }
