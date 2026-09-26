package com.muso.music.ui.player

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/**
 * The MAIN player's own video output as a surface - the SimpMusic single-stream
 * approach: whatever the main ExoPlayer plays (a muxed video+audio format while
 * "show video in player" is on) renders here, so the picture can never drift
 * from the audio, the position or any control.
 *
 * [crop] = ZOOM (fill the bounds, cropping) vs FIT (letterbox inside the bounds,
 * never a stretched or cropped picture - used by the fullscreen route).
 */
@Composable
fun MainPlayerVideo(
    player: Player,
    modifier: Modifier = Modifier,
    crop: Boolean = true,
) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                resizeMode = if (crop) {
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
        },
        update = { it.player = player },
        modifier = modifier,
    )
}

/**
 * Spotify-Canvas-style fullscreen video surface: a dedicated ExoPlayer that
 * plays the song's video MUTED, cropped edge-to-edge, looping a short 7-10s
 * highlight of the track's "main" section - completely independent of the main
 * player, which keeps playing the audio. When the video is longer than ~12s a
 * ClippingConfiguration swaps in the highlight (~25% in, 8s long); short
 * videos loop whole. [onReady] fires only once the loop is actually showable,
 * so the thumbnail can cover the slot until then and fade out afterwards.
 */
@Composable
fun CanvasVideoPlayer(
    videoUrl: String,
    playing: Boolean,
    onReady: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            volume = 0f
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
        }
    }
    val clipped = remember { mutableStateOf(false) }
    DisposableEffect(videoUrl) {
        clipped.value = false
        player.setMediaItem(MediaItem.fromUri(videoUrl))
        player.prepare()
        onDispose {
            player.stop()
            player.clearMediaItems()
        }
    }
    DisposableEffect(videoUrl) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState != Player.STATE_READY) return
                if (!clipped.value) {
                    val durationMs = player.duration
                    if (durationMs > 12_000L) {
                        clipped.value = true
                        val startMs = (durationMs * 0.25f).toLong().coerceAtMost(45_000L)
                        val endMs = (startMs + 8_000L).coerceAtMost(durationMs)
                        player.setMediaItem(
                            MediaItem.Builder()
                                .setUri(videoUrl)
                                .setClippingConfiguration(
                                    MediaItem.ClippingConfiguration.Builder()
                                        .setStartPositionMs(startMs)
                                        .setEndPositionMs(endMs)
                                        .build()
                                )
                                .build()
                        )
                        player.prepare()
                        return
                    }
                }
                onReady()
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    LaunchedEffect(playing) {
        player.playWhenReady = playing
    }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
        },
        update = { it.player = player },
        modifier = modifier.fillMaxSize(),
    )
    DisposableEffect(Unit) {
        onDispose { player.release() }
    }
}
