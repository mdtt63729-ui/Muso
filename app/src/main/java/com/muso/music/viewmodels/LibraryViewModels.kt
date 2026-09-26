@file:OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)

package com.muso.music.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import com.zionhuang.innertube.YouTube
import com.muso.music.constants.AlbumFilter
import com.muso.music.constants.AlbumFilterKey
import com.muso.music.constants.AlbumSortDescendingKey
import com.muso.music.constants.AlbumSortType
import com.muso.music.constants.AlbumSortTypeKey
import com.muso.music.constants.ArtistFilter
import com.muso.music.constants.ArtistFilterKey
import com.muso.music.constants.ArtistSongSortDescendingKey
import com.muso.music.constants.ArtistSongSortType
import com.muso.music.constants.ArtistSongSortTypeKey
import com.muso.music.constants.ArtistSortDescendingKey
import com.muso.music.constants.ArtistSortType
import com.muso.music.constants.ArtistSortTypeKey
import com.muso.music.constants.PlaylistSortDescendingKey
import com.muso.music.constants.PlaylistSortType
import com.muso.music.constants.PlaylistSortTypeKey
import com.muso.music.constants.SongFilter
import com.muso.music.constants.SongFilterKey
import com.muso.music.constants.SongSortDescendingKey
import com.muso.music.constants.SongSortType
import com.muso.music.constants.SongSortTypeKey
import com.muso.music.db.MusicDatabase
import com.muso.music.extensions.reversed
import com.muso.music.extensions.toEnum
import com.muso.music.playback.DownloadUtil
import com.muso.music.utils.dataStore
import com.muso.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class LibrarySongsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    downloadUtil: DownloadUtil,
) : ViewModel() {
    val allSongs = context.dataStore.data
        .map {
            Triple(
                it[SongFilterKey].toEnum(SongFilter.LIBRARY),
                it[SongSortTypeKey].toEnum(SongSortType.CREATE_DATE),
                (it[SongSortDescendingKey] ?: true)
            )
        }
        .distinctUntilChanged()
        .flatMapLatest { (filter, sortType, descending) ->
            when (filter) {
                SongFilter.LIBRARY -> database.songs(sortType, descending)
                SongFilter.LIKED -> database.likedSongs(sortType, descending)
                SongFilter.DOWNLOADED -> downloadUtil.downloads.flatMapLatest { downloads ->
                    database.allSongs()
                        .flowOn(Dispatchers.IO)
                        .map { songs ->
                            songs.filter {
                                downloads[it.id]?.state == Download.STATE_COMPLETED
                            }
                        }
                        .map { songs ->
                            when (sortType) {
                                SongSortType.CREATE_DATE -> songs.sortedBy { downloads[it.id]?.updateTimeMs ?: 0L }
                                SongSortType.NAME -> songs.sortedBy { it.song.title }
                                SongSortType.ARTIST -> songs.sortedBy { song ->
                                    song.artists.joinToString(separator = "") { it.name }
                                }

                                SongSortType.PLAY_TIME -> songs.sortedBy { it.song.totalPlayTime }
                            }.reversed(descending)
                        }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, null)
}

@HiltViewModel
class LibraryArtistsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
) : ViewModel() {
    val allArtists = context.dataStore.data
        .map {
            Triple(
                it[ArtistFilterKey].toEnum(ArtistFilter.LIBRARY),
                it[ArtistSortTypeKey].toEnum(ArtistSortType.CREATE_DATE),
                it[ArtistSortDescendingKey] ?: true
            )
        }
        .distinctUntilChanged()
        .flatMapLatest { (filter, sortType, descending) ->
            when (filter) {
                ArtistFilter.LIBRARY -> database.artists(sortType, descending)
                ArtistFilter.LIKED -> database.artistsBookmarked(sortType, descending)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            allArtists.collect { artists ->
                artists
                    ?.map { it.artist }
                    ?.filter {
                        it.thumbnailUrl == null || Duration.between(it.lastUpdateTime, LocalDateTime.now()) > Duration.ofDays(10)
                    }
                    ?.forEach { artist ->
                        YouTube.artist(artist.id).onSuccess { artistPage ->
                            database.query {
                                update(artist, artistPage)
                            }
                        }
                    }
            }
        }
    }
}

@HiltViewModel
class LibraryAlbumsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
) : ViewModel() {
    val allAlbums = context.dataStore.data
        .map {
            Triple(
                it[AlbumFilterKey].toEnum(AlbumFilter.LIBRARY),
                it[AlbumSortTypeKey].toEnum(AlbumSortType.CREATE_DATE),
                it[AlbumSortDescendingKey] ?: true
            )
        }
        .distinctUntilChanged()
        .flatMapLatest { (filter, sortType, descending) ->
            when (filter) {
                AlbumFilter.LIBRARY -> database.albums(sortType, descending)
                AlbumFilter.LIKED -> database.albumsLiked(sortType, descending)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            allAlbums.collect { albums ->
                albums
                    ?.filter {
                        it.album.songCount == 0
                    }
                    ?.forEach { album ->
                        YouTube.album(album.id).onSuccess { albumPage ->
                            database.query {
                                update(album.album, albumPage)
                            }
                        }.onFailure {
                            reportException(it)
                            if (it.message?.contains("NOT_FOUND") == true) {
                                database.query {
                                    delete(album.album)
                                }
                            }
                        }
                    }
            }
        }
    }
}

@HiltViewModel
class LibraryPlaylistsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
) : ViewModel() {
    val allPlaylists = context.dataStore.data
        .map {
            it[PlaylistSortTypeKey].toEnum(PlaylistSortType.CREATE_DATE) to (it[PlaylistSortDescendingKey] ?: true)
        }
        .distinctUntilChanged()
        .flatMapLatest { (sortType, descending) ->
            database.playlists(sortType, descending)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
}

@HiltViewModel
class ArtistSongsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val artistId = savedStateHandle.get<String>("artistId")!!
    val artist = database.artist(artistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val songs = context.dataStore.data
        .map {
            it[ArtistSongSortTypeKey].toEnum(ArtistSongSortType.CREATE_DATE) to (it[ArtistSongSortDescendingKey] ?: true)
        }
        .distinctUntilChanged()
        .flatMapLatest { (sortType, descending) ->
            database.artistSongs(artistId, sortType, descending)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}

/**
 * Feeds the ReTune-style mixed library: songs / artists / albums / playlists, with a
 * debounced in-library search query.
 */
@HiltViewModel
class LibraryMixViewModel @Inject constructor(
    database: MusicDatabase,
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val debouncedSearchQuery = _searchQuery
        .debounce(250)
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val songs = database.songs(SongSortType.CREATE_DATE, true)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val albums = database.albums(AlbumSortType.CREATE_DATE, true)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val artists = database.artists(ArtistSortType.CREATE_DATE, true)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val playlists = database.playlists(PlaylistSortType.CREATE_DATE, true)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}

/**
 * Backs the auto playlists (Liked / Offline) opened from the library mix grid.
 */
@HiltViewModel
class AutoPlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    database: MusicDatabase,
    downloadUtil: DownloadUtil,
) : ViewModel() {
    val playlist: String = checkNotNull(savedStateHandle["playlist"])

    val songs = when (playlist) {
        "liked" -> database.likedSongs(SongSortType.CREATE_DATE, true)
        else -> downloadUtil.downloads.flatMapLatest { downloads ->
            database.allSongs().map { songs ->
                songs.filter { downloads[it.id]?.state == Download.STATE_COMPLETED }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
