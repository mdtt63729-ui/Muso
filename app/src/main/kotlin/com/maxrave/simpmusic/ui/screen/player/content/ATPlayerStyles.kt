package com.maxrave.simpmusic.ui.screen.player.content

/*
 * Muso Round 174: the ArchiveTune player design styles.
 *
 * ArchiveTune ships ten player designs (V1 Classic .. V10 Editorial). Muso's
 * own three (Classic/Spotify, Expressive/M3, Immersive/AppleMusic) map to
 * V1/V6/V7; this file implements the other seven against the same
 * NowPlayingContentState / NowPlayingContentActions contract the existing
 * styles use, so they drop into the shell without any new wiring:
 *
 *   MODERN (V2)            - rounded card artwork, left-aligned type
 *   MINIMAL (V3)           - sparse: small art, hairline progress, 3 buttons
 *   CINEMATIC (V4)         - blurred full-bleed backdrop, letterboxed block
 *   LITTLE (V5)            - tiny art, pill progress, compact everything
 *   IMMERSIVE EXTENDED(V8) - dark canvas + extended chips + queue peek
 *   MATERIAL EXTENDED (V9) - M3 tonal buttons + segmented shuffle/repeat chips
 *   EDITORIAL (V10)        - giant display type, offset art, underline seek
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.ui.platform.LocalContext
import com.maxrave.simpmusic.extension.formatDuration
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.viewModel.UIEvent
import kotlin.math.roundToLong

// ---------------------------------------------------------------------------
// Shared helpers
// ---------------------------------------------------------------------------

@Composable
private fun atTrackArtworkUrl(page: Int, state: NowPlayingContentState): String? =
    state.artworkQueue.getOrNull(page)?.thumbnails?.lastOrNull()?.url
        ?: state.screenData.thumbnailURL

/** Artwork pager: swipe changes song, exactly like the existing styles. */
@Composable
private fun ATArtworkPager(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(0.dp),
    showCanvasVideo: Boolean = true,
) {
    val pageCount = state.artworkQueue.size.coerceAtLeast(1)
    LaunchedEffect(state.artworkPagerState.settledPage) {
        if (pageCount > 1) {
            val target = state.artworkPagerState.settledPage
            if (target != state.currentOrderIndex && state.artworkQueue.getOrNull(target) != null) {
                actions.onSeekToQueueIndex(target)
            }
        }
    }
    HorizontalPager(
        state = state.artworkPagerState,
        modifier = modifier,
        beyondViewportPageCount = 1,
    ) { page ->
        val isCurrent = page == state.artworkPagerState.currentPage
        val url = atTrackArtworkUrl(page, state)
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .graphicsLayer { alpha = if (isCurrent) 1f else 0.65f },
        ) {
            if (showCanvasVideo && isCurrent && state.canvasData?.isVideo == true) {
                com.maxrave.simpmusic.expect.ui.MediaPlayerView(
                    url = state.canvasData.url,
                    cropToBounds = true,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                AsyncImage(
                    model =
                        ImageRequest.Builder(LocalContext.current)
                            .data(url)
                            .diskCacheKey(url + "AT_STYLE")
                            .crossfade(true)
                            .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun ATTitle(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    centered: Boolean,
    big: Boolean = false,
) {
    Text(
        text = state.screenData.nowPlayingTitle,
        style = if (big) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        maxLines = if (big) 3 else 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(2.dp))
    Text(
        text = state.screenData.artistName,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ATSlider(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    accent: Color,
) {
    Slider(
        value = state.sliderValue,
        onValueChange = actions.onSliderChange,
        onValueChangeFinished = actions.onSliderChangeFinished,
        valueRange = 0f..100f,
        colors =
            SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
            ),
        modifier = Modifier.fillMaxWidth(),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = formatDuration((state.timelineState.total * (state.sliderValue / 100f)).roundToLong()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatDuration(state.timelineState.total),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ATTransportControls(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    playIcon: ImageVector,
    playFilled: Boolean,
    accent: Color,
    compact: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { actions.onUIEvent(UIEvent.Previous) }) {
            Icon(SimpIcons.SkipPrevious, null, tint = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.width(4.dp))
        if (playFilled) {
            FilledIconButton(
                onClick = { actions.onUIEvent(UIEvent.PlayPause) },
                modifier = Modifier.size(if (compact) 56.dp else 72.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = accent),
            ) {
                Icon(
                    playIcon,
                    null,
                    modifier = Modifier.size(if (compact) 30.dp else 38.dp),
                    tint = Color.Black,
                )
            }
        } else {
            IconButton(
                onClick = { actions.onUIEvent(UIEvent.PlayPause) },
                modifier = Modifier.size(if (compact) 56.dp else 72.dp),
            ) {
                Icon(
                    playIcon,
                    null,
                    modifier = Modifier.size(if (compact) 40.dp else 52.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = { actions.onUIEvent(UIEvent.Next) }) {
            Icon(SimpIcons.SkipNext, null, tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun ATSecondaryRow(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    accent: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { actions.onUIEvent(UIEvent.ToggleLike) }) {
                Icon(
                    if (state.likeStatus) SimpIcons.Favorite else SimpIcons.FavoriteBorder,
                    null,
                    tint = if (state.likeStatus) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = actions.onShowFullscreenLyrics) {
                Icon(SimpIcons.Lyrics, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (state.shouldShowVideo) {
                IconButton(onClick = actions.onEnterFullscreenVideo) {
                    Icon(SimpIcons.Fullscreen, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = actions.onShowQueue) {
                Icon(SimpIcons.QueueMusic, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = actions.onShowMoreSheet) {
                Icon(SimpIcons.MoreVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// V2 — MODERN
// ---------------------------------------------------------------------------

@Composable
fun NowPlayingContentModern(state: NowPlayingContentState, actions: NowPlayingContentActions) {
    val accent = state.startColor.value
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        state.startColor.value.copy(alpha = 0.35f),
                        PlayerBackdropColor,
                        PlayerBackdropColor,
                    ),
                ),
            ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .background(Color.Transparent),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.4f))
            ATArtworkPager(
                state = state,
                actions = actions,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                shape = RoundedCornerShape(28.dp),
            )
            Spacer(Modifier.weight(0.5f))
            ATTitle(state = state, actions = actions, centered = false)
            Spacer(Modifier.height(18.dp))
            ATSlider(state = state, actions = actions, accent = accent)
            Spacer(Modifier.height(10.dp))
            ATTransportControls(
                state = state,
                actions = actions,
                playIcon = if (state.controllerState.isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                playFilled = true,
                accent = accent,
            )
            Spacer(Modifier.height(6.dp))
            ATSecondaryRow(state = state, actions = actions, accent = accent)
            Spacer(Modifier.weight(0.35f))
        }
    }
}

// ---------------------------------------------------------------------------
// V3 — MINIMAL
// ---------------------------------------------------------------------------

@Composable
fun NowPlayingContentMinimal(state: NowPlayingContentState, actions: NowPlayingContentActions) {
    val accent = state.startColor.value
    Box(Modifier.fillMaxSize().background(PlayerBackdropColor)) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.35f))
            ATArtworkPager(
                state = state,
                actions = actions,
                modifier = Modifier.size(148.dp),
                shape = RoundedCornerShape(8.dp),
            )
            Spacer(Modifier.weight(0.4f))
            Text(
                text = state.screenData.nowPlayingTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = state.screenData.artistName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.weight(0.4f))
            // Hairline progress with no visible thumb — the "line" is the seek.
            Slider(
                value = state.sliderValue,
                onValueChange = actions.onSliderChange,
                onValueChangeFinished = actions.onSliderChangeFinished,
                valueRange = 0f..100f,
                colors =
                    SliderDefaults.colors(
                        thumbColor = accent,
                        activeTrackColor = accent,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                    ),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    formatDuration((state.timelineState.total * (state.sliderValue / 100f)).roundToLong()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    formatDuration(state.timelineState.total),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.weight(0.3f))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { actions.onUIEvent(UIEvent.Previous) }) {
                    Icon(SimpIcons.SkipPrevious, null, tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { actions.onUIEvent(UIEvent.PlayPause) }, modifier = Modifier.size(64.dp)) {
                    Icon(
                        if (state.controllerState.isPlaying) SimpIcons.PauseCircle else SimpIcons.PlayCircle,
                        null,
                        modifier = Modifier.size(52.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = { actions.onUIEvent(UIEvent.Next) }) {
                    Icon(SimpIcons.SkipNext, null, tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            Spacer(Modifier.weight(0.25f))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = actions.onShowQueue) {
                    Icon(SimpIcons.QueueMusic, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = actions.onShowFullscreenLyrics) {
                    Icon(SimpIcons.Lyrics, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = actions.onShowMoreSheet) {
                    Icon(SimpIcons.MoreVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.weight(0.2f))
        }
    }
}

// ---------------------------------------------------------------------------
// V4 — CINEMATIC (ArchiveTune's default design)
// ---------------------------------------------------------------------------

@Composable
fun NowPlayingContentCinematic(state: NowPlayingContentState, actions: NowPlayingContentActions) {
    val accent = state.startColor.value
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // Blurred, full-bleed artwork backdrop with a dark scrim.
        Box(Modifier.fillMaxSize()) {
            state.screenData.thumbnailURL?.let { url ->
                AsyncImage(
                    model =
                        ImageRequest.Builder(LocalContext.current)
                            .data(url)
                            .diskCacheKey(url + "CINEMATIC_BG")
                            .crossfade(true)
                            .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(48.dp).graphicsLayer { alpha = 0.45f },
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.5f), Color.Black.copy(alpha = 0.85f)),
                        ),
                    ),
            )
        }
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.3f))
            ATArtworkPager(
                state = state,
                actions = actions,
                modifier =
                    Modifier
                        .fillMaxWidth(0.86f)
                        .aspectRatio(1f),
                shape = RoundedCornerShape(20.dp),
            )
            Spacer(Modifier.weight(0.45f))
            // Letterboxed block: small uppercase caption then the big title.
            Text(
                text = state.screenData.artistName.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 2.sp,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = state.screenData.nowPlayingTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            ATSlider(state = state, actions = actions, accent = accent)
            Spacer(Modifier.height(12.dp))
            ATTransportControls(
                state = state,
                actions = actions,
                playIcon = if (state.controllerState.isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                playFilled = true,
                accent = accent,
            )
            Spacer(Modifier.height(4.dp))
            ATSecondaryRow(state = state, actions = actions, accent = accent)
            Spacer(Modifier.weight(0.3f))
        }
    }
}

// ---------------------------------------------------------------------------
// V5 — LITTLE
// ---------------------------------------------------------------------------

@Composable
fun NowPlayingContentLittle(state: NowPlayingContentState, actions: NowPlayingContentActions) {
    val accent = state.startColor.value
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(accent.copy(alpha = 0.18f), PlayerBackdropColor, PlayerBackdropColor),
                ),
            ),
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.5f))
            ATArtworkPager(
                state = state,
                actions = actions,
                modifier = Modifier.size(104.dp),
                shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.weight(0.6f))
            Text(
                text = state.screenData.nowPlayingTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = state.screenData.artistName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.weight(0.55f))
            ATSlider(state = state, actions = actions, accent = accent, trackHeight = 6)
            Spacer(Modifier.height(8.dp))
            ATTransportControls(
                state = state,
                actions = actions,
                playIcon = if (state.controllerState.isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                playFilled = true,
                accent = accent,
                compact = true,
            )
            Spacer(Modifier.height(2.dp))
            ATSecondaryRow(state = state, actions = actions, accent = accent)
            Spacer(Modifier.weight(0.3f))
        }
    }
}

// ---------------------------------------------------------------------------
// V8 — IMMERSIVE EXTENDED
// ---------------------------------------------------------------------------

@Composable
fun NowPlayingContentImmersiveExtended(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    val accent = state.startColor.value
    Box(Modifier.fillMaxSize().background(Color(0xFF0A0A0C))) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Spacer(Modifier.weight(0.25f))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = actions.onDismiss) {
                    Icon(SimpIcons.KeyboardArrowDown, null, tint = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "PLAYING NOW",
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 2.sp,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
            Spacer(Modifier.weight(0.35f))
            ATArtworkPager(
                state = state,
                actions = actions,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                shape = RoundedCornerShape(24.dp),
            )
            Spacer(Modifier.weight(0.4f))
            ATTitle(state = state, actions = actions, centered = false, big = true)
            Spacer(Modifier.height(12.dp))
            // Extended: codec/quality chips row (the "Extended" in the name).
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.audioCodecLabel != null) {
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                state.audioCodecLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                            )
                        },
                    )
                }
                if (state.screenData.isExplicit) {
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                "EXPLICIT",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                            )
                        },
                    )
                }
                if (state.shouldShowVideo) {
                    SuggestionChip(
                        onClick = actions.onEnterFullscreenVideo,
                        label = {
                            Text(
                                "VIDEO",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                            )
                        },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            ATSlider(state = state, actions = actions, accent = accent)
            Spacer(Modifier.height(8.dp))
            ATTransportControls(
                state = state,
                actions = actions,
                playIcon = if (state.controllerState.isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                playFilled = true,
                accent = accent,
            )
            Spacer(Modifier.height(4.dp))
            ATSecondaryRow(state = state, actions = actions, accent = accent)
            // Extended: a peek at what the queue plays next.
            val next =
                remember(state.artworkQueue, state.currentOrderIndex) {
                    state.artworkQueue.drop(state.currentOrderIndex + 1).take(2)
                }
            if (next.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "NEXT UP",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.55f),
                )
                next.forEach { track ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = track.duration ?: "",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.55f),
                        )
                    }
                }
            }
            Spacer(Modifier.weight(0.25f))
        }
    }
}

// ---------------------------------------------------------------------------
// V9 — MATERIAL EXTENDED
// ---------------------------------------------------------------------------

@Composable
fun NowPlayingContentMaterialExtended(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    val accent = state.startColor.value
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(accent.copy(alpha = 0.22f), PlayerBackdropColor, PlayerBackdropColor),
                ),
            ),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Spacer(Modifier.weight(0.2f))
            Text(
                text = state.screenData.nowPlayingTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = state.screenData.artistName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.weight(0.25f))
            ATArtworkPager(
                state = state,
                actions = actions,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                shape = RoundedCornerShape(32.dp),
            )
            Spacer(Modifier.weight(0.3f))
            // Segmented shuffle/repeat chips — the Material "segmented" control.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.controllerState.isShuffle,
                    onClick = { actions.onUIEvent(UIEvent.Shuffle) },
                    label = { Text("Shuffle") },
                )
                FilterChip(
                    selected = state.controllerState.repeatState !is com.maxrave.domain.mediaservice.handler.RepeatState.None,
                    onClick = { actions.onUIEvent(UIEvent.Repeat) },
                    label = {
                        Text(
                            when (state.controllerState.repeatState) {
                                is com.maxrave.domain.mediaservice.handler.RepeatState.One -> "Repeat 1"
                                else -> "Repeat"
                            },
                        )
                    },
                )
            }
            Spacer(Modifier.height(10.dp))
            ATSlider(state = state, actions = actions, accent = accent)
            Spacer(Modifier.height(6.dp))
            // Tonally filled prev/next + huge filled play.
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(onClick = { actions.onUIEvent(UIEvent.Previous) }) {
                    Icon(SimpIcons.SkipPrevious, null)
                }
                FilledIconButton(
                    onClick = { actions.onUIEvent(UIEvent.PlayPause) },
                    modifier = Modifier.size(84.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = accent),
                ) {
                    Icon(
                        if (state.controllerState.isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                        null,
                        modifier = Modifier.size(44.dp),
                        tint = Color.Black,
                    )
                }
                FilledTonalIconButton(onClick = { actions.onUIEvent(UIEvent.Next) }) {
                    Icon(SimpIcons.SkipNext, null)
                }
            }
            Spacer(Modifier.height(6.dp))
            ATSecondaryRow(state = state, actions = actions, accent = accent)
            Spacer(Modifier.weight(0.25f))
        }
    }
}

