package com.maxrave.simpmusic.ui.component

/*
 * Echo player slider styles - a faithful port of the files the user provided
 * (Echo-Word-By-Word-Lyrics-System/4-player-slider-styles):
 *
 *   - WavySlider.kt    -> EchoWavySlider: Material3 Expressive's
 *                         LinearWavyProgressIndicator with an animated
 *                         amplitude (calms to flat when paused), the thumb
 *                         gap, and a thumb circle riding the progress.
 *   - SquigglySlider.kt -> EchoSquigglySlider: the travelling-phase
 *                         squiggle (wavePhase advances every frame while
 *                         playing, flattens on pause or drag), active
 *                         segment in the accent, inactive dimmed, vertical
 *                         progress bar riding the wave front.
 *   - SlimPlayerSlider: kept from before (no slim file was provided).
 *
 * PlayerSliderByStyle keeps the same contract the players already use:
 * position 0..1, onSeek(0..1), onSeekFinished().
 */

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.ProgressIndicatorDefaults
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.muso.music.constants.SliderStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Slider colors per the Echo PlayerSliderColors for rich player backgrounds. */
@Composable
private fun echoSliderColors(accent: Color): SliderColors = SliderDefaults.colors(
    activeTrackColor = accent,
    activeTickColor = accent,
    thumbColor = accent,
    inactiveTrackColor = Color.White.copy(alpha = 0.4f),
)

/**
 * Renders the chosen non-default slider (PlayerSliderByStyle contract:
 * [position] 0..1, [onSeek] 0..1 while dragging, [onSeekFinished] on release).
 */
@Composable
fun PlayerSliderByStyle(
    style: SliderStyle,
    position: Float,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
) {
    when (style) {
        SliderStyle.SLIM -> SlimPlayerSlider(position, onSeek, onSeekFinished, accent, modifier)
        SliderStyle.SQUIGGLY -> EchoSquigglySlider(
            value = position,
            onValueChange = onSeek,
            onValueChangeFinished = onSeekFinished,
            colors = echoSliderColors(accent),
            isPlaying = isPlaying,
            modifier = modifier,
        )
        else -> EchoWavySlider(
            value = position,
            onValueChange = onSeek,
            onValueChangeFinished = onSeekFinished,
            colors = echoSliderColors(accent),
            isPlaying = isPlaying,
            modifier = modifier,
        )
    }
}

/* ------------------------------ WavySlider.kt ------------------------------ */

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EchoWavySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    isPlaying: Boolean = true,
    enabled: Boolean = true,
    strokeWidth: Dp = 4.dp,
    thumbRadius: Dp = 8.dp,
    wavelength: Dp = WavyProgressIndicatorDefaults.LinearDeterminateWavelength,
    waveSpeed: Dp = wavelength,
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { strokeWidth.toPx() }
    val thumbRadiusPx = with(density) { thumbRadius.toPx() }
    val stroke = remember(strokeWidthPx) { Stroke(width = strokeWidthPx, cap = StrokeCap.Round) }

    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(value) }

    val displayValue = if (isDragging) dragValue else value

    // The wave's amplitude eases in while playing and calms flat when paused.
    val animatedAmplitude by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "amplitude",
    )

    val activeColor = colors.activeTrackColor
    val inactiveColor = colors.inactiveTrackColor
    val thumbColor = colors.thumbColor

    val containerHeight = maxOf(WavyProgressIndicatorDefaults.LinearContainerHeight, thumbRadius * 2)

    val baseModifier = modifier.fillMaxWidth().height(containerHeight)

    val interactiveModifier =
        if (enabled) {
            baseModifier
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val newValue = (offset.x / size.width).coerceIn(0f, 1f)
                        onValueChange(newValue)
                        onValueChangeFinished?.invoke()
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragValue = (offset.x / size.width).coerceIn(0f, 1f)
                            onValueChange(dragValue)
                        },
                        onDragEnd = {
                            isDragging = false
                            onValueChangeFinished?.invoke()
                        },
                        onDragCancel = { isDragging = false },
                        onHorizontalDrag = { _, dragAmount ->
                            dragValue = (dragValue + dragAmount / size.width).coerceIn(0f, 1f)
                            onValueChange(dragValue)
                        },
                    )
                }
        } else {
            baseModifier
        }

    Box(modifier = interactiveModifier, contentAlignment = Alignment.Center) {
        LinearWavyProgressIndicator(
            progress = { displayValue },
            modifier = Modifier.fillMaxWidth(),
            color = activeColor,
            trackColor = inactiveColor,
            stroke = stroke,
            trackStroke = stroke,
            gapSize = thumbRadius + 4.dp,
            stopSize = WavyProgressIndicatorDefaults.LinearTrackStopIndicatorSize,
            amplitude = { progress -> if (progress > 0f) animatedAmplitude else 0f },
            wavelength = wavelength,
            waveSpeed = waveSpeed,
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val thumbX = size.width * displayValue
            val thumbY = size.height / 2
            drawCircle(color = thumbColor, radius = thumbRadiusPx, center = Offset(thumbX, thumbY))
        }
    }
}

/* ---------------------------- SquigglySlider.kt ---------------------------- */

