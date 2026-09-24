package com.retro.cassetteplayer.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** One cover found online. */
data class CoverResult(
    val title: String,
    val artist: String,
    /** Small image for the results grid. */
    val thumbnailUrl: String,
    /** Large image that gets embedded in the file. */
    val imageUrl: String,
    val source: String,
)

/**
 * Looks up album covers on two free, key-less APIs (iTunes Search and Deezer) and
 * downloads the chosen image.
 */
object CoverSearch {

    private const val TIMEOUT_MS = 10_000
    private const val MAX_IMAGE_BYTES = 8 * 1024 * 1024

    /** Returns null when both sources fail (no connection), otherwise the merged results. */
    suspend fun search(query: String): List<CoverResult>? = withContext(Dispatchers.IO) {
        val term = query.trim()
        if (term.isEmpty()) return@withContext emptyList()
        coroutineScope {
            val itunes = async { runCatching { searchItunes(term) }.getOrNull() }
            val deezer = async { runCatching { searchDeezer(term) }.getOrNull() }
            val a = itunes.await()
            val b = deezer.await()
            if (a == null && b == null) null
            else (a.orEmpty() + b.orEmpty()).distinctBy { it.imageUrl }
        }
    }

    suspend fun download(url: String): Pair<ByteArray, String>? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = open(url)
            try {
                val mime = connection.contentType?.substringBefore(';')?.takeIf { it.startsWith("image/") }
                    ?: "image/jpeg"
                val bytes = connection.inputStream.use { it.readBytes() }
                if (bytes.isEmpty() || bytes.size > MAX_IMAGE_BYTES) null else bytes to mime
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    private fun searchItunes(term: String): List<CoverResult> {
        val url = "https://itunes.apple.com/search?media=music&entity=album&limit=25&term=" + encode(term)
        val results = JSONObject(get(url)).optJSONArray("results") ?: return emptyList()
        return List(results.length()) { results.getJSONObject(it) }.mapNotNull { item ->
            val art = item.optString("artworkUrl100").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            CoverResult(
                title = item.optString("collectionName"),
                artist = item.optString("artistName"),
                thumbnailUrl = art.replace("100x100bb", "300x300bb"),
                imageUrl = art.replace("100x100bb", "1000x1000bb"),
                source = "iTunes",
            )
        }
    }

    private fun searchDeezer(term: String): List<CoverResult> {
        val url = "https://api.deezer.com/search/album?limit=25&q=" + encode(term)
        val data = JSONObject(get(url)).optJSONArray("data") ?: return emptyList()
        return List(data.length()) { data.getJSONObject(it) }.mapNotNull { item ->
            val big = item.optString("cover_xl").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            CoverResult(
                title = item.optString("title"),
                artist = item.optJSONObject("artist")?.optString("name").orEmpty(),
                thumbnailUrl = item.optString("cover_medium").ifBlank { big },
                imageUrl = big,
                source = "Deezer",
            )
        }
    }

    private fun get(url: String): String {
        val connection = open(url)
        return try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("User-Agent", "RetroCassette/2.0 (Android)")
            instanceFollowRedirects = true
        }

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
}
