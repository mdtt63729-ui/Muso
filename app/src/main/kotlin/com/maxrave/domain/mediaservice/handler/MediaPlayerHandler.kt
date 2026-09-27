package com.maxrave.domain.mediaservice.handler

import com.maxrave.domain.data.model.browse.album.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Muso integration shim for the SimpMusic player suite. The suite's queue view
 * reads queueData / currentOrderIndex and reorders through these methods; the
 * Muso host provides the real queue through NowPlayingContentState and routes
 * onSeekToQueueIndex / onMoveQueueItem / onRemoveQueueItem directly to the
 * player connection. This stub exists so koinInject() resolves inside the
 * suite's UI files.
 */
/**
 * MUSO adapter of the SimpMusic MediaPlayerHandler: the playlist UI's
 * ViewModels drive playback through these methods and read
 * [nowPlayingState] / [controlState]. The real behaviour is supplied by the
 * MusoSuiteBridge, which implements [Command]s against Muso's
 * PlayerConnection; until a bridge attaches, everything is inert.
 */
class MediaPlayerHandler {
    /** Set by MusoSuiteBridge; the single place playback actually happens. */
    var commandSink: ((Command) -> Unit)? = null

    val nowPlayingState = MutableStateFlow(NowPlayingTrackState.initial())
    val controlState = MutableStateFlow(
        ControlState(
            isPlaying = false, isShuffle = false, repeatState = RepeatState.None,
            isLiked = false, isNextAvailable = false, isPreviousAvailable = false,
            isCrossfading = false, volume = 1f,
        )
    )

    private val _queueData = MutableStateFlow<QueueData?>(null)
    val queueData: StateFlow<QueueData?> = _queueData.asStateFlow()

    fun currentOrderIndex(): Int = -1

    fun removeMediaItem(position: Int) { commandSink?.invoke(Command.RemoveMediaItem(position)) }

    fun playMediaItemInMediaSource(index: Int) { commandSink?.invoke(Command.PlayMediaItemInMediaSource(index)) }

    suspend fun swap(from: Int, to: Int) { commandSink?.invoke(Command.Swap(from, to)) }

    fun loadMore() { }

    suspend fun moveItemUp(position: Int) { commandSink?.invoke(Command.MoveItem(position, -1)) }

    suspend fun moveItemDown(position: Int) { commandSink?.invoke(Command.MoveItem(position, +1)) }

    // --- the playlist screen's playback surface ---

    fun reset() { commandSink?.invoke(Command.Reset) }

    fun setQueueData(queueData: QueueData) {
        if (queueData is QueueData.Data) _queueData.value = queueData
        commandSink?.invoke(Command.SetQueueData(queueData))
    }

    fun <T> loadMediaItem(
        anyTrack: T,
        type: String,
        index: Int? = null,
    ) {
        @Suppress("UNCHECKED_CAST")
        commandSink?.invoke(Command.LoadItem(anyTrack as Any, type, index))
    }

    fun shufflePlaylist(firstPlayIndex: Int = 0) {
        commandSink?.invoke(Command.ShufflePlaylist(firstPlayIndex))
    }

    fun playNext(track: com.maxrave.domain.data.model.browse.album.Track) {
        commandSink?.invoke(Command.PlayNext(track))
    }

    fun loadMoreCatalog(
        listTracks: ArrayList<com.maxrave.domain.data.model.browse.album.Track>,
        isAddToQueue: Boolean = false,
    ) {
        commandSink?.invoke(Command.AddToQueue(listTracks, isAddToQueue))
    }

    /** Playback commands the MusoSuiteBridge executes against Muso's player. */
    sealed class Command {
        data object Reset : Command()
        data class SetQueueData(val queueData: QueueData) : Command()
        data class LoadItem(val track: Any, val type: String, val index: Int?) : Command()
        data class ShufflePlaylist(val firstPlayIndex: Int) : Command()
        data class PlayNext(val track: com.maxrave.domain.data.model.browse.album.Track) : Command()
        data class AddToQueue(
            val tracks: ArrayList<com.maxrave.domain.data.model.browse.album.Track>,
            val isAddToQueue: Boolean,
        ) : Command()
        data class RemoveMediaItem(val position: Int) : Command()
        data class PlayMediaItemInMediaSource(val index: Int) : Command()
        data class Swap(val from: Int, val to: Int) : Command()
        data class MoveItem(val position: Int, val direction: Int) : Command()
    }
}





