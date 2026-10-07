package com.muso.music.playback

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.content.Intent
import android.database.SQLException
import android.media.audiofx.AudioEffect
import android.net.ConnectivityManager
import android.os.Binder
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_POSITION_DISCONTINUITY
import androidx.media3.common.Player.EVENT_TIMELINE_CHANGED
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.Player.STATE_READY
import androidx.media3.common.Timeline
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.models.SongItem
import com.zionhuang.innertube.models.WatchEndpoint
import com.zionhuang.innertube.models.response.PlayerResponse
import com.muso.music.MainActivity
import com.muso.music.R
import com.muso.music.constants.AudioNormalizationKey
import com.muso.music.constants.ShowVideoInPlayerKey
import com.muso.music.constants.VideoQuality
import com.muso.music.constants.VideoQualityKey
import com.muso.music.constants.AudioQuality
import com.muso.music.constants.AudioQualityKey
import com.muso.music.constants.itagPreference
import com.muso.music.constants.LoudnessPreset
import com.muso.music.constants.LoudnessPresetKey
import com.muso.music.constants.HistoryDurationKey
import com.muso.music.constants.PreventDuplicateTracksKey
import com.muso.music.constants.PauseOnMuteKey
import com.muso.music.constants.DataSaverKey
import com.muso.music.constants.AudioOffloadKey
import android.database.ContentObserver
import android.media.AudioManager
import com.muso.music.constants.AutoLoadMoreKey
import com.muso.music.constants.EndlessQueueKey
import com.muso.music.constants.AutoSkipNextOnErrorKey
import com.muso.music.constants.AutoDownloadLikedSongsKey
import com.muso.music.constants.DiscordTokenKey
import com.muso.music.constants.EnableDiscordRPCKey
import com.muso.music.constants.HideExplicitKey
import com.muso.music.constants.MediaSessionConstants.CommandToggleLibrary
import com.muso.music.constants.MediaSessionConstants.CommandToggleLike
import com.muso.music.constants.MediaSessionConstants.CommandToggleRepeatMode
import com.muso.music.constants.MediaSessionConstants.CommandToggleShuffle
import com.muso.music.constants.PauseListenHistoryKey
import com.muso.music.constants.PersistentQueueKey
import com.muso.music.constants.CrossfadeEnabledKey
import com.muso.music.constants.CrossfadeDurationKey
import com.muso.music.constants.PlayerVolumeKey
import com.muso.music.constants.RepeatModeKey
import com.muso.music.constants.PreloadLyricsKey
import com.muso.music.constants.PreloadNextSongKey
import com.muso.music.constants.AutomixKey
import com.muso.music.constants.SpatialAudioKey
import com.muso.music.constants.ShuffleModeKey
import com.muso.music.constants.SongSortType
import com.muso.music.constants.ShowLyricsKey
import com.muso.music.constants.SkipSilenceKey
import com.muso.music.db.MusicDatabase
import com.muso.music.db.entities.SongEntity
import com.muso.music.db.entities.Event
import com.muso.music.db.entities.FormatEntity
import com.muso.music.db.entities.LyricsEntity
import com.muso.music.db.entities.RelatedSongMap
import com.muso.music.di.DownloadCache
import com.muso.music.di.PlayerCache
import com.muso.music.extensions.SilentHandler
import com.muso.music.extensions.collect
import com.muso.music.extensions.collectLatest
import com.muso.music.extensions.currentMetadata
import com.muso.music.extensions.findNextMediaItemById
import com.muso.music.extensions.mediaItems
import com.muso.music.extensions.metadata
import com.muso.music.extensions.toMediaItem
import com.muso.music.lyrics.LyricsHelper
import com.muso.music.models.PersistQueue
import com.muso.music.models.toMediaMetadata
import com.muso.music.playback.queues.EmptyQueue
import com.muso.music.playback.queues.ListQueue
import com.muso.music.playback.queues.Queue
import com.muso.music.playback.queues.YouTubeQueue
import com.muso.music.playback.queues.filterExplicit
import com.muso.music.utils.CoilBitmapLoader
import com.muso.music.utils.DiscordRPC
import com.muso.music.utils.dataStore
import com.muso.music.utils.enumPreference
import com.muso.music.utils.preference
import com.muso.music.utils.get
import com.muso.music.utils.isInternetAvailable
import com.muso.music.utils.reportException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.pow
import kotlin.time.Duration.Companion.seconds


