package com.muso.music.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter

/**
 * AsyncImage with a shimmer skeleton behind it while the image downloads, replacing the
 * blank gap a slowly-arriving thumbnail used to leave in playlist cards and song rows.
 * Once the image lands it crossfades in (crossfade is enabled app-wide in the Coil
 * ImageLoader).
 *
 * Implemented with rememberAsyncImagePainter rather than SubcomposeAsyncImage so there is
 * no subcomposition cost per card — the shimmer layer is simply dropped as soon as the
 * painter reports success, which keeps scrolling smooth even in long lists.
 */
@Composable
fun ShimmerAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val painter = rememberAsyncImagePainter(model = model)
    Box(modifier = modifier) {
        Image(
            painter = painter,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
        )
        if (painter.state is AsyncImagePainter.State.Loading) {
            ShimmerBox(modifier = Modifier.fillMaxSize())
        }
    }
}

/**
 * A soft moving-gradient skeleton block. Colors derive from the theme so it reads as a
 * "loading" state in both light and dark themes.
 */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmerBox")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
        ),
        label = "shimmerShift",
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(
                    base.copy(alpha = 0.35f),
                    base.copy(alpha = 0.75f),
                    base.copy(alpha = 0.35f),
                ),
                start = Offset(x = shift * 1200f - 600f, y = 0f),
                end = Offset(x = shift * 1200f, y = 240f),
            )
        )
    )
}
