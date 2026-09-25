package com.muso.music.ui.player

import android.content.res.Configuration
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.media3.common.C
import androidx.media3.common.Tracks
import androidx.media3.common.Format
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_READY
import androidx.navigation.NavController
import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.muso.music.constants.ShowLyricsKey
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.media3.common.Player.STATE_BUFFERING
import androidx.annotation.DrawableRes
import com.muso.music.LocalPlayerConnection
import com.muso.music.constants.HighQualityVideoKey
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibility
import com.muso.music.R
import com.muso.music.constants.DarkModeKey
import com.muso.music.constants.PlayerHorizontalPadding
import com.muso.music.constants.PlayerTextAlignmentKey
import com.muso.music.constants.GestureAnimationsKey
import com.muso.music.constants.AnimationsEnabledKey
import com.muso.music.constants.PlayerStyleKey
import com.muso.music.constants.PlayerStyle
import com.muso.music.constants.PureBlackKey
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.muso.music.constants.PlayerBackgroundStyle
import com.muso.music.constants.PlayerBackgroundStyleKey
import com.muso.music.constants.QueuePeekHeight
import com.muso.music.constants.ShowVideoInPlayerKey
import com.muso.music.constants.KeepScreenOnKey
import com.muso.music.constants.SliderStyle
import com.muso.music.constants.SliderStyleKey
import androidx.compose.runtime.CompositionLocalProvider
import com.muso.music.constants.PlayerButtonsStyle
import com.muso.music.constants.PlayerButtonsStyleKey
import com.muso.music.constants.ShowCodecOnPlayerKey
import com.muso.music.constants.HidePlayerSliderKey
import com.muso.music.extensions.togglePlayPause
import com.muso.music.extensions.toggleRepeatMode
import com.muso.music.models.MediaMetadata
import com.muso.music.ui.component.BottomSheet
import com.muso.music.ui.component.BottomSheetState
import com.muso.music.ui.component.rememberBottomSheetState
import com.muso.music.ui.screens.settings.DarkMode
import com.muso.music.ui.screens.settings.PlayerTextAlignment
import com.muso.music.utils.makeTimeString
import com.muso.music.utils.rememberEnumPreference
import com.muso.music.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import me.saket.squiggles.SquigglySlider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val useBlackBackground = remember(isSystemInDarkTheme, darkTheme, pureBlack) {
        val useDarkTheme = if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
        useDarkTheme && pureBlack
    }
    val backgroundColor = if (useBlackBackground && state.value > state.collapsedBound) {
        lerp(MaterialTheme.colorScheme.surfaceContainer, Color.Black, state.progress)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    val playerTextAlignment by rememberEnumPreference(PlayerTextAlignmentKey, PlayerTextAlignment.CENTER)
    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.DEFAULT)
    val hidePlayerSlider by rememberPreference(HidePlayerSliderKey, false)
    val showCodecOnPlayer by rememberPreference(ShowCodecOnPlayerKey, true)
    val (showVideo, onShowVideoChange) = rememberPreference(ShowVideoInPlayerKey, defaultValue = true)
    val playerBackgroundStyle by rememberEnumPreference(PlayerBackgroundStyleKey, defaultValue = PlayerBackgroundStyle.DEFAULT)
    val keepScreenOn by rememberPreference(KeepScreenOnKey, defaultValue = false)
    val highQualityVideo by rememberPreference(HighQualityVideoKey, defaultValue = true)
    val (showLyrics, onShowLyricsChange) = rememberPreference(ShowLyricsKey, defaultValue = false)

    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)

    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    var position by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.currentPosition)
    }
    var duration by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.duration)
    }
    var sliderPosition by remember {
        mutableStateOf<Long?>(null)
    }

    LaunchedEffect(playbackState) {
        if (playbackState == STATE_READY) {
            while (isActive) {
                delay(100)
                position = playerConnection.player.currentPosition
                duration = playerConnection.player.duration
            }
        }
    }

    val queueSheetState = rememberBottomSheetState(
        dismissedBound = QueuePeekHeight + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        expandedBound = state.expandedBound,
    )

    BottomSheet(
        state = state,
        modifier = modifier,
        backgroundColor = backgroundColor,
        onDismiss = {
            playerConnection.player.stop()
            playerConnection.player.clearMediaItems()
        },
        collapsedContent = {
            MiniPlayer(
                position = position,
                duration = duration,
            )
        }
    ) {
        // Full-screen video state, hoisted above the controls so the controls can colour
        // themselves for the video backdrop (white on scrim) instead of theme colours.
        val playerStyle by rememberEnumPreference(PlayerStyleKey, PlayerStyle.APPLE)
        val gestureAnimationsEnabled by rememberPreference(GestureAnimationsKey, true)
        val animationsEnabled by rememberPreference(AnimationsEnabledKey, true)

        // === Real audio codec detection (Echo Music port): the player's currently selected
        // audio track, observed through onTracksChanged. Nothing is faked when unavailable -
        // the pill simply stays empty and the UI reads clean.
        var currentAudioFormat by remember { mutableStateOf<Format?>(null) }
        DisposableEffect(playerConnection.player) {
            val playerToListen = playerConnection.player
            val listener = object : androidx.media3.common.Player.Listener {
                override fun onTracksChanged(tracks: Tracks) {
                    currentAudioFormat = tracks.groups.firstOrNull { it.type == C.TRACK_TYPE_AUDIO }
                        ?.getTrackFormat(0)
                }
            }
            playerToListen.addListener(listener)
            currentAudioFormat = playerToListen.currentTracks.groups
                .firstOrNull { it.type == C.TRACK_TYPE_AUDIO }
                ?.getTrackFormat(0)
            onDispose { playerToListen.removeListener(listener) }
        }
        val codecLabel = remember(currentAudioFormat) { formatAudioInfo(currentAudioFormat) }

        val videoEnabled = showVideo && !showLyrics && state.progress > 0.5f
        var videoActive by remember(mediaMetadata?.id) { mutableStateOf(false) }
        var controlsVisible by rememberSaveable { mutableStateOf(true) }

        val classicControls: @Composable ColumnScope.(MediaMetadata) -> Unit = { mediaMetadata ->
            // === SimpMusic "Apple Music" now playing (ported): bold title, lighter artists,
            // a thin pill progress bar that thickens while touched, big plain transport
            // glyphs with no containers, shuffle/repeat low at the sides, and a
            // Lyrics | Video | Queue dock. Over a playing video everything flips to
            // white on a gradient scrim, Apple-style.
            val onVideo = videoEnabled && videoActive
            val primaryText = if (onVideo) Color.White else MaterialTheme.colorScheme.onSurface
            val secondaryText = if (onVideo) Color.White.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant
            val accent = if (onVideo) Color.White else MaterialTheme.colorScheme.primary

            // Title row: title + artists, heart on the right.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding),
            ) {
                Column(
                    horizontalAlignment = when (playerTextAlignment) {
                        PlayerTextAlignment.SIDED -> Alignment.Start
                        PlayerTextAlignment.CENTER -> Alignment.CenterHorizontally
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = mediaMetadata.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee()
                            .clickable(enabled = mediaMetadata.album != null) {
                                navController.navigate("album/${mediaMetadata.album!!.id}")
                                state.collapseSoft()
                            },
                    )

                    Spacer(Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = when (playerTextAlignment) {
                            PlayerTextAlignment.SIDED -> Arrangement.Start
                            PlayerTextAlignment.CENTER -> Arrangement.Center
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        mediaMetadata.artists.fastForEachIndexed { index, artist ->
                            Text(
                                text = artist.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = secondaryText,
                                maxLines = 1,
                                modifier = Modifier
                                    .basicMarquee()
                                    .clickable(enabled = artist.id != null) {
                                        navController.navigate("artist/${artist.id}")
                                        state.collapseSoft()
                                    },
                            )

                            if (index != mediaMetadata.artists.lastIndex) {
                                Text(
                                    text = ", ",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = secondaryText,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                FilledIconToggleButton(
                    checked = currentSong?.song?.liked == true,
                    onCheckedChange = { playerConnection.toggleLike() },
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconToggleButtonColors(
                        containerColor = if (onVideo) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = secondaryText,
                        checkedContainerColor = if (onVideo) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primaryContainer,
                        checkedContentColor = if (onVideo) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        painter = painterResource(
                            if (currentSong?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border
                        ),
                        contentDescription = null,
                        tint = if (currentSong?.song?.liked == true) MaterialTheme.colorScheme.error else LocalContentColor.current,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Thin pill progress bar (Apple Music): 7dp at rest, 14dp while touched, no thumb.
            when {
                hidePlayerSlider -> {}
                sliderStyle == SliderStyle.SQUIGGLY -> {
                    SquigglySlider(
                        value = (sliderPosition ?: position).toFloat(),
                        valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                        onValueChange = { sliderPosition = it.toLong() },
                        onValueChangeFinished = {
                            sliderPosition?.let {
                                playerConnection.player.seekTo(it)
                                position = it
                            }
                            sliderPosition = null
                        },
                        squigglesSpec = SquigglySlider.SquigglesSpec(
                            amplitude = if (isPlaying) 2.dp else 0.dp,
                            strokeWidth = 4.dp,
                        ),
                        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                    )
                }

                else -> {
                    ThinProgressSlider(
                        position = position,
                        duration = if (duration == C.TIME_UNSET) 0L else duration,
                        accent = accent,
                        inactive = if (onVideo) Color.White.copy(alpha = 0.3f) else secondaryText.copy(alpha = 0.3f),
                        onValueChange = { sliderPosition = it },
                        onValueChangeFinished = {
                            sliderPosition?.let {
                                playerConnection.player.seekTo(it)
                                position = it
                            }
                            sliderPosition = null
                        },
                        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding + 4.dp)
            ) {
                Text(
                    text = makeTimeString(sliderPosition ?: position),
                    style = MaterialTheme.typography.labelMedium,
                    color = secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (showCodecOnPlayer && codecLabel.isNotEmpty()) {
                    Text(
                        text = codecLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = secondaryText.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }

                Text(
                    text = if (duration != C.TIME_UNSET) "-" + makeTimeString(duration - (sliderPosition ?: position).coerceAtMost(duration)) else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(20.dp))

            // === Transport: prev | play | next. Plain glyphs, no containers - a tight
            // centered cluster like Apple's own player. The play glyph is the biggest.
            Row(
                horizontalArrangement = Arrangement.spacedBy(52.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(
                    onClick = { if (canSkipPrevious) playerConnection.service.fadeSkip(false) },
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_previous),
                        contentDescription = null,
                        tint = primaryText.copy(alpha = if (canSkipPrevious) 1f else 0.35f),
                        modifier = Modifier.size(36.dp),
                    )
                }

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (playbackState == STATE_ENDED) {
                                playerConnection.player.seekTo(0, 0)
                                playerConnection.player.playWhenReady = true
                            } else if (playbackState != STATE_BUFFERING) {
                                playerConnection.player.togglePlayPause()
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (playbackState == STATE_BUFFERING) {
                        CircularProgressIndicator(
                            color = accent,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(30.dp),
                        )
                    } else {
                        Crossfade(
                            targetState = if (playbackState == STATE_ENDED) R.drawable.replay else if (isPlaying) R.drawable.pause else R.drawable.play,
                            animationSpec = tween(150),
                            label = "playPauseIcon",
                        ) { iconRes ->
                            Icon(
                                painter = painterResource(iconRes),
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(60.dp),
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { if (canSkipNext) playerConnection.service.fadeSkip(true) },
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = null,
                        tint = primaryText.copy(alpha = if (canSkipNext) 1f else 0.35f),
                        modifier = Modifier.size(36.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Shuffle / repeat, low at the sides (Apple Music keeps them out of the transport).
            val isShuffle by playerConnection.shuffleModeEnabled.collectAsState()
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding + 12.dp),
            ) {
                IconButton(
                    onClick = {
                        playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.shuffle),
                        contentDescription = null,
                        tint = if (isShuffle) accent else secondaryText.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp),
                    )
                }

                IconButton(onClick = playerConnection.player::toggleRepeatMode) {
                    Icon(
                        painter = painterResource(if (repeatMode == REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat),
                        contentDescription = null,
                        tint = if (repeatMode != REPEAT_MODE_OFF) accent else secondaryText.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // === Dock: Lyrics | Video | Queue (SimpMusic Apple Music dock) ===
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding + 16.dp),
            ) {
                PlayerDockButton(
                    iconRes = R.drawable.lyrics,
                    label = stringResource(R.string.lyrics),
                    active = showLyrics,
                    onVideo = onVideo,
                    onClick = { onShowLyricsChange(!showLyrics) },
                )

                PlayerDockButton(
                    iconRes = R.drawable.slow_motion_video,
                    label = stringResource(R.string.video),
                    active = showVideo,
                    onVideo = onVideo,
                    onClick = { onShowVideoChange(!showVideo) },
                )

                PlayerDockButton(
                    iconRes = R.drawable.queue_music,
                    label = stringResource(R.string.queue),
                    active = false,
                    onVideo = onVideo,
                    onClick = { queueSheetState.expandSoft() },
                )
            }
        }

        val expressiveControls: @Composable ColumnScope.(MediaMetadata) -> Unit = { mediaMetadata ->

            // === SimpMusic Material 3 Expressive player ===
            // Track info row: title + artists, with an expressive heart toggle on the right.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding),
            ) {
                Column(
                    horizontalAlignment = when (playerTextAlignment) {
                        PlayerTextAlignment.SIDED -> Alignment.Start
                        PlayerTextAlignment.CENTER -> Alignment.CenterHorizontally
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = mediaMetadata.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee()
                            .clickable(enabled = mediaMetadata.album != null) {
                                navController.navigate("album/${mediaMetadata.album!!.id}")
                                state.collapseSoft()
                            },
                    )

                    Spacer(Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = when (playerTextAlignment) {
                            PlayerTextAlignment.SIDED -> Arrangement.Start
                            PlayerTextAlignment.CENTER -> Arrangement.Center
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        mediaMetadata.artists.fastForEachIndexed { index, artist ->
                            Text(
                                text = artist.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                modifier = Modifier
                                    .basicMarquee()
                                    .clickable(enabled = artist.id != null) {
                                        navController.navigate("artist/${artist.id}")
                                        state.collapseSoft()
                                    },
                            )

                            if (index != mediaMetadata.artists.lastIndex) {
                                Text(
                                    text = ", ",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Heart — expressive filled toggle (M3E track info row).
                FilledIconToggleButton(
                    checked = currentSong?.song?.liked == true,
                    onCheckedChange = { playerConnection.toggleLike() },
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconToggleButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    modifier = Modifier.size(48.dp),
                ) {
                    Crossfade(targetState = currentSong?.song?.liked == true, label = "likeIcon") { liked ->
                        Icon(
                            painter = painterResource(
                                if (liked) R.drawable.favorite else R.drawable.favorite_border
                            ),
                            contentDescription = null,
                            tint = if (liked) MaterialTheme.colorScheme.error else LocalContentColor.current,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            when {
                hidePlayerSlider -> {}
                sliderStyle == SliderStyle.DEFAULT -> {
                    Slider(
                        value = (sliderPosition ?: position).toFloat(),
                        valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                        onValueChange = {
                            sliderPosition = it.toLong()
                        },
                        onValueChangeFinished = {
                            sliderPosition?.let {
                                playerConnection.player.seekTo(it)
                                position = it
                            }
                            sliderPosition = null
                        },
                        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding)
                    )
                }

                SliderStyle.SQUIGGLY -> {
                    SquigglySlider(
                        value = (sliderPosition ?: position).toFloat(),
                        valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                        onValueChange = {
                            sliderPosition = it.toLong()
                        },
                        onValueChangeFinished = {
                            sliderPosition?.let {
                                playerConnection.player.seekTo(it)
                                position = it
                            }
                            sliderPosition = null
                        },
                        squigglesSpec = SquigglySlider.SquigglesSpec(
                            amplitude = if (isPlaying) 2.dp else 0.dp,
                            strokeWidth = 4.dp,
                        ),
                        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding + 4.dp)
            ) {
                Text(
                    text = makeTimeString(sliderPosition ?: position),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(16.dp))

            // === Expressive transport row (SimpMusic port): prev | play | next pills.
            // The pressed pill grows x1.15 on a spring while its neighbours shrink
            // proportionally, and the play button's corner morphs 22dp (playing) <-> 34dp.
            val prevInteraction = remember { MutableInteractionSource() }
            val playInteraction = remember { MutableInteractionSource() }
            val nextInteraction = remember { MutableInteractionSource() }
            val prevPressed by prevInteraction.collectIsPressedAsState()
            val playPressed by playInteraction.collectIsPressedAsState()
            val nextPressed by nextInteraction.collectIsPressedAsState()

            val prevWeight by animateFloatAsState(
                targetValue = if (prevPressed) 0.55f * 1.15f else 0.55f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "prevWeight",
            )
            val playWeight by animateFloatAsState(
                targetValue = if (playPressed) 1.2f * 1.15f else 1.2f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "playWeight",
            )
            val nextWeight by animateFloatAsState(
                targetValue = if (nextPressed) 0.55f * 1.15f else 0.55f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "nextWeight",
            )

            val playCorner by animateDpAsState(
                targetValue = if (isPlaying) 22.dp else 34.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "playCorner",
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding)
                    .height(68.dp),
            ) {
                // Previous — full pill on secondaryContainer.
                Surface(
                    onClick = { if (canSkipPrevious) playerConnection.service.fadeSkip(false) },
                    shape = RoundedCornerShape(34.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    interactionSource = prevInteraction,
                    modifier = Modifier
                        .weight(prevWeight)
                        .fillMaxHeight(),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_previous),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                                alpha = if (canSkipPrevious) 1f else 0.4f,
                            ),
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }

                // Play / Pause — primary container, corner radius morphs with playback state.
                Surface(
                    onClick = {
                        if (playbackState == STATE_ENDED) {
                            playerConnection.player.seekTo(0, 0)
                            playerConnection.player.playWhenReady = true
                        } else if (playbackState != STATE_BUFFERING) {
                            playerConnection.player.togglePlayPause()
                        }
                    },
                    shape = RoundedCornerShape(playCorner),
                    color = MaterialTheme.colorScheme.primary,
                    interactionSource = playInteraction,
                    modifier = Modifier
                        .weight(playWeight)
                        .fillMaxHeight(),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (playbackState == STATE_BUFFERING) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(22.dp),
                            )
                        } else {
                            Crossfade(
                                targetState = if (playbackState == STATE_ENDED) R.drawable.replay else if (isPlaying) R.drawable.pause else R.drawable.play,
                                animationSpec = tween(150),
                                label = "playPauseIcon",
                                modifier = Modifier.fillMaxSize(),
                            ) { iconRes ->
                                Icon(
                                    painter = painterResource(iconRes),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(36.dp),
                                )
                            }
                        }
                    }
                }

                // Next — mirror of Previous.
                Surface(
                    onClick = { if (canSkipNext) playerConnection.service.fadeSkip(true) },
                    shape = RoundedCornerShape(34.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    interactionSource = nextInteraction,
                    modifier = Modifier
                        .weight(nextWeight)
                        .fillMaxHeight(),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_next),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                                alpha = if (canSkipNext) 1f else 0.4f,
                            ),
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // === Connected control group (SimpMusic M3E): shuffle | repeat | lyrics | video.
            // 48dp slots, 3dp gaps, rounded end caps; active slots fill primaryContainer.
            val isShuffle by playerConnection.shuffleModeEnabled.collectAsState()
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding)
                    .height(48.dp),
            ) {
                ExpressiveControlSlot(
                    active = isShuffle,
                    shape = RoundedCornerShape(
                        topStart = 24.dp, topEnd = 6.dp, bottomEnd = 6.dp, bottomStart = 24.dp
                    ),
                    iconRes = R.drawable.shuffle,
                    contentDescription = null,
                    onClick = {
                        playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                ExpressiveControlSlot(
                    active = repeatMode != REPEAT_MODE_OFF,
                    shape = RoundedCornerShape(6.dp),
                    iconRes = if (repeatMode == REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat,
                    contentDescription = null,
                    onClick = playerConnection.player::toggleRepeatMode,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                ExpressiveControlSlot(
                    active = showLyrics,
                    shape = RoundedCornerShape(6.dp),
                    iconRes = R.drawable.lyrics,
                    contentDescription = null,
                    onClick = { onShowLyricsChange(!showLyrics) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                ExpressiveControlSlot(
                    active = showVideo,
                    shape = RoundedCornerShape(
                        topStart = 6.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 6.dp
                    ),
                    iconRes = R.drawable.slow_motion_video,
                    contentDescription = null,
                    onClick = { onShowVideoChange(!showVideo) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }

        val immersiveControls: @Composable ColumnScope.(MediaMetadata) -> Unit = { mediaMetadata ->
            // === Immersive: large metadata, nothing else - no shuffle/repeat row, a minimal
            // Lyrics | Queue dock, and the biggest transport of any style. The artwork zone
            // above takes all the remaining vertical space, so the screen reads as art first.
            val onVideo = videoEnabled && videoActive
            val primaryText = if (onVideo) Color.White else MaterialTheme.colorScheme.onSurface
            val secondaryText = if (onVideo) Color.White.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant
            val accent = if (onVideo) Color.White else MaterialTheme.colorScheme.primary

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding),
            ) {
                Text(
                    text = mediaMetadata.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = primaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee()
                        .clickable(enabled = mediaMetadata.album != null) {
                            navController.navigate("album/${mediaMetadata.album!!.id}")
                            state.collapseSoft()
                        },
                )

                Spacer(Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    mediaMetadata.artists.fastForEachIndexed { index, artist ->
                        Text(
                            text = artist.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = secondaryText,
                            maxLines = 1,
                            modifier = Modifier
                                .basicMarquee()
                                .clickable(enabled = artist.id != null) {
                                    navController.navigate("artist/${artist.id}")
                                    state.collapseSoft()
                                },
                        )

                        if (index != mediaMetadata.artists.lastIndex) {
                            Text(
                                text = ", ",
                                style = MaterialTheme.typography.titleMedium,
                                color = secondaryText,
                            )
                        }
                    }
                }

                if (showCodecOnPlayer && codecLabel.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = codecLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = secondaryText.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            if (!hidePlayerSlider) {
                ThinProgressSlider(
                    position = position,
                    duration = if (duration == C.TIME_UNSET) 0L else duration,
                    accent = accent,
                    inactive = if (onVideo) Color.White.copy(alpha = 0.3f) else secondaryText.copy(alpha = 0.3f),
                    onValueChange = { sliderPosition = it },
                    onValueChangeFinished = {
                        sliderPosition?.let {
                            playerConnection.player.seekTo(it)
                            position = it
                        }
                        sliderPosition = null
                    },
                    modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding + 4.dp)
            ) {
                Text(
                    text = makeTimeString(sliderPosition ?: position),
                    style = MaterialTheme.typography.labelMedium,
                    color = secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = if (duration != C.TIME_UNSET) "-" + makeTimeString(duration - (sliderPosition ?: position).coerceAtMost(duration)) else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(28.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(56.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(
                    onClick = { if (canSkipPrevious) playerConnection.service.fadeSkip(false) },
                    modifier = Modifier.size(60.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_previous),
                        contentDescription = null,
                        tint = primaryText.copy(alpha = if (canSkipPrevious) 1f else 0.35f),
                        modifier = Modifier.size(40.dp),
                    )
                }

                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (playbackState == STATE_ENDED) {
                                playerConnection.player.seekTo(0, 0)
                                playerConnection.player.playWhenReady = true
                            } else if (playbackState != STATE_BUFFERING) {
                                playerConnection.player.togglePlayPause()
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (playbackState == STATE_BUFFERING) {
                        CircularProgressIndicator(
                            color = accent,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(30.dp),
                        )
                    } else {
                        Crossfade(
                            targetState = if (playbackState == STATE_ENDED) R.drawable.replay else if (isPlaying) R.drawable.pause else R.drawable.play,
                            animationSpec = tween(150),
                            label = "playPauseIcon",
                        ) { iconRes ->
                            Icon(
                                painter = painterResource(iconRes),
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(64.dp),
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { if (canSkipNext) playerConnection.service.fadeSkip(true) },
                    modifier = Modifier.size(60.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = null,
                        tint = primaryText.copy(alpha = if (canSkipNext) 1f else 0.35f),
                        modifier = Modifier.size(40.dp),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding + 16.dp),
            ) {
                PlayerDockButton(
                    iconRes = R.drawable.lyrics,
                    label = stringResource(R.string.lyrics),
                    active = showLyrics,
                    onVideo = onVideo,
                    onClick = { onShowLyricsChange(!showLyrics) },
                )

                PlayerDockButton(
                    iconRes = R.drawable.queue_music,
                    label = stringResource(R.string.queue),
                    active = false,
                    onVideo = onVideo,
                    onClick = { queueSheetState.expandSoft() },
                )
            }
        }

        val appleControls: @Composable ColumnScope.(MediaMetadata) -> Unit = { mediaMetadata ->
            // === Apple-inspired: centered metadata with no heart in the title row -
            // heart, shuffle and repeat sit together in one quiet row under the times,
            // like Apple Music's compact cluster. Everything else matches Muso Classic.
            val onVideo = videoEnabled && videoActive
            val primaryText = if (onVideo) Color.White else MaterialTheme.colorScheme.onSurface
            val secondaryText = if (onVideo) Color.White.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant
            val accent = if (onVideo) Color.White else MaterialTheme.colorScheme.primary

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding),
            ) {
                Text(
                    text = mediaMetadata.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee()
                        .clickable(enabled = mediaMetadata.album != null) {
                            navController.navigate("album/${mediaMetadata.album!!.id}")
                            state.collapseSoft()
                        },
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    mediaMetadata.artists.fastForEachIndexed { index, artist ->
                        Text(
                            text = artist.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = secondaryText,
                            maxLines = 1,
                            modifier = Modifier
                                .basicMarquee()
                                .clickable(enabled = artist.id != null) {
                                    navController.navigate("artist/${artist.id}")
                                    state.collapseSoft()
                                },
                        )

                        if (index != mediaMetadata.artists.lastIndex) {
                            Text(
                                text = ", ",
                                style = MaterialTheme.typography.titleMedium,
                                color = secondaryText,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            when {
                hidePlayerSlider -> {}
                sliderStyle == SliderStyle.SQUIGGLY -> {
                    SquigglySlider(
                        value = (sliderPosition ?: position).toFloat(),
                        valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                        onValueChange = { sliderPosition = it.toLong() },
                        onValueChangeFinished = {
                            sliderPosition?.let {
                                playerConnection.player.seekTo(it)
                                position = it
                            }
                            sliderPosition = null
                        },
                        squigglesSpec = SquigglySlider.SquigglesSpec(
                            amplitude = if (isPlaying) 2.dp else 0.dp,
                            strokeWidth = 4.dp,
                        ),
                        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                    )
                }

                else -> {
                    ThinProgressSlider(
                        position = position,
                        duration = if (duration == C.TIME_UNSET) 0L else duration,
                        accent = accent,
                        inactive = if (onVideo) Color.White.copy(alpha = 0.3f) else secondaryText.copy(alpha = 0.3f),
                        onValueChange = { sliderPosition = it },
                        onValueChangeFinished = {
                            sliderPosition?.let {
                                playerConnection.player.seekTo(it)
                                position = it
                            }
                            sliderPosition = null
                        },
                        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding + 4.dp)
            ) {
                Text(
                    text = makeTimeString(sliderPosition ?: position),
                    style = MaterialTheme.typography.labelMedium,
                    color = secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (showCodecOnPlayer && codecLabel.isNotEmpty()) {
                    Text(
                        text = codecLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = secondaryText.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }

                Text(
                    text = if (duration != C.TIME_UNSET) "-" + makeTimeString(duration - (sliderPosition ?: position).coerceAtMost(duration)) else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(14.dp))

            val isShuffle by playerConnection.shuffleModeEnabled.collectAsState()
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                FilledIconToggleButton(
                    checked = currentSong?.song?.liked == true,
                    onCheckedChange = { playerConnection.toggleLike() },
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconToggleButtonColors(
                        containerColor = if (onVideo) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = secondaryText,
                        checkedContainerColor = if (onVideo) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primaryContainer,
                        checkedContentColor = if (onVideo) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        painter = painterResource(
                            if (currentSong?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border
                        ),
                        contentDescription = null,
                        tint = if (currentSong?.song?.liked == true) MaterialTheme.colorScheme.error else LocalContentColor.current,
                        modifier = Modifier.size(22.dp),
                    )
                }

                IconButton(
                    onClick = {
                        playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.shuffle),
                        contentDescription = null,
                        tint = if (isShuffle) accent else secondaryText.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp),
                    )
                }

                IconButton(onClick = playerConnection.player::toggleRepeatMode) {
                    Icon(
                        painter = painterResource(if (repeatMode == REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat),
                        contentDescription = null,
                        tint = if (repeatMode != REPEAT_MODE_OFF) accent else secondaryText.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(52.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(
                    onClick = { if (canSkipPrevious) playerConnection.service.fadeSkip(false) },
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_previous),
                        contentDescription = null,
                        tint = primaryText.copy(alpha = if (canSkipPrevious) 1f else 0.35f),
                        modifier = Modifier.size(36.dp),
                    )
                }

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (playbackState == STATE_ENDED) {
                                playerConnection.player.seekTo(0, 0)
                                playerConnection.player.playWhenReady = true
                            } else if (playbackState != STATE_BUFFERING) {
                                playerConnection.player.togglePlayPause()
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (playbackState == STATE_BUFFERING) {
                        CircularProgressIndicator(
                            color = accent,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(30.dp),
                        )
                    } else {
                        Crossfade(
                            targetState = if (playbackState == STATE_ENDED) R.drawable.replay else if (isPlaying) R.drawable.pause else R.drawable.play,
                            animationSpec = tween(150),
                            label = "playPauseIcon",
                        ) { iconRes ->
                            Icon(
                                painter = painterResource(iconRes),
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(60.dp),
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { if (canSkipNext) playerConnection.service.fadeSkip(true) },
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = null,
                        tint = primaryText.copy(alpha = if (canSkipNext) 1f else 0.35f),
                        modifier = Modifier.size(36.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding + 16.dp),
            ) {
                PlayerDockButton(
                    iconRes = R.drawable.lyrics,
                    label = stringResource(R.string.lyrics),
                    active = showLyrics,
                    onVideo = onVideo,
                    onClick = { onShowLyricsChange(!showLyrics) },
                )

                PlayerDockButton(
                    iconRes = R.drawable.slow_motion_video,
                    label = stringResource(R.string.video),
                    active = showVideo,
                    onVideo = onVideo,
                    onClick = { onShowVideoChange(!showVideo) },
                )

                PlayerDockButton(
                    iconRes = R.drawable.queue_music,
                    label = stringResource(R.string.queue),
                    active = false,
                    onVideo = onVideo,
                    onClick = { queueSheetState.expandSoft() },
                )
            }
        }

        val controlsContent: @Composable ColumnScope.(MediaMetadata) -> Unit = when (playerStyle) {
            PlayerStyle.CLASSIC -> classicControls
            PlayerStyle.EXPRESSIVE -> expressiveControls
            PlayerStyle.IMMERSIVE -> immersiveControls
            PlayerStyle.APPLE -> appleControls
        }

        // Player background style: the current artwork, heavily blurred, behind the whole
        // player (liquid-glass look). The video background takes priority when it is showing.
        if (playerBackgroundStyle == PlayerBackgroundStyle.BLURRED_ARTWORK &&
            !(showVideo && !showLyrics && state.progress > 0.5f)
        ) {
            mediaMetadata?.thumbnailUrl?.let { thumbnailUrl ->
                Box(Modifier.matchParentSize()) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .blur(64.dp),
                    )
                    // Scrim on top keeps the controls readable; it leans on the player's own
                    // background color so it matches both the light and dark/pure-black themes.
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(
                                backgroundColor.copy(
                                    alpha = if (useBlackBackground) 0.72f else 0.86f
                                )
                            )
                    )
                }
            }
        }

        if (videoEnabled) {
            PlayerVideo(
                videoId = mediaMetadata?.id,
                isPlaying = isPlaying,
                positionProvider = { playerConnection.player.currentPosition },
                videoQualityHeight = if (highQualityVideo) 720 else 360,
                modifier = Modifier.matchParentSize(),
                onVideoAvailable = { videoActive = it }
            )
        }

        // Immersive mode: while the full-screen video plays, the status bar always hides and
        // the navigation bar hides once the controls are toggled away. Everything comes back
        // when the sheet collapses or the player leaves composition.
        val immersiveView = LocalView.current
        LaunchedEffect(state.isExpanded, videoEnabled, videoActive, controlsVisible, keepScreenOn) {
            val window = (immersiveView.context as? Activity)?.window ?: return@LaunchedEffect
            // Echo-Music "keep screen on when player is expanded" — keeps the screen awake
            // whenever the player sheet is open (lyrics reading, video watching).
            if (keepScreenOn && state.isExpanded) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            val insetsController = WindowCompat.getInsetsController(window, immersiveView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (state.isExpanded && videoEnabled && videoActive) {
                if (controlsVisible) {
                    insetsController.show(WindowInsetsCompat.Type.navigationBars())
                } else {
                    insetsController.hide(WindowInsetsCompat.Type.navigationBars())
                }
                insetsController.hide(WindowInsetsCompat.Type.statusBars())
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        DisposableEffect(immersiveView) {
            onDispose {
                val window = (immersiveView.context as? Activity)?.window ?: return@onDispose
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                WindowCompat.getInsetsController(window, immersiveView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }

        when (LocalConfiguration.current.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                Row(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                        .padding(bottom = queueSheetState.collapsedBound)
                ) {
                    if (videoEnabled && videoActive) {
                        // The full-screen video renders behind everything; this region only
                        // catches taps to show/hide the controls.
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectTapGestures {
                                        controlsVisible = !controlsVisible
                                    }
                                }
                        )
                    } else {
                        // === Echo Nightly-style gesture: swipe the artwork left/right to
                        // skip, with the artwork tracking the finger (velocity comes free with
                        // the fling: release past a quarter of the width triggers the skip).
                        var swipeOffset by remember { mutableStateOf(0f) }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .pointerInput(gestureAnimationsEnabled) {
                                    if (!gestureAnimationsEnabled) {
                                        return@pointerInput
                                    }
                                    detectHorizontalDragGestures(
                                        onDragEnd = {
                                            val threshold = size.width / 4f
                                            when {
                                                swipeOffset < -threshold -> playerConnection.service.fadeSkip(true)
                                                swipeOffset > threshold -> playerConnection.service.fadeSkip(false)
                                            }
                                            swipeOffset = 0f
                                        },
                                        onDragCancel = { swipeOffset = 0f },
                                    ) { change, dragAmount ->
                                        change.consume()
                                        swipeOffset += dragAmount
                                    }
                                }
                        ) {
                            Thumbnail(
                                sliderPositionProvider = { sliderPosition },
                                modifier = Modifier
                                    .nestedScroll(state.preUpPostDownNestedScrollConnection)
                                    .graphicsLayer {
                                        translationX = swipeOffset.coerceIn(-size.width.toFloat(), size.width.toFloat()) * 0.55f
                                    }
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                    ) {
                        Spacer(Modifier.weight(1f))

                        PlayerControls(
                            videoActive = videoEnabled && videoActive,
                            controlsVisible = controlsVisible,
                        ) {
                            mediaMetadata?.let {
                                controlsContent(it)
                            }
                        }

                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            else -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                        .padding(bottom = queueSheetState.collapsedBound)
                ) {
                    if (videoEnabled && videoActive) {
                        // The full-screen video renders behind everything; this region only
                        // catches taps to show/hide the controls.
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectTapGestures {
                                        controlsVisible = !controlsVisible
                                    }
                                }
                        )
                    } else {
                        // === Echo Nightly-style gesture: swipe the artwork left/right to
                        // skip, with the artwork tracking the finger (velocity comes free with
                        // the fling: release past a quarter of the width triggers the skip).
                        var swipeOffset by remember { mutableStateOf(0f) }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .pointerInput(gestureAnimationsEnabled) {
                                    if (!gestureAnimationsEnabled) {
                                        return@pointerInput
                                    }
                                    detectHorizontalDragGestures(
                                        onDragEnd = {
                                            val threshold = size.width / 4f
                                            when {
                                                swipeOffset < -threshold -> playerConnection.service.fadeSkip(true)
                                                swipeOffset > threshold -> playerConnection.service.fadeSkip(false)
                                            }
                                            swipeOffset = 0f
                                        },
                                        onDragCancel = { swipeOffset = 0f },
                                    ) { change, dragAmount ->
                                        change.consume()
                                        swipeOffset += dragAmount
                                    }
                                }
                        ) {
                            Thumbnail(
                                sliderPositionProvider = { sliderPosition },
                                modifier = Modifier
                                    .nestedScroll(state.preUpPostDownNestedScrollConnection)
                                    .graphicsLayer {
                                        translationX = swipeOffset.coerceIn(-size.width.toFloat(), size.width.toFloat()) * 0.55f
                                    }
                            )
                        }
                    }

                    PlayerControls(
                        videoActive = videoEnabled && videoActive,
                        controlsVisible = controlsVisible,
                    ) {
                        mediaMetadata?.let {
                            controlsContent(it)
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        Queue(
            state = queueSheetState,
            playerBottomSheetState = state,
            backgroundColor = backgroundColor,
            navController = navController
        )
    }
}

/**
 * The fixed controls block (title row, slider, transport, dock) shared by the portrait and
 * landscape layouts. While a full-screen video plays it never leaves composition: the
 * controls simply fade out with a graphicsLayer alpha and a gradient scrim rides the same
 * alpha, so a tap toggling them animates instantly instead of rebuilding the whole cluster.
 */
@Composable
private fun PlayerControls(
    videoActive: Boolean,
    controlsVisible: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Animation settings: an instant cut replaces the fades when animations are off.
    val animationsEnabled by rememberPreference(AnimationsEnabledKey, true)
    val controlsAlpha by animateFloatAsState(
        targetValue = if (videoActive && !controlsVisible) 0f else 1f,
        animationSpec = tween(
            durationMillis = if (!animationsEnabled) 0 else if (controlsVisible) 180 else 500,
            easing = androidx.compose.animation.core.LinearEasing,
        ),
        label = "playerControlsAlpha",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = controlsAlpha }
    ) {
        if (videoActive) {
            // Apple-style scrim so white controls stay readable over the picture.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.25f to Color.Black.copy(alpha = 0.30f),
                            1f to Color.Black.copy(alpha = 0.82f),
                        )
                    )
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Player buttons style (Echo appearance): recolor the control icons through
            // LocalContentColor so every player style picks it up in one place.
            val playerButtonsStyle by rememberEnumPreference(PlayerButtonsStyleKey, PlayerButtonsStyle.DEFAULT)
            val buttonsTint = when (playerButtonsStyle) {
                PlayerButtonsStyle.DEFAULT -> LocalContentColor.current
                PlayerButtonsStyle.PRIMARY -> MaterialTheme.colorScheme.primary
                PlayerButtonsStyle.TERTIARY -> MaterialTheme.colorScheme.tertiary
            }
            CompositionLocalProvider(LocalContentColor provides buttonsTint) {
                content()
            }
        }
    }
}

/**
 * One pill of the Lyrics | Video | Queue dock: an outlined chip that fills with the
 * theme primary (white over video) while active.
 */
@Composable
private fun PlayerDockButton(
    @DrawableRes iconRes: Int,
    label: String,
    active: Boolean,
    onVideo: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (active) {
            if (onVideo) Color.White.copy(alpha = 0.22f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(200),
        label = "dockContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (active) {
            if (onVideo) Color.White else MaterialTheme.colorScheme.primary
        } else {
            if (onVideo) Color.White.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "dockContent",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
        )
    }
}

/**
 * Apple Music-style thin progress pill: 7dp tall at rest, 14dp while touched, no thumb,
 * both ends equally round. Tap anywhere on the bar to seek, or drag to scrub.
 */
@Composable
private fun ThinProgressSlider(
    position: Long,
    duration: Long,
    accent: Color,
    inactive: Color,
    onValueChange: (Long) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeDuration = if (duration <= 0L) 1L else duration
    var scrubFraction by remember { mutableStateOf<Float?>(null) }
    val fraction = scrubFraction ?: (position.toFloat() / safeDuration).coerceIn(0f, 1f)

    // The track thickens while touched - same spring feel as Apple's bar.
    var pressedNow by remember { mutableStateOf(false) }
    val trackHeight by animateDpAsState(
        targetValue = if (pressedNow) 14.dp else 7.dp,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "thinSliderHeight",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInput(safeDuration) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        pressedNow = true
                        scrubFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onValueChange((scrubFraction!! * safeDuration).toLong())
                    },
                    onHorizontalDrag = { change, _ ->
                        scrubFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        onValueChange((scrubFraction!! * safeDuration).toLong())
                    },
                    onDragEnd = {
                        scrubFraction = null
                        pressedNow = false
                        onValueChangeFinished()
                    },
                    onDragCancel = {
                        scrubFraction = null
                        pressedNow = false
                        onValueChangeFinished()
                    },
                )
            }
            .pointerInput(safeDuration) {
                detectTapGestures { offset ->
                    val f = (offset.x / size.width).coerceIn(0f, 1f)
                    onValueChange((f * safeDuration).toLong())
                    onValueChangeFinished()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(RoundedCornerShape(50))
                .background(inactive)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(50))
                    .background(accent)
            )
        }
    }
}

@Composable
private fun ExpressiveControlSlot(
    active: Boolean,
    shape: Shape,
    @DrawableRes iconRes: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (active) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(200),
        label = "slotContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (active) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "slotContent",
    )
    Surface(
        onClick = onClick,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * Human-readable audio info for the player, from the player's real selected track:
 * codec name (AAC, Opus, FLAC...) and average bitrate when the format reports one.
 * Returns an empty string when nothing is known - the UI then hides the pill rather
 * than showing a made-up value.
 */
private fun formatAudioInfo(format: Format?): String {
    if (format == null) return ""
    val codecName = when (format.sampleMimeType) {
        "audio/mp4a-latm", "audio/mp4a" -> "AAC"
        "audio/opus" -> "Opus"
        "audio/mpeg" -> "MP3"
        "audio/flac" -> "FLAC"
        "audio/alac" -> "ALAC"
        "audio/vorbis" -> "Vorbis"
        "audio/raw", "audio/l16", "audio/pcm" -> "PCM"
        else -> format.sampleMimeType?.substringAfter("audio/")?.uppercase()
    } ?: return ""
    val parts = mutableListOf(codecName)
    if (format.bitrate > 0) {
        parts += "${format.bitrate / 1000} kbps"
    }
    return parts.joinToString(" \u2022 ")
}
