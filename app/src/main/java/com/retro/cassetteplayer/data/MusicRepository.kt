package com.retro.cassetteplayer.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads local audio files through the MediaStore API. */
class MusicRepository(private val context: Context) {

    suspend fun loadSongs(): List<Song> = withContext(Dispatchers.IO) {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
        )
        // Skip voice notes and audio saved by messaging apps (WhatsApp, Telegram, …).
        val pathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.RELATIVE_PATH
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Audio.Media.DATA
        }
        val exclusions = EXCLUDED_PATH_FRAGMENTS.joinToString(" AND ") { "$pathColumn NOT LIKE ?" }
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ($pathColumn IS NULL OR ($exclusions))"
        val selectionArgs = EXCLUDED_PATH_FRAGMENTS.map { "%$it%" }.toTypedArray()
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val songs = mutableListOf<Song>()
        context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)
            ?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val albumId = cursor.getLong(albumIdCol)
                    songs += Song(
                        id = id,
                        title = cursor.getString(titleCol).orUnknown("Faixa sem título"),
                        artist = cursor.getString(artistCol).orUnknown("Artista desconhecido"),
                        album = cursor.getString(albumCol).orUnknown("Álbum desconhecido"),
                        albumId = albumId,
                        durationMs = cursor.getLong(durationCol),
                        uri = ContentUris.withAppendedId(collection, id),
                        artworkUri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId),
                        dateAdded = cursor.getLong(dateAddedCol),
                    )
                }
            }
        songs
    }

    private fun String?.orUnknown(fallback: String): String =
        if (isNullOrBlank() || this == MediaStore.UNKNOWN_STRING) fallback else this

    private companion object {
        val ALBUM_ART_URI: Uri = Uri.parse("content://media/external/audio/albumart")

        /**
         * Folder fragments of messaging apps whose audio should not show up as music.
         * SQLite LIKE is case-insensitive for ASCII, so "whatsapp" also matches
         * "WhatsApp Audio", "WhatsApp Business" and "Android/media/com.whatsapp/…".
         */
        val EXCLUDED_PATH_FRAGMENTS = listOf(
            "whatsapp",
            "telegram",
            "org.thunderdog.challegram",
            "com.facebook.orca",
        )
    }
}
