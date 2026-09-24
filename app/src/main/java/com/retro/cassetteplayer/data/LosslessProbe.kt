package com.retro.cassetteplayer.data

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Finds lossless audio that the MIME type can't reveal (ALAC inside .m4a looks like AAC):
 * reads only the container header with MediaExtractor. Results are cached per file
 * (id + date added) so each file is probed once; songs identified as lossless during
 * playback are remembered too.
 */
class LosslessProbe(private val context: Context) {

    private val prefs = context.getSharedPreferences("lossless_probe", Context.MODE_PRIVATE)

    private fun key(song: Song) = "${song.id}:${song.dateAdded}"

    /** Ids of songs known to be lossless from the cache (no I/O on the files). */
    fun cachedLosslessIds(songs: List<Song>): Set<Long> =
        songs.filter { prefs.getBoolean(key(it), false) }.mapTo(HashSet()) { it.id }

    /** Probes songs that haven't been checked yet; returns the ids found to be lossless. */
    suspend fun probe(songs: List<Song>): Set<Long> = withContext(Dispatchers.IO) {
        val found = HashSet<Long>()
        val editor = prefs.edit()
        songs.filter { it.needsLosslessProbe && !prefs.contains(key(it)) }.forEach { song ->
            val lossless = isLosslessCodec(song)
            editor.putBoolean(key(song), lossless)
            if (lossless) found += song.id
        }
        editor.apply()
        found
    }

    /** Remembers a song the player decoded as lossless. */
    fun markLossless(song: Song) {
        prefs.edit().putBoolean(key(song), true).apply()
    }

    private fun isLosslessCodec(song: Song): Boolean {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(context, song.uri, null)
            (0 until extractor.trackCount).any { index ->
                val mime = extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME).orEmpty()
                mime.equals("audio/alac", ignoreCase = true) ||
                    mime.equals(MediaFormat.MIMETYPE_AUDIO_FLAC, ignoreCase = true) ||
                    mime.equals(MediaFormat.MIMETYPE_AUDIO_RAW, ignoreCase = true)
            }
        } catch (e: Exception) {
            false
        } finally {
            extractor.release()
        }
    }
}
