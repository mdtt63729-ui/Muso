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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import androidx.lifecycle.compose.collectAsStateWithLifecycle


/*
 * Muso -> SimpMusic suite bridge.
 *
 * Extracted from the old MusoSuiteHost.kt when that file was deleted: the host composable was
 * dead (nothing called it), but [MusoSuiteBridge] is NOT - MusoNavbarHost feeds the suite's
 * shared state through it, and the glass bottom bar's MiniPlayer reads that state. Deleting the
 * whole file took the bridge with it and broke the build.
 *
 * The seven ArchiveTune player styles are gone from PlayerStyle and DataStoreManager, so their
 * branches are dropped here too; the three that remain map exactly as before.
 */
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
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    val shuffle by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()
    val repeatMode by playerConnection.repeatMode.collectAsStateWithLifecycle()
    val currentSong by playerConnection.currentSong.collectAsStateWithLifecycle(initialValue = null)
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
    // The lyrics animation-style picker's IMMERSIVE entry also selects the Apple-Music sheet,
    // so it drives the suite's lyrics style alongside the player style.
    val lyricsAnimationStyleBridge by com.muso.music.utils.rememberEnumPreference(
        com.muso.music.constants.LyricsAnimationStyleKey,
        com.muso.music.constants.LyricsAnimationStyle.ENHANCED,
    )
    LaunchedEffect(musoLyricsStyle, musoPlayerStyle, lyricsAnimationStyleBridge) {
        dsmBridge.lyricsStyle.value =
            if (musoLyricsStyle == com.muso.music.constants.LyricsStyle.APPLE_MUSIC ||
                musoPlayerStyle == com.muso.music.constants.PlayerStyle.IMMERSIVE ||
                lyricsAnimationStyleBridge == com.muso.music.constants.LyricsAnimationStyle.IMMERSIVE
            ) {
                com.maxrave.domain.manager.DataStoreManager.LYRICS_STYLE_APPLE_MUSIC
            } else {
                com.maxrave.domain.manager.DataStoreManager.LYRICS_STYLE_CLASSIC
            }
    }
    // Round 176: lyrics sync-offset bridge. This sync used to live only in the
    // dead MusoSuiteHost composable, so the Lyrics sync offset setting never
    // reached the suite lyrics view that actually reads dsm.lyricsOffsetMs.
    val lyricsOffsetMsBridge by com.muso.music.utils.rememberPreference(
        com.muso.music.constants.LyricsOffsetKey,
        defaultValue = 0,
    )
    androidx.compose.runtime.LaunchedEffect(lyricsOffsetMsBridge) {
        dsmBridge.lyricsOffsetMs.value = lyricsOffsetMsBridge
    }

    val canvasUrlBridge by playerConnection.service.videoStreamUrl.collectAsStateWithLifecycle()
    val queueTitleBridge by playerConnection.queueTitle.collectAsStateWithLifecycle()
    val musoLyricsBridge by playerConnection.currentLyrics.collectAsStateWithLifecycle()
    LaunchedEffect(mediaMetadata, canvasUrlBridge, queueTitleBridge, musoLyricsBridge, bitmapBridge) {
        val raw = musoLyricsBridge?.lyrics
        val lyricsData = if (raw.isNullOrBlank() || raw == LyricsEntity.LYRICS_NOT_FOUND) {
            null
        } else {
            parseLrcToSuiteLines(raw)?.let { (lines, synced, rich) ->
                NowPlayingScreenData.LyricsData(
                    lyrics = Lyrics(
                        error = false,
                        lines = lines,
                        syncType = when {
                            rich -> "RICH_SYNCED"
                            synced -> "LINE_SYNCED"
                            else -> "UNSYNCED"
                        },
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
            // Round 188: the fetch has concluded without lyrics only when the service
            // published the NOT_FOUND marker - a null raw while the request is still
            // running must keep reading as "loading", not "unavailable".
            lyricsUnavailable = raw == LyricsEntity.LYRICS_NOT_FOUND,
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
    // The UI interpolates the playhead from the real PlayerConnection when it needs
    // frame-level lyric timing. The shared timeline is therefore a coarse UI snapshot,
    // not a 4 Hz animation clock. Updating it at 500 ms cuts background recompositions
    // in half while keeping elapsed/remaining labels and mini-player progress smooth.
    LaunchedEffect(player) {
        while (isActive) {
            val total = player.duration.takeIf { it != androidx.media3.common.C.TIME_UNSET } ?: 0L
            sharedViewModel.timeline.value = com.maxrave.domain.data.model.streams.TimeLine(
                current = player.currentPosition.coerceAtLeast(0),
                total = total,
                bufferedPercent = player.bufferedPercentage,
                loading = player.playbackState == androidx.media3.common.Player.STATE_BUFFERING,
            )
            kotlinx.coroutines.delay(500)
        }
    }

    // --- playback commands: the suite's MediaViewModels -> Muso's player ---
    LaunchedEffect(playerConnection) {
        // Use the effect's own scope. The previous standalone CoroutineScope was never
        // cancelled, so queued playback commands could outlive this composable.
        val scope = this
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
    // Google-hosted song art (YouTube Music): the database stores a small
    // variant (typically =w544-h544-...), and the suite's player renders with
    // coil3, which does NOT go through the coil2 interceptor - so these URLs
    // loaded at 544px and looked pixelated on the big artwork. Bump the size
    // to 1200px, keeping the original crop/rounding flags so the same master
    // is served.
    if (url.contains("googleusercontent.com/") || url.contains("ggpht.com")) {
        val base = url.substringBefore("?")
        val wh = Regex("""=w(\d+)-h(\d+)([^=]*)$""").find(base)
        if (wh != null) {
            val width = wh.groupValues[1].toIntOrNull() ?: 0
            if (width in 1 until 1200) {
                return base.replaceRange(wh.range, "=w1200-h1200" + wh.groupValues[3])
            }
        }
        return url
    }
    val i = url.indexOf("i.ytimg.com/vi/")
    if (i < 0) return url
    val id = url.substringAfter("i.ytimg.com/vi/").substringBefore("/")
    if (id.isBlank()) return url
    return "https://i.ytimg.com/vi/$id/maxresdefault.jpg"
}

