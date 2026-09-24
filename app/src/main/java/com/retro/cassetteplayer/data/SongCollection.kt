package com.retro.cassetteplayer.data

import android.net.Uri

enum class CollectionKind { PLAYLIST, ALBUM, ARTIST, FOLDER }

/** Playlists the app builds by itself; their names are localised in the UI. */
enum class AutoPlaylist { FAVORITES, ALL_SONGS, RECENTLY_ADDED, MOST_PLAYED, FORGOTTEN }

/**
 * A playable group of songs shown in Home / Library: an album, an artist or a playlist.
 * Only raw data lives here (names, counts); display text is localised in the UI layer
 * so switching language never leaves stale strings behind.
 */
data class SongCollection(
    val id: String,
    val kind: CollectionKind,
    /** Album / artist / user playlist name as stored; empty for unknown or automatic playlists. */
    val title: String,
    /** Album artist (albums only). */
    val artist: String = "",
    /** One cover, or up to four for a 2x2 collage. */
    val artworkUris: List<Uri>,
    val songs: List<Song>,
    val lastAdded: Long,
    /** Set when this is a playlist the user created (and can edit). */
    val userPlaylistId: String? = null,
    /** Set for the playlists the app builds automatically. */
    val auto: AutoPlaylist? = null,
) {
    val isFavorites: Boolean get() = auto == AutoPlaylist.FAVORITES

    /** Every track is lossless (shows the HQ badge). */
    val isLossless: Boolean get() = songs.isNotEmpty() && songs.all { it.isLossless }
}

data class LibraryCollections(
    val playlists: List<SongCollection> = emptyList(),
    val albums: List<SongCollection> = emptyList(),
    val artists: List<SongCollection> = emptyList(),
    val folders: List<SongCollection> = emptyList(),
) {
    val all: List<SongCollection> get() = playlists + albums + artists + folders
    val userPlaylists: List<SongCollection> get() = playlists.filter { it.userPlaylistId != null }
    fun find(id: String): SongCollection? = all.firstOrNull { it.id == id }
}

const val FAVORITES_ID = "playlist:favorites"

private const val SMART_PLAYLIST_SIZE = 50
/** "Não ouço há tempo": played before, but not in the last 30 days. */
private const val FORGOTTEN_AFTER_MS = 30L * 24 * 60 * 60 * 1000

private fun List<Song>.collage(): List<Uri> =
    distinctBy { it.albumId }.mapNotNull { it.artworkUri }.take(4)

fun buildLibrary(
    songs: List<Song>,
    userPlaylists: List<UserPlaylist> = emptyList(),
    favoriteIds: List<Long> = emptyList(),
    stats: Map<Long, SongStats> = emptyMap(),
    now: Long = System.currentTimeMillis(),
): LibraryCollections {
    val songsById = songs.associateBy { it.id }
    val created = userPlaylists.sortedByDescending { it.updatedAt }.map { playlist ->
        val tracks = playlist.songIds.mapNotNull { songsById[it] }
        SongCollection(
            id = "user:${playlist.id}",
            kind = CollectionKind.PLAYLIST,
            title = playlist.name,
            artworkUris = tracks.collage(),
            songs = tracks,
            lastAdded = playlist.updatedAt,
            userPlaylistId = playlist.id,
        )
    }
    if (songs.isEmpty()) return LibraryCollections(playlists = created)

    val albums = songs.groupBy { it.albumId }.map { (id, tracks) ->
        val first = tracks.first()
        SongCollection(
            id = "album:$id",
            kind = CollectionKind.ALBUM,
            title = first.album,
            artist = first.artist,
            artworkUris = listOfNotNull(first.artworkUri),
            songs = tracks,
            lastAdded = tracks.maxOf { it.dateAdded },
        )
    }

    val artists = songs.groupBy { it.artist }.map { (name, tracks) ->
        SongCollection(
            id = "artist:$name",
            kind = CollectionKind.ARTIST,
            title = name,
            artworkUris = listOfNotNull(tracks.first().artworkUri),
            songs = tracks,
            lastAdded = tracks.maxOf { it.dateAdded },
        )
    }

    val folders = songs.filter { it.folder.isNotEmpty() }.groupBy { it.folder }.map { (path, tracks) ->
        SongCollection(
            id = "folder:$path",
            kind = CollectionKind.FOLDER,
            title = path,
            artworkUris = tracks.collage(),
            songs = tracks.sortedBy { it.fileName.lowercase() },
            lastAdded = tracks.maxOf { it.dateAdded },
        )
    }.sortedBy { it.title.lowercase() }

    val favorites = favoriteIds.mapNotNull { songsById[it] }
    val recent = songs.sortedByDescending { it.dateAdded }.take(50)
    val playlists = listOf(
        SongCollection(
            id = FAVORITES_ID,
            kind = CollectionKind.PLAYLIST,
            title = "",
            artworkUris = favorites.collage(),
            songs = favorites,
            lastAdded = Long.MAX_VALUE,
            auto = AutoPlaylist.FAVORITES,
        ),
        SongCollection(
            id = "playlist:all",
            kind = CollectionKind.PLAYLIST,
            title = "",
            artworkUris = songs.collage(),
            songs = songs,
            lastAdded = Long.MAX_VALUE,
            auto = AutoPlaylist.ALL_SONGS,
        ),
        SongCollection(
            id = "playlist:recent",
            kind = CollectionKind.PLAYLIST,
            title = "",
            artworkUris = recent.collage(),
            songs = recent,
            lastAdded = Long.MAX_VALUE - 1,
            auto = AutoPlaylist.RECENTLY_ADDED,
        ),
    )

    // Smart playlists from the listening history (only shown once they have songs)
    val mostPlayed = songs.filter { (stats[it.id]?.plays ?: 0) > 0 }
        .sortedByDescending { stats[it.id]?.plays ?: 0 }
        .take(SMART_PLAYLIST_SIZE)
    val forgotten = songs.filter { song ->
        val last = stats[song.id]?.lastPlayed ?: 0L
        last > 0 && now - last > FORGOTTEN_AFTER_MS
    }.sortedBy { stats[it.id]?.lastPlayed ?: 0L }.take(SMART_PLAYLIST_SIZE)
    val smart = listOfNotNull(
        mostPlayed.takeIf { it.isNotEmpty() }?.let {
            SongCollection(
                id = "playlist:most-played",
                kind = CollectionKind.PLAYLIST,
                title = "",
                artworkUris = it.collage(),
                songs = it,
                lastAdded = Long.MAX_VALUE - 2,
                auto = AutoPlaylist.MOST_PLAYED,
            )
        },
        forgotten.takeIf { it.isNotEmpty() }?.let {
            SongCollection(
                id = "playlist:forgotten",
                kind = CollectionKind.PLAYLIST,
                title = "",
                artworkUris = it.collage(),
                songs = it,
                lastAdded = Long.MAX_VALUE - 3,
                auto = AutoPlaylist.FORGOTTEN,
            )
        },
    )

    return LibraryCollections(playlists + smart + created, albums, artists, folders)
}
