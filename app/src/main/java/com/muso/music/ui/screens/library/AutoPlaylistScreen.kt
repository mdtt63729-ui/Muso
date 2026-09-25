package com.muso.music.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.muso.music.LocalPlayerAwareWindowInsets
import com.muso.music.LocalPlayerConnection
import com.muso.music.R
import com.muso.music.constants.CONTENT_TYPE_HEADER
import com.muso.music.constants.CONTENT_TYPE_SONG
import com.muso.music.extensions.toMediaItem
import com.muso.music.playback.queues.ListQueue
import com.muso.music.ui.component.EmptyPlaceholder
import com.muso.music.ui.component.LocalMenuState
import com.muso.music.ui.component.SongListItem
import com.muso.music.ui.menu.SongMenu
import kotlin.random.Random

/**
 * Auto playlist (Liked / Offline) opened from the library mix grid — a simple song list
 * with play-all / shuffle actions and the standard song context menu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AutoPlaylistScreen(
    navController: NavController,
    viewModel: AutoPlaylistViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val songs by viewModel.songs.collectAsState()
    val title = when (viewModel.playlist) {
        "liked" -> stringResource(R.string.liked)
        else -> stringResource(R.string.offline)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) {
        item(key = "header", contentType = CONTENT_TYPE_HEADER) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButtonLikeBack(navController)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        item(key = "buttons", contentType = CONTENT_TYPE_HEADER) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Button(
                    onClick = {
                        if (songs.isNotEmpty()) {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = title,
                                    items = songs.map { it.toMediaItem() },
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.play))
                }

                Button(
                    onClick = {
                        if (songs.isNotEmpty()) {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = title,
                                    items = songs.map { it.toMediaItem() },
                                    startIndex = Random.nextInt(songs.size),
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.shuffle),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.shuffle))
                }
            }
        }

        if (songs.isEmpty()) {
            item {
                EmptyPlaceholder(
                    icon = R.drawable.music_note,
                    text = stringResource(R.string.no_results_found),
                )
            }
        }

        items(
            items = songs,
            key = { it.id },
            contentType = { CONTENT_TYPE_SONG },
        ) { song ->
            SongListItem(
                song = song,
                isActive = song.id == mediaMetadata?.id,
                isPlaying = isPlaying,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = title,
                                    items = songs.map { it.toMediaItem() },
                                    startIndex = songs.indexOf(song),
                                )
                            )
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                SongMenu(
                                    originalSong = song,
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

@Composable
private fun IconButtonLikeBack(navController: NavController) {
    androidx.compose.material3.IconButton(onClick = { navController.popBackStack() }) {
        Icon(
            painter = painterResource(R.drawable.arrow_back),
            contentDescription = null,
        )
    }
}
