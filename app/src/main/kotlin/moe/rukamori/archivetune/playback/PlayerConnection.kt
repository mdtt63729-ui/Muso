/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Timeline
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.db.MusicDatabase
import moe.rukamori.archivetune.extensions.currentMetadata
import moe.rukamori.archivetune.extensions.getCurrentQueueIndex
import moe.rukamori.archivetune.extensions.getQueueWindows
import moe.rukamori.archivetune.models.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import moe.rukamori.archivetune.playback.MusicService.MusicBinder
import moe.rukamori.archivetune.playback.queues.Queue
import moe.rukamori.archivetune.canvas.CanvasPlaybackRequest
import moe.rukamori.archivetune.utils.isLocalMediaId
import moe.rukamori.archivetune.utils.reportException
import java.util.Locale

internal enum class CanvasArtworkRefetchResult {
    Success,
    Failure,
    AlreadyRunning,
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerConnection private constructor(
    context: Context,
    val player: Player,
    localPlayerOrNull: ExoPlayer?,
    val database: MusicDatabase,
    scope: CoroutineScope,
    serviceOrNull: MusicService?,
    val mediaMetadata: MutableStateFlow<MediaMetadata?>,
    waitingForNetworkConnectionFlow: StateFlow<Boolean>,
    queueRestoreCompletedFlow: StateFlow<Boolean>,
) : Player.Listener {

    /**
     * ArchiveTune's service and its local player. Both are present on the service-backed
     * constructor and absent on the Muso bridge, which wraps an external player.
     *
     * They stay NON-NULL typed even though the bridge has neither: the ArchiveTune UI reads
     * `connection.service...` and `connection.localPlayer...` in ~40 places that are only
     * ever reached with a real service, and a nullable type here broke every one of them.
     * Nothing on the bridge path reads either field - the Enhanced lyrics renderer needs
     * only [player], [mediaMetadata] and [playbackParameters] - so the guard below is a
     * trap for a future caller reaching for a service the bridge does not have, not a path
     * any current code takes.
     */
    val service: MusicService
        get() = serviceOrNull ?: error("This PlayerConnection wraps an external player and has no MusicService")

    val localPlayer: ExoPlayer
        get() = localPlayerOrNull ?: error("This PlayerConnection wraps an external player and has no local ExoPlayer")

    /** ArchiveTune's own playback stack. */
    constructor(
        context: Context,
        binder: MusicBinder,
        database: MusicDatabase,
        scope: CoroutineScope,
    ) : this(
        context = context,
        player = binder.service.player,
        localPlayerOrNull = binder.service.localPlayer,
        database = database,
        scope = scope,
        serviceOrNull = binder.service,
        mediaMetadata = binder.service.currentMediaMetadata,
        waitingForNetworkConnectionFlow = binder.service.waitingForNetworkConnection,
        queueRestoreCompletedFlow = binder.service.queueRestoreCompleted,
    )

    /**
     * Muso bridge: wrap an externally-owned player - Muso's playback service - so the
     * ArchiveTune renderers can run against it. The Enhanced lyrics sheet reads only
     * [player], [mediaMetadata] and the database, so the queue / Together / canvas
     * operations this class exposes are deliberately inert here.
     */
    constructor(
        context: Context,
        player: Player,
        mediaMetadata: MutableStateFlow<MediaMetadata?>,
        database: MusicDatabase,
        scope: CoroutineScope,
    ) : this(
        context = context,
        player = player,
        // Muso's player is itself an ExoPlayer, so when it is one the bridge gets a working
        // local player too; only a non-ExoPlayer Player leaves it absent.
        localPlayerOrNull = player as? ExoPlayer,
        database = database,
        scope = scope,
        serviceOrNull = null,
        mediaMetadata = mediaMetadata,
        waitingForNetworkConnectionFlow = MutableStateFlow(false),
        queueRestoreCompletedFlow = MutableStateFlow(true),
    )

    val playbackState = MutableStateFlow(player.playbackState)
    private val _isPlaying = MutableStateFlow(player.isPlaying)
    val isPlaying = _isPlaying.asStateFlow()
    val playbackParameters = MutableStateFlow(player.playbackParameters)
    val currentSong =
        mediaMetadata.flatMapLatest {
            database.song(it?.id)
        }
    val currentLyrics =
        mediaMetadata.flatMapLatest { mediaMetadata ->
            database.lyrics(mediaMetadata?.id)
        }
    val currentFormat =
        mediaMetadata.flatMapLatest { mediaMetadata ->
            database.format(mediaMetadata?.id)
        }

    val queueTitle = MutableStateFlow<String?>(null)
    val queueWindows = MutableStateFlow<List<Timeline.Window>>(emptyList())
    val currentMediaItemIndex = MutableStateFlow(-1)
    val currentWindowIndex = MutableStateFlow(-1)

    val shuffleModeEnabled = MutableStateFlow(false)
    val repeatMode = MutableStateFlow(REPEAT_MODE_OFF)

    val canSkipPrevious = MutableStateFlow(true)
    val canSkipNext = MutableStateFlow(true)

    val aodModeEnabled = MutableStateFlow(false)

    val error = MutableStateFlow<PlaybackException?>(null)
    private var dismissedPlaybackError: PlaybackException? = null
    val waitingForNetworkConnection = waitingForNetworkConnectionFlow
    val queueRestoreCompleted = queueRestoreCompletedFlow

    internal val canvasNetworkAllowed: StateFlow<Boolean> = serviceOrNull
        ?.canvasPlaybackUseCase
        ?.policy
        ?.map { it.networkAllowed }
        ?.stateIn(scope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), false)
        ?: MutableStateFlow(false)

