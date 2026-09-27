package com.maxrave.simpmusic.utils

import android.content.Context
import androidx.annotation.StringRes
import com.muso.music.R

/** Suite resource accessor: wired to the application context at startup. */
object SuiteRes {
    @Volatile var context: Context? = null
    fun get(@StringRes id: Int, vararg args: Any): String {
        val c = context ?: return ""
        return if (args.isEmpty()) c.getString(id) else c.getString(id, *args)
    }
}

suspend fun getString(@StringRes id: Int, vararg args: Any): String = SuiteRes.get(id, *args)
