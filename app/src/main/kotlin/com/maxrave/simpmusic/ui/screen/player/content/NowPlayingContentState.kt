package com.maxrave.simpmusic.ui.screen.player.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.streams.TimeLine
import com.maxrave.domain.data.player.GenericCastState
import com.maxrave.domain.mediaservice.handler.ControlState
import com.maxrave.simpmusic.extension.GradientOffset
import com.maxrave.simpmusic.viewModel.LyricsProvider
import com.maxrave.simpmusic.viewModel.NowPlayingScreenData
import com.maxrave.simpmusic.viewModel.UIEvent
import kotlinx.coroutines.flow.StateFlow

/**
 * Whether the lyrics currently on screen can be rated.
 *
 * SimpMusic Lyrics is the only provider with a vote endpoint, and the vote is cast against the
 * SimpMusic record itself — so the provider tag alone is NOT the condition: `simpMusicLyrics`
 * must actually be there. Either half qualifying is enough, because the dialog rates whichever
 * of the two came from SimpMusic.
 *
 * Lives on the contract the three styles share. The Apple Music style shipped its floating vote
 * button ungated — offering a rating on YouTube, LRCLIB and Spotify lyrics alike — precisely
 * because this rule existed only as an expression copy-pasted inside the other two styles, where
 * a new style had no reason to go looking for it.
 */
internal fun NowPlayingScreenData.LyricsData?.canVote(): Boolean {
    val data = this ?: return false
    val votableLyrics =
        data.lyricsProvider == LyricsProvider.SIMPMUSIC && data.lyrics.simpMusicLyrics != null
    val votableTranslation =
        data.translatedLyrics?.second == LyricsProvider.SIMPMUSIC &&
            data.translatedLyrics?.first?.simpMusicLyrics != null
    return votableLyrics || votableTranslation
}

// Backdrop behind the player. A dark surface rather than pure black: #000000 reads as a hole
// next to the artwork-tinted gradient and cards, which is why Spotify sits its player on a
// near-black surface instead. Used for the gradient's end colour, the fade-to target and the
// area below the gradient so all three match exactly and leave no seam.
internal val PlayerBackdropColor = Color(0xFF121212)

private val RICH_SYNC_TIMESTAMP_REGEX = Regex("""<\d{2}:\d{2}\.\d{2,3}>\s*""")
private val WHITESPACE_REGEX = Regex("""\s+""")

// Word-by-word lyrics carry a timestamp per word; replace each with a space
// (not ""), then collapse — otherwise the words run together.
// Shared by every Now Playing content style (Spotify + M3 Expressive).
internal fun String.stripRichSyncTimestamps(): String =
    replace(RICH_SYNC_TIMESTAMP_REGEX, " ")
        .replace(WHITESPACE_REGEX, " ")
        .trim()

// Real stream format helpers. The badge intentionally uses the resolved playback format,
// never an itag's nominal quality bucket. That means the user sees the actual container,
// codec and bitrate that the player resolved (for example: WEBM • OPUS • 129 kbps).
internal fun String?.toAudioCodecLabel(): String? {
    val codec = this ?: return null
    return when {
        codec.contains("opus", ignoreCase = true) -> "OPUS"
        codec.contains("mp4a", ignoreCase = true) || codec.contains("aac", ignoreCase = true) -> "AAC"
        codec.contains("flac", ignoreCase = true) -> "FLAC"
        codec.contains("vorbis", ignoreCase = true) -> "VORBIS"
        else -> null
    }
}

internal fun String?.toAudioContainerLabel(): String? {
    val mime = this ?: return null
    val container = mime.substringAfter('/').substringBefore(';').trim()
    if (container.isBlank()) return null
    return when (container.lowercase()) {
        "x-m4a", "m4a" -> "M4A"
        "mp4" -> "MP4"
        "webm" -> "WEBM"
        "ogg" -> "OGG"
        "flac" -> "FLAC"
        else -> container.uppercase()
    }
}

/**
 * The real codec capsule rendered immediately below the player's seek slider.
 *
 * - Setting OFF: no capsule and no loading work is shown.
 * - Setting ON + format unresolved: a compact animated loading capsule is shown.
 * - Format resolved: container + codec + the exact resolved bitrate are revealed with a
 *   smooth fade/scale transition.
 */
