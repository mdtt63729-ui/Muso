package com.zionhuang.music.lyrics

import android.content.Context
import android.util.LruCache
import com.zionhuang.music.constants.LyricsProviderOrderKey
import com.zionhuang.music.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.zionhuang.music.models.MediaMetadata
import com.zionhuang.music.utils.dataStore
import com.zionhuang.music.utils.get
import com.zionhuang.music.utils.reportException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class LyricsHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val cache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)

    private val lyricsProviders: List<LyricsProvider>
        get() = LyricsProviderRegistry.getOrderedProviders(context.dataStore[LyricsProviderOrderKey])

    suspend fun getLyrics(mediaMetadata: MediaMetadata): String {
        val cached = cache.get(mediaMetadata.id)?.firstOrNull()
        if (cached != null) {
            return cached.lyrics
        }
        lyricsProviders.forEach { provider ->
            if (provider.isEnabled(context)) {
                provider.getLyrics(
                    mediaMetadata.id,
                    mediaMetadata.title,
                    mediaMetadata.artists.joinToString { it.name },
                    mediaMetadata.duration,
                    mediaMetadata.album?.title
                ).onSuccess { lyrics ->
                    return lyrics
                }.onFailure {
                    reportException(it)
                }
            }
        }
        return LYRICS_NOT_FOUND
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
        lyricsProviders.forEach { provider ->
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
    }
}

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)
