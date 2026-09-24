package com.retro.cassetteplayer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.retro.cassetteplayer.MainActivity
import com.retro.cassetteplayer.R
import com.retro.cassetteplayer.playback.PlaybackService
import java.util.concurrent.Executors
import com.google.common.util.concurrent.ListenableFuture
import com.retro.cassetteplayer.playback.LibraryBrowser
import com.retro.cassetteplayer.playback.SessionStore
import com.retro.cassetteplayer.playback.loadResumableSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** What the widget shows; pushed by [PlaybackService] on every player change. */
data class WidgetState(
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val artworkUri: Uri? = null,
)

/** Home-screen player widget. Buttons talk to the playback service through a MediaController. */
class PlayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        PlayerWidget.render(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == ACTION_PLAY_PAUSE || action == ACTION_NEXT || action == ACTION_PREVIOUS) {
            val pending = goAsync()
            PlayerWidget.sendCommand(context.applicationContext, action) { pending.finish() }
        } else {
            super.onReceive(context, intent)
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.retro.cassetteplayer.widget.PLAY_PAUSE"
        const val ACTION_NEXT = "com.retro.cassetteplayer.widget.NEXT"
        const val ACTION_PREVIOUS = "com.retro.cassetteplayer.widget.PREVIOUS"
    }
}

object PlayerWidget {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Volatile private var state = WidgetState()
    @Volatile private var artworkFor: Uri? = null
    @Volatile private var artwork: Bitmap? = null

    /** Called by the service when what's playing changes. */
    fun update(context: Context, newState: WidgetState) {
        if (newState == state) return
        val artworkChanged = newState.artworkUri != state.artworkUri
        state = newState
        if (!hasWidgets(context)) return
        if (artworkChanged) {
            val uri = newState.artworkUri
            io.execute {
                val bitmap = uri?.let { loadArtwork(context, it) }
                artworkFor = uri
                artwork = bitmap
                mainHandler.post { render(context) }
            }
        } else {
            render(context)
        }
    }

    fun render(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, PlayerWidgetProvider::class.java))
        if (ids.isEmpty()) return
        val s = state
        val views = RemoteViews(context.packageName, R.layout.widget_player).apply {
            setTextViewText(R.id.widget_title, s.title.ifBlank { context.getString(R.string.app_name) })
            setTextViewText(
                R.id.widget_artist,
                if (s.title.isBlank()) context.getString(R.string.widget_idle) else s.artist.ifBlank { context.getString(R.string.unknown_artist) },
            )
            val bitmap = artwork.takeIf { artworkFor == s.artworkUri }
            if (bitmap != null) setImageViewBitmap(R.id.widget_cover, bitmap)
            else setImageViewResource(R.id.widget_cover, R.drawable.ic_launcher_foreground)

            setImageViewResource(R.id.widget_play, if (s.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play)
            setContentDescription(
                R.id.widget_play,
                context.getString(if (s.isPlaying) R.string.action_pause else R.string.action_play),
            )
            // Spinning reels (animated progress bars) while playing, still ones when paused
            val spin = if (s.isPlaying) View.VISIBLE else View.GONE
            val still = if (s.isPlaying) View.GONE else View.VISIBLE
            setViewVisibility(R.id.widget_reel_left_spin, spin)
            setViewVisibility(R.id.widget_reel_right_spin, spin)
            setViewVisibility(R.id.widget_reel_left, still)
            setViewVisibility(R.id.widget_reel_right, still)

            setOnClickPendingIntent(R.id.widget_play, commandIntent(context, PlayerWidgetProvider.ACTION_PLAY_PAUSE, 1))
            setOnClickPendingIntent(R.id.widget_next, commandIntent(context, PlayerWidgetProvider.ACTION_NEXT, 2))
            setOnClickPendingIntent(R.id.widget_prev, commandIntent(context, PlayerWidgetProvider.ACTION_PREVIOUS, 3))
            val open = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            setOnClickPendingIntent(R.id.widget_cover, open)
            setOnClickPendingIntent(R.id.widget_title, open)
            setOnClickPendingIntent(R.id.widget_artist, open)
        }
        manager.updateAppWidget(ids, views)
    }

    /** Connects to the playback service, runs the command, then lets the receiver finish. */
    fun sendCommand(context: Context, action: String, done: () -> Unit) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            val controller = runCatching { future.get() }.getOrNull()
            if (controller != null) {
                when (action) {
                    PlayerWidgetProvider.ACTION_NEXT -> controller.seekToNext()
                    PlayerWidgetProvider.ACTION_PREVIOUS -> controller.seekToPrevious()
                    else -> when {
                        controller.isPlaying -> controller.pause()
                        controller.playbackState == Player.STATE_ENDED -> {
                            controller.seekToDefaultPosition(0)
                            controller.play()
                        }
                        controller.mediaItemCount == 0 -> {
                            // Nothing loaded (e.g. after a reboot): resume the last session
                            scope.launch {
                                val session = loadResumableSession(SessionStore(context), LibraryBrowser(context))
                                if (session != null) {
                                    controller.shuffleModeEnabled = session.shuffle
                                    controller.repeatMode = session.repeatMode
                                    controller.setMediaItems(session.items, session.index, session.positionMs)
                                    controller.prepare()
                                    controller.play()
                                }
                                release(future)
                                done()
                            }
                            return@addListener
                        }
                        else -> {
                            if (controller.playbackState == Player.STATE_IDLE) controller.prepare()
                            controller.play()
                        }
                    }
                }
                release(future)
            }
            done()
        }, ContextCompat.getMainExecutor(context))
    }

    /** Gives the commands time to reach the session before disconnecting. */
    private fun release(future: ListenableFuture<MediaController>) {
        mainHandler.postDelayed({ MediaController.releaseFuture(future) }, 3_000)
    }

    private fun commandIntent(context: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, PlayerWidgetProvider::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun hasWidgets(context: Context): Boolean = runCatching {
        AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, PlayerWidgetProvider::class.java))
            .isNotEmpty()
    }.getOrDefault(false)

    /** Small cover (RemoteViews have a tight size budget). */
    private fun loadArtwork(context: Context, uri: Uri): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= ARTWORK_PX) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return null
        Bitmap.createScaledBitmap(decoded, ARTWORK_PX, ARTWORK_PX, true)
    }.getOrNull()

    private const val ARTWORK_PX = 180
}
