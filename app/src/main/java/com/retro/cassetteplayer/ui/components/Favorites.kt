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
