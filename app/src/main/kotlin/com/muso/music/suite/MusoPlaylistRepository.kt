package com.muso.music.suite

import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.browse.playlist.Author
import com.maxrave.domain.data.model.browse.playlist.PlaylistBrowse
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.model.searchResult.songs.Thumbnail
import com.maxrave.domain.data.type.ChartItem
import com.maxrave.domain.data.type.PlaylistType
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.utils.Resource
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.models.SongItem
import com.zionhuang.innertube.models.WatchEndpoint
import com.zionhuang.innertube.pages.PlaylistPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.datetime.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * Muso adapter of SimpMusic's PlaylistRepository. Playlist metadata is held in
 * an in-memory session store (backed by live flows so liked/download state
 * changes re-emit), and playlist/radio data is fetched straight from YouTube
 * through Muso's Innertube client — exactly what Muso's own online playlist
 * screen uses.
 */
class MusoPlaylistRepository : PlaylistRepository {

    private val store = ConcurrentHashMap<String, MutableStateFlow<PlaylistEntity?>>()

    private fun flowFor(id: String): MutableStateFlow<PlaylistEntity?> =
        store.getOrPut(id) { MutableStateFlow(null) }

    // --- fetch: Muso's Innertube client ---

    private fun SongItem.toTrack(): Track = Track(
        album = album?.let { com.maxrave.domain.data.model.searchResult.songs.Album(id = it.id, name = it.name) },
        artists = artists.map { com.maxrave.domain.data.model.searchResult.songs.Artist(id = it.id, name = it.name) },
        duration = duration?.let { formatDuration(it) },
        durationSeconds = duration,
        isAvailable = true,
        isExplicit = explicit,
        likeStatus = "INDIFFERENT",
        thumbnails = listOf(Thumbnail(url = thumbnail, height = 544, width = 544)),
        title = title,
        videoId = id,
        videoType = "SONG",
        category = null,
        feedbackTokens = null,
        resultType = null,
    )

    private fun formatDuration(totalSeconds: Int): String =
        "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)

    private fun PlaylistPage.toBrowse(): PlaylistBrowse = PlaylistBrowse(
        author = Author(id = playlist.author?.id ?: "", name = playlist.author?.name ?: ""),
        description = "",
        duration = "",
        durationSeconds = 0,
        id = playlist.id,
        privacy = "PUBLIC",
        thumbnails = listOf(Thumbnail(url = playlist.thumbnail, height = 544, width = 544)),
        title = playlist.title,
        trackCount = songs.size,
        tracks = songs.map { it.toTrack() },
        year = "",
        shuffleEndpoint = null,
        radioEndpoint = null,
    )

    /**
     * Some public YouTube Music playlists occasionally fail the strict playlist-page parser
     * (usually after a backend response shape change) even though the same playlist can still
     * be opened through the watch/queue endpoint. Keep the normal playlist page as the first
     * path, then fall back to the queue endpoint so a playlist tap never becomes the generic
     * "Empty response" screen just because one parser failed.
     */
    private suspend fun loadPlaylistWithFallback(playlistId: String): Result<Pair<PlaylistBrowse, String?>> {
        val normalizedId =
            playlistId
                .trim()
                .removePrefix("VL")
                .removePrefix("vl")

        val primary = YouTube.playlist(normalizedId)
        primary.onSuccess { page ->
            return Result.success(page.toBrowse() to page.songsContinuation)
        }

        val primaryError = primary.exceptionOrNull()

        val fallback = YouTube.next(WatchEndpoint(playlistId = normalizedId))
        fallback.onSuccess { next ->
            if (next.items.isNotEmpty()) {
                val thumbnail = next.items.firstOrNull()?.thumbnail.orEmpty()
                val browse =
                    PlaylistBrowse(
                        author = Author(id = "", name = "YouTube Music"),
                        description = "",
                        duration = "",
                        durationSeconds = 0,
                        id = normalizedId,
                        privacy = "PUBLIC",
                        thumbnails =
                            thumbnail
                                .takeIf(String::isNotBlank)
                                ?.let { listOf(Thumbnail(url = it, height = 544, width = 544)) }
                                .orEmpty(),
                        title = next.title?.takeIf(String::isNotBlank) ?: "Playlist",
                        trackCount = next.items.size,
                        tracks = next.items.map { it.toTrack() },
                        year = "",
                        shuffleEndpoint = null,
                        radioEndpoint = null,
                    )
                return Result.success(browse to next.continuation)
            }
        }

        return Result.failure(
            primaryError ?: fallback.exceptionOrNull() ?: IllegalStateException("Cannot load playlist"),
        )
    }

    override fun getPlaylistData(
        playlistId: String,
        viewString: String,
    ): Flow<Resource<Pair<PlaylistBrowse, String?>>> = flow {
        loadPlaylistWithFallback(playlistId)
            .onSuccess { emit(Resource.Success(it)) }
            .onFailure { emit(Resource.Error(it.message ?: "Cannot load playlist")) }
    }.flowOn(Dispatchers.IO)

