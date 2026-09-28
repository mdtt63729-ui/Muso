package com.muso.music.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.muso.music.LocalPlayerConnection
import com.muso.music.R

/**
 * FULLSCREEN VIDEO - the destination the suite player's fullscreen button
 * navigates to. The route was never registered in Muso, so the button did
 * nothing. The canvas video fills the screen; a tap toggles a minimal
 * overlay (back + title), the back gesture/button leaves. Same
 * videoStreamUrl the canvas player plays - nothing is fetched twice.
 */
@Composable
fun FullscreenVideoScreen(navController: NavController) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val videoUrl by playerConnection.service.videoStreamUrl.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    var overlayVisible by remember { mutableStateOf(true) }
    BackHandler { navController.popBackStack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { overlayVisible = !overlayVisible },
    ) {
        videoUrl?.let { url ->
            com.maxrave.simpmusic.expect.ui.MediaPlayerView(
                url = url,
                cropToBounds = true,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (overlayVisible) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
                Text(
                    text = mediaMetadata?.title.orEmpty(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 16.dp),
                )
            }
        }
    }
}