    private val canvasArtworkRefetchMutex = Mutex()
    private val _isCanvasArtworkRefetching = MutableStateFlow(false)
    internal val isCanvasArtworkRefetching = _isCanvasArtworkRefetching.asStateFlow()

    private var metadataExtractionJob: Job? = null

    init {
        player.addListener(this)

        playbackState.value = player.playbackState
        _isPlaying.value = player.isPlaying
        playbackParameters.value = player.playbackParameters
        queueTitle.value = serviceOrNull?.queueTitle
        queueWindows.value = player.getQueueWindows()
        currentWindowIndex.value = player.getCurrentQueueIndex()
        currentMediaItemIndex.value = player.currentMediaItemIndex
        shuffleModeEnabled.value = player.shuffleModeEnabled
        repeatMode.value = player.repeatMode
        if (player.mediaItemCount > 0 && mediaMetadata.value == null) {
            mediaMetadata.value = player.currentMetadata
        }

        metadataExtractionJob =
            scope.launch(Dispatchers.IO) {
                mediaMetadata
                    .distinctUntilChangedBy { it?.id }
                    .collectLatest { metadata ->
                        val mediaId = metadata?.id ?: return@collectLatest
                        if (mediaId.isLocalMediaId()) {
                            val storedFormat = database.format(mediaId).first()
                            if (storedFormat != null && storedFormat.bitrate == 0 && storedFormat.sampleRate == null) {
                                val result =
                                    extractLocalAudioProperties(context, mediaId)
                                        ?: return@collectLatest
                                ensureActive()
                                val finalBitrate =
                                    if (result.first <= 0 && result.second == null) {
                                        -1
                                    } else {
                                        result.first
                                    }
                                database.updateLocalAudioMetadata(mediaId, finalBitrate, result.second)
                            }
                        }
                    }
            }
    }

