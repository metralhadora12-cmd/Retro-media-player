package com.retro.cassetteplayer.playback

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.retro.cassetteplayer.R
import com.retro.cassetteplayer.data.AutoPlaylist
import com.retro.cassetteplayer.data.CollectionKind
import com.retro.cassetteplayer.data.FavoritesRepository
import com.retro.cassetteplayer.data.LibraryCollections
import com.retro.cassetteplayer.data.MusicRepository
import com.retro.cassetteplayer.data.PlayStats
import com.retro.cassetteplayer.data.PlaylistRepository
import com.retro.cassetteplayer.data.Song
import com.retro.cassetteplayer.data.SongCollection
import com.retro.cassetteplayer.data.buildLibrary
import com.retro.cassetteplayer.data.toMediaItem

/**
 * The media tree shown by Android Auto (and other media browsers):
 * root → Playlists / Albums / Artists / Folders → collection → songs.
 *
 * Browsable ids: "root", "cat:<name>", "coll:<collection id>".
 * Playable ids: "song:<song id>@<collection id>", so choosing a song queues its whole
 * collection (see [expand]). Items in the player always use the plain song id.
 */
class LibraryBrowser(private val context: Context) {

    private val repository = MusicRepository(context)
    private var cachedSongs: List<Song> = emptyList()
    private var cachedAt = 0L

    suspend fun songs(): List<Song> {
        val now = SystemClock.elapsedRealtime()
        if (cachedSongs.isEmpty() || now - cachedAt > CACHE_MS) {
            cachedSongs = runCatching { repository.loadSongs() }.getOrDefault(cachedSongs)
            cachedAt = now
        }
        return cachedSongs
    }

    private suspend fun library(): LibraryCollections = buildLibrary(
        songs = songs(),
        userPlaylists = PlaylistRepository(context).playlists.value,
        favoriteIds = FavoritesRepository(context).favorites.value,
        stats = PlayStats.state.value.songs,
    )

    fun root(): MediaItem = browsable(ROOT, context.getString(R.string.app_name), MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)

    suspend fun children(parentId: String): List<MediaItem> {
        val library = library()
        return when {
            parentId == ROOT -> listOf(
                browsable(CAT_PLAYLISTS, context.getString(R.string.filter_playlists), MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS),
                browsable(CAT_ALBUMS, context.getString(R.string.filter_albums), MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS, grid = true),
                browsable(CAT_ARTISTS, context.getString(R.string.filter_artists), MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS),
                browsable(CAT_FOLDERS, context.getString(R.string.filter_folders), MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
            )
            parentId == CAT_PLAYLISTS -> library.playlists.filter { it.songs.isNotEmpty() }.map(::collectionItem)
            parentId == CAT_ALBUMS -> library.albums.sortedBy { it.title.lowercase() }.map(::collectionItem)
            parentId == CAT_ARTISTS -> library.artists.sortedBy { it.title.lowercase() }.map(::collectionItem)
            parentId == CAT_FOLDERS -> library.folders.map(::collectionItem)
            parentId.startsWith(COLL) -> {
                val collection = library.find(parentId.removePrefix(COLL)) ?: return emptyList()
                collection.songs.map { song -> playable(song, collection.id) }
            }
            else -> emptyList()
        }
    }

    suspend fun item(mediaId: String): MediaItem? = when {
        mediaId == ROOT -> root()
        mediaId.startsWith(COLL) -> library().find(mediaId.removePrefix(COLL))?.let(::collectionItem)
        mediaId.startsWith(CAT) -> children(ROOT).firstOrNull { it.mediaId == mediaId }
        else -> songById(songIdOf(mediaId))?.toMediaItem()
    }

    /** Turns browser / voice requests into playable items; items from the app pass through. */
    suspend fun resolve(items: List<MediaItem>): List<MediaItem> = items.flatMap { item ->
        val uri = item.requestMetadata.mediaUri ?: item.localConfiguration?.uri
        when {
            uri != null -> listOf(item.buildUpon().setUri(uri).build())
            item.mediaId.isEmpty() && item.requestMetadata.searchQuery != null ->
                search(item.requestMetadata.searchQuery.orEmpty()).map { it.toMediaItem() }
            else -> listOfNotNull(songById(songIdOf(item.mediaId))?.toMediaItem())
        }
    }

    /**
     * A single song chosen in a browser queues the collection it was listed in, starting
     * at that song. Returns null when [items] is not such a request.
     */
    suspend fun expand(items: List<MediaItem>): Pair<List<MediaItem>, Int>? {
        val id = items.singleOrNull()?.mediaId ?: return null
        if (!id.startsWith(SONG) || '@' !in id) return null
        val songId = songIdOf(id)
        val collection = library().find(id.substringAfter('@')) ?: return null
        val index = collection.songs.indexOfFirst { it.id == songId }.coerceAtLeast(0)
        return collection.songs.map { it.toMediaItem() } to index
    }

    /** Voice search ("play … on Retro Cassette"): title, artist or album matches. */
    suspend fun search(query: String): List<Song> {
        val all = songs()
        val q = query.trim()
        if (q.isEmpty()) return all.shuffled()
        val byArtist = all.filter { it.artist.equals(q, ignoreCase = true) }
        if (byArtist.isNotEmpty()) return byArtist
        val byAlbum = all.filter { it.album.equals(q, ignoreCase = true) }
        if (byAlbum.isNotEmpty()) return byAlbum
        return all.filter {
            it.title.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true) ||
                it.album.contains(q, ignoreCase = true)
        }
    }

