package com.muso.music.ui.player

import androidx.compose.runtime.DisposableEffect
import com.maxrave.domain.data.entities.SongEntity
import com.muso.music.utils.makeTimeString
import com.muso.music.playback.queues.Queue
import com.muso.music.extensions.toMediaItem
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.ImageBitmap
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.NavHostController
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.data.model.searchResult.songs.Thumbnail
import com.maxrave.domain.data.model.streams.TimeLine
import com.maxrave.domain.mediaservice.handler.ControlState
import com.maxrave.domain.mediaservice.handler.RepeatState
import com.maxrave.simpmusic.extension.GradientAngle
import com.maxrave.simpmusic.extension.GradientOffset
import com.maxrave.simpmusic.ui.screen.player.content.NowPlayingContentActions
import com.maxrave.simpmusic.ui.screen.player.content.NowPlayingContentState
import com.maxrave.simpmusic.ui.screen.player.content.NowPlayingContentAppleMusic
import com.maxrave.simpmusic.ui.screen.player.content.NowPlayingContentSpotify
import com.maxrave.simpmusic.ui.screen.player.content.NowPlayingContentM3Expressive
import com.maxrave.domain.data.model.metadata.Line
import com.maxrave.domain.data.model.metadata.Lyrics
import com.maxrave.simpmusic.extension.getColorFromPalette
import com.maxrave.simpmusic.viewModel.LyricsProvider
import com.maxrave.simpmusic.viewModel.NowPlayingScreenData
import com.maxrave.simpmusic.ui.icon.ArrowForwardIos
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.viewModel.UIEvent
import com.muso.music.db.entities.LyricsEntity
import com.muso.music.playback.PlayerConnection
import com.kmpalette.rememberPaletteState
import androidx.compose.ui.platform.LocalContext
import coil3.SingletonImageLoader
import coil3.request.SuccessResult
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.maxrave.simpmusic.expect.ui.toImageBitmap
import kotlinx.coroutines.flow.collectLatest
import com.muso.music.ui.component.SuiteRes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive

/**
 * MUSO SUITE HOST — the bridge that feeds the REAL SimpMusic player suite
 * (byte-for-byte UI files) with Muso's playback state.
 *
 * Everything the three styles render comes from NowPlayingContentState; every
 * touch flows back through NowPlayingContentActions into Muso's player. This
 * is the adapter layer the integration guide calls for — the UI files
 * themselves are untouched.
 */
