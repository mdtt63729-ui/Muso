package com.muso.music.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.muso.music.LocalPlayerAwareWindowInsets
import com.muso.music.LocalPlayerConnection
import com.muso.music.R
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.runtime.remember

import com.muso.music.extensions.toMediaItem
import com.muso.music.constants.CONTENT_TYPE_ALBUM
import com.muso.music.constants.CONTENT_TYPE_ARTIST
import com.muso.music.constants.CONTENT_TYPE_HEADER
import com.muso.music.constants.CONTENT_TYPE_PLAYLIST
import com.muso.music.constants.CONTENT_TYPE_SONG
import com.muso.music.constants.GridThumbnailHeight
import com.muso.music.constants.LibraryFilter
import com.muso.music.constants.LibraryViewType
import com.muso.music.constants.MixSortDescendingKey
import com.muso.music.constants.MixSortType
import com.muso.music.constants.MixSortTypeKey
import com.muso.music.constants.ShowCachedPlaylistKey
import com.muso.music.constants.ShowUploadedPlaylistKey
import com.muso.music.constants.ShowDownloadedPlaylistKey
import com.muso.music.constants.ShowLikedPlaylistKey
import com.muso.music.constants.MixViewTypeKey
import com.muso.music.db.entities.Album
import com.muso.music.db.entities.Artist
import com.muso.music.db.entities.Playlist
import com.muso.music.db.entities.PlaylistEntity
import com.muso.music.db.entities.Song
import com.muso.music.ui.component.AlbumGridItem
import com.muso.music.ui.component.AlbumListItem
import com.muso.music.ui.component.ArtistGridItem
import com.muso.music.ui.component.ArtistListItem
import com.muso.music.ui.component.EmptyPlaceholder
import com.muso.music.ui.component.LibrarySearchHeader
import com.muso.music.ui.component.LocalMenuState
import com.muso.music.ui.component.PlaylistGridItem
import com.muso.music.ui.component.PlaylistListItem
import com.muso.music.ui.component.SongGridItem
import com.muso.music.ui.component.SongListItem
import com.muso.music.ui.component.SortHeader
import com.muso.music.ui.menu.AlbumMenu
import com.muso.music.ui.menu.ArtistMenu
import com.muso.music.ui.menu.PlaylistMenu
import com.muso.music.ui.menu.SongMenu
import com.muso.music.utils.rememberEnumPreference
import com.muso.music.viewmodels.LibraryMixViewModel
import com.muso.music.utils.rememberPreference
import java.time.LocalDateTime

