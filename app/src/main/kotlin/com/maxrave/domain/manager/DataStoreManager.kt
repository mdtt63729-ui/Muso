package com.maxrave.domain.manager

import androidx.datastore.preferences.core.edit
import com.muso.music.constants.EndlessQueueKey
import com.muso.music.utils.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Muso integration shim of the SimpMusic DataStoreManager: only the settings
 * the player suite reads are provided. endlessQueue is backed by Muso's own
 * DataStore keys; the defaults below are SimpMusic's out-of-the-box values.
 */
class DataStoreManager(
    private val context: android.content.Context,
) {
    enum class ProxyType {
        PROXY_TYPE_HTTP,
        PROXY_TYPE_SOCKS,
    }

    companion object {
        const val TRUE = "true"
        const val FALSE = "false"
        // Suite style keys: values match what the hosting layer feeds into the flows below.
        const val LYRICS_STYLE_CLASSIC = "1"
        const val LYRICS_STYLE_APPLE_MUSIC = "2"
        const val NOW_PLAYING_STYLE_SPOTIFY = "SPOTIFY"
        const val NOW_PLAYING_STYLE_M3_EXPRESSIVE = "M3_EXPRESSIVE"
        const val NOW_PLAYING_STYLE_APPLE_MUSIC = "APPLE_MUSIC"
        const val PROXY_TYPE_HTTP = "PROXY_TYPE_HTTP"
        const val PROXY_TYPE_SOCKS = "PROXY_TYPE_SOCKS"
        // Theme (values match upstream)
        const val THEME_MODE_SYSTEM = "SYSTEM"
        const val THEME_MODE_DARK = "DARK"
        const val THEME_MODE_LIGHT = "LIGHT"
        const val THEME_COLOR_DEFAULT = "DEFAULT"
        const val THEME_COLOR_WALLPAPER = "WALLPAPER"
        const val THEME_COLOR_CUSTOM = "CUSTOM"
        const val DEFAULT_THEME_COLOR_HEX = "FF8ECAE6"
        // Lyrics providers (ModalBottomSheet comparisons) — values match SimpMusic upstream
        const val SIMPMUSIC = "simpmusic"
        const val YOUTUBE = "youtube"
        const val LRCLIB = "lrclib"
        const val BETTER_LYRICS = "better_lyrics"
    }

    /**
     * REAL storage (was a no-op stub): the suite's Endless queue switch now
     * persists into Muso's DataStore and MusicService honors it by appending a
     * radio tail before the queue runs out.
     */
    val endlessQueue: Flow<String> = context.dataStore.data.map { preferences ->
        if (preferences[com.muso.music.constants.EndlessQueueKey] ?: true) TRUE else FALSE
    }
    /** Liquid glass surfaces (navigation bar / mini player). Muso's Appearance setting feeds this. */
    val enableLiquidGlass = MutableStateFlow(FALSE)
    val lyricsOffsetMs = MutableStateFlow(0)
    val crossfadeEnabled = MutableStateFlow("false")
    val playbackSpeed = MutableStateFlow(1f)
    val pitch = MutableStateFlow(0)
    val lyricsStyle = MutableStateFlow("1")
    val nowPlayingStyle = MutableStateFlow(NOW_PLAYING_STYLE_SPOTIFY)
    val romanizationLanguages = MutableStateFlow("")

    suspend fun setEndlessQueue(endlessQueue: Boolean) {
        context.dataStore.edit { settings ->
            settings[com.muso.music.constants.EndlessQueueKey] = endlessQueue
        }
    }
}