@Composable
fun MusoSuiteHost(
    playerConnection: PlayerConnection,
    navController: NavHostController,
    playerStyle: com.muso.music.constants.PlayerStyle,
    codecLabel: String?,
    queueTitle: String?,
    onDismiss: () -> Unit,
    onShowSongInfo: () -> Unit,
    onShowAddToPlaylist: () -> Unit,
    onShowMoreSheet: () -> Unit,
    onShowMusoLyrics: () -> Unit,
) {
    // Lyrics timing offset (v2.2.0): the suite player reads the live value through
    // the DataStoreManager shim; mirror Muso's preference into it.
    val lyricsOffsetMs by com.muso.music.utils.rememberPreference(
        com.muso.music.constants.LyricsOffsetKey, defaultValue = 0,
    )
    val dsm: com.maxrave.domain.manager.DataStoreManager = org.koin.compose.koinInject()
    androidx.compose.runtime.LaunchedEffect(lyricsOffsetMs) {
        dsm.lyricsOffsetMs.value = lyricsOffsetMs
    }
    // Lyrics style follows the PLAYER STYLE (user request: every player gets
    // its own lyrics UI). The explicit Apple Music lyrics preference still
    // wins everywhere; otherwise Spotify/M3 Expressive players use their own
    // classic-suite lyrics view and the Apple Music player uses the Apple
    // lyrics view.
    val lyricsStylePref by com.muso.music.utils.rememberPreference(
        com.muso.music.constants.LyricsStyleKey, defaultValue = "1",
    )
    val lyricsPlayerStyle by com.muso.music.utils.rememberEnumPreference(
        com.muso.music.constants.PlayerStyleKey, defaultValue = com.muso.music.constants.PlayerStyle.EXPRESSIVE,
    )
    androidx.compose.runtime.LaunchedEffect(lyricsStylePref, lyricsPlayerStyle) {
        dsm.lyricsStyle.value = when {
            lyricsStylePref == com.maxrave.domain.manager.DataStoreManager.LYRICS_STYLE_APPLE_MUSIC ->
                com.maxrave.domain.manager.DataStoreManager.LYRICS_STYLE_APPLE_MUSIC
            lyricsPlayerStyle == com.muso.music.constants.PlayerStyle.IMMERSIVE ->
                com.maxrave.domain.manager.DataStoreManager.LYRICS_STYLE_APPLE_MUSIC
            else -> com.maxrave.domain.manager.DataStoreManager.LYRICS_STYLE_CLASSIC
        }
    }
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val queueWindows by playerConnection.queueWindows.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val canvasUrl by playerConnection.service.videoStreamUrl.collectAsState()

    val player = playerConnection.player

    // ---------- queue as SimpMusic Tracks ----------
    val artworkQueue = remember(queueWindows) {
        queueWindows.map { window ->
            val md = window.mediaItem.mediaMetadata
            Track(
                album = null,
                artists = md.artist?.let { a ->
                    a.split(", ").map { Artist(id = null, name = it.trim()) }
                },
                duration = null,
                durationSeconds = null,
                isAvailable = true,
                isExplicit = false,
                likeStatus = null,
                thumbnails = (md.artworkUri ?: window.mediaItem.mediaMetadata.extras?.getString("thumbnailUrl"))
                    ?.toString()?.let { hqYtThumb(it) }?.let { listOf(Thumbnail(height = 544, url = it, width = 544)) },
                title = md.title?.toString() ?: "",
                videoId = window.mediaItem.mediaId ?: "",
                videoType = null,
                category = null,
                feedbackTokens = null,
                resultType = null,
            )
        }
    }
    val currentOrderIndex = remember(artworkQueue) { player.currentMediaItemIndex.coerceIn(0, (artworkQueue.size - 1).coerceAtLeast(0)) }

    // ---------- timeline ----------
    val timelineFlow = remember { MutableStateFlow(TimeLine(0, 0, 0, loading = false)) }
    LaunchedEffect(player) {
        while (isActive) {
            timelineFlow.value = TimeLine(
                current = player.currentPosition,
                total = player.duration.takeIf { it != C.TIME_UNSET } ?: 0,
                bufferedPercent = player.bufferedPercentage,
                loading = false,
            )
            delay(250)
        }
    }
    val timelineState by timelineFlow.collectAsState()

    // ---------- slider latch ----------
    var isSliding by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    val displayedSliderValue = if (isSliding) sliderValue else {
        if (timelineState.total > 0) timelineState.current.toFloat() / timelineState.total.toFloat() * 100f else 0f
    }

    // ---------- colors: live palette from the artwork ----------
    val startColor = remember { Animatable(Color(0xFF1DB954)) }
    val endColor = remember { Animatable(Color(0xFF101010)) }
    var artworkBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val paletteState = rememberPaletteState()
    val context = LocalContext.current
    LaunchedEffect(mediaMetadata?.id, mediaMetadata?.thumbnailUrl) {
        artworkBitmap = null
        val url = hqYtThumb(mediaMetadata?.thumbnailUrl)
        if (url != null) {
            runCatching {
                val loader = SingletonImageLoader.get(context)
                // 1024, not 256: this bitmap IS the artwork the suite player
                // renders (fullscreen background + square thumb). 256 came
                // out visibly pixelated on every screen (user report).
                val result = loader.execute(
                    ImageRequest.Builder(context).data(url).size(1024).allowHardware(false).build()
                )
                (result as? SuccessResult)?.image?.toImageBitmap()
            }.getOrNull()?.let { bmp ->
                artworkBitmap = bmp
                paletteState.generate(bmp)
            }
        }
    }
    LaunchedEffect(paletteState) {
        snapshotFlow { paletteState.palette }
            .distinctUntilChanged()
            .collectLatest { palette ->
                val dominant = palette.getColorFromPalette()
                startColor.animateTo(dominant, tween(800))
                endColor.animateTo(Color(0xFF101010), tween(800))
            }
    }

    // ---------- lyrics: Muso's stored lyrics parsed into the suite model ----------
    val musoLyrics by playerConnection.currentLyrics.collectAsState()
    val lyricsData = remember(musoLyrics) {
        val raw = musoLyrics?.lyrics
        if (raw.isNullOrBlank() || raw == LyricsEntity.LYRICS_NOT_FOUND) {
            null
        } else {
            parseLrcToSuiteLines(raw)?.let { (lines, synced) ->
                NowPlayingScreenData.LyricsData(
                    lyrics = Lyrics(
                        error = false,
                        lines = lines,
                        syncType = if (synced) "LINE_SYNCED" else "UNSYNCED",
                    ),
                    translatedLyrics = null,
                    lyricsProvider = LyricsProvider.LRCLIB,
                )
            }
        }
    }

    // ---------- pager ----------
    val artworkPagerState = rememberPagerState(initialPage = currentOrderIndex) { artworkQueue.size }
    LaunchedEffect(artworkQueue.size) { }
    // Player -> pager: a track change re-anchors the pager without a fling.
    LaunchedEffect(currentOrderIndex, artworkQueue.size) {
        if (artworkQueue.isNotEmpty() && artworkPagerState.currentPage != currentOrderIndex &&
            !artworkPagerState.isScrollInProgress
        ) {
            artworkPagerState.scrollToPage(currentOrderIndex)
        }
    }
    // Pager -> player: a settled swipe changes the track.
    LaunchedEffect(artworkPagerState) {
        snapshotFlow { artworkPagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                if (artworkQueue.isNotEmpty() && page != currentOrderIndex && !artworkPagerState.isScrollInProgress) {
                    player.seekTo(page, 0)
                }
            }
    }

    // ---------- screen data ----------
    val screenData = remember(mediaMetadata, canvasUrl, queueTitle, lyricsData, artworkBitmap) {
        val md = mediaMetadata
        NowPlayingScreenData(
            playlistName = queueTitle ?: "Now Playing",
            nowPlayingTitle = md?.title ?: "",
            artistName = md?.artists?.joinToString(", ") { it.name } ?: "",
            isVideo = canvasUrl != null,
            isExplicit = false,
            thumbnailURL = hqYtThumb(md?.thumbnailUrl),
            canvasData = canvasUrl?.let { NowPlayingScreenData.CanvasData(isVideo = true, url = it) },
            lyricsData = lyricsData,
            songInfoData = null,
            bitmap = artworkBitmap,
        )
    }

    val controllerState = remember(isPlaying, currentSong?.song?.liked) {
        ControlState(
            isPlaying = isPlaying,
            isShuffle = player.shuffleModeEnabled,
            repeatState = when (player.repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatState.One
                Player.REPEAT_MODE_ALL -> RepeatState.All
                else -> RepeatState.None
            },
            isLiked = currentSong?.song?.liked == true,
            isNextAvailable = player.hasNextMediaItem(),
            isPreviousAvailable = true,
            isCrossfading = false,
            volume = player.volume,
        )
    }

    // ---------- actions ----------
    val actions = remember(playerConnection, navController) {
        NowPlayingContentActions(
            onUIEvent = { event ->
                when (event) {
                    UIEvent.PlayPause -> if (player.isPlaying) player.pause() else player.play()
                    UIEvent.Next -> player.seekToNextMediaItem()
                    UIEvent.Previous, UIEvent.SkipToPrevious -> player.seekToPreviousMediaItem()
                    UIEvent.Backward -> player.seekTo((player.currentPosition - 5000).coerceAtLeast(0))
                    UIEvent.Forward -> player.seekTo((player.currentPosition + 5000).coerceAtMost(
                        player.duration.takeIf { it != C.TIME_UNSET } ?: 0
                    ))
                    UIEvent.Stop -> { }
                    UIEvent.Shuffle -> player.shuffleModeEnabled = !player.shuffleModeEnabled
                    UIEvent.Repeat -> player.repeatMode = when (player.repeatMode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                        else -> Player.REPEAT_MODE_OFF
                    }
                    is UIEvent.UpdateProgress -> {
                        val total = player.duration.takeIf { it != C.TIME_UNSET } ?: 0
                        player.seekTo((total * event.newProgress / 100f).toLong())
                    }
                    is UIEvent.UpdateVolume -> player.volume = event.newVolume
                    UIEvent.ToggleLike -> playerConnection.toggleLike()
                }
            },
            onSeekToQueueIndex = { index -> if (index in 0 until player.mediaItemCount) player.seekTo(index, 0) },
            onArtworkBitmap = { },
            onSliderChange = { value -> isSliding = true; sliderValue = value },
            onSliderChangeFinished = {
                val total = player.duration.takeIf { it != C.TIME_UNSET } ?: 0
                if (total > 0) player.seekTo((total * sliderValue / 100f).toLong())
                isSliding = false
            },
            onToggleControls = { },
            onNavigateToArtist = {
                mediaMetadata?.artists?.firstOrNull()?.id?.let { id ->
                    navController.navigate("artist/$id")
                    onDismiss()
                }
            },
            onAddToYouTubeLiked = { },
            onShowMoreSheet = onShowMoreSheet,
            onShowQueue = { },
            onShowInfo = onShowSongInfo,
            onShowAddToPlaylist = onShowAddToPlaylist,
            onShowFullscreenLyrics = onShowMusoLyrics,
            onShowVoteDialog = { },
            onEnterFullscreenVideo = { },
            onDismiss = onDismiss,
            onToolbarVisibilityChange = { },
            onMoveQueueItem = { from, to -> if (from != to) player.moveMediaItem(from, to) },
            onRemoveQueueItem = { index -> player.removeMediaItem(index) },
        )
    }

    // The suite's fullscreen lyrics view and queue sheet read their header
    // from sharedViewModel.nowPlayingScreenData (NOT from the state we pass
    // down), so mirror the built screenData into it - without this the
    // lyrics view showed the empty initial data and lyrics never appeared.
    val sharedViewModelForScreenData: com.maxrave.simpmusic.viewModel.SharedViewModel =
        org.koin.compose.koinInject()
    LaunchedEffect(screenData) {
        sharedViewModelForScreenData.nowPlayingScreenData.value = screenData
    }

    val currentLyricLineIndex = remember(timelineState, lyricsData, lyricsOffsetMs) {
        val lines = lyricsData?.lyrics?.lines ?: return@remember -1
        if (lyricsData.lyrics.syncType == "UNSYNCED") return@remember -1
        // Playback position corrected by the user's timing offset — the index is
        // derived from position alone, so toggling the lyrics view on always lands
        // on the line that is singing right now.
        val position = timelineState.current + lyricsOffsetMs.toLong()
        var index = -1
        for ((i, line) in lines.withIndex()) {
            val start = line.startTimeMs.toLongOrNull() ?: continue
            if (start <= position) index = i else break
        }
        index
    }
    val state = remember(
        screenData, controllerState, timelineState, displayedSliderValue, currentOrderIndex,
        artworkQueue, isSliding, canvasUrl, currentLyricLineIndex,
    ) {
        NowPlayingContentState(
            screenData = screenData,
            controllerState = controllerState,
            timelineState = timelineState,
            timelineFlow = timelineFlow,
            likeStatus = currentSong?.song?.liked == true,
            castState = com.maxrave.domain.data.player.GenericCastState.NOT_CASTING,
            shouldShowVideo = canvasUrl != null,
            isUserLoggedIn = false,
            artworkQueue = artworkQueue,
            currentOrderIndex = currentOrderIndex,
            artworkPagerState = artworkPagerState,
            startColor = startColor,
            endColor = endColor,
            spotShadowColor = Color.Black,
            gradientOffset = GradientOffset(GradientAngle.CW135),
            sliderTrackColor = Color.White,
            sliderValue = displayedSliderValue,
            currentLyricLineIndex = currentLyricLineIndex,
            showControlLayout = true,
            controlLayoutAlpha = 1f,
            showHideMiddleLayout = true,
            shouldShowToolbar = false,
            isInPipMode = false,
            mainScrollState = ScrollState(0),
            isExpanded = true,
            dismissIcon = SimpIcons.ArrowForwardIos,
            audioCodecLabel = codecLabel?.substringBefore(" •"),
            videoAspectRatio = 16f / 9,
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        when (playerStyle) {
            com.muso.music.constants.PlayerStyle.CLASSIC -> NowPlayingContentSpotify(state = state, actions = actions)
            com.muso.music.constants.PlayerStyle.EXPRESSIVE -> NowPlayingContentM3Expressive(state = state, actions = actions)
            // ForceDarkContent: the Apple Music style is a black canvas by
            // design, and the suite's typo() takes its text colors from the
            // HOST theme - on Muso's light theme that meant dark-brown
            // queue/lyrics text on the black backdrop, unreadable. The
            // reference app runs this whole style inside ForceDarkContent;
            // now Muso does too (white titles, grey bodies, dark scheme).
            com.muso.music.constants.PlayerStyle.IMMERSIVE ->
                com.maxrave.simpmusic.ui.theme.ForceDarkContent {
                    NowPlayingContentAppleMusic(state = state, actions = actions)
                }
        }
    }
}

/**
 * Parses the raw lyrics string Muso stores (LRC with `[mm:ss.xx]` stamps, or
 * plain unsynced text) into the suite's [Line] model.
 *
 * @return the lines plus a flag telling whether the lyrics are timestamped
 *   ("LINE_SYNCED") or plain text ("UNSYNCED"), or null when there is nothing
 *   usable at all.
 */
private fun parseLrcToSuiteLines(data: String): Pair<List<Line>, Boolean>? {
    val stamp = Regex("""\[(\d{1,3}):(\d{1,2}(?:[.,]\d{1,3})?)]""")
    // Enhanced-LRC word timing tags: <mm:ss.xxx> before every word. They are
    // metadata, not lyric text - left in, every rendered line started with a
    // visible "<03:19.606>" (user report). Strip them all.
    val wordTag = Regex("""<\d{1,3}:\d{1,2}(?:[.,]\d{1,3})?>""")
    val timed = mutableListOf<Pair<Long, String>>()
    for (rawLine in data.lineSequence()) {
        val stamps = stamp.findAll(rawLine).toList()
        if (stamps.isEmpty()) continue
        val text = wordTag.replace(rawLine.substring(stamps.last().range.last + 1), "").trim()
        for (m in stamps) {
            val minutes = m.groupValues[1].toLong()
            val seconds = m.groupValues[2].replace(',', '.').toDouble()
            timed.add(minutes * 60_000L + (seconds * 1000).toLong() to text)
        }
    }
    if (timed.isNotEmpty()) {
        timed.sortBy { it.first }
        val lines = timed.mapIndexed { i, (start, text) ->
            val end = timed.getOrNull(i + 1)?.first ?: (start + 5_000L)
            Line(
                startTimeMs = start.toString(),
                endTimeMs = end.toString(),
                syllables = null,
                words = text.ifBlank { "♪" },
            )
        }
        return lines to true
    }
    // No timestamps: plain lyrics - every non-empty line becomes a word line.
    val words = data.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
    if (words.isEmpty()) return null
    return words.map { word ->
        Line(startTimeMs = "0", endTimeMs = "0", syllables = null, words = word)
    } to false
}


// =====================================================================================
// MUSO SUITE BRIDGE — feeds the SimpMusic SharedViewModel shim with live player state.
//
// The suite's glass navigation bar renders its own MiniPlayer from
// SharedViewModel.nowPlayingState / controllerState / timeline and sends its
// transport events through SharedViewModel.onUIEvent — this bridge is the
// single place that keeps those fed from (and wired back into) Muso's
// PlayerConnection. Host it once, next to the navigation bar.
// =====================================================================================
@Composable
fun MusoSuiteBridge(
    playerConnection: PlayerConnection,
    sharedViewModel: com.maxrave.simpmusic.viewModel.SharedViewModel =
        org.koin.compose.koinInject(),
    mediaPlayerHandler: com.maxrave.domain.mediaservice.handler.MediaPlayerHandler =
        org.koin.compose.koinInject(),
) {
    var currentQueueData by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<com.maxrave.domain.mediaservice.handler.QueueData.Data?>(null)
    }
    val player = playerConnection.player
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val shuffle by playerConnection.shuffleModeEnabled.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val liked = currentSong?.song?.liked == true

    // Player style: the suite's own NowPlayingScreen picks its content style
    // (Spotify / M3 Expressive / Apple Music) from DataStoreManager - mirror
    // Muso's PlayerStyle preference into it.
    val dsmBridge: com.maxrave.domain.manager.DataStoreManager = org.koin.compose.koinInject()
    val musoPlayerStyle by com.muso.music.utils.rememberEnumPreference(
        com.muso.music.constants.PlayerStyleKey,
        com.muso.music.constants.PlayerStyle.EXPRESSIVE,
    )
    LaunchedEffect(musoPlayerStyle) {
        dsmBridge.nowPlayingStyle.value = when (musoPlayerStyle) {
            com.muso.music.constants.PlayerStyle.EXPRESSIVE ->
                com.maxrave.domain.manager.DataStoreManager.NOW_PLAYING_STYLE_M3_EXPRESSIVE
            com.muso.music.constants.PlayerStyle.IMMERSIVE ->
                com.maxrave.domain.manager.DataStoreManager.NOW_PLAYING_STYLE_APPLE_MUSIC
            else -> com.maxrave.domain.manager.DataStoreManager.NOW_PLAYING_STYLE_SPOTIFY
        }
    }

    // --- screen data: title/artist/artwork + lyrics + canvas for the
    // reference NowPlayingScreen (and its fullscreen lyrics / queue sheets).
    // v0.5.145 moved the player to the reference's own NowPlayingScreen,
    // which reads everything from this flow - without this feed its lyrics,
    // canvas and palette would be gone. The canvas URL only appears AFTER
    // MusicService preloaded its initial segment, so the thumbnail never
    // gives way to a black frame.
    val contextBridge = LocalContext.current
    var bitmapBridge by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(mediaMetadata?.id, mediaMetadata?.thumbnailUrl) {
        bitmapBridge = null
        val url = mediaMetadata?.thumbnailUrl
        if (url != null) {
            runCatching {
                val loader = SingletonImageLoader.get(contextBridge)
                val result = loader.execute(
                    ImageRequest.Builder(contextBridge).data(url).size(1024).allowHardware(false).build()
                )
                (result as? SuccessResult)?.image?.toImageBitmap()
            }.getOrNull()?.let { bitmapBridge = it }
        }
    }
    // Per-player lyrics UI (motion of the lyrics follows the player style):
    // the Apple Music player renders the Apple Music floating-card lyrics,
    // the others the classic highlighted-line style. The user's own lyrics
    // style preference (Apple Music) wins for every player when set.
    val musoLyricsStyle by com.muso.music.utils.rememberEnumPreference(
        com.muso.music.constants.LyricsStyleKey,
        com.muso.music.constants.LyricsStyle.CLASSIC,
    )
    LaunchedEffect(musoLyricsStyle, musoPlayerStyle) {
        dsmBridge.lyricsStyle.value =
            if (musoLyricsStyle == com.muso.music.constants.LyricsStyle.APPLE_MUSIC ||
                musoPlayerStyle == com.muso.music.constants.PlayerStyle.IMMERSIVE
            ) {
                com.maxrave.domain.manager.DataStoreManager.LYRICS_STYLE_APPLE_MUSIC
            } else {
                com.maxrave.domain.manager.DataStoreManager.LYRICS_STYLE_CLASSIC
            }
    }
    val canvasUrlBridge by playerConnection.service.videoStreamUrl.collectAsState()
    val queueTitleBridge by playerConnection.queueTitle.collectAsState()
    val musoLyricsBridge by playerConnection.currentLyrics.collectAsState()
    LaunchedEffect(mediaMetadata, canvasUrlBridge, queueTitleBridge, musoLyricsBridge, bitmapBridge) {
        val raw = musoLyricsBridge?.lyrics
        val lyricsData = if (raw.isNullOrBlank() || raw == LyricsEntity.LYRICS_NOT_FOUND) {
            null
        } else {
            parseLrcToSuiteLines(raw)?.let { (lines, synced) ->
                NowPlayingScreenData.LyricsData(
                    lyrics = Lyrics(
                        error = false,
                        lines = lines,
                        syncType = if (synced) "LINE_SYNCED" else "UNSYNCED",
                    ),
                    translatedLyrics = null,
                    lyricsProvider = LyricsProvider.LRCLIB,
                )
            }
        }
        val md = mediaMetadata
        sharedViewModel.nowPlayingScreenData.value = NowPlayingScreenData(
            playlistName = queueTitleBridge ?: "Now Playing",
            nowPlayingTitle = md?.title ?: "",
            artistName = md?.artists?.joinToString(", ") { it.name } ?: "",
            isVideo = canvasUrlBridge != null,
            isExplicit = false,
            thumbnailURL = hqYtThumb(md?.thumbnailUrl),
            canvasData = canvasUrlBridge?.let { NowPlayingScreenData.CanvasData(isVideo = true, url = it) },
            lyricsData = lyricsData,
            songInfoData = null,
            bitmap = bitmapBridge,
        )
    }

    // --- events: suite -> Muso ---
    LaunchedEffect(playerConnection) {
        sharedViewModel.eventSink = { event ->
            when (event) {
                com.maxrave.simpmusic.viewModel.UIEvent.PlayPause -> if (player.isPlaying) player.pause() else player.play()
                com.maxrave.simpmusic.viewModel.UIEvent.Next -> player.seekToNextMediaItem()
                com.maxrave.simpmusic.viewModel.UIEvent.Previous, com.maxrave.simpmusic.viewModel.UIEvent.SkipToPrevious -> player.seekToPreviousMediaItem()
                com.maxrave.simpmusic.viewModel.UIEvent.Backward -> player.seekTo((player.currentPosition - 5000).coerceAtLeast(0))
                com.maxrave.simpmusic.viewModel.UIEvent.Forward -> player.seekTo((player.currentPosition + 5000).coerceAtMost(player.duration.coerceAtLeast(0)))
                com.maxrave.simpmusic.viewModel.UIEvent.Stop -> { }
                com.maxrave.simpmusic.viewModel.UIEvent.Shuffle -> player.shuffleModeEnabled = !player.shuffleModeEnabled
                com.maxrave.simpmusic.viewModel.UIEvent.Repeat -> player.repeatMode = when (player.repeatMode) {
                    androidx.media3.common.Player.REPEAT_MODE_OFF -> androidx.media3.common.Player.REPEAT_MODE_ALL
                    androidx.media3.common.Player.REPEAT_MODE_ALL -> androidx.media3.common.Player.REPEAT_MODE_ONE
                    else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                }
                is com.maxrave.simpmusic.viewModel.UIEvent.UpdateProgress -> {
                    val total = player.duration.takeIf { it > 0 } ?: 0L
                    player.seekTo((total * event.newProgress / 100f).toLong())
                }
                is com.maxrave.simpmusic.viewModel.UIEvent.UpdateVolume -> player.volume = event.newVolume
                com.maxrave.simpmusic.viewModel.UIEvent.ToggleLike -> playerConnection.toggleLike()
            }
        }
        // Reference behaviour: "stopping" from the suite never destroys the
        // queue. The glass MiniPlayer's swipe-down is right above the navbar
        // and fired far too easily during tab navigation, and the old
        // stop()+clearMediaItems() left a frozen song with dead transport
        // buttons (play/pause/next/previous do nothing on an empty queue).
        // Pausing is the most it should ever do.
        sharedViewModel.stopSink = {
            player.pause()
        }
    }
    DisposableEffect(playerConnection) {
        onDispose {
            sharedViewModel.eventSink = null
            sharedViewModel.stopSink = null
        }
    }

    // --- state: Muso -> suite ---
    LaunchedEffect(mediaMetadata, isPlaying, shuffle, repeatMode, liked) {
        val songEntity = mediaMetadata?.let { md ->
            SongEntity(
                videoId = md.id,
                albumId = md.album?.id,
                albumName = md.album?.title,
                artistId = md.artists.mapNotNull { it.id },
                artistName = md.artists.map { it.name },
                duration = md.duration.toLong().let { com.muso.music.utils.makeTimeString(it) },
                durationSeconds = md.duration,
                isAvailable = true,
                isExplicit = false,
                likeStatus = if (liked) "LIKE" else "INDIFFERENT",
                thumbnails = hqYtThumb(md.thumbnailUrl),
                title = md.title,
                videoType = "SONG",
                category = null,
                resultType = null,
            )
        }
        val nowPlaying = if (mediaMetadata == null) null else
            com.maxrave.domain.mediaservice.handler.NowPlayingTrackState(
                mediaItem = com.maxrave.domain.data.player.GenericMediaItem(
                    mediaId = mediaMetadata!!.id,
                    uri = null,
                    metadata = com.maxrave.domain.data.player.GenericMediaMetadata(
                        title = mediaMetadata!!.title,
                        artist = mediaMetadata!!.artists.joinToString { it.name },
                        artworkUri = mediaMetadata!!.thumbnailUrl,
                    ),
                ),
                track = null,
                songEntity = songEntity,
            )
        sharedViewModel.nowPlayingState.value = nowPlaying
        mediaPlayerHandler.nowPlayingState.value =
            nowPlaying ?: com.maxrave.domain.mediaservice.handler.NowPlayingTrackState.initial()
        val controlStateValue = com.maxrave.domain.mediaservice.handler.ControlState(
            isPlaying = isPlaying,
            isShuffle = shuffle,
            repeatState = when (repeatMode) {
                androidx.media3.common.Player.REPEAT_MODE_ONE -> com.maxrave.domain.mediaservice.handler.RepeatState.One
                androidx.media3.common.Player.REPEAT_MODE_ALL -> com.maxrave.domain.mediaservice.handler.RepeatState.All
                else -> com.maxrave.domain.mediaservice.handler.RepeatState.None
            },
            isLiked = liked,
            isNextAvailable = player.hasNextMediaItem(),
            isPreviousAvailable = player.hasPreviousMediaItem(),
            isCrossfading = false,
            volume = player.volume,
        )
        sharedViewModel.controllerState.value = controlStateValue
        mediaPlayerHandler.controlState.value = controlStateValue
        sharedViewModel.isServiceRunning = true
    }

    // --- timeline poll ---
    LaunchedEffect(player) {
        while (isActive) {
            val total = player.duration.takeIf { it != androidx.media3.common.C.TIME_UNSET } ?: 0L
            sharedViewModel.timeline.value = com.maxrave.domain.data.model.streams.TimeLine(
                current = player.currentPosition.coerceAtLeast(0),
                total = total,
                bufferedPercent = player.bufferedPercentage,
                loading = player.playbackState == androidx.media3.common.Player.STATE_BUFFERING,
            )
            kotlinx.coroutines.delay(250)
        }
    }

    // --- playback commands: the suite's MediaViewModels -> Muso's player ---
    LaunchedEffect(playerConnection) {
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
        mediaPlayerHandler.commandSink = { command ->
            scope.launch {
                when (command) {
                    com.maxrave.domain.mediaservice.handler.MediaPlayerHandler.Command.Reset -> {
                        player.stop()
                        player.clearMediaItems()
                    }
                    is com.maxrave.domain.mediaservice.handler.MediaPlayerHandler.Command.SetQueueData -> {
                        currentQueueData = command.queueData
                    }
                    is com.maxrave.domain.mediaservice.handler.MediaPlayerHandler.Command.LoadItem -> {
                        val track = command.track as? com.maxrave.domain.data.model.browse.album.Track
                        if (track != null) {
                            val list = currentQueueData?.listTracks ?: listOf(track)
                            val index = command.index
                                ?: list.indexOfFirst { it.videoId == track.videoId }.coerceAtLeast(0)
                            playSuiteTracks(playerConnection, list, index, currentQueueData?.playlistName)
                        }
                    }
                    is com.maxrave.domain.mediaservice.handler.MediaPlayerHandler.Command.ShufflePlaylist -> {
                        val list = currentQueueData?.listTracks?.shuffled()
                        if (!list.isNullOrEmpty()) {
                            val index = command.firstPlayIndex.coerceIn(0, list.size - 1)
                            playSuiteTracks(playerConnection, list, index, currentQueueData?.playlistName)
                        }
                    }
                    is com.maxrave.domain.mediaservice.handler.MediaPlayerHandler.Command.PlayNext -> {
                        playerConnection.playNext(listOf(command.track.toMusMediaItem()))
                    }
                    is com.maxrave.domain.mediaservice.handler.MediaPlayerHandler.Command.AddToQueue -> {
                        playerConnection.addToQueue(command.tracks.map { it.toMusMediaItem() })
                    }
                    // Queue reordering in the suite's queue sheet is not wired through to
                    // Muso's queue yet; Muso's own queue sheet remains authoritative.
                    is com.maxrave.domain.mediaservice.handler.MediaPlayerHandler.Command.RemoveMediaItem,
                    is com.maxrave.domain.mediaservice.handler.MediaPlayerHandler.Command.PlayMediaItemInMediaSource,
                    is com.maxrave.domain.mediaservice.handler.MediaPlayerHandler.Command.Swap,
                    is com.maxrave.domain.mediaservice.handler.MediaPlayerHandler.Command.MoveItem,
                    -> { }
                }
            }
        }
    }
}

