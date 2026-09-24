package com.retro.cassetteplayer.data

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uri: Uri,
    val artworkUri: Uri?,
)

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val artworkUri: Uri?,
    val songs: List<Song>,
)

/**
 * Builds a [MediaItem] for the session. The URI is also carried in the request metadata
 * because the session rebuilds items from it (see PlaybackService.onAddMediaItems).
 */
fun Song.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setRequestMetadata(
            MediaItem.RequestMetadata.Builder()
                .setMediaUri(uri)
                .build()
        )
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(artworkUri)
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .build()
        )
        .build()
