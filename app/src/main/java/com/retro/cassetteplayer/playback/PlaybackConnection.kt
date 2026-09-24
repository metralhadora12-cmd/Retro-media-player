package com.retro.cassetteplayer.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.retro.cassetteplayer.data.Song
import com.retro.cassetteplayer.data.toMediaItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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

/** What the decoder reports for the current track (e.g. FLAC, 24-bit, 96 kHz). */
data class AudioFormatInfo(
    val codec: String,
    val lossless: Boolean,
    /** 0 when unknown. */
    val bitDepth: Int,
    /** Hz, 0 when unknown. */
    val sampleRate: Int,
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
    val audioFormat: AudioFormatInfo? = null,
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

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 4)
    /** Title of a track that could not be played (unsupported format, broken file…). */
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish(player)

        override fun onPlayerError(error: PlaybackException) {
            val player = controller ?: return
            _errors.tryEmit(player.mediaMetadata.title?.toString().orEmpty())
            // Skip the unplayable track instead of leaving the queue stuck.
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
                player.prepare()
                player.play()
            }
        }
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

    /** Loads a saved session paused at its position, only if nothing is loaded yet. */
    fun restoreSession(songs: List<Song>, index: Int, positionMs: Long, shuffle: Boolean, repeatMode: Int) {
        if (songs.isEmpty()) return
        withController { player ->
            if (player.mediaItemCount > 0) return@withController
            player.shuffleModeEnabled = shuffle
            player.repeatMode = repeatMode
            player.setMediaItems(songs.map(Song::toMediaItem), index.coerceIn(songs.indices), positionMs)
            player.prepare()
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

    fun addAllToQueue(songs: List<Song>) {
        if (songs.isEmpty()) return
        withController { player ->
            if (player.mediaItemCount == 0) playSongs(songs) else player.addMediaItems(songs.map(Song::toMediaItem))
        }
    }

    /** Removes every queue entry of a song (used after the file is deleted). */
    fun removeFromQueue(mediaId: String) = withController { player ->
        for (i in player.mediaItemCount - 1 downTo 0) {
            if (player.getMediaItemAt(i).mediaId == mediaId) player.removeMediaItem(i)
        }
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
            audioFormat = audioFormatOf(player),
        )
    }

    @OptIn(UnstableApi::class)
    private fun audioFormatOf(player: Player): AudioFormatInfo? {
        val format = player.currentTracks.groups
            .firstOrNull { it.type == C.TRACK_TYPE_AUDIO && it.isSelected }
            ?.let { group -> (0 until group.length).firstOrNull { group.isTrackSelected(it) }?.let(group::getTrackFormat) }
            ?: return null
        val mime = format.sampleMimeType ?: return null
        val codec = when (mime) {
            MimeTypes.AUDIO_FLAC -> "FLAC"
            MimeTypes.AUDIO_ALAC -> "ALAC"
            MimeTypes.AUDIO_RAW -> "PCM"
            MimeTypes.AUDIO_WAV -> "WAV"
            MimeTypes.AUDIO_MPEG -> "MP3"
            MimeTypes.AUDIO_AAC -> "AAC"
            MimeTypes.AUDIO_OPUS -> "OPUS"
            MimeTypes.AUDIO_VORBIS -> "OGG"
            else -> mime.substringAfter('/').uppercase()
        }
        val lossless = mime in LOSSLESS_MIME_TYPES
        val bitDepth = when (format.pcmEncoding) {
            C.ENCODING_PCM_8BIT -> 8
            C.ENCODING_PCM_16BIT, C.ENCODING_PCM_16BIT_BIG_ENDIAN -> 16
            C.ENCODING_PCM_24BIT -> 24
            C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 32
            else -> 0
        }
        val sampleRate = format.sampleRate.takeIf { it != Format.NO_VALUE } ?: 0
        return AudioFormatInfo(codec, lossless, bitDepth, sampleRate)
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
        val LOSSLESS_MIME_TYPES = setOf(
            MimeTypes.AUDIO_FLAC,
            MimeTypes.AUDIO_ALAC,
            MimeTypes.AUDIO_RAW,
            MimeTypes.AUDIO_WAV,
        )
    }

    private fun Player.safeDuration(): Long = duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L
}
