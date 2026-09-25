package com.zionhuang.music.ui.screens.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.zionhuang.music.R

private class LibraryTab(
    val titleRes: Int,
    val content: @Composable (NavController) -> Unit,
)

/**
 * Combined Library screen (SimpMusic-style navigation): hosts the Songs / Artists / Albums /
 * Playlists screens as tabs under one bottom-bar entry. The tab selection survives navigation.
 */
private val libraryTabs = listOf(
    LibraryTab(R.string.songs) { navController -> LibrarySongsScreen(navController) },
    LibraryTab(R.string.artists) { navController -> LibraryArtistsScreen(navController) },
    LibraryTab(R.string.albums) { navController -> LibraryAlbumsScreen(navController) },
    LibraryTab(R.string.playlists) { navController -> LibraryPlaylistsScreen(navController) },
)

@Composable
fun LibraryScreen(
    navController: NavController,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            libraryTabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(text = stringResource(tab.titleRes)) }
                )
            }
        }
        libraryTabs[selectedTab].content(navController)
    }
}