    suspend fun songById(id: Long?): Song? = id?.let { songId -> songs().firstOrNull { it.id == songId } }

    private fun songIdOf(mediaId: String): Long? =
        mediaId.removePrefix(SONG).substringBefore('@').toLongOrNull()

    private fun collectionItem(collection: SongCollection): MediaItem {
        val title = when (collection.auto) {
            AutoPlaylist.FAVORITES -> context.getString(R.string.playlist_favorites)
            AutoPlaylist.ALL_SONGS -> context.getString(R.string.playlist_all_songs)
            AutoPlaylist.RECENTLY_ADDED -> context.getString(R.string.playlist_recently_added)
            AutoPlaylist.MOST_PLAYED -> context.getString(R.string.playlist_most_played)
            AutoPlaylist.FORGOTTEN -> context.getString(R.string.playlist_forgotten)
            null -> when (collection.kind) {
                CollectionKind.ALBUM -> collection.title.ifBlank { context.getString(R.string.unknown_album) }
                CollectionKind.ARTIST -> collection.title.ifBlank { context.getString(R.string.unknown_artist) }
                CollectionKind.FOLDER -> collection.title.substringAfterLast('/')
                CollectionKind.PLAYLIST -> collection.title
            }
        }
        val type = when (collection.kind) {
            CollectionKind.ALBUM -> MediaMetadata.MEDIA_TYPE_ALBUM
            CollectionKind.ARTIST -> MediaMetadata.MEDIA_TYPE_ARTIST
            CollectionKind.PLAYLIST -> MediaMetadata.MEDIA_TYPE_PLAYLIST
            CollectionKind.FOLDER -> MediaMetadata.MEDIA_TYPE_FOLDER_MIXED
        }
        return MediaItem.Builder()
            .setMediaId(COLL + collection.id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setSubtitle(if (collection.kind == CollectionKind.ALBUM) collection.artist else null)
                    .setArtworkUri(collection.artworkUris.firstOrNull())
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(type)
                    .build()
            )
            .build()
    }

    private fun playable(song: Song, collectionId: String): MediaItem =
        song.toMediaItem().buildUpon().setMediaId("$SONG${song.id}@$collectionId").build()

    private fun browsable(id: String, title: String, type: Int, grid: Boolean = false): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(type)
                    .setExtras(Bundle().apply {
                        putInt(CONTENT_STYLE_BROWSABLE, if (grid) STYLE_GRID else STYLE_LIST)
                        putInt(CONTENT_STYLE_PLAYABLE, STYLE_LIST)
                    })
                    .build()
            )
            .build()

    companion object {
        const val ROOT = "root"
        private const val CAT = "cat:"
        private const val CAT_PLAYLISTS = "cat:playlists"
        private const val CAT_ALBUMS = "cat:albums"
        private const val CAT_ARTISTS = "cat:artists"
        private const val CAT_FOLDERS = "cat:folders"
        private const val COLL = "coll:"
        private const val SONG = "song:"
        private const val CACHE_MS = 30_000L
        private const val CONTENT_STYLE_BROWSABLE = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
        private const val CONTENT_STYLE_PLAYABLE = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
        private const val STYLE_LIST = 1
        private const val STYLE_GRID = 2
    }
}
