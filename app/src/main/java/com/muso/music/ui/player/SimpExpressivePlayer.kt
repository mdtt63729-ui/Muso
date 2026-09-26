package com.muso.music.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.muso.music.R
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import com.muso.music.models.MediaMetadata
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.muso.music.LocalDatabase
import com.muso.music.db.entities.FormatEntity
import com.muso.music.utils.makeTimeString
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// SimpMusic ExpressiveTransportRow weights, verbatim.
private const val SIDE_WEIGHT = 0.55f
private const val PLAY_WEIGHT = 1.2f
private const val PRESS_GROWTH = 1.15f

/**
 * SimpMusic's Material 3 Expressive Now Playing, ported from
 * NowPlayingContentM3Expressive.kt (ExpressiveTrackInfoRow /
 * ExpressivePlaybackControls / ExpressiveTransportRow /
 * ExpressiveConnectedGroup). Muso's material3 (1.3.0-rc01) has no
 * LinearWavyProgressIndicator, motionScheme or MaterialExpressiveTheme, so
 * the wavy progress is a custom Canvas sine and the motion springs are plain
 * spring specs tuned to the same feel; every other value (weights, paddings,
 * 68dp transport, corner morph 22<->34dp, 48dp connected group with 24/6dp
 * caps) matches the original.
 */
