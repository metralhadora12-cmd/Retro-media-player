package com.retro.cassetteplayer.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

/** Listening history of one song. */
data class SongStats(
    val plays: Int = 0,
    /** Milliseconds since epoch, 0 = never. */
    val lastPlayed: Long = 0L,
    val listenedMs: Long = 0L,
)

data class StatsSnapshot(
    val songs: Map<Long, SongStats> = emptyMap(),
    /** "yyyy-MM-dd" → milliseconds listened that day. */
    val daily: Map<String, Long> = emptyMap(),
    /** "yyyy-MM" → song id → milliseconds listened that month. */
    val monthly: Map<String, Map<Long, Long>> = emptyMap(),
) {
    val totalListenedMs: Long get() = daily.values.sum()
    val totalPlays: Int get() = songs.values.sumOf { it.plays }
}

/**
 * Play counts and listening time, recorded by the playback service and read by the UI
 * (smart playlists and the stats screen). Kept in memory, saved to a JSON file.
 */
object PlayStats {

    private val songs = HashMap<Long, SongStats>()
    private val daily = HashMap<String, Long>()
    private val monthly = HashMap<String, HashMap<Long, Long>>()
    private var file: File? = null
    private var dirty = false

    private val _state = MutableStateFlow(StatsSnapshot())
    val state: StateFlow<StatsSnapshot> = _state.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (file != null) return
        val f = File(context.filesDir, "play_stats.json")
        file = f
        runCatching {
            if (!f.exists()) return@runCatching
            val root = JSONObject(f.readText())
            root.optJSONObject("songs")?.let { o ->
                o.keys().forEach { key ->
                    val s = o.getJSONObject(key)
                    songs[key.toLong()] = SongStats(s.optInt("p"), s.optLong("l"), s.optLong("t"))
                }
            }
            root.optJSONObject("daily")?.let { o -> o.keys().forEach { daily[it] = o.getLong(it) } }
            root.optJSONObject("monthly")?.let { o ->
                o.keys().forEach { month ->
                    val m = o.getJSONObject(month)
                    monthly[month] = HashMap<Long, Long>().apply { m.keys().forEach { put(it.toLong(), m.getLong(it)) } }
                }
            }
        }
        publish()
    }

    /** Adds listening time to a song, today and this month. */
    @Synchronized
    fun addListening(songId: Long, ms: Long) {
        if (ms <= 0) return
        val s = songs[songId] ?: SongStats()
        songs[songId] = s.copy(listenedMs = s.listenedMs + ms)
        val today = LocalDate.now().toString()
        daily[today] = (daily[today] ?: 0L) + ms
        val month = monthly.getOrPut(YearMonth.now().toString()) { HashMap() }
        month[songId] = (month[songId] ?: 0L) + ms
        dirty = true
    }

    /** Counts one play (called once the song has been listened to for a while). */
    @Synchronized
    fun countPlay(songId: Long) {
        val s = songs[songId] ?: SongStats()
        songs[songId] = s.copy(plays = s.plays + 1, lastPlayed = System.currentTimeMillis())
        dirty = true
        publish()
    }

    /** Publishes the latest numbers and writes them to disk if anything changed. */
    @Synchronized
    fun flush() {
        if (!dirty) return
        dirty = false
        publish()
        val root = JSONObject()
        root.put("songs", JSONObject().apply {
            songs.forEach { (id, s) -> put(id.toString(), JSONObject().put("p", s.plays).put("l", s.lastPlayed).put("t", s.listenedMs)) }
        })
        root.put("daily", JSONObject().apply { daily.forEach { (k, v) -> put(k, v) } })
        root.put("monthly", JSONObject().apply {
            monthly.forEach { (month, m) -> put(month, JSONObject().apply { m.forEach { (id, v) -> put(id.toString(), v) } }) }
        })
        val target = file ?: return
        runCatching {
            val tmp = File(target.parentFile, target.name + ".tmp")
            tmp.writeText(root.toString())
            tmp.renameTo(target)
        }
    }

    private fun publish() {
        _state.value = StatsSnapshot(
            songs = HashMap(songs),
            daily = HashMap(daily),
            monthly = monthly.mapValues { HashMap(it.value) },
        )
    }
}
