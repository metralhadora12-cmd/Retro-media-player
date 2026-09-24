package com.retro.cassetteplayer

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.annotation.StringRes
import java.util.Locale

/**
 * Per-app language. Android 13+ uses the system per-app locale (it also shows up in the
 * system's App languages settings); older versions keep the choice in preferences and
 * wrap the Activity context with it.
 */
object AppLanguage {

    data class Option(val tag: String, @StringRes val label: Int)

    /** "" = follow the system language. */
    val options = listOf(
        Option("", R.string.language_system),
        Option("pt-BR", R.string.language_pt),
        Option("en", R.string.language_en),
        Option("es", R.string.language_es),
    )

    private const val PREFS = "app_language"
    private const val KEY_TAG = "tag"

    fun current(context: Context): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)
                ?.applicationLocales?.takeUnless { it.isEmpty }?.get(0)?.toLanguageTag()
                ?.let(::matchOption).orEmpty()
        } else {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TAG, "").orEmpty()
        }

    fun apply(activity: Activity, tag: String) {
        if (tag == current(activity)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // The system recreates the activity with the new locale.
            activity.getSystemService(LocaleManager::class.java)?.applicationLocales =
                if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        } else {
            activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_TAG, tag).apply()
            activity.recreate()
        }
    }

    /** Used from attachBaseContext on Android 12 and below. */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = base.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TAG, "").orEmpty()
        if (tag.isEmpty()) return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
        return base.createConfigurationContext(config)
    }

    /** Maps e.g. "pt" or "en-US" onto the closest offered option. */
    private fun matchOption(tag: String): String {
        options.firstOrNull { it.tag.equals(tag, ignoreCase = true) }?.let { return it.tag }
        val language = tag.substringBefore('-')
        return options.firstOrNull { it.tag.substringBefore('-').equals(language, ignoreCase = true) }?.tag ?: tag
    }
}
