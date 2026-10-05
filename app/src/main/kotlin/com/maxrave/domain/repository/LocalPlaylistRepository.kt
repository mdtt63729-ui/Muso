package com.maxrave.domain.repository

import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.browse.playlist.PlaylistState
import com.maxrave.domain.utils.LocalResource
import kotlinx.coroutines.flow.Flow

/**
 * Muso integration shim of SimpMusic's local-playlist repository. The suite's
 * selection sheet lists local playlists through [getAllLocalPlaylists] and
 * adds/removes songs through the track methods; the YouTube sync entry backs
 * the playlist screen's "save to local" action. The Muso-backed implementation
 * lives in com.muso.music.suite.MusoLocalPlaylistRepository.
 */
interface LocalPlaylistRepository {
    fun getAllLocalPlaylists(): Flow<List<LocalPlaylistEntity>>

    fun addTrackToLocalPlaylist(
        id: Long,
        song: SongEntity,
        successMessage: String,
        updatedYtMessage: String,
        errorMessage: String,
    ): Flow<LocalResource<String>>

    fun removeTrackFromLocalPlaylist(
        id: Long,
        song: SongEntity,
        successMessage: String,
        updatedYtMessage: String,
        errorMessage: String,
    ): Flow<LocalResource<String>>

    fun syncYouTubePlaylistToLocalPlaylist(
        data: PlaylistState,
        tracks: List<Track>,
        syncedString: String,
        errorString: String,
    ): Flow<LocalResource<String>>
}

class NoopLocalPlaylistRepository : LocalPlaylistRepository {
    override fun getAllLocalPlaylists(): Flow<List<LocalPlaylistEntity>> =
        kotlinx.coroutines.flow.flowOf(emptyList())

    override fun addTrackToLocalPlaylist(
        id: Long,
        song: SongEntity,
        successMessage: String,
        updatedYtMessage: String,
        errorMessage: String,
    ): Flow<LocalResource<String>> = kotlinx.coroutines.flow.flowOf(LocalResource.Error(errorMessage))

    override fun removeTrackFromLocalPlaylist(
        id: Long,
        song: SongEntity,
        successMessage: String,
        updatedYtMessage: String,
        errorMessage: String,
    ): Flow<LocalResource<String>> = kotlinx.coroutines.flow.flowOf(LocalResource.Error(errorMessage))

    override fun syncYouTubePlaylistToLocalPlaylist(
        data: PlaylistState,
        tracks: List<Track>,
        syncedString: String,
        errorString: String,
    ): Flow<LocalResource<String>> = kotlinx.coroutines.flow.flowOf(LocalResource.Error(errorString))
}
