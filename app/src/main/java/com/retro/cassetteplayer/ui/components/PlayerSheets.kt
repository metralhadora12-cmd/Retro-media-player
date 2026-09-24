package com.retro.cassetteplayer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.TimerOff
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.retro.cassetteplayer.R
import com.retro.cassetteplayer.playback.SleepTimer
import com.retro.cassetteplayer.ui.theme.InkSurface
import com.retro.cassetteplayer.ui.theme.TapeOrange
import com.retro.cassetteplayer.ui.theme.TextPrimary
import com.retro.cassetteplayer.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

private val SLEEP_OPTIONS = listOf(15, 30, 45, 60, 90)

/** Remaining time of the sleep timer, ticking every second ("12:34"), or null when off. */
@Composable
fun sleepTimerLabel(state: SleepTimer.State): String? {
    var now by remember { mutableLongStateOf(android.os.SystemClock.elapsedRealtime()) }
    LaunchedEffect(state) {
        while (state.endAtElapsed != null) {
            now = android.os.SystemClock.elapsedRealtime()
            delay(1_000)
        }
    }
    return when {
        state.endAtElapsed != null -> stringResource(R.string.sleep_remaining, formatClock(state.remainingMs(now)))
        state.endOfTrack -> stringResource(R.string.sleep_end_of_track_active)
        else -> null
    }
}

fun formatClock(ms: Long): String {
    val totalSeconds = (ms + 999) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    else String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    state: SleepTimer.State,
    onStart: (Int) -> Unit,
    onEndOfTrack: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = InkSurface,
    ) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(
                stringResource(R.string.player_sleep_timer),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            sleepTimerLabel(state)?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = TapeOrange, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
            }
            Spacer(Modifier.size(8.dp))
            SLEEP_OPTIONS.forEach { minutes ->
                SheetRow(Icons.Rounded.Timer, stringResource(R.string.sleep_minutes, minutes)) { onStart(minutes) }
            }
            SheetRow(Icons.Rounded.Bedtime, stringResource(R.string.sleep_end_of_track), onEndOfTrack)
            if (state.active) SheetRow(Icons.Rounded.TimerOff, stringResource(R.string.sleep_off), onCancel)
        }
    }
}

@Composable
private fun SheetRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
    }
}

private val SPEED_PRESETS = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)

/** Speed 0.5×–2× and pitch in semitones (−6…+6). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedSheet(
    speed: Float,
    pitch: Float,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val sliderColors = SliderDefaults.colors(
        thumbColor = TapeOrange,
        activeTrackColor = TapeOrange,
        inactiveTrackColor = TextSecondary.copy(alpha = 0.3f),
    )
    val semitones = (12 * log2(pitch.toDouble())).roundToInt()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = InkSurface,
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.player_speed), style = MaterialTheme.typography.titleLarge, color = TextPrimary, modifier = Modifier.weight(1f))
                PillButton(stringResource(R.string.speed_reset), onClick = {
                    onSpeedChange(1f)
                    onPitchChange(1f)
                })
            }
            Spacer(Modifier.size(16.dp))
            Text(
                "${stringResource(R.string.speed_label)} · ${stringResource(R.string.speed_value, formatSpeed(speed))}",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Slider(
                value = speed,
                onValueChange = { onSpeedChange((it * 20).roundToInt() / 20f) },
                valueRange = 0.5f..2f,
                colors = sliderColors,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SPEED_PRESETS) { preset ->
                    RetroChip(
                        text = stringResource(R.string.speed_value, formatSpeed(preset)),
                        selected = preset == speed,
                        onClick = { onSpeedChange(preset) },
                    )
                }
            }
            Spacer(Modifier.size(20.dp))
            Text(
                "${stringResource(R.string.pitch_label)} · ${stringResource(R.string.pitch_semitones, if (semitones > 0) "+$semitones" else "$semitones")}",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Slider(
                value = semitones.toFloat(),
                onValueChange = { onPitchChange(2.0.pow(it.roundToInt() / 12.0).toFloat()) },
                valueRange = -6f..6f,
                steps = 11,
                colors = sliderColors,
            )
            Text(stringResource(R.string.speed_hires_note), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

private fun formatSpeed(speed: Float): String =
    String.format(Locale.ROOT, "%.2f", speed).trimEnd('0').trimEnd('.')