    private suspend fun extractLocalAudioProperties(
        context: Context,
        uriString: String,
    ): Pair<Int, Int?>? =
        withContext(Dispatchers.IO) {
            val extractor = android.media.MediaExtractor()
            var bitrate = 0
            var sampleRate: Int? = null
            try {
                val uri = android.net.Uri.parse(uriString)
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                if (pfd == null) {
                    timber.log.Timber
                        .tag("LocalMetadataExtractor")
                        .w("Could not open file descriptor for %s", uriString)
                    return@withContext null
                }
                pfd.use { descriptor ->
                    extractor.setDataSource(descriptor.fileDescriptor)
                    if (extractor.trackCount == 0) {
                        return@withContext Pair(-1, null)
                    }
                    var foundAudioTrack = false
                    for (i in 0 until extractor.trackCount) {
                        val format = extractor.getTrackFormat(i)
                        val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: ""
                        if (mime.startsWith("audio/")) {
                            foundAudioTrack = true
                            if (format.containsKey(android.media.MediaFormat.KEY_BIT_RATE)) {
                                bitrate = format.getInteger(android.media.MediaFormat.KEY_BIT_RATE)
                            }
                            if (format.containsKey(android.media.MediaFormat.KEY_SAMPLE_RATE)) {
                                sampleRate = format.getInteger(android.media.MediaFormat.KEY_SAMPLE_RATE)
                            }
                            break
                        }
                    }
                    if (!foundAudioTrack) {
                        return@withContext Pair(-1, null)
                    }
                }
                Pair(bitrate, sampleRate)
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                timber.log.Timber
                    .tag("LocalMetadataExtractor")
                    .w(e, "Permission denied extracting metadata for %s", uriString)
                null
            } catch (e: java.io.FileNotFoundException) {
                timber.log.Timber
                    .tag("LocalMetadataExtractor")
                    .w(e, "File not found for %s", uriString)
                null
            } catch (e: java.io.IOException) {
                val message = e.message?.lowercase() ?: ""
                if ("unsupported" in message || "malformed" in message || "invalid" in message || "failed to instantiate" in message) {
                    timber.log.Timber
                        .tag("LocalMetadataExtractor")
                        .w(e, "Confirmed unsupported file %s", uriString)
                    Pair(-1, null)
                } else {
                    timber.log.Timber
                        .tag("LocalMetadataExtractor")
                        .w(e, "Transient I/O error extracting metadata for %s", uriString)
                    null
                }
            } catch (e: Exception) {
                timber.log.Timber
                    .tag("LocalMetadataExtractor")
                    .w(e, "Unexpected error extracting metadata for %s", uriString)
                null
            } finally {
                runCatching { extractor.release() }
            }
        }

    fun playQueue(queue: Queue) {
        serviceOrNull?.playQueue(queue)
    }

    fun startRadioSeamlessly() {
        serviceOrNull?.startRadioSeamlessly()
    }

    fun playNext(item: MediaItem) = playNext(listOf(item))

    fun playNext(items: List<MediaItem>) {
        serviceOrNull?.playNext(items)
    }

    fun moveQueueItemToNext(mediaItemIndex: Int) {
        serviceOrNull?.moveQueueItemToNext(mediaItemIndex)
    }

    fun addToQueue(item: MediaItem) = addToQueue(listOf(item))

    fun addToQueue(items: List<MediaItem>) {
        serviceOrNull?.addToQueue(items)
    }

    fun playFromVoiceSearch(query: String) {
        serviceOrNull?.playFromVoiceSearch(query)
    }

    fun toggleLike() {
        serviceOrNull?.toggleLike()
    }

    internal suspend fun refetchCanvasArtwork(
        metadata: MediaMetadata,
        requireVertical: Boolean,
    ): CanvasArtworkRefetchResult {
        if (!canvasArtworkRefetchMutex.tryLock()) return CanvasArtworkRefetchResult.AlreadyRunning

        _isCanvasArtworkRefetching.value = true
        return try {
            val country = Locale.getDefault().country
            val storefront = if (country.length == 2) country.lowercase(Locale.ROOT) else "us"
            val refreshed = serviceOrNull?.canvasPlaybackUseCase?.refresh(
                CanvasPlaybackRequest(
                    mediaId = metadata.id,
                    title = metadata.title,
                    artist = metadata.artists.firstOrNull()?.name.orEmpty(),
                    storefront = storefront,
                    requireVertical = requireVertical,
                ),
            )
            if (refreshed != true) return CanvasArtworkRefetchResult.Failure

            CanvasArtworkRefetchResult.Success
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            timber.log.Timber.tag("CanvasArtwork").w(error, "Canvas refetch failed for %s", metadata.id)
            CanvasArtworkRefetchResult.Failure
        } finally {
            _isCanvasArtworkRefetching.value = false
            canvasArtworkRefetchMutex.unlock()
        }
    }

