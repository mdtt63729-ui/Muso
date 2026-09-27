package com.maxrave.simpmusic.viewModel

import androidx.compose.ui.graphics.ImageBitmap
import com.maxrave.domain.data.entities.NewFormatEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.download.DownloadProgress
import com.maxrave.domain.data.entities.SongInfoEntity
import com.maxrave.domain.data.model.metadata.Lyrics
import com.maxrave.domain.data.model.streams.TimeLine
import com.maxrave.domain.mediaservice.handler.ControlState
import com.maxrave.domain.mediaservice.handler.NowPlayingTrackState
import com.maxrave.domain.mediaservice.handler.RepeatState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.maxrave.domain.manager.DataStoreManager
import kotlinx.coroutines.flow.asStateFlow

/**
 * Muso integration shim for the SimpMusic player suite: the UI files read only
 * these members; the Muso host (MusoSuiteHost) supplies the real data through
 * NowPlayingContentState and routes the events through NowPlayingContentActions.
 */
class SharedViewModel(
    /** Backed by SimpMusic's DataStore in the upstream app; Muso feeds it from its own preferences. */
    private val dataStoreManager: DataStoreManager? = null,
    /** The suite's media handler; the MusoSuiteBridge keeps it fed and wired to Muso's player. */
    private val mediaPlayerHandler: com.maxrave.domain.mediaservice.handler.MediaPlayerHandler? = null,
) {
    private val _lastPlayerViewTab = MutableStateFlow<String?>(null)
    val lastPlayerViewTab: StateFlow<String?> = _lastPlayerViewTab.asStateFlow()
    fun setLastPlayerViewTab(tabName: String) {
        _lastPlayerViewTab.value = tabName
    }

    val format: SharedFlow<NewFormatEntity?> = MutableSharedFlow()
    val extractSource: StateFlow<String?> = MutableStateFlow(null)

    /**
     * LIVE player state. In upstream SimpMusic the real SharedViewModel fills
     * these from its media backend; here [com.muso.music.ui.player.MusoSuiteBridge]
     * pushes Muso's PlayerConnection state into them every time it changes,
     * which is what the suite's MiniPlayer (glass navigation bar) reads.
     */
    val nowPlayingState = MutableStateFlow<NowPlayingTrackState?>(null)
    val controllerState = MutableStateFlow(
        ControlState(
            isPlaying = false, isShuffle = false, repeatState = RepeatState.None,
            isLiked = false, isNextAvailable = false, isPreviousAvailable = false,
            isCrossfading = false, volume = 1f,
        )
    )
    val timeline = MutableStateFlow(TimeLine(0, 0, 0, loading = false))
    val nowPlayingScreenData: MutableStateFlow<NowPlayingScreenData> =
        MutableStateFlow(NowPlayingScreenData.initial())
    val likeStatus: StateFlow<Boolean> = MutableStateFlow(false)
    val downloadFileProgress: StateFlow<DownloadProgress> = MutableStateFlow(DownloadProgress.INIT)

    /** Set by MusoSuiteBridge: routes suite UIEvents into Muso's player. */
    var eventSink: ((UIEvent) -> Unit)? = null
    var stopSink: (() -> Unit)? = null
    var isServiceRunning = false

    fun getEnableLiquidGlass() = dataStoreManager?.enableLiquidGlass ?: MutableStateFlow(DataStoreManager.FALSE)
    fun getLyricsOffsetMs() = dataStoreManager?.lyricsOffsetMs ?: MutableStateFlow(0)
    fun getLyricsStyle() = dataStoreManager?.lyricsStyle ?: MutableStateFlow(DataStoreManager.LYRICS_STYLE_CLASSIC)
    fun onUIEvent(uiEvent: UIEvent) { eventSink?.invoke(uiEvent) }
    fun stopPlayer() { stopSink?.invoke() }
    fun getQueueDataState(): kotlinx.coroutines.flow.StateFlow<com.maxrave.domain.mediaservice.handler.QueueData?> =
        mediaPlayerHandler?.queueData ?: kotlinx.coroutines.flow.MutableStateFlow(null)

    // --- reference NowPlayingScreen (v0.5.145 port) surface ---
    /** Cast is not a Muso feature: a constant "not casting" state. */
    val castState: StateFlow<com.maxrave.domain.data.player.GenericCastState> =
        MutableStateFlow(com.maxrave.domain.data.player.GenericCastState.NOT_CASTING)

    /** The reference shows the video surface only for video SONGS; Muso's
     * video layer is the canvas inside the content styles, so this stays off. */
    val getVideo: StateFlow<Boolean> = MutableStateFlow(false)

    /** LRCLIB lyrics voting is not wired in Muso; the vote dialog renders neutral. */
    val translatedVoteState: StateFlow<VoteData?> = MutableStateFlow(null)
    val lyricsVoteState: StateFlow<VoteData?> = MutableStateFlow(null)
    fun voteLyrics(upvote: Boolean) { }
    fun voteTranslatedLyrics(upvote: Boolean) { }

    /** One-shot "open the fullscreen lyrics view" request flag. */
    private val _fullscreenLyricsRequest = MutableStateFlow(false)
    val fullscreenLyricsRequest: StateFlow<Boolean> = _fullscreenLyricsRequest.asStateFlow()
    fun requestFullscreenLyrics() { _fullscreenLyricsRequest.value = true }
    fun consumeFullscreenLyricsRequest() { _fullscreenLyricsRequest.value = false }

    /** Artwork bitmap pushed by the player content (palette source of truth). */
    private val _artworkBitmap = MutableStateFlow<ImageBitmap?>(null)
    val artworkBitmap: StateFlow<ImageBitmap?> = _artworkBitmap.asStateFlow()
    fun setBitmap(bitmap: ImageBitmap?) { _artworkBitmap.value = bitmap }

    /** Muso has no YouTube login; the reference's login-gated UI stays hidden. */
    fun isUserLoggedInFlow(): kotlinx.coroutines.flow.Flow<Boolean> = MutableStateFlow(false)

    /** The player style the reference NowPlayingScreen should render. */
    fun getNowPlayingStyle(): kotlinx.coroutines.flow.Flow<String> =
        dataStoreManager?.nowPlayingStyle ?: MutableStateFlow(DataStoreManager.NOW_PLAYING_STYLE_SPOTIFY)

    /** The bridge installs this to route the reference's YouTube-like button to Muso's like. */
    var addToYouTubeLikedSink: (() -> Unit)? = null
    fun addToYouTubeLiked() { addToYouTubeLikedSink?.invoke() }

    fun addListToQueue(listTrack: ArrayList<Track>) {
        mediaPlayerHandler?.loadMoreCatalog(ArrayList(listTrack), true)
    }
    fun downloadFile(bitmap: ImageBitmap) { }
    fun downloadFileDone() { }
}

