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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.media3.common.C
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.muso.music.constants.ShowLyricsKey
import androidx.compose.animation.Crossfade
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
        val controlsContent: @Composable ColumnScope.(MediaMetadata) -> Unit = { mediaMetadata ->
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

            when (sliderStyle) {
                SliderStyle.DEFAULT -> {
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
                    onClick = { if (canSkipPrevious) playerConnection.player.seekToPrevious() },
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
                    onClick = { if (canSkipNext) playerConnection.player.seekToNext() },
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

        // Full-screen video background (SimpMusic-style): the video renders behind the whole
        // player; tapping the video area toggles all controls.
        val videoEnabled = showVideo && !showLyrics && state.progress > 0.5f
        var videoActive by remember(mediaMetadata?.id) { mutableStateOf(false) }
        var controlsVisible by rememberSaveable { mutableStateOf(true) }

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
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.weight(1f)
                        ) {
                            Thumbnail(
                                sliderPositionProvider = { sliderPosition },
                                modifier = Modifier.nestedScroll(state.preUpPostDownNestedScrollConnection)
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

                        if (videoEnabled && videoActive) {
                            AnimatedVisibility(
                                visible = controlsVisible,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    mediaMetadata?.let {
                                        controlsContent(it)
                                    }
                                }
                            }
                        } else {
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
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.weight(1f)
                        ) {
                            Thumbnail(
                                sliderPositionProvider = { sliderPosition },
                                modifier = Modifier.nestedScroll(state.preUpPostDownNestedScrollConnection)
                            )
                        }
                    }

                    if (videoEnabled && videoActive) {
                        AnimatedVisibility(
                            visible = controlsVisible,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                mediaMetadata?.let {
                                    controlsContent(it)
                                }
                            }
                        }
                    } else {
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
 * One slot of the expressive connected control group (SimpMusic M3E port): a tonal
 * surface that animates to primaryContainer when active.
 */
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
