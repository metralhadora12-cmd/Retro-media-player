package com.retro.cassetteplayer.playback

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sleep timer: pauses playback after a delay or at the end of the current track, fading
 * the volume out first. The UI sets it; [PlaybackService] enforces it.
 */
object SleepTimer {

    data class State(
        /** SystemClock.elapsedRealtime() at which playback stops, or null. */
        val endAtElapsed: Long? = null,
        val endOfTrack: Boolean = false,
    ) {
        val active: Boolean get() = endAtElapsed != null || endOfTrack

        fun remainingMs(now: Long = SystemClock.elapsedRealtime()): Long =
            endAtElapsed?.let { (it - now).coerceAtLeast(0L) } ?: 0L
    }

    /** Volume fades out over the last seconds before stopping. */
    const val FADE_MS = 20_000L

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun start(minutes: Int) {
        _state.value = State(endAtElapsed = SystemClock.elapsedRealtime() + minutes * 60_000L)
    }

    fun stopAtEndOfTrack() {
        _state.value = State(endOfTrack = true)
    }

    fun cancel() {
        _state.value = State()
    }
}
