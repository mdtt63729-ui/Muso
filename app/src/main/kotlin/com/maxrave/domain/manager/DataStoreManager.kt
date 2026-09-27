package com.maxrave.domain.manager

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Muso integration shim of the SimpMusic DataStoreManager: only the settings
 * the player suite reads are provided. Phase 2 can back these with Muso's own
 * DataStore keys; the defaults below are SimpMusic's out-of-the-box values.
 */
class DataStoreManager {
    companion object {
        const val TRUE = "true"
        const val FALSE = "false"
    }

    val endlessQueue = MutableStateFlow("false")
    /** Liquid glass surfaces (navigation bar / mini player). Muso's Appearance setting feeds this. */
    val enableLiquidGlass = MutableStateFlow(TRUE)
    val lyricsOffsetMs = MutableStateFlow(0)
    val crossfadeEnabled = MutableStateFlow("false")
    val playbackSpeed = MutableStateFlow(1f)
    val pitch = MutableStateFlow(0)
    val lyricsStyle = MutableStateFlow("1")
    val romanizationLanguages = MutableStateFlow("")

    suspend fun setEndlessQueue(endlessQueue: Boolean) { }
}