/** Play the suite's track list as a Muso queue starting at [startIndex]. */
private fun playSuiteTracks(
    playerConnection: PlayerConnection,
    tracks: List<com.maxrave.domain.data.model.browse.album.Track>,
    startIndex: Int,
    playlistName: String?,
) {
    val items = tracks.map { it.toMusMediaItem() }
    if (items.isEmpty()) return
    playerConnection.playQueue(SuiteTrackQueue(items, startIndex, playlistName))
}

/** A static Muso queue built from an already-loaded suite track list. */
private class SuiteTrackQueue(
    private val items: List<androidx.media3.common.MediaItem>,
    private val startIndex: Int,
    private val queueTitle: String?,
) : Queue {
    override val preloadItem: com.muso.music.models.MediaMetadata? = null

    override suspend fun getInitialStatus(): Queue.Status = Queue.Status(
        title = queueTitle,
        items = items,
        mediaItemIndex = startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
    )

    override fun hasNextPage(): Boolean = false

    override suspend fun nextPage(): List<androidx.media3.common.MediaItem> = emptyList()
}

/** Domain Track -> Muso MediaItem. */
private fun com.maxrave.domain.data.model.browse.album.Track.toMusMediaItem(): androidx.media3.common.MediaItem {
    val metadata = com.muso.music.models.MediaMetadata(
        id = videoId,
        title = title,
        artists = (artists ?: emptyList()).map {
            com.muso.music.models.MediaMetadata.Artist(id = it.id, name = it.name)
        },
        duration = durationSeconds ?: 0,
        thumbnailUrl = thumbnails?.lastOrNull()?.url,
        explicit = isExplicit,
    )
    return metadata.toMediaItem()
}

// YouTube thumbnail URLs come in many low-res forms (w544, hqdefault, ...);
// the highest-resolution variant of /vi/<id>/ art is maxresdefault. Upgrading
// at the source means the player artwork, palette and every queue/list render
// from the full-resolution image, not just the ones that pass through Coil's
// interceptor.
private fun hqYtThumb(url: String?): String? {
    if (url == null) return null
    val i = url.indexOf("i.ytimg.com/vi/")
    if (i < 0) return url
    val id = url.substringAfter("i.ytimg.com/vi/").substringBefore("/")
    if (id.isBlank()) return url
    return "https://i.ytimg.com/vi/$id/maxresdefault.jpg"
}
