package com.muso.music.ui.player

import android.content.res.Configuration
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import android.graphics.drawable.BitmapDrawable
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
import androidx.palette.graphics.Palette
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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
import com.muso.music.LocalDatabase
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SliderDefaults
import androidx.media3.common.VideoSize
import android.content.pm.ActivityInfo
import android.app.Activity
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs
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
import androidx.compose.ui.layout.onSizeChanged
import coil.compose.AsyncImage
import com.muso.music.constants.PlayerBackgroundStyle
import com.muso.music.constants.PlayerBackgroundStyleKey
import com.muso.music.constants.QueuePeekHeight
import com.muso.music.constants.ShowVideoInPlayerKey
import com.muso.music.constants.KeepScreenOnKey
import com.muso.music.constants.SliderStyle
import com.muso.music.constants.SliderStyleKey
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import com.muso.music.constants.PlayerButtonsStyle
import com.muso.music.constants.PlayerButtonsStyleKey
import com.muso.music.constants.ShowCodecOnPlayerKey
import com.muso.music.constants.HidePlayerSliderKey
import com.muso.music.extensions.togglePlayPause
import com.muso.music.extensions.toggleRepeatMode
import com.muso.music.models.MediaMetadata
import com.muso.music.lyrics.LyricsUtils.findCurrentLineIndex
import com.muso.music.ui.menu.AddToPlaylistDialog
import com.muso.music.ui.component.BottomSheet
import com.muso.music.ui.component.BottomSheetState
import com.muso.music.extensions.metadata
import com.muso.music.ui.component.Lyrics
import com.muso.music.ui.component.rememberBottomSheetState
import com.muso.music.ui.screens.settings.DarkMode
import com.muso.music.ui.screens.settings.PlayerTextAlignment
import com.muso.music.utils.makeTimeString
import com.muso.music.utils.rememberEnumPreference
import com.muso.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlinx.coroutines.isActive
import me.saket.squiggles.SquigglySlider
import androidx.compose.ui.platform.LocalContext
import coil.imageLoader
import coil.request.ImageRequest
import com.muso.music.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.muso.music.lyrics.LyricsEntry
import com.muso.music.lyrics.LyricsEntry.Companion.HEAD_LYRICS_ENTRY
import com.muso.music.lyrics.LyricsUtils.parseLyrics
import echo.music.iad1tya.betterlyrics.TTMLParser
import com.muso.music.ui.player.SimpExpressiveContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableIntStateOf
import androidx.media3.common.Timeline
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.models.response.PlayerResponse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import android.content.Context
import android.media.AudioManager
import android.database.ContentObserver
import android.provider.Settings
import android.os.Handler
import android.os.Looper

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
    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.SQUIGGLY)
    val hidePlayerSlider by rememberPreference(HidePlayerSliderKey, false)
    val showCodecOnPlayer by rememberPreference(ShowCodecOnPlayerKey, true)
    val (showVideo, onShowVideoChange) = rememberPreference(ShowVideoInPlayerKey, defaultValue = true)
    val playerBackgroundStyle by rememberEnumPreference(PlayerBackgroundStyleKey, defaultValue = PlayerBackgroundStyle.DEFAULT)
    val keepScreenOn by rememberPreference(KeepScreenOnKey, defaultValue = false)
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
        val playerStyle by rememberEnumPreference(PlayerStyleKey, PlayerStyle.EXPRESSIVE)
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

        var showSongInfoDialog by rememberSaveable { mutableStateOf(false) }
        var showAddToPlaylistDialog by rememberSaveable { mutableStateOf(false) }
        val database = LocalDatabase.current

        // === SimpMusic artwork pager ============================================
        // One HorizontalPager across the queue's covers, two-way synced with the
        // player: a real user swipe that settles changes the song, and a song
        // change settles the pager (the slide IS the track-change feedback).
        val pagerQueueWindows by playerConnection.queueWindows.collectAsState()
        val currentWindowIndex by playerConnection.currentWindowIndex.collectAsState()
        val pagerState = rememberPagerState(
            // PlayerConnection's currentWindowIndex starts at -1; a negative
            // initialPage crashes the pager's first draw (Index -1, size N).
            initialPage = currentWindowIndex.coerceAtLeast(0),
            pageCount = { pagerQueueWindows.size },
        )
        var pagerUserSwipe by remember { mutableStateOf(false) }
        var pagerProgrammatic by remember { mutableStateOf(false) }
        // Player -> pager: single-page moves animate (the slide is the feedback),
        // multi-page jumps cut (radio queue trims must not fling through covers).
        LaunchedEffect(currentWindowIndex, pagerQueueWindows.size) {
            if (pagerQueueWindows.isEmpty()) return@LaunchedEffect
            val target = currentWindowIndex.coerceIn(0, pagerQueueWindows.size - 1)
            if (pagerState.settledPage != target) {
                pagerProgrammatic = true
                try {
                    if (abs(pagerState.settledPage - target) <= 1) {
                        pagerState.animateScrollToPage(target)
                    } else {
                        pagerState.scrollToPage(target)
                    }
                } finally {
                    pagerProgrammatic = false
                }
            }
        }
        // A real user swipe (not the programmatic settle above) arms the latch.
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.isScrollInProgress }
                .distinctUntilChanged()
                .collect { dragging -> if (dragging && !pagerProgrammatic) pagerUserSwipe = true }
        }
        // Pager -> player: a settled page from a user swipe changes the song.
        LaunchedEffect(pagerState, currentWindowIndex) {
            snapshotFlow { pagerState.settledPage }
                .distinctUntilChanged()
                .collect { page ->
                    if (pagerUserSwipe) {
                        pagerUserSwipe = false
                        when {
                            page == currentWindowIndex + 1 -> playerConnection.service.fadeSkip(true)
                            page == currentWindowIndex - 1 -> playerConnection.service.fadeSkip(false)
                            page != currentWindowIndex -> playerConnection.player.seekTo(page, 0)
                        }
                    }
                }
        }

        // The stream's real aspect ratio: every style sizes its video frame from
        // this ONE value (SimpMusic), so frame and layout can never drift.
        var videoAspectRatio by remember { mutableFloatStateOf(16f / 9f) }
        DisposableEffect(playerConnection.player) {
            val playerToListen = playerConnection.player
            val listener = object : androidx.media3.common.Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        videoAspectRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
                    }
                }
            }
            playerToListen.addListener(listener)
            onDispose { playerToListen.removeListener(listener) }
        }

        // Landscape fullscreen video route (SimpMusic FullscreenPlayer), entered
        // from the fullscreen button in the over-video overlay.
        var fullscreenVideo by rememberSaveable { mutableStateOf(false) }
        // Over-video subtitle (SimpMusic): the current lyric line over the video.
        var videoSubtitles by rememberSaveable { mutableStateOf(false) }
        // Apple Music tabbed bodies (SimpMusic): the dock switches the artwork
        // area between the LYRICS and QUEUE bodies; re-tapping returns to MAIN.
        var appleView by rememberSaveable { mutableStateOf(AppleMusicView.MAIN) }

        val classicControls: @Composable ColumnScope.(MediaMetadata) -> Unit = { mediaMetadata ->
            // === SimpMusic "Classic / Spotify" now playing, ported: force-dark,
            // white text, a buffered indicator under the slider, ONE transport
            // row with shuffle and repeat integrated (SpaceEvenly, five slots),
            // and an info / add-to-playlist / queue action row.
            val fg = Color.White
            val fgDim = Color.White.copy(alpha = 0.72f)
            val isShuffle by playerConnection.shuffleModeEnabled.collectAsState()

            // Track info row: marquee title + artists, heart on the right.
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
                        color = fg,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee()
                            .clickable(enabled = mediaMetadata.album != null) {
                                navController.navigate("album/${mediaMetadata.album!!.id}")
                                state.collapseSoft()
                            },
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        mediaMetadata.artists.fastForEachIndexed { index, artist ->
                            Text(
                                text = artist.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = fgDim,
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
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = fgDim,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                IconButton(
                    onClick = { playerConnection.toggleLike() },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        painter = painterResource(
                            if (currentSong?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border
                        ),
                        contentDescription = null,
                        tint = if (currentSong?.song?.liked == true) MaterialTheme.colorScheme.error else fg,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Seek: buffered progress under the slider (SimpMusic Classic).
            if (!hidePlayerSlider) {
                if (sliderStyle == SliderStyle.SQUIGGLY) {
                    SquigglyPositionSlider(
                        positionProvider = { position },
                        sliderPosition = sliderPosition,
                        duration = duration,
                        isPlaying = isPlaying,
                        onValueChange = { sliderPosition = it.toLong() },
                        onScrubEnd = {
                            sliderPosition?.let {
                                playerConnection.player.seekTo(it)
                                position = it
                            }
                            sliderPosition = null
                        },
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    ) {
                        LinearProgressIndicator(
                            progress = { playerConnection.player.bufferedPercentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = fgDim.copy(alpha = 0.6f),
                            trackColor = fgDim.copy(alpha = 0.2f),
                        )
                        Slider(
                            value = if (duration == C.TIME_UNSET || duration <= 0) 0f
                            else ((sliderPosition ?: position).toFloat() / duration).coerceIn(0f, 1f),
                            onValueChange = { fraction ->
                                if (duration != C.TIME_UNSET && duration > 0) {
                                    sliderPosition = (fraction * duration).toLong()
                                }
                            },
                            onValueChangeFinished = {
                                sliderPosition?.let {
                                    playerConnection.player.seekTo(it)
                                    position = it
                                }
                                sliderPosition = null
                            },
                            colors = SliderDefaults.colors(
                                activeTrackColor = Color.White,
                                inactiveTrackColor = fgDim.copy(alpha = 0.2f),
                                thumbColor = Color.White,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // Times row: elapsed | codec pill | -remaining.
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            ) {
                PositionTimeText(
                    positionProvider = { position },
                    sliderPosition = sliderPosition,
                    makeText = { makeTimeString(it) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = fgDim,
                )
                if (showCodecOnPlayer && codecLabel.isNotEmpty()) {
                    Text(
                        text = codecLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = fgDim.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
                PositionTimeText(
                    positionProvider = { position },
                    sliderPosition = sliderPosition,
                    makeText = {
                        if (duration != C.TIME_UNSET) "-" + makeTimeString((duration - it).coerceAtMost(duration)) else ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = fgDim,
                )
            }

            Spacer(Modifier.height(6.dp))

            // Transport: shuffle | previous | play | next | repeat (one row).
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
            ) {
                IconButton(
                    onClick = {
                        playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled
                    },
                    modifier = Modifier.size(42.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.shuffle),
                        contentDescription = null,
                        tint = if (isShuffle) MaterialTheme.colorScheme.primary else fgDim.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp),
                    )
                }
                IconButton(
                    onClick = { if (canSkipPrevious) playerConnection.service.fadeSkip(false) },
                    modifier = Modifier.size(52.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_previous),
                        contentDescription = null,
                        tint = fg.copy(alpha = if (canSkipPrevious) 1f else 0.4f),
                        modifier = Modifier.size(34.dp),
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
                            color = fg,
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
                                tint = fg,
                                modifier = Modifier.size(56.dp),
                            )
                        }
                    }
                }
                IconButton(
                    onClick = { if (canSkipNext) playerConnection.service.fadeSkip(true) },
                    modifier = Modifier.size(52.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = null,
                        tint = fg.copy(alpha = if (canSkipNext) 1f else 0.4f),
                        modifier = Modifier.size(34.dp),
                    )
                }
                IconButton(
                    onClick = playerConnection.player::toggleRepeatMode,
                    modifier = Modifier.size(42.dp),
                ) {
                    Icon(
                        painter = painterResource(if (repeatMode == REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat),
                        contentDescription = null,
                        tint = if (repeatMode != REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else fgDim.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Action row: info left, add-to-playlist + queue right.
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                IconButton(onClick = { showSongInfoDialog = true }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.info),
                        contentDescription = null,
                        tint = fgDim,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Row {
                    IconButton(onClick = { showAddToPlaylistDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.playlist_add),
                            contentDescription = null,
                            tint = fgDim,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    IconButton(onClick = { queueSheetState.expandSoft() }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.queue_music),
                            contentDescription = null,
                            tint = fgDim,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            if (showSongInfoDialog) {
                SongInfoDialog(
                    mediaMetadata = mediaMetadata,
                    onDismiss = { showSongInfoDialog = false },
                )
            }
            AddToPlaylistDialog(
                isVisible = showAddToPlaylistDialog,
                onGetSong = {
                    database.transaction {
                        insert(mediaMetadata)
                    }
                    listOf(mediaMetadata.id)
                },
                onDismiss = { showAddToPlaylistDialog = false },
            )
        }


        // "Playing from": resolves which LOCAL playlist the current queue was
        // started from, by matching the queue's song ids against each playlist
        // (Echo and SimpMusic both surface this in the player header). Runs once
        // per queue, off the main thread.
        // Apple Music backdrop seed: the artwork's dominant colour (64px Palette
        // decode, off the main thread). Drives the frosted backdrop's three-stop
        // gradient and the dock's active pill colours, like SimpMusic's palette.
        var appleSeedColor by remember { mutableStateOf<Color?>(null) }
        val appleSeedContext = LocalContext.current
        LaunchedEffect(mediaMetadata?.thumbnailUrl) {
            appleSeedColor = mediaMetadata?.thumbnailUrl?.let { url ->
                withContext(Dispatchers.IO) {
                    runCatching {
                        val result = appleSeedContext.imageLoader.execute(
                            ImageRequest.Builder(appleSeedContext).data(url).size(64).build(),
                        )
                        (result.drawable as? BitmapDrawable)?.bitmap?.let { bmp ->
                            Palette.from(bmp).generate().dominantSwatch?.rgb?.let { Color(it) }
                        }
                    }.getOrNull()
                }
            }
        }

        var playingFrom by rememberSaveable { mutableStateOf<String?>(null) }
        val playingQueueWindows by playerConnection.queueWindows.collectAsState()
        LaunchedEffect(playingQueueWindows) {
            val ids = playingQueueWindows
                .map { it.mediaItem.mediaId.removePrefix("Video") }
                .filter { it.isNotEmpty() }
            if (ids.isEmpty()) {
                playingFrom = null
            } else {
                withContext(Dispatchers.IO) {
                    playingFrom = runCatching {
                        database.playlistsByCreateDateAsc().first()
                            .firstNotNullOfOrNull { playlist ->
                                val pids = database.playlistSongs(playlist.id).first()
                                    .map { it.map.songId }
                                if (pids.isNotEmpty() && pids == ids) playlist.playlist.name else null
                            }
                    }.getOrNull()
                }
            }
        }

        val expressiveControls: @Composable ColumnScope.(MediaMetadata) -> Unit = { mediaMetadata ->
            // === SimpMusic Material 3 Expressive player, 1:1 port ===
            SimpExpressiveContent(
                mediaMetadata = mediaMetadata,
                navController = navController,
                repeatMode = repeatMode,
                shuffleModeEnabled = playerConnection.player.shuffleModeEnabled,
                isLiked = currentSong?.song?.liked == true,
                isPlaying = isPlaying,
                buffering = playbackState == STATE_BUFFERING,
                canSkipPrevious = playerConnection.player.hasPreviousMediaItem(),
                canSkipNext = playerConnection.player.hasNextMediaItem(),
                duration = duration,
                progressFractionProvider = {
                    if (duration == C.TIME_UNSET) 0f
                    else ((sliderPosition ?: position).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                },
                onScrub = { fraction ->
                    if (duration != C.TIME_UNSET) sliderPosition = (fraction * duration).toLong()
                },
                onScrubEnd = { fraction ->
                    if (duration != C.TIME_UNSET) {
                        val target = (fraction * duration).toLong()
                        playerConnection.player.seekTo(target)
                        position = target
                    }
                    sliderPosition = null
                },
                onToggleLike = { playerConnection.toggleLike() },
                onShuffle = {
                    playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled
                },
                onRepeat = {
                    playerConnection.player.repeatMode = when (repeatMode) {
                        REPEAT_MODE_OFF -> REPEAT_MODE_ALL
                        REPEAT_MODE_ALL -> REPEAT_MODE_ONE
                        else -> REPEAT_MODE_OFF
                    }
                },
                onPlayPause = {
                    if (playerConnection.player.isPlaying) playerConnection.player.pause() else playerConnection.player.play()
                },
                onPrevious = { playerConnection.player.seekToPreviousMediaItem() },
                onNext = { playerConnection.player.seekToNextMediaItem() },
                onShowLyrics = { onShowLyricsChange(!showLyrics) },
                lyricsActive = showLyrics,
                onShowInfo = { showSongInfoDialog = true },
                onAddToPlaylist = { showAddToPlaylistDialog = true },
                onShowQueue = { queueSheetState.expandSoft() },
            )
            // SimpMusic M3 Expressive: the connected group's Details and
            // Add-to-playlist slots run real dialogs - song info (metadata +
            // live stream format from the database) and the shared playlist picker.
            if (showSongInfoDialog) {
                SongInfoDialog(
                    mediaMetadata = mediaMetadata,
                    onDismiss = { showSongInfoDialog = false },
                )
            }
            AddToPlaylistDialog(
                isVisible = showAddToPlaylistDialog,
                onGetSong = {
                    database.transaction {
                        insert(mediaMetadata)
                    }
                    listOf(mediaMetadata.id)
                },
                onDismiss = { showAddToPlaylistDialog = false },
            )
        }

        val appleControls: @Composable ColumnScope.(MediaMetadata) -> Unit = { mediaMetadata ->
            // === SimpMusic's Apple Music style, 1:1 from NowPlayingContentAppleMusic.kt
            // + applemusic/AppleMusicShared.kt: white-on-dark text over the frosted
            // artwork backdrop, the thin 7dp thumbless progress bar that swells to
            // 14dp while touched, plain white transport (46dp skips, 66dp play,
            // 58dp gaps), and the Lyrics | Queue dock with its light active pill.
            val onVideo = videoEnabled && videoActive
            val fg = Color.White
            val fgSoft = if (onVideo) Color.White.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.72f)
            val trackActive = Color.White.copy(alpha = 0.92f)
            val trackInactive = Color.White.copy(alpha = 0.26f)
            val seed = appleSeedColor ?: MaterialTheme.colorScheme.primary
            val activePillContainer = lerp(seed, Color.White, 0.75f)
            val activePillContent = lerp(seed, Color.Black, 0.6f)
            val liked = currentSong?.song?.liked == true

            // --- Title row: 20dp gutter, title + artist left, heart + more right ---
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
                        color = fg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start,
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
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        mediaMetadata.artists.fastForEachIndexed { index, artist ->
                            Text(
                                text = artist.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = fgSoft,
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
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = fgSoft,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                IconButton(
                    onClick = { playerConnection.toggleLike() },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        painter = painterResource(if (liked) R.drawable.favorite else R.drawable.favorite_border),
                        contentDescription = null,
                        tint = fg,
                        modifier = Modifier.size(24.dp),
                    )
                }
                IconButton(
                    onClick = { showSongInfoDialog = true },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = null,
                        tint = fg,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            // --- Thin 7dp thumbless progress bar (swells to 14dp while touched),
            // inside a constant 18dp shell so nothing around it re-measures. ---
            var appleDragging by remember { mutableStateOf(false) }
            var appleDragFraction by remember { mutableStateOf(0f) }
            val appleTrackHeight by animateDpAsState(
                targetValue = if (appleDragging) 14.dp else 7.dp,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
                label = "appleSliderInflate",
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            if (duration != C.TIME_UNSET) {
                                val target = (fraction * duration).toLong()
                                playerConnection.player.seekTo(target)
                                position = target
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                appleDragging = true
                                appleDragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                                if (duration != C.TIME_UNSET) {
                                    sliderPosition = (appleDragFraction * duration).toLong()
                                }
                            },
                            onDragEnd = {
                                if (duration != C.TIME_UNSET) {
                                    val target = (appleDragFraction * duration).toLong()
                                    playerConnection.player.seekTo(target)
                                    position = target
                                }
                                sliderPosition = null
                                appleDragging = false
                            },
                            onDragCancel = {
                                sliderPosition = null
                                appleDragging = false
                            },
                            onHorizontalDrag = { change, _ ->
                                change.consume()
                                appleDragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                if (duration != C.TIME_UNSET) {
                                    sliderPosition = (appleDragFraction * duration).toLong()
                                }
                            },
                        )
                    },
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(appleTrackHeight)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(trackInactive)
                        // The active fill reads the position/slider state INSIDE the draw
                        // lambda: draw-phase-only invalidation, so the 100 ms ticks
                        // animate the bar with ZERO recomposition.
                        .drawBehind {
                            val fraction = (
                                if (appleDragging) appleDragFraction
                                else if (duration == C.TIME_UNSET) 0f
                                else ((sliderPosition ?: position).toFloat() / duration.toFloat())
                                ).coerceIn(0f, 1f)
                            drawRect(
                                color = trackActive,
                                size = size.copy(width = size.width * fraction),
                            )
                        },
                )
            }

            // --- Times row: elapsed left, -remaining right ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                PositionTimeText(
                    positionProvider = { position },
                    sliderPosition = sliderPosition,
                    makeText = { makeTimeString(it) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = fgSoft,
                    modifier = Modifier.weight(1f),
                )
                PositionTimeText(
                    positionProvider = { position },
                    sliderPosition = sliderPosition,
                    makeText = {
                        if (duration != C.TIME_UNSET) "-" + makeTimeString((duration - it).coerceAtLeast(0L)) else ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = fgSoft,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(12.dp))

            // --- Transport: 46dp skips, 66dp plain white play, 58dp gaps, press swell ---
            val applePlayInteraction = remember { MutableInteractionSource() }
            val applePlayPressed by applePlayInteraction.collectIsPressedAsState()
            val applePlayScale by animateFloatAsState(
                targetValue = if (applePlayPressed) 1.35f else 1f,
                animationSpec = spring(dampingRatio = 0.45f, stiffness = 380f),
                label = "applePlayInflate",
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(58.dp, Alignment.CenterHorizontally),
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
                        tint = fg.copy(alpha = if (canSkipPrevious) 1f else 0.4f),
                        modifier = Modifier.size(46.dp),
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = applePlayScale
                            scaleY = applePlayScale
                        }
                        .size(76.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = applePlayInteraction,
                            indication = null,
                        ) {
                            if (playbackState == STATE_ENDED) {
                                playerConnection.player.seekTo(0, 0)
                                playerConnection.player.playWhenReady = true
                            } else {
                                playerConnection.player.togglePlayPause()
                            }
                        },
                ) {
                    Crossfade(
                        targetState = if (playbackState == STATE_ENDED) R.drawable.replay else if (isPlaying) R.drawable.pause else R.drawable.play,
                        animationSpec = tween(150),
                        label = "applePlayPauseIcon",
                    ) { iconRes ->
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            tint = fg,
                            modifier = Modifier.size(66.dp),
                        )
                    }
                }
                IconButton(
                    onClick = { if (canSkipNext) playerConnection.service.fadeSkip(true) },
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = null,
                        tint = fg.copy(alpha = if (canSkipNext) 1f else 0.4f),
                        modifier = Modifier.size(46.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Spacer(Modifier.height(6.dp))

            // Device volume row (SimpMusic Apple Music): the system media volume,
            // live-synced with the hardware keys too.
            DeviceVolumeRow(tint = Color.White.copy(alpha = 0.72f))

            Spacer(Modifier.height(6.dp))

            // --- Dock: Lyrics | Queue (tabbed bodies, SimpMusic) ---
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (appleView == AppleMusicView.LYRICS) activePillContainer else Color.Transparent
                        )
                        .clickable {
                            appleView = if (appleView == AppleMusicView.LYRICS) {
                                AppleMusicView.MAIN
                            } else {
                                AppleMusicView.LYRICS
                            }
                        },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.lyrics),
                        contentDescription = null,
                        tint = if (appleView == AppleMusicView.LYRICS) activePillContent else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(22.dp),
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (appleView == AppleMusicView.QUEUE) activePillContainer else Color.Transparent
                        )
                        .clickable {
                            appleView = if (appleView == AppleMusicView.QUEUE) {
                                AppleMusicView.MAIN
                            } else {
                                AppleMusicView.QUEUE
                            }
                        },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.queue_music),
                        contentDescription = null,
                        tint = if (appleView == AppleMusicView.QUEUE) activePillContent else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

        }

        val controlsContent: @Composable ColumnScope.(MediaMetadata) -> Unit = when (playerStyle) {
            PlayerStyle.SPOTIFY -> classicControls
            PlayerStyle.EXPRESSIVE -> expressiveControls
            PlayerStyle.APPLE -> appleControls
        }

        // Player background style: the current artwork, heavily blurred, behind the whole
        // player (liquid-glass look). The video background takes priority when it is showing.
        // SimpMusic Apple Music backdrop: the frosted cover art (heavy blur)
        // under a translucent three-stop wash of the artwork's dominant colour,
        // darkest at the bottom so the white text reads comfortably.
        if (playerStyle == PlayerStyle.APPLE && !(showVideo && !showLyrics && state.progress > 0.5f)) {
            val seed = appleSeedColor ?: MaterialTheme.colorScheme.primary
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
                    Box(
                        Modifier
                            .matchParentSize()
                            .alpha(0.85f)
                            .background(
                                Brush.verticalGradient(
                                    0f to lerp(seed, Color.Black, 0.05f),
                                    0.48f to lerp(seed, Color.Black, 0.32f),
                                    1f to lerp(seed, Color.Black, 0.78f),
                                ),
                            ),
                    )
                }
            }
        }

        // SimpMusic Classic backdrop: the artwork's dominant colour slides down
        // a diagonal gradient into the near-black player surface (#121212).
        if (playerStyle == PlayerStyle.SPOTIFY &&
            !(showVideo && !showLyrics && state.progress > 0.5f)
        ) {
            val seed = appleSeedColor
            val startColor by animateColorAsState(
                targetValue = seed ?: Color(0xFF1F1F1F),
                animationSpec = tween(800),
                label = "classicStart",
            )
            val endColor by animateColorAsState(
                targetValue = seed?.let { lerp(it, Color.Black, 0.55f) } ?: Color(0xFF151515),
                animationSpec = tween(800),
                label = "classicEnd",
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        drawRect(Color(0xFF121212))
                        drawRect(
                            Brush.linearGradient(
                                colors = listOf(startColor, endColor, Color(0xFF121212)),
                                start = Offset.Zero,
                                end = Offset(size.width, size.height),
                            ),
                        )
                    },
            )
        }

        if (playerBackgroundStyle == PlayerBackgroundStyle.BLURRED_ARTWORK &&

            playerStyle == PlayerStyle.EXPRESSIVE &&
            !(showVideo && !showLyrics && state.progress > 0.5f)
        ) {
            mediaMetadata?.thumbnailUrl?.let { thumbnailUrl ->
                Box(Modifier.matchParentSize()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(thumbnailUrl)
                            // Decode the artwork tiny and let matchParentSize upscale it:
                            // a heavy blur that costs nothing per frame, instead of
                            // Modifier.blur(64.dp) re-rendering on every animation frame.
                            .size(64)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
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

        // SimpMusic-style single-stream video: the MAIN player renders its own
        // output. In PORTRAIT the video is framed inside the artwork slot (per
        // style, at the stream's real aspect ratio); in LANDSCAPE it fills the
        // whole screen behind everything.
        val isVideoStream by playerConnection.service.isVideoPlayback.collectAsState()
        LaunchedEffect(videoEnabled, isVideoStream) {
            videoActive = videoEnabled && isVideoStream
        }
        if (videoEnabled && LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            MainPlayerVideo(
                player = playerConnection.player,
                modifier = Modifier.matchParentSize(),
            )
        }

        // SimpMusic-style auto-hide: 3s after the controls appear over the
        // fullscreen video they fade away again.
        LaunchedEffect(controlsVisible, videoEnabled, videoActive) {
            if (controlsVisible && videoEnabled && videoActive) {
                delay(3000)
                controlsVisible = false
            }
        }

        // Immersive mode: while the full-screen video plays, the status bar always hides and
        // the navigation bar hides once the controls are toggled away. Everything comes back
        // when the sheet collapses or the player leaves composition.
        val immersiveView = LocalView.current
        LaunchedEffect(state.isExpanded, videoEnabled, videoActive, controlsVisible, keepScreenOn, fullscreenVideo) {
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
            if (fullscreenVideo) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            } else if (state.isExpanded && videoEnabled && videoActive) {
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
                ) {
                    if (videoEnabled && videoActive) {
                        // SimpMusic-style fullscreen video, portrait: the video renders
                        // behind everything at the sheet root and the controls overlay the
                        // BOTTOM of the full screen over the scrim - no mini-player
                        // padding, no split halves, the picture runs edge to edge.
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures {
                                        controlsVisible = !controlsVisible
                                    }
                                }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Bottom)),
                            ) {
                                Spacer(Modifier.weight(1f))
                                PlayerControls(
                                    videoActive = true,
                                    controlsVisible = controlsVisible,
                                ) {
                                    mediaMetadata?.let { controlsContent(it) }
                                }
                                Spacer(Modifier.height(16.dp))
                            }
                        }
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
                                modifier = (
                                    if (playerStyle == PlayerStyle.EXPRESSIVE)
                                        Modifier
                                            .padding(horizontal = 20.dp)
                                            .clip(RoundedCornerShape(28.dp))
                                    else if (playerStyle == PlayerStyle.APPLE)
                                        Modifier
                                    else Modifier
                                    )
                                    .nestedScroll(state.preUpPostDownNestedScrollConnection)
                                    .graphicsLayer {
                                        translationX = swipeOffset.coerceIn(-size.width.toFloat(), size.width.toFloat()) * 0.55f
                                    }
                            )
                        }
                    }

                    if (!(videoEnabled && videoActive)) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                        ) {
                            Spacer(Modifier.weight(1f))

                            PlayerControls(
                                videoActive = false,
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
            }

            else -> {
                // The Classic (SPOTIFY) page scrolls; the state is hoisted so the
                // below-fold cards know when they are actually reached.
                val classicScrollState = rememberScrollState()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                        .nestedScroll(state.preUpPostDownNestedScrollConnection)
                        .then(
                            if (playerStyle == PlayerStyle.SPOTIFY) {
                                // SimpMusic Classic: the whole page scrolls - the
                                // below-the-fold cards live under the controls.
                                Modifier.verticalScroll(classicScrollState)
                            } else Modifier
                        ),
                ) {
                    // The artwork area stays composed at all times so the thumbnail can
                    // FADE OUT smoothly when the video stream becomes ready - SimpMusic's
                    // behaviour - instead of hard-swapping to an empty box.
                    val videoVisible = videoEnabled && videoActive
                    val artworkAlpha by animateFloatAsState(
                        targetValue = if (videoVisible) 0f else 1f,
                        animationSpec = tween(300),
                        label = "artworkFade",
                    )
                    // === SimpMusic M3 Expressive header row: 44dp tonal circles, a
                    // down-chevron that collapses the sheet, and the centered NOW PLAYING
                    // label. The more button opens the real song Details dialog.
                    // SimpMusic Apple Music grabber: the style's only top chrome.
                    // Tap to collapse the player sheet.
                    if (playerStyle == PlayerStyle.APPLE && !videoVisible) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                                .padding(top = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 36.dp, height = 5.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.35f))
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                    ) { state.collapseSoft() },
                            )
                        }
                    }

                    if (playerStyle == PlayerStyle.SPOTIFY && !videoVisible) {
                        // SimpMusic Classic top bar: dismiss chevron, the centered
                        // NOW PLAYING + playlist stack, more-vert on the right.
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                                .padding(horizontal = 4.dp),
                        ) {
                            IconButton(
                                onClick = { state.collapseSoft() },
                                modifier = Modifier.size(44.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.expand_more),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = stringResource(R.string.now_playing_upper).uppercase(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                )
                                playingFrom?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.72f),
                                        maxLines = 1,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .basicMarquee(),
                                    )
                                }
                            }
                            IconButton(
                                onClick = { showSongInfoDialog = true },
                                modifier = Modifier.size(44.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.more_vert),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }

                    if (playerStyle == PlayerStyle.EXPRESSIVE && !videoVisible) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                                .padding(top = 8.dp, start = 20.dp, end = 20.dp),
                        ) {
                            Surface(
                                onClick = { state.collapseSoft() },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(44.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        painter = painterResource(R.drawable.expand_more),
                                        contentDescription = null,
                                    )
                                }
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.now_playing_upper).uppercase(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                playingFrom?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .basicMarquee(),
                                    )
                                }
                            }
                            Surface(
                                onClick = { showSongInfoDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(44.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = null,
                                    )
                                }
                            }
                        }
                    }

                    // Apple Music tabbed bodies (SimpMusic): the dock switches the
                    // artwork area to the LYRICS or QUEUE body; re-tap returns to MAIN.
                    if (playerStyle == PlayerStyle.APPLE && appleView == AppleMusicView.LYRICS) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        ) {
                            AppleMusicLyricsBody(
                                mediaMetadata = mediaMetadata,
                                sliderPositionProvider = { sliderPosition },
                                onBack = { appleView = AppleMusicView.MAIN },
                            )
                        }
                    } else if (playerStyle == PlayerStyle.APPLE && appleView == AppleMusicView.QUEUE) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        ) {
                            AppleMusicQueueBody(
                                queue = pagerQueueWindows,
                                currentWindowIndex = currentWindowIndex,
                                onPlayIndex = { index -> playerConnection.player.seekTo(index, 0) },
                                onBack = { appleView = AppleMusicView.MAIN },
                            )
                        }
                    } else {
                    if (pagerQueueWindows.isEmpty()) {
                        // Never compose the pager with an empty queue: pageCount
                        // 0 parks the page at -1 and the refill draw crashes
                        // (IndexOutOfBoundsException: Index -1, size 1). Hold the
                        // artwork slot with an empty box until the queue lands.
                        Box(
                            modifier = Modifier
                                .then(
                                    if (playerStyle == PlayerStyle.SPOTIFY) {
                                        Modifier
                                            .fillMaxWidth()
                                            .height((LocalConfiguration.current.screenWidthDp.dp - 40.dp))
                                    } else Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ),
                        )
                    } else {
                    // === SimpMusic artwork pager =====================================
                    // The queue's covers swipe horizontally; a settled user swipe
                    // changes the song. The current page holds the artwork (which
                    // fades out for the video, keeping its slot); adjacent pages
                    // show the covers of what comes next / before in the queue.
                    HorizontalPager(
                        state = pagerState,
                        beyondViewportPageCount = 1,
                        userScrollEnabled = gestureAnimationsEnabled &&
                            repeatMode != REPEAT_MODE_ONE &&
                            pagerQueueWindows.isNotEmpty(),
                        modifier = Modifier
                            .then(
                                if (playerStyle == PlayerStyle.SPOTIFY) {
                                    // Classic scrolls the page: the artwork is a
                                    // square card, not a flexing weight.
                                    Modifier
                                        .fillMaxWidth()
                                        .height((LocalConfiguration.current.screenWidthDp.dp - 40.dp))
                                } else Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ),
                    ) { page ->
                        val isCurrentPage = page == currentWindowIndex
                        Box(
                            contentAlignment = if (playerStyle == PlayerStyle.APPLE) Alignment.TopCenter else Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            if (isCurrentPage) {
                                Thumbnail(
                                    sliderPositionProvider = { sliderPosition },
                                    modifier = (
                                        if (playerStyle == PlayerStyle.EXPRESSIVE)
                                            Modifier
                                                .padding(horizontal = 20.dp)
                                                .clip(RoundedCornerShape(28.dp))
                                        else if (playerStyle == PlayerStyle.APPLE)
                                            Modifier
                                        else Modifier
                                        )
                                        .graphicsLayer { alpha = artworkAlpha },
                                )
                                if (videoEnabled) {
                                    // SimpMusic framing: Classic = an 8dp-rounded box at
                                    // the video's own aspect ratio; Expressive = the 28dp
                                    // card takes the video's shape (capped at square so a
                                    // tall video cannot push the fold); Apple = centred,
                                    // 12dp corners only when the video is portrait.
                                    val videoAlpha by animateFloatAsState(
                                        targetValue = if (videoVisible) 1f else 0f,
                                        animationSpec = tween(300),
                                        label = "videoFade",
                                    )
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer { alpha = videoAlpha },
                                    ) {
                                        MainPlayerVideo(
                                            player = playerConnection.player,
                                            modifier = when (playerStyle) {
                                                PlayerStyle.EXPRESSIVE -> Modifier
                                                    .padding(horizontal = 20.dp)
                                                    .fillMaxWidth()
                                                    .aspectRatio(videoAspectRatio.coerceAtLeast(1f))
                                                    .clip(RoundedCornerShape(28.dp))
                                                PlayerStyle.APPLE -> Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(videoAspectRatio)
                                                    .then(
                                                        if (videoAspectRatio < 1f) {
                                                            Modifier.clip(RoundedCornerShape(12.dp))
                                                        } else Modifier
                                                    )
                                                else -> Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(videoAspectRatio)
                                                    .clip(RoundedCornerShape(8.dp))
                                            },
                                            crop = true,
                                        )
                                        if (videoVisible && videoSubtitles) {
                                            Spacer(Modifier.height(10.dp))
                                            PlayerInlineLyricLine(
                                                positionProvider = { position },
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodyMedium,
                                                chip = true,
                                            )
                                        }
                                    }
                                    if (videoVisible) {
                                        // Over-video overlay (SimpMusic): fullscreen
                                        // top-end, -5s/+5s centred, subtitle toggle
                                        // bottom-end; a tap toggles, 3s auto-hide.
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .pointerInput(Unit) {
                                                    detectTapGestures {
                                                        controlsVisible = !controlsVisible
                                                    }
                                                },
                                        )
                                        if (controlsVisible) {
                                            Column(
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            0f to Color.Black.copy(alpha = 0.4f),
                                                            0.2f to Color.Transparent,
                                                            0.8f to Color.Transparent,
                                                            1f to Color.Black.copy(alpha = 0.4f),
                                                        ),
                                                    ),
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(56.dp),
                                                    contentAlignment = Alignment.CenterEnd,
                                                ) {
                                                    IconButton(
                                                        onClick = { fullscreenVideo = true },
                                                        modifier = Modifier
                                                            .size(56.dp)
                                                            .padding(12.dp),
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.fullscreen),
                                                            contentDescription = null,
                                                            tint = Color.White.copy(alpha = 0.8f),
                                                        )
                                                    }
                                                }
                                                Spacer(Modifier.weight(1f))
                                                Row(
                                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth(),
                                                ) {
                                                    IconButton(
                                                        onClick = { playerConnection.player.seekBack() },
                                                        modifier = Modifier.size(48.dp),
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.replay),
                                                            contentDescription = null,
                                                            tint = Color.White.copy(alpha = 0.8f),
                                                            modifier = Modifier.size(30.dp),
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { playerConnection.player.seekForward() },
                                                        modifier = Modifier.size(48.dp),
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.fast_forward),
                                                            contentDescription = null,
                                                            tint = Color.White.copy(alpha = 0.8f),
                                                            modifier = Modifier.size(30.dp),
                                                        )
                                                    }
                                                }
                                                Spacer(Modifier.weight(1f))
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(56.dp),
                                                    contentAlignment = Alignment.CenterEnd,
                                                ) {
                                                    IconButton(
                                                        onClick = { videoSubtitles = !videoSubtitles },
                                                        modifier = Modifier
                                                            .size(56.dp)
                                                            .padding(12.dp),
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.lyrics),
                                                            contentDescription = null,
                                                            tint = Color.White.copy(alpha = 0.8f),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Adjacent queue page: the cover of the song that comes
                                // next / before in the queue.
                                val pageMetadata = pagerQueueWindows.getOrNull(page)
                                    ?.mediaItem?.metadata as? MediaMetadata
                                AsyncImage(
                                    model = pageMetadata?.thumbnailUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = (
                                        if (playerStyle == PlayerStyle.EXPRESSIVE)
                                            Modifier
                                                .padding(horizontal = 20.dp)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(28.dp))
                                        else if (playerStyle == PlayerStyle.APPLE)
                                            Modifier.fillMaxSize()
                                        else Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                        ),
                                )
                            }
                        }
                    }


                    }
                    }

                    if (!videoVisible && (playerStyle == PlayerStyle.EXPRESSIVE || playerStyle == PlayerStyle.SPOTIFY)) {
                        // SimpMusic: the current lyric line sits centered in the gap
                        // between the artwork and the info block (Expressive + Classic).
                        PlayerInlineLyricLine(
                            positionProvider = { position },
                            color = if (playerStyle == PlayerStyle.SPOTIFY) Color.White.copy(alpha = 0.72f)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }

                    PlayerControls(
                        videoActive = false,
                        controlsVisible = controlsVisible,
                    ) {
                        mediaMetadata?.let {
                            controlsContent(it)
                        }
                    }

                    if (playerStyle == PlayerStyle.SPOTIFY) {
                        // === SimpMusic Classic below-the-fold cards =============
                        ClassicBelowFoldCards(
                            mediaMetadata = mediaMetadata,
                            scrollState = classicScrollState,
                            sliderPositionProvider = { sliderPosition },
                            seedColor = appleSeedColor,
                            navController = navController,
                            onShowLyrics = { onShowLyricsChange(true) },
                        )
                        Spacer(Modifier.height(48.dp))
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        // No collapsed-queue peek bar: the queue sheet only composes once it is
        // actually being pulled up (a queue button opens it via expandSoft()).
        // SimpMusic landscape fullscreen video route: the over-video fullscreen
        // button locks the screen landscape, hides the system bars and fills the
        // whole player with the video plus its control overlay.
        if (fullscreenVideo) {
            FullscreenVideoPlayer(
                player = playerConnection.player,
                mediaMetadata = mediaMetadata,
                positionProvider = { position },
                durationProvider = { duration },
                isPlaying = isPlaying,
                playbackState = playbackState,
                canSkipPrevious = canSkipPrevious,
                canSkipNext = canSkipNext,
                onPrevious = { playerConnection.service.fadeSkip(false) },
                onNext = { playerConnection.service.fadeSkip(true) },
                onPlayPause = {
                    if (playbackState == STATE_ENDED) {
                        playerConnection.player.seekTo(0, 0)
                        playerConnection.player.playWhenReady = true
                    } else {
                        playerConnection.player.togglePlayPause()
                    }
                },
                onSeek = {
                    playerConnection.player.seekTo(it)
                    position = it
                },
                onShowInfo = { showSongInfoDialog = true },
                onExit = { fullscreenVideo = false },
            )
        }

        if (!queueSheetState.isCollapsed) {

            Queue(
                state = queueSheetState,
                playerBottomSheetState = state,
                backgroundColor = backgroundColor,
                navController = navController
            )
        }
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

/**
 * Time text that reads the playback position through [positionProvider] inside a
 * derivedStateOf: it only recomposes when the displayed STRING changes (~1 Hz),
 * not on every 100 ms position tick. Scrubbing still updates immediately because
 * [sliderPosition] is a remember key of the derived block.
 */
@Composable
private fun PositionTimeText(
    positionProvider: () -> Long,
    sliderPosition: Long?,
    makeText: (Long) -> String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) {
    val text by remember(sliderPosition) {
        derivedStateOf { makeText(sliderPosition ?: positionProvider()) }
    }
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        modifier = modifier,
    )
}

/**
 * Squiggly slider leaf: the only component in the default player that needs the raw
 * 100 ms position value, so reading it here contains the per-tick recomposition to
 * this one small composable instead of the whole player screen.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SquigglyPositionSlider(
    positionProvider: () -> Long,
    sliderPosition: Long?,
    duration: Long,
    isPlaying: Boolean,
    onValueChange: (Long) -> Unit,
    onScrubEnd: () -> Unit,
) {
    SquigglySlider(
        value = (sliderPosition ?: positionProvider()).toFloat(),
        valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
        onValueChange = { onValueChange(it.toLong()) },
        onValueChangeFinished = onScrubEnd,
        squigglesSpec = SquigglySlider.SquigglesSpec(
            amplitude = if (isPlaying) 2.dp else 0.dp,
            strokeWidth = 4.dp,
        ),
        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
    )
}


/**
 * The current lyric line, shared by the Expressive/Classic in-player gap and the
 * over-video subtitle (SimpMusic): the lyrics are parsed once per change, then a
 * derivedStateOf recomputes the ACTIVE line on every position tick without
 * recomposing anything else. Fades between lines over 300ms.
 */
@Composable
private fun PlayerInlineLyricLine(
    positionProvider: () -> Long,
    color: Color,
    style: TextStyle,
    chip: Boolean = false,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val lyricsEntity by playerConnection.currentLyrics.collectAsState(initial = null)
    val lyricsText = remember(lyricsEntity) { lyricsEntity?.lyrics }
    val lines = remember(lyricsText) {
        when {
            lyricsText == null || lyricsText == LYRICS_NOT_FOUND -> emptyList()
            lyricsText.trimStart().startsWith("<?xml") || lyricsText.trimStart().startsWith("<tt") ->
                listOf(HEAD_LYRICS_ENTRY) + TTMLParser.parseTTML(lyricsText).map {
                    LyricsEntry((it.startTime * 1000).toLong(), it.text)
                }
            lyricsText.startsWith("[") -> listOf(HEAD_LYRICS_ENTRY) + parseLyrics(lyricsText)
            else -> lyricsText.lines().mapIndexed { index, line -> LyricsEntry(index * 100L, line) }
        }
    }
    val currentLine by remember(lines) {
        derivedStateOf {
            if (lines.isEmpty()) ""
            else lines.getOrNull(findCurrentLineIndex(lines, positionProvider()))
                ?.takeIf { it.text.isNotEmpty() }?.text ?: ""
        }
    }
    Crossfade(
        targetState = currentLine,
        animationSpec = tween(durationMillis = 300),
        label = "inlineLyric",
    ) { lineText ->
        if (chip) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = lineText,
                    style = style,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(),
                )
            }
        } else {
            Text(
                text = lineText,
                style = style,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .basicMarquee(),
            )
        }
    }
}

