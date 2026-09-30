package com.muso.music.lyrics

import android.content.Context
import android.util.LruCache
import com.muso.music.constants.LyricsProviderOrderKey
import com.muso.music.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.muso.music.models.MediaMetadata
import com.muso.music.utils.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class LyricsHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val cache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)
    private val lyricsMemoryCache = LruCache<String, String>(MEMORY_CACHE_SIZE)

    /**
     * Resolves the provider chain from DataStore with the *suspend* API — no runBlocking, so a
     * preferences read can never block the calling thread (toggle presses and playback stay lag-free).
     */
    private suspend fun getProviders(): List<LyricsProvider> {
        val order = context.dataStore.data.first()[LyricsProviderOrderKey]
        return LyricsProviderRegistry.getOrderedProviders(order)
    }

    suspend fun getLyrics(mediaMetadata: MediaMetadata): String = coroutineScope {
        // Ultra-fast lyrics (user request): results are held in a small
        // in-memory cache keyed by song id, so revisiting a song (or a
        // prefetch + the real fetch racing) never re-hits the providers.
        lyricsMemoryCache.get(mediaMetadata.id)?.let { return@coroutineScope it }

        val providers = getProviders().filter { it.isEnabled(context) }
        if (providers.isEmpty()) return@coroutineScope LYRICS_NOT_FOUND

        // All enabled providers are queried IN PARALLEL; results are taken in
        // the user's priority order, so the first provider that actually has
        // the lyrics wins - no more waiting for each failing provider in
        // sequence before the next one is even asked.
        val artists = mediaMetadata.artists.joinToString { it.name }
        val pending = providers.map { provider ->
            async(Dispatchers.IO) {
                runCatching {
                    provider.getLyrics(
                        mediaMetadata.id,
                        mediaMetadata.title,
                        artists,
                        mediaMetadata.duration,
                        mediaMetadata.album?.title,
                    ).getOrThrow()
                }
            }
        }
        for (job in pending) {
            val lyrics = runCatching { job.await().getOrThrow() }.getOrNull()
            if (!lyrics.isNullOrBlank()) {
                pending.forEach { it.cancel() }
                lyricsMemoryCache.put(mediaMetadata.id, lyrics)
                return@coroutineScope lyrics
            }
        }
        LYRICS_NOT_FOUND
    }

    suspend fun getAllLyrics(
        mediaId: String,
        songTitle: String,
        songArtists: String,
        duration: Int,
        callback: (LyricsResult) -> Unit,
    ) {
        val cacheKey = "$songArtists-$songTitle".replace(" ", "")
        cache.get(cacheKey)?.let { results ->
            results.forEach {
                callback(it)
            }
            return
        }
        val allResult = mutableListOf<LyricsResult>()
        getProviders().forEach { provider ->
            if (provider.isEnabled(context)) {
                provider.getAllLyrics(mediaId, songTitle, songArtists, duration, null) { lyrics ->
                    val result = LyricsResult(provider.name, lyrics)
                    allResult += result
                    callback(result)
                }
            }
        }
        cache.put(cacheKey, allResult)
    }

    companion object {
        private const val MAX_CACHE_SIZE = 3
        private const val MEMORY_CACHE_SIZE = 128
    }
}

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)
