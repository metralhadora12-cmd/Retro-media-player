package com.retro.cassetteplayer.data

import android.net.Uri

enum class CollectionKind { PLAYLIST, ALBUM, ARTIST }

/** A playable group of songs shown in Home / Library: an album, an artist or an automatic playlist. */
data class SongCollection(
    val id: String,
    val kind: CollectionKind,
    val title: String,
    val subtitle: String,
    /** One cover, or up to four for a 2x2 collage. */
    val artworkUris: List<Uri>,
    val songs: List<Song>,
    val lastAdded: Long,
    /** Set when this is a playlist the user created (and can edit). */
    val userPlaylistId: String? = null,
)

data class LibraryCollections(
    val playlists: List<SongCollection> = emptyList(),
    val albums: List<SongCollection> = emptyList(),
    val artists: List<SongCollection> = emptyList(),
) {
    val all: List<SongCollection> get() = playlists + albums + artists
    val userPlaylists: List<SongCollection> get() = playlists.filter { it.userPlaylistId != null }
    fun find(id: String): SongCollection? = all.firstOrNull { it.id == id }
}

private fun List<Song>.collage(): List<Uri> =
    distinctBy { it.albumId }.mapNotNull { it.artworkUri }.take(4)

private fun tracks(count: Int) = if (count == 1) "1 faixa" else "$count faixas"

fun buildLibrary(songs: List<Song>, userPlaylists: List<UserPlaylist> = emptyList()): LibraryCollections {
    val songsById = songs.associateBy { it.id }
    val created = userPlaylists.sortedByDescending { it.updatedAt }.map { playlist ->
        val tracks = playlist.songIds.mapNotNull { songsById[it] }
        SongCollection(
            id = "user:${playlist.id}",
            kind = CollectionKind.PLAYLIST,
            title = playlist.name,
            subtitle = "Playlist • ${tracks(tracks.size)}",
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
            subtitle = "Álbum • ${first.artist} • ${tracks(tracks.size)}",
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
            subtitle = "Artista • ${tracks(tracks.size)}",
            artworkUris = listOfNotNull(tracks.first().artworkUri),
            songs = tracks,
            lastAdded = tracks.maxOf { it.dateAdded },
        )
    }

    val recent = songs.sortedByDescending { it.dateAdded }.take(50)
    val playlists = listOf(
        SongCollection(
            id = "playlist:all",
            kind = CollectionKind.PLAYLIST,
            title = "Todas as músicas",
            subtitle = "Playlist automática • ${tracks(songs.size)}",
            artworkUris = songs.collage(),
            songs = songs,
            lastAdded = Long.MAX_VALUE,
        ),
        SongCollection(
            id = "playlist:recent",
            kind = CollectionKind.PLAYLIST,
            title = "Adicionadas recentemente",
            subtitle = "Playlist automática • ${tracks(recent.size)}",
            artworkUris = recent.collage(),
            songs = recent,
            lastAdded = Long.MAX_VALUE - 1,
        ),
    )

    return LibraryCollections(playlists + created, albums, artists)
}
