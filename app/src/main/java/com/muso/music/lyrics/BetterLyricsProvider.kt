package com.muso.music.lyrics

import android.content.Context
import echo.music.iad1tya.betterlyrics.BetterLyrics
import com.muso.music.constants.EnableBetterLyricsKey
import com.muso.music.utils.dataStore
import com.muso.music.utils.get

object BetterLyricsProvider : LyricsProvider {
    override val name = "BetterLyrics"

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableBetterLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = BetterLyrics.getLyrics(title, artist, duration, album)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        callback: (String) -> Unit,
    ) {
        BetterLyrics.getAllLyrics(title, artist, duration, album, callback)
    }
}