    override fun getFullPlaylistData(
        playlistId: String,
        viewString: String,
    ): Flow<Resource<PlaylistBrowse>> = flow {
        loadPlaylistWithFallback(playlistId)
            .onSuccess { emit(Resource.Success(it.first)) }
            .onFailure { emit(Resource.Error(it.message ?: "Cannot load playlist")) }
    }.flowOn(Dispatchers.IO)

    private fun radioFlow(
        radioId: String,
        defaultDescription: String,
    ): Flow<Resource<Pair<PlaylistBrowse, String?>>> = flow {
        val endpoint = WatchEndpoint(playlistId = radioId)
        val result = YouTube.next(endpoint)
        result
            .onSuccess { next ->
                val browse = PlaylistBrowse(
                    author = Author(id = "", name = "YouTube Music"),
                    description = defaultDescription,
                    duration = "",
                    durationSeconds = 0,
                    id = radioId,
                    privacy = "PRIVATE",
                    thumbnails = emptyList(),
                    title = next.title ?: "Radio",
                    trackCount = next.items.size,
                    tracks = next.items.map { it.toTrack() },
                    year = "",
                    shuffleEndpoint = null,
                    radioEndpoint = null,
                )
                emit(Resource.Success(browse to next.continuation))
            }.onFailure {
                emit(Resource.Error(it.message ?: "Cannot load radio"))
            }
    }.flowOn(Dispatchers.IO)

    override fun getRadio(
        radioId: String,
        defaultDescription: String,
        radioString: String,
        viewString: String,
        originalTrack: SongEntity?,
        artist: ArtistEntity?,
    ): Flow<Resource<Pair<PlaylistBrowse, String?>>> = radioFlow(radioId, defaultDescription)

    override fun getRDATRadioData(
        radioId: String,
        viewString: String,
    ): Flow<Resource<Pair<PlaylistBrowse, String?>>> = radioFlow(radioId, "")

    // --- in-memory session store ---

    override fun getAllPlaylists(limit: Int): Flow<List<PlaylistEntity>> =
        flowOf(store.values.mapNotNull { it.value }.take(limit))

    override fun getPlaylist(id: String): Flow<PlaylistEntity?> = flowFor(id)

    override fun getLikedPlaylists(): Flow<List<PlaylistEntity>> =
        flowOf(store.values.mapNotNull { it.value }.filter { it.liked })

    override suspend fun insertPlaylist(playlistEntity: PlaylistEntity) {
        flowFor(playlistEntity.id).value = playlistEntity
    }

    override suspend fun insertAndReplacePlaylist(playlistEntity: PlaylistEntity) {
        flowFor(playlistEntity.id).value = playlistEntity
    }

    override suspend fun insertRadioPlaylist(playlistEntity: PlaylistEntity) {
        flowFor(playlistEntity.id).value = playlistEntity
    }

    override suspend fun updatePlaylistLiked(playlistId: String, likeStatus: Int) {
        flowFor(playlistId).value = flowFor(playlistId).value?.copy(liked = likeStatus == 1)
    }

    override suspend fun updatePlaylistInLibrary(inLibrary: LocalDateTime, playlistId: String) {
        flowFor(playlistId).value = flowFor(playlistId).value?.copy(inLibrary = inLibrary)
    }

    override suspend fun updatePlaylistDownloadState(playlistId: String, downloadState: Int) {
        flowFor(playlistId).value = flowFor(playlistId).value?.copy(downloadState = downloadState)
    }

    // --- not applicable in Muso ---

    override fun getAllDownloadedPlaylist(): Flow<List<PlaylistType>> = flowOf(emptyList())

    override fun getAllDownloadingPlaylist(): Flow<List<PlaylistType>> = flowOf(emptyList())

    override fun getLibraryPlaylist(): Flow<List<PlaylistsResult>?> = flowOf(null)

    override fun getMixedForYou(): Flow<List<PlaylistsResult>?> = flowOf(null)

    override fun updateYourYouTubePlaylistTitle(
        playlistId: String,
        newTitle: String,
    ): Flow<Resource<String>> = flowOf(Resource.Error("Not supported"))

    override suspend fun insertYourYouTubePlaylist(yourYouTubePlaylist: com.maxrave.domain.data.entities.YourYouTubePlaylistList) { }

    override fun getYourYouTubePlaylistList(emailPageId: String): Flow<com.maxrave.domain.data.entities.YourYouTubePlaylistList?> = flowOf(null)

    override suspend fun deleteAllYourYouTubePlaylist() { }

    override fun getChartPlaylist(): Flow<Resource<List<ChartItem>>> = flow {
        emit(Resource.Error("Not supported"))
    }
}