@Composable
internal fun PlayerCodecCapsule(
    state: NowPlayingContentState,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.White.copy(alpha = 0.16f),
    contentColor: Color = Color.White.copy(alpha = 0.9f),
) {
    if (!state.showCodecBadge) return

    val hasResult = state.audioCodecLabel != null
    val loading = state.audioCodecLoading
    if (!loading && !hasResult) return
    val infinite = rememberInfiniteTransition(label = "codecCapsuleLoading")
    val shimmer by infinite.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "codecCapsuleShimmer",
    )

    AnimatedVisibility(
        visible = loading || hasResult,
        enter = fadeIn(tween(220)) + scaleIn(tween(260), initialScale = 0.88f),
        exit = fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.92f),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(containerColor)
                    .padding(horizontal = 11.dp, vertical = 4.dp),
            ) {
                if (loading) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(contentColor.copy(alpha = shimmer)),
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 58.dp, height = 7.dp)
                                .clip(RoundedCornerShape(50))
                                .background(contentColor.copy(alpha = shimmer * 0.8f)),
                        )
                    }
                } else {
                    Text(
                        text = state.audioCodecLabel.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Everything a Now Playing content layer reads. The shell ([com.maxrave.simpmusic.ui.screen.player.NowPlayingScreenContent])
 * owns the ViewModel collection, palette animation, sheets/dialogs and gesture state machines;
 * a content composable only renders from this snapshot.
 */
@Stable
class NowPlayingContentState(
    val screenData: NowPlayingScreenData,
    val controllerState: ControlState,
    val timelineState: TimeLine,
    val timelineFlow: StateFlow<TimeLine>,
    val likeStatus: Boolean,
    val castState: GenericCastState,
    val shouldShowVideo: Boolean,
    val isUserLoggedIn: Boolean,
    val artworkQueue: List<Track>,
    val currentOrderIndex: Int,
    val artworkPagerState: PagerState,
    val startColor: Animatable<Color, AnimationVector4D>,
    val endColor: Animatable<Color, AnimationVector4D>,
    val spotShadowColor: Color,
    val gradientOffset: GradientOffset,
    val sliderTrackColor: Color,
    val sliderValue: Float,
    val currentLyricLineIndex: Int,
    val showControlLayout: Boolean,
    val controlLayoutAlpha: Float,
    val showHideMiddleLayout: Boolean,
    val shouldShowToolbar: Boolean,
    val isInPipMode: Boolean,
    val mainScrollState: ScrollState,
    val isExpanded: Boolean,
    val dismissIcon: ImageVector,
    /** Real current-track stream details, e.g. "WEBM • OPUS • 129 kbps". */
    val audioCodecLabel: String? = null,
    /** True while the current track's resolved stream format is still being obtained. */
    val audioCodecLoading: Boolean = false,
    /** Whether the codec capsule may render — the Show codec on player setting. */
    val showCodecBadge: Boolean = false,
    /**
     * Width / height of the video now playing, 16:9 until the player knows it. Every style sizes
     * its video frame from this one value, so a frame and the spacer that measures it cannot drift.
     */
    val videoAspectRatio: Float = 16f / 9,
) {
    /**
     * Reliable current-track artwork. Some Muso bridge states do not populate
     * NowPlayingScreenData.thumbnailURL, while the real queue Track already
     * carries the original artwork URL. Every Now Playing style should use this
     * single resolved value so no style falls back to a blank/placeholder cover.
     */
    val thumbnailURL: String?
        get() = screenData.thumbnailURL?.takeIf { it.isNotBlank() }
            ?: artworkQueue.getOrNull(currentOrderIndex)?.thumbnails
                ?.maxByOrNull { it.width * it.height }?.url
            ?: artworkQueue.firstOrNull()?.thumbnails
                ?.maxByOrNull { it.width * it.height }?.url

    /** Foreground chosen from the actual animated artwork background, not the app theme. */
    val adaptiveForeground: Color
        get() = if (startColor.value.luminance() > 0.52f) Color.Black else Color.White

    val adaptiveForegroundMuted: Color
        get() = adaptiveForeground.copy(alpha = 0.72f)

    val adaptiveBackground: Color
        get() = if (startColor.value.luminance() > 0.52f) Color.White else Color.Black
}

/**
 * Everything a Now Playing content layer can do. All callbacks land in the shell, which owns
 * the ViewModel, the navController and the sheet/dialog visibility flags.
 */
@Stable
class NowPlayingContentActions(
    val onUIEvent: (UIEvent) -> Unit,
    val onSeekToQueueIndex: (Int) -> Unit,
    val onArtworkBitmap: (ImageBitmap) -> Unit,
    val onSliderChange: (Float) -> Unit,
    val onSliderChangeFinished: () -> Unit,
    val onToggleControls: () -> Unit,
    val onNavigateToArtist: () -> Unit,
    val onAddToYouTubeLiked: () -> Unit,
    val onShowMoreSheet: () -> Unit,
    val onShowQueue: () -> Unit,
    val onShowInfo: () -> Unit,
    val onShowAddToPlaylist: () -> Unit,
    val onShowFullscreenLyrics: () -> Unit,
    val onShowVoteDialog: () -> Unit,
    val onEnterFullscreenVideo: () -> Unit,
    val onDismiss: () -> Unit,
    val onToolbarVisibilityChange: (Boolean) -> Unit,
    /** Reorders the queue. `from`/`to` are absolute indices into [NowPlayingContentState.artworkQueue]. */
    val onMoveQueueItem: (from: Int, to: Int) -> Unit,
    /** Removes one queue entry. `index` is an absolute index into [NowPlayingContentState.artworkQueue]. */
    val onRemoveQueueItem: (index: Int) -> Unit,
)