    fun dismissPlaybackError() {
        dismissedPlaybackError = error.value ?: player.playerError
        error.value = null
    }

    fun seekToNext() {
        val state = serviceOrNull?.togetherSessionState?.value as? moe.rukamori.archivetune.together.TogetherSessionState.Joined
        if (state?.role is moe.rukamori.archivetune.together.TogetherRole.Guest) {
            serviceOrNull?.requestTogetherControl(moe.rukamori.archivetune.together.ControlAction.SkipNext)
            return
        }
        player.seekToNext()
        player.prepare()
        player.playWhenReady = true
    }

    fun seekToPrevious() {
        val state = serviceOrNull?.togetherSessionState?.value as? moe.rukamori.archivetune.together.TogetherSessionState.Joined
        if (state?.role is moe.rukamori.archivetune.together.TogetherRole.Guest) {
            serviceOrNull?.requestTogetherControl(moe.rukamori.archivetune.together.ControlAction.SkipPrevious)
            return
        }
        player.seekToPrevious()
        player.prepare()
        player.playWhenReady = true
    }

    override fun onPlaybackStateChanged(state: Int) {
        playbackState.value = state
        updatePlaybackError(player.playerError)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        this.playbackParameters.value = playbackParameters
    }

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        currentMediaItemIndex.value = player.currentMediaItemIndex
        currentWindowIndex.value = player.getCurrentQueueIndex()
        updateCanSkipPreviousAndNext()
    }

    override fun onTimelineChanged(
        timeline: Timeline,
        reason: Int,
    ) {
        queueWindows.value = player.getQueueWindows()
        queueTitle.value = serviceOrNull?.queueTitle
        currentMediaItemIndex.value = player.currentMediaItemIndex
        currentWindowIndex.value = player.getCurrentQueueIndex()
        updateCanSkipPreviousAndNext()
    }

    override fun onShuffleModeEnabledChanged(enabled: Boolean) {
        shuffleModeEnabled.value = enabled
        queueWindows.value = player.getQueueWindows()
        currentWindowIndex.value = player.getCurrentQueueIndex()
        updateCanSkipPreviousAndNext()
    }

    override fun onRepeatModeChanged(mode: Int) {
        repeatMode.value = mode
        updateCanSkipPreviousAndNext()
    }

    override fun onPlayerErrorChanged(playbackError: PlaybackException?) {
        if (playbackError != null) {
            reportException(playbackError)
        }
        updatePlaybackError(playbackError)
    }

    private fun updatePlaybackError(playbackError: PlaybackException?) {
        when {
            playbackError == null -> {
                dismissedPlaybackError = null
                error.value = null
            }

            playbackError !== dismissedPlaybackError -> {
                dismissedPlaybackError = null
                error.value = playbackError
            }
        }
    }

    private fun updateCanSkipPreviousAndNext() {
        if (!player.currentTimeline.isEmpty) {
            val window =
                player.currentTimeline.getWindow(player.currentMediaItemIndex, Timeline.Window())
            canSkipPrevious.value = player.isCommandAvailable(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) ||
                !window.isLive ||
                player.isCommandAvailable(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            canSkipNext.value = window.isLive &&
                window.isDynamic ||
                player.isCommandAvailable(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
        } else {
            canSkipPrevious.value = false
            canSkipNext.value = false
        }
    }

    fun dispose() {
        player.removeListener(this)
        metadataExtractionJob?.cancel()
        metadataExtractionJob = null
    }
}
