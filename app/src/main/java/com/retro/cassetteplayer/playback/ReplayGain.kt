package com.retro.cassetteplayer.playback

import android.content.Context
import android.net.Uri
import kotlin.math.pow

/** ReplayGain values (dB) found in a file's tags. */
data class ReplayGainInfo(val trackGain: Float?, val albumGain: Float?)

/**
 * Reads ReplayGain tags without a full tag parser: the tags live near the start of the
 * file (FLAC/Ogg Vorbis comments, ID3v2 TXXX frames, MP4 "----" atoms), so the first
 * bytes are scanned for "REPLAYGAIN_TRACK_GAIN" / "REPLAYGAIN_ALBUM_GAIN" and the number
 * that follows. Text encoded as UTF-16 is handled by dropping the zero bytes.
 */
object ReplayGain {

    private const val SCAN_BYTES = 1_048_576
    private val cache = HashMap<Uri, ReplayGainInfo>()
    private val trackRegex = Regex("""(?i)replaygain_track_gain[^0-9+\-]{0,24}([+-]?\d{1,2}(?:[.,]\d+)?)\s*dB""")
    private val albumRegex = Regex("""(?i)replaygain_album_gain[^0-9+\-]{0,24}([+-]?\d{1,2}(?:[.,]\d+)?)\s*dB""")

    /** Blocking: call off the main thread. */
    fun read(context: Context, uri: Uri): ReplayGainInfo {
        synchronized(cache) { cache[uri]?.let { return it } }
        val info = runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(SCAN_BYTES)
                var total = 0
                while (total < buffer.size) {
                    val n = input.read(buffer, total, buffer.size - total)
                    if (n <= 0) break
                    total += n
                }
                buffer.copyOf(total)
            } ?: return@runCatching ReplayGainInfo(null, null)
            val text = String(bytes, Charsets.ISO_8859_1).replace("\u0000", "")
            ReplayGainInfo(
                trackGain = trackRegex.find(text)?.groupValues?.get(1)?.replace(',', '.')?.toFloatOrNull(),
                albumGain = albumRegex.find(text)?.groupValues?.get(1)?.replace(',', '.')?.toFloatOrNull(),
            )
        }.getOrDefault(ReplayGainInfo(null, null))
        synchronized(cache) { cache[uri] = info }
        return info
    }

    /**
     * Linear volume for a gain in dB. Only attenuation is possible through the player
     * volume, so positive gains are capped at 1.
     */
    fun volumeFor(gainDb: Float?): Float =
        if (gainDb == null) 1f else 10f.pow(gainDb / 20f).coerceIn(0.05f, 1f)
}
