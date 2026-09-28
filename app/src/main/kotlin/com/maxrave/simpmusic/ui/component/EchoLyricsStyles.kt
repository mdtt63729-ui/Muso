package com.maxrave.simpmusic.ui.component

/*
 * Echo word-by-word lyrics animation styles (Echo-Music PRD, folder
 * 2-word-by-word-animation-styles), rendered on top of the suite's already
 * parsed rich-sync data ([ParsedRichSyncLine]) so every style shares the
 * exact word timings the built-in flare wipe uses.
 *
 * FLARE (the default) keeps the suite's own RichSyncLyricsLineItem and never
 * reaches this file; the ten Echo styles are implemented here with the PRD's
 * formulas:
 *   FADE:     wordAlpha = 0.35 + 0.65 * smoothstep(progress)
 *   GLOW:     active word gets a soft glow shadow
 *   SLIDE:    translationY = (1 - progress) * offset
 *   KARAOKE:  per-word horizontal gradient fill as the word is sung
 * Lines without word timings never reach here - LyricsView only takes the
 * rich-sync branch when the line carries <mm:ss.xx> tags, and falls back to
 * the line-level items otherwise.
 */

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import com.maxrave.simpmusic.extension.ParsedRichSyncLine
import com.maxrave.simpmusic.ui.theme.typo
import com.muso.music.constants.LyricsAnimationStyle

/** Ink of a word that has not been sung yet. */
private val EchoPendingWordColor = Color(0xFF9E9E9E)
/** Ink of a word that is being sung or has been sung. */
private val EchoSungWordColor = Color.White
/** The glow behind the active word in GLOW / LYRICS_V2. */
private val EchoGlowColor = Color(0xAAFFFFFF)

private fun smoothstep(p: Float): Float = p * p * (3f - 2f * p)

