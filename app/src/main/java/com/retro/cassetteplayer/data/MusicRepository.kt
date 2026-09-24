package com.retro.cassetteplayer.data

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Result of trying to delete a song file. */
sealed interface DeleteOutcome {
    data object Deleted : DeleteOutcome
    /** The system must confirm first; launch this and call again / finish on RESULT_OK. */
    class NeedsConfirmation(val intentSender: IntentSender) : DeleteOutcome
    data object Failed : DeleteOutcome
}

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
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DISPLAY_NAME,
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
        context.contentResolver.query(collection, projection + pathColumn, selection, selectionArgs, sortOrder)
            ?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val pathCol = cursor.getColumnIndex(pathColumn)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val albumId = cursor.getLong(albumIdCol)
                    songs += Song(
                        id = id,
                        title = cursor.getString(titleCol).orEmptyIfUnknown(),
                        artist = cursor.getString(artistCol).orEmptyIfUnknown(),
                        album = cursor.getString(albumCol).orEmptyIfUnknown(),
                        albumId = albumId,
                        durationMs = cursor.getLong(durationCol),
                        uri = ContentUris.withAppendedId(collection, id),
                        artworkUri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId),
                        dateAdded = cursor.getLong(dateAddedCol),
                        mimeType = cursor.getString(mimeCol).orEmpty(),
                        fileName = cursor.getString(nameCol).orEmpty(),
                        folder = if (pathCol >= 0) folderOf(cursor.getString(pathCol)) else "",
                    )
                }
            }
        songs
    }

    /**
     * Deletes the song's file. Android 11+ always goes through the system confirmation
     * (createDeleteRequest); on Android 10 a RecoverableSecurityException carries the
     * confirmation for files the app doesn't own; older versions delete directly.
     */
    fun requestDelete(song: Song): DeleteOutcome {
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val request = MediaStore.createDeleteRequest(resolver, listOf(song.uri))
            return DeleteOutcome.NeedsConfirmation(request.intentSender)
        }
        return try {
            if (resolver.delete(song.uri, null, null) > 0) DeleteOutcome.Deleted else DeleteOutcome.Failed
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                DeleteOutcome.NeedsConfirmation(e.userAction.actionIntent.intentSender)
            } else {
                DeleteOutcome.Failed
            }
        }
    }

    /** "Music/Rock/" (RELATIVE_PATH) or "/storage/emulated/0/Music/Rock/a.mp3" (DATA) → "Music/Rock". */
    private fun folderOf(path: String?): String {
        if (path.isNullOrBlank()) return ""
        val dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) path else path.substringBeforeLast('/', "")
        return dir.removePrefix("/storage/emulated/0/").trim('/')
    }

    /** Unknown values become "" and are shown with a localised label by the UI. */
    private fun String?.orEmptyIfUnknown(): String =
        if (isNullOrBlank() || this == MediaStore.UNKNOWN_STRING) "" else this

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