/**
 * SimpMusic FullscreenPlayer: the landscape fullscreen video route. Locks
 * landscape while composed and restores the original orientation on exit; a
 * single tap toggles the control overlay, a double tap on a half seeks
 * -5s/+5s, and the overlay carries the marquee title, transport, seek slider
 * and times.
 */
@Composable
private fun FullscreenVideoPlayer(
    player: androidx.media3.common.Player,
    mediaMetadata: MediaMetadata?,
    positionProvider: () -> Long,
    durationProvider: () -> Long,
    isPlaying: Boolean,
    playbackState: Int,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onShowInfo: () -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation
            ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }
    var overlayVisible by remember { mutableStateOf(true) }
    var scrubPosition by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(overlayVisible) {
        if (overlayVisible) {
            delay(3000)
            overlayVisible = false
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { overlayVisible = !overlayVisible },
                    onDoubleTap = { offset ->
                        if (offset.x < size.width / 2f) player.seekBack() else player.seekForward()
                    },
                )
            },
    ) {
        MainPlayerVideo(
            player = player,
            modifier = Modifier.matchParentSize(),
            crop = false,
        )
        if (overlayVisible) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .windowInsetsPadding(
                        WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Bottom)
                    ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                ) {
                    IconButton(onClick = onExit, modifier = Modifier.size(44.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.expand_more),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Text(
                        text = mediaMetadata?.title ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee(),
                    )
                    IconButton(onClick = onShowInfo, modifier = Modifier.size(44.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .align(Alignment.CenterHorizontally),
                ) {
                    IconButton(
                        onClick = onPrevious,
                        enabled = canSkipPrevious,
                        modifier = Modifier.size(52.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_previous),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = if (canSkipPrevious) 1f else 0.4f),
                            modifier = Modifier.size(34.dp),
                        )
                    }
                    IconButton(
                        onClick = { player.seekBack() },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.replay),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onPlayPause() },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (playbackState == STATE_BUFFERING) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(28.dp),
                            )
                        } else {
                            Icon(
                                painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp),
                            )
                        }
                    }
                    IconButton(
                        onClick = { player.seekForward() },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.fast_forward),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    IconButton(
                        onClick = onNext,
                        enabled = canSkipNext,
                        modifier = Modifier.size(52.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_next),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = if (canSkipNext) 1f else 0.4f),
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                ) {
                    val duration = durationProvider()
                    Slider(
                        value = if (duration == C.TIME_UNSET || duration <= 0) 0f
                        else ((scrubPosition ?: positionProvider()).toFloat() / duration).coerceIn(0f, 1f),
                        onValueChange = { fraction ->
                            if (duration != C.TIME_UNSET && duration > 0) {
                                scrubPosition = (fraction * duration).toLong()
                            }
                        },
                        onValueChangeFinished = {
                            scrubPosition?.let { onSeek(it) }
                            scrubPosition = null
                        },
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                            thumbColor = Color.White,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = makeTimeString(scrubPosition ?: positionProvider()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.72f),
                        )
                        Text(
                            text = if (duration == C.TIME_UNSET) "" else makeTimeString(duration),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.72f),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}


