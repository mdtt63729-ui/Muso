package com.muso.music.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.muso.music.R
import com.muso.music.constants.LibraryFilter

/**
 * LIBRARY TAB (reference image 581028): the icon pill row (Playlists / Songs /
 * Albums / Artists / Podcasts) on top, the redesigned library home under it.
 * A fresh open of the tab ALWAYS lands on the home - the pill state is not
 * persisted - so the featured Liked card, the category grid and Recently
 * Played are the first thing seen. Tapping the active pill again returns to
 * the home from any sub list.
 */
@Composable
fun LibraryScreen(
    navController: NavController,
) {
    var filterType by remember { mutableStateOf(LibraryFilter.LIBRARY) }

    val filterContent = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
            Spacer(Modifier.width(16.dp))
            LibraryPill(
                iconRes = R.drawable.queue_music,
                label = stringResource(R.string.filter_playlists),
                selected = filterType == LibraryFilter.PLAYLISTS,
                onClick = { filterType = if (filterType == LibraryFilter.PLAYLISTS) LibraryFilter.LIBRARY else LibraryFilter.PLAYLISTS },
            )
            Spacer(Modifier.width(8.dp))
            LibraryPill(
                iconRes = R.drawable.music_note,
                label = stringResource(R.string.filter_songs),
                selected = filterType == LibraryFilter.SONGS,
                onClick = { filterType = if (filterType == LibraryFilter.SONGS) LibraryFilter.LIBRARY else LibraryFilter.SONGS },
            )
            Spacer(Modifier.width(8.dp))
            LibraryPill(
                iconRes = R.drawable.album,
                label = stringResource(R.string.filter_albums),
                selected = filterType == LibraryFilter.ALBUMS,
                onClick = { filterType = if (filterType == LibraryFilter.ALBUMS) LibraryFilter.LIBRARY else LibraryFilter.ALBUMS },
            )
            Spacer(Modifier.width(8.dp))
            LibraryPill(
                iconRes = R.drawable.person,
                label = stringResource(R.string.filter_artists),
                selected = filterType == LibraryFilter.ARTISTS,
                onClick = { filterType = if (filterType == LibraryFilter.ARTISTS) LibraryFilter.LIBRARY else LibraryFilter.ARTISTS },
            )
            Spacer(Modifier.width(8.dp))
            LibraryPill(
                iconRes = R.drawable.radio,
                label = stringResource(R.string.filter_podcasts),
                selected = filterType == LibraryFilter.PODCASTS,
                onClick = { filterType = if (filterType == LibraryFilter.PODCASTS) LibraryFilter.LIBRARY else LibraryFilter.PODCASTS },
            )
            Spacer(Modifier.width(16.dp))
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

/** One pill from the reference image: rounded-full, translucent fill, thin
 *  border, icon + label; the selected pill takes the primary tint. */
@Composable
private fun LibraryPill(
    iconRes: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val container =
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f)
    val borderColor =
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.outlineVariant
    val contentColor =
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(50))
            .background(container)
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor,
        )
    }
}