/**
 * ReTune-style mixed library: everything the user has saved (playlists, albums, artists,
 * plus songs while searching) in one grid or list, with the filter chips row on top, an
 * in-library search, sort header and a grid/list toggle. The Liked and Offline auto
 * playlists sit at the top of the grid.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryMixScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    viewModel: LibraryMixViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val (sortType, onSortTypeChange) = rememberEnumPreference(MixSortTypeKey, MixSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(MixSortDescendingKey, true)
    var viewType by rememberEnumPreference(MixViewTypeKey, LibraryViewType.GRID)

    // Plain remember (not rememberSaveable): restoring the library tab must never come
    // back in search mode, so the focus race cannot happen on tab restore at all.
    var isSearchActive by remember { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val debouncedSearchQuery by viewModel.debouncedSearchQuery.collectAsState()

    val songs by viewModel.songs.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    val query = if (isSearchActive) searchQuery else debouncedSearchQuery
    val searching = query.isNotBlank()
    val matches: (Array<out String?>) -> Boolean = { values ->
        !searching || values.any { it?.contains(query, ignoreCase = true) == true }
    }

    // Auto playlists (Echo appearance settings): each one can be hidden from the grid.
    val showLiked by rememberPreference(ShowLikedPlaylistKey, true)
    val showDownloaded by rememberPreference(ShowDownloadedPlaylistKey, true)
    val showUploaded by rememberPreference(ShowUploadedPlaylistKey, true)
    val showCached by rememberPreference(ShowCachedPlaylistKey, true)
    val autoPlaylists = listOfNotNull(
        if (showLiked) Playlist(PlaylistEntity(id = PlaylistEntity.LIKED_PLAYLIST_ID, name = stringResource(R.string.liked)), songCount = 0, thumbnails = emptyList()) else null,
        if (showDownloaded) Playlist(PlaylistEntity(id = PlaylistEntity.DOWNLOADED_PLAYLIST_ID, name = stringResource(R.string.offline)), songCount = 0, thumbnails = emptyList()) else null,
        if (showUploaded) Playlist(PlaylistEntity(id = "LP_UPLOADED", name = stringResource(R.string.uploaded)), songCount = 0, thumbnails = emptyList()) else null,
        if (showCached) Playlist(PlaylistEntity(id = "LP_CACHED", name = stringResource(R.string.cached)), songCount = 0, thumbnails = emptyList()) else null,
    )
    val visibleAutoPlaylists = autoPlaylists.filter { matches(arrayOf(it.playlist.name)) }

    val sortedItems: List<Any> = when (sortType) {
        MixSortType.CREATE_DATE -> (playlists + albums + artists).sortedBy { item ->
            when (item) {
                is Album -> item.album.lastUpdateTime
                else -> LocalDateTime.MIN
            }
        }

        MixSortType.NAME -> (playlists + albums + artists).sortedBy { item ->
            when (item) {
                is Playlist -> item.playlist.name
                is Album -> item.album.title
                is Artist -> item.artist.name
                else -> ""
            }
        }
    }.let { if (sortDescending) it.asReversed() else it }

    val filteredItems = (if (searching) sortedItems + songs else sortedItems).filter { item ->
        when (item) {
            is Song -> matches(arrayOf(item.song.title, item.song.albumName, *item.artists.map { it.name }.toTypedArray()))
            is Album -> matches(arrayOf(item.album.title, *item.artists.map { it.name }.toTypedArray()))
            is Artist -> matches(arrayOf(item.artist.name))
            is Playlist -> matches(arrayOf(item.playlist.name))
            else -> true
        }
    }

    val header = @Composable {
        Column {
            filterContent()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            ) {
                LibrarySearchHeader(
                    isSearchActive = isSearchActive,
                    searchQuery = searchQuery,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onBack = {
                        isSearchActive = false
                        viewModel.updateSearchQuery("")
                    },
                    keyboardController = keyboardController,
                    modifier = Modifier.weight(1f),
                    inactiveContent = {
                        Text(
                            text = stringResource(R.string.library),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(painterResource(R.drawable.search), contentDescription = null)
                        }
                        IconButton(onClick = {
                            viewType = if (viewType == LibraryViewType.GRID) LibraryViewType.LIST else LibraryViewType.GRID
                        }) {
                            Icon(
                                painterResource(
                                    if (viewType == LibraryViewType.GRID) R.drawable.list else R.drawable.grid_view
                                ),
                                contentDescription = null
                            )
                        }
                    },
                )
            }
            SortHeader(
                sortType = sortType,
                sortDescending = sortDescending,
                onSortTypeChange = onSortTypeChange,
                onSortDescendingChange = onSortDescendingChange,
                sortTypeText = {
                    when (it) {
                        MixSortType.CREATE_DATE -> R.string.mix_sort_date
                        MixSortType.NAME -> R.string.mix_sort_name
                    }
                }
            )
        }
    }

    val isEmpty = filteredItems.isEmpty() && visibleAutoPlaylists.isEmpty()

    if (viewType == LibraryViewType.GRID) {
        val lazyGridState = rememberLazyGridState()
        LazyVerticalGrid(
            state = lazyGridState,
            columns = GridCells.Adaptive(GridThumbnailHeight + 24.dp),
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            item(
                key = "header",
                span = { GridItemSpan(maxLineSpan) },
                contentType = CONTENT_TYPE_HEADER,
            ) {
                header()
            }

            gridItems(
                items = visibleAutoPlaylists,
                key = { "auto_" + it.id },
                contentType = { CONTENT_TYPE_PLAYLIST },
            ) { playlist ->
                PlaylistGridItem(
                    playlist = playlist,
                    fillMaxWidth = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                navController.navigate(
                                    when (playlist.id) {
                                        PlaylistEntity.LIKED_PLAYLIST_ID -> "auto_playlist/liked"
                                        "LP_UPLOADED" -> "uploaded"
                                        "LP_CACHED" -> "cached"
                                        else -> "auto_playlist/offline"
                                    }
                                )
                            },
                            onLongClick = {},
                        ),
                )
            }

            if (isEmpty) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyPlaceholder(
                        icon = R.drawable.library_music,
                        text = stringResource(R.string.library_mix_empty),
                    )
                }
            }

            gridItems(
                items = filteredItems,
                key = { item ->
                    when (item) {
                        is Playlist -> "playlist_${item.id}"
                        is Album -> "album_${item.id}"
                        is Artist -> "artist_${item.id}"
                        is Song -> "song_${item.id}"
                        else -> item.hashCode().toString()
                    }
                },
                contentType = { item ->
                    when (item) {
                        is Playlist -> CONTENT_TYPE_PLAYLIST
                        is Album -> CONTENT_TYPE_ALBUM
                        is Artist -> CONTENT_TYPE_ARTIST
                        is Song -> CONTENT_TYPE_SONG
                        else -> CONTENT_TYPE_HEADER
                    }
                },
            ) { item ->
                when (item) {
                    is Playlist -> PlaylistGridItem(
                        playlist = item,
                        fillMaxWidth = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { navController.navigate("local_playlist/${item.id}") },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        PlaylistMenu(
                                            playlist = item,
                                            coroutineScope = coroutineScope,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                            ),
                    )

                    is Album -> AlbumGridItem(
                        album = item,
                        isActive = item.id == mediaMetadata?.album?.id,
                        isPlaying = isPlaying,
                        coroutineScope = coroutineScope,
                        fillMaxWidth = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { navController.navigate("album/${item.id}") },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        AlbumMenu(
                                            originalAlbum = item,
                                            navController = navController,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                            ),
                    )

                    is Artist -> ArtistGridItem(
                        artist = item,
                        fillMaxWidth = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { navController.navigate("artist/${item.id}") },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        ArtistMenu(
                                            originalArtist = item,
                                            coroutineScope = coroutineScope,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                            ),
                    )

                    is Song -> SongGridItem(
                        song = item,
                        isActive = item.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    val index = songs.indexOfFirst { it.id == item.id }
                                    if (index != -1) {
                                        playerConnection.playQueue(
                                            com.muso.music.playback.queues.ListQueue(
                                                title = item.song.title,
                                                items = songs.map { it.toMediaItem() },
                                                startIndex = index,
                                            )
                                        )
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        SongMenu(
                                            originalSong = item,
                                            navController = navController,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                            ),
                    )
                }
            }
        }
    } else {
        val lazyListState = rememberLazyListState()
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            item(key = "header", contentType = CONTENT_TYPE_HEADER) {
                header()
            }

            items(
                items = visibleAutoPlaylists,
                key = { "auto_" + it.id },
                contentType = { CONTENT_TYPE_PLAYLIST },
            ) { playlist ->
                PlaylistListItem(
                    playlist = playlist,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                navController.navigate(
                                    when (playlist.id) {
                                        PlaylistEntity.LIKED_PLAYLIST_ID -> "auto_playlist/liked"
                                        "LP_UPLOADED" -> "uploaded"
                                        "LP_CACHED" -> "cached"
                                        else -> "auto_playlist/offline"
                                    }
                                )
                            },
                            onLongClick = {},
                        ),
                )
            }

            if (isEmpty) {
                item {
                    EmptyPlaceholder(
                        icon = R.drawable.library_music,
                        text = stringResource(R.string.library_mix_empty),
                    )
                }
            }

            items(
                items = filteredItems,
                key = { item ->
                    when (item) {
                        is Playlist -> "playlist_${item.id}"
                        is Album -> "album_${item.id}"
                        is Artist -> "artist_${item.id}"
                        is Song -> "song_${item.id}"
                        else -> item.hashCode().toString()
                    }
                },
                contentType = { item ->
                    when (item) {
                        is Playlist -> CONTENT_TYPE_PLAYLIST
                        is Album -> CONTENT_TYPE_ALBUM
                        is Artist -> CONTENT_TYPE_ARTIST
                        is Song -> CONTENT_TYPE_SONG
                        else -> CONTENT_TYPE_HEADER
                    }
                },
            ) { item ->
                when (item) {
                    is Playlist -> PlaylistListItem(
                        playlist = item,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { navController.navigate("local_playlist/${item.id}") },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        PlaylistMenu(
                                            playlist = item,
                                            coroutineScope = coroutineScope,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                            ),
                    )

                    is Album -> AlbumListItem(
                        album = item,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { navController.navigate("album/${item.id}") },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        AlbumMenu(
                                            originalAlbum = item,
                                            navController = navController,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                            ),
                    )

                    is Artist -> ArtistListItem(
                        artist = item,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { navController.navigate("artist/${item.id}") },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        ArtistMenu(
                                            originalArtist = item,
                                            coroutineScope = coroutineScope,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                            ),
                    )

                    is Song -> SongListItem(
                        song = item,
                        isActive = item.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    val index = songs.indexOfFirst { it.id == item.id }
                                    if (index != -1) {
                                        playerConnection.playQueue(
                                            com.muso.music.playback.queues.ListQueue(
                                                title = item.song.title,
                                                items = songs.map { it.toMediaItem() },
                                                startIndex = index,
                                            )
                                        )
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        SongMenu(
                                            originalSong = item,
                                            navController = navController,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                            ),
                    )
                }
            }
        }
    }
}
