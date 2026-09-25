package com.muso.music.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.muso.music.LocalPlayerConnection
import com.muso.music.constants.LyricsTextSizeKey
import com.muso.music.constants.PlayerHorizontalPadding
import com.muso.music.constants.SeekExtraSecondsKey
import com.muso.music.constants.ShowLyricsKey
import com.muso.music.constants.ReducedMotionKey
import com.muso.music.constants.RotatingArtworkKey
import com.muso.music.constants.CropAlbumArtKey
import com.muso.music.constants.HidePlayerThumbnailKey
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.aspectRatio
import com.muso.music.constants.ThumbnailCornerRadius
import com.muso.music.ui.component.BounceIconButton
import com.muso.music.ui.component.Lyrics
import com.muso.music.constants.AnimatedArtworkKey
import com.muso.music.utils.rememberPreference

@Composable
fun Thumbnail(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentView = LocalView.current

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val error by playerConnection.error.collectAsState()

    val showLyrics by rememberPreference(ShowLyricsKey, false)
    val (lyricsTextSize, onLyricsTextSize) = rememberPreference(LyricsTextSizeKey, 26)
    var lyricsFullscreen by rememberSaveable { mutableStateOf(false) }
    val animatedArtwork by rememberPreference(AnimatedArtworkKey, false)
    // Reduced Motion silences the Ken Burns breathing zoom (Animation settings).
    val reducedMotion by rememberPreference(ReducedMotionKey, false)
    val hideThumbnail by rememberPreference(HidePlayerThumbnailKey, false)
    val seekExtraSeconds by rememberPreference(SeekExtraSecondsKey, false)
    var lastSeekTapTime by remember { mutableStateOf(0L) }
    var accumulatedSeekMs by remember { mutableStateOf(0L) }
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)
    val rotatingArtwork by rememberPreference(RotatingArtworkKey, false)
    val kenBurnsActive = animatedArtwork && !reducedMotion
    val rotateActive = rotatingArtwork && !reducedMotion

    // Animated artwork (Ken Burns): a very slow breathing zoom. Pure GPU transform on the
    // image layer, so it costs nothing to scroll or interact.
    val kenBurns = rememberInfiniteTransition(label = "kenBurns")
    val kenBurnsScale by kenBurns.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "kenBurnsScale",
    )

    // Rotating artwork (vinyl-style): slow continuous spin, pure GPU transform.
    val rotationTransition = rememberInfiniteTransition(label = "rotateArtwork")
    val rotationAngle by rotationTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
        ),
        label = "rotationAngle",
    )

    DisposableEffect(showLyrics) {
        currentView.keepScreenOn = showLyrics
        onDispose {
            currentView.keepScreenOn = false
        }
    }

    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = !showLyrics && error == null && !hideThumbnail,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = PlayerHorizontalPadding)
            ) {
                AsyncImage(
                    model = mediaMetadata?.thumbnailUrl,
                    contentDescription = null,
                    contentScale = if (cropAlbumArt) ContentScale.Crop else ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (cropAlbumArt) Modifier.aspectRatio(1f) else Modifier)
                        .clip(
                            if (rotateActive) {
                                androidx.compose.foundation.shape.CircleShape
                            } else {
                                RoundedCornerShape(ThumbnailCornerRadius * 2)
                            },
                        )
                        .graphicsLayer {
                            if (kenBurnsActive) {
                                scaleX = kenBurnsScale
                                scaleY = kenBurnsScale
                                transformOrigin = TransformOrigin(0.42f, 0.38f)
                            }
                            if (rotateActive) {
                                rotationZ = rotationAngle
                            }
                        }
                        .pointerInput(seekExtraSeconds) {
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    if (seekExtraSeconds) {
                                        // Echo Player and Audio: repeated taps add up (10s, 20s, 30s...).
                                        val now = System.currentTimeMillis()
                                        accumulatedSeekMs =
                                            if (now - lastSeekTapTime < 2000L) accumulatedSeekMs + 10_000L else 10_000L
                                        lastSeekTapTime = now
                                        val base = playerConnection.player.currentPosition
                                        val target = if (offset.x < size.width / 2) {
                                            base - accumulatedSeekMs
                                        } else {
                                            base + accumulatedSeekMs
                                        }
                                        val duration = playerConnection.player.duration
                                        val clamped = if (duration > 0) target.coerceIn(0L, duration) else target.coerceAtLeast(0L)
                                        playerConnection.player.seekTo(clamped)
                                    } else if (offset.x < size.width / 2) {
                                        playerConnection.player.seekBack()
                                    } else {
                                        playerConnection.player.seekForward()
                                    }
                                }
                            )
                        }
                )
            }
        }

        AnimatedVisibility(
            visible = showLyrics && error == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Lyrics(sliderPositionProvider = sliderPositionProvider)

                // Echo-style lyrics toolbar: fullscreen + three-dot actions.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    var lyricsMenu by remember { mutableStateOf(false) }
                    BounceIconButton(onClick = { lyricsFullscreen = true }) {
                        Icon(painterResource(R.drawable.fullscreen), null, Modifier.size(20.dp))
                    }
                    BounceIconButton(onClick = { lyricsMenu = true }) {
                        Icon(painterResource(R.drawable.more_vert), null, Modifier.size(20.dp))
                    }
                    DropdownMenu(
                        expanded = lyricsMenu,
                        onDismissRequest = { lyricsMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.bigger_lyrics)) },
                            onClick = {
                                onLyricsTextSize((lyricsTextSize + 2).coerceAtMost(40))
                                lyricsMenu = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.smaller_lyrics)) },
                            onClick = {
                                onLyricsTextSize((lyricsTextSize - 2).coerceAtLeast(18))
                                lyricsMenu = false
                            },
                        )
                    }
                }
            }
        }

        // Fullscreen lyrics (Echo lyrics experience): a true full-screen dialog with
        // the same synced lyrics inside, dismissed by tap or the close button.
        if (lyricsFullscreen) {
            Dialog(
                onDismissRequest = { lyricsFullscreen = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .systemBarsPadding(),
                ) {
                    Lyrics(sliderPositionProvider = sliderPositionProvider)
                    BounceIconButton(
                        onClick = { lyricsFullscreen = false },
                        modifier = Modifier.align(Alignment.TopEnd),
                        buttonSize = 44.dp,
                    ) {
                        Icon(painterResource(R.drawable.close), null, Modifier.size(22.dp))
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .padding(32.dp)
                .align(Alignment.Center)
        ) {
            error?.let { error ->
                PlaybackError(
                    error = error,
                    retry = playerConnection.player::prepare
                )
            }
        }
    }
}
