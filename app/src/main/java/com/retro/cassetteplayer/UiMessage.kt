package com.retro.cassetteplayer

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/**
 * One-off feedback (snackbar) described by resource ids, resolved with the Activity's
 * context so it always appears in the language currently selected.
 */
sealed interface UiMessage {
    fun resolve(context: Context): String

    class Text(@StringRes val res: Int, vararg val args: Any) : UiMessage {
        override fun resolve(context: Context) = context.getString(res, *args)
    }

    class Plural(@PluralsRes val res: Int, val count: Int) : UiMessage {
        override fun resolve(context: Context) = context.resources.getQuantityString(res, count, count)
    }
}
