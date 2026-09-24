package com.retro.cassetteplayer.data

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.TagOptionSingleton
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.coroutines.resume

/** Tag values as read from (or to be written to) an audio file. */
data class SongTags(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumArtist: String = "",
    val year: String = "",
    val track: String = "",
    val genre: String = "",
    val artwork: ByteArray? = null,
    val artworkMime: String = "image/jpeg",
)

/** Outcome of writing tags. */
sealed interface WriteResult {
    data object Saved : WriteResult
    data object Failed : WriteResult
    /** Android 10: the system must confirm write access first; retry after RESULT_OK. */
    class NeedsPermission(val intentSender: IntentSender) : WriteResult
}

/** What to do with the embedded cover when saving. */
sealed interface ArtworkChange {
    data object Keep : ArtworkChange
    data object Remove : ArtworkChange
    class Set(val bytes: ByteArray, val mime: String) : ArtworkChange
}

/**
 * Reads and writes tags directly in the audio files with JAudioTagger. Android only
 * gives stream access to other apps' media, so each file is copied to the cache, edited
 * there, and streamed back through the ContentResolver; MediaStore then rescans it.
 */
class TagEditor(private val context: Context) {

    init {
        TagOptionSingleton.getInstance().isAndroid = true
        Logger.getLogger("org.jaudiotagger").level = Level.OFF
    }