/** In-memory cache of below-fold description-card data, so re-entering the
 * player for a song never re-fetches it from the player endpoint. */
private val classicDetailsCache = mutableMapOf<String, PlayerResponse.VideoDetails>()

/** Apple Music tabbed bodies (SimpMusic): MAIN, LYRICS, QUEUE. */
enum class AppleMusicView {
    MAIN, LYRICS, QUEUE
}

/**
 * SimpMusic Apple Music LYRICS body: a compact header (small artwork, ellipsized
 * title/artist, back to MAIN) over the shared lyrics renderer on a transparent
 * background so the frosted backdrop shows through.
 */
@Composable
private fun AppleMusicLyricsBody(
    mediaMetadata: MediaMetadata?,
    sliderPositionProvider: () -> Long?,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            AsyncImage(
                model = mediaMetadata?.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(55.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = mediaMetadata?.title ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = mediaMetadata?.artists?.joinToString { it.name } ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Lyrics(
                sliderPositionProvider = sliderPositionProvider,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * SimpMusic Apple Music QUEUE body: the same compact header over the queue list;
 * rows are number + artwork + title/artist, the current row highlighted, tap plays.
 */
@Composable
private fun AppleMusicQueueBody(
    queue: List<Timeline.Window>,
    currentWindowIndex: Int,
    onPlayIndex: (Int) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = stringResource(R.string.queue),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            itemsIndexed(queue) { index, window ->
                val md = window.mediaItem.metadata as? MediaMetadata
                val isCurrent = index == currentWindowIndex
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlayIndex(index) }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = if (isCurrent) 1f else 0.45f),
                        modifier = Modifier.width(28.dp),
                    )
                    AsyncImage(
                        model = md?.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                    ) {
                        Text(
                            text = md?.title ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = if (isCurrent) 1f else 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = md?.artists?.joinToString { it.name } ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (isCurrent) {
                        Icon(
                            painter = painterResource(R.drawable.graphic_eq),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * SimpMusic Apple Music device volume row: a thin slider bound to the SYSTEM media
 * volume (not the app's own volume), with a ContentObserver so hardware volume keys
 * stay in sync with the slider position too.
 */
@Composable
private fun DeviceVolumeRow(tint: Color) {
    val context = LocalContext.current
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    var deviceVolume by remember {
        mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    }
    DisposableEffect(Unit) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                deviceVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            }
        }
        context.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.volume_down),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Slider(
            value = if (maxVolume > 0) deviceVolume.toFloat() / maxVolume else 0f,
            onValueChange = { fraction ->
                val target = (fraction * maxVolume).roundToInt().coerceIn(0, maxVolume)
                runCatching { audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0) }
                deviceVolume = target
            },
            colors = SliderDefaults.colors(
                activeTrackColor = Color.White,
                inactiveTrackColor = tint.copy(alpha = 0.3f),
                thumbColor = Color.White,
            ),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        Icon(
            painter = painterResource(R.drawable.volume_up),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * SimpMusic Classic below-the-fold cards: a Lyrics card (embedded lyrics preview
 * with a Show button), an Artist card (channel art + song count, links to the
 * artist) and a Description card (view count + the video's description), all on
 * the artwork's palette-derived card colours.
 */
@Composable
private fun ClassicBelowFoldCards(
    mediaMetadata: MediaMetadata?,
    scrollState: ScrollState,
    sliderPositionProvider: () -> Long?,
    seedColor: Color?,
    navController: NavController,
    onShowLyrics: () -> Unit,
) {
    val mediaMetadata = mediaMetadata ?: return
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val lyricsEntity by playerConnection.currentLyrics.collectAsState(initial = null)
    val lyrics = lyricsEntity?.lyrics
    val hasLyrics = lyrics != null && lyrics != LYRICS_NOT_FOUND

    // Artist card data: the first artist's saved channel art + song count.
    val mainArtistId = mediaMetadata.artists.firstOrNull()?.id
    val artist by remember(mainArtistId) {
        if (mainArtistId != null) database.artist(mainArtistId) else flowOf(null)
    }.collectAsState(initial = null)

    // Description card data (SimpMusic song info): view count + description from
    // the innertube player response. Fetched ONLY for YouTube ids (11 chars -
    // local songs never hit the network), ONLY once the user actually scrolls
    // below the fold where the card is visible, and at most once per song: a
    // player-endpoint call on every player open was heavy enough to get the
    // YouTube client throttled, which broke search.
    var videoDetails by remember(mediaMetadata.id) {
        mutableStateOf(classicDetailsCache[mediaMetadata.id])
    }
    LaunchedEffect(mediaMetadata.id) {
        if (mediaMetadata.id.length != 11) return@LaunchedEffect
        if (videoDetails != null) return@LaunchedEffect
        snapshotFlow { scrollState.value > 0 }.first { it }
        withContext(Dispatchers.IO) {
            YouTube.player(mediaMetadata.id).getOrNull()?.videoDetails
        }?.also { classicDetailsCache[mediaMetadata.id] = it }?.let { videoDetails = it }
    }

    val cardShape = RoundedCornerShape(8.dp)

    // --- Lyrics card -------------------------------------------------------
    if (hasLyrics) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = cardShape,
            colors = CardDefaults.elevatedCardColors(
                containerColor = seedColor?.copy(alpha = 0.35f) ?: Color(0xFF212121),
            ),
        ) {
            Column(Modifier.padding(15.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.lyrics),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onShowLyrics) {
                        Text(text = stringResource(R.string.show))
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(8.dp)),
                ) {
                    Lyrics(
                        sliderPositionProvider = sliderPositionProvider,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    // --- Artist card -------------------------------------------------------
    artist?.let { a ->
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clickable { navController.navigate("artist/${a.id}") },
            shape = cardShape,
            colors = CardDefaults.elevatedCardColors(
                containerColor = Color(0xFF212121),
            ),
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                ) {
                    AsyncImage(
                        model = a.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Black.copy(alpha = 0.6f),
                                    0.4f to Color.Transparent,
                                ),
                            ),
                    )
                    Text(
                        text = stringResource(R.string.artists),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(15.dp),
                    )
                }
                Column(Modifier.padding(15.dp)) {
                    Text(
                        text = a.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${a.songCount} " + stringResource(R.string.songs),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.72f),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    // --- Description card --------------------------------------------------
    val vd = videoDetails
    if (vd != null) {
        val viewsText = vd.viewCount.toLongOrNull()
            ?.takeIf { it > 0 }
            ?.let { String.format("%,d", it) }
        val description = vd.shortDescription?.takeIf { it.isNotBlank() }
        if (viewsText != null || description != null) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = cardShape,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = seedColor?.copy(alpha = 0.35f) ?: Color(0xFF212121),
                ),
            ) {
                Column(Modifier.padding(15.dp)) {
                    if (viewsText != null) {
                        Text(
                            text = stringResource(R.string.song_views, viewsText),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                        )
                    }
                    if (description != null) {
                        if (viewsText != null) Spacer(Modifier.height(6.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.72f),
                        )
                    }
                }
            }
        }
    }
}