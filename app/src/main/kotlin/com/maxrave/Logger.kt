package com.maxrave.logger

import android.util.Log

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

object Logger {
    // Tags suppressed at all log levels. Add a tag here to silence its logs globally.
    private val mutedTags =
        setOf(
            "DiscordWebSocket",
        )

    private fun isMuted(tag: String): Boolean = tag in mutedTags

    fun d(
        tag: String,
        message: String,
    ) {
        if (isMuted(tag)) return
        Log.d(tag, message)
    }

    fun i(
        tag: String,
        message: String,
    ) {
        if (isMuted(tag)) return
        Log.i(tag, message)
    }

    fun w(
        tag: String,
        message: String,
    ) {
        if (isMuted(tag)) return
        Log.w(tag, message)
    }

    fun e(
        tag: String,
        message: String,
    ) {
        if (isMuted(tag)) return
        Log.e(tag, message)
    }
}