/**
 * One Echo-styled rich-sync line: a drop-in alternative for
 * [RichSyncLyricsLineItem] inside LyricsView's RICH_SYNCED branch. The
 * romanization and translation rows below mirror LyricsLineItem.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EchoLyricsLine(
    parsedLine: ParsedRichSyncLine,
    translatedWords: String?,
    romanizedWords: String?,
    currentTimeMs: Long,
    isCurrent: Boolean,
    style: LyricsAnimationStyle,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(12.dp))
        if (style == LyricsAnimationStyle.METRO_LYRICS) {
            MetroEchoLine(
                parsedLine = parsedLine,
                currentTimeMs = currentTimeMs,
                isCurrent = isCurrent,
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                val words = parsedLine.words
                val last = words.size - 1
                words.forEachIndexed { index, word ->
                    val startMs = word.startTimeMs
                    val endMs =
                        words.getOrNull(index + 1)?.startTimeMs
                            ?: parsedLine.lineEndTimeMs.coerceAtLeast(startMs + 1L)
                    val duration = (endMs - startMs).coerceAtLeast(1L)
                    val progress =
                        ((currentTimeMs - startMs).toFloat() / duration).coerceIn(0f, 1f)
                    if (style == LyricsAnimationStyle.APPLE_V2) {
                        // Letter-by-letter two-layer fill (see AppleV2EchoWord).
                        AppleV2EchoWord(
                            text = word.text,
                            progress = progress,
                            isLineCurrent = isCurrent,
                            isLast = index == last,
                        )
                    } else {
                    EchoWord(
                        text = word.text,
                        progress = progress,
                        isLineCurrent = isCurrent,
                        isWordActive = isCurrent && currentTimeMs >= startMs && currentTimeMs < endMs,
                        isWordComplete = currentTimeMs >= endMs,
                        style = style,
                        isLast = index == last,
                    )
                    }
                }
            }
        }
        if (romanizedWords != null) {
            Text(
                text = romanizedWords,
                style = typo().bodyMedium,
                color = if (isCurrent) Color(0xFFD8D8D8) else Color(0xFF7A7A7A),
            )
        }
        if (translatedWords != null) {
            Text(
                text = translatedWords,
                style = typo().bodyMedium,
                color = if (isCurrent) Color(0xFFFFE97F) else Color(0xFF8A8563),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun EchoWord(
    text: String,
    progress: Float,
    isLineCurrent: Boolean,
    isWordActive: Boolean,
    isWordComplete: Boolean,
    style: LyricsAnimationStyle,
    isLast: Boolean,
) {
    val baseStyle = if (isLineCurrent) typo().headlineLarge else typo().headlineMedium
    val word = if (isLast) text else "$text "

    // ECHOMUSIC_1 / LYRICS_V2 drive their motion through an animated float
    // (the PRD's AnimatedWordV2 pattern: tween 350 ms) so the rise reads as
    // one fluid move rather than ten discrete ticks per second.
    val riseTarget = when (style) {
        LyricsAnimationStyle.ECHOMUSIC_1, LyricsAnimationStyle.LYRICS_V2 ->
            if (isWordComplete || isWordActive) 1f else 0f
        else -> 0f
    }
    val rise by animateFloatAsState(
        targetValue = riseTarget,
        animationSpec = tween(durationMillis = 350),
        label = "echoWordRise",
    )
    val slideProgress by animateFloatAsState(
        targetValue = if (isLineCurrent) progress else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "echoWordSlide",
    )

    var color = EchoPendingWordColor
    var alpha = 1f
    var renderedStyle = baseStyle
    when (style) {
        LyricsAnimationStyle.NONE -> {
            color =
                if (!isLineCurrent) EchoPendingWordColor
                else if (isWordComplete || isWordActive) EchoSungWordColor
                else EchoPendingWordColor
        }
        LyricsAnimationStyle.FADE -> {
            color = if (isLineCurrent) EchoSungWordColor else EchoPendingWordColor
            alpha = if (isLineCurrent) 0.35f + 0.65f * smoothstep(progress) else 0.35f
        }
        LyricsAnimationStyle.GLOW -> {
            color = if (isLineCurrent) EchoSungWordColor else EchoPendingWordColor
            alpha = if (isLineCurrent) 1f else 0.35f
            if (isWordActive) {
                renderedStyle = baseStyle.copy(shadow = Shadow(color = EchoGlowColor, blurRadius = 18f))
            }
        }
        LyricsAnimationStyle.LYRICS_V2 -> {
            color = if (isLineCurrent) EchoSungWordColor else EchoPendingWordColor
            alpha = if (isLineCurrent) 0.3f + 0.7f * rise else 0.3f
            if (isWordActive) {
                renderedStyle = baseStyle.copy(shadow = Shadow(color = EchoGlowColor, blurRadius = 12f))
            }
        }
        LyricsAnimationStyle.SLIDE -> {
            color = if (isLineCurrent) EchoSungWordColor else EchoPendingWordColor
        }
        LyricsAnimationStyle.KARAOKE -> {
            color = EchoSungWordColor
            if (isLineCurrent && !isWordComplete) {
                val cut = (progress * 0.999f).coerceAtMost(1f)
                renderedStyle = baseStyle.copy(
                    brush = Brush.horizontalGradient(
                        *arrayOf(
                            0f to EchoSungWordColor,
                            cut to EchoSungWordColor,
                            (cut + 0.001f).coerceAtMost(1f) to EchoPendingWordColor,
                            1f to EchoPendingWordColor,
                        ),
                    ),
                )
            } else if (!isLineCurrent) {
                color = EchoPendingWordColor
            }
        }
        LyricsAnimationStyle.APPLE -> {
            color =
                if (isLineCurrent && (isWordComplete || isWordActive)) EchoSungWordColor
                else EchoPendingWordColor
            alpha = if (isLineCurrent) 1f else 0.45f
        }
        else -> {
            // ECHOMUSIC_1 (and anything else) shares the colour path.
            color = if (isLineCurrent) EchoSungWordColor else EchoPendingWordColor
            alpha = if (isLineCurrent) 0.25f + 0.75f * rise else 0.25f
        }
    }

    var ty = 0f
    var scale = 1f
    when (style) {
        LyricsAnimationStyle.SLIDE -> ty = (1f - slideProgress) * 10f
        LyricsAnimationStyle.ECHOMUSIC_1 -> {
            ty = (1f - rise) * 8f
            scale = 0.92f + 0.08f * rise
        }
        LyricsAnimationStyle.LYRICS_V2 -> {
            ty = (1f - rise) * 6f
            if (isWordActive) scale = 1f + 0.06f * rise
        }
        else -> {}
    }

    Text(
        text = word,
        style = renderedStyle,
        color = color,
        overflow = TextOverflow.Visible,
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            translationY = ty
            scaleX = scale
            scaleY = scale
        },
    )
}

/**
 * APPLE_V2 (letter-by-letter), ported from the kimi lyrics reference: each
 * word of the ACTIVE line renders as two stacked layers -
 *
 *  - a dim base pre-render at 30% opacity, and
 *  - a bright fill that sweeps in left-to-right with a soft gradient front
 *    (feather ~8% of the word), masked by the word's own 0..1 progress, with
 *    a soft 12px glow riding the fill;
 *
 * and while the word is being sung it gently floats up
 * (4px * sin(progress * PI)) and scales by 2%. Completed words show the full
 * fill; not-yet-sung words show the dim base. Lines that are not current
 * render as plain dim text.
 */
