package com.muso.music.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.muso.music.LocalPlayerAwareWindowInsets
import com.muso.music.LocalPlayerConnection
import com.muso.music.R
import com.muso.music.constants.CONTENT_TYPE_HEADER
import com.muso.music.constants.CONTENT_TYPE_SONG
import com.muso.music.constants.MixSortDescendingKey
import com.muso.music.constants.MixSortType
import com.muso.music.constants.MixSortTypeKey
import com.muso.music.extensions.toMediaItem
import com.muso.music.playback.queues.ListQueue
import com.muso.music.ui.component.LocalMenuState
import com.muso.music.ui.component.SongListItem
import com.muso.music.ui.menu.SongMenu
import com.muso.music.utils.rememberPreference
import com.muso.music.utils.rememberEnumPreference

/**
 * LIBRARY HOME (Material 3 redesign, reference image 581028):
 * filter chips on top (no search bar), a wide rounded sort bar, a full-width
 * featured "Liked / MOST PLAYED" card, a row of big category cards
 * (Downloaded / My top 50 / History), and - scrolling down - the
 * "Recently Played" list: every song the user has played, each one fully
 * cached in the background so it keeps playing offline. Tapping a song
 * plays it from that list (the list itself becomes the queue).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryMixScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    viewModel: com.muso.music.viewmodels.LibraryMixViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val (sortType, onSortTypeChange) = rememberEnumPreference(MixSortTypeKey, MixSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(MixSortDescendingKey, true)

    val likedSongs by viewModel.likedSongs.collectAsState()
    val recentSongs by viewModel.recentSongs.collectAsState()

    val recentlyPlayedTitle = stringResource(R.string.recently_played)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) {
        item(key = "chips", contentType = CONTENT_TYPE_HEADER) {
            filterContent()
        }

        // --- Wide rounded sort bar (reference image) ---
        item(key = "sortbar", contentType = CONTENT_TYPE_HEADER) {
            var sortMenuExpanded by remember { mutableStateOf(false) }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp, end = 4.dp),
                ) {
                    Box {
                        TextButton(onClick = { sortMenuExpanded = true }) {
                            Text(
                                text = stringResource(
                                    when (sortType) {
                                        MixSortType.CREATE_DATE -> R.string.date_added
                                        MixSortType.NAME -> R.string.mix_sort_name
                                    }
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.date_added)) },
                                onClick = {
                                    onSortTypeChange(MixSortType.CREATE_DATE)
                                    sortMenuExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.mix_sort_name)) },
                                onClick = {
                                    onSortTypeChange(MixSortType.NAME)
                                    sortMenuExpanded = false
                                },
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { onSortDescendingChange(!sortDescending) }) {
                        Icon(
                            painterResource(
                                if (sortDescending) R.drawable.arrow_downward else R.drawable.arrow_upward
                            ),
                            contentDescription = null,
                        )
                    }
                }
            }
        }

        // --- Featured "Liked" card ---
        item(key = "liked_card", contentType = CONTENT_TYPE_HEADER) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                            )
                        )
                    )
                    .combinedClickable(
                        onClick = { navController.navigate("auto_playlist/liked") },
                        onLongClick = {},
                    ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.favorite),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(38.dp),
                        )
                    }
                    Spacer(Modifier.size(22.dp))
                    Column {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ) {
                            Text(
                                text = stringResource(R.string.most_played).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                        Text(
                            text = stringResource(R.string.liked),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = pluralStringResource(R.plurals.n_song, likedSongs.size, likedSongs.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // --- Category cards, 2x2 like the reference image (no separate Cached
        // card - the cache lives on as Recently Played below) ---
        item(key = "category_cards", contentType = CONTENT_TYPE_HEADER) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    LibraryHomeCard(
                        iconRes = R.drawable.simp_baseline_downloaded,
                        label = stringResource(R.string.downloaded),
                        modifier = Modifier.weight(1f),
                    ) { navController.navigate("auto_playlist/offline") }
                    LibraryHomeCard(
                        iconRes = R.drawable.trending_up,
                        label = stringResource(R.string.my_top_50),
                        modifier = Modifier.weight(1f),
                    ) { navController.navigate("auto_playlist/top") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    LibraryHomeCard(
                        iconRes = R.drawable.history,
                        label = stringResource(R.string.history),
                        modifier = Modifier.weight(1f),
                    ) { navController.navigate("history") }
                    LibraryHomeCard(
                        iconRes = R.drawable.library_music,
                        label = stringResource(R.string.uploaded),
                        modifier = Modifier.weight(1f),
                    ) { navController.navigate("uploaded") }
                }
            }
        }

        // --- Recently Played (the streaming cache, no separate option) ---
        if (recentSongs.isNotEmpty()) {
            item(key = "recent_header", contentType = CONTENT_TYPE_HEADER) {
                Text(
                    text = recentlyPlayedTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            items(
                items = recentSongs,
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
                                        title = recentlyPlayedTitle,
                                        items = recentSongs.map { it.toMediaItem() },
                                        startIndex = recentSongs.indexOf(song),
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
}

/** One big square category card from the reference image: tonal surface,
 *  icon in a tinted rounded container, label underneath. */
@Composable
private fun LibraryHomeCard(
    iconRes: Int,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .padding(top = 8.dp)
            .aspectRatio(1f)
            .combinedClickable(onClick = onClick, onLongClick = {}),
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}
