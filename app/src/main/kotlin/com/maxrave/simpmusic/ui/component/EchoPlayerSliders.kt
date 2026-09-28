package com.maxrave.simpmusic.ui.component

/*
 * Echo player slider styles (Echo-Music PRD, folder 4-player-slider-styles)
 * for the Classic (Spotify) player: WAVY (a sine wave whose played side
 * carries more energy), SLIM (a hairline track with a small thumb) and
 * SQUIGGLY (the squigglyslider library the settings preview already uses).
 * DEFAULT stays the Classic player's own Material slider and never reaches
 * this file.
 */

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.muso.music.constants.SliderStyle

/**
 * Renders the chosen non-default slider. [position] is a 0..1 fraction;
 * [onSeek] receives a 0..1 fraction while dragging, [onSeekFinished] fires
 * when the gesture ends (same contract as the Classic player's Slider).
 */
@Composable
fun PlayerSliderByStyle(
    style: SliderStyle,
    position: Float,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    when (style) {
        SliderStyle.SLIM -> SlimPlayerSlider(position, onSeek, onSeekFinished, accent, modifier)
        // SQUIGGLY is the high-frequency double-harmonic squiggle; WAVY the
        // calm nine-wave. (DEFAULT never reaches this file.)
        else -> WavyPlayerSlider(
            position = position,
            onSeek = onSeek,
            onSeekFinished = onSeekFinished,
            accent = accent,
            squiggle = style == SliderStyle.SQUIGGLY,
            modifier = modifier,
        )
    }
}

@Composable
private fun WavyPlayerSlider(
    position: Float,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    accent: Color,
    squiggle: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var dragFraction by remember { mutableFloatStateOf(Float.NaN) }
    val shown = if (dragFraction.isNaN()) position else dragFraction

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onSeek((offset.x / size.width).coerceIn(0f, 1f))
                    onSeekFinished()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        onSeek(dragFraction)
                    },
                    onDragEnd = {
                        onSeekFinished()
                        dragFraction = Float.NaN
                    },
                    onDragCancel = {
                        dragFraction = Float.NaN
                    },
                )
            },
    ) {
        val w = size.width
        val h = size.height
        val mid = h / 2f
        // The played side carries more amplitude (energy) than the part still
        // ahead, exactly the Echo WAVY behaviour.
        val activeAmp = h * 0.30f
        val idleAmp = h * 0.12f
        val waveLen = w / (if (squiggle) 16f else 9f)
        val harmonic = if (squiggle) 0.4f else 0f
        val cutX = w * shown

        fun drawWave(fromX: Float, toX: Float, amp: Float, color: Color) {
            if (toX <= fromX) return
            val path = Path()
            var x = fromX
            var first = true
            while (x <= toX) {
                val phase = (x / waveLen) * (2f * Math.PI.toFloat())
                val y = mid + amp * (kotlin.math.sin(phase) + harmonic * kotlin.math.sin(phase * 2f + 1.2f))
                if (first) {
                    path.moveTo(x, y)
                    first = false
                } else {
                    path.lineTo(x, y)
                }
                x += 2f
            }
            drawPath(path, color = color, style = Stroke(width = 5f, cap = StrokeCap.Round))
        }

        drawWave(0f, cutX, activeAmp, accent)
        drawWave(cutX, w, idleAmp, accent.copy(alpha = 0.45f))
        // Playhead: a small dot riding the cut point.
        drawCircle(color = accent, radius = 7f, center = Offset(cutX, mid))
    }
}

@Composable
private fun SlimPlayerSlider(
    position: Float,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    var dragFraction by remember { mutableFloatStateOf(Float.NaN) }
    val shown = if (dragFraction.isNaN()) position else dragFraction

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onSeek((offset.x / size.width).coerceIn(0f, 1f))
                    onSeekFinished()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        onSeek(dragFraction)
                    },
                    onDragEnd = {
                        onSeekFinished()
                        dragFraction = Float.NaN
                    },
                    onDragCancel = {
                        dragFraction = Float.NaN
                    },
                )
            },
    ) {
        val w = size.width
        val mid = size.height / 2f
        val cutX = w * shown
        // Hairline track.
        drawLine(
            color = accent.copy(alpha = 0.30f),
            start = Offset(0f, mid),
            end = Offset(w, mid),
            strokeWidth = 3f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = accent,
            start = Offset(0f, mid),
            end = Offset(cutX, mid),
            strokeWidth = 3f,
            cap = StrokeCap.Round,
        )
        drawCircle(color = accent, radius = 6f, center = Offset(cutX, mid))
    }
}
