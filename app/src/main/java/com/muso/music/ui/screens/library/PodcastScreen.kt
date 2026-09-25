package com.muso.music.ui.screens.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.zionhuang.innertube.models.SongItem
import com.muso.music.LocalPlayerAwareWindowInsets
import com.muso.music.LocalPlayerConnection
import com.muso.music.R
import com.muso.music.constants.CONTENT_TYPE_HEADER
import com.muso.music.models.toMediaMetadata
import com.muso.music.playback.queues.YouTubeQueue
import com.muso.music.ui.component.EmptyPlaceholder
import com.muso.music.utils.makeTimeString
import com.muso.music.viewmodels.PodcastViewModel
import com.muso.music.viewmodels.UploadedViewModel

/**
 * Podcast episode list (ReTune port, online): episodes of a subscribed show; tapping one
 * plays it through the online queue path.
 */
@Composable
fun PodcastScreen(
    navController: NavController,
    viewModel: PodcastViewModel = hiltViewModel(),
) {
    val episodes by viewModel.episodes.collectAsState()
    OnlineSongsScreen(
        navController = navController,
        title = stringResource(R.string.filter_podcasts),
        result = episodes,
    )
}

/** Uploaded (privately owned) songs from the user's YouTube Music library (online). */
@Composable
fun UploadedScreen(
    navController: NavController,
    viewModel: UploadedViewModel = hiltViewModel(),
) {
    val songs by viewModel.songs.collectAsState()
    OnlineSongsScreen(
        navController = navController,
        title = stringResource(R.string.uploaded),
        result = songs,
    )
}

@Composable
private fun OnlineSongsScreen(
    navController: NavController,
    title: String,
    result: Result<List<SongItem>>?,
) {
    val playerConnection = LocalPlayerConnection.current ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) {
        item(key = "header", contentType = CONTENT_TYPE_HEADER) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        when {
            result == null -> item(key = "loading") {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                ) {
                    CircularProgressIndicator()
                }
            }

            result.getOrNull().isNullOrEmpty() -> item(key = "empty") {
                EmptyPlaceholder(
                    icon = R.drawable.music_note,
                    text = if (result.isFailure) {
                        stringResource(R.string.login_required)
                    } else {
                        stringResource(R.string.no_results_found)
                    },
                )
            }

            else -> items(
                items = result.getOrThrow(),
                key = { it.id },
            ) { item ->
                OnlineSongRow(item) {
                    playerConnection.playQueue(YouTubeQueue.radio(item.toMediaMetadata()))
                }
            }
        }
    }
}

@Composable
private fun OnlineSongRow(
    item: SongItem,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        AsyncImage(
            model = item.thumbnail,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
            Row {
                Text(
                    text = item.artists.joinToString { it.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false),
                )
                item.duration?.let {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = makeTimeString(it * 1000L),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}