sealed class UIEvent {
    data object PlayPause : UIEvent()

    data object Backward : UIEvent()

    data object Forward : UIEvent()

    data object Next : UIEvent()

    data object Previous : UIEvent()

    /**
     * Always advances to the previous track — bypasses the 3-second
     * "seek to start of current track" rule used by [Previous]. Used by the
     * NowPlaying artwork pager swipe.
     */
    data object SkipToPrevious : UIEvent()

    data object Stop : UIEvent()

    data object Shuffle : UIEvent()

    data object Repeat : UIEvent()

    data class UpdateProgress(
        val newProgress: Float,
    ) : UIEvent()

    data class UpdateVolume(
        val newVolume: Float,
    ) : UIEvent()

    data object ToggleLike : UIEvent()
}

enum class LyricsProvider {
    SIMPMUSIC,
    YOUTUBE,
    SPOTIFY,
    LRCLIB,
    BETTER_LYRICS,
    AI,
    OFFLINE,
}

data class NowPlayingScreenData(
    val playlistName: String,
    val nowPlayingTitle: String,
    val artistName: String,
    val isVideo: Boolean,
    val isExplicit: Boolean = false,
    val thumbnailURL: String?,
    val canvasData: CanvasData? = null,
    val lyricsData: LyricsData? = null,
    val songInfoData: SongInfoEntity? = null,
    val bitmap: ImageBitmap? = null,
) {
    data class CanvasData(
        val isVideo: Boolean,
        val url: String,
    )

    data class LyricsData(
        val lyrics: Lyrics,
        val translatedLyrics: Pair<Lyrics, LyricsProvider>? = null,
        val lyricsProvider: LyricsProvider,
    )

    companion object {
        fun initial(): NowPlayingScreenData =
            NowPlayingScreenData(
                nowPlayingTitle = "",
                artistName = "",
                isVideo = false,
                thumbnailURL = null,
                canvasData = null,
                lyricsData = null,
                songInfoData = null,
                playlistName = "",
            )
    }
}

data class VoteData(
    val id: String,
    val vote: Int,
    val state: VoteState,
)

sealed class VoteState {
    data object Idle : VoteState()

    data object Loading : VoteState()

    data class Success(
        val upvote: Boolean,
    ) : VoteState()

    data class Error(
        val message: String,
    ) : VoteState()
}

/**
 * Whether a stored canvas url points at something a player should open rather than an image.
 *
 * The column holds whatever the active source wrote: a Spotify canvas is an `.mp4`, while AM
 * animated artwork is an HLS `.m3u8` master playlist. Testing only for `.mp4` — as this did before
 * AM existed — sends every AM artwork down the still-image branch, and because the branch that
 * reads this is the one that restores a *cached* url, the failure only appears from the second play
 * of a track onwards.
 */
private fun String.isCanvasVideoUrl(): Boolean = contains(".mp4") || contains(".m3u8")
