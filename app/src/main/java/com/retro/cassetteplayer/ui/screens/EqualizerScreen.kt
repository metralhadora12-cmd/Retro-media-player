package com.retro.cassetteplayer.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.retro.cassetteplayer.R
import com.retro.cassetteplayer.playback.EqualizerManager
import com.retro.cassetteplayer.ui.components.PillButton
import com.retro.cassetteplayer.ui.components.RetroChip
import com.retro.cassetteplayer.ui.theme.DisplayFont
import com.retro.cassetteplayer.ui.theme.Ink
import com.retro.cassetteplayer.ui.theme.InkRaised
import com.retro.cassetteplayer.ui.theme.InkSurface
import com.retro.cassetteplayer.ui.theme.TapeOrange
import com.retro.cassetteplayer.ui.theme.TextPrimary
import com.retro.cassetteplayer.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.roundToInt
import com.retro.cassetteplayer.ui.theme.Hairline

/** Built-in equalizer: on/off, presets, one retro fader per band and bass boost. */
@Composable
fun EqualizerScreen(onBack: () -> Unit) {
    val state by EqualizerManager.state.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
    ) {
        ScreenTopBar(stringResource(R.string.eq_title), onBack)

        when {
            state.unsupported -> StatusMessage(stringResource(R.string.eq_unavailable))
            !state.attached -> StatusMessage(stringResource(R.string.eq_waiting))
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
            ) {
                // On / off
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.eq_enabled),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = state.enabled,
                        onCheckedChange = EqualizerManager::setEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextPrimary,
                            checkedTrackColor = TapeOrange,
                            uncheckedTrackColor = InkRaised,
                        ),
                    )
                }

                val controlsAlpha = if (state.enabled) 1f else 0.45f
                Column(Modifier.alpha(controlsAlpha)) {
                    // Presets
                    Text(
                        stringResource(R.string.eq_presets).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TapeOrange,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp),
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            RetroChip(
                                text = stringResource(R.string.eq_custom),
                                selected = state.preset == EqualizerManager.CUSTOM_PRESET,
                                onClick = {},
                            )
                        }
                        itemsIndexed(state.presets) { index, name ->
                            RetroChip(
                                text = name,
                                selected = state.preset == index,
                                onClick = { EqualizerManager.usePreset(index) },
                            )
                        }
                    }

                    // Faders
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .background(InkSurface, RoundedCornerShape(12.dp))
                            .padding(vertical = 16.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        state.bands.forEach { band ->
                            BandFader(
                                band = band,
                                minLevel = state.minLevel,
                                maxLevel = state.maxLevel,
                                onLevelChange = { EqualizerManager.setBandLevel(band.index, it) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    // Bass boost
                    if (state.bassBoostSupported) {
                        Text(
                            stringResource(R.string.eq_bass_boost).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = TapeOrange,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                        )
                        Slider(
                            value = state.bassBoost / 1000f,
                            onValueChange = { EqualizerManager.setBassBoost((it * 1000).roundToInt()) },
                            colors = SliderDefaults.colors(
                                thumbColor = TapeOrange,
                                activeTrackColor = TapeOrange,
                                inactiveTrackColor = InkRaised,
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }

                    PillButton(
                        text = stringResource(R.string.eq_reset),
                        onClick = EqualizerManager::reset,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                    )
                }
            }
        }
    }
}

/** A mixing-desk style vertical fader: groove, 0 dB line, orange fill and a metal cap. */
@Composable
private fun BandFader(
    band: EqualizerManager.Band,
    minLevel: Int,
    maxLevel: Int,
    onLevelChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val range = (maxLevel - minLevel).coerceAtLeast(1)
    val fraction = (band.level - minLevel).toFloat() / range // 0 = bottom, 1 = top
    val frequency = formatFrequency(band.centerHz)
    val db = formatDb(band.level)
    fun levelAt(y: Float, height: Float): Int {
        val f = 1f - (y / height).coerceIn(0f, 1f)
        return (minLevel + f * range).roundToInt()
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(db, style = MaterialTheme.typography.labelSmall.copy(fontFamily = DisplayFont), color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .semantics {
                    contentDescription = frequency
                    stateDescription = db
                }
                .pointerInput(minLevel, maxLevel) {
                    detectTapGestures { offset -> onLevelChange(levelAt(offset.y, size.height.toFloat())) }
                }
                .pointerInput(minLevel, maxLevel) {
                    detectVerticalDragGestures { change, _ ->
                        change.consume()
                        onLevelChange(levelAt(change.position.y, size.height.toFloat()))
                    }
                }
        ) {
            val cx = size.width / 2f
            val capHeight = 14.dp.toPx()
            val top = capHeight / 2f
            val bottom = size.height - capHeight / 2f
            val track = bottom - top
            val zeroY = bottom - track * ((0f - minLevel) / range)
            val capY = bottom - track * fraction

            // Groove
            drawRoundRect(
                color = Color.Black,
                topLeft = Offset(cx - 2.dp.toPx(), top),
                size = Size(4.dp.toPx(), track),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
            // Scale ticks and the 0 dB line
            for (i in 0..8) {
                val y = top + track * i / 8f
                drawLine(Hairline, Offset(cx - 12.dp.toPx(), y), Offset(cx - 6.dp.toPx(), y), 1.dp.toPx())
                drawLine(Hairline, Offset(cx + 6.dp.toPx(), y), Offset(cx + 12.dp.toPx(), y), 1.dp.toPx())
            }
            drawLine(TextSecondary.copy(alpha = 0.6f), Offset(cx - 14.dp.toPx(), zeroY), Offset(cx + 14.dp.toPx(), zeroY), 1.dp.toPx())
            // Orange fill from 0 dB to the cap
            drawRect(
                color = TapeOrange,
                topLeft = Offset(cx - 2.dp.toPx(), minOf(zeroY, capY)),
                size = Size(4.dp.toPx(), kotlin.math.abs(zeroY - capY)),
            )
            // Metal cap with an orange index line
            val capWidth = 30.dp.toPx()
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFFE8E9EC), Color(0xFF9FA3AA), Color(0xFF6B6F76)),
                    startY = capY - capHeight / 2f,
                    endY = capY + capHeight / 2f,
                ),
                topLeft = Offset(cx - capWidth / 2f, capY - capHeight / 2f),
                size = Size(capWidth, capHeight),
                cornerRadius = CornerRadius(3.dp.toPx()),
            )
            drawLine(TapeOrange, Offset(cx - capWidth / 2f + 3.dp.toPx(), capY), Offset(cx + capWidth / 2f - 3.dp.toPx(), capY), 2.dp.toPx())
        }
        Spacer(Modifier.height(8.dp))
        Text(frequency, style = MaterialTheme.typography.labelSmall.copy(fontFamily = DisplayFont), color = TextPrimary)
    }
}

@Composable
private fun formatFrequency(hz: Int): String =
    if (hz >= 1000) {
        val khz = hz / 1000f
        stringResource(
            R.string.eq_band_khz,
            if (khz % 1f == 0f) khz.toInt().toString() else String.format(Locale.ROOT, "%.1f", khz),
        )
    } else {
        stringResource(R.string.eq_band_hz, hz)
    }

@Composable
private fun formatDb(levelMillibel: Int): String {
    val db = levelMillibel / 100f
    val text = String.format(Locale.ROOT, "%+.0f", db)
    return stringResource(R.string.eq_db, text)
}
