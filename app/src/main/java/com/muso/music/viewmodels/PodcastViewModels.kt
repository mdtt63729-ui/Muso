package com.muso.music.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.datasource.cache.SimpleCache
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.models.SongItem
import com.zionhuang.innertube.pages.PodcastShowItem
import com.muso.music.di.PlayerCache
import com.muso.music.db.MusicDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * View models for the online library extras (ReTune ports): saved podcasts, podcast
 * episodes, uploaded songs, and the locally cached songs.
 */
@HiltViewModel
class PodcastsViewModel @Inject constructor() : ViewModel() {
    private val _shows = MutableStateFlow<Result<List<PodcastShowItem>>?>(null)
    val shows = _shows.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _shows.value = YouTube.savedPodcasts()
        }
    }
}

@HiltViewModel
class PodcastViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val podcastId: String = checkNotNull(savedStateHandle["podcastId"])

    private val _episodes = MutableStateFlow<Result<List<SongItem>>?>(null)
    val episodes = _episodes.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _episodes.value = YouTube.podcastEpisodes(podcastId)
        }
    }
}

@HiltViewModel
class UploadedViewModel @Inject constructor() : ViewModel() {
    private val _songs = MutableStateFlow<Result<List<SongItem>>?>(null)
    val songs = _songs.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _songs.value = YouTube.uploadedSongs()
        }
    }
}

@HiltViewModel
class CachedViewModel @Inject constructor(
    @PlayerCache private val playerCache: SimpleCache,
    database: MusicDatabase,
) : ViewModel() {
    val songs = database.allSongs()
        .let { all ->
            kotlinx.coroutines.flow.flow {
                all.collect { songs ->
                    emit(songs.filter { playerCache.isCached(it.id) })
                }
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
