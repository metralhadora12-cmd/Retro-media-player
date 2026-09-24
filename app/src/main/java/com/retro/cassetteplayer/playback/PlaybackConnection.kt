package com.retro.cassetteplayer.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.retro.cassetteplayer.data.Song
import com.retro.cassetteplayer.data.toMediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** One entry of the play queue, in playback order (shuffle-aware). */
data class QueueItem(
    val index: Int,
    val mediaId: String,
    val title: String,
    val artist: String,
    val artworkUri: Uri?,
)

data class PlaybackState(
    val mediaId: String? = null,
    val title: String = "",
    val artist: String = "",
    val artworkUri: Uri? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    /** Current item first, then what plays next. */
    val queue: List<QueueItem> = emptyList(),
) {
    val hasMedia: Boolean get() = mediaId != null
    val progress: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

/**
 * Connects the UI to [PlaybackService] through a [MediaController] and exposes the
 * player state as a [StateFlow]. Commands issued before the controller is connected
 * are queued and replayed once it is ready.
 */
class PlaybackConnection(context: Context) {

    private val appContext = context.applicationContext
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private val pendingCommands = mutableListOf<(MediaController) -> Unit>()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish(player)
    }

    fun connect() {
        if (controllerFuture != null) return
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        controllerFuture = future
        future.addListener({
            val connected = runCatching { future.get() }.getOrNull() ?: return@addListener
            controller = connected
            connected.addListener(listener)
            publish(connected)
            pendingCommands.forEach { it(connected) }
            pendingCommands.clear()
        }, ContextCompat.getMainExecutor(appContext))
    }

    fun release() {
        controller?.removeListener(listener)
        controllerFuture?.let(MediaController::releaseFuture)
        controllerFuture = null
        controller = null
        pendingCommands.clear()
    }

    /** Refreshes only the position, used by the UI ticker while playing. */
    fun refreshPosition() {
        val player = controller ?: return
        _state.update {
            it.copy(positionMs = player.currentPosition.coerceAtLeast(0L), durationMs = player.safeDuration())
        }
    }

    fun playSongs(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (songs.isEmpty()) return
        withController { player ->
            player.shuffleModeEnabled = shuffle
            player.setMediaItems(songs.map(Song::toMediaItem), startIndex.coerceIn(songs.indices), 0L)
            player.prepare()
            player.play()
        }
    }

    fun playNext(song: Song) = withController { player ->
        if (player.mediaItemCount == 0) {
            playSongs(listOf(song))
        } else {
            player.addMediaItem(player.currentMediaItemIndex + 1, song.toMediaItem())
        }
    }

    fun addToQueue(song: Song) = withController { player ->
        if (player.mediaItemCount == 0) playSongs(listOf(song)) else player.addMediaItem(song.toMediaItem())
    }

    fun togglePlayPause() = withController { player ->
        when {
            player.isPlaying -> player.pause()
            player.playbackState == Player.STATE_ENDED -> {
                player.seekToDefaultPosition(0)
                player.play()
            }
            else -> {
                if (player.playbackState == Player.STATE_IDLE) player.prepare()
                player.play()
            }
        }
    }

    fun skipNext() = withController { it.seekToNext() }
    fun skipPrevious() = withController { it.seekToPrevious() }
    fun rewind() = withController { it.seekBack() }
    fun fastForward() = withController { it.seekForward() }

    fun seekTo(positionMs: Long) = withController { player ->
        player.seekTo(positionMs.coerceAtLeast(0L))
        publish(player)
    }

    fun toggleShuffle() = withController { it.shuffleModeEnabled = !it.shuffleModeEnabled }

    /** OFF -> ALL -> ONE -> OFF */
    fun cycleRepeat() = withController { player ->
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun playQueueItem(index: Int) = withController { player ->
        if (index in 0 until player.mediaItemCount) {
            player.seekToDefaultPosition(index)
            player.play()
        }
    }

    fun removeQueueItem(index: Int) = withController { player ->
        if (index in 0 until player.mediaItemCount) player.removeMediaItem(index)
    }

    private fun withController(command: (MediaController) -> Unit) {
        val current = controller
        if (current != null) command(current) else pendingCommands += command
    }

    private fun publish(player: Player) {
        val item = player.currentMediaItem
        val metadata = player.mediaMetadata
        _state.value = PlaybackState(
            mediaId = item?.mediaId,
            title = metadata.title?.toString().orEmpty(),
            artist = metadata.artist?.toString().orEmpty(),
            artworkUri = metadata.artworkUri,
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.safeDuration(),
            shuffleEnabled = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
            queue = buildQueue(player),
        )
    }

    private fun buildQueue(player: Player): List<QueueItem> {
        val timeline = player.currentTimeline
        if (timeline.isEmpty) return emptyList()
        val items = mutableListOf<QueueItem>()
        var index = player.currentMediaItemIndex
        while (index != C.INDEX_UNSET && items.size < MAX_QUEUE_ITEMS) {
            val item = player.getMediaItemAt(index)
            items += QueueItem(
                index = index,
                mediaId = item.mediaId,
                title = item.mediaMetadata.title?.toString().orEmpty(),
                artist = item.mediaMetadata.artist?.toString().orEmpty(),
                artworkUri = item.mediaMetadata.artworkUri,
            )
            index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, player.shuffleModeEnabled)
        }
        return items
    }

    private companion object {
        const val MAX_QUEUE_ITEMS = 300
    }

    private fun Player.safeDuration(): Long = duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L
}
