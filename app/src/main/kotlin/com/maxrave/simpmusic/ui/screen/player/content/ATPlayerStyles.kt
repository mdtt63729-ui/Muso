@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package com.maxrave.simpmusic.ui.screen.player.content

/*
 * Muso Round 189: the ArchiveTune player designs, ported AS THEY ARE from the
 * ArchiveTune repository (moe.rukamori kit sources shipped in this app are the
 * upstream code, and the layout numbers below mirror those sources exactly).
 *
 * ArchiveTune ships ten player designs (V1 Classic .. V10 Editorial). Muso's
 * own three (Classic/Spotify, Expressive/M3, Immersive/AppleMusic) map to
 * V1/V6/V7; this file implements the other seven against the same
 * NowPlayingContentState / NowPlayingContentActions contract the existing
 * styles use, so they drop into the shell without any new wiring:
 *
 *   MODERN (V2)            - asymmetric tab chips + wide 1.6:1 play pill
 *   MINIMAL (V3)           - flat ghost buttons, only style with full transport
 *   CINEMATIC (V4)         - glass chips + 88dp morphing-corner play button
 *   LITTLE (V5)            - typography-driven, whole screen is the seekbar
 *   IMMERSIVE EXTENDED(V8) - blurred artwork backdrop + flat white controls
 *   MATERIAL EXTENDED (V9) - dynamic flat background + wavy slider + morph transport
 *   EDITORIAL (V10)        - two-tone die-cut editorial layout
 *
 * Every artwork box is filled EDGE TO EDGE with the current song's thumbnail
 * (ContentScale.Crop) and loads the ultra-high (1200px / maxres) variant.
 */

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Favorite
import com.maxrave.simpmusic.ui.icon.FavoriteBorder
import com.maxrave.simpmusic.ui.icon.GraphicEq
import com.maxrave.simpmusic.ui.icon.KeyboardArrowDown
import com.maxrave.simpmusic.ui.icon.Lyrics
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.Pause
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.PlaylistAdd
import com.maxrave.simpmusic.ui.icon.QueueMusic
import com.maxrave.simpmusic.ui.icon.Repeat
import com.maxrave.simpmusic.ui.icon.RepeatOne
import com.maxrave.simpmusic.ui.icon.Share
import com.maxrave.simpmusic.ui.icon.Shuffle
import com.maxrave.simpmusic.ui.icon.SkipNext
import com.maxrave.simpmusic.ui.icon.SkipPrevious
import com.maxrave.simpmusic.viewModel.UIEvent
import com.maxrave.simpmusic.ui.theme.LocalForceDarkText
import com.materialkolor.ktx.toColor
import com.materialkolor.ktx.toHct
import moe.rukamori.archivetune.ui.component.PlayerSliderTrack
import moe.rukamori.archivetune.ui.player.StyledPlaybackSlider
import moe.rukamori.archivetune.ui.player.V9AnimatedPlaybackControls
import moe.rukamori.archivetune.ui.player.WavySliderExpressive
import moe.rukamori.archivetune.utils.makeTimeString
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

// ---------------------------------------------------------------------------
// Shared helpers
// ---------------------------------------------------------------------------

/**
 * Ultra-high artwork: bump the stored googleusercontent variant from ~544px to
 * 1200px, or use maxresdefault for plain i.ytimg.com URLs (user request).
 */
private fun atUltraHigh(url: String?): String? {
    if (url == null) return null
    if (url.contains("googleusercontent.com/") || url.contains("ggpht.com")) {
        val base = url.substringBefore("?")
        val wh = Regex("""=w(\d+)-h(\d+)([^=]*)$""").find(base) ?: return url
        val width = wh.groupValues[1].toIntOrNull() ?: 0
        if (width in 1 until 1200) {
            return base.replaceRange(wh.range, "=w1200-h1200" + wh.groupValues[3])
        }
        return url
    }
    val i = url.indexOf("i.ytimg.com/vi/")
    if (i < 0) return url
    val id = url.substringAfter("i.ytimg.com/vi/").substringBefore("/")
    if (id.isBlank()) return url
    return "https://i.ytimg.com/vi/$id/maxresdefault.jpg"
}

@Composable
private fun atTrackArtworkUrl(page: Int, state: NowPlayingContentState): String? =
    atUltraHigh(
        state.artworkQueue.getOrNull(page)?.thumbnails?.lastOrNull()?.url
            ?: state.thumbnailURL,
    )

/** The centered "Now Playing / playlist" header above the artwork (Thumbnail.kt). */
@Composable
private fun ATNowPlayingHeader(
    state: NowPlayingContentState,
    textColor: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
    ) {
        Text(
            text = "Now Playing",
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
        )
        val playingFrom = state.screenData.playlistName
        if (!playingFrom.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = playingFrom,
                style = MaterialTheme.typography.titleMedium,
                color = textColor.copy(alpha = 0.8f),
                maxLines = 1,
                modifier = Modifier.basicMarquee(),
            )
        }
    }
}

/**
 * Artwork pager: swipe changes song. The box is filled EDGE TO EDGE with the
 * current track's ultra-high thumbnail (user request).
 */
