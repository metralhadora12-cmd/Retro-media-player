package com.retro.cassetteplayer.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** A playlist created by the user. Songs are referenced by their MediaStore id. */
data class UserPlaylist(
    val id: String,
    val name: String,
    val songIds: List<Long>,
    /** Milliseconds since epoch of the last change, used for "Atividade recente". */
    val updatedAt: Long,
)

/** Stores user playlists as JSON in SharedPreferences and exposes them as a flow. */
class PlaylistRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _playlists = MutableStateFlow(load())
    val playlists: StateFlow<List<UserPlaylist>> = _playlists.asStateFlow()

    fun create(name: String, songIds: List<Long> = emptyList()): UserPlaylist {
        val playlist = UserPlaylist(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            songIds = songIds.distinct(),
            updatedAt = System.currentTimeMillis(),
        )
        update { it + playlist }
        return playlist
    }

    fun rename(id: String, name: String) = edit(id) { it.copy(name = name.trim()) }

    fun delete(id: String) = update { list -> list.filterNot { it.id == id } }

    /** Appends songs that are not in the playlist yet; returns how many were added. */
    fun addSongs(id: String, songIds: List<Long>): Int {
        val playlist = _playlists.value.firstOrNull { it.id == id } ?: return 0
        val newIds = songIds.distinct().filterNot { it in playlist.songIds }
        if (newIds.isNotEmpty()) edit(id) { it.copy(songIds = it.songIds + newIds) }
        return newIds.size
    }

    fun removeSong(id: String, songId: Long) = edit(id) { playlist ->
        playlist.copy(songIds = playlist.songIds - songId)
    }

    private fun edit(id: String, transform: (UserPlaylist) -> UserPlaylist) = update { list ->
        list.map { if (it.id == id) transform(it).copy(updatedAt = System.currentTimeMillis()) else it }
    }

    private fun update(transform: (List<UserPlaylist>) -> List<UserPlaylist>) {
        val updated = transform(_playlists.value)
        _playlists.value = updated
        save(updated)
    }

    private fun load(): List<UserPlaylist> {
        val raw = prefs.getString(KEY_PLAYLISTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { i ->
                val obj = array.getJSONObject(i)
                val ids = obj.getJSONArray("songIds")
                UserPlaylist(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    songIds = List(ids.length()) { ids.getLong(it) },
                    updatedAt = obj.optLong("updatedAt"),
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun save(playlists: List<UserPlaylist>) {
        val array = JSONArray()
        playlists.forEach { playlist ->
            array.put(
                JSONObject()
                    .put("id", playlist.id)
                    .put("name", playlist.name)
                    .put("songIds", JSONArray(playlist.songIds))
                    .put("updatedAt", playlist.updatedAt)
            )
        }
        prefs.edit().putString(KEY_PLAYLISTS, array.toString()).apply()
    }

    private companion object {
        const val PREFS_NAME = "user_playlists"
        const val KEY_PLAYLISTS = "playlists"
    }
}
