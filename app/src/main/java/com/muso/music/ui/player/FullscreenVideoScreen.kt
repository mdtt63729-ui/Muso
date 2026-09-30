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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import androidx.navigation.NavController
import com.muso.music.LocalPlayerConnection
import com.muso.music.R
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import com.maxrave.common.Config
import androidx.media3.datasource.cache.SimpleCache

/**
 * FULLSCREEN VIDEO (Round 174 rewrite).
 *
 * What was wrong: the screen reused the suite's canvas MediaPlayerView, which
 * is a 15-second LOOPING segment player (the in-player canvas PRD). In
 * fullscreen the video therefore jumped back to the start every fifteen
 * seconds — "videos don't play properly". It also had its own audio (double
 * sound with the service) and started from 0:00 instead of the song's
 * position.
 *
 * This screen now owns a dedicated player that:
 *  - plays the WHOLE video (no loop controller);
 *  - starts at the main service player's position and follows it (drift
 *    corrected, pause/play followed);
 *  - is MUTED — the audio keeps coming from the service stream, so the sound
 *    never doubles and stays gapless;
 *  - fills the screen EDGE TO EDGE (scale-to-cover, no insets, no bars).
 *
 * Blank-screen guard: if the app is restored into this route with a dead
 * video URL (process death while the fullscreen video was open — the blank
 * fullscreen player the user saw at app launch), the screen pops itself back
 * out once the grace period passes without a playable URL.
 */
@Composable
fun FullscreenVideoScreen(navController: NavController) {
    val context = LocalContext.current
    val canvasCache: SimpleCache = koinInject(named(Config.CANVAS_CACHE))
    val playerConnection = LocalPlayerConnection.current
    val videoUrl = playerConnection?.service?.videoStreamUrl?.collectAsState()?.value
    val mediaMetadata = playerConnection?.mediaMetadata?.collectAsState()?.value
    val isPlaying = playerConnection?.isPlaying?.collectAsState()?.value

    var overlayVisible by remember { mutableStateOf(true) }
    BackHandler { navController.popBackStack() }

    // Blank-restored-route guard: no URL and no metadata after the grace
    // period means this route was restored by the system after process death
    // with an expired stream - leave instead of showing a black screen.
    LaunchedEffect(playerConnection) {
        if (playerConnection == null) {
            delay(1200)
            navController.popBackStack()
            return@LaunchedEffect
        }
        delay(3000)
        if (videoUrl == null) navController.popBackStack()
    }

    val exoPlayer =
        remember {
            val cacheSink = CacheDataSink.Factory().setCache(canvasCache)
            val upstreamFactory = DefaultDataSource.Factory(context, DefaultHttpDataSource.Factory())
            val downStreamFactory = FileDataSource.Factory()
            val cacheDataSourceFactory =
                CacheDataSource
                    .Factory()
                    .setCache(canvasCache)
                    .setCacheWriteDataSinkFactory(cacheSink)
                    .setCacheReadDataSourceFactory(downStreamFactory)
                    .setUpstreamDataSourceFactory(upstreamFactory)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            ExoPlayer
                .Builder(context)
                .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
                .build()
                .apply {
                    videoScalingMode = C.VIDEO_SCALING_MODE_DEFAULT
                    // The service stream carries the audio; this surface is
                    // video only. Muting here is what keeps the sound single
                    // and gapless.
                    volume = 0f
                    repeatMode = Player.REPEAT_MODE_OFF
                }
        }

    var preparedFor by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(videoUrl) {
        val url = videoUrl ?: return@LaunchedEffect
        if (preparedFor != url) {
            preparedFor = url
            exoPlayer.setMediaItem(MediaItem.fromUri(url))
            exoPlayer.prepare()
            // Start in step with the song, not from zero.
            val songPosition = playerConnection?.player?.currentPosition ?: 0L
            runCatching { exoPlayer.seekTo(songPosition) }
            exoPlayer.play()
        }
    }

    // Follow the service: pause/play and drift correction.
    LaunchedEffect(isPlaying, playerConnection) {
        while (true) {
            delay(500)
            val playing = playerConnection?.isPlaying?.value
            if (playing == false && exoPlayer.isPlaying) exoPlayer.pause()
            if (playing == true && !exoPlayer.isPlaying && preparedFor != null) exoPlayer.play()
            val songPos = playerConnection?.player?.currentPosition ?: continue
            val videoPos = runCatching { exoPlayer.currentPosition }.getOrDefault(0L)
            if (kotlin.math.abs(videoPos - songPos) > 700 && exoPlayer.duration != C.TIME_UNSET) {
                runCatching { exoPlayer.seekTo(songPos) }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { exoPlayer.release() }
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { overlayVisible = !overlayVisible },
    ) {
        videoUrl?.let { url ->
            // Edge to edge: scale-to-cover the entire screen, true aspect
            // ratio preserved, overflow clipped - no bars, no inset margins.
            val presentationState = rememberPresentationState(exoPlayer)
            Box(Modifier.fillMaxSize().graphicsLayer { clip = true }) {
                PlayerSurface(
                    player = exoPlayer,
                    surfaceType = SURFACE_TYPE_SURFACE_VIEW,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .resizeWithContentScale(
                                contentScale = ContentScale.Crop,
                                sourceSizeDp = presentationState.videoSizeDp,
                            ),
                )
                if (presentationState.coverSurface) {
                    Box(Modifier.fillMaxSize().background(Color.Black))
                }
            }
        }
        if (overlayVisible) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
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