@Composable
private fun AppleV2EchoWord(
    text: String,
    progress: Float,
    isLineCurrent: Boolean,
    isLast: Boolean,
) {
    val word = if (isLast) text else "$text "
    val baseStyle = if (isLineCurrent) typo().headlineLarge else typo().headlineMedium
    if (!isLineCurrent) {
        Text(
            text = word,
            style = baseStyle,
            color = EchoPendingWordColor,
            overflow = TextOverflow.Visible,
        )
        return
    }
    // Soft glow riding the fill layer (reference: text-shadow 0 0 12px @35%).
    val fillStyle = baseStyle.copy(
        shadow = Shadow(color = Color.White.copy(alpha = 0.35f), blurRadius = 12f),
    )
    // The mask: opaque up to the word's progress, then an 8% feather to
    // transparent - the letters fill in one after another as it advances.
    val cut = progress
    val fillBrush = if (progress >= 1f) null else Brush.horizontalGradient(
        *arrayOf(
            0f to Color.White,
            cut to Color.White,
            (cut + 0.08f).coerceAtMost(1f) to Color.Transparent,
            1f to Color.Transparent,
        ),
    )
    // Gentle float + scale while the word sings.
    val sinP = sin(progress * Math.PI.toFloat())

    Box {
        // Base layer - the dim pre-render of the whole word.
        Text(
            text = word,
            style = baseStyle,
            color = Color.White.copy(alpha = 0.3f),
            overflow = TextOverflow.Visible,
        )
        // Fill layer - sweeps over the base, masked by the word's progress.
        if (fillBrush != null) {
            Text(
                text = word,
                style = fillStyle.copy(brush = fillBrush),
                color = Color.Unspecified,
                overflow = TextOverflow.Visible,
                modifier = Modifier.graphicsLayer {
                    translationY = -4f * sinP
                    scaleX = 1f + 0.02f * sinP
                    scaleY = 1f + 0.02f * sinP
                },
            )
        } else {
            Text(
                text = word,
                style = fillStyle,
                color = Color.White,
                overflow = TextOverflow.Visible,
                modifier = Modifier.graphicsLayer {
                    translationY = 0f
                    scaleX = 1f
                    scaleY = 1f
                },
            )
        }
    }
}

