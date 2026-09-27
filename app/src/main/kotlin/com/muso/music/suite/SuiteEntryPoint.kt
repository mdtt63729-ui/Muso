package com.muso.music.suite

import com.maxrave.simpmusic.ui.component.SuiteRes

/**
 * Hilt entry point through which the Koin-bound SimpMusic suite repositories
 * reach Muso's Hilt-managed singletons (database, download manager).
 */
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface SuiteEntryPoint {
    fun database(): com.muso.music.db.MusicDatabase

    fun downloadUtil(): com.muso.music.playback.DownloadUtil

    fun appContext(): android.content.Context
}
