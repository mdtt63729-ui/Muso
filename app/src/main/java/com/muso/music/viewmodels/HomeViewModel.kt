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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
        val fromTimeStamp = System.currentTimeMillis() - 86400000 * 7 * 2

        // Cold-start path: fetch the independent local sections concurrently, then publish them
        // as ONE state update. The previous sequential implementation caused several back-to-back
        // Compose passes during the splash -> Home handoff (quick picks -> favourites -> history),
        // which was visible as a hitch even though Room itself was running on Dispatchers.IO.
        val (quickPicks, forgottenFavorites, keepListening) = coroutineScope {
            val quick = async { database.quickPicks().first().shuffled().take(20) }
            val forgotten = async { database.forgottenFavorites().first().shuffled().take(20) }
            val songs = async {
                database.mostPlayedSongs(fromTimeStamp, limit = 15, offset = 5)
                    .first().shuffled().take(10)
            }
            val albums = async {
                database.mostPlayedAlbums(fromTimeStamp, limit = 8, offset = 2)
                    .first().filter { it.album.thumbnailUrl != null }.shuffled().take(5)
            }
            val artists = async {
                database.mostPlayedArtists(fromTimeStamp).first()
                    .filter { it.artist.isYouTubeArtist && it.artist.thumbnailUrl != null }
                    .shuffled().take(5)
            }
            Triple(quick.await(), forgotten.await(), (songs.await() + albums.await() + artists.await()).shuffled())
        }

        allLocalItems.value = (quickPicks + forgottenFavorites + keepListening)
            .filter { it is Song || it is Album }
        this.quickPicks.value = quickPicks
        this.forgottenFavorites.value = forgottenFavorites
        this.keepListening.value = keepListening

        // Give the first Home frame a chance to render before starting the heavier network fan-out.
        // This is deliberately a yield, not an arbitrary delay: it costs no visible startup time
        // when the UI is already idle, but prevents the first remote burst from landing in the
        // same frame as the Home composition.
        kotlinx.coroutines.yield()

        // Remote sections are independent too. Run them concurrently and publish the completed
        // result set together, rather than invalidating Home once per endpoint as each call returns.
        val remote = coroutineScope {
            val account = async {
                if (YouTube.cookie != null) {
                    YouTube.likedPlaylists().getOrNull()
                } else null
            }
            val artistRecommendations = async {
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
                            items = items.filterExplicit(hideExplicit).shuffled()
                                .ifEmpty { return@mapNotNull null },
                        )
                    }
            }
            val songRecommendations = async {
                database.mostPlayedSongs(fromTimeStamp, limit = 10).first()
                    .filter { it.album != null }
                    .shuffled().take(2)
                    .mapNotNull { song ->
                        val endpoint = YouTube.next(WatchEndpoint(videoId = song.id)).getOrNull()?.relatedEndpoint
                            ?: return@mapNotNull null
                        val page = YouTube.related(endpoint).getOrNull() ?: return@mapNotNull null
                        SimilarRecommendation(
                            title = song,
                            items = (page.songs.shuffled().take(8) +
                                    page.albums.shuffled().take(4) +
                                    page.artists.shuffled().take(4) +
                                    page.playlists.shuffled().take(4))
                                .filterExplicit(hideExplicit).shuffled()
                                .ifEmpty { return@mapNotNull null },
                        )
                    }
            }
            val home = async {
                retrying(isEmpty = { it.sections.isEmpty() }) { YouTube.home() }
                    .getOrNull()?.filterExplicit(hideExplicit)
            }
            val explore = async {
                retrying { YouTube.explore() }.getOrNull()?.let { page ->
                    val artists = database.artistsByCreateDateAsc().first()
                    val artistIds = artists.map(Artist::id).toHashSet()
                    val favouriteArtistIds = artists.filter { it.artist.bookmarkedAt != null }
                        .map(Artist::id).toHashSet()
                    page.copy(
                        newReleaseAlbums = page.newReleaseAlbums.sortedBy { album ->
                            when {
                                album.artists.orEmpty().any { it.id in favouriteArtistIds } -> 0
                                album.artists.orEmpty().any { it.id in artistIds } -> 1
                                else -> 2
                            }
                        }.filterExplicit(hideExplicit),
                    )
                }
            }
            val accountValue = account.await()
            val similarValue = (artistRecommendations.await() + songRecommendations.await()).shuffled()
            RemoteHomeData(
                accountPlaylists = accountValue,
                similarRecommendations = similarValue,
                homePage = home.await(),
                explorePage = explore.await(),
            )
        }

        this.accountPlaylists.value = remote.accountPlaylists
        this.similarRecommendations.value = remote.similarRecommendations
        this.homePage.value = remote.homePage
        this.explorePage.value = remote.explorePage
        allYtItems.value = remote.similarRecommendations.flatMap { it.items } +
                remote.homePage?.sections?.flatMap { it.items }.orEmpty() +
                remote.explorePage?.newReleaseAlbums.orEmpty()
        isLoading.value = false

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

    private data class RemoteHomeData(
        val accountPlaylists: List<PlaylistItem>?,
        val similarRecommendations: List<SimilarRecommendation>,
        val homePage: HomePage?,
        val explorePage: ExplorePage?,
    )

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
