package com.retro.cassetteplayer.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

/** Favourite song ids, most recently liked first, persisted in SharedPreferences. */
class FavoritesRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _favorites = MutableStateFlow(load())
    val favorites: StateFlow<List<Long>> = _favorites.asStateFlow()

    /** Adds or removes [songId]; returns true when it is now a favourite. */
    fun toggle(songId: Long): Boolean {
        val current = _favorites.value
        val nowFavorite = songId !in current
        val updated = if (nowFavorite) listOf(songId) + current else current - songId
        _favorites.value = updated
        prefs.edit().putString(KEY_IDS, JSONArray(updated).toString()).apply()
        return nowFavorite
    }

    private fun load(): List<Long> {
        val raw = prefs.getString(KEY_IDS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { array.getLong(it) }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val PREFS_NAME = "favorites"
        const val KEY_IDS = "song_ids"
    }
}