@Composable
private fun ATArtworkPager(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
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
        beyondViewportPageCount = 0,
    ) { page ->
        val isCurrent = page == state.artworkPagerState.currentPage
        val url = atTrackArtworkUrl(page, state)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(cornerRadius))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .graphicsLayer { alpha = if (isCurrent) 1f else 0.65f },
        ) {
            if (isCurrent && state.screenData.canvasData?.isVideo == true) {
                com.maxrave.simpmusic.expect.ui.MediaPlayerView(
                    url = state.screenData.canvasData.url,
                    cropToBounds = true,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(url)
                        .diskCacheKey((url ?: "none") + "AT_STYLE_HQ")
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

/** Single static artwork box, edge to edge (V8 / V9 / V10). */
@Composable
private fun ATStaticArtwork(
    state: NowPlayingContentState,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
    placeholder: Color = Color.White.copy(alpha = 0.08f),
) {
    val url = atUltraHigh(
        state.artworkQueue.getOrNull(state.currentOrderIndex)?.thumbnails?.lastOrNull()?.url
            ?: state.thumbnailURL,
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(placeholder),
    ) {
        if (state.screenData.canvasData?.isVideo == true) {
            com.maxrave.simpmusic.expect.ui.MediaPlayerView(
                url = state.screenData.canvasData.url,
                cropToBounds = true,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .diskCacheKey((url ?: "none") + "AT_STYLE_HQ")
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Title + artists column (PlayerTitleSection). */
@Composable
private fun ATTrackInfoColumn(
    state: NowPlayingContentState,
    textColor: Color,
) {
    Text(
        text = state.screenData.nowPlayingTitle,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = textColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .basicMarquee(),
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = state.screenData.artistName,
        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
        color = textColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .basicMarquee(),
    )
}

/** The AT slider: the user's slider style preference drives the real AT slider. */
@Composable
private fun ATStyledSlider(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    activeColor: Color,
    modifier: Modifier = Modifier,
) {
    val sliderStyle by com.muso.music.utils.rememberEnumPreference(
        key = com.muso.music.constants.SliderStyleKey,
        defaultValue = com.muso.music.constants.SliderStyle.Standard,
    )
    StyledPlaybackSlider(
        sliderStyle = sliderStyle,
        value = state.sliderValue,
        valueRange = 0f..100f,
        onValueChange = actions.onSliderChange,
        onValueChangeFinished = actions.onSliderChangeFinished,
        activeColor = activeColor,
        isPlaying = state.controllerState.isPlaying,
        modifier = modifier,
    )
}

/** Time labels: elapsed left, duration right (PlayerTimeLabel). */
@Composable
private fun ATTimeLabels(
    state: NowPlayingContentState,
    textColor: Color,
    horizontalPadding: Dp = 36.dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
    ) {
        Text(
            text = makeTimeString(
                (state.timelineState.total * (state.sliderValue / 100f)).roundToLong(),
            ),
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Text(
            text = makeTimeString(state.timelineState.total),
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

private fun atShareSong(context: Context, state: NowPlayingContentState) {
    val intent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(
            Intent.EXTRA_TEXT,
            "Now playing: ${state.screenData.nowPlayingTitle} - ${state.screenData.artistName}",
        )
    }
    context.startActivity(Intent.createChooser(intent, null))
}

// ---------------------------------------------------------------------------
// V2 / V3 / V4 shared shell (Player.kt 1841-1869 + PlayerControlsContent)
// ---------------------------------------------------------------------------

@Composable
private fun ATCinemaShell(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    topActions: @Composable () -> Unit,
    transport: @Composable () -> Unit,
) {
    // Resolve foreground from the same artwork-derived seed that drives the player background.
    // AT styles used a permanently dark Material scheme, so a light artwork background could
    // leave black-on-white / white-on-black components mismatched. Keep the actual layout intact,
    // but make the semantic content colours follow the rendered background.
    val artworkBackground = state.startColor.value
    val isLightBackground = artworkBackground.luminance() > 0.52f
    val textBackgroundColor = if (isLightBackground) Color.Black else Color.White
    val backgroundColor = if (isLightBackground) Color.White else Color.Black
    val adaptiveScheme = MaterialTheme.colorScheme.copy(
        background = backgroundColor,
        surface = backgroundColor,
        surfaceVariant = if (isLightBackground) Color(0xFFE9E9E9) else Color(0xFF202020),
        onBackground = textBackgroundColor,
        onSurface = textBackgroundColor,
        onSurfaceVariant = textBackgroundColor.copy(alpha = 0.78f),
        primary = textBackgroundColor,
        onPrimary = backgroundColor,
        primaryContainer = textBackgroundColor.copy(alpha = 0.14f),
        onPrimaryContainer = textBackgroundColor,
    )
    MaterialTheme(colorScheme = adaptiveScheme) {
        CompositionLocalProvider(LocalContentColor provides textBackgroundColor) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize(),
            ) {
        ATNowPlayingHeader(state = state, textColor = textBackgroundColor)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            ATArtworkPager(
                state = state,
                actions = actions,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .aspectRatio(1f),
                cornerRadius = 16.dp,
            )
        }

        // Title row
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                ATTrackInfoColumn(state = state, textColor = textBackgroundColor)
            }
            Spacer(Modifier.width(12.dp))
            topActions()
        }

        Spacer(Modifier.height(12.dp))

        ATStyledSlider(
            state = state,
            actions = actions,
            activeColor = textBackgroundColor,
            modifier = Modifier.padding(horizontal = 32.dp),
        )

        PlayerCodecCapsule(
            state = state,
            modifier = Modifier.padding(top = 6.dp),
        )

        Spacer(Modifier.height(4.dp))

        ATTimeLabels(state = state, textColor = textBackgroundColor)

        Spacer(Modifier.height(12.dp))

        transport()

                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// V2 — MODERN
// ---------------------------------------------------------------------------

@Composable
fun NowPlayingContentModern(state: NowPlayingContentState, actions: NowPlayingContentActions) {
    val context = LocalContext.current
    val textBackgroundColor = MaterialTheme.colorScheme.onBackground
    val textButtonColor = textBackgroundColor
    val iconButtonColor = MaterialTheme.colorScheme.surface
    ATCinemaShell(
        state = state,
        actions = actions,
        topActions = {
            // V2: asymmetric tab chips (PlayerTopActions V2)
            val shareShape = RoundedCornerShape(
                topStart = 50.dp, bottomStart = 50.dp,
                topEnd = 10.dp, bottomEnd = 10.dp,
            )
            val favShape = RoundedCornerShape(
                topStart = 10.dp, bottomStart = 10.dp,
                topEnd = 50.dp, bottomEnd = 50.dp,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(shareShape)
                        .background(textButtonColor)
                        .clickable { atShareSong(context, state) },
                ) {
                    Icon(
                        imageVector = SimpIcons.Share,
                        contentDescription = null,
                        tint = iconButtonColor,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(24.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(favShape)
                        .background(textButtonColor)
                        .clickable { actions.onUIEvent(UIEvent.ToggleLike) },
                ) {
                    Icon(
                        imageVector = if (state.likeStatus) SimpIcons.Favorite else SimpIcons.FavoriteBorder,
                        contentDescription = null,
                        tint = iconButtonColor,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(24.dp),
                    )
                }
            }
        },
        transport = {
            // V2: width-proportional pill transport (PlayerPlaybackControls V2)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val maxW = maxWidth
                val playButtonHeight = maxW / 6f
                val playButtonWidth = playButtonHeight * 1.6f
                val sideButtonHeight = playButtonHeight * 0.8f
                val sideButtonWidth = sideButtonHeight * 1.3f
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    FilledTonalIconButton(
                        onClick = { actions.onUIEvent(UIEvent.Previous) },
                        enabled = state.controllerState.isPreviousAvailable,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = textButtonColor,
                            contentColor = iconButtonColor,
                        ),
                        modifier = Modifier
                            .size(width = sideButtonWidth, height = sideButtonHeight)
                            .clip(RoundedCornerShape(32.dp)),
                    ) {
                        Icon(
                            imageVector = SimpIcons.SkipPrevious,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    FilledIconButton(
                        onClick = { actions.onUIEvent(UIEvent.PlayPause) },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = textButtonColor,
                            contentColor = iconButtonColor,
                        ),
                        modifier = Modifier
                            .size(width = playButtonWidth, height = playButtonHeight)
                            .clip(RoundedCornerShape(32.dp)),
                    ) {
                        Icon(
                            imageVector = if (state.controllerState.isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(42.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    FilledTonalIconButton(
                        onClick = { actions.onUIEvent(UIEvent.Next) },
                        enabled = state.controllerState.isNextAvailable,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = textButtonColor,
                            contentColor = iconButtonColor,
                        ),
                        modifier = Modifier
                            .size(width = sideButtonWidth, height = sideButtonHeight)
                            .clip(RoundedCornerShape(32.dp)),
                    ) {
                        Icon(
                            imageVector = SimpIcons.SkipNext,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }
        },
    )
}

// ---------------------------------------------------------------------------
// V3 — MINIMAL
// ---------------------------------------------------------------------------

@Composable
fun NowPlayingContentMinimal(state: NowPlayingContentState, actions: NowPlayingContentActions) {
    val context = LocalContext.current
    val textBackgroundColor = MaterialTheme.colorScheme.onBackground
    val icBackgroundColor = MaterialTheme.colorScheme.surface
    ATCinemaShell(
        state = state,
        actions = actions,
        topActions = {
            // V3: borderless action boxes (PlayerTopActions V3)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { atShareSong(context, state) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = SimpIcons.Share,
                        contentDescription = null,
                        tint = textBackgroundColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { actions.onUIEvent(UIEvent.ToggleLike) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (state.likeStatus) SimpIcons.Favorite else SimpIcons.FavoriteBorder,
                        contentDescription = null,
                        tint = if (state.likeStatus) {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                        } else {
                            textBackgroundColor.copy(alpha = 0.7f)
                        },
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
        transport = {
            // V3: flat SpaceEvenly transport with shuffle + repeat
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { actions.onUIEvent(UIEvent.Shuffle) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = SimpIcons.Shuffle,
                            contentDescription = null,
                            tint = textBackgroundColor.copy(
                                alpha = if (state.controllerState.isShuffle) 1f else 0.4f,
                            ),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(textBackgroundColor.copy(alpha = 0.08f))
                            .clickable(enabled = state.controllerState.isPreviousAvailable) {
                                actions.onUIEvent(UIEvent.Previous)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = SimpIcons.SkipPrevious,
                            contentDescription = null,
                            tint = textBackgroundColor.copy(
                                alpha = if (state.controllerState.isPreviousAvailable) 0.9f else 0.4f,
                            ),
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(50))
                            .background(textBackgroundColor)
                            .clickable { actions.onUIEvent(UIEvent.PlayPause) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (state.controllerState.isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                            contentDescription = null,
                            tint = icBackgroundColor,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(textBackgroundColor.copy(alpha = 0.08f))
                            .clickable(enabled = state.controllerState.isNextAvailable) {
                                actions.onUIEvent(UIEvent.Next)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = SimpIcons.SkipNext,
                            contentDescription = null,
                            tint = textBackgroundColor.copy(
                                alpha = if (state.controllerState.isNextAvailable) 0.9f else 0.4f,
                            ),
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { actions.onUIEvent(UIEvent.Repeat) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (state.controllerState.repeatState == com.maxrave.domain.mediaservice.handler.RepeatState.One) {
                                SimpIcons.RepeatOne
                            } else {
                                SimpIcons.Repeat
                            },
                            contentDescription = null,
                            tint = textBackgroundColor.copy(
                                alpha = if (state.controllerState.repeatState == com.maxrave.domain.mediaservice.handler.RepeatState.None) 0.4f else 1f,
                            ),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        },
    )
}

// ---------------------------------------------------------------------------
// V4 — CINEMATIC (ArchiveTune's default)
// ---------------------------------------------------------------------------

@Composable
fun NowPlayingContentCinematic(state: NowPlayingContentState, actions: NowPlayingContentActions) {
    val context = LocalContext.current
    val textBackgroundColor = MaterialTheme.colorScheme.onBackground
    val textButtonColor = textBackgroundColor
    val icBackgroundColor = MaterialTheme.colorScheme.surface
    val view = LocalView.current
    ATCinemaShell(
        state = state,
        actions = actions,
        topActions = {
            // V4: three glass chips - share / favourite / more (PlayerTopActions V4)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = { atShareSong(context, state) },
                    shape = RoundedCornerShape(14.dp),
                    color = textBackgroundColor.copy(alpha = 0.12f),
                    modifier = Modifier
                        .height(44.dp)
                        .width(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = SimpIcons.Share,
                            contentDescription = null,
                            tint = textBackgroundColor,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Surface(
                    onClick = { actions.onUIEvent(UIEvent.ToggleLike) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (state.likeStatus) {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
                    } else {
                        textBackgroundColor.copy(alpha = 0.12f)
                    },
                    modifier = Modifier
                        .height(44.dp)
                        .width(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = if (state.likeStatus) SimpIcons.Favorite else SimpIcons.FavoriteBorder,
                            contentDescription = null,
                            tint = if (state.likeStatus) {
                                MaterialTheme.colorScheme.error
                            } else {
                                textBackgroundColor
                            },
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Surface(
                    onClick = actions.onShowMoreSheet,
                    shape = RoundedCornerShape(14.dp),
                    color = textBackgroundColor.copy(alpha = 0.12f),
                    modifier = Modifier
                        .height(44.dp)
                        .width(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = SimpIcons.MoreVert,
                            contentDescription = null,
                            tint = textBackgroundColor,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        },
        transport = {
            // V4: responsive glass transport with a morphing-corner play button
            val cinematicPlayPauseCorner by animateDpAsState(
                targetValue = if (state.controllerState.isPlaying) 28.dp else 44.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "cinematicPlayPauseCorner",
            )
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
            ) {
                val baseLarge = 56.dp
                val baseSmall = 46.dp
                val baseGap = 12.dp
                val baseLargeIcon = 28.dp
                val baseSmallIcon = 22.dp
                val baseLargeRadius = 18.dp
                val baseSmallRadius = 16.dp
                val centerSize = 88.dp
                val centerPadding = 40.dp
                val sideTotal = (maxWidth - centerSize - centerPadding) / 2f
                val scale = ((sideTotal - baseGap) / (baseLarge + baseSmall))
                    .coerceAtMost(1f)
                    .coerceAtLeast(0.6f)
                val large = baseLarge * scale
                val small = baseSmall * scale
                val gap = baseGap * scale
                val largeIcon = baseLargeIcon * scale
                val smallIcon = baseSmallIcon * scale
                val largeRadius = baseLargeRadius * scale
                val smallRadius = baseSmallRadius * scale

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            onClick = { actions.onUIEvent(UIEvent.Shuffle) },
                            shape = RoundedCornerShape(smallRadius),
                            color = textBackgroundColor.copy(
                                alpha = if (state.controllerState.isShuffle) 0.2f else 0.08f,
                            ),
                            modifier = Modifier.size(small),
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = SimpIcons.Shuffle,
                                    contentDescription = null,
                                    tint = textBackgroundColor.copy(
                                        alpha = if (state.controllerState.isShuffle) 1f else 0.6f,
                                    ),
                                    modifier = Modifier.size(smallIcon),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(gap))
                        Surface(
                            onClick = { actions.onUIEvent(UIEvent.Previous) },
                            enabled = state.controllerState.isPreviousAvailable,
                            shape = RoundedCornerShape(largeRadius),
                            color = textBackgroundColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(large),
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = SimpIcons.SkipPrevious,
                                    contentDescription = null,
                                    tint = textBackgroundColor.copy(
                                        alpha = if (state.controllerState.isPreviousAvailable) 1f else 0.4f,
                                    ),
                                    modifier = Modifier.size(largeIcon),
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = { actions.onUIEvent(UIEvent.PlayPause) },
                        shape = RoundedCornerShape(cinematicPlayPauseCorner),
                        color = textButtonColor,
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .size(88.dp),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (state.controllerState.isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                                contentDescription = null,
                                tint = icBackgroundColor,
                                modifier = Modifier.size(44.dp),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            onClick = { actions.onUIEvent(UIEvent.Next) },
                            enabled = state.controllerState.isNextAvailable,
                            shape = RoundedCornerShape(largeRadius),
                            color = textBackgroundColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(large),
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = SimpIcons.SkipNext,
                                    contentDescription = null,
                                    tint = textBackgroundColor.copy(
                                        alpha = if (state.controllerState.isNextAvailable) 1f else 0.4f,
                                    ),
                                    modifier = Modifier.size(largeIcon),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(gap))
                        Surface(
                            onClick = { actions.onUIEvent(UIEvent.Repeat) },
                            shape = RoundedCornerShape(smallRadius),
                            color = textBackgroundColor.copy(
                                alpha = if (state.controllerState.repeatState != com.maxrave.domain.mediaservice.handler.RepeatState.None) 0.2f else 0.08f,
                            ),
                            modifier = Modifier.size(small),
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (state.controllerState.repeatState == com.maxrave.domain.mediaservice.handler.RepeatState.One) {
                                        SimpIcons.RepeatOne
                                    } else {
                                        SimpIcons.Repeat
                                    },
                                    contentDescription = null,
                                    tint = textBackgroundColor.copy(
                                        alpha = if (state.controllerState.repeatState == com.maxrave.domain.mediaservice.handler.RepeatState.None) 0.6f else 1f,
                                    ),
                                    modifier = Modifier.size(smallIcon),
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}

// ---------------------------------------------------------------------------
// V5 — LITTLE
// ---------------------------------------------------------------------------

@Composable
fun NowPlayingContentLittle(state: NowPlayingContentState, actions: NowPlayingContentActions) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val textColor = MaterialTheme.colorScheme.onPrimaryContainer

    val durationMs = state.timelineState.total
    val progressFraction =
        if (durationMs > 0) state.sliderValue / 100f else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(primaryContainer)
            .littlePlayerOverlayGestures(
                seekEnabled = true,
                durationMs = durationMs,
                progressFraction = progressFraction,
                canSkipPrevious = state.controllerState.isPreviousAvailable,
                canSkipNext = state.controllerState.isNextAvailable,
                onSeekToPositionMs = { ms ->
                    if (durationMs > 0) actions.onSliderChange((ms * 100f / durationMs).coerceIn(0f, 100f))
                },
                onSeekFinished = actions.onSliderChangeFinished,
                onSkipPrevious = { actions.onUIEvent(UIEvent.Previous) },
                onSkipNext = { actions.onUIEvent(UIEvent.Next) },
            ),
    ) {
        // Full-screen progress fill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(progressFraction)
                .align(Alignment.TopStart)
                .background(primary.copy(alpha = 0.28f)),
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val titleColor = textColor.copy(alpha = 0.95f)
            val secondaryColor = textColor.copy(alpha = 0.6f)
            val timeColor = textColor.copy(alpha = 0.85f)

            val scale = minOf(maxWidth / 420.dp, maxHeight / 260.dp).coerceIn(0.78f, 1.15f)
            val titleSize = (56f * scale).sp
            val timeSize = (44f * scale).sp
            val iconSize = (26f * scale).dp
            val collapseIconSize = (28f * scale).dp
            val horizontalPadding = (18f * scale).dp
            val verticalPadding = (10f * scale).dp

            val timeText = remember(state.sliderValue, durationMs) {
                val positionText = makeTimeString((durationMs * (state.sliderValue / 100f)).roundToLong())
                val durationText = if (durationMs > 0) makeTimeString(durationMs) else ""
                if (durationText.isBlank()) positionText else "$positionText/$durationText"
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            ) {
                Spacer(Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.screenData.nowPlayingTitle,
                            color = titleColor,
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = titleSize,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee(),
                        )
                        Spacer(Modifier.height((10f * scale).dp))
                        val playingFrom = state.screenData.playlistName
                        if (!playingFrom.isNullOrBlank()) {
                            Text(
                                text = playingFrom,
                                color = secondaryColor,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee(),
                            )
                        }
                        if (state.screenData.artistName.isNotBlank()) {
                            Text(
                                text = "by - ${state.screenData.artistName}",
                                color = secondaryColor,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee(),
                            )
                        }
                    }

                    Spacer(Modifier.width((16f * scale).dp))

                    Text(
                        text = timeText,
                        color = timeColor,
                        fontSize = timeSize,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        modifier = Modifier.widthIn(min = (140f * scale).dp),
                    )
                }

                PlayerCodecCapsule(
                    state = state,
                    modifier = Modifier.padding(top = (6f * scale).dp),
                    containerColor = textColor.copy(alpha = 0.10f),
                    contentColor = textColor.copy(alpha = 0.82f),
                )

                Spacer(Modifier.height((14f * scale).dp))
                Spacer(Modifier.height((6f * scale).dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = SimpIcons.KeyboardArrowDown,
                        contentDescription = null,
                        tint = textColor.copy(alpha = 0.8f),
                        modifier = Modifier
                            .size(collapseIconSize)
                            .clickable(onClick = actions.onDismiss),
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = if (state.likeStatus) SimpIcons.Favorite else SimpIcons.FavoriteBorder,
                        contentDescription = null,
                        tint = if (state.likeStatus) {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                        } else {
                            textColor.copy(alpha = 0.78f)
                        },
                        modifier = Modifier
                            .size(iconSize)
                            .clickable { actions.onUIEvent(UIEvent.ToggleLike) },
                    )
                    Spacer(Modifier.width((18f * scale).dp))
                    Icon(
                        imageVector = SimpIcons.QueueMusic,
                        contentDescription = null,
                        tint = textColor.copy(alpha = 0.78f),
                        modifier = Modifier
                            .size(iconSize)
                            .clickable(onClick = actions.onShowQueue),
                    )
                    Spacer(Modifier.width((18f * scale).dp))
                    Icon(
                        imageVector = SimpIcons.MoreVert,
                        contentDescription = null,
                        tint = textColor.copy(alpha = 0.78f),
                        modifier = Modifier
                            .size(iconSize)
                            .clickable(onClick = actions.onShowMoreSheet),
                    )
                }
            }
        }
    }
}

/** V5 gestures: vertical drag in the progress region seeks, double-tap skips. */
private fun Modifier.littlePlayerOverlayGestures(
    seekEnabled: Boolean,
    durationMs: Long,
    progressFraction: Float,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    onSeekToPositionMs: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
): Modifier = Modifier.composed {
    val view = LocalView.current
    this.pointerInput(seekEnabled, durationMs, canSkipPrevious, canSkipNext) {
        var lastTapUptimeMs = 0L
        var lastTapPosition: androidx.compose.ui.geometry.Offset? = null
        val doubleTapTimeoutMs = viewConfiguration.doubleTapTimeoutMillis.toLong()
        val touchSlop = viewConfiguration.touchSlop

        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = true)
            val pointerId = down.id

            var upPosition = down.position
            val minOverlayHeightPx = 24.dp.toPx()
            val overlayHeightPx =
                (progressFraction * size.height).coerceAtLeast(minOverlayHeightPx)
            val seekAllowedFromDown =
                seekEnabled && durationMs > 0L && down.position.y <= overlayHeightPx

            var isSeeking = false

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val change = event.changes.firstOrNull { it.id == pointerId } ?: continue
                upPosition = change.position

                if (!change.pressed) break

                if (!isSeeking && seekAllowedFromDown) {
                    val distanceFromDown = (change.position - down.position).getDistance()
                    if (distanceFromDown > touchSlop) isSeeking = true
                }

                if (isSeeking) {
                    val fraction =
                        if (size.height > 0) (change.position.y / size.height.toFloat()) else 0f
                    val clampedFraction = fraction.coerceIn(0f, 1f)
                    val targetMs = (durationMs.toDouble() * clampedFraction.toDouble())
                        .roundToLong()
                        .coerceIn(0L, durationMs)
                    onSeekToPositionMs(targetMs)
                    change.consume()
                }
            }

            if (isSeeking) {
                onSeekFinished()
                lastTapUptimeMs = 0L
                lastTapPosition = null
            } else {
                val now = SystemClock.uptimeMillis()
                val previousTapPosition = lastTapPosition
                val isDoubleTap = previousTapPosition != null &&
                    (now - lastTapUptimeMs) <= doubleTapTimeoutMs &&
                    (upPosition - previousTapPosition).getDistance() <= (touchSlop * 2f)

                if (isDoubleTap) {
                    val isTopSide = upPosition.y < size.height / 2f
                    if (isTopSide) {
                        if (canSkipPrevious) {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                            onSkipPrevious()
                        }
                    } else {
                        if (canSkipNext) {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                            onSkipNext()
                        }
                    }
                    lastTapUptimeMs = 0L
                    lastTapPosition = null
                } else {
                    lastTapUptimeMs = now
                    lastTapPosition = upPosition
                }
            }
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
    val backdropUrl = atUltraHigh(
        state.artworkQueue.getOrNull(state.currentOrderIndex)?.thumbnails?.lastOrNull()?.url
            ?: state.thumbnailURL,
    )

    // The V8 backdrop is not the raw palette color: the artwork is drawn at 66% opacity and then
    // covered by a 52% black scrim. The old foreground test looked only at the raw palette seed,
    // so a bright red/orange cover could incorrectly produce BLACK text over the much darker
    // rendered surface (exactly what the screenshots show). Resolve contrast against the surface
    // that is actually painted instead.
    val renderedArtwork = state.startColor.value.copy(alpha = 0.66f).compositeOver(Color.Black)
    val renderedBackdrop = Color.Black.copy(alpha = 0.52f).compositeOver(renderedArtwork)
    val isLightRenderedBackdrop = renderedBackdrop.luminance() > 0.52f
    val foreground = if (isLightRenderedBackdrop) Color.Black else Color.White
    val secondaryForeground = foreground
    val adaptiveScheme = MaterialTheme.colorScheme.copy(
        background = renderedBackdrop,
        surface = renderedBackdrop,
        surfaceVariant = if (isLightRenderedBackdrop) Color(0xFFE9E9E9) else Color(0xFF252525),
        onBackground = foreground,
        onSurface = foreground,
        onSurfaceVariant = foreground.copy(alpha = 0.78f),
        primary = foreground,
        onPrimary = renderedBackdrop,
        primaryContainer = foreground.copy(alpha = 0.14f),
        onPrimaryContainer = foreground,
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // V8PlayerBackdrop: blurred artwork, scaled up, dark scrim on top
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(backdropUrl)
                    .diskCacheKey((backdropUrl ?: "none") + "AT_V8_BACKDROP")
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(26.dp)
                    .graphicsLayer {
                        scaleX = 1.16f
                        scaleY = 1.16f
                        alpha = 0.66f
                    },
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.52f)),
            )
        }

        MaterialTheme(colorScheme = adaptiveScheme) {
            CompositionLocalProvider(
                LocalContentColor provides foreground,
                LocalForceDarkText provides isLightRenderedBackdrop.not(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
            Spacer(Modifier.height(14.dp))
            // V8Header: centered "Now playing" + subtitle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Now playing",
                    style = MaterialTheme.typography.titleLarge,
                    color = foreground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = state.screenData.playlistName.ifBlank {
                        state.screenData.artistName
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = secondaryForeground,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(),
                )
            }
            Spacer(Modifier.height(28.dp))
            Spacer(Modifier.weight(1f))

            // V8Artwork: capped square, 8dp corners
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val maxArtworkSize = (maxWidth - 48.dp).coerceAtMost(420.dp)
                ATStaticArtwork(
                    state = state,
                    modifier = Modifier.size(maxArtworkSize),
                    cornerRadius = 8.dp,
                )
            }

            Spacer(Modifier.height(28.dp))

            // V8MetadataActions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = state.screenData.nowPlayingTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = foreground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee(),
                    )
                    Text(
                        text = state.screenData.artistName,
                        style = MaterialTheme.typography.titleMedium,
                        color = foreground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee(),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(foreground.copy(alpha = 0.16f))
                            .clickable { actions.onShowMoreSheet() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = SimpIcons.MoreVert,
                            contentDescription = null,
                            tint = foreground,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(foreground.copy(alpha = 0.16f))
                            .clickable { actions.onUIEvent(UIEvent.ToggleLike) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (state.likeStatus) SimpIcons.Favorite else SimpIcons.FavoriteBorder,
                            contentDescription = null,
                            tint = foreground,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // V8PlaybackProgress: flat slider + quality chip between the times
            ATV8FlatSlider(
                value = state.sliderValue,
                onValueChange = actions.onSliderChange,
                onValueChangeFinished = actions.onSliderChangeFinished,
                activeColor = foreground.copy(alpha = 0.88f),
                inactiveColor = foreground.copy(alpha = 0.32f),
                trackHeight = 9.dp,
            )
            PlayerCodecCapsule(
                state = state,
                modifier = Modifier.padding(top = 4.dp),
                containerColor = foreground.copy(alpha = 0.10f),
                contentColor = foreground.copy(alpha = 0.86f),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(
                    text = makeTimeString(
                        (state.timelineState.total * (state.sliderValue / 100f)).roundToLong(),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = foreground,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
                Text(
                    text = makeTimeString(state.timelineState.total),
                    style = MaterialTheme.typography.labelMedium,
                    color = foreground,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }

            Spacer(Modifier.height(18.dp))

            // V8TransportControls: SpaceEvenly prev / play / next
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .clickable(enabled = state.controllerState.isPreviousAvailable) {
                            actions.onUIEvent(UIEvent.Previous)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = SimpIcons.SkipPrevious,
                        contentDescription = null,
                        tint = foreground.copy(
                            alpha = if (state.controllerState.isPreviousAvailable) 1f else 0.4f,
                        ),
                        modifier = Modifier.size(44.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .clickable { actions.onUIEvent(UIEvent.PlayPause) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (state.controllerState.isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                        contentDescription = null,
                        tint = foreground,
                        modifier = Modifier.size(52.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .clickable(enabled = state.controllerState.isNextAvailable) {
                            actions.onUIEvent(UIEvent.Next)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = SimpIcons.SkipNext,
                        contentDescription = null,
                        tint = foreground.copy(
                            alpha = if (state.controllerState.isNextAvailable) 1f else 0.4f,
                        ),
                        modifier = Modifier.size(44.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

/** V8's flat slider: invisible thumb, custom flat track (V8FlatSlider). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ATV8FlatSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    activeColor: Color,
    inactiveColor: Color,
    trackHeight: Dp,
) {
    val colors = SliderDefaults.colors(
        activeTrackColor = activeColor,
        activeTickColor = activeColor,
        thumbColor = Color.Transparent,
        inactiveTrackColor = inactiveColor,
    )
    Slider(
        value = value,
        valueRange = 0f..100f,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        thumb = { Spacer(modifier = Modifier.size(0.dp)) },
        track = { sliderState ->
            PlayerSliderTrack(
                sliderState = sliderState,
                colors = colors,
                trackHeight = trackHeight,
            )
        },
        modifier = Modifier.height(30.dp),
    )
}

// ---------------------------------------------------------------------------
// V9 — MATERIAL EXTENDED
// ---------------------------------------------------------------------------

@Composable
fun NowPlayingContentMaterialExtended(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    val dominantColor = state.startColor.value
    // dynamicBgColor: HSV remap of the dominant artwork color (dark theme)
    val targetBgColor = remember(dominantColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(dominantColor.toArgb(), hsv)
        hsv[1] = hsv[1].coerceIn(0.12f, 0.35f)
        hsv[2] = 0.08f
        Color(android.graphics.Color.HSVToColor(hsv))
    }
    val dynamicBgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(durationMillis = 800),
        label = "dynamicBgColor",
    )
    // dynamicTextColor: inverse-toned dominant color
    val dynamicTextColor = remember(dominantColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(dominantColor.toArgb(), hsv)
        hsv[1] = hsv[1].coerceAtMost(0.12f)
        // 1f, not 0.96f: the foreground must be TRUE white. At 0.96 it was ~#F5F5F5, and the
        // labels drawn on top of it at 72-78% alpha read as three-quarters white.
        hsv[2] = 1f
        Color(android.graphics.Color.HSVToColor(hsv))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(dynamicBgColor)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(14.dp))

        // V9Header: collapse + "Now playing" + lyrics / queue buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(dynamicTextColor.copy(alpha = 0.16f))
                    .clickable { actions.onDismiss() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = SimpIcons.KeyboardArrowDown,
                    contentDescription = null,
                    tint = dynamicTextColor,
                    modifier = Modifier.size(26.dp),
                )
            }
            Text(
                text = "Now playing",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = dynamicTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .basicMarquee(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(dynamicTextColor.copy(alpha = 0.16f))
                        .clickable { actions.onShowFullscreenLyrics() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = SimpIcons.Lyrics,
                        contentDescription = null,
                        tint = dynamicTextColor,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(dynamicTextColor.copy(alpha = 0.16f))
                        .clickable { actions.onShowQueue() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = SimpIcons.QueueMusic,
                        contentDescription = null,
                        tint = dynamicTextColor,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(26.dp))

        // Artwork: fills the leftover height, square, 36dp corners
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            ATStaticArtwork(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                cornerRadius = 36.dp,
                placeholder = dynamicTextColor.copy(alpha = 0.08f),
            )
        }

        Spacer(Modifier.height(26.dp))

        // Metadata row: title / artists + heart
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = state.screenData.nowPlayingTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    color = dynamicTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(),
                )
                Text(
                    text = state.screenData.artistName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = dynamicTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(),
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { actions.onUIEvent(UIEvent.ToggleLike) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (state.likeStatus) SimpIcons.Favorite else SimpIcons.FavoriteBorder,
                    contentDescription = null,
                    tint = if (state.likeStatus) dominantColor else dynamicTextColor,
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        Spacer(Modifier.height(26.dp))

        // Wavy progress + times
        WavySliderExpressive(
            value = { state.sliderValue / 100f },
            onValueChange = { fraction -> actions.onSliderChange(fraction * 100f) },
            onValueCommit = { actions.onSliderChangeFinished() },
            enabled = state.timelineState.total > 0L,
            activeTrackColor = dynamicTextColor,
            inactiveTrackColor = dynamicTextColor.copy(alpha = 0.24f),
            thumbColor = dynamicTextColor,
            isPlaying = state.controllerState.isPlaying,
            isVisible = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
        )
        PlayerCodecCapsule(
            state = state,
            modifier = Modifier.padding(top = 4.dp),
            containerColor = dynamicTextColor.copy(alpha = 0.10f),
            contentColor = dynamicTextColor.copy(alpha = 0.86f),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        ) {
            Text(
                text = makeTimeString(
                    (state.timelineState.total * (state.sliderValue / 100f)).roundToLong(),
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = dynamicTextColor,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            Text(
                text = makeTimeString(state.timelineState.total),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = dynamicTextColor,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }

        Spacer(Modifier.height(24.dp))

        // The real AT morphing transport
        V9AnimatedPlaybackControls(
            isPlayingProvider = { state.controllerState.isPlaying },
            onPrevious = { actions.onUIEvent(UIEvent.Previous) },
            onPlayPause = { actions.onUIEvent(UIEvent.PlayPause) },
            onNext = { actions.onUIEvent(UIEvent.Next) },
            height = 80.dp,
            releaseDelay = 220L,
            pressAnimationSpec = MotionScheme.standard().fastSpatialSpec(),
            colorOtherButtons = dynamicTextColor.copy(alpha = 0.08f),
            colorPlayPause = dynamicTextColor,
            tintPlayPauseIcon = if (
                dynamicTextColor.red + dynamicTextColor.green + dynamicTextColor.blue > 1.5f
            ) {
                Color.Black
            } else {
                Color.White
            },
            tintOtherIcons = dynamicTextColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )

        Spacer(Modifier.height(16.dp))
    }
}

// ---------------------------------------------------------------------------
// V10 — EDITORIAL
// ---------------------------------------------------------------------------

@Composable
fun NowPlayingContentEditorial(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    val seed = state.startColor.value
    // field = primary seed tone 30, accent = primary seed tone 90 (dark)
    val targetFieldColor = remember(seed) { seed.toHct().withTone(30.0).toColor() }
    val targetAccentColor = remember(seed) { seed.toHct().withTone(90.0).toColor() }
    val field by animateColorAsState(
        targetValue = targetFieldColor,
        animationSpec = tween(durationMillis = 800),
        label = "editorialField",
    )
    val accent by animateColorAsState(
        targetValue = targetAccentColor,
        animationSpec = tween(durationMillis = 800),
        label = "editorialAccent",
    )

    val songId = remember(state.screenData.nowPlayingTitle, state.screenData.artistName) {
        state.screenData.nowPlayingTitle + "|" + state.screenData.artistName
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EditorialCircleButton(
                onClick = actions.onDismiss,
                size = 44.dp,
                accent = accent,
                field = field,
            ) {
                Icon(
                    imageVector = SimpIcons.KeyboardArrowDown,
                    contentDescription = null,
                    tint = field,
                    modifier = Modifier.size(26.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EditorialCircleButton(
                    onClick = actions.onShowFullscreenLyrics,
                    size = 44.dp,
                    accent = accent,
                    field = field,
                ) {
                    Icon(
                        imageVector = SimpIcons.Lyrics,
                        contentDescription = null,
                        tint = field,
                        modifier = Modifier.size(22.dp),
                    )
                }
                EditorialCircleButton(
                    onClick = actions.onShowMoreSheet,
                    size = 44.dp,
                    accent = accent,
                    field = field,
                ) {
                    Icon(
                        imageVector = SimpIcons.MoreVert,
                        contentDescription = null,
                        tint = field,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        // Die-cut art
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            EditorialDieCutArt(
                artworkUrl = atUltraHigh(
                    state.artworkQueue.getOrNull(state.currentOrderIndex)?.thumbnails?.lastOrNull()?.url
                        ?: state.thumbnailURL,
                ),
                songId = songId,
                isPlaying = state.controllerState.isPlaying,
                accent = accent,
                field = field,
            )
        }

        // Headline: serif italic display type sized by title length
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        ) {
            val headlineBase = when {
                state.screenData.nowPlayingTitle.length <= 12 -> MaterialTheme.typography.displayLarge
                state.screenData.nowPlayingTitle.length <= 24 -> MaterialTheme.typography.displayMedium
                else -> MaterialTheme.typography.displaySmall
            }
            Text(
                text = state.screenData.nowPlayingTitle,
                style = headlineBase.copy(
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                ),
                color = accent,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.screenData.artistName.uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 2.sp,
                    color = accent.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = 2000,
                    ),
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Control cluster
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            // Row 1: play pill + next
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(accent)
                        .clickable { actions.onUIEvent(UIEvent.PlayPause) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (state.controllerState.isPlaying) "PAUSE" else "PLAY",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 3.sp,
                        color = field,
                    )
                }
                EditorialCircleButton(
                    onClick = { actions.onUIEvent(UIEvent.Next) },
                    size = 80.dp,
                    accent = accent,
                    field = field,
                ) {
                    Icon(
                        imageVector = SimpIcons.SkipNext,
                        contentDescription = null,
                        tint = field,
                        modifier = Modifier.size(34.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Row 2: previous + wavy progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EditorialCircleButton(
                    onClick = { actions.onUIEvent(UIEvent.Previous) },
                    size = 80.dp,
                    accent = accent,
                    field = field,
                ) {
                    Icon(
                        imageVector = SimpIcons.SkipPrevious,
                        contentDescription = null,
                        tint = field,
                        modifier = Modifier.size(34.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    val progressFraction = state.sliderValue / 100f
                    val animatedProgress by animateFloatAsState(
                        targetValue = progressFraction,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                        label = "EditorialProgress",
                    )
                    val lineStroke = Stroke(
                        width = with(androidx.compose.ui.platform.LocalDensity.current) { 4.dp.toPx() },
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                    Box(contentAlignment = Alignment.Center) {
                        LinearWavyProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp),
                            color = accent,
                            trackColor = accent.copy(alpha = 0.35f),
                            stroke = lineStroke,
                            trackStroke = lineStroke,
                            amplitude = { if (state.controllerState.isPlaying) 1f else 0f },
                        )
                        Slider(
                            value = state.sliderValue,
                            onValueChange = actions.onSliderChange,
                            onValueChangeFinished = actions.onSliderChangeFinished,
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.Transparent,
                                activeTrackColor = Color.Transparent,
                                inactiveTrackColor = Color.Transparent,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    PlayerCodecCapsule(
                        state = state,
                        modifier = Modifier.padding(top = 5.dp),
                        containerColor = accent.copy(alpha = 0.12f),
                        contentColor = accent.copy(alpha = 0.9f),
                    )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = atEditorialTime(
                                (state.timelineState.total * (state.sliderValue / 100f)).roundToLong(),
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = accent.copy(alpha = 0.8f),
                        )
                        Text(
                            text = atEditorialTime(state.timelineState.total),
                            style = MaterialTheme.typography.labelMedium,
                            color = accent.copy(alpha = 0.8f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Toggle row: like + add to playlist, swipe up opens the queue
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            if (dragAmount < -15) {
                                change.consume()
                                actions.onShowQueue()
                            }
                        }
                    },
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EditorialToggleButton(
                    checked = state.likeStatus,
                    onCheckedChange = { actions.onUIEvent(UIEvent.ToggleLike) },
                    accent = accent,
                    field = field,
                    icon = if (state.likeStatus) SimpIcons.Favorite else SimpIcons.FavoriteBorder,
                )
                EditorialToggleButton(
                    checked = false,
                    onCheckedChange = actions.onShowAddToPlaylist,
                    accent = accent,
                    field = field,
                    icon = SimpIcons.PlaylistAdd,
                )
            }
        }

        Spacer(
            Modifier
                .navigationBarsPadding()
                .height(6.dp),
        )
    }
}

private fun atEditorialTime(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

@Composable
private fun EditorialCircleButton(
    onClick: () -> Unit,
    size: Dp,
    accent: Color,
    field: Color,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(accent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun EditorialToggleButton(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    accent: Color,
    field: Color,
    icon: ImageVector,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (checked) accent else accent.copy(alpha = 0.08f))
            .clickable(onClick = onCheckedChange),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) field else accent,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** The die-cut artwork: shape chosen per song, morphing on song change. */
@Composable
private fun EditorialDieCutArt(
    artworkUrl: String?,
    songId: String,
    isPlaying: Boolean,
    accent: Color,
    field: Color,
) {
    val dieCuts = remember {
        listOf(
            MaterialShapes.Flower,
            MaterialShapes.Clover4Leaf,
            MaterialShapes.Puffy,
            MaterialShapes.Cookie12Sided,
            MaterialShapes.SoftBurst,
        )
    }
    val targetPolygon = remember(songId) {
        dieCuts[abs(songId.hashCode()) % dieCuts.size]
    }
    var morphFrom by remember { mutableStateOf(targetPolygon) }
    var morphTo by remember { mutableStateOf(targetPolygon) }
    val morphProgress = remember { Animatable(1f) }
    LaunchedEffect(targetPolygon) {
        if (targetPolygon !== morphTo) {
            morphFrom = morphTo
            morphTo = targetPolygon
            morphProgress.snapTo(0f)
            morphProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }
    }
    val morph = remember(morphFrom, morphTo) { Morph(morphFrom, morphTo) }
    val dieCutShape = remember(morph, morphProgress.value) {
        EditorialMorphShape(morph, morphProgress.value)
    }
    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.94f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "EditorialArtScale",
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (maxWidth < 50.dp || maxHeight < 50.dp) return@BoxWithConstraints
        val artSize = minOf(maxWidth, maxHeight) * 0.95f
        Box(
            modifier = Modifier
                .size(artSize)
                .graphicsLayer {
                    scaleX = artScale
                    scaleY = artScale
                }
                .clip(dieCutShape)
                .background(accent),
            contentAlignment = Alignment.Center,
        ) {
            if (artworkUrl != null) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(
                        com.muso.music.R.drawable.music_note,
                    ),
                    contentDescription = null,
                    tint = field,
                    modifier = Modifier.size(artSize * 0.3f),
                )
            }
        }
    }
}

internal class EditorialMorphShape(
    private val morph: Morph,
    private val progress: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: androidx.compose.ui.unit.Density,
    ): Outline {
        val path = morph.toPath(progress).asComposePath()
        val matrix = Matrix()
        val bounds = morph.calculateBounds()
        val boundsWidth = bounds[2] - bounds[0]
        val boundsHeight = bounds[3] - bounds[1]
        matrix.scale(size.width / boundsWidth, size.height / boundsHeight)
        matrix.translate(-bounds[0], -bounds[1])
        path.transform(matrix)
        return Outline.Generic(path)
    }
}