@Composable
fun EchoSquigglySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    isPlaying: Boolean = true,
) {
    val primaryColor = colors.activeTrackColor
    val inactiveTrackColor = colors.inactiveTrackColor

    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(value) }

    val currentValue = if (isDragging) dragPosition else value
    val progress = currentValue.coerceIn(0f, 1f)

    var phaseOffset by remember { mutableFloatStateOf(0f) }
    var heightFraction by remember { mutableFloatStateOf(if (isPlaying) 1f else 0f) }

    val scope = rememberCoroutineScope()

    val waveLength = 80f
    val lineAmplitude = 6f
    val phaseSpeed = 24f
    val transitionPeriods = 1.5f
    val minWaveEndpoint = 0f
    val matchedWaveEndpoint = 1f
    val transitionEnabled = true

    LaunchedEffect(isPlaying, isDragging) {
        scope.launch {
            val shouldFlatten = !isPlaying || isDragging
            val targetHeight = if (shouldFlatten) 0f else 1f
            val animDuration = if (shouldFlatten) 150 else 200
            val startDelay = if (shouldFlatten) 0L else 30L

            delay(startDelay)

            val animator = Animatable(heightFraction)
            animator.animateTo(
                targetValue = targetHeight,
                animationSpec = tween(durationMillis = animDuration, easing = LinearEasing),
            ) {
                heightFraction = this.value
            }
        }
    }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect

        var lastFrameTime = withFrameMillis { it }
        while (isActive) {
            withFrameMillis { frameTimeMillis ->
                val deltaTime = (frameTimeMillis - lastFrameTime) / 1000f
                phaseOffset += deltaTime * phaseSpeed
                phaseOffset %= waveLength
                lastFrameTime = frameTimeMillis
            }
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(48.dp)
                .then(
                    if (enabled) {
                        Modifier.pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val newPosition = (offset.x / size.width).coerceIn(0f, 1f)
                                    onValueChange(newPosition)
                                    onValueChangeFinished?.invoke()
                                }
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        isDragging = true
                                        dragPosition = (offset.x / size.width).coerceIn(0f, 1f)
                                        onValueChange(dragPosition)
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                        onValueChangeFinished?.invoke()
                                    },
                                    onDragCancel = { isDragging = false },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        dragPosition = (change.position.x / size.width).coerceIn(0f, 1f)
                                        onValueChange(dragPosition)
                                    },
                                )
                            }
                    } else {
                        Modifier
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(48.dp)) {
            val strokeWidth = 5.dp.toPx()
            val totalWidth = size.width
            val totalProgressPx = totalWidth * progress
            val centerY = size.height / 2f

            val waveProgressPx =
                if (!transitionEnabled || progress > matchedWaveEndpoint) {
                    totalWidth * progress
                } else {
                    val t = (progress / matchedWaveEndpoint).coerceIn(0f, 1f)
                    totalWidth * (minWaveEndpoint + (matchedWaveEndpoint - minWaveEndpoint) * t)
                }

            fun computeAmplitude(x: Float, sign: Float): Float {
                return if (transitionEnabled) {
                    val length = transitionPeriods * waveLength
                    val coeff = ((waveProgressPx + length / 2f - x) / length).coerceIn(0f, 1f)
                    sign * heightFraction * lineAmplitude * coeff
                } else {
                    sign * heightFraction * lineAmplitude
                }
            }

            val path = Path()
            val waveStart = -phaseOffset - waveLength / 2f
            val waveEnd = if (transitionEnabled) totalWidth else waveProgressPx

            path.moveTo(waveStart, centerY)

            var currentX = waveStart
            var waveSign = 1f
            var currentAmp = computeAmplitude(currentX, waveSign)
            val dist = waveLength / 2f

            while (currentX < waveEnd) {
                waveSign = -waveSign
                val nextX = currentX + dist
                val midX = currentX + dist / 2f
                val nextAmp = computeAmplitude(nextX, waveSign)

                path.cubicTo(
                    midX,
                    centerY + currentAmp,
                    midX,
                    centerY + nextAmp,
                    nextX,
                    centerY + nextAmp,
                )

                currentAmp = nextAmp
                currentX = nextX
            }

            val clipTop = lineAmplitude + strokeWidth

            val disabledAlpha = 77f / 255f
            val dimmedColor = primaryColor.copy(alpha = disabledAlpha)
            val capRadius = strokeWidth / 2f

            fun drawPathSegment(startX: Float, endX: Float, color: Color) {
                if (endX <= startX) return
                clipRect(
                    left = startX,
                    top = centerY - clipTop,
                    right = endX,
                    bottom = centerY + clipTop,
                ) {
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
            }

            drawPathSegment(0f, totalProgressPx, primaryColor)
            drawPathSegment(totalProgressPx, totalWidth, dimmedColor)

            fun getWaveY(x: Float): Float {
                val phase = (x - waveStart) / waveLength
                val waveCycle = phase - kotlin.math.floor(phase)
                val waveValue = kotlin.math.cos(waveCycle * 2f * kotlin.math.PI.toFloat())

                val ampCoeff =
                    if (transitionEnabled) {
                        val length = transitionPeriods * waveLength
                        ((waveProgressPx + length / 2f - x) / length).coerceIn(0f, 1f)
                    } else {
                        1f
                    }

                return centerY + waveValue * lineAmplitude * heightFraction * ampCoeff
            }

            drawCircle(
                color = primaryColor,
                radius = capRadius,
                center = Offset(0f, getWaveY(0f)),
            )

            val endWaveY = getWaveY(totalWidth)
            clipRect(
                left = totalWidth,
                top = centerY - clipTop,
                right = totalWidth + capRadius,
                bottom = centerY + clipTop,
            ) {
                drawCircle(
                    color = inactiveTrackColor,
                    radius = capRadius,
                    center = Offset(totalWidth, endWaveY),
                )
            }

            val barHalfHeight = (lineAmplitude + strokeWidth)
            val barWidth = 5.dp.toPx()

            if (barHalfHeight > 0.5f) {
                drawLine(
                    color = primaryColor,
                    start = Offset(totalProgressPx, centerY - barHalfHeight),
                    end = Offset(totalProgressPx, centerY + barHalfHeight),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

/* ------------------------------ Slim (kept) ------------------------------ */

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