sealed class RepeatState {
    data object None : RepeatState()

    data object All : RepeatState()

    data object One : RepeatState()
}

data class ControlState(
    val isPlaying: Boolean,
    val isShuffle: Boolean,
    val repeatState: RepeatState,
    val isLiked: Boolean,
    val isNextAvailable: Boolean,
    val isPreviousAvailable: Boolean,
    val isCrossfading: Boolean,
    val volume: Float, // 0f..1f
)

data class NowPlayingTrackState(
    val mediaItem: GenericMediaItem,
    val track: Track?,
    val songEntity: SongEntity?,
) {
    fun isNotEmpty(): Boolean = this != initial()

    companion object {
        fun initial(): NowPlayingTrackState =
            NowPlayingTrackState(
                mediaItem = GenericMediaItem.EMPTY,
                track = null,
                songEntity = null,
            )
    }
}

data class SleepTimerState(
    val isDone: Boolean,
    val timeRemaining: Int,
)

data class QueueData(
    val queueState: StateSource = StateSource.STATE_CREATED,
    val data: Data = Data(),
) {
    data class Data(
        val listTracks: List<Track> = arrayListOf(),
        val firstPlayedTrack: Track? = null,
        val playlistId: String? = null,
        val playlistName: String? = null,
        val playlistType: PlaylistType? = null,
        val continuation: String? = null,
    )

    enum class StateSource {
        STATE_CREATED,
        STATE_INITIALIZING,
        STATE_INITIALIZED,
        STATE_ERROR,
    }

    fun addTrackList(tracks: Collection<Track>): QueueData {
        val temp = this.data.listTracks.toMutableList()
        temp.addAll(tracks)
        return this.copy(
            data =
                this.data.copy(
                    listTracks = temp,
                ),
        )
    }

    fun addToIndex(
        track: Track,
        index: Int,
    ): QueueData {
        val temp = this.data.listTracks.toMutableList()
        temp.add(index, track)
        return this.copy(
            data =
                this.data.copy(
                    listTracks = temp,
                ),
        )
    }

    fun removeFirstTrackForPlaylistAndAlbum(): QueueData {
        val temp = this.data.listTracks.toMutableList()
        temp.removeAt(0)
        return this.copy(
            data =
                this.data.copy(
                    listTracks = temp,
                    firstPlayedTrack = null,
                ),
        )
    }

    fun removeTrackWithIndex(index: Int): QueueData {
        val temp = this.data.listTracks.toMutableList()
        temp.removeAt(index)
        return this.copy(
            data =
                this.data.copy(
                    listTracks = temp,
                ),
        )
    }

    fun setContinuation(continuation: String): QueueData =
        this.copy(
            data =
                this.data.copy(
                    continuation = continuation,
                ),
        )

    fun isLocalPlaylist(): Boolean = this.data.playlistType == PlaylistType.LOCAL_PLAYLIST

    fun isRadio(): Boolean = this.data.playlistType == PlaylistType.RADIO

    /** True for both plain playlists and albums — an album queue is a playlist that knows its origin. */
    fun isPlaylist(): Boolean =
        this.data.playlistType == PlaylistType.PLAYLIST ||
            this.data.playlistType == PlaylistType.ALBUM
}

enum class PlaylistType {
    PLAYLIST,

    /**
     * A queue loaded from an album. Behaves exactly like [PLAYLIST] everywhere else — it exists so
     * playback can tell that the tracks were sequenced together deliberately, which is what lets
     * crossfade step aside inside an album while still fading into whatever is queued after it.
     */
    ALBUM,
    LOCAL_PLAYLIST,
    RADIO,
}

sealed class ToastType(
    extra: String? = null,
) {
    data object ExplicitContent : ToastType()

    data class PlayerError(
        val error: String,
    ) : ToastType(error)

    data class SponsorBlockSkip(
        val category: String,
    ) : ToastType(category)
}
