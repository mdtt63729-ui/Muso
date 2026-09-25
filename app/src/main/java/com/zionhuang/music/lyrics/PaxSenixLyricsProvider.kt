package com.zionhuang.music.lyrics

import android.content.Context
import com.music.paxsenix.Paxsenix
import com.zionhuang.music.constants.EnablePaxsenixKey
import com.zionhuang.music.utils.dataStore
import com.zionhuang.music.utils.get

object PaxSenixLyricsProvider : LyricsProvider {
    override val name = "Paxsenix"

    override fun isEnabled(context: Context): Boolean {
        val enabled = context.dataStore[EnablePaxsenixKey] ?: true
        if (enabled) {
            val appVersion = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
            } catch (e: Exception) {
                "Unknown"
            }
            Paxsenix.init(appVersion)
        }
        return enabled
    }

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> =
        try {
            Paxsenix.getLyrics(title, artist, duration, album)
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        callback: (String) -> Unit,
    ) {
        try {
            Paxsenix.getAllLyrics(title, artist, duration, album, callback)
        } catch (_: Exception) {
        }
    }
}
