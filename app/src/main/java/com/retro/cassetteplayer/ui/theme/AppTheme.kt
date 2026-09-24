package com.retro.cassetteplayer.ui.theme

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.retro.cassetteplayer.R

enum class ThemeMode(@StringRes val label: Int) {
    DARK(R.string.theme_dark),
    LIGHT(R.string.theme_light),
    SYSTEM(R.string.theme_system),
}

/** App-wide theme state (dark by default), persisted in preferences. */
object AppTheme {
    private const val PREFS = "appearance"
    private const val KEY_MODE = "theme_mode"

    var mode by mutableStateOf(ThemeMode.DARK)
        private set

    /** Updated from the Activity so SYSTEM can follow the device's dark mode. */
    var systemDark by mutableStateOf(true)

    val isDark: Boolean
        get() = when (mode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> systemDark
        }

    val palette: AppPalette get() = if (isDark) DarkPalette else LightPalette

    fun load(context: Context) {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_MODE, null)
        mode = ThemeMode.entries.firstOrNull { it.name == saved } ?: ThemeMode.DARK
        systemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }

    fun setMode(context: Context, newMode: ThemeMode) {
        mode = newMode
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_MODE, newMode.name).apply()
    }
}
