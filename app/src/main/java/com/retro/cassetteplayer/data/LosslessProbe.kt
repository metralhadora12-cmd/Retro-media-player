package com.retro.cassetteplayer.data

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Finds lossless audio that the MIME type can't reveal, e.g. ALAC inside .m4a (looks
 * like AAC) or FLAC files the device reports with a generic type. It reads the file
 * header itself (MP4 sample description, FLAC / WAV / AIFF / APE / WavPack magic), since
 * many devices' MediaExtractor don't expose ALAC. Results are cached per file
 * (id + date added) so each file is probed once; songs identified as lossless during
 * playback are remembered too.
 */
class LosslessProbe(private val context: Context) {

    private val prefs = context.getSharedPreferences("lossless_probe", Context.MODE_PRIVATE)

    // "v2": results of the old MediaExtractor-only probe (which missed ALAC) are ignored
    private fun key(song: Song) = "v2:${song.id}:${song.dateAdded}"

    /** Ids of songs known to be lossless from the cache (no I/O on the files). */
    fun cachedLosslessIds(songs: List<Song>): Set<Long> =
        songs.filter { prefs.getBoolean(key(it), false) }.mapTo(HashSet()) { it.id }

    /** Probes songs that haven't been checked yet; returns the ids found to be lossless. */
    suspend fun probe(songs: List<Song>): Set<Long> = withContext(Dispatchers.IO) {
        val found = HashSet<Long>()
        val editor = prefs.edit()
        songs.filter { !it.isLossless && !prefs.contains(key(it)) }.forEachIndexed { i, song ->
            val lossless = isLossless(song)
            editor.putBoolean(key(song), lossless)
            if (lossless) found += song.id
            if (i % 50 == 49) editor.apply()
        }
        editor.apply()
        found
    }

    /** Remembers a song the player decoded as lossless. */
    fun markLossless(song: Song) {
        prefs.edit().putBoolean(key(song), true).apply()
    }

    private fun isLossless(song: Song): Boolean {
        val fromHeader = runCatching {
            context.contentResolver.openFileDescriptor(song.uri, "r")?.use { pfd ->
                FileInputStream(pfd.fileDescriptor).channel.use { losslessHeader(it) }
            }
        }.getOrNull()
        return fromHeader ?: (song.needsLosslessProbe && extractorSaysLossless(song))
    }

    /** true / false when the header is recognised, null when it can't tell. */
    private fun losslessHeader(channel: FileChannel): Boolean? {
        val head = read(channel, 0, 64) ?: return null
        fun at(offset: Int, text: String) =
            head.size >= offset + text.length && text.indices.all { head[offset + it] == text[it].code.toByte() }

        // Skip an ID3v2 tag (FLAC files sometimes carry one)
        if (at(0, "ID3") && head.size >= 10) {
            val size = (head[6].toInt() and 0x7F shl 21) or (head[7].toInt() and 0x7F shl 14) or
                (head[8].toInt() and 0x7F shl 7) or (head[9].toInt() and 0x7F)
            val afterTag = read(channel, 10L + size, 4) ?: return null
            return String(afterTag, Charsets.ISO_8859_1) == "fLaC"
        }
        return when {
            at(0, "fLaC") -> true
            at(0, "MAC ") -> true // Monkey's Audio
            at(0, "wvpk") -> true // WavPack
            at(0, "RIFF") && at(8, "WAVE") -> wavIsPcm(channel)
            at(0, "FORM") && (at(8, "AIFF") || at(8, "AIFC")) -> true
            at(0, "OggS") -> {
                val page = read(channel, 0, 256) ?: return null
                String(page, Charsets.ISO_8859_1).contains("\u007FFLAC")
            }
            at(4, "ftyp") -> mp4HasLosslessTrack(channel)
            else -> false
        }
    }

    /** WAV format tag 1 (PCM), 3 (float) or 0xFFFE (extensible) is lossless. */
    private fun wavIsPcm(channel: FileChannel): Boolean {
        var pos = 12L
        repeat(16) {
            val header = read(channel, pos, 10) ?: return false
            val id = String(header, 0, 4, Charsets.ISO_8859_1)
            val size = ByteBuffer.wrap(header, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL
            if (id == "fmt ") {
                val tag = ByteBuffer.wrap(header, 8, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF
                return tag == 1 || tag == 3 || tag == 0xFFFE
            }
            pos += 8 + size + (size and 1)
        }
        return false
    }

    /**
     * Walks the top-level MP4 boxes to "moov" (it may sit at the end of the file) and
     * looks for an "alac" or "fLaC" sample entry inside it.
     */
    private fun mp4HasLosslessTrack(channel: FileChannel): Boolean {
        val fileSize = channel.size()
        var pos = 0L
        while (pos + 8 <= fileSize) {
            val header = read(channel, pos, 16) ?: return false
            var size = ByteBuffer.wrap(header, 0, 4).int.toLong() and 0xFFFFFFFFL
            val type = String(header, 4, 4, Charsets.ISO_8859_1)
            var headerSize = 8L
            if (size == 1L && header.size >= 16) {
                size = ByteBuffer.wrap(header, 8, 8).long
                headerSize = 16L
            } else if (size == 0L) {
                size = fileSize - pos
            }
            if (size < headerSize) return false
            if (type == "moov") {
                val length = (size - headerSize).coerceAtMost(MAX_MOOV_BYTES.toLong()).toInt()
                val moov = read(channel, pos + headerSize, length) ?: return false
                return containsAscii(moov, "alac") || containsAscii(moov, "fLaC")
            }
            pos += size
        }
        return false
    }

    private fun containsAscii(bytes: ByteArray, text: String): Boolean {
        val pattern = text.toByteArray(Charsets.ISO_8859_1)
        outer@ for (i in 0..bytes.size - pattern.size) {
            for (j in pattern.indices) if (bytes[i + j] != pattern[j]) continue@outer
            return true
        }
        return false
    }

    private fun read(channel: FileChannel, position: Long, length: Int): ByteArray? {
        if (position < 0 || length <= 0) return null
        val buffer = ByteBuffer.allocate(length)
        var total = 0
        while (buffer.hasRemaining()) {
            val n = channel.read(buffer, position + total)
            if (n <= 0) break
            total += n
        }
        return if (total == 0) null else buffer.array().copyOf(total)
    }

    /** Fallback: the platform extractor (works for ALAC on some devices). */
    private fun extractorSaysLossless(song: Song): Boolean {
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

    private companion object {
        const val MAX_MOOV_BYTES = 8 * 1024 * 1024
    }
}
