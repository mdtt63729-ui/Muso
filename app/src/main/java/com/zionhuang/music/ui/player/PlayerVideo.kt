package com.zionhuang.music.ui.player

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
 * Spotify/SimpMusic-style video surface for the player: a secondary, muted ExoPlayer instance
 * that plays the song's video stream, kept in sync with the main audio player. The audio
 * pipeline (cache, normalization, queue) is completely untouched — this is display-only.
 *
 * When the video is unavailable (no video formats, fetch error) the [fallback] composable is
 * shown instead, so the player degrades to the regular artwork thumbnail.
 */
@Composable
fun PlayerVideo(
    videoId: String?,
    isPlaying: Boolean,
    positionProvider: () -> Long,
    modifier: Modifier = Modifier,
    fallback: @Composable () -> Unit,
) {
    if (videoId == null) {
        fallback()
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

    // Resolve the lowest-resolution video stream for the current song (bandwidth-friendly —
    // this layer is decorative, the audio comes from the main player).
    LaunchedEffect(videoId) {
        withContext(Dispatchers.IO) {
            runCatching {
                val response = YouTube.player(videoId).getOrThrow()
                val streamingData = response.streamingData
                val videoOnly = streamingData?.adaptiveFormats.orEmpty()
                    .filter { !it.isAudio && !it.url.isNullOrEmpty() }
                    .minByOrNull { it.height ?: Int.MAX_VALUE }
                val muxed = streamingData?.formats.orEmpty()
                    .filter { !it.url.isNullOrEmpty() && (it.height ?: 0) > 0 }
                    .minByOrNull { it.height ?: Int.MAX_VALUE }
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
            delay(2000)
            if (videoPlayer.playbackState == Player.STATE_READY) {
                val mainPosition = positionProvider()
                if (abs(videoPlayer.currentPosition - mainPosition) > 2000) {
                    videoPlayer.seekTo(mainPosition)
                }
            }
        }
    }

    if (failed) {
        fallback()
    } else {
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
