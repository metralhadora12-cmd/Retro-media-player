package com.retro.cassetteplayer.playback

import android.content.Context
import android.content.Intent
import androidx.media3.common.C
import androidx.media3.common.Player
import com.retro.cassetteplayer.data.AppSettings

/**
 * Tells scrobbler apps what is playing, so they can send it to Last.fm (they handle the
 * account and the Last.fm rules). Uses the Simple Last.fm Scrobbler API and the common
 * "com.android.music" broadcasts, sent explicitly to the known scrobbler packages.
 */
class Scrobbler(private val context: Context) {

    private data class Track(val id: String, val title: String, val artist: String, val album: String, val durationMs: Long)

    private var current: Track? = null
    private var playing = false

    /** Call on media item transitions and play/pause changes. */
    fun update(player: Player, completedPrevious: Boolean) {
        if (!AppSettings.scrobbling.value) {
            current = null
            playing = false
            return
        }
        val item = player.currentMediaItem
        val metadata = player.mediaMetadata
        val track = item?.let {
            Track(
                id = it.mediaId,
                title = metadata.title?.toString().orEmpty(),
                artist = metadata.artist?.toString().orEmpty(),
                album = metadata.albumTitle?.toString().orEmpty(),
                durationMs = player.duration.takeIf { d -> d != C.TIME_UNSET && d > 0 } ?: 0L,
            )
        }
        val previous = current
        if (previous != null && previous.id != track?.id) {
            if (completedPrevious) send(previous, SLS_COMPLETE, playing = false)
            playing = false
        }
        current = track
        if (track == null || track.title.isBlank()) return
        val nowPlaying = player.isPlaying
        if (nowPlaying == playing && previous?.id == track.id) return
        val state = when {
            !nowPlaying -> SLS_PAUSE
            previous?.id == track.id -> SLS_RESUME
            else -> SLS_START
        }
        playing = nowPlaying
        send(track, state, nowPlaying)
    }

    private fun send(track: Track, slsState: Int, playing: Boolean) {
        runCatching {
            context.sendBroadcast(
                Intent(SLS_ACTION)
                    .setPackage(SLS_PACKAGE)
                    .putExtra("app-name", "Retro Cassette")
                    .putExtra("app-package", context.packageName)
                    .putExtra("state", slsState)
                    .putExtra("track", track.title)
                    .putExtra("artist", track.artist)
                    .putExtra("album", track.album)
                    .putExtra("duration", (track.durationMs / 1000).toInt())
            )
            MUSIC_PACKAGES.forEach { pkg ->
                listOf(META_CHANGED, PLAYSTATE_CHANGED).forEach { action ->
                    context.sendBroadcast(
                        Intent(action)
                            .setPackage(pkg)
                            .putExtra("id", track.id.toLongOrNull() ?: 0L)
                            .putExtra("track", track.title)
                            .putExtra("artist", track.artist)
                            .putExtra("album", track.album)
                            .putExtra("duration", track.durationMs)
                            .putExtra("playing", playing)
                            .putExtra("package", context.packageName)
                    )
                }
            }
        }
    }

    private companion object {
        const val SLS_ACTION = "com.adam.aslfms.notify.playstatechanged"
        const val SLS_PACKAGE = "com.adam.aslfms"
        const val SLS_START = 0
        const val SLS_RESUME = 1
        const val SLS_PAUSE = 2
        const val SLS_COMPLETE = 3
        const val META_CHANGED = "com.android.music.metachanged"
        const val PLAYSTATE_CHANGED = "com.android.music.playstatechanged"
        /** Pano Scrobbler, Last.fm, Scrobble Droid. */
        val MUSIC_PACKAGES = listOf("com.arn.scrobble", "fm.last.android", "net.jjc1138.android.scrobbler")
    }
}
