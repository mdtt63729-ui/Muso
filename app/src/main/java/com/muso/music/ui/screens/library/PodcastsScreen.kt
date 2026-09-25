package com.muso.music.ui.screens.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import com.zionhuang.innertube.pages.PodcastShowItem
import com.muso.music.LocalPlayerAwareWindowInsets
import com.muso.music.R
import com.muso.music.constants.CONTENT_TYPE_HEADER
import com.muso.music.ui.component.EmptyPlaceholder
import com.muso.music.viewmodels.PodcastsViewModel

/**
 * Saved podcasts (ReTune port, online): the user's subscribed podcast shows from YouTube
 * Music. Tapping a show opens its episode list.
 */
@Composable
fun PodcastsScreen(
    navController: NavController,
    topFilterContent: (@Composable () -> Unit)? = null,
    viewModel: PodcastsViewModel = hiltViewModel(),
) {
    val shows by viewModel.shows.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) {
        topFilterContent?.let { top ->
            item(key = "topFilter", contentType = CONTENT_TYPE_HEADER) {
                top()
            }
        }

        item(key = "header", contentType = CONTENT_TYPE_HEADER) {
            Text(
                text = stringResource(R.string.filter_podcasts),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        when {
            shows == null -> item(key = "loading") {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                ) {
                    CircularProgressIndicator()
                }
            }

            shows?.getOrNull().isNullOrEmpty() -> item(key = "empty") {
                EmptyPlaceholder(
                    icon = R.drawable.graphic_eq,
                    text = if (shows!!.isFailure) {
                        stringResource(R.string.login_required)
                    } else {
                        stringResource(R.string.podcasts_empty)
                    },
                )
            }

            else -> items(
                items = shows!!.getOrThrow(),
                key = { it.id },
            ) { show ->
                PodcastShowRow(show) {
                    navController.navigate("podcast/${show.id}")
                }
            }
        }
    }
}

@Composable
private fun PodcastShowRow(
    show: PodcastShowItem,
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
            model = show.thumbnail,
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = show.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
            show.author?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                )
            }
        }
        Icon(
            painter = painterResource(R.drawable.arrow_forward),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
