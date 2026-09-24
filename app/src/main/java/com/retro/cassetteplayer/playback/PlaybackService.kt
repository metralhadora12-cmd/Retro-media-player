package com.retro.cassetteplayer.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Hosts the ExoPlayer inside a [MediaSessionService] so playback keeps going in the
 * background and is exposed to the system media notification / lock screen.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var sessionStore: SessionStore
    private val handler = Handler(Looper.getMainLooper())

    /** Saves the position every 10 s while playing, so a killed app resumes close to where it was. */
    private val periodicSave = object : Runnable {
        override fun run() {
            mediaSession?.player?.let { if (it.isPlaying) sessionStore.save(it) }
            handler.postDelayed(this, SAVE_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Lossless / hi-res: platform decoders first, the bundled FFmpeg decoder as a
        // fallback (e.g. ALAC), and float PCM output so 24-bit audio keeps its precision.
        val renderersFactory = DefaultRenderersFactory(this)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setEnableAudioFloatOutput(true)
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

        // Remember the session (queue, track, position, modes) to resume it next time.
        sessionStore = SessionStore(this)
        player.addListener(object : Player.Listener {
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
            }
        })
        handler.postDelayed(periodicSave, SAVE_INTERVAL_MS)

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(SessionCallback)
            .apply { if (sessionActivity != null) setSessionActivity(sessionActivity) }
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        mediaSession?.player?.let(sessionStore::save)
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0 ||
            player.playbackState == Player.STATE_ENDED
        ) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(periodicSave)
        mediaSession?.player?.let(sessionStore::save)
        EqualizerManager.release()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    /** Media items arriving from controllers carry their URI in the request metadata. */
    private object SessionCallback : MediaSession.Callback {
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> {
            val resolved = mediaItems.map { item ->
                val uri = item.requestMetadata.mediaUri ?: item.localConfiguration?.uri
                if (uri == null) item else item.buildUpon().setUri(uri).build()
            }
            return Futures.immediateFuture(resolved.toMutableList())
        }
    }

    companion object {
        const val SEEK_INCREMENT_MS = 10_000L
        private const val SAVE_INTERVAL_MS = 10_000L
    }
}