/**
 * METRO_LYRICS: canvas word-level render. Every word is measured and laid
 * out left-to-right with wrapping, then drawn with a colour driven by its
 * own progress - sung words white, the active word's fill advancing through
 * the word, pending words dim.
 */
@Composable
private fun MetroEchoLine(
    parsedLine: ParsedRichSyncLine,
    currentTimeMs: Long,
    isCurrent: Boolean,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val baseStyle = if (isCurrent) typo().headlineLarge else typo().headlineMedium
    val maxWidthPx = with(density) { 360.dp.toPx() }
    val layout = remember(parsedLine, isCurrent, baseStyle) {
        measureMetroWords(textMeasurer, parsedLine, baseStyle, maxWidthPx, density)
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(layout.heightDp),
    ) {
        layout.rows.forEach { row ->
            var x = 0f
            row.words.forEach { slot ->
                val t = currentTimeMs
                val progress = ((t - slot.startMs).toFloat() / (slot.endMs - slot.startMs).coerceAtLeast(1L))
                    .coerceIn(0f, 1f)
                val paint =
                    when {
                        !isCurrent -> EchoPendingWordColor
                        t >= slot.endMs -> EchoSungWordColor
                        t >= slot.startMs -> lerpColor(EchoPendingWordColor, EchoSungWordColor, progress)
                        else -> EchoPendingWordColor
                    }
                drawText(
                    textLayoutResult = slot.layout,
                    color = paint,
                    topLeft = Offset(x, row.y),
                )
                x += slot.widthPx + layout.spacePx
            }
        }
    }
}

private fun lerpColor(from: Color, to: Color, fraction: Float): Color =
    Color(
        red = from.red + (to.red - from.red) * fraction,
        green = from.green + (to.green - from.green) * fraction,
        blue = from.blue + (to.blue - from.blue) * fraction,
        alpha = from.alpha + (to.alpha - from.alpha) * fraction,
    )

private data class MetroWordSlot(
    val layout: TextLayoutResult,
    val widthPx: Float,
    val startMs: Long,
    val endMs: Long,
)

private data class MetroRow(
    val y: Float,
    val words: List<MetroWordSlot>,
)

private data class MetroLayout(
    val rows: List<MetroRow>,
    val heightDp: Dp,
    val spacePx: Float,
)

private fun measureMetroWords(
    textMeasurer: TextMeasurer,
    parsedLine: ParsedRichSyncLine,
    style: TextStyle,
    maxWidthPx: Float,
    density: Density,
): MetroLayout {
    val spacePx = with(density) { 4.dp.toPx() }
    val lineHeightPx = with(density) { 34.dp.toPx() }
    val rows = mutableListOf<MetroRow>()
    var current = mutableListOf<MetroWordSlot>()
    var x = 0f
    var y = 0f
    val words = parsedLine.words
    words.forEachIndexed { index, word ->
        val text = if (index == words.size - 1) word.text else word.text + " "
        val layout = textMeasurer.measure(text = text, style = style, maxLines = 1)
        if (x > 0f && x + layout.size.width.toFloat() > maxWidthPx) {
            rows.add(MetroRow(y = y, words = current))
            current = mutableListOf()
            x = 0f
            y += lineHeightPx
        }
        val startMs = word.startTimeMs
        val endMs = words.getOrNull(index + 1)?.startTimeMs
            ?: parsedLine.lineEndTimeMs.coerceAtLeast(startMs + 1L)
        current.add(
            MetroWordSlot(
                layout = layout,
                widthPx = layout.size.width.toFloat(),
                startMs = startMs,
                endMs = endMs,
            ),
        )
        x += layout.size.width + spacePx
    }
    if (current.isNotEmpty()) rows.add(MetroRow(y = y, words = current))
    val heightDp = with(density) { ((y + lineHeightPx) / density.density).dp }
    return MetroLayout(rows = rows, heightDp = heightDp, spacePx = spacePx)
}
