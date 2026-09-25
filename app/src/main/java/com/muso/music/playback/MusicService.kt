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
import com.muso.music.constants.AudioQuality
import com.muso.music.constants.AudioQualityKey
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
import kotlinx.coroutines.CoroutineScope
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

    private val audioQuality by enumPreference(this, AudioQualityKey, AudioQuality.AUTO)

    // Echo Player and Audio settings
    private val dataSaver by preference(this, DataSaverKey, false)
    private var volumeObserver: ContentObserver? = null

    private var currentQueue: Queue = EmptyQueue
    var queueTitle: String? = null

    val currentMediaMetadata = MutableStateFlow<com.muso.music.models.MediaMetadata?>(null)
    private val currentSong = currentMediaMetadata.flatMapLatest { mediaMetadata ->
        database.song(mediaMetadata?.id)
    }.stateIn(scope, SharingStarted.Lazily, null)
    private val currentFormat = currentMediaMetadata.flatMapLatest { mediaMetadata ->
        database.format(mediaMetadata?.id)
    }

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

        combine(
            currentMediaMetadata.distinctUntilChangedBy { it?.id },
            dataStore.data.map { it[ShowLyricsKey] ?: false }.distinctUntilChanged()
        ) { mediaMetadata, showLyrics ->
            mediaMetadata to showLyrics
        }.collectLatest(scope) { (mediaMetadata, showLyrics) ->
            if (showLyrics && mediaMetadata != null && database.lyrics(mediaMetadata.id).first() == null) {
                val lyrics = lyricsHelper.getLyrics(mediaMetadata)
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
                playQueue(
                    queue = ListQueue(
                        title = queue.title,
                        items = queue.items.map { it.toMediaItem() },
                        startIndex = queue.mediaItemIndex,
                        position = queue.position
                    ),
                    playWhenReady = false
                )
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
        mediaSession.setCustomLayout(
            listOf(
                CommandButton.Builder()
                    .setDisplayName(getString(if (currentSong.value?.song?.inLibrary != null) R.string.remove_from_library else R.string.add_to_library))
                    .setIconResId(if (currentSong.value?.song?.inLibrary != null) R.drawable.library_add_check else R.drawable.library_add)
                    .setSessionCommand(CommandToggleLibrary)
                    .setEnabled(currentSong.value != null)
                    .build(),
                CommandButton.Builder()
                    .setDisplayName(getString(if (currentSong.value?.song?.liked == true) R.string.action_remove_like else R.string.action_like))
                    .setIconResId(if (currentSong.value?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border)
                    .setSessionCommand(CommandToggleLike)
                    .setEnabled(currentSong.value != null)
                    .build(),
                CommandButton.Builder()
                    .setDisplayName(getString(if (player.shuffleModeEnabled) R.string.action_shuffle_off else R.string.action_shuffle_on))
                    .setIconResId(if (player.shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle)
                    .setSessionCommand(CommandToggleShuffle)
                    .build(),
                CommandButton.Builder()
                    .setDisplayName(
                        getString(
                            when (player.repeatMode) {
                                REPEAT_MODE_OFF -> R.string.repeat_mode_off
                                REPEAT_MODE_ONE -> R.string.repeat_mode_one
                                REPEAT_MODE_ALL -> R.string.repeat_mode_all
                                else -> throw IllegalStateException()
                            }
                        )
                    )
                    .setIconResId(
                        when (player.repeatMode) {
                            REPEAT_MODE_OFF -> R.drawable.repeat
                            REPEAT_MODE_ONE -> R.drawable.repeat_one_on
                            REPEAT_MODE_ALL -> R.drawable.repeat_on
                            else -> throw IllegalStateException()
                        }
                    )
                    .setSessionCommand(CommandToggleRepeatMode)
                    .build()
            )
        )
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
                player.addMediaItems(0, initialStatus.items.subList(0, initialStatus.mediaItemIndex))
                player.addMediaItems(initialStatus.items.subList(initialStatus.mediaItemIndex + 1, initialStatus.items.size))
            } else {
                player.setMediaItems(initialStatus.items, if (initialStatus.mediaItemIndex > 0) initialStatus.mediaItemIndex else 0, initialStatus.position)
                player.prepare()
                player.playWhenReady = playWhenReady
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
        player.addMediaItems(
            if (player.mediaItemCount == 0) 0 else player.currentMediaItemIndex + 1,
            withoutQueueDuplicates(items),
        )
        player.prepare()
    }

    fun addToQueue(items: List<MediaItem>) {
        player.addMediaItems(withoutQueueDuplicates(items))
        player.prepare()
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
        if (dataStore.get(PreloadNextSongKey, false)) {
            scope.launch(Dispatchers.IO + SilentHandler) {
                runCatching { downloadUtil.preloadSong(nextId) }
            }
        }
        if (dataStore.get(PreloadLyricsKey, false)) {
            scope.launch(Dispatchers.IO + SilentHandler) {
                runCatching {
                    database.song(nextId).first()?.let { song ->
                        lyricsHelper.getLyrics(song.toMediaMetadata())
                    }
                }
            }
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        // Auto load more songs
        if (dataStore.get(AutoLoadMoreKey, true) &&
            reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT &&
            player.mediaItemCount - player.currentMediaItemIndex <= 5 &&
            currentQueue.hasNextPage()
        ) {
            scope.launch(SilentHandler) {
                val mediaItems = currentQueue.nextPage().filterExplicit(dataStore.get(HideExplicitKey, false))
                if (player.playbackState != STATE_IDLE) {
                    player.addMediaItems(mediaItems)
                }
            }
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

    private fun createDataSourceFactory(): DataSource.Factory {
        val songUrlCache = HashMap<String, Pair<String, Long>>()
        return ResolvingDataSource.Factory(createCacheDataSource()) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")

            if (downloadCache.isCached(mediaId, dataSpec.position, if (dataSpec.length >= 0) dataSpec.length else 1) ||
                playerCache.isCached(mediaId, dataSpec.position, CHUNK_LENGTH)
            ) {
                scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                return@Factory dataSpec
            }

            songUrlCache[mediaId]?.takeIf { it.second < System.currentTimeMillis() }?.let {
                scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                return@Factory dataSpec.withUri(it.first.toUri())
            }

            // Check whether format exists so that users from older version can view format details
            // There may be inconsistent between the downloaded file and the displayed info if user change audio quality frequently
            val playedFormat = runBlocking(Dispatchers.IO) { database.format(mediaId).first() }
            val playerResponse = runBlocking(Dispatchers.IO) {
                YouTube.player(mediaId)
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

            val format =
                if (playedFormat != null) {
                    playerResponse.streamingData?.adaptiveFormats?.find {
                        // Use itag to identify previously played format
                        it.itag == playedFormat.itag
                    }
                } else {
                    playerResponse.streamingData?.adaptiveFormats
                        ?.filter { it.isAudio }
                        ?.maxByOrNull {
                            it.bitrate * when (if (dataSaver) AudioQuality.LOW else audioQuality) {
                                AudioQuality.AUTO -> if (connectivityManager.isActiveNetworkMetered) -1 else 1
                                AudioQuality.HIGH -> 1
                                AudioQuality.LOW -> -1
                            } + (if (it.mimeType.startsWith("audio/webm")) 10240 else 0) // prefer opus stream
                        }
                } ?: throw PlaybackException(getString(R.string.error_no_stream), null, ERROR_CODE_NO_STREAM)

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
                arrayOf(MatroskaExtractor(), FragmentedMp4Extractor())
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
        val mediaItem = eventTime.timeline.getWindow(eventTime.windowIndex, Timeline.Window()).mediaItem
        if (playbackStats.totalPlayTimeMs >= 30000 && !dataStore.get(PauseListenHistoryKey, false)) {
            database.query {
                incrementTotalPlayTime(mediaItem.mediaId, playbackStats.totalPlayTimeMs)
                try {
                    insert(
                        Event(
                            songId = mediaItem.mediaId,
                            timestamp = LocalDateTime.now(),
                            playTime = playbackStats.totalPlayTimeMs
                        )
                    )
                } catch (_: SQLException) {
                }
            }
        }
    }

    private fun saveQueueToDisk() {
        if (player.playbackState == STATE_IDLE) {
            filesDir.resolve(PERSISTENT_QUEUE_FILE).delete()
            return
        }
        val persistQueue = PersistQueue(
            title = queueTitle,
            items = player.mediaItems.mapNotNull { it.metadata },
            mediaItemIndex = player.currentMediaItemIndex,
            position = player.currentPosition
        )
        runCatching {
            filesDir.resolve(PERSISTENT_QUEUE_FILE).outputStream().use { fos ->
                ObjectOutputStream(fos).use { oos ->
                    oos.writeObject(persistQueue)
                }
            }
        }.onFailure {
            reportException(it)
        }
    }

    override fun onDestroy() {
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
