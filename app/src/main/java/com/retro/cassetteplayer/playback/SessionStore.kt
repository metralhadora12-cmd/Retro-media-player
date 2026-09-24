package com.retro.cassetteplayer.playback

import android.content.Context
import androidx.media3.common.Player

/** Last playback session: the queue, current track, position and modes. */
data class SavedSession(
    val songIds: List<Long>,
    val index: Int,
    val positionMs: Long,
    val shuffle: Boolean,
    val repeatMode: Int,
)

/** Persists the playback session so it can be resumed after the app is closed. */
class SessionStore(context: Context) {

    private val prefs = context.getSharedPreferences("last_session", Context.MODE_PRIVATE)

    fun save(player: Player) {
        val ids = (0 until player.mediaItemCount).mapNotNull { player.getMediaItemAt(it).mediaId.toLongOrNull() }
        if (ids.isEmpty()) return
        prefs.edit()
            .putString(KEY_IDS, ids.joinToString(","))
            .putInt(KEY_INDEX, player.currentMediaItemIndex.coerceIn(0, ids.lastIndex))
            .putLong(KEY_POSITION, player.currentPosition.coerceAtLeast(0L))
            .putBoolean(KEY_SHUFFLE, player.shuffleModeEnabled)
            .putInt(KEY_REPEAT, player.repeatMode)
            .apply()
    }

    fun load(): SavedSession? {
        val ids = prefs.getString(KEY_IDS, null)
            ?.split(',')?.mapNotNull { it.toLongOrNull() }
            ?.takeIf { it.isNotEmpty() } ?: return null
        return SavedSession(
            songIds = ids,
            index = prefs.getInt(KEY_INDEX, 0),
            positionMs = prefs.getLong(KEY_POSITION, 0L),
            shuffle = prefs.getBoolean(KEY_SHUFFLE, false),
            repeatMode = prefs.getInt(KEY_REPEAT, Player.REPEAT_MODE_OFF),
        )
    }

    private companion object {
        const val KEY_IDS = "ids"
        const val KEY_INDEX = "index"
        const val KEY_POSITION = "position"
        const val KEY_SHUFFLE = "shuffle"
        const val KEY_REPEAT = "repeat"
    }
}