@Composable
fun ColumnScope.SimpExpressiveContent(
    mediaMetadata: MediaMetadata,
    navController: NavController,
    repeatMode: Int,
    shuffleModeEnabled: Boolean,
    isLiked: Boolean,
    isPlaying: Boolean,
    buffering: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    duration: Long,
    /** Deferred position read: invoked only inside draw/derived scopes, so the
     * 100 ms position ticks never recompose this whole screen. */
    progressFractionProvider: () -> Float,
    onScrub: (Float) -> Unit,
    onScrubEnd: (Float) -> Unit,
    onToggleLike: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShowLyrics: () -> Unit,
    lyricsActive: Boolean,
    onShowInfo: () -> Unit,
    onAddToPlaylist: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    // === ExpressiveTrackInfoRow: title + artist left, 48dp tonal heart right ===
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mediaMetadata.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee()
                    .clickable(enabled = mediaMetadata.album != null) {
                        navController.navigate("album/${mediaMetadata.album!!.id}")
                    },
            )
            Spacer(Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = mediaMetadata.artists.joinToString { it.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .basicMarquee()
                        .clickable(enabled = mediaMetadata.artists.firstOrNull()?.id != null) {
                            navController.navigate("artist/${mediaMetadata.artists.first().id}")
                        },
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        FilledIconToggleButton(
            checked = isLiked,
            onCheckedChange = { onToggleLike() },
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconToggleButtonColors(
                containerColor = colorScheme.surfaceContainerHigh,
                contentColor = colorScheme.onSurfaceVariant,
                checkedContainerColor = colorScheme.primaryContainer,
                checkedContentColor = colorScheme.onPrimaryContainer,
            ),
            modifier = Modifier.size(48.dp),
        ) {
            Crossfade(targetState = isLiked, label = "expressiveLike") { liked ->
                Icon(
                    painter = painterResource(if (liked) R.drawable.favorite else R.drawable.favorite_border),
                    contentDescription = null,
                )
            }
        }
    }

    // === ExpressivePlaybackControls: wavy seek + time row + transport ===
    Box(
        Modifier
            .padding(top = 15.dp)
            .padding(horizontal = 20.dp),
    ) {
        WavySeekBar(
            progressFractionProvider = progressFractionProvider,
            isPlaying = isPlaying,
            activeColor = colorScheme.primary,
            trackColor = colorScheme.secondaryContainer,
            thumbColor = colorScheme.primary,
            onScrub = onScrub,
            onScrubEnd = onScrubEnd,
        )
    }
    Row(
        Modifier
            .fillMaxWidth()
            .offset(y = (-8).dp)
            .padding(horizontal = 20.dp),
    ) {
        ElapsedTimeText(
            duration = duration,
            progressFractionProvider = progressFractionProvider,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (duration < 0) "" else makeTimeString(duration),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Right,
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(Modifier.height(8.dp))
    ExpressiveTransportRow(
        isPlaying = isPlaying,
        buffering = buffering,
        canSkipPrevious = canSkipPrevious,
        canSkipNext = canSkipNext,
        onPlayPause = onPlayPause,
        onPrevious = onPrevious,
        onNext = onNext,
        modifier = Modifier.padding(horizontal = 20.dp),
    )

    // === ExpressiveConnectedGroup - SimpMusic's Info | Cast | Shuffle | Repeat |
    // Add-to-playlist. Muso has no Cast support, so Lyrics takes that
    // slot: Details | Lyrics | Shuffle | Repeat | Add. 48dp row, 3dp
    // gaps, 24/6dp end caps, active slots on primaryContainer.
    val startCap = RoundedCornerShape(topStart = 24.dp, topEnd = 6.dp, bottomEnd = 6.dp, bottomStart = 24.dp)
    val endCap = RoundedCornerShape(topStart = 6.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 6.dp)
    val middle = RoundedCornerShape(6.dp)
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(48.dp),
    ) {
        ExpressiveConnectedSlot(shape = startCap, active = false, onClick = onShowInfo) {
            Icon(
                painter = painterResource(R.drawable.info),
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
        ExpressiveConnectedSlot(shape = middle, active = lyricsActive, onClick = onShowLyrics) {
            Icon(
                painter = painterResource(R.drawable.lyrics),
                contentDescription = null,
                tint = if (lyricsActive) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
        ExpressiveConnectedSlot(shape = middle, active = shuffleModeEnabled, onClick = onShuffle) {
            Crossfade(targetState = shuffleModeEnabled, label = "expressiveShuffle") { sh ->
                Icon(
                    painter = painterResource(R.drawable.shuffle),
                    contentDescription = null,
                    tint = if (sh) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        ExpressiveConnectedSlot(shape = middle, active = repeatMode != REPEAT_MODE_OFF, onClick = onRepeat) {
            Crossfade(targetState = repeatMode, label = "expressiveRepeat") { rm ->
                Icon(
                    painter = painterResource(if (rm == REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat),
                    contentDescription = null,
                    tint = if (rm != REPEAT_MODE_OFF) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        ExpressiveConnectedSlot(shape = endCap, active = false, onClick = onAddToPlaylist) {
            Icon(
                painter = painterResource(R.drawable.playlist_add),
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** 48dp connected-group slot, SimpMusic verbatim: fillHeight, tonal surface. */
@Composable
private fun RowScope.ExpressiveConnectedSlot(
    shape: RoundedCornerShape,
    active: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (active) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh,
        contentColor = if (active) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

/** Three pill buttons in a 68dp row; pressed one grows x1.15, play corner morphs 22<->34dp. */
@Composable
private fun ExpressiveTransportRow(
    isPlaying: Boolean,
    buffering: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme

    val prevInteraction = remember { MutableInteractionSource() }
    val playInteraction = remember { MutableInteractionSource() }
    val nextInteraction = remember { MutableInteractionSource() }
    val prevPressed by prevInteraction.collectIsPressedAsState()
    val playPressed by playInteraction.collectIsPressedAsState()
    val nextPressed by nextInteraction.collectIsPressedAsState()

    val spatial = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
    val prevWeight by animateFloatAsState(
        targetValue = if (prevPressed) SIDE_WEIGHT * PRESS_GROWTH else SIDE_WEIGHT,
        animationSpec = spatial,
        label = "prevWeight",
    )
    val playWeight by animateFloatAsState(
        targetValue = if (playPressed) PLAY_WEIGHT * PRESS_GROWTH else PLAY_WEIGHT,
        animationSpec = spatial,
        label = "playWeight",
    )
    val nextWeight by animateFloatAsState(
        targetValue = if (nextPressed) SIDE_WEIGHT * PRESS_GROWTH else SIDE_WEIGHT,
        animationSpec = spatial,
        label = "nextWeight",
    )
    val playCorner by animateDpAsState(
        targetValue = if (isPlaying) 22.dp else 34.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "playCorner",
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp),
    ) {
        Surface(
            onClick = { if (canSkipPrevious) onPrevious() },
            shape = RoundedCornerShape(34.dp),
            color = colorScheme.secondaryContainer,
            interactionSource = prevInteraction,
            modifier = Modifier
                .weight(prevWeight)
                .fillMaxHeight(),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    painter = painterResource(R.drawable.skip_previous),
                    contentDescription = null,
                    tint = colorScheme.onSecondaryContainer.copy(alpha = if (canSkipPrevious) 1f else 0.4f),
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Surface(
            onClick = { if (!buffering) onPlayPause() },
            shape = RoundedCornerShape(playCorner),
            color = colorScheme.primary,
            interactionSource = playInteraction,
            modifier = Modifier
                .weight(playWeight)
                .fillMaxHeight(),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Crossfade(targetState = buffering, label = "playLoading") { loading ->
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = colorScheme.onPrimary,
                            strokeWidth = 3.dp,
                        )
                    } else {
                        Crossfade(targetState = isPlaying, label = "playPauseIcon") { playing ->
                            Icon(
                                painter = painterResource(if (playing) R.drawable.pause else R.drawable.play),
                                contentDescription = null,
                                tint = colorScheme.onPrimary,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }
                }
            }
        }
        Surface(
            onClick = { if (canSkipNext) onNext() },
            shape = RoundedCornerShape(34.dp),
            color = colorScheme.secondaryContainer,
            interactionSource = nextInteraction,
            modifier = Modifier
                .weight(nextWeight)
                .fillMaxHeight(),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    painter = painterResource(R.drawable.skip_next),
                    contentDescription = null,
                    tint = colorScheme.onSecondaryContainer.copy(alpha = if (canSkipNext) 1f else 0.4f),
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

/**
 * Seekable wavy progress bar, Muso's Canvas substitute for material3's
 * LinearWavyProgressIndicator: sine on the active segment (amplitude up
 * while playing, flat when paused or scrubbing), flat track behind, and a
 * 14dp circle thumb morphing into a 6x22dp bar while dragging. Commits the
 * seek only when the interaction ends.
 */
@Composable
fun WavySeekBar(
    progressFractionProvider: () -> Float,
    isPlaying: Boolean,
    activeColor: Color,
    trackColor: Color,
    thumbColor: Color,
    onScrub: (Float) -> Unit,
    onScrubEnd: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isInteracting by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var widthPx by remember { mutableIntStateOf(0) }

    // The displayed fraction is derived INSIDE the Canvas draw block below, so
    // 100 ms position ticks invalidate the draw pass only - this whole composable
    // never recomposes while the song simply plays on.
    fun displayedFraction(): Float =
        (if (isInteracting) dragFraction else progressFractionProvider()).coerceIn(0f, 1f)
    val amplitude by animateFloatAsState(
        targetValue = if (isPlaying && !isInteracting) 1f else 0f,
        animationSpec = tween(600),
        label = "waveAmplitude",
    )
    val thumbMorph by animateFloatAsState(
        targetValue = if (isInteracting) 1f else 0f,
        animationSpec = tween(250),
        label = "waveThumbMorph",
    )

    fun fractionAt(x: Float): Float = if (widthPx <= 0) 0f else (x / widthPx).coerceIn(0f, 1f)

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .onSizeChanged { widthPx = it.width }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = fractionAt(offset.x)
                    dragFraction = fraction
                    onScrub(fraction)
                    onScrubEnd(fraction)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isInteracting = true
                        val fraction = fractionAt(offset.x)
                        dragFraction = fraction
                        onScrub(fraction)
                    },
                    onDragEnd = {
                        isInteracting = false
                        onScrubEnd(dragFraction)
                    },
                    onDragCancel = {
                        isInteracting = false
                        onScrubEnd(dragFraction)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val fraction = fractionAt(change.position.x)
                        dragFraction = fraction
                        onScrub(fraction)
                    },
                )
            },
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
            val displayed = displayedFraction()
            val thickness = 5.dp.toPx()
            val centerY = size.height / 2f
            // Flat track across the full width (the wave lives on the active segment only).
            drawRoundRect(
                color = trackColor,
                topLeft = androidx.compose.ui.geometry.Offset(0f, centerY - thickness / 2f),
                size = androidx.compose.ui.geometry.Size(size.width, thickness),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(thickness / 2f, thickness / 2f),
            )
            val waveAmp = (4.dp.toPx()) * amplitude
            val activeWidth = size.width * displayed
            if (activeWidth > 1f && waveAmp > 0.5f) {
                clipRect(right = activeWidth) {
                    val step = 3.dp.toPx()
                    val period = 56.dp.toPx()
                    val path = Path()
                    var x = 0f
                    path.moveTo(0f, centerY)
                    while (x <= size.width) {
                        val y = centerY + waveAmp * sin((x / period) * 2f * Math.PI.toFloat())
                        path.lineTo(x, y)
                        x += step
                    }
                    drawPath(path, color = activeColor, style = Stroke(width = thickness))
                }
            } else if (activeWidth > 1f) {
                drawRoundRect(
                    color = activeColor,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, centerY - thickness / 2f),
                    size = androidx.compose.ui.geometry.Size(activeWidth, thickness),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(thickness / 2f, thickness / 2f),
                )
            }
        }
        val thumbWidthDp = (14f + (6f - 14f) * thumbMorph).dp
        val thumbHeightDp = (14f + (22f - 14f) * thumbMorph).dp
        Box(
            modifier = Modifier
                .offset {
                    val thumbWidthPx = thumbWidthDp.toPx()
                    IntOffset(
                        x = ((widthPx - thumbWidthPx) * displayedFraction()).roundToInt(),
                        y = 0,
                    )
                }
                .size(width = thumbWidthDp, height = thumbHeightDp)
                .clip(RoundedCornerShape(percent = 50))
                .background(thumbColor),
        )
    }
}


/**
 * The Details slot's dialog, SimpMusic's Info sheet trimmed to Muso's data:
 * song metadata plus the live stream format from the database (mime, codec,
 * bitrate, sample rate), read through the same `database.format(id)` flow the
 * settings screens use.
 */
@Composable
fun SongInfoDialog(
    mediaMetadata: MediaMetadata,
    onDismiss: () -> Unit,
) {
    val database = LocalDatabase.current
    var format by remember { mutableStateOf<FormatEntity?>(null) }
    LaunchedEffect(mediaMetadata.id) {
        database.format(mediaMetadata.id).collect { format = it }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.details)) },
        text = {
            Column {
                SongInfoRow("Title", mediaMetadata.title)
                SongInfoRow("Artist", mediaMetadata.artists.joinToString { it.name })
                SongInfoRow("Album", mediaMetadata.album?.title)
                SongInfoRow("Duration", makeTimeString(mediaMetadata.duration.toLong()))
                SongInfoRow("Codec", format?.codecs)
                SongInfoRow("MIME", format?.mimeType)
                SongInfoRow("Bitrate", format?.bitrate?.let { "${it / 1000} kbps" })
                SongInfoRow("Sample rate", format?.sampleRate?.let { "$it Hz" })
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

/** One label/value line of the Details dialog. */
@Composable
private fun SongInfoRow(label: String, value: String?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp),
        )
        Text(
            text = value ?: "-",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Elapsed-time text that reads the position through [progressFractionProvider] inside a
 * derivedStateOf, so it only recomposes when the displayed string changes (~1 Hz),
 * not on every 100 ms position tick.
 */
@Composable
private fun ElapsedTimeText(
    duration: Long,
    progressFractionProvider: () -> Float,
    modifier: Modifier = Modifier,
) {
    val text by remember(duration) {
        derivedStateOf { makeTimeString((duration * progressFractionProvider()).toLong()) }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier,
    )
}
