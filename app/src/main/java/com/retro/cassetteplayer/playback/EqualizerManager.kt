package com.retro.cassetteplayer.playback

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Built-in equalizer: an [Equalizer] (plus [BassBoost] when supported) attached to the
 * player's audio session inside [PlaybackService]. The UI (same process) reads [state]
 * and calls the setters; settings are persisted and re-applied on every attach.
 */
object EqualizerManager {

    data class Band(val index: Int, val centerHz: Int, val level: Int)

    data class State(
        /** Effect attached to a playing session. */
        val attached: Boolean = false,
        /** Attaching failed: the device doesn't provide the effect. */
        val unsupported: Boolean = false,
        val enabled: Boolean = false,
        /** Band level range in millibels. */
        val minLevel: Int = -1500,
        val maxLevel: Int = 1500,
        val bands: List<Band> = emptyList(),
        val presets: List<String> = emptyList(),
        /** Selected system preset, or [CUSTOM_PRESET]. */
        val preset: Int = CUSTOM_PRESET,
        val bassBoostSupported: Boolean = false,
        /** 0..1000 */
        val bassBoost: Int = 0,
    )

    const val CUSTOM_PRESET = -1

    private const val PREFS = "equalizer"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_PRESET = "preset"
    private const val KEY_LEVELS = "levels"
    private const val KEY_BASS = "bass_boost"

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var appContext: Context? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    private val prefs get() = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun init(context: Context) {
        appContext = context.applicationContext
        val saved = prefs ?: return
        _state.value = _state.value.copy(
            enabled = saved.getBoolean(KEY_ENABLED, false),
            preset = saved.getInt(KEY_PRESET, CUSTOM_PRESET),
            bassBoost = saved.getInt(KEY_BASS, 0),
        )
    }

    /** Attaches the effects to [audioSessionId] and applies the saved settings. */
    fun attach(audioSessionId: Int) {
        release()
        val eq = runCatching { Equalizer(0, audioSessionId) }.getOrNull()
        if (eq == null) {
            _state.value = _state.value.copy(attached = false, unsupported = true)
            return
        }
        equalizer = eq
        bassBoost = runCatching { BassBoost(0, audioSessionId) }.getOrNull()
            ?.takeIf { it.strengthSupported }

        val saved = prefs
        val enabled = saved?.getBoolean(KEY_ENABLED, false) ?: false
        val preset = saved?.getInt(KEY_PRESET, CUSTOM_PRESET) ?: CUSTOM_PRESET
        val savedLevels = saved?.getString(KEY_LEVELS, null)
            ?.split(',')?.mapNotNull { it.toIntOrNull() }.orEmpty()
        val bass = saved?.getInt(KEY_BASS, 0) ?: 0

        runCatching {
            if (preset != CUSTOM_PRESET && preset < eq.numberOfPresets) {
                eq.usePreset(preset.toShort())
            } else {
                savedLevels.forEachIndexed { band, level ->
                    if (band < eq.numberOfBands) eq.setBandLevel(band.toShort(), level.toShort())
                }
            }
            eq.setEnabled(enabled)
            bassBoost?.setStrength(bass.toShort())
            bassBoost?.setEnabled(enabled && bass > 0)
        }
        publish(enabled = enabled, preset = preset, bass = bass)
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        equalizer = null
        bassBoost = null
        _state.value = _state.value.copy(attached = false)
    }

    fun setEnabled(enabled: Boolean) {
        runCatching {
            equalizer?.setEnabled(enabled)
            bassBoost?.setEnabled(enabled && _state.value.bassBoost > 0)
        }
        prefs?.edit()?.putBoolean(KEY_ENABLED, enabled)?.apply()
        _state.value = _state.value.copy(enabled = enabled)
    }

    fun setBandLevel(band: Int, level: Int) {
        val eq = equalizer ?: return
        val clamped = level.coerceIn(_state.value.minLevel, _state.value.maxLevel)
        runCatching { eq.setBandLevel(band.toShort(), clamped.toShort()) }
        saveCustomLevels()
        publish(preset = CUSTOM_PRESET)
    }

    fun usePreset(preset: Int) {
        val eq = equalizer ?: return
        runCatching { eq.usePreset(preset.toShort()) }
        prefs?.edit()?.putInt(KEY_PRESET, preset)?.apply()
        publish(preset = preset)
    }

    /** Flattens every band to 0 dB. */
    fun reset() {
        val eq = equalizer ?: return
        runCatching {
            for (band in 0 until eq.numberOfBands) eq.setBandLevel(band.toShort(), 0.toShort())
        }
        saveCustomLevels()
        setBassBoost(0)
        publish(preset = CUSTOM_PRESET)
    }

    fun setBassBoost(strength: Int) {
        val value = strength.coerceIn(0, 1000)
        runCatching {
            bassBoost?.setStrength(value.toShort())
            bassBoost?.setEnabled(_state.value.enabled && value > 0)
        }
        prefs?.edit()?.putInt(KEY_BASS, value)?.apply()
        _state.value = _state.value.copy(bassBoost = value)
    }

    private fun saveCustomLevels() {
        val eq = equalizer ?: return
        val levels = runCatching {
            (0 until eq.numberOfBands).joinToString(",") { eq.getBandLevel(it.toShort()).toString() }
        }.getOrNull() ?: return
        prefs?.edit()?.putString(KEY_LEVELS, levels)?.putInt(KEY_PRESET, CUSTOM_PRESET)?.apply()
    }

    private fun publish(
        enabled: Boolean = _state.value.enabled,
        preset: Int = _state.value.preset,
        bass: Int = _state.value.bassBoost,
    ) {
        val eq = equalizer ?: return
        runCatching {
            val range = eq.bandLevelRange
            _state.value = State(
                attached = true,
                unsupported = false,
                enabled = enabled,
                minLevel = range[0].toInt(),
                maxLevel = range[1].toInt(),
                bands = (0 until eq.numberOfBands).map { band ->
                    Band(
                        index = band,
                        centerHz = eq.getCenterFreq(band.toShort()) / 1000,
                        level = eq.getBandLevel(band.toShort()).toInt(),
                    )
                },
                presets = (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) },
                preset = preset,
                bassBoostSupported = bassBoost != null,
                bassBoost = bass,
            )
        }
    }
}
