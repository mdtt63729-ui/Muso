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
                }

                else -> {}
            }

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding + 4.dp)
            ) {
                PositionTimeText(
                    positionProvider = { position },
                    sliderPosition = sliderPosition,
                    makeText = { makeTimeString(it) },
                    style = MaterialTheme.typography.labelMedium,
                    color = secondaryText,
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

                PositionTimeText(
                    positionProvider = { position },
                    sliderPosition = sliderPosition,
                    makeText = {
                        if (duration != C.TIME_UNSET) "-" + makeTimeString((duration - it).coerceAtMost(duration)) else ""
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = secondaryText,
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
                    iconRes = R.drawable.queue_music,
                    label = stringResource(R.string.queue),
                    active = false,
                    onVideo = onVideo,
                    onClick = { queueSheetState.expandSoft() },
                )
            }
        }

        var showSongInfoDialog by rememberSaveable { mutableStateOf(false) }
        var showAddToPlaylistDialog by rememberSaveable { mutableStateOf(false) }
        val database = LocalDatabase.current

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

            // --- Dock: Lyrics | Queue - 40dp circles, light pill while active ---
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
                        .background(if (showLyrics) activePillContainer else Color.Transparent)
                        .clickable { onShowLyricsChange(!showLyrics) },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.lyrics),
                        contentDescription = null,
                        tint = if (showLyrics) activePillContent else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(22.dp),
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { queueSheetState.expandSoft() },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.queue_music),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
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

        if (playerBackgroundStyle == PlayerBackgroundStyle.BLURRED_ARTWORK &&
            playerStyle != PlayerStyle.APPLE &&
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
        // video output fullscreen behind everything. MusicService picks a muxed
        // format while "show video in player" is on; there is no second player
        // to keep in sync anymore.
        val isVideoStream by playerConnection.service.isVideoPlayback.collectAsState()
        LaunchedEffect(videoEnabled, isVideoStream) {
            videoActive = videoEnabled && isVideoStream
        }
        if (videoEnabled) {
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
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

                    // === Echo Nightly-style gesture: swipe the artwork left/right to
                    // skip, with the artwork tracking the finger (velocity comes free with
                    // the fling: release past a quarter of the width triggers the skip).
                    var swipeOffset by remember { mutableStateOf(0f) }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = if (playerStyle == PlayerStyle.APPLE) Alignment.TopCenter else Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = artworkAlpha }
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
                        // SimpMusic-style fullscreen video: while the stream plays the
                        // video fills the whole area behind the (faded-out) artwork and
                        // the controls overlay the BOTTOM of the full screen over the
                        // scrim - no mini-player padding, no split halves.
                        if (videoVisible) {
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
                        }
                    }

                    if (!videoVisible && playerStyle == PlayerStyle.EXPRESSIVE) {
                        // SimpMusic M3 Expressive: the current lyric line sits centered
                        // in the gap between the artwork card and the info block.
                        val lyricsEntity by playerConnection.currentLyrics.collectAsState(initial = null)
                        val m3eLyricsText = remember(lyricsEntity) { lyricsEntity?.lyrics }
                        val m3eLines = remember(m3eLyricsText) {
                            when {
                                m3eLyricsText == null || m3eLyricsText == LYRICS_NOT_FOUND -> emptyList()
                                m3eLyricsText.trimStart().startsWith("<?xml") || m3eLyricsText.trimStart().startsWith("<tt") ->
                                    listOf(HEAD_LYRICS_ENTRY) + TTMLParser.parseTTML(m3eLyricsText).map {
                                        LyricsEntry((it.startTime * 1000).toLong(), it.text)
                                    }
                                m3eLyricsText.startsWith("[") -> listOf(HEAD_LYRICS_ENTRY) + parseLyrics(m3eLyricsText)
                                else -> m3eLyricsText.lines().mapIndexed { index, line -> LyricsEntry(index * 100L, line) }
                            }
                        }
                        // derivedStateOf: re-evaluated per position tick internally, but
                        // readers only recompose when the resulting LINE actually changes.
                        val m3eCurrentLine by remember(m3eLines) {
                            derivedStateOf {
                                if (m3eLines.isEmpty()) ""
                                else m3eLines.getOrNull(findCurrentLineIndex(m3eLines, position))
                                    ?.takeIf { it.text.isNotEmpty() }?.text ?: ""
                            }
                        }
                        Crossfade(
                            targetState = m3eCurrentLine,
                            animationSpec = tween(durationMillis = 300),
                            label = "m3eInlineLyric",
                        ) { lineText ->
                            Text(
                                text = lineText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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

                    if (!videoVisible) {
                        PlayerControls(
                            videoActive = false,
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
        }

        // No collapsed-queue peek bar: the queue sheet only composes once it is
        // actually being pulled up (a queue button opens it via expandSoft()).
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
