package com.muso.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.models.PlaylistItem
import com.zionhuang.innertube.models.WatchEndpoint
import com.zionhuang.innertube.models.YTItem
import com.zionhuang.innertube.models.filterExplicit
import com.zionhuang.innertube.pages.ExplorePage
import com.zionhuang.innertube.pages.HomePage
import com.muso.music.constants.HideExplicitKey
import com.muso.music.db.MusicDatabase
import com.muso.music.db.entities.Album
import com.muso.music.db.entities.Artist
import com.muso.music.db.entities.LocalItem
import com.muso.music.db.entities.Song
import com.muso.music.models.SimilarRecommendation
import com.muso.music.utils.dataStore
import com.muso.music.utils.get
import com.muso.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
) : ViewModel() {
    val isRefreshing = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)

    val quickPicks = MutableStateFlow<List<Song>?>(null)
    val forgottenFavorites = MutableStateFlow<List<Song>?>(null)
    val keepListening = MutableStateFlow<List<LocalItem>?>(null)
    val similarRecommendations = MutableStateFlow<List<SimilarRecommendation>?>(null)
    val accountPlaylists = MutableStateFlow<List<PlaylistItem>?>(null)
    val homePage = MutableStateFlow<HomePage?>(null)
    val explorePage = MutableStateFlow<ExplorePage?>(null)

    val allLocalItems = MutableStateFlow<List<LocalItem>>(emptyList())
    val allYtItems = MutableStateFlow<List<YTItem>>(emptyList())

    private suspend fun load() {
        isLoading.value = true

        val hideExplicit = context.dataStore.get(HideExplicitKey, false)

        quickPicks.value = database.quickPicks()
            .first().shuffled().take(20)

        forgottenFavorites.value = database.forgottenFavorites()
            .first().shuffled().take(20)

        val fromTimeStamp = System.currentTimeMillis() - 86400000 * 7 * 2
        val keepListeningSongs = database.mostPlayedSongs(fromTimeStamp, limit = 15, offset = 5)
            .first().shuffled().take(10)
        val keepListeningAlbums = database.mostPlayedAlbums(fromTimeStamp, limit = 8, offset = 2)
            .first().filter { it.album.thumbnailUrl != null }.shuffled().take(5)
        val keepListeningArtists = database.mostPlayedArtists(fromTimeStamp)
            .first().filter { it.artist.isYouTubeArtist && it.artist.thumbnailUrl != null }.shuffled().take(5)
        keepListening.value = (keepListeningSongs + keepListeningAlbums + keepListeningArtists).shuffled()

        allLocalItems.value = (quickPicks.value.orEmpty() + forgottenFavorites.value.orEmpty() + keepListening.value.orEmpty())
            .filter { it is Song || it is Album }

        if (YouTube.cookie != null) { // if logged in
            YouTube.likedPlaylists().onSuccess {
                accountPlaylists.value = it
            }.onFailure {
                reportException(it)
            }
        }

        // Similar to artists
        val artistRecommendations =
            database.mostPlayedArtists(fromTimeStamp, limit = 10).first()
                .filter { it.artist.isYouTubeArtist }
                .shuffled().take(3)
                .mapNotNull {
                    val items = mutableListOf<YTItem>()
                    YouTube.artist(it.id).onSuccess { page ->
                        items += page.sections.getOrNull(page.sections.size - 2)?.items.orEmpty()
                        items += page.sections.lastOrNull()?.items.orEmpty()
                    }
                    SimilarRecommendation(
                        title = it,
                        items = items
                            .filterExplicit(hideExplicit)
                            .shuffled()
                            .ifEmpty { return@mapNotNull null }
                    )
                }
        // Similar to songs
        val songRecommendations =
            database.mostPlayedSongs(fromTimeStamp, limit = 10).first()
                .filter { it.album != null }
                .shuffled().take(2)
                .mapNotNull { song ->
                    val endpoint = YouTube.next(WatchEndpoint(videoId = song.id)).getOrNull()?.relatedEndpoint ?: return@mapNotNull null
                    val page = YouTube.related(endpoint).getOrNull() ?: return@mapNotNull null
                    SimilarRecommendation(
                        title = song,
                        items = (page.songs.shuffled().take(8) +
                                page.albums.shuffled().take(4) +
                                page.artists.shuffled().take(4) +
                                page.playlists.shuffled().take(4))
                            .filterExplicit(hideExplicit)
                            .shuffled()
                            .ifEmpty { return@mapNotNull null }
                    )
                }
        similarRecommendations.value = (artistRecommendations + songRecommendations).shuffled()

        retrying(isEmpty = { it.sections.isEmpty() }) { YouTube.home() }.onSuccess { page ->
            homePage.value = page.filterExplicit(hideExplicit)
        }.onFailure {
            reportException(it)
        }

        retrying { YouTube.explore() }.onSuccess { page ->
            val artists: Set<String>
            val favouriteArtists: Set<String>
            database.artistsByCreateDateAsc().first().let { list ->
                artists = list.map(Artist::id).toHashSet()
                favouriteArtists = list
                    .filter { it.artist.bookmarkedAt != null }
                    .map { it.id }
                    .toHashSet()
            }
            explorePage.value = page.copy(
                newReleaseAlbums = page.newReleaseAlbums
                    .sortedBy { album ->
                        if (album.artists.orEmpty().any { it.id in favouriteArtists }) 0
                        else if (album.artists.orEmpty().any { it.id in artists }) 1
                        else 2
                    }
                    .filterExplicit(hideExplicit)
            )
        }.onFailure {
            reportException(it)
        }

        allYtItems.value = similarRecommendations.value?.flatMap { it.items }.orEmpty() +
                homePage.value?.sections?.flatMap { it.items }.orEmpty() +
                explorePage.value?.newReleaseAlbums.orEmpty()

        isLoading.value = false

        // Remember everything for the rest of this process: re-entering the app (minimize,
        // back out and reopen) shows content instantly instead of reloading, and only a
        // full app close (process death) starts fresh.
        cache = HomeCache(
            quickPicks.value,
            forgottenFavorites.value,
            keepListening.value,
            similarRecommendations.value,
            accountPlaylists.value,
            homePage.value,
            explorePage.value,
            allLocalItems.value,
            allYtItems.value,
        )
    }

    /**
     * Transient InnerTube failures (cold connection, rate limit) used to leave the home feed
     * empty until a manual refresh; retry a few times before giving up.
     */
    private suspend fun <T> retrying(
        isEmpty: (T) -> Boolean = { false },
        block: suspend () -> Result<T>,
    ): Result<T> {
        var result = block()
        var attempt = 0
        while (attempt < 3 && (result.isFailure || (result.isSuccess && isEmpty(result.getOrThrow())))) {
            delay(1000L * (attempt + 1))
            attempt++
            result = block()
        }
        return result
    }

    fun refresh() {
        if (isRefreshing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing.value = true
            load()
            isRefreshing.value = false
        }
    }

    init {
        val cached = cache
        if (cached != null) {
            quickPicks.value = cached.quickPicks
            forgottenFavorites.value = cached.forgottenFavorites
            keepListening.value = cached.keepListening
            similarRecommendations.value = cached.similarRecommendations
            accountPlaylists.value = cached.accountPlaylists
            homePage.value = cached.homePage
            explorePage.value = cached.explorePage
            allLocalItems.value = cached.allLocalItems
            allYtItems.value = cached.allYtItems
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                load()
            }
        }
    }

    companion object {
        @Volatile
        private var cache: HomeCache? = null
    }

    private data class HomeCache(
        val quickPicks: List<Song>?,
        val forgottenFavorites: List<Song>?,
        val keepListening: List<LocalItem>?,
        val similarRecommendations: List<SimilarRecommendation>?,
        val accountPlaylists: List<PlaylistItem>?,
        val homePage: HomePage?,
        val explorePage: ExplorePage?,
        val allLocalItems: List<LocalItem>,
        val allYtItems: List<YTItem>,
    )
}