@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@AndroidEntryPoint
class MusicService : MediaLibraryService(),
    Player.Listener,
    PlaybackStatsListener.Callback {
    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var downloadUtil: DownloadUtil

    @Inject
    lateinit var lyricsHelper: LyricsHelper

    @Inject
    lateinit var mediaLibrarySessionCallback: MediaLibrarySessionCallback

    private var scope = CoroutineScope(Dispatchers.Main) + Job()
    private val binder = MusicBinder()

    private lateinit var connectivityManager: ConnectivityManager

    private val audioQuality by enumPreference(this, AudioQualityKey, AudioQuality.HIGH_OPUS)

    // Echo Player and Audio settings
    private val dataSaver by preference(this, DataSaverKey, false)
    private val showVideoInPlayer by preference(this, ShowVideoInPlayerKey, true)
    private val videoQuality by enumPreference(this, VideoQualityKey, VideoQuality.Q720)

    /** While "show video in player" is on and the current song has a video: the
     * URL of that video stream, for the player's fullscreen canvas surface (a
     * muted, looping highlight that runs independently of the audio). Null when
     * the setting is off or the song has no video - the thumbnail then stays. */
    val videoStreamUrl = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private var volumeObserver: ContentObserver? = null

    private var currentQueue: Queue = EmptyQueue
    var queueTitle: String? = null

    val currentMediaMetadata = MutableStateFlow<com.muso.music.models.MediaMetadata?>(null)

    /** Own scope for the canvas-video resolver: it survives playback-scope
     *  recreation and its work never competes with the audio path. */
    private val canvasScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // SimpMusic canvas: the video URL is resolved for EVERY song,
        // independently of the audio path - downloaded/cached songs skip the
        // audio resolver, so it could never publish their URL and the canvas
        // kept playing the PREVIOUS song's video. Clearing first guarantees a
        // stale video can never bleed into the next song.
        canvasScope.launch {
            currentMediaMetadata.collectLatest { mediaMetadata ->
                if (mediaMetadata == null || !showVideoInPlayer) {
                    videoStreamUrl.value = null
                } else {
                    videoStreamUrl.value = null
                    val url = resolveCanvasVideoUrl(mediaMetadata)
                    // PRD (text-2.txt) §14/§30: preload the initial segment
                    // BEFORE publishing the URL, so the player renders its
                    // first frame from cache - the thumbnail stays until the
                    // video can really appear, then the crossfade swaps it.
                    // No black flash, no spinner, no stall.
                    if (url != null) {
                        // Preload is best-effort: a failed warm-up must never cost
                        // the user the video itself (that is why videos stopped
                        // appearing at all - the old HTTP client used by the
                        // preloader could not open googlevideo URLs behind the
                        // proxy). Two quick tries with the proxied client, then
                        // publish anyway: the player streams the rest fine.
                        if (!preloadCanvasSegment(url) && !preloadCanvasSegment(url)) {
                            android.util.Log.w("MusicService", "canvas preload failed; publishing unwarmed")
                        }
                        videoStreamUrl.value = url
                    }
                }
            }
        }
    }
    private val currentSong = currentMediaMetadata.flatMapLatest { mediaMetadata ->
        database.song(mediaMetadata?.id)
    }.stateIn(scope, SharingStarted.Lazily, null)
    private val currentFormat = currentMediaMetadata.flatMapLatest { mediaMetadata ->
        database.format(mediaMetadata?.id)
    }

    /** In-memory throttle for retrying LYRICS_NOT_FOUND rows (see the lyrics collector). */
    private val lyricsNotFoundRetries = java.util.concurrent.ConcurrentHashMap<String, Long>()

    // Endless queue (user request): the queue sheet's Endless queue switch used
    // to write into a no-op stub, so it never did anything. The preference is
    // now real (EndlessQueueKey); when enabled, a radio tail of similar songs
    // is appended before the queue runs out so playback never stops.
    private var endlessQueueEnabled = true
    private val endlessQueueAppendedIds = mutableSetOf<String>()
    private var endlessQueueJob: Job? = null
    private var endlessRadioQueue: YouTubeQueue? = null

    private val normalizeFactor = MutableStateFlow(1f)
    val playerVolume = MutableStateFlow(dataStore.get(PlayerVolumeKey, 1f).coerceIn(0f, 1f))

    // Crossfade (fade-based smooth transitions): the volume pipeline multiplies a fade
    // factor, so fades never fight the volume slider or audio normalization.
    private val crossfadeFactor = MutableStateFlow(1f)
    private var crossfadeEnabled = false
    private var crossfadeDuration = 4
    private var isCrossfadingOut = false
    private var fadeJob: Job? = null

    lateinit var sleepTimer: SleepTimer

    @Inject
    @PlayerCache
    lateinit var playerCache: SimpleCache

    @Inject
    @DownloadCache
    lateinit var downloadCache: SimpleCache

    lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaLibrarySession

    private var isAudioEffectSessionOpened = false

    private var audioEffectsManager: AudioEffectsManager? = null

    private var discordRpc: DiscordRPC? = null

    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(this, { NOTIFICATION_ID }, CHANNEL_ID, R.string.music_player)
                .apply {
                    setSmallIcon(R.drawable.small_icon)
                }
        )
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(createMediaSourceFactory())
            .setRenderersFactory(createRenderersFactory())
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(), true
            )
            .setSeekBackIncrementMs(5000)
            .setSeekForwardIncrementMs(5000)
            .build()
            .apply {
                addListener(this@MusicService)
                sleepTimer = SleepTimer(scope, this)
                addListener(sleepTimer)
                addAnalyticsListener(PlaybackStatsListener(false, this@MusicService))
            }
        // Publish the real player to the suite: its video surface and
        // subtitle view resolve the "mainPlayer" qualifier from Koin.
        com.muso.music.suite.SuitePlayerRegistry.player = player

        audioEffectsManager = AudioEffectsManager(this, player).also { it.start() }
        mediaLibrarySessionCallback.apply {
            toggleLike = ::toggleLike
            toggleLibrary = ::toggleLibrary
        }
        mediaSession = MediaLibrarySession.Builder(this, player, mediaLibrarySessionCallback)
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setBitmapLoader(CoilBitmapLoader(this, scope))
            .build()
        player.repeatMode = dataStore.get(RepeatModeKey, REPEAT_MODE_OFF)
        // Publish the button layout immediately: the media notification
        // controller connects when the session is created, and controllers
        // that connect before the first playback-state change need the
        // preferences with their initial connection result.
        updateNotification()

        // Echo Player and Audio: prune old listening history once at startup.
        scope.launch(Dispatchers.IO) {
            val hours = dataStore.get(HistoryDurationKey, 0)
            if (hours > 0) {
                database.query {
                    deleteEventsBefore(LocalDateTime.now().minusHours(hours.toLong()))
                }
            }
        }

        // Echo Player and Audio: pause when the media stream is muted (volume 0).
        volumeObserver = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                if (!dataStore.get(PauseOnMuteKey, false)) return
                val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager ?: return
                if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
                    player.pause()
                }
            }
        }
        contentResolver.registerContentObserver(
            android.provider.Settings.System.getUriFor("volume_music_speaker"),
            false,
            volumeObserver!!,
        )
        // SimpMusic-style "save shuffle and repeat mode": both survive a restart.
        player.shuffleModeEnabled = dataStore.get(ShuffleModeKey, false)

        // Keep a connected controller so that notification works
        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({ controllerFuture.get() }, MoreExecutors.directExecutor())

        connectivityManager = getSystemService()!!

        // === SimpMusic-style auto-download: keep every liked song available offline.
        // A song is only queued when the download manager has never seen it, so failed
        // or user-removed downloads are never retried behind the user's back, and the
        // loop always converges (queued ids immediately appear in the downloads map).
        scope.launch(Dispatchers.IO) {
            dataStore.data.map { it[AutoDownloadLikedSongsKey] == true }
                .distinctUntilChanged()
                .collectLatest { enabled ->
                    if (!enabled) return@collectLatest
                    val inFlight = mutableSetOf<String>()
                    combine(
                        database.likedSongs(SongSortType.CREATE_DATE, true),
                        downloadUtil.downloads,
                    ) { songs, downloads ->
                        songs.filter { it.id !in downloads && it.id !in inFlight }
                    }.collect { pending ->
                        pending.forEach { song ->
                            inFlight += song.id
                            val downloadRequest = DownloadRequest.Builder(song.id, song.id.toUri())
                                .setCustomCacheKey(song.id)
                                .setData(song.song.title.toByteArray())
                                .build()
                            DownloadService.sendAddDownload(
                                this@MusicService,
                                ExoDownloadService::class.java,
                                downloadRequest,
                                false,
                            )
                        }
                    }
                }
        }

        combine(playerVolume, normalizeFactor, crossfadeFactor) { playerVolume, normalizeFactor, crossfadeFactor ->
            playerVolume * normalizeFactor * crossfadeFactor
        }.collectLatest(scope) {
            player.volume = it
        }

        // Crossfade settings
        scope.launch {
            dataStore.data.collect { prefs ->
                crossfadeEnabled = prefs[CrossfadeEnabledKey] ?: false
                crossfadeDuration = (prefs[CrossfadeDurationKey] ?: 4).coerceIn(1, 12)
                if (!crossfadeEnabled) {
                    isCrossfadingOut = false
                    fadeJob?.cancel()
                    crossfadeFactor.value = 1f
                }
            }
        }

        // Crossfade watcher: during the last N seconds of a track the volume eases down;
        // the fade-in after every transition brings it back up. Manual skips mid-track
        // are unaffected (the factor is already 1).
        scope.launch {
            while (isActive) {
                delay(100)
                if (!crossfadeEnabled || crossfadeDuration <= 0) continue
                val durationMs = player.duration
                if (durationMs <= 0) continue
                val remainingMs = durationMs - player.currentPosition
                if (player.playWhenReady && player.playbackState == STATE_READY &&
                    remainingMs > 0 && remainingMs <= crossfadeDuration * 1000L
                ) {
                    isCrossfadingOut = true
                    fadeJob?.cancel()
                    crossfadeFactor.value = (remainingMs.toFloat() / (crossfadeDuration * 1000f)).coerceIn(0f, 1f)
                } else if (isCrossfadingOut && remainingMs > crossfadeDuration * 1000L) {
                    // user seeked backwards - back to full volume
                    isCrossfadingOut = false
                    crossfadeFactor.value = 1f
                }
            }
        }

        playerVolume.debounce(1000).collect(scope) { volume ->
            dataStore.edit { settings ->
                settings[PlayerVolumeKey] = volume
            }
        }

        currentSong.debounce(1000).collect(scope) { song ->
            updateNotification()
            if (song != null) {
                discordRpc?.updateSong(song)
            } else {
                discordRpc?.closeRPC()
            }
        }

        // Lyrics are fetched for EVERY song and stored in the local DB so they
        // also load offline. Offline, the fetch is skipped entirely: writing
        // LYRICS_NOT_FOUND without a connection would permanently mark the
        // song as lyric-less even after the network returns. A stale NOT_FOUND
        // row is retried once the connection is back.
        currentMediaMetadata.distinctUntilChangedBy { it?.id }.collectLatest(scope) { mediaMetadata ->
            if (mediaMetadata != null) {
                val existingLyrics = database.lyrics(mediaMetadata.id).first()
                val online = isInternetAvailable(this@MusicService)
                // Offline: never fetch and never write - a LYRICS_NOT_FOUND row
                // written without a connection would permanently mark the song
                // as lyric-less. A stale NOT_FOUND row is retried at most once
                // every 24 hours while online (new lyrics can appear at
                // providers over time); retry timestamps live in memory so the
                // database schema stays untouched.
                val now = System.currentTimeMillis()
                val lastNotFoundRetry = lyricsNotFoundRetries[mediaMetadata.id] ?: 0L
                val needsFetch =
                    online && (
                        existingLyrics == null ||
                            (
                                existingLyrics.lyrics == LyricsEntity.LYRICS_NOT_FOUND &&
                                    lastNotFoundRetry < now - LYRICS_NOT_FOUND_RETRY_MS
                                )
                    )
                if (needsFetch) {
                    if (existingLyrics != null) {
                        lyricsNotFoundRetries[mediaMetadata.id] = now
                    }
                    val lyrics = lyricsHelper.getLyrics(mediaMetadata)
                    if (lyrics != LyricsEntity.LYRICS_NOT_FOUND || existingLyrics == null) {
                        database.query {
                            upsert(
                                LyricsEntity(
                                    id = mediaMetadata.id,
                                    lyrics = lyrics
                                )
                            )
                        }
                    }
                }
            }
        }

        dataStore.data
            .map { it[EndlessQueueKey] ?: true }
            .distinctUntilChanged()
            .collect(scope) { enabled ->
                endlessQueueEnabled = enabled
                if (!enabled) {
                    endlessRadioQueue = null
                    endlessQueueAppendedIds.clear()
                    if (::player.isInitialized) trimQueueWhenEndlessDisabled()
                } else if (::player.isInitialized) {
                    maybeExtendEndlessQueue(force = true)
                }
            }

        // Full-song streaming cache (user request): every song that STARTS
        // PLAYING is fully pulled into the player cache in the background, so
        // it keeps playing offline and shows up in the library's Recently
        // Played section. Skipped entirely in data saver mode.
        currentMediaMetadata.distinctUntilChangedBy { it?.id }.collectLatest(scope) { mediaMetadata ->
            if (mediaMetadata != null && !dataSaver) {
                runCatching { downloadUtil.cacheSong(mediaMetadata.id) }
            }
        }

        dataStore.data
            .map { it[SkipSilenceKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) {
                player.skipSilenceEnabled = it
            }

        combine(
            currentFormat,
            dataStore.data
                .map { it[AudioNormalizationKey] ?: true }
                .distinctUntilChanged(),
            dataStore.data
                .map { it[LoudnessPresetKey] ?: LoudnessPreset.NORMAL.name }
                .distinctUntilChanged(),
        ) { format, normalizeAudio, presetName ->
            Triple(
                format,
                normalizeAudio,
                LoudnessPreset.values().firstOrNull { it.name == presetName } ?: LoudnessPreset.NORMAL,
            )
        }.collectLatest(scope) { (format, normalizeAudio, loudnessPreset) ->
            normalizeFactor.value = when {
                !normalizeAudio || format?.loudnessDb == null -> 1f
                loudnessPreset == LoudnessPreset.OFF -> 1f
                loudnessPreset == LoudnessPreset.STRONG ->
                    (10f.pow(-format.loudnessDb.toFloat() / 20f)).coerceAtMost(2f)
                else -> min(10f.pow(-format.loudnessDb.toFloat() / 20f), 1f)
            }
        }

        dataStore.data
            .map { it[DiscordTokenKey] to (it[EnableDiscordRPCKey] ?: true) }
            .debounce(300)
            .distinctUntilChanged()
            .collect(scope) { (key, enabled) ->
                if (discordRpc?.isRpcRunning() == true) {
                    discordRpc?.closeRPC()
                }
                discordRpc = null
                if (key != null && enabled) {
                    discordRpc = DiscordRPC(this, key)
                    currentSong.value?.let {
                        discordRpc?.updateSong(it)
                    }
                }
            }

        if (dataStore.get(PersistentQueueKey, true)) {
            runCatching {
                filesDir.resolve(PERSISTENT_QUEUE_FILE).inputStream().use { fis ->
                    ObjectInputStream(fis).use { oos ->
                        oos.readObject() as PersistQueue
                    }
                }
            }.onSuccess { queue ->
                if (queue.items.isNotEmpty()) {
                    val safeIndex = queue.mediaItemIndex.coerceIn(0, queue.items.lastIndex)
                    val safePosition = queue.position.coerceAtLeast(0L)
                    playQueue(
                        queue = ListQueue(
                            title = queue.title,
                            items = queue.items.map { it.toMediaItem() },
                            startIndex = safeIndex,
                            position = safePosition,
                        ),
                        playWhenReady = false,
                    )
                }
            }.onFailure {
                // Never keep a corrupted/stale queue snapshot around: otherwise every service
                // restart can resurrect the same wrong track instead of the actual last item.
                filesDir.resolve(PERSISTENT_QUEUE_FILE).delete()
            }
        }

        // Save queue periodically to prevent queue loss from crash or force kill
        scope.launch {
            while (isActive) {
                delay(10.seconds)
                if (dataStore.get(PersistentQueueKey, true)) {
                    saveQueueToDisk()
                }
            }
        }
    }

    private fun updateNotification() {
        // Spotify-style five-button row (user request, reference video): like,
        // previous, play/pause, next, shuffle - on EVERY surface the media
        // session feeds (lock screen card, notification shade, quick settings
        // carousel, Wear OS). The seekbar + timestamps come from the SEEK_TO
        // command the notification controller already holds; these preferences
        // replace the previous custom-only row so the transports are always
        // present alongside the like button.
        val customLayout = listOf(
                CommandButton.Builder()
                    .setDisplayName(getString(if (currentSong.value?.song?.liked == true) R.string.action_remove_like else R.string.action_like))
                    .setIconResId(if (currentSong.value?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border)
                    .setSessionCommand(CommandToggleLike)
                    .setEnabled(currentSong.value != null)
                    .build(),
                CommandButton.Builder()
                    .setDisplayName(getString(R.string.media_notification_previous))
                    .setIconResId(R.drawable.skip_previous)
                    .setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .setEnabled(player.hasPreviousMediaItem())
                    .build(),
                CommandButton.Builder()
                    .setDisplayName(getString(R.string.media_notification_play_pause))
                    .setIconResId(if (player.isPlaying) R.drawable.pause else R.drawable.play)
                    .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
                    .build(),
                CommandButton.Builder()
                    .setDisplayName(getString(R.string.media_notification_next))
                    .setIconResId(R.drawable.skip_next)
                    .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT)
                    .setEnabled(player.hasNextMediaItem())
                    .build(),
                CommandButton.Builder()
                    .setDisplayName(getString(if (player.shuffleModeEnabled) R.string.action_shuffle_off else R.string.action_shuffle_on))
                    .setIconResId(if (player.shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle)
                    .setSessionCommand(CommandToggleShuffle)
                    .build()
        )
        // The custom layout is what pre-Android 13 notification actions read;
        // the lock screen and Android 13+ system media controls read custom
        // buttons from the MEDIA BUTTON PREFERENCES instead (media3 1.11).
        // Set both so every surface - notification shade, lock screen, quick
        // settings media carousel, Wear OS - gets the buttons.
        mediaSession.setCustomLayout(customLayout)
        mediaSession.setMediaButtonPreferences(customLayout)
    }

    private suspend fun recoverSong(mediaId: String, playerResponse: PlayerResponse? = null) {
        val song = database.song(mediaId).first()
        val mediaMetadata = withContext(Dispatchers.Main) {
            player.findNextMediaItemById(mediaId)?.metadata
        } ?: return
        val duration = song?.song?.duration?.takeIf { it != -1 }
            ?: mediaMetadata.duration.takeIf { it != -1 }
            ?: (playerResponse ?: YouTube.player(mediaId).getOrNull())?.videoDetails?.lengthSeconds?.toInt()
            ?: -1
        database.query {
            if (song == null) insert(mediaMetadata.copy(duration = duration))
            else if (song.song.duration == -1) update(song.song.copy(duration = duration))
        }
        if (!database.hasRelatedSongs(mediaId)) {
            val relatedEndpoint = YouTube.next(WatchEndpoint(videoId = mediaId)).getOrNull()?.relatedEndpoint ?: return
            val relatedPage = YouTube.related(relatedEndpoint).getOrNull() ?: return
            database.query {
                relatedPage.songs
                    .map(SongItem::toMediaMetadata)
                    .onEach(::insert)
                    .map {
                        RelatedSongMap(
                            songId = mediaId,
                            relatedSongId = it.id
                        )
                    }
                    .forEach(::insert)
            }
        }
    }

    fun playQueue(queue: Queue, playWhenReady: Boolean = true) {
        if (!scope.isActive) {
            scope = CoroutineScope(Dispatchers.Main) + Job()
        }
        currentQueue = queue
        queueTitle = null
        player.shuffleModeEnabled = false
        if (queue.preloadItem != null) {
            player.setMediaItem(queue.preloadItem!!.toMediaItem())
            player.prepare()
            player.playWhenReady = playWhenReady
        }

        scope.launch(SilentHandler) {
            val initialStatus = withContext(Dispatchers.IO) {
                queue.getInitialStatus().filterExplicit(dataStore.get(HideExplicitKey, false))
            }
            if (queue.preloadItem != null && player.playbackState == STATE_IDLE) return@launch
            if (initialStatus.title != null) {
                queueTitle = initialStatus.title
            }
            if (initialStatus.items.isEmpty()) return@launch
            if (queue.preloadItem != null) {
                // add missing songs back, without affecting current playing song
                val nextItems = if (endlessQueueEnabled) initialStatus.items else {
                    val start = initialStatus.mediaItemIndex.coerceIn(0, initialStatus.items.lastIndex)
                    initialStatus.items.subList(start, (start + NON_ENDLESS_QUEUE_SIZE).coerceAtMost(initialStatus.items.size))
                }
                val selectedIndex = if (endlessQueueEnabled) initialStatus.mediaItemIndex else 0
                player.addMediaItems(0, nextItems.subList(0, selectedIndex))
                player.addMediaItems(nextItems.subList(selectedIndex + 1, nextItems.size))
                if (player.mediaItemCount <= 1) maybeExtendEndlessQueue(force = true)
            } else {
                val items = if (endlessQueueEnabled) initialStatus.items else {
                    val start = initialStatus.mediaItemIndex.coerceIn(0, initialStatus.items.lastIndex)
                    initialStatus.items.subList(start, (start + NON_ENDLESS_QUEUE_SIZE).coerceAtMost(initialStatus.items.size))
                }
                player.setMediaItems(items, if (endlessQueueEnabled) initialStatus.mediaItemIndex else 0, if (endlessQueueEnabled) initialStatus.position else 0L)
                player.prepare()
                player.playWhenReady = playWhenReady
                // A single-track play must immediately get a similar-song tail. Waiting for the
                // first media-item transition made the queue look empty until the song ended.
                if (player.mediaItemCount <= 1) {
                    maybeExtendEndlessQueue(force = true)
                }
            }
        }
    }

    fun startRadioSeamlessly() {
        val currentMediaMetadata = player.currentMetadata ?: return
        if (player.currentMediaItemIndex > 0) player.removeMediaItems(0, player.currentMediaItemIndex)
        if (player.currentMediaItemIndex < player.mediaItemCount - 1) player.removeMediaItems(player.currentMediaItemIndex + 1, player.mediaItemCount)
        scope.launch(SilentHandler) {
            val radioQueue = YouTubeQueue(endpoint = WatchEndpoint(videoId = currentMediaMetadata.id))
            val initialStatus = radioQueue.getInitialStatus()
            if (initialStatus.title != null) {
                queueTitle = initialStatus.title
            }
            player.addMediaItems(initialStatus.items.drop(1))
            currentQueue = radioQueue
        }
    }

    // Echo Player and Audio: optionally drop songs that are already queued.
    private fun withoutQueueDuplicates(items: List<MediaItem>): List<MediaItem> {
        if (!dataStore.get(PreventDuplicateTracksKey, false)) return items
        val existing = HashSet<String>()
        for (i in 0 until player.mediaItemCount) {
            existing.add(player.getMediaItemAt(i).mediaId)
        }
        return items.filter { it.mediaId !in existing }
    }

    fun playNext(items: List<MediaItem>) {
        val safeItems = withoutQueueDuplicates(items)
        if (safeItems.isEmpty()) return
        val wasEmpty = player.mediaItemCount == 0
        val insertIndex = if (wasEmpty) 0 else (player.currentMediaItemIndex + 1).coerceAtMost(player.mediaItemCount)
        player.addMediaItems(insertIndex, safeItems)
        // ExoPlayer keeps a prepared/playing timeline prepared when items are appended.
        // Re-preparing on every menu action can restart source resolution and surface a
        // transient playback error. Only prepare when the player was actually idle/empty.
        if (wasEmpty || player.playbackState == Player.STATE_IDLE || player.playerError != null) {
            player.prepare()
            if (wasEmpty) player.playWhenReady = true
        }
    }

    fun addToQueue(items: List<MediaItem>) {
        val safeItems = withoutQueueDuplicates(items)
        if (safeItems.isEmpty()) return
        val wasEmpty = player.mediaItemCount == 0
        player.addMediaItems(safeItems)
        // Do not call prepare() for an already playing/prepared queue. Appending to the
        // timeline is sufficient and avoids unnecessary network/source re-resolution.
        if (wasEmpty || player.playbackState == Player.STATE_IDLE || player.playerError != null) {
            player.prepare()
            if (wasEmpty) player.playWhenReady = true
        }
    }

    fun toggleLibrary() {
        database.query {
            currentSong.value?.let {
                update(it.song.toggleLibrary())
            }
        }
    }

    fun toggleLike() {
        database.query {
            currentSong.value?.let {
                update(it.song.toggleLike())
            }
        }
    }

    private fun openAudioEffectSession() {
        if (isAudioEffectSessionOpened) return
        isAudioEffectSessionOpened = true
        sendBroadcast(
            Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
        )
    }

    private fun closeAudioEffectSession() {
        if (!isAudioEffectSessionOpened) return
        isAudioEffectSessionOpened = false
        sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            }
        )
    }

    // Automix (Echo Player and Audio): a quick volume fade when skipping, so manual
    // skips blend as smoothly as natural track transitions do. Falls back to the plain
    // skip when the setting is off.
    fun fadeSkip(forward: Boolean) {
        if (!dataStore.get(AutomixKey, false) || crossfadeEnabled) {
            if (forward) player.seekToNext() else player.seekToPrevious()
            return
        }
        fadeJob?.cancel()
        fadeJob = scope.launch {
            val steps = 7
            repeat(steps) { i ->
                crossfadeFactor.value = 1f - (i + 1f) / steps
                delay(45)
            }
            if (forward) player.seekToNext() else player.seekToPrevious()
            crossfadeFactor.value = 0f
            // quick fade back in, mirroring the natural-transition treatment
            repeat(steps) { i ->
                crossfadeFactor.value = (i + 1f) / steps
                delay(45)
            }
            crossfadeFactor.value = 1f
        }
    }

    // Preload (Echo Player and Audio): cache the next song's audio and lyrics early.
    private fun preloadNext() {
        if (player.playbackState == STATE_IDLE) return
        val nextIndex = player.currentMediaItemIndex + 1
        if (nextIndex >= player.mediaItemCount) return
        val nextId = player.getMediaItemAt(nextIndex).mediaId ?: return
        // Ultra-fast starts (user request): preloading the next song's audio and
        // lyrics is ON by default now - the next track begins instantly and its
        // lyrics are already in memory when the lyrics view opens.
        if (dataStore.get(PreloadNextSongKey, true)) {
            scope.launch(Dispatchers.IO + SilentHandler) {
                runCatching { downloadUtil.preloadSong(nextId) }
            }
        }
        if (dataStore.get(PreloadLyricsKey, true)) {
            scope.launch(Dispatchers.IO + SilentHandler) {
                runCatching {
                    database.song(nextId).first()?.let { song ->
                        lyricsHelper.getLyrics(song.toMediaMetadata())
                    }
                }
            }
        }
    }

    /** True continuation-backed endless queue. */
    private fun maybeExtendEndlessQueue(force: Boolean = false) {
        if (!endlessQueueEnabled || player.currentMetadata?.mediaType == MEDIA_TYPE_PODCAST_EPISODE) return
        val remaining = player.mediaItemCount - player.currentMediaItemIndex
        if (!force && remaining > ENDLESS_QUEUE_LOW_WATERMARK) return
        if (endlessQueueJob?.isActive == true) return
        endlessQueueJob = scope.launch(SilentHandler) {
            try {
                val hideExplicit = dataStore.get(HideExplicitKey, false)
                val known = mutableSetOf<String>().apply {
                    for (index in 0 until player.mediaItemCount) add(player.getMediaItemAt(index).mediaId)
                    addAll(endlessQueueAppendedIds)
                }
                if (currentQueue.hasNextPage()) {
                    val page = withContext(Dispatchers.IO) { currentQueue.nextPage().filterExplicit(hideExplicit) }
                    val fresh = page.filter { known.add(it.mediaId) }
                    if (fresh.isNotEmpty() && player.playbackState != STATE_IDLE) {
                        player.addMediaItems(fresh)
                        endlessQueueAppendedIds += fresh.map { it.mediaId }
                    }
                    return@launch
                }
                var radio = endlessRadioQueue
                    ?: YouTubeQueue.radio(player.currentMetadata ?: return@launch).also { endlessRadioQueue = it }
                val fresh = mutableListOf<MediaItem>()
                repeat(ENDLESS_QUEUE_MAX_PAGE_ATTEMPTS) {
                    if (fresh.size >= ENDLESS_QUEUE_BATCH_SIZE) return@repeat
                    val page = withContext(Dispatchers.IO) {
                        if (radio.hasNextPage()) {
                            radio.nextPage().filterExplicit(hideExplicit)
                        } else {
                            val restarted = YouTubeQueue.radio(player.currentMetadata ?: return@withContext emptyList())
                            endlessRadioQueue = restarted
                            radio = restarted
                            restarted.getInitialStatus().items.filterExplicit(hideExplicit)
                        }
                    }
                    page.forEach { if (known.add(it.mediaId) && fresh.size < ENDLESS_QUEUE_BATCH_SIZE) fresh += it }
                }
                if (fresh.isNotEmpty() && player.playbackState != STATE_IDLE) {
                    player.addMediaItems(fresh)
                    endlessQueueAppendedIds += fresh.map { it.mediaId }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.w("MusicService", "Endless queue extension failed", e)
            } finally {
                endlessQueueJob = null
            }
        }
    }

    private fun trimQueueWhenEndlessDisabled() {
        if (player.mediaItemCount == 0) return
        val currentIndex = player.currentMediaItemIndex.coerceAtLeast(0)
        val keepUntilExclusive = (currentIndex + NON_ENDLESS_QUEUE_SIZE).coerceAtMost(player.mediaItemCount)
        if (keepUntilExclusive < player.mediaItemCount) player.removeMediaItems(keepUntilExclusive, player.mediaItemCount)
        if (currentIndex > 0 && currentIndex < player.mediaItemCount) player.removeMediaItems(0, currentIndex)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        // Keep the notification row's play/pause glyph in sync with the player.
        updateNotification()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        // Endless queue: append a radio tail while the queue is about to run
        // out (the queue's own pages are handled by auto-load-more above).
        if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
            maybeExtendEndlessQueue()
        }

        preloadNext()

        // Crossfade: fade the volume back in after every track transition.
        if (crossfadeEnabled) {
            isCrossfadingOut = false
            fadeJob?.cancel()
            fadeJob = scope.launch {
                val start = crossfadeFactor.value
                if (start < 1f) {
                    val fadeInMs = (crossfadeDuration * 1000L / 3).coerceIn(500L, 2000L)
                    val steps = 20
                    repeat(steps) { step ->
                        crossfadeFactor.value = start + (1f - start) * ((step + 1).toFloat() / steps)
                        delay(fadeInMs / steps)
                    }
                }
                crossfadeFactor.value = 1f
            }
        }
    }

    override fun onPlaybackStateChanged(@Player.State playbackState: Int) {
        if (playbackState == STATE_IDLE) {
            currentQueue = EmptyQueue
            player.shuffleModeEnabled = false
            queueTitle = null
            endlessRadioQueue = null
            endlessQueueAppendedIds.clear()
        }

        // Crossfade: never leave the volume faded down when playback stops or ends.
        if (crossfadeEnabled && (playbackState == STATE_IDLE || playbackState == STATE_ENDED)) {
            isCrossfadingOut = false
            fadeJob?.cancel()
            crossfadeFactor.value = 1f
        }
    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (events.containsAny(Player.EVENT_PLAYBACK_STATE_CHANGED, Player.EVENT_PLAY_WHEN_READY_CHANGED)) {
            val isBufferingOrReady = player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_READY
            if (isBufferingOrReady && player.playWhenReady) {
                openAudioEffectSession()
            } else {
                closeAudioEffectSession()
            }
        }
        if (events.containsAny(EVENT_TIMELINE_CHANGED, EVENT_POSITION_DISCONTINUITY)) {
            currentMediaMetadata.value = player.currentMetadata
        }
    }


    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        updateNotification()
        if (shuffleModeEnabled) {
            // Always put current playing item at first
            val shuffledIndices = IntArray(player.mediaItemCount) { it }
            shuffledIndices.shuffle()
            shuffledIndices[shuffledIndices.indexOf(player.currentMediaItemIndex)] = shuffledIndices[0]
            shuffledIndices[0] = player.currentMediaItemIndex
            player.setShuffleOrder(DefaultShuffleOrder(shuffledIndices, System.currentTimeMillis()))
        }
        // Persist the shuffle state across restarts.
        scope.launch {
            dataStore.edit { settings ->
                settings[ShuffleModeKey] = shuffleModeEnabled
            }
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        updateNotification()
        scope.launch {
            dataStore.edit { settings ->
                settings[RepeatModeKey] = repeatMode
            }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        if (dataStore.get(AutoSkipNextOnErrorKey, false) &&
            isInternetAvailable(this) &&
            player.hasNextMediaItem()
        ) {
            player.seekToNext()
            player.prepare()
            player.playWhenReady = true
        }
    }

    private fun createCacheDataSource(): CacheDataSource.Factory =
        CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(
                CacheDataSource.Factory()
                    .setCache(playerCache)
                    .setUpstreamDataSourceFactory(
                        DefaultDataSource.Factory(
                            this,
                            OkHttpDataSource.Factory(
                                OkHttpClient.Builder()
                                    .proxy(YouTube.proxy)
                                    .build()
                            )
                        )
                    )
            )
            .setCacheWriteDataSinkFactory(null)
            .setFlags(FLAG_IGNORE_CACHE_ON_ERROR)

    /**
     * The fullscreen-canvas video URL for a song, resolved independently of
     * the audio path. HIGH QUALITY OR NOTHING (the user's rule): only a
     * VIDEO-ONLY adaptive stream at least as tall as the video-quality setting
     * is accepted - the smallest one at or above it, so 720p does not stream
     * 1080p data for a background loop. When nothing that good exists the
     * canvas simply never comes: no low-quality stream, no muxed fallback - the
     * thumbnail stays. Never throws.
     */
    /** Warm the first ~4 MB of the canvas video into the Koin canvas cache
     * (LRU) before the URL is published. Reads go through CacheDataSource, so
     * whatever is read here is served to the player from cache afterwards -
     * the visual starts instantly once shown. A failed/slow preload keeps the
     * thumbnail in place (the URL is simply not published). */
    private suspend fun preloadCanvasSegment(url: String): Boolean {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            runCatching {
                val cache = com.muso.music.App.koin.get<androidx.media3.datasource.cache.SimpleCache>(
                    org.koin.core.qualifier.named(com.maxrave.common.Config.CANVAS_CACHE),
                )
                val source = androidx.media3.datasource.cache.CacheDataSource.Factory()
                    .setCache(cache)
                    .setUpstreamDataSourceFactory(
                        androidx.media3.datasource.okhttp.OkHttpDataSource.Factory(
                            okhttp3.OkHttpClient.Builder()
                                .proxy(com.zionhuang.innertube.YouTube.proxy)
                                .connectTimeout(java.time.Duration.ofSeconds(8))
                                .readTimeout(java.time.Duration.ofSeconds(8))
                                .build()
                        ),
                    )
                    .createDataSource()
                source.open(androidx.media3.datasource.DataSpec(android.net.Uri.parse(url)))
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                while (total < 4L * 1024 * 1024) {
                    val read = source.read(buffer, 0, buffer.size)
                    if (read == androidx.media3.common.C.RESULT_END_OF_INPUT) break
                    total += read
                }
                source.close()
                total > 0
            }.getOrDefault(false)
        }
    }

    private suspend fun resolveCanvasVideoUrl(mediaMetadata: com.muso.music.models.MediaMetadata): String? =
        runCatching {
            val downloadedVideo =
                moe.rukamori.archivetune.storage.StorageLocationRepository
                    .cacheDirectory(this, moe.rukamori.archivetune.storage.StorageFolderKind.DOWNLOADS)
                    .resolve("canvas")
                    .listFiles()
                    ?.firstOrNull {
                        it.isFile && it.nameWithoutExtension == mediaMetadata.id && it.length() > 0L
                    }
            if (downloadedVideo != null) return@runCatching android.net.Uri.fromFile(downloadedVideo).toString()

            val playerResponse = YouTube.player(mediaMetadata.id).getOrThrow()
            val targetHeight = when (videoQuality) {
                VideoQuality.Q360 -> 360
                VideoQuality.Q720 -> 720
                VideoQuality.Q1080 -> 1080
            }
            val streamingData = playerResponse.streamingData
            // The quality bar is absolute (Round 94): a stream must be at least
            // the target height or there is no video at all. A MUXED stream
            // at/above the bar is equally sharp though, so it is a valid
            // fallback when the video-only adaptive set has nothing at that
            // height - that is what made "some videos never play": a lot of
            // songs only offer muxed pictures at a perfectly good quality.
            val adaptive = streamingData?.adaptiveFormats.orEmpty()
                .filter { !it.url.isNullOrEmpty() && !it.isAudio && (it.height ?: 0) >= targetHeight }
            val muxed = streamingData?.formats.orEmpty()
                .filter { !it.url.isNullOrEmpty() && (it.height ?: 0) >= targetHeight }
            // Preferred: the smallest at-or-above-target video-only stream (or a
            // muxed one at that bar). User report (v0.5.152): videos NEVER came
            // even after a whole song - for many songs nothing reaches the bar,
            // and "no video at all" beats the quality bar. Final fallback: the
            // best video of ANY height, so a canvas appears whenever the song
            // simply HAS a video.
            val preferred = adaptive.minByOrNull { it.height ?: 0 } ?: muxed.maxByOrNull { it.height ?: 0 }
            val anyAdaptive = streamingData?.adaptiveFormats.orEmpty()
                .filter { !it.url.isNullOrEmpty() && !it.isAudio }
            val anyMuxed = streamingData?.formats.orEmpty()
                .filter { !it.url.isNullOrEmpty() }
            (preferred ?: anyAdaptive.maxByOrNull { it.height ?: 0 } ?: anyMuxed.maxByOrNull { it.height ?: 0 })?.url
        }.getOrNull()

    private fun createDataSourceFactory(): DataSource.Factory {
        val songUrlCache = HashMap<String, Pair<String, Long>>()
        return ResolvingDataSource.Factory(createCacheDataSource()) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")

            val requestedLength = if (dataSpec.length >= 0) dataSpec.length else 1L
            val cachedAtPosition =
                downloadCache.isCached(mediaId, dataSpec.position, requestedLength) ||
                    playerCache.isCached(mediaId, dataSpec.position, CHUNK_LENGTH)

            if (cachedAtPosition) {
                scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                // Instant path ONLY when the WHOLE file is cached. A partial hit
                // stranded the player at the cache boundary: the spec keeps its
                // unresolved placeholder URI (no stream URL was resolved), so the
                // moment playback crossed into uncached ranges the upstream fetch
                // died - "resumes from cache, then stops". Partially cached songs
                // fall through to the full resolve below, where the real URL makes
                // the boundary fetch work; the cached ranges still play from cache.
                //
                // Round 193 (offline fix): the whole-file probe used to be
                // Long.MAX_VALUE alone, which no real (finite) cache span ever
                // satisfies - so a DOWNLOADED or fully-CACHED song fell through to
                // the stream-URL resolve below, which needs the network and threw
                // offline ("downloaded and cached songs do not play offline"). The
                // file is now recognised from its recorded content length - a
                // whole-file probe plus first-byte/last-byte probes - with the
                // unbounded probe kept as a fallback.
                val recordedLength =
                    runCatching {
                        runBlocking(Dispatchers.IO) { database.format(mediaId).first() }
                    }.getOrNull()?.contentLength ?: -1L
                val fullyCached =
                    downloadCache.isCached(mediaId, 0, Long.MAX_VALUE) ||
                        playerCache.isCached(mediaId, 0, Long.MAX_VALUE) ||
                        (
                            recordedLength > 0L &&
                                (
                                    downloadCache.isCached(mediaId, 0, recordedLength) ||
                                        playerCache.isCached(mediaId, 0, recordedLength) ||
                                        (
                                            downloadCache.isCached(mediaId, 0, 1L) &&
                                                downloadCache.isCached(mediaId, recordedLength - 1L, 1L)
                                            ) ||
                                        (
                                            playerCache.isCached(mediaId, 0, 1L) &&
                                                playerCache.isCached(mediaId, recordedLength - 1L, 1L)
                                            )
                                    )
                            )
                if (fullyCached) {
                    return@Factory dataSpec
                }
                // Offline with something cached at this position: serve the
                // cached bytes instead of demanding a network resolve, so a
                // downloaded/cached song starts with no connection at all.
                if (!isInternetAvailable(this@MusicService)) {
                    return@Factory dataSpec
                }
            } else if (!isInternetAvailable(this@MusicService)) {
                // Nothing cached for this position and no network: fail with the
                // clear "no internet" error instead of a raw connect failure.
                throw PlaybackException(
                    getString(R.string.error_no_internet),
                    null,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                )
            }

            songUrlCache[mediaId]?.takeIf { it.second < System.currentTimeMillis() }?.let {
                scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                return@Factory dataSpec.withUri(it.first.toUri())
            }

            // Check whether format exists so that users from older version can view format details
            // There may be inconsistent between the downloaded file and the displayed info if user change audio quality frequently
            val wantedQuality = if (dataSaver) AudioQuality.LOW else audioQuality
            val playedFormat = runBlocking(Dispatchers.IO) { database.format(mediaId).first() }
            val playerResponse = runBlocking(Dispatchers.IO) {
                YouTube.player(
                    mediaId,
                    requireHighQuality = wantedQuality == AudioQuality.HIGH_OPUS ||
                        wantedQuality == AudioQuality.HIGH_AAC,
                )
            }.getOrElse { throwable ->
                when (throwable) {
                    is ConnectException, is UnknownHostException -> {
                        throw PlaybackException(getString(R.string.error_no_internet), throwable, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)
                    }

                    is SocketTimeoutException -> {
                        throw PlaybackException(getString(R.string.error_timeout), throwable, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT)
                    }

                    else -> throw PlaybackException(getString(R.string.error_unknown), throwable, PlaybackException.ERROR_CODE_REMOTE_ERROR)
                }
            }
            if (playerResponse.playabilityStatus.status != "OK") {
                throw PlaybackException(playerResponse.playabilityStatus.reason, null, PlaybackException.ERROR_CODE_REMOTE_ERROR)
            }

            // Quality setting must always govern: the previously played format is
            // reused ONLY when it still belongs to the selected quality family.
            // Without this guard, a song cached at the old low itag would ignore
            // the quality picker forever (user report: "always stays Low").
            val format = playedFormat
                ?.takeIf { pf -> wantedQuality.itagPreference().contains(pf.itag) }
                ?.let { pf ->
                    playerResponse.streamingData?.adaptiveFormats?.find {
                        // Use itag to identify previously played format
                        it.itag == pf.itag
                    }
                }
                ?: playerResponse.streamingData?.adaptiveFormats
                    ?.filter { it.isAudio }
                    ?.let { audio ->
                        // SimpMusic quality system: the setting's exact itag first,
                        // then its high-quality twin (774 <-> 141), then the family
                        // order - and only if none exists fall back to the highest
                        // bitrate. Data saver always forces Low.
                        (if (dataSaver) AudioQuality.LOW.itagPreference() else wantedQuality.itagPreference())
                            .firstNotNullOfOrNull { wantedItag ->
                                audio.find { it.itag == wantedItag }
                            }
                            ?: audio.maxByOrNull { it.bitrate }
                    }
                ?: throw PlaybackException(getString(R.string.error_no_stream), null, ERROR_CODE_NO_STREAM)

            database.query {
                upsert(
                    FormatEntity(
                        id = mediaId,
                        itag = format.itag,
                        mimeType = format.mimeType.split(";")[0],
                        codecs = format.mimeType.split("codecs=")[1].removeSurrounding("\""),
                        bitrate = format.bitrate,
                        sampleRate = format.audioSampleRate,
                        contentLength = format.contentLength!!,
                        loudnessDb = playerResponse.playerConfig?.audioConfig?.loudnessDb
                    )
                )
            }
            scope.launch(Dispatchers.IO) { recoverSong(mediaId, playerResponse) }

            songUrlCache[mediaId] = format.url!! to playerResponse.streamingData!!.expiresInSeconds * 1000L
            dataSpec.withUri(format.url!!.toUri()).subrange(dataSpec.uriPositionOffset, CHUNK_LENGTH)
        }
    }

    private fun createMediaSourceFactory() =
        DefaultMediaSourceFactory(
            createDataSourceFactory(),
            ExtractorsFactory {
                arrayOf(MatroskaExtractor(), Mp4Extractor(), FragmentedMp4Extractor())
            }
        )

    private fun createRenderersFactory() =
        object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ) = DefaultAudioSink.Builder(this@MusicService)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessorChain(
                    // Spatial audio (Echo Player and Audio): a real stereo-widening DSP,
                    // prepended to the chain when enabled (applies on next app start).
                    DefaultAudioSink.DefaultAudioProcessorChain(
                        *buildList {
                            if (dataStore.get(SpatialAudioKey, false)) {
                                add(SpatialAudioProcessor())
                            }
                            add(SilenceSkippingAudioProcessor(2_000_000, 0.01f, 2_000_000, 0, 256))
                            add(SonicAudioProcessor())
                        }.toTypedArray(),
                    ),
                )
                .build()
                .also { sink ->
                    // Audio offload: media3 exposes this on the sink instance (API 29+),
                    // not on the builder. Build-time choice, applied on renderers creation.
                    if (Build.VERSION.SDK_INT >= 29) {
                        sink.setOffloadMode(
                            if (dataStore.get(AudioOffloadKey, false)) {
                                AudioSink.OFFLOAD_MODE_ENABLED_GAPLESS_REQUIRED
                            } else {
                                AudioSink.OFFLOAD_MODE_DISABLED
                            }
                        )
                    }
                }
        }

    override fun onPlaybackStatsReady(eventTime: AnalyticsListener.EventTime, playbackStats: PlaybackStats) {
        if (playbackStats.totalPlayTimeMs < 30_000 || dataStore.get(PauseListenHistoryKey, false)) return
        val timeline = eventTime.timeline
        val windowIndex = eventTime.windowIndex
        if (timeline.isEmpty || windowIndex !in 0 until timeline.windowCount) return
        val mediaItem = timeline.getWindow(windowIndex, Timeline.Window()).mediaItem
        val mediaId = mediaItem.mediaId
        if (mediaId.isBlank()) return
        database.query {
            runCatching {
                incrementTotalPlayTime(mediaId, playbackStats.totalPlayTimeMs)
                insert(
                    Event(
                        songId = mediaId,
                        timestamp = LocalDateTime.now(),
                        playTime = playbackStats.totalPlayTimeMs
                    )
                )
            }.onFailure { reportException(it) }
        }
    }

    private fun saveQueueToDisk() {
        if (player.playbackState == STATE_IDLE) {
            filesDir.resolve(PERSISTENT_QUEUE_FILE).delete()
            return
        }
        val items = player.mediaItems.mapNotNull { it.metadata }
        if (items.isEmpty()) {
            filesDir.resolve(PERSISTENT_QUEUE_FILE).delete()
            return
        }
        val persistQueue = PersistQueue(
            title = queueTitle,
            items = items,
            mediaItemIndex = player.currentMediaItemIndex.coerceIn(0, items.lastIndex),
            position = player.currentPosition.coerceAtLeast(0L),
        )
        val target = filesDir.resolve(PERSISTENT_QUEUE_FILE)
        val temp = filesDir.resolve("$PERSISTENT_QUEUE_FILE.tmp")
        runCatching {
            temp.outputStream().use { fos ->
                ObjectOutputStream(fos).use { oos ->
                    oos.writeObject(persistQueue)
                    oos.flush()
                    fos.fd.sync()
                }
            }
            if (target.exists() && !target.delete()) {
                error("Unable to replace persistent queue")
            }
            if (!temp.renameTo(target)) {
                temp.delete()
                error("Unable to move persistent queue into place")
            }
        }.onFailure {
            temp.delete()
            reportException(it)
        }
    }

    override fun onDestroy() {
        com.muso.music.suite.SuitePlayerRegistry.player = null
        volumeObserver?.let { contentResolver.unregisterContentObserver(it) }
        volumeObserver = null
        if (dataStore.get(PersistentQueueKey, true)) {
            saveQueueToDisk()
        }
        if (discordRpc?.isRpcRunning() == true) {
            discordRpc?.closeRPC()
        }
        discordRpc = null
        audioEffectsManager?.release()
        audioEffectsManager = null
        mediaSession.release()
        player.removeListener(this)
        player.removeListener(sleepTimer)
        player.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = super.onBind(intent) ?: binder

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    inner class MusicBinder : Binder() {
        val service: MusicService
            get() = this@MusicService
    }

    companion object {
        private const val LYRICS_NOT_FOUND_RETRY_MS = 24L * 60 * 60 * 1000
        private const val ENDLESS_QUEUE_LOW_WATERMARK = 5
        private const val ENDLESS_QUEUE_BATCH_SIZE = 15
        private const val ENDLESS_QUEUE_MAX_PAGE_ATTEMPTS = 4
        private const val NON_ENDLESS_QUEUE_SIZE = 15

        const val ROOT = "root"
        const val SONG = "song"
        const val ARTIST = "artist"
        const val ALBUM = "album"
        const val PLAYLIST = "playlist"

        const val CHANNEL_ID = "music_channel_01"
        const val NOTIFICATION_ID = 888
        const val ERROR_CODE_NO_STREAM = 1000001
        const val CHUNK_LENGTH = 512 * 1024L
        const val PERSISTENT_QUEUE_FILE = "persistent_queue.data"
    }
}