    /**
     * On Android 11+ the user must allow writing to files the app doesn't own. Returns the
     * system confirmation to launch, or null when no confirmation is needed.
     */
    fun writeRequest(songs: List<Song>): IntentSender? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createWriteRequest(context.contentResolver, songs.map { it.uri }).intentSender
        } else {
            null
        }

    suspend fun read(song: Song): SongTags? = withContext(Dispatchers.IO) {
        val temp = copyToCache(song) ?: return@withContext null
        try {
            val tag = AudioFileIO.read(temp).tag ?: return@withContext SongTags()
            val artwork = tag.firstArtwork
            SongTags(
                title = tag.value(FieldKey.TITLE),
                artist = tag.value(FieldKey.ARTIST),
                album = tag.value(FieldKey.ALBUM),
                albumArtist = tag.value(FieldKey.ALBUM_ARTIST),
                year = tag.value(FieldKey.YEAR),
                track = tag.value(FieldKey.TRACK),
                genre = tag.value(FieldKey.GENRE),
                artwork = artwork?.binaryData,
                artworkMime = artwork?.mimeType?.takeIf { it.isNotBlank() } ?: "image/jpeg",
            )
        } catch (e: Exception) {
            null
        } finally {
            temp.delete()
        }
    }

    /** Lyrics embedded in the file's tags (plain text or LRC), if any. */
    suspend fun readLyrics(song: Song): String? = withContext(Dispatchers.IO) {
        val temp = copyToCache(song) ?: return@withContext null
        try {
            AudioFileIO.read(temp).tag?.value(FieldKey.LYRICS)?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        } finally {
            temp.delete()
        }
    }

    /**
     * Writes [tags] into [song] (text fields) and applies [artwork] to it and to every
     * song in [artworkAlsoFor].
     */
    suspend fun write(
        song: Song,
        tags: SongTags,
        artwork: ArtworkChange,
        artworkAlsoFor: List<Song> = emptyList(),
    ): WriteResult = withContext(Dispatchers.IO) {
        try {
            if (writeAll(song, tags, artwork, artworkAlsoFor)) WriteResult.Saved else WriteResult.Failed
        } catch (e: PermissionNeeded) {
            WriteResult.NeedsPermission(e.intentSender)
        }
    }

    private class PermissionNeeded(val intentSender: IntentSender) : Exception()

    private suspend fun writeAll(
        song: Song,
        tags: SongTags,
        artwork: ArtworkChange,
        artworkAlsoFor: List<Song>,
    ): Boolean {
        var ok = editFile(song) { tag ->
            tag.put(FieldKey.TITLE, tags.title)
            tag.put(FieldKey.ARTIST, tags.artist)
            tag.put(FieldKey.ALBUM, tags.album)
            tag.put(FieldKey.ALBUM_ARTIST, tags.albumArtist)
            tag.put(FieldKey.YEAR, tags.year)
            tag.put(FieldKey.TRACK, tags.track)
            tag.put(FieldKey.GENRE, tags.genre)
            applyArtwork(tag, artwork)
        }
        if (artwork !is ArtworkChange.Keep) {
            artworkAlsoFor.filter { it.id != song.id }.forEach { other ->
                ok = editFile(other) { tag -> applyArtwork(tag, artwork) } && ok
            }
        }
        return ok
    }

    private suspend fun editFile(song: Song, edit: (org.jaudiotagger.tag.Tag) -> Unit): Boolean {
        val temp = copyToCache(song) ?: return false
        return try {
            val audioFile = AudioFileIO.read(temp)
            edit(audioFile.tagOrCreateAndSetDefault)
            audioFile.commit()
            context.contentResolver.openOutputStream(song.uri, "wt")?.use { out ->
                temp.inputStream().use { it.copyTo(out) }
            } ?: return false
            rescan(song)
            true
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                throw PermissionNeeded(e.userAction.actionIntent.intentSender)
            }
            false
        } catch (e: Exception) {
            false
        } finally {
            temp.delete()
        }
    }

    private fun applyArtwork(tag: org.jaudiotagger.tag.Tag, change: ArtworkChange) {
        when (change) {
            ArtworkChange.Keep -> Unit
            ArtworkChange.Remove -> runCatching { tag.deleteArtworkField() }
            is ArtworkChange.Set -> {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(change.bytes, 0, change.bytes.size, bounds)
                val artwork = ArtworkFactory.getNew().apply {
                    binaryData = change.bytes
                    mimeType = change.mime
                    pictureType = FRONT_COVER
                    description = ""
                    width = bounds.outWidth.coerceAtLeast(0)
                    height = bounds.outHeight.coerceAtLeast(0)
                }
                runCatching { tag.deleteArtworkField() }
                tag.setField(artwork)
            }
        }
    }

    /** Copies the song into the cache with its real extension (JAudioTagger picks the format by it). */
    private fun copyToCache(song: Song): File? = runCatching {
        val dir = File(context.cacheDir, "tag-edit").apply { mkdirs() }
        val temp = File(dir, "edit-${song.id}.${extensionOf(song)}")
        context.contentResolver.openInputStream(song.uri)?.use { input ->
            temp.outputStream().use { input.copyTo(it) }
        } ?: return null
        temp
    }.getOrNull()

    private fun extensionOf(song: Song): String {
        val name = context.contentResolver.query(
            song.uri, arrayOf(MediaStore.Audio.Media.DISPLAY_NAME), null, null, null,
        )?.use { if (it.moveToFirst()) it.getString(0) else null }
        return name?.substringAfterLast('.', "")?.lowercase()?.takeIf { it.isNotBlank() }
            ?: when (song.mimeType.lowercase()) {
                "audio/flac", "audio/x-flac" -> "flac"
                "audio/mp4", "audio/m4a", "audio/x-m4a", "audio/aac" -> "m4a"
                "audio/ogg", "application/ogg" -> "ogg"
                "audio/wav", "audio/x-wav", "audio/wave" -> "wav"
                else -> "mp3"
            }
    }

    /** Asks MediaStore to re-read the file so the library shows the new tags. */
    private suspend fun rescan(song: Song) {
        @Suppress("DEPRECATION")
        val path = context.contentResolver.query(
            song.uri, arrayOf(MediaStore.Audio.Media.DATA), null, null, null,
        )?.use { if (it.moveToFirst()) it.getString(0) else null } ?: return
        suspendCancellableCoroutine { cont ->
            MediaScannerConnection.scanFile(context, arrayOf(path), null) { _, _ ->
                if (cont.isActive) cont.resume(Unit)
            }
        }
    }

    private fun org.jaudiotagger.tag.Tag.value(key: FieldKey): String =
        runCatching { getFirst(key) }.getOrNull().orEmpty()

    /** Sets a text field, or removes it when [value] is blank. */
    private fun org.jaudiotagger.tag.Tag.put(key: FieldKey, value: String) {
        runCatching {
            if (value.isBlank()) deleteField(key) else setField(key, value.trim())
        }
    }

    private companion object {
        /** ID3 / FLAC picture type "Cover (front)". */
        const val FRONT_COVER = 3
    }
}
