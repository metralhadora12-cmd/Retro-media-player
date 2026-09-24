package com.retro.cassetteplayer.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.retro.cassetteplayer.data.AppSettings
import com.retro.cassetteplayer.data.PlayStats
import com.retro.cassetteplayer.data.ReplayGainMode
import com.retro.cassetteplayer.data.toMediaItem
import com.retro.cassetteplayer.widget.PlayerWidget
import com.retro.cassetteplayer.widget.WidgetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Hosts the ExoPlayer inside a [MediaLibraryService] so playback keeps going in the
 * background, shows up in the system media controls and can be browsed by Android Auto.
 *
 * Besides playing, it runs the sleep timer, fades between tracks, applies ReplayGain,
 * flips the tape at side B, records listening stats, feeds scrobblers and the widget.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {

    private var mediaSession: MediaLibrarySession? = null
    private lateinit var sessionStore: SessionStore
    private lateinit var browser: LibraryBrowser
    private lateinit var scrobbler: Scrobbler
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Volume = ReplayGain × track fade × sleep-timer fade
    private var gainVolume = 1f
    private var fadeVolume = 1f
    private var sleepVolume = 1f
    /** Fade the next track in (set after an automatic transition). */
    private var fadingIn = false

    // Stats: listening time of the current item and whether it was counted as a play
    private var statsMediaId: String? = null
    private var statsListenedMs = 0L
    private var statsCounted = false
    private var lastTickAt = 0L
    private var lastStatsFlush = 0L

    private var headsetCallbackReady = false
    private var flipping = false

    /** Saves the position every 10 s while playing, so a killed app resumes close to where it was. */
    private val periodicSave = object : Runnable {
        override fun run() {
            mediaSession?.player?.let { if (it.isPlaying) sessionStore.save(it) }
            handler.postDelayed(this, SAVE_INTERVAL_MS)
        }
    }

    /** Fades, sleep timer and stats, ten times a second. */
    private val ticker = object : Runnable {
        override fun run() {
            mediaSession?.player?.let(::tick)
            handler.postDelayed(this, TICK_MS)
        }
    }

    private val headsetCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            // The first call just lists what is already connected
            if (!headsetCallbackReady || !AppSettings.resumeOnConnect.value) return
            if (addedDevices.none { it.isSink && it.type in HEADSET_TYPES }) return
            val player = mediaSession?.player ?: return
            if (player.mediaItemCount > 0 && !player.isPlaying) {
                // Let the audio route settle before starting
                handler.postDelayed({
                    if (player.playbackState == Player.STATE_IDLE) player.prepare()
                    player.play()
                }, RESUME_DELAY_MS)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        AppSettings.init(this)
        PlayStats.init(this)
        sessionStore = SessionStore(this)
        browser = LibraryBrowser(this)
        scrobbler = Scrobbler(this)

        // Lossless / hi-res: platform decoders first, the bundled FFmpeg decoder as a
        // fallback (e.g. ALAC), and float PCM output so 24-bit audio keeps its precision.
        // The tape effects run as an audio processor on regular 16-bit audio.
        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ): AudioSink = DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessors(arrayOf(TapeEffectsProcessor()))
                .build()
        }
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setEnableAudioFloatOutput(AppSettings.hiResOutput.value)
        val player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(SEEK_INCREMENT_MS)
            .setSeekForwardIncrementMs(SEEK_INCREMENT_MS)
            .build()

        val sessionActivity = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(
                this, 0, it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        // Built-in equalizer follows the player's audio session.
        EqualizerManager.init(this)
        EqualizerManager.attach(player.audioSessionId)
        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioSessionIdChanged(eventTime: AnalyticsListener.EventTime, audioSessionId: Int) {
                EqualizerManager.attach(audioSessionId)
            }
        })

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val automatic = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
                fadingIn = automatic && AppSettings.crossfadeSeconds.value > 0
                startStats(mediaItem?.mediaId)
                updateReplayGain(player)
                scrobbler.update(player, completedPrevious = automatic || reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT)
                if (automatic) maybeFlipTape(player)
                // "End of track" sleep timer: the player paused at the end of the previous item
                if (SleepTimer.state.value.endOfTrack && automatic) SleepTimer.cancel()
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
                    SleepTimer.cancel()
                }
            }

            override fun onEvents(player: Player, events: Player.Events) {
                if (events.containsAny(
                        Player.EVENT_TIMELINE_CHANGED,
                        Player.EVENT_MEDIA_ITEM_TRANSITION,
                        Player.EVENT_IS_PLAYING_CHANGED,
                        Player.EVENT_POSITION_DISCONTINUITY,
                        Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                        Player.EVENT_REPEAT_MODE_CHANGED,
                    )
                ) {
                    sessionStore.save(player)
                }
                if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                    scrobbler.update(player, completedPrevious = false)
                    if (!player.isPlaying) PlayStats.flush()
                }
                if (events.containsAny(
                        Player.EVENT_MEDIA_METADATA_CHANGED,
                        Player.EVENT_MEDIA_ITEM_TRANSITION,
                        Player.EVENT_IS_PLAYING_CHANGED,
                    )
                ) {
                    PlayerWidget.update(
                        this@PlaybackService,
                        WidgetState(
                            title = player.mediaMetadata.title?.toString().orEmpty(),
                            artist = player.mediaMetadata.artist?.toString().orEmpty(),
                            isPlaying = player.isPlaying,
                            artworkUri = player.mediaMetadata.artworkUri,
                        ),
                    )
                }
            }
        })
        handler.postDelayed(periodicSave, SAVE_INTERVAL_MS)
        handler.post(ticker)

        // Settings the player follows live
        scope.launch {
            combine(AppSettings.speed.flow, AppSettings.pitch.flow) { speed, pitch -> PlaybackParameters(speed, pitch) }
                .collect { player.playbackParameters = it }
        }
        scope.launch { AppSettings.replayGain.flow.collect { updateReplayGain(player) } }
        scope.launch {
            SleepTimer.state.collect { timer ->
                player.pauseAtEndOfMediaItems = timer.endOfTrack
                if (!timer.active) {
                    sleepVolume = 1f
                    applyVolume(player)
                }
            }
        }

        // Resume when headphones / a Bluetooth device connect (if enabled)
        val audioManager = getSystemService(AudioManager::class.java)
        audioManager?.registerAudioDeviceCallback(headsetCallback, handler)
        handler.post { headsetCallbackReady = true }

        mediaSession = MediaLibrarySession.Builder(this, player, LibraryCallback())
            .apply { if (sessionActivity != null) setSessionActivity(sessionActivity) }
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        mediaSession?.player?.let(sessionStore::save)
        PlayStats.flush()
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0 ||
            player.playbackState == Player.STATE_ENDED
        ) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(periodicSave)
        handler.removeCallbacks(ticker)
        getSystemService(AudioManager::class.java)?.unregisterAudioDeviceCallback(headsetCallback)
        scope.cancel()
        mediaSession?.player?.let(sessionStore::save)
        PlayStats.flush()
        EqualizerManager.release()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    // --- Ticker: fades, sleep timer, stats ------------------------------------------------

    private fun tick(player: Player) {
        val now = SystemClock.elapsedRealtime()
        val elapsed = if (lastTickAt == 0L) 0L else (now - lastTickAt).coerceAtMost(1_000L)
        lastTickAt = now

        // Track fade: out at the end of a track that is followed by another, in after an automatic change
        val fadeMs = AppSettings.crossfadeSeconds.value * 1000L
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L
        val position = player.currentPosition
        fadeVolume = if (fadeMs > 0 && duration > fadeMs * 2 && player.isPlaying) {
            val remaining = duration - position
            val fadeOut = if (player.hasNextMediaItem() && remaining < fadeMs) remaining.toFloat() / fadeMs else 1f
            val fadeIn = if (fadingIn && position < fadeMs) position.toFloat() / fadeMs else 1f
            if (fadingIn && position >= fadeMs) fadingIn = false
            minOf(fadeOut, fadeIn).coerceIn(0f, 1f)
        } else {
            fadingIn = false
            1f
        }

        // Sleep timer
        val timer = SleepTimer.state.value
        sleepVolume = when {
            timer.endAtElapsed != null -> {
                val remaining = timer.remainingMs(now)
                if (remaining <= 0L) {
                    player.pause()
                    SleepTimer.cancel()
                    1f
                } else {
                    (remaining.toFloat() / SleepTimer.FADE_MS).coerceIn(0f, 1f)
                }
            }
            timer.endOfTrack && duration > 0 && player.isPlaying ->
                ((duration - position).toFloat() / END_OF_TRACK_FADE_MS).coerceIn(0f, 1f)
            else -> 1f
        }
        applyVolume(player)

        // Listening stats: a song counts as played after 30 s (or half of a short song)
        if (player.isPlaying && elapsed > 0) {
            val id = player.currentMediaItem?.mediaId
            if (id != statsMediaId) startStats(id)
            id?.toLongOrNull()?.let { songId ->
                PlayStats.addListening(songId, elapsed)
                statsListenedMs += elapsed
                val threshold = if (duration > 0) minOf(PLAY_COUNT_MS, duration / 2) else PLAY_COUNT_MS
                if (!statsCounted && statsListenedMs >= threshold) {
                    statsCounted = true
                    PlayStats.countPlay(songId)
                }
            }
            if (now - lastStatsFlush > STATS_FLUSH_MS) {
                lastStatsFlush = now
                PlayStats.flush()
            }
        }
    }

    private fun startStats(mediaId: String?) {
        statsMediaId = mediaId
        statsListenedMs = 0L
        statsCounted = false
    }

    private fun applyVolume(player: Player) {
        val volume = (gainVolume * fadeVolume * sleepVolume).coerceIn(0f, 1f)
        if (player.volume != volume) player.volume = volume
    }

    // --- ReplayGain -----------------------------------------------------------------

    private fun updateReplayGain(player: Player) {
        val mode = AppSettings.replayGain.value
        val item = player.currentMediaItem
        val uri = item?.localConfiguration?.uri ?: item?.requestMetadata?.mediaUri
        if (mode == ReplayGainMode.OFF || item == null || uri == null) {
            gainVolume = 1f
            applyVolume(player)
            return
        }
        val mediaId = item.mediaId
        scope.launch {
            val info = withContext(Dispatchers.IO) { ReplayGain.read(this@PlaybackService, uri) }
            if (player.currentMediaItem?.mediaId != mediaId) return@launch
            val gain = if (mode == ReplayGainMode.ALBUM) info.albumGain ?: info.trackGain else info.trackGain ?: info.albumGain
            gainVolume = ReplayGain.volumeFor(gain)
            applyVolume(player)
            // Read the next track's tags ahead of time so its volume is right from the start
            if (player.hasNextMediaItem()) {
                val next = player.getMediaItemAt(player.nextMediaItemIndex)
                val nextUri = next.localConfiguration?.uri ?: next.requestMetadata.mediaUri
                if (nextUri != null) withContext(Dispatchers.IO) { ReplayGain.read(this@PlaybackService, nextUri) }
            }
        }
    }

    // --- Side A / side B --------------------------------------------------------------

    /** Reaching the first track of side B pauses briefly while the tape "flips" (the UI animates it). */
    private fun maybeFlipTape(player: Player) {
        val sides = TapeSides.of(player) ?: return
        if (flipping || player.currentMediaItemIndex != sides.sideBStart) return
        flipping = true
        player.pause()
        handler.postDelayed({
            flipping = false
            player.play()
        }, FLIP_PAUSE_MS)
    }

    // --- Media library (Android Auto, system media controls) ---------------------------

    private inner class LibraryCallback : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> =
            Futures.immediateFuture(LibraryResult.ofItem(this@PlaybackService.browser.root(), params))

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = future {
            val children = this@PlaybackService.browser.children(parentId)
            val from = (page * pageSize).coerceAtMost(children.size)
            val to = (from + pageSize).coerceAtMost(children.size)
            LibraryResult.ofItemList(ImmutableList.copyOf(children.subList(from, to)), params)
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> = future {
            val item = this@PlaybackService.browser.item(mediaId)
            if (item != null) LibraryResult.ofItem(item, null)
            else LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
        }

        /** Media items arriving from controllers carry their URI in the request metadata. */
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> {
            // Items from the app already carry their URI: resolve them right away
            if (mediaItems.all { it.requestMetadata.mediaUri != null || it.localConfiguration != null }) {
                return Futures.immediateFuture(
                    mediaItems.map { item ->
                        val uri = item.requestMetadata.mediaUri ?: item.localConfiguration?.uri
                        if (uri == null) item else item.buildUpon().setUri(uri).build()
                    }.toMutableList()
                )
            }
            return future { browser.resolve(mediaItems).toMutableList() }
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            if (mediaItems.all { it.requestMetadata.mediaUri != null }) {
                val resolved = mediaItems.map { it.buildUpon().setUri(it.requestMetadata.mediaUri).build() }
                return Futures.immediateFuture(MediaSession.MediaItemsWithStartPosition(resolved, startIndex, startPositionMs))
            }
            return future {
                // A song picked in Android Auto plays its whole album / playlist
                val expanded = browser.expand(mediaItems)
                if (expanded != null) {
                    MediaSession.MediaItemsWithStartPosition(expanded.first, expanded.second, C.TIME_UNSET)
                } else {
                    MediaSession.MediaItemsWithStartPosition(browser.resolve(mediaItems), startIndex, startPositionMs)
                }
            }
        }

        /** Play pressed with nothing loaded (Bluetooth button, system media card): resume the last session. */
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = future {
            val resumable = loadResumableSession(sessionStore, browser)
                ?: throw UnsupportedOperationException("Nothing to resume")
            mediaSession.player.shuffleModeEnabled = resumable.shuffle
            mediaSession.player.repeatMode = resumable.repeatMode
            MediaSession.MediaItemsWithStartPosition(resumable.items, resumable.index, resumable.positionMs)
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> {
            session.notifySearchResultChanged(browser, query, 1, params)
            return Futures.immediateFuture(LibraryResult.ofVoid())
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = future {
            val results = this@PlaybackService.browser.search(query).map { it.toMediaItem() }
            val from = (page * pageSize).coerceAtMost(results.size)
            val to = (from + pageSize).coerceAtMost(results.size)
            LibraryResult.ofItemList(ImmutableList.copyOf(results.subList(from, to)), params)
        }
    }

    /** Runs [block] on the main thread and exposes the result as a ListenableFuture. */
    private fun <T> future(block: suspend () -> T): ListenableFuture<T> {
        val result = SettableFuture.create<T>()
        scope.launch {
            try {
                result.set(block())
            } catch (e: Throwable) {
                result.setException(e)
            }
        }
        return result
    }

    companion object {
        const val SEEK_INCREMENT_MS = 10_000L
        private const val SAVE_INTERVAL_MS = 10_000L
        private const val TICK_MS = 100L
        private const val END_OF_TRACK_FADE_MS = 8_000L
        private const val PLAY_COUNT_MS = 30_000L
        private const val STATS_FLUSH_MS = 30_000L
        private const val RESUME_DELAY_MS = 1_200L
        private const val FLIP_PAUSE_MS = 1_800L
        private val HEADSET_TYPES = setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER,
        )
    }
}
