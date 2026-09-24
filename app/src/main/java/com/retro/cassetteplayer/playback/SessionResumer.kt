package com.retro.cassetteplayer.playback

import androidx.media3.common.MediaItem
import com.retro.cassetteplayer.data.toMediaItem

/** Last session's queue rebuilt from the saved song ids (for play requests on an empty player). */
data class ResumableSession(
    val items: List<MediaItem>,
    val index: Int,
    val positionMs: Long,
    val shuffle: Boolean,
    val repeatMode: Int,
)

suspend fun loadResumableSession(store: SessionStore, browser: LibraryBrowser): ResumableSession? {
    val saved = store.load() ?: return null
    val byId = browser.songs().associateBy { it.id }
    val queue = saved.songIds.mapNotNull { byId[it] }
    if (queue.isEmpty()) return null
    val currentId = saved.songIds.getOrNull(saved.index)
    val index = queue.indexOfFirst { it.id == currentId }
    return ResumableSession(
        items = queue.map { it.toMediaItem() },
        index = index.coerceAtLeast(0),
        positionMs = if (index >= 0) saved.positionMs else 0L,
        shuffle = saved.shuffle,
        repeatMode = saved.repeatMode,
    )
}
