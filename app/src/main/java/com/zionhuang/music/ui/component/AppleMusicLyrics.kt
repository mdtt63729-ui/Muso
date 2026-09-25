package com.zionhuang.music.ui.component

import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * Apple Music lyrics focus treatment, ported from SimpMusic's AppleMusicLyricsLines.
 *
 * Every line renders at the SAME size — what separates the line being sung from the rest is
 * focus, not scale: the active line is opaque and sharp, its neighbours dim and progressively
 * blurred with distance, as if the page had a shallow depth of field.
 *
 * The distance rule is AMLL's (resolveBlurLevel): the sung line is exempt outright, and lines
 * already sung carry a +1 so the page behind the singer recedes faster than the page ahead.
 * Blur magnitude is expressed against the font size rather than in fixed dp, so changing the
 * type size keeps the depth of field proportional.
 */
private const val BLUR_PER_LINE_EM = 0.095f
private const val BLUR_MAX_EM = 0.45f

// Blur and the alpha falloff work together rather than either doing the job alone: blur alone
// leaves far lines as bright smears, fade alone leaves them sharp and readable when they should
// not be. One line out still reads clearly, two is noticeably dimmer, and past three they level
// off — blur is doing most of the work by then.
private const val ACTIVE_LINE_ALPHA = 1f
private const val ALPHA_FALLOFF_PER_LINE = 0.25f
private const val MIN_LINE_ALPHA = 0.25f

// Blur layers are the expensive part of this treatment. During fast scroll, dozens of far-away
// lines would each get a RenderEffect for nothing — they are already at MIN_LINE_ALPHA where the
// blur is imperceptible. Capping the blur distance keeps only a small window of layers alive.
private const val BLUR_MAX_DISTANCE = 5

// Before the FIRST line is due — an intro, a long instrumental opening — there is no sung line
// for anything to be near, so every line is dimmed uniformly and NOT blurred: blur means "far
// from where we are in the song", and during an intro nowhere is where we are.
private const val PRE_ROLL_LINE_ALPHA = 0.6f

/** Apple sets lyrics far larger than body copy; matched to SimpMusic's Apple Music renderer. */
val AppleMusicLyricFontSize = 26.sp

/** The line NOT being sung is grey — the single biggest signal separating it from the sung line. */
val AppleMusicInactiveLineColor = Color(0xFF9B9B9B)

/**
 * Depth-of-field treatment for one lyric line. [distanceFromCurrent] is signed line distance
 * from the line being sung; its magnitude drives both blur and dimming.
 *
 * Both values are animated rather than applied outright, so the focus glides down the page with
 * the song instead of snapping. On devices below API 31 (no RenderEffect) the blur silently
 * degrades to the alpha falloff only.
 */
@Composable
fun Modifier.appleMusicLyricFocus(
    distanceFromCurrent: Int,
    hasActiveLine: Boolean,
    // An unsynced sheet has no sung line and never will have one, so every line IS the sung
    // line: full opacity, no blur.
    allLinesCurrent: Boolean = false,
): Modifier {
    // Signed: negative means this line has already been sung. The page behind the singer
    // recedes faster than the page ahead of it.
    val distance = when {
        distanceFromCurrent < 0 -> abs(distanceFromCurrent) + 1
        else -> distanceFromCurrent
    }
    val fontSizeDp = with(LocalDensity.current) { AppleMusicLyricFontSize.toDp() }
    val blurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val targetBlur: Dp =
        if (!blurSupported || allLinesCurrent || !hasActiveLine || distanceFromCurrent == 0 || distance > BLUR_MAX_DISTANCE) {
            0.dp
        } else {
            fontSizeDp * (distance * BLUR_PER_LINE_EM).coerceAtMost(BLUR_MAX_EM)
        }
    val targetAlpha =
        when {
            // Ahead of the [hasActiveLine] branch: an unsynced sheet satisfies both and wants
            // the opposite answer — every line is the sung line, so every line is fully lit.
            allLinesCurrent -> ACTIVE_LINE_ALPHA
            // Checked before the distance test: the caller passes distance 0 for every line
            // while no line is active, and 0 otherwise means "this is the sung line".
            !hasActiveLine -> PRE_ROLL_LINE_ALPHA
            distanceFromCurrent == 0 -> ACTIVE_LINE_ALPHA
            else -> (1f - distance * ALPHA_FALLOFF_PER_LINE).coerceAtLeast(MIN_LINE_ALPHA)
        }

    val blurRadius by animateDpAsState(targetValue = targetBlur, animationSpec = tween(400), label = "appleMusicLyricBlur")
    val lineAlpha by animateFloatAsState(targetValue = targetAlpha, animationSpec = tween(400), label = "appleMusicLyricAlpha")

    // alpha BEFORE blur: blurring an already-faded line keeps the two effects independent,
    // whereas fading a blurred layer washes the blur out into a flat smear.
    return this
        .alpha(lineAlpha)
        .then(
            if (blurRadius > 0.dp) {
                // Unbounded, not the default: the default clips the blur to the line's own
                // bounds, so the softened glyphs get sliced off square at the edges and the
                // line reads as a smudged block rather than an out-of-focus word.
                Modifier.blur(blurRadius, BlurredEdgeTreatment.Unbounded)
            } else {
                Modifier
            }
        )
}
