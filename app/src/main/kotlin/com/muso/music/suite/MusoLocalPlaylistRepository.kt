package com.muso.music.suite

import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.browse.playlist.PlaylistState
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.utils.Resource
import com.muso.music.db.MusicDatabase
import com.muso.music.db.entities.ArtistEntity
import com.muso.music.db.entities.SongArtistMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Muso adapter of SimpMusic's LocalPlaylistRepository, backed by Muso's own
 * Room playlist tables. Muso's playlist ids are UUID strings while the suite's
 * selection flow works with Longs, so a session-stable bidirectional mapping
 * is kept here.
 */
class MusoLocalPlaylistRepository(
    private val db: MusicDatabase,
) : LocalPlaylistRepository {

    private val toSuiteId = ConcurrentHashMap<String, Long>()
    private val toMusoId = ConcurrentHashMap<Long, String>()
    private val nextId = AtomicLong(1000L)

    private fun suiteIdFor(musoId: String): Long = toSuiteId.getOrPut(musoId) {
        val id = nextId.incrementAndGet()
        toMusoId[id] = musoId
        id
    }

    private fun ensureSong(song: SongEntity) {
        runCatching {
            db.dao.insert(
                com.muso.music.db.entities.SongEntity(
                    id = song.videoId,
                    title = song.title,
                    duration = song.durationSeconds,
                    thumbnailUrl = song.thumbnails,
                    albumId = song.albumId,
                    albumName = song.albumName,
                ),
            )
            song.artistId.orEmpty().forEachIndexed { index, artistId ->
                val artistName = song.artistName?.getOrNull(index) ?: return@forEachIndexed
                db.dao.insert(ArtistEntity(id = artistId, name = artistName))
                db.dao.insert(SongArtistMap(songId = song.videoId, artistId = artistId, position = index))
            }
        }
    }

    private suspend fun musoPlaylist(suiteId: Long): com.muso.music.db.entities.Playlist? =
        toMusoId[suiteId]?.let { db.dao.playlist(it).firstOrNull() }

    override fun getAllLocalPlaylists(): Flow<List<LocalPlaylistEntity>> =
        db.dao.playlistsByCreateDateAsc().map { playlists ->
            playlists.map { playlist ->
                LocalPlaylistEntity(
                    id = suiteIdFor(playlist.playlist.id),
                    title = playlist.playlist.name,
                    thumbnail = playlist.thumbnails.firstOrNull(),
                    tracks = null,
                )
            }
        }

    override fun addTrackToLocalPlaylist(
        id: Long,
        song: SongEntity,
        successMessage: String,
        updatedYtMessage: String,
        errorMessage: String,
    ): Flow<Resource<String>> = flow {
        val playlist = musoPlaylist(id)
        if (playlist == null) {
            emit(Resource.Error(errorMessage))
            return@flow
        }
        ensureSong(song)
        val result = runCatching {
            db.transaction { db.dao.addSongToPlaylist(playlist, listOf(song.videoId)) }
        }
        if (result.isSuccess) emit(Resource.Success(successMessage)) else emit(Resource.Error(errorMessage))
    }.flowOn(Dispatchers.IO)

    override fun removeTrackFromLocalPlaylist(
        id: Long,
        song: SongEntity,
        successMessage: String,
        updatedYtMessage: String,
        errorMessage: String,
    ): Flow<Resource<String>> = flow {
        val playlist = musoPlaylist(id)
        if (playlist == null) {
            emit(Resource.Error(errorMessage))
            return@flow
        }
        val result = runCatching {
            db.transaction {
                val map = db.dao.playlistSongMaps(song.videoId).firstOrNull { it.playlistId == playlist.playlist.id }
                if (map != null) {
                    db.dao.move(playlist.playlist.id, map.position, Int.MAX_VALUE)
                    db.dao.delete(map.copy(position = Int.MAX_VALUE))
                }
            }
        }
        if (result.isSuccess) emit(Resource.Success(successMessage)) else emit(Resource.Error(errorMessage))
    }.flowOn(Dispatchers.IO)

    override fun syncYouTubePlaylistToLocalPlaylist(
        data: PlaylistState,
        tracks: List<Track>,
        syncedString: String,
        errorString: String,
    ): Flow<Resource<String>> = flow {
        val result = runCatching {
            val entity = com.muso.music.db.entities.PlaylistEntity(name = data.title)
            db.dao.insert(entity)
            toSuiteIdForPersisted(entity.id)
            db.transaction {
                tracks.forEach { track ->
                    runCatching {
                        db.dao.insert(
                            com.muso.music.db.entities.SongEntity(
                                id = track.videoId,
                                title = track.title,
                                duration = track.durationSeconds ?: 0,
                                thumbnailUrl = track.thumbnails?.lastOrNull()?.url,
                                albumId = track.album?.id,
                                albumName = track.album?.name,
                            ),
                        )
                        track.artists.orEmpty().forEachIndexed { index, artist ->
                            val artistId = artist.id ?: return@forEachIndexed
                            db.dao.insert(ArtistEntity(id = artistId, name = artist.name))
                            db.dao.insert(SongArtistMap(songId = track.videoId, artistId = artistId, position = index))
                        }
                    }
                    db.dao.insert(com.muso.music.db.entities.PlaylistSongMap(songId = track.videoId, playlistId = entity.id))
                }
            }
        }
        if (result.isSuccess) emit(Resource.Success(syncedString)) else emit(Resource.Error(errorString))
    }.flowOn(Dispatchers.IO)

    private fun toSuiteIdForPersisted(musoId: String): Long = suiteIdFor(musoId)
}
