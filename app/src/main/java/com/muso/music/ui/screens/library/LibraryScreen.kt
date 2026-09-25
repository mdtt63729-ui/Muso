package com.muso.music.ui.screens.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.muso.music.R
import com.muso.music.constants.ChipSortTypeKey
import com.muso.music.constants.LibraryFilter
import com.muso.music.ui.component.ChipsRow
import com.muso.music.utils.rememberEnumPreference

/**
 * ReTune-style library: a filter chips row (Playlists / Songs / Albums / Artists) switches
 * between the individual library screens, and the default view mixes everything in one
 * grid. Tapping the active chip again returns to the mixed library view.
 */
@Composable
fun LibraryScreen(
    navController: NavController,
) {
    var filterType by rememberEnumPreference(ChipSortTypeKey, LibraryFilter.LIBRARY)

    val filterContent = @Composable {
        Row(modifier = Modifier.fillMaxWidth()) {
            ChipsRow(
                chips = listOf(
                    LibraryFilter.PLAYLISTS to stringResource(R.string.filter_playlists),
                    LibraryFilter.SONGS to stringResource(R.string.filter_songs),
                    LibraryFilter.ALBUMS to stringResource(R.string.filter_albums),
                    LibraryFilter.ARTISTS to stringResource(R.string.filter_artists),
                    LibraryFilter.PODCASTS to stringResource(R.string.filter_podcasts),
                ),
                currentValue = filterType,
                onValueUpdate = {
                    filterType = if (filterType == it) LibraryFilter.LIBRARY else it
                },
                modifier = Modifier.weight(1f),
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (filterType) {
            LibraryFilter.LIBRARY -> LibraryMixScreen(navController, filterContent)
            LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(navController, topFilterContent = filterContent)
            LibraryFilter.SONGS -> LibrarySongsScreen(navController, topFilterContent = filterContent)
            LibraryFilter.ALBUMS -> LibraryAlbumsScreen(navController, topFilterContent = filterContent)
            LibraryFilter.ARTISTS -> LibraryArtistsScreen(navController, topFilterContent = filterContent)
            LibraryFilter.PODCASTS -> PodcastsScreen(navController, topFilterContent = filterContent)
        }
    }
}
