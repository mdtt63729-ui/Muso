package com.muso.music.ui.player

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.zionhuang.innertube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Spotify/SimpMusic-style full-screen video surface for the player: a secondary, muted
 * ExoPlayer instance that plays the song's video stream, kept in sync with the main audio
 * player. The audio pipeline (cache, normalization, queue) is completely untouched — this is
 * display-only.
 *
 * Renders nothing while the video is unavailable; [onVideoAvailable] reports whether a
 * playable stream was resolved, so the caller can fall back to the regular artwork layout.
 */
@Composable
fun PlayerVideo(
    videoId: String?,
    isPlaying: Boolean,
    positionProvider: () -> Long,
    modifier: Modifier = Modifier,
    videoQualityHeight: Int = 720,
    onVideoAvailable: (Boolean) -> Unit = {},
) {
    if (videoId == null) {
        onVideoAvailable(false)
        return
    }

    var videoUrl by remember(videoId) { mutableStateOf<String?>(null) }
    var failed by remember(videoId) { mutableStateOf(false) }

    val context = LocalContext.current
    val videoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            volume = 0f
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoPlayer.release()
        }
    }

    // Resolve the video stream closest to the requested quality (e.g. 720p) — video-only
    // streams preferred, muxed as fallback. This layer is decorative: the audio comes from
    // the main player.
    LaunchedEffect(videoId, videoQualityHeight) {
        withContext(Dispatchers.IO) {
            runCatching {
                val response = YouTube.player(videoId).getOrThrow()
                val streamingData = response.streamingData
                val videoOnly = streamingData?.adaptiveFormats.orEmpty()
                    .filter { !it.isAudio && !it.url.isNullOrEmpty() && (it.height ?: 0) > 0 }
                    .minByOrNull { abs((it.height ?: 0) - videoQualityHeight) }
                val muxed = streamingData?.formats.orEmpty()
                    .filter { !it.url.isNullOrEmpty() && (it.height ?: 0) > 0 }
                    .minByOrNull { abs((it.height ?: 0) - videoQualityHeight) }
                videoOnly ?: muxed
            }.onSuccess { format ->
                if (format?.url != null) {
                    videoUrl = format.url
                } else {
                    failed = true
                }
            }.onFailure {
                failed = true
            }
        }
    }

    LaunchedEffect(videoUrl, failed) {
        onVideoAvailable(videoUrl != null && !failed)
    }

    LaunchedEffect(videoUrl) {
        val url = videoUrl ?: return@LaunchedEffect
        videoPlayer.setMediaItem(MediaItem.fromUri(url))
        videoPlayer.prepare()
        videoPlayer.seekTo(positionProvider())
        videoPlayer.playWhenReady = isPlaying
    }

    // Follow the main player's play/pause state.
    LaunchedEffect(isPlaying) {
        videoPlayer.playWhenReady = isPlaying
    }

    // Periodically re-sync with the main player: after a seek or a stall the two clocks drift,
    // and a video that is visibly ahead/behind the audio is worse than no video at all.
    LaunchedEffect(videoUrl, isPlaying) {
        while (isActive) {
            delay(1000)
            if (videoPlayer.playbackState == Player.STATE_READY && isPlaying) {
                val mainPosition = positionProvider()
                if (abs(videoPlayer.currentPosition - mainPosition) > 1000) {
                    videoPlayer.seekTo(mainPosition)
                }
            }
        }
    }

    if (!failed && videoUrl != null) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { it.player = videoPlayer },
            modifier = modifier
        )
    }
}
