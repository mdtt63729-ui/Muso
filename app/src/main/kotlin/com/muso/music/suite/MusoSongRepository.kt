package com.muso.music.suite

import com.maxrave.domain.data.entities.QueueEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.entities.SongInfoEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.streams.YouTubeWatchEndpoint
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.Resource
import com.muso.music.db.MusicDatabase
import com.muso.music.db.entities.ArtistEntity
import com.muso.music.db.entities.SongArtistMap
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.models.SongItem
import com.zionhuang.innertube.models.WatchEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime

/**
 * Muso adapter of SimpMusic's SongRepository. Reads resolve against Muso's
 * own Room database; playlist continuation / radio requests go through Muso's
 * Innertube client.
 */
class MusoSongRepository(
    private val db: MusicDatabase,
) : SongRepository {

    private fun formatDuration(totalSeconds: Int): String =
        "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)

    private fun com.muso.music.db.entities.Song.toDomain(): SongEntity = SongEntity(
        videoId = song.id,
        albumId = song.albumId,
        albumName = song.albumName,
        artistId = artists.mapNotNull { it.id },
        artistName = artists.map { it.name },
        duration = formatDuration(song.duration.coerceAtLeast(0)),
        durationSeconds = song.duration.coerceAtLeast(0),
        isAvailable = true,
        isExplicit = false,
        likeStatus = if (song.liked) "LIKE" else "INDIFFERENT",
        thumbnails = song.thumbnailUrl,
        title = song.title,
        videoType = "SONG",
        category = null,
        resultType = null,
        liked = song.liked,
        totalPlayTime = song.totalPlayTime,
        inLibrary = song.inLibrary?.let {
            LocalDateTime(it.year, it.monthValue, it.dayOfMonth, it.hour, it.minute, it.second, it.nano)
        } ?: LocalDateTime(1970, 1, 1, 0, 0, 0, 0),
    )

    override fun getAllSongs(limit: Int): Flow<List<SongEntity>> = flowOf(emptyList())

    override suspend fun setInLibrary(videoId: String, inLibrary: LocalDateTime) { }

    override fun getSongsByListVideoId(listVideoId: List<String>): Flow<List<SongEntity>> =
        if (listVideoId.isEmpty()) {
            flowOf(emptyList())
        } else {
            db.dao.songsByIds(listVideoId.toTypedArray()).map { songs -> songs.map { it.toDomain() } }
        }

    override fun getDownloadedSongs(): Flow<List<SongEntity>?> = flowOf(null)

    override fun getDownloadingSongs(): Flow<List<SongEntity>?> = flowOf(null)

    override fun getPreparingSongs(): Flow<List<SongEntity>> = flowOf(emptyList())

    override fun getDownloadedVideoIdListFromListVideoIdAsFlow(listVideoId: List<String>): Flow<List<String>> =
        flowOf(emptyList())

    override fun getLikedSongs(): Flow<List<SongEntity>> = flowOf(emptyList())

    override fun getLikedSongsByArtist(channelId: String): Flow<List<SongEntity>> = flowOf(emptyList())

    override suspend fun downloadAllLikedSongs(): Int = 0

    override suspend fun clearHistoryAndOrphanedSongs(): Int = 0

    override fun getCanvasSong(max: Int): Flow<List<SongEntity>> = flowOf(emptyList())

    override fun getSongById(id: String): Flow<SongEntity?> =
        db.dao.songsByIds(arrayOf(id)).map { it.firstOrNull()?.toDomain() }

    override fun getSongAsFlow(id: String): Flow<SongEntity?> = getSongById(id)

    override fun insertSong(songEntity: SongEntity): Flow<Long> = flow {
        val inserted = runCatching {
            db.dao.insert(
                com.muso.music.db.entities.SongEntity(
                    id = songEntity.videoId,
                    title = songEntity.title,
                    duration = songEntity.durationSeconds,
                    thumbnailUrl = songEntity.thumbnails,
                    albumId = songEntity.albumId,
                    albumName = songEntity.albumName,
                ),
            )
            songEntity.artistId.orEmpty().forEachIndexed { index, artistId ->
                val artistName = songEntity.artistName?.getOrNull(index) ?: return@forEachIndexed
                db.dao.insert(ArtistEntity(id = artistId, name = artistName))
                db.dao.insert(SongArtistMap(songId = songEntity.videoId, artistId = artistId, position = index))
            }
        }
        emit(inserted.getOrDefault(1L))
    }.flowOn(Dispatchers.IO)

    override fun updateThumbnailsSongEntity(videoId: String, thumbnails: List<com.maxrave.domain.data.model.searchResult.songs.Thumbnail>) { }

    override fun updateVideoTypeSongEntity(videoId: String, videoType: String) { }

    override suspend fun updateListenCount(videoId: String) { }

    override suspend fun resetTotalPlayTime(videoId: String) { }

    override suspend fun updateLikeStatus(videoId: String, likeStatus: Int) {
        runCatching { db.dao.setLikedById(videoId, likeStatus == 1) }
    }

    override fun updateSongInLibrary(videoId: String, inLibrary: LocalDateTime) { }

    override suspend fun updateDurationSeconds(videoId: String, durationSeconds: Int) { }

    override fun getMostPlayedSongs(): Flow<List<SongEntity>> = flowOf(emptyList())

    override suspend fun updateDownloadState(videoId: String, downloadState: Int) { }

    override suspend fun getRecentSong(limit: Int, offset: Int): List<SongEntity> = emptyList()

    override suspend fun insertSongInfo(songInfo: SongInfoEntity) { }

    override fun getSongInfoEntity(videoId: String): Flow<SongInfoEntity?> = flowOf(null)

    override suspend fun getSongInfo(videoId: String): Flow<SongInfoEntity?> = flowOf(null)

    override suspend fun getLikeStatus(videoId: String): Flow<Boolean> = flowOf(false)

    override suspend fun addToYouTubeLiked(mediaId: String?): Flow<Int> = flowOf(0)

    private fun SongItem.toTrack(): Track = Track(
        album = album?.let { com.maxrave.domain.data.model.searchResult.songs.Album(id = it.id, name = it.name) },
        artists = artists.map { com.maxrave.domain.data.model.searchResult.songs.Artist(id = it.id, name = it.name) },
        duration = duration?.let { formatDuration(it) },
        durationSeconds = duration,
        isAvailable = true,
        isExplicit = explicit,
        likeStatus = "INDIFFERENT",
        thumbnails = listOf(
            com.maxrave.domain.data.model.searchResult.songs.Thumbnail(url = thumbnail, height = 544, width = 544),
        ),
        title = title,
        videoId = id,
        videoType = "SONG",
        category = null,
        feedbackTokens = null,
        resultType = null,
    )

    override fun getContinueTrack(
        playlistId: String,
        continuation: String,
        fromPlaylist: Boolean,
    ): Flow<Pair<ArrayList<Track>?, String?>> = flow {
        val result = YouTube.playlistContinuation(continuation)
        result
            .onSuccess { page ->
                emit(ArrayList(page.songs.map { it.toTrack() }) to page.continuation)
            }.onFailure {
                emit(ArrayList<Track>() to null)
            }
    }.flowOn(Dispatchers.IO)

    override suspend fun removeFromYouTubeLiked(mediaId: String?): Flow<Int> = flowOf(0)

    override fun downloadToFile(
        track: com.maxrave.domain.data.model.browse.album.Track,
        path: String,
        videoId: String,
        isVideo: Boolean,
    ): Flow<com.maxrave.domain.data.model.download.DownloadProgress> = flowOf(com.maxrave.domain.data.model.download.DownloadProgress.INIT)

    override fun getRelatedData(videoId: String): Flow<Resource<Pair<List<com.maxrave.domain.data.model.browse.album.Track>, String?>>> = getRadioFromEndpoint(
        com.maxrave.domain.data.model.streams.YouTubeWatchEndpoint(videoId = videoId),
    )

    override fun getRadioFromEndpoint(endpoint: YouTubeWatchEndpoint): Flow<Resource<Pair<List<Track>, String?>>> = flow {
        val result = YouTube.next(
            WatchEndpoint(
                videoId = endpoint.videoId,
                playlistId = endpoint.playlistId,
                playlistSetVideoId = endpoint.playlistSetVideoId,
                params = endpoint.params,
                index = endpoint.index,
            ),
        )
        result
            .onSuccess { next ->
                emit(Resource.Success(next.items.map { it.toTrack() } to next.continuation))
            }.onFailure {
                emit(Resource.Error(it.message ?: "Cannot load radio"))
            }
    }.flowOn(Dispatchers.IO)

    override suspend fun recoverQueue(temp: List<Track>) { }

    override suspend fun removeQueue() { }

    override fun getSavedQueue(): Flow<List<QueueEntity>?> = flowOf(null)
}
