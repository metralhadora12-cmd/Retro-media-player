package com.retro.cassetteplayer.data

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import com.retro.cassetteplayer.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Cassette shell / label style drawn in the player, the widget and shared mixtapes. */
enum class CassetteModel(@StringRes val label: Int) {
    NORMAL(R.string.cassette_model_normal),
    CHROME(R.string.cassette_model_chrome),
    METAL(R.string.cassette_model_metal),
}

/** Colour of the label's stripe band. */
enum class LabelColor(val argb: Long, @StringRes val label: Int) {
    ORANGE(0xFFD9621C, R.string.label_color_orange),
    RED(0xFFC62E2E, R.string.label_color_red),
    BLUE(0xFF2F6FB5, R.string.label_color_blue),
    GREEN(0xFF3E8E4E, R.string.label_color_green),
    PURPLE(0xFF7A4BB0, R.string.label_color_purple),
    YELLOW(0xFFE0A91B, R.string.label_color_yellow),
}

enum class ReplayGainMode(@StringRes val label: Int) {
    OFF(R.string.settings_off),
    TRACK(R.string.replay_gain_track),
    ALBUM(R.string.replay_gain_album),
}

/** One persisted setting exposed as a [StateFlow]. */
class Setting<T> internal constructor(
    private val key: String,
    private val default: T,
    private val read: SharedPreferences.(String, T) -> T,
    private val write: SharedPreferences.Editor.(String, T) -> Unit,
) {
    private val _flow = MutableStateFlow(default)
    val flow: StateFlow<T> = _flow.asStateFlow()
    val value: T get() = _flow.value

    internal fun load(prefs: SharedPreferences) {
        _flow.value = runCatching { prefs.read(key, default) }.getOrDefault(default)
    }

    fun set(value: T) {
        _flow.value = value
        AppSettings.prefs?.edit()?.apply { write(key, value) }?.apply()
    }
}

/**
 * App-wide settings shared by the UI and the playback service (same process). The
 * service collects the flows, so changes apply right away.
 */
object AppSettings {
    internal var prefs: SharedPreferences? = null
        private set

    private val all = mutableListOf<Setting<*>>()

    private fun bool(key: String, default: Boolean) =
        Setting(key, default, { k, d -> getBoolean(k, d) }, { k, v -> putBoolean(k, v) }).also { all += it }

    private fun int(key: String, default: Int) =
        Setting(key, default, { k, d -> getInt(k, d) }, { k, v -> putInt(k, v) }).also { all += it }

    private fun float(key: String, default: Float) =
        Setting(key, default, { k, d -> getFloat(k, d) }, { k, v -> putFloat(k, v) }).also { all += it }

    private inline fun <reified E : Enum<E>> enum(key: String, default: E) =
        Setting(
            key,
            default,
            { k, d -> getString(k, null)?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: d },
            { k, v -> putString(k, v.name) },
        ).also { all += it }

    // Playback
    /** Fade between tracks, in seconds (0 = off). */
    val crossfadeSeconds = int("crossfade_seconds", 0)
    val replayGain = enum("replay_gain", ReplayGainMode.OFF)
    val resumeOnConnect = bool("resume_on_connect", false)
    /** 24-bit float output for hi-res files (speed and tape effects don't apply to those). */
    val hiResOutput = bool("hi_res_output", true)
    val speed = float("speed", 1f)
    val pitch = float("pitch", 1f)

    // Tape effects
    val tapeHiss = bool("tape_hiss", false)
    val wowFlutter = bool("wow_flutter", false)
    val keyClicks = bool("key_clicks", false)

    // Look
    val cassetteModel = enum("cassette_model", CassetteModel.NORMAL)
    val labelColor = enum("label_color", LabelColor.ORANGE)
    val handwrittenLabel = bool("handwritten_label", true)
    val coverColors = bool("cover_colors", true)
    val sideAB = bool("side_ab", false)

    // Integrations
    val scrobbling = bool("scrobbling", false)

    fun init(context: Context) {
        if (prefs != null) return
        val shared = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs = shared
        all.forEach { it.load(shared) }
    }
}
