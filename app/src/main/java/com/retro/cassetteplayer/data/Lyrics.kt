package com.retro.cassetteplayer.data

import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

data class LyricLine(val timeMs: Long, val text: String)

enum class LyricsSource { FILE, LRC_FILE, ONLINE }

/** [synced] when timestamps are available (LRC), otherwise [plain] text. */
data class Lyrics(
    val synced: List<LyricLine>?,
    val plain: String?,
    val source: LyricsSource,
)

sealed interface LyricsResult {
    class Found(val lyrics: Lyrics) : LyricsResult
    data object NotFound : LyricsResult
    /** Nothing local and the online lookup failed (no connection). */
    data object Offline : LyricsResult
}

/** Parses LRC ("[mm:ss.xx] line"), including several timestamps on one line. */
object LrcParser {
    private val timeTag = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")

    fun parse(text: String): List<LyricLine>? {
        val lines = mutableListOf<LyricLine>()
        text.lineSequence().forEach { raw ->
            val stamps = timeTag.findAll(raw).toList()
            if (stamps.isEmpty()) return@forEach
            val lyric = raw.substring(stamps.last().range.last + 1).trim()
            stamps.forEach { m ->
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val frac = m.groupValues[3]
                val ms = when (frac.length) {
                    0 -> 0L
                    1 -> frac.toLong() * 100
                    2 -> frac.toLong() * 10
                    else -> frac.take(3).toLong()
                }
                lines += LyricLine(min * 60_000 + sec * 1_000 + ms, lyric)
            }
        }
        return lines.sortedBy { it.timeMs }.takeIf { it.size >= 2 }
    }
}

/**
 * Finds lyrics for a song: embedded tag → sidecar .lrc → LRCLIB (free, no key).
 * Found lyrics are cached on the device so each song is only looked up once.
 */
class LyricsRepository(private val context: Context) {

    private val tagEditor = TagEditor(context)
    private val cacheDir get() = File(context.filesDir, "lyrics").apply { mkdirs() }

    suspend fun lyricsFor(song: Song, forceRefresh: Boolean = false): LyricsResult = withContext(Dispatchers.IO) {
        if (!forceRefresh) readCache(song)?.let { return@withContext LyricsResult.Found(it) }

        val local = fromTag(song) ?: fromSidecar(song)
        if (local != null) {
            writeCache(song, local)
            return@withContext LyricsResult.Found(local)
        }
        when (val online = fromLrclib(song)) {
            null -> LyricsResult.Offline
            else -> online.getOrNull()?.let {
                writeCache(song, it)
                LyricsResult.Found(it)
            } ?: LyricsResult.NotFound
        }
    }

    private suspend fun fromTag(song: Song): Lyrics? =
        tagEditor.readLyrics(song)?.let { text -> toLyrics(text, LyricsSource.FILE) }

    /** "Song.lrc" next to "Song.flac"; often unreadable on Android 11+ (not a media file). */
    private fun fromSidecar(song: Song): Lyrics? = runCatching {
        @Suppress("DEPRECATION")
        val path = context.contentResolver.query(
            song.uri, arrayOf(MediaStore.Audio.Media.DATA), null, null, null,
        )?.use { if (it.moveToFirst()) it.getString(0) else null } ?: return null
        val lrc = File(path.substringBeforeLast('.') + ".lrc")
        if (!lrc.canRead()) return null
        toLyrics(lrc.readText(), LyricsSource.LRC_FILE)
    }.getOrNull()

    /**
     * null = request failed (offline); Result(null) = not found; Result(lyrics) = found.
     * Tries the exact match endpoint first, then a search picking the closest duration.
     */
    private fun fromLrclib(song: Song): Result<Lyrics?>? {
        if (song.title.isBlank()) return Result.success(null)
        return try {
            val params = buildString {
                append("track_name=").append(encode(song.title))
                append("&artist_name=").append(encode(song.artist))
                if (song.album.isNotBlank()) append("&album_name=").append(encode(song.album))
                if (song.durationMs > 0) append("&duration=").append(song.durationMs / 1000)
            }
            val exact = get("https://lrclib.net/api/get?$params")?.let { JSONObject(it) }
            val match = exact ?: run {
                val search = get(
                    "https://lrclib.net/api/search?track_name=${encode(song.title)}&artist_name=${encode(song.artist)}"
                )?.let { JSONArray(it) } ?: return Result.success(null)
                List(search.length()) { search.getJSONObject(it) }
                    .minByOrNull { abs(it.optDouble("duration", 0.0) * 1000 - song.durationMs) }
            } ?: return Result.success(null)
            Result.success(fromLrclibJson(match))
        } catch (e: FileNotFoundException) {
            Result.success(null)
        } catch (e: Exception) {
            null
        }
    }

    private fun fromLrclibJson(o: JSONObject): Lyrics? {
        val synced = o.optString("syncedLyrics").takeIf { it.isNotBlank() && it != "null" }
        val plain = o.optString("plainLyrics").takeIf { it.isNotBlank() && it != "null" }
        synced?.let { LrcParser.parse(it) }?.let { return Lyrics(it, plain, LyricsSource.ONLINE) }
        return plain?.let { Lyrics(null, it, LyricsSource.ONLINE) }
    }

    private fun toLyrics(text: String, source: LyricsSource): Lyrics? {
        if (text.isBlank()) return null
        val synced = LrcParser.parse(text)
        return Lyrics(synced, if (synced == null) text.trim() else null, source)
    }

    /** Body of a 200 response, null for 404; throws on network errors. */
    private fun get(url: String): String? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("User-Agent", "RetroCassette/2.2 (Android)")
        }
        return try {
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> connection.inputStream.bufferedReader().use { it.readText() }
                HttpURLConnection.HTTP_NOT_FOUND -> null
                else -> throw java.io.IOException("HTTP ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }

    /** Every lyric already saved on the device, as plain text by song id (for lyrics search). */
    suspend fun cachedTexts(): Map<Long, String> = withContext(Dispatchers.IO) {
        cacheDir.listFiles().orEmpty().mapNotNull { file ->
            val id = file.nameWithoutExtension.toLongOrNull() ?: return@mapNotNull null
            val text = runCatching {
                val o = JSONObject(file.readText())
                o.optJSONArray("synced")?.let { a -> List(a.length()) { a.getJSONObject(it).getString("x") }.joinToString("\n") }
                    ?: o.optString("plain")
            }.getOrNull()
            text?.takeIf { it.isNotBlank() }?.let { id to it }
        }.toMap()
    }

    private fun cacheFile(song: Song) = File(cacheDir, "${song.id}.json")

    private fun readCache(song: Song): Lyrics? = runCatching {
        val file = cacheFile(song)
        if (!file.exists()) return null
        val o = JSONObject(file.readText())
        val source = LyricsSource.valueOf(o.getString("source"))
        val synced = o.optJSONArray("synced")?.let { a ->
            List(a.length()) { a.getJSONObject(it) }.map { LyricLine(it.getLong("t"), it.getString("x")) }
        }
        Lyrics(synced, o.optString("plain").takeIf { it.isNotEmpty() }, source)
    }.getOrNull()

    private fun writeCache(song: Song, lyrics: Lyrics) {
        runCatching {
            val o = JSONObject().put("source", lyrics.source.name).put("plain", lyrics.plain.orEmpty())
            lyrics.synced?.let { lines ->
                o.put("synced", JSONArray(lines.map { JSONObject().put("t", it.timeMs).put("x", it.text) }))
            }
            cacheFile(song).writeText(o.toString())
        }
    }

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
}
