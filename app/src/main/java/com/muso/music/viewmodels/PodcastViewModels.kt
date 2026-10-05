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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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
    private val database: MusicDatabase,
) : ViewModel() {
    // Round 193: the whole-file probe used to run PER SONG over the entire
    // library on every song-table emission - while a song plays those emissions
    // are constant, and each isCached call takes the cache's lock, so opening
    // this page during playback stalled. The cache keys are read ONCE per
    // emission and only the (few) songs with cached data get the exact check.
    val songs = database.allSongs()
        .map { all ->
            val cachedKeys = runCatching { playerCache.keys }.getOrNull().orEmpty()
            all.filter { it.id in cachedKeys && isFullyCached(it.id) }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /** Same idiom the upstream ArchiveTune player uses: probe with the recorded
     * content length, keeping the unbounded probe as a fallback. */
    private suspend fun isFullyCached(songId: String): Boolean {
        if (playerCache.isCached(songId, 0L, Long.MAX_VALUE)) return true
        val recordedLength =
            runCatching { database.format(songId).first()?.contentLength }.getOrNull() ?: -1L
        if (recordedLength <= 0L) return false
        return playerCache.isCached(songId, 0L, recordedLength) ||
            (playerCache.isCached(songId, 0L, 1L) && playerCache.isCached(songId, recordedLength - 1L, 1L))
    }
}
