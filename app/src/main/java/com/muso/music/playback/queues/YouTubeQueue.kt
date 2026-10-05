package com.muso.music.playback.queues

import androidx.media3.common.MediaItem
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.models.WatchEndpoint
import com.muso.music.extensions.toMediaItem
import com.muso.music.models.MediaMetadata
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class YouTubeQueue(
    private var endpoint: WatchEndpoint,
    override val preloadItem: MediaMetadata? = null,
) : Queue {
    private var continuation: String? = null

    override suspend fun getInitialStatus(): Queue.Status {
        val nextResult = withContext(IO) {
            YouTube.next(endpoint, continuation).getOrThrow()
        }
        endpoint = nextResult.endpoint
        continuation = nextResult.continuation
        return Queue.Status(
            title = nextResult.title,
            items = nextResult.items.map { it.toMediaItem() },
            mediaItemIndex = nextResult.currentIndex ?: 0
        )
    }

    override fun hasNextPage(): Boolean = continuation != null

    override suspend fun nextPage(): List<MediaItem> {
        val nextResult = withContext(IO) {
            YouTube.next(endpoint, continuation).getOrThrow()
        }
        endpoint = nextResult.endpoint
        continuation = nextResult.continuation
        return nextResult.items.map { it.toMediaItem() }
    }

    companion object {
        /**
         * Starts radio from the exact watch endpoint supplied by YouTube Music when
         * one is available. Artist "Top songs" items frequently carry params /
         * music-video configuration in that endpoint; rebuilding it from only the
         * video id can leave the queue stuck on the loading state.
         */
        fun radio(
            song: MediaMetadata,
            endpoint: WatchEndpoint? = null,
        ) = YouTubeQueue(
            endpoint = endpoint ?: WatchEndpoint(videoId = song.id),
            preloadItem = song,
        )
    }
}