// ---------------------------------------------------------------------------
// V10 — EDITORIAL
// ---------------------------------------------------------------------------

@Composable
fun NowPlayingContentEditorial(state: NowPlayingContentState, actions: NowPlayingContentActions) {
    val accent = state.startColor.value
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(accent.copy(alpha = 0.14f), PlayerBackdropColor, PlayerBackdropColor),
                ),
            ),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Spacer(Modifier.weight(0.18f))
            // Magazine: giant display title FIRST, artwork offset below.
            Text(
                text = state.screenData.nowPlayingTitle,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "by ${state.screenData.artistName}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.weight(0.22f))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                ATArtworkPager(
                    state = state,
                    actions = actions,
                    modifier =
                        Modifier
                            .fillMaxWidth(0.78f)
                            .aspectRatio(1f),
                    shape = RoundedCornerShape(4.dp),
                )
            }
            Spacer(Modifier.weight(0.25f))
            ATSlider(state = state, actions = actions, accent = accent)
            Spacer(Modifier.height(6.dp))
            ATTransportControls(
                state = state,
                actions = actions,
                playIcon = if (state.controllerState.isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                playFilled = true,
                accent = accent,
                compact = true,
            )
            Spacer(Modifier.height(2.dp))
            ATSecondaryRow(state = state, actions = actions, accent = accent)
            Spacer(Modifier.weight(0.22f))
        }
    }
}
