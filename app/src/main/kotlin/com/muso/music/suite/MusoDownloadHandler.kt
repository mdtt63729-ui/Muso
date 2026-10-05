package com.muso.music.suite

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.DownloadRequest
import com.maxrave.domain.mediaservice.handler.DownloadHandler
import com.muso.music.playback.DownloadUtil
import com.muso.music.playback.ExoDownloadService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Muso adapter of SimpMusic's DownloadHandler: downloads are queued through
 * Muso's ExoDownloadService exactly like the song menu does, and progress is
 * surfaced from Muso's DownloadUtil map (audio slot only; Muso has no separate
 * video download).
 */
class MusoDownloadHandler(
    private val context: Context,
    private val downloadUtil: DownloadUtil,
) : DownloadHandler {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val downloads: StateFlow<Map<String, Pair<DownloadHandler.Download?, DownloadHandler.Download?>>> =
        downloadUtil.downloads
            .let { flow ->
                kotlinx.coroutines.flow.flow {
                    flow.collect { map ->
                        emit(map.mapValues { (_, download) -> DownloadHandler.Download(state = download.state) to null })
                    }
                }
            }
            .stateIn(
                scope = scope,
                started = kotlinx.coroutines.flow.SharingStarted.Eagerly,
                initialValue = emptyMap<String, Pair<DownloadHandler.Download?, DownloadHandler.Download?>>(),
            )

    override val downloadTask: StateFlow<Map<String, Int>> = MutableStateFlow(emptyMap())

    override suspend fun downloadTrack(videoId: String, title: String, thumbnail: String) {
        // Start artwork caching from the exact thumbnail that came with the download request.
        // Do not depend solely on the main Room song table: this handler also receives catalog
        // songs from the embedded SimpMusic stack, which may not have been persisted there yet.
        // The artwork job is best-effort and deliberately does not block the audio download.
        scope.launch(Dispatchers.IO) {
            downloadUtil.cacheArtworkForDownload(videoId, thumbnail)
        }

        val request = DownloadRequest.Builder(videoId, videoId.toUri())
            .setCustomCacheKey(videoId)
            .setData(title.toByteArray())
            .build()
        androidx.media3.exoplayer.offline.DownloadService.sendAddDownload(
            context,
            ExoDownloadService::class.java,
            request,
            false,
        )
        // If this song's video has already rendered, start its offline video
        // download alongside the audio rather than waiting for the audio to finish.
        downloadUtil.downloadRenderedCanvasVideo(videoId)
    }

    override fun removeDownload(videoId: String) {
        androidx.media3.exoplayer.offline.DownloadService.sendRemoveDownload(
            context,
            ExoDownloadService::class.java,
            videoId,
            false,
        )
    }

    override fun removeAllDownloads() {
        scope.launch {
            downloadUtil.downloads.value.keys.forEach { removeDownload(it) }
        }
    }
}
