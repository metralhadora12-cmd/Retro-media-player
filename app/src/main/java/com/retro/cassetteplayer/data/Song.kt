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
    /** Seconds since epoch, from MediaStore.DATE_ADDED. */
    val dateAdded: Long,
    /** MediaStore MIME type, e.g. "audio/flac". */
    val mimeType: String = "",
    /** File name, e.g. "01 Song.flac" (used for its extension). */
    val fileName: String = "",
    /** Set when the codec was found to be lossless by probing / playing the file (e.g. ALAC in .m4a). */
    val probedLossless: Boolean = false,
) {
    /** Short container/codec label for lossless files (FLAC, WAV, …), or null. */
    val losslessLabel: String?
        get() = losslessLabel(mimeType) ?: losslessLabelForExtension(fileName) ?: if (probedLossless) "ALAC" else null
    val isLossless: Boolean get() = losslessLabel != null

    /** Containers that may hold either lossy or lossless audio and need probing (M4A: AAC or ALAC). */
    val needsLosslessProbe: Boolean
        get() = losslessLabel == null && (
            mimeType.lowercase() in setOf("audio/mp4", "audio/m4a", "audio/x-m4a", "audio/mp4a-latm", "audio/alac") ||
                fileName.substringAfterLast('.', "").lowercase() in setOf("m4a", "mp4", "alac", "caf")
            )
}

/** Lossless formats recognised by the file extension (some devices report generic MIME types). */
fun losslessLabelForExtension(fileName: String): String? = when (fileName.substringAfterLast('.', "").lowercase()) {
    "flac" -> "FLAC"
    "wav", "wave" -> "WAV"
    "aif", "aiff", "aifc" -> "AIFF"
    "ape" -> "APE"
    "wv" -> "WV"
    "dsf", "dff" -> "DSD"
    else -> null
}

/**
 * MIME types that are always lossless. (M4A can hold either AAC or ALAC, so it is only
 * recognised as lossless at playback time from the decoded track.)
 */
fun losslessLabel(mimeType: String): String? = when (mimeType.lowercase()) {
    "audio/flac", "audio/x-flac" -> "FLAC"
    "audio/wav", "audio/x-wav", "audio/wave", "audio/vnd.wave" -> "WAV"
    "audio/aiff", "audio/x-aiff" -> "AIFF"
    "audio/alac", "audio/x-alac" -> "ALAC"
    "audio/x-ape", "audio/ape" -> "APE"
    "audio/x-wavpack", "audio/wavpack" -> "WV"
    else -> null
}

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
