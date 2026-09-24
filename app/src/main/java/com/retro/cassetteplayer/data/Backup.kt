package com.retro.cassetteplayer.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Backup of user playlists and favourites as JSON. Songs are stored with their
 * MediaStore id *and* identifying tags, because ids differ between devices (and can
 * change after a rescan): on restore a song is matched by id first, then by
 * title + artist (+ album / duration).
 */
object Backup {

    private const val FORMAT = "retro-cassette-backup"
    private const val VERSION = 1

    data class SongRef(
        val id: Long,
        val title: String,
        val artist: String,
        val album: String,
        val durationMs: Long,
    )

    data class Contents(
        val playlists: List<Pair<String, List<SongRef>>>,
        val favorites: List<SongRef>,
    )

    fun export(songs: List<Song>, playlists: List<UserPlaylist>, favoriteIds: List<Long>): String {
        val byId = songs.associateBy { it.id }
        fun ref(id: Long): JSONObject {
            val song = byId[id]
            return JSONObject()
                .put("id", id)
                .put("title", song?.title.orEmpty())
                .put("artist", song?.artist.orEmpty())
                .put("album", song?.album.orEmpty())
                .put("durationMs", song?.durationMs ?: 0L)
        }
        return JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put("favorites", JSONArray(favoriteIds.map(::ref)))
            .put(
                "playlists",
                JSONArray(
                    playlists.map { playlist ->
                        JSONObject()
                            .put("name", playlist.name)
                            .put("songs", JSONArray(playlist.songIds.map(::ref)))
                    }
                ),
            )
            .toString(2)
    }

    /** Returns null when the text is not a backup made by this app. */
    fun parse(text: String): Contents? = runCatching {
        val root = JSONObject(text)
        if (root.optString("format") != FORMAT) return null
        fun refs(array: JSONArray?): List<SongRef> =
            if (array == null) emptyList()
            else List(array.length()) { array.getJSONObject(it) }.map { o ->
                SongRef(
                    id = o.optLong("id", -1L),
                    title = o.optString("title"),
                    artist = o.optString("artist"),
                    album = o.optString("album"),
                    durationMs = o.optLong("durationMs"),
                )
            }
        val playlists = root.optJSONArray("playlists")?.let { array ->
            List(array.length()) { array.getJSONObject(it) }.map { o ->
                o.optString("name") to refs(o.optJSONArray("songs"))
            }
        }.orEmpty()
        Contents(playlists.filter { it.first.isNotBlank() }, refs(root.optJSONArray("favorites")))
    }.getOrNull()

    /** Matches backed-up songs against the current library. */
    class Matcher(songs: List<Song>) {
        private val byId = songs.associateBy { it.id }
        private val byTitleArtist = songs.groupBy { key(it.title, it.artist) }

        fun find(ref: SongRef): Song? {
            byId[ref.id]?.let { song ->
                // Same id is only trusted if the tags still agree (ids are reused across devices)
                if (ref.title.isBlank() || key(song.title, song.artist) == key(ref.title, ref.artist)) return song
            }
            if (ref.title.isBlank()) return null
            val candidates = byTitleArtist[key(ref.title, ref.artist)].orEmpty()
            return candidates.firstOrNull { it.album.equals(ref.album, ignoreCase = true) }
                ?: candidates.minByOrNull { kotlin.math.abs(it.durationMs - ref.durationMs) }
        }

        private fun key(title: String, artist: String) = title.trim().lowercase() + "\u0000" + artist.trim().lowercase()
    }
}
