package com.muso.music.ui.player

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
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
