package com.retro.cassetteplayer.playback

import androidx.media3.common.Player
import com.retro.cassetteplayer.data.AppSettings

/**
 * "Lado A / Lado B": with the option on, the queue is a two-sided tape. The first half
 * (in queue order) is side A, the rest side B. Not used while shuffling.
 */
data class TapeSides(val sideBStart: Int) {

    fun sideOf(index: Int): Char = if (index >= sideBStart) 'B' else 'A'

    companion object {
        fun of(player: Player): TapeSides? {
            val count = player.mediaItemCount
            if (!AppSettings.sideAB.value || player.shuffleModeEnabled || count < 2) return null
            return TapeSides((count + 1) / 2)
        }
    }
}
