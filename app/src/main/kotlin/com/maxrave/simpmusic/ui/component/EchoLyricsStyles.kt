package com.maxrave.simpmusic.ui.component
import androidx.compose.ui.text.withStyle

/*
 * Unified lyrics animation renderer. All six selectable styles consume the
 * same resolved line/word/character timeline, so changing a style never changes
 * timing or provider-specific rendering paths.
 *   FADE:     wordAlpha = 0.35 + 0.65 * smoothstep(progress)
 *   KARAOKE:  per-character karaoke fill as the word is sung
 * Line-only and untimed lyrics receive a deterministic fallback window from LyricsView.
 */

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.withTransform
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
import com.maxrave.simpmusic.ui.screen.player.content.stripRichSyncTimestamps
import com.maxrave.simpmusic.ui.theme.typo
import com.muso.music.constants.LyricsAnimationStyle

/** Ink of a word that has not been sung yet. */
private val EchoPendingWordColor = Color(0xFF9E9E9E)
/** Ink of a word that is being sung or has been sung. */
private val EchoSungWordColor = Color.White
/** The glow behind the active word in Lyrics V2 / V2 Mode / Enhanced. */
private val EchoGlowColor = Color(0xAAFFFFFF)

private fun smoothstep(p: Float): Float = p * p * (3f - 2f * p)

/** The player publishes its position every 50 ms; the sweep must move per frame. */
private const val ECHO_PLAYHEAD_TICK_MS = 300L

/**
 * HTML-parity timing (user report: the word sweeps felt laggy and steppy).
 * The kimi reference's sync loop runs off requestAnimationFrame reading
 * audio.currentTime, so its word fill advances once per DISPLAY FRAME; a
 * player that ticks every 50 ms gives a fill that jumps 20 times a second.
 * This carries the ticked position forward between ticks so the playhead
 * advances once per frame, corrected to the truth on every real tick.
 */
@Composable
private fun rememberEchoPlayhead(rawMs: Long, playerOffsetMs: Long, enabled: Boolean): State<Long> {
    val playhead = remember { mutableLongStateOf(rawMs) }
    // Round 194: same live-player playhead as the flare renderer - the frame-
    // exact position while a player is playing, the ticked value + wall-clock
    // interpolation otherwise (see LyricsView.rememberSmoothPlayhead).
    val playerConnection = com.muso.music.LocalPlayerConnectionOrNull.current
    val latestRawMs by rememberUpdatedState(rawMs)
    LaunchedEffect(enabled, playerConnection) {
        if (!enabled) {
            playhead.longValue = latestRawMs
            return@LaunchedEffect
        }
        var baseRawMs = latestRawMs
        var baseNanos = 0L
        while (true) {
            val frameNanos = withFrameNanos { it }
            val player = playerConnection?.player
            if (player != null && player.isPlaying) {
                // IMPORTANT: rawMs is already in lyric/heard time (lyrics offset applied).
                // Do not switch back to uncorrected player time here or the animation and the
                // selected line will disagree by exactly the user configured lyrics offset.
                playhead.longValue = (player.currentPosition - playerOffsetMs).coerceAtLeast(0L)
                baseRawMs = playhead.longValue
                baseNanos = frameNanos
            } else {
                val latest = latestRawMs
                if (baseNanos == 0L || latest != baseRawMs) {
                    baseRawMs = latest
                    baseNanos = frameNanos
                }
                val elapsedMs = ((frameNanos - baseNanos) / 1_000_000L)
                    .coerceIn(0L, ECHO_PLAYHEAD_TICK_MS)
                playhead.longValue = baseRawMs + elapsedMs
            }
        }
    }
    return playhead
}

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
    /** Listener/audio correction already applied to currentTimeMs by LyricsView. */
    playerOffsetMs: Long = 0L,
    isCurrent: Boolean,
    style: LyricsAnimationStyle,
    modifier: Modifier = Modifier,
) {
    // IMPORTANT: do not build one Text composable per word/character here. That approach made the
    // current line recompose dozens of text nodes every frame. The renderer below measures once
    // and then draws the complete line from one Canvas; only the Canvas is invalidated by the
    // frame-smooth playhead. This is the same architecture as a requestAnimationFrame renderer.
    val playhead = rememberEchoPlayhead(currentTimeMs, playerOffsetMs, enabled = isCurrent)
    val v2Bounce by com.muso.music.utils.rememberPreference(
        moe.rukamori.archivetune.constants.LyricsV2BounceFactorKey, 1f,
    )
    val v2Glow by com.muso.music.utils.rememberPreference(
        moe.rukamori.archivetune.constants.LyricsV2GlowFactorKey, 1f,
    )
    val v2FillWidth by com.muso.music.utils.rememberPreference(
        moe.rukamori.archivetune.constants.LyricsV2FillTransitionWidthKey, 8f,
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(12.dp))
        EchoCanvasLine(
            parsedLine = parsedLine,
            playhead = playhead,
            isCurrent = isCurrent,
            style = style,
            v2Bounce = v2Bounce,
            v2Glow = v2Glow,
            v2FillWidth = v2FillWidth,
            modifier = Modifier.fillMaxWidth(),
        )
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
private fun EchoCanvasLine(
    parsedLine: ParsedRichSyncLine,
    playhead: State<Long>,
    isCurrent: Boolean,
    style: LyricsAnimationStyle,
    v2Bounce: Float,
    v2Glow: Float,
    v2FillWidth: Float,
    modifier: Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val textStyle = if (isCurrent) typo().headlineLarge else typo().headlineMedium
    val maxWidthPx = with(density) { 360.dp.toPx() }
    val layout = remember(parsedLine, isCurrent, textStyle, maxWidthPx) {
        measureMetroWords(textMeasurer, parsedLine, textStyle, maxWidthPx, density)
    }

    Canvas(
        modifier = modifier.height(layout.heightDp),
    ) {
        val t = playhead.value
        layout.rows.forEach { row ->
            var x = 0f
            row.words.forEach { slot ->
                val wordProgress =
                    ((t - slot.startMs).toFloat() / (slot.endMs - slot.startMs).coerceAtLeast(1L))
                        .coerceIn(0f, 1f)
                val wordActive = isCurrent && t >= slot.startMs && t < slot.endMs
                val wordComplete = t >= slot.endMs
                val pulse = sin(wordProgress * kotlin.math.PI).toFloat().coerceIn(0f, 1f)

                slot.characters.forEachIndexed { charIndex, character ->
                    // Use the character's own timed slice instead of dividing the word into
                    // equal buckets. Equal buckets look acceptable on a demo lyric, but they
                    // drift badly on sung words where the vowel/consonant durations differ.
                    // The timing model is precomputed once per word, so this remains a draw-only
                    // operation while playback is running.
                    val charProgress =
                        ((t - character.startMs).toFloat() /
                            (character.endMs - character.startMs).coerceAtLeast(1L))
                            .coerceIn(0f, 1f)
                    val eased = smoothstep(charProgress)
                    var color = if (isCurrent) EchoSungWordColor else EchoPendingWordColor
                    var alpha = if (isCurrent) 1f else 0.35f
                    var translationY = 0f
                    var scale = 1f

                    when (style) {
                        LyricsAnimationStyle.NONE -> {
                            color = if (isCurrent && (wordComplete || wordActive)) EchoSungWordColor else EchoPendingWordColor
                        }
                        LyricsAnimationStyle.FADE -> {
                            alpha = if (isCurrent) 0.35f + 0.65f * eased else 0.35f
                        }
                        LyricsAnimationStyle.KARAOKE -> Unit
                        LyricsAnimationStyle.LYRICS_V2 -> {
                            val rise = if (wordActive || wordComplete) 1f else 0f
                            alpha = if (isCurrent) 0.30f + 0.70f * rise else 0.30f
                            translationY = (1f - rise) * 6f
                            if (wordActive) scale = 1f + 0.06f * pulse * v2Bounce.coerceIn(0f, 2f)
                        }
                        LyricsAnimationStyle.V2_MODE -> {
                            translationY = if (wordActive) -4f * v2Bounce.coerceIn(0f, 2f) * pulse else 0f
                            scale = 1f + 0.015f * v2Bounce.coerceIn(0f, 2f) * pulse
                            if (wordActive) {
                                alpha *= 0.92f + 0.08f * (eased * v2Glow.coerceIn(0f, 2f))
                            }
                        }
                        LyricsAnimationStyle.ENHANCED -> {
                            alpha = if (isCurrent) 0.40f + 0.60f * eased else 0.28f
                            translationY = if (wordActive) (1f - eased) * 2.5f else 0f
                            scale = 0.985f + 0.015f * eased
                            if (wordActive) alpha *= 0.94f + 0.06f * eased
                        }
                    }

                    // v2FillWidth intentionally stays a timing preference. Its old implementation
                    // changed layout/measurement while the song was playing; keeping it out of the
                    // layout pass removes that source of frame spikes while preserving the setting.
                    val fill = if (style == LyricsAnimationStyle.KARAOKE) eased else eased
                    val drawColor = lerpColor(EchoPendingWordColor, color, fill).copy(alpha = alpha)
                    val charX = x + slot.characters.take(charIndex).sumOf { it.widthPx.toDouble() }.toFloat()
                    withTransform({
                        translate(left = charX, top = row.y + translationY)
                        scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
                    }) {
                        drawText(
                            textLayoutResult = character.layout,
                            color = drawColor,
                            topLeft = Offset.Zero,
                        )
                    }
                }
                x += slot.widthPx + layout.spacePx
            }
        }
    }
}

/**
 * One word, deriving its progress/active/complete state from the
 * frame-smooth playhead. derivedStateOf means the line body above never
 * recomposes at frame rate: a word whose derived values did not change
 * (everything except the word being sung) is skipped entirely.
 */
@Composable
private fun EchoAnimatedWord(
    text: String,
    startMs: Long,
    endMs: Long,
    duration: Long,
    isCurrent: Boolean,
    style: LyricsAnimationStyle,
    isLast: Boolean,
    playhead: State<Long>,
    v2Bounce: Float,
    v2Glow: Float,
    v2FillWidth: Float,
) {
    val progress by remember(startMs, endMs, isCurrent) {
        derivedStateOf {
            ((playhead.value - startMs).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        }
    }
    val isWordActive by remember(startMs, endMs, isCurrent) {
        derivedStateOf { isCurrent && playhead.value >= startMs && playhead.value < endMs }
    }
    val isWordComplete by remember(endMs, isCurrent) {
        derivedStateOf { playhead.value >= endMs }
    }

    EchoWord(
        text = text,
        progress = progress,
        isLineCurrent = isCurrent,
        isWordActive = isWordActive,
        isWordComplete = isWordComplete,
        style = style,
        isLast = isLast,
    )
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
    // All six styles use the same frame-synced timing. The style only changes
    // how the already-timed characters look and move.
    val pulse = sin(progress * kotlin.math.PI).toFloat().coerceIn(0f, 1f)

    var color = if (isLineCurrent) EchoSungWordColor else EchoPendingWordColor
    var alpha = if (isLineCurrent) 1f else 0.35f
    var renderedStyle = baseStyle
    var translationY = 0f
    var scale = 1f

    when (style) {
        LyricsAnimationStyle.NONE -> {
            color = if (isLineCurrent && (isWordComplete || isWordActive)) {
                EchoSungWordColor
            } else {
                EchoPendingWordColor
            }
            alpha = if (isLineCurrent) 1f else 0.35f
        }

        LyricsAnimationStyle.FADE -> {
            color = if (isLineCurrent) EchoSungWordColor else EchoPendingWordColor
            alpha = if (isLineCurrent) {
                0.35f + 0.65f * smoothstep(progress)
            } else {
                0.35f
            }
        }

        LyricsAnimationStyle.KARAOKE -> {
            // LetterSyncedWord below performs the actual per-character fill.
            color = if (isLineCurrent) EchoSungWordColor else EchoPendingWordColor
            alpha = if (isLineCurrent) 1f else 0.35f
        }

        LyricsAnimationStyle.LYRICS_V2 -> {
            val rise = if (isWordActive || isWordComplete) 1f else 0f
            color = if (isLineCurrent) EchoSungWordColor else EchoPendingWordColor
            alpha = if (isLineCurrent) 0.30f + 0.70f * rise else 0.30f
            translationY = (1f - rise) * 6f
            if (isWordActive) {
                scale = 1f + 0.06f * rise
            }
        }

        LyricsAnimationStyle.V2_MODE -> {
            // Ported from ArchiveTune LyricsV2's AnimatedWordV2:
            // linear word progress, sine bounce/float and transient glow.
            color = if (isLineCurrent) EchoSungWordColor else EchoPendingWordColor
            alpha = if (isLineCurrent) 1f else 0.30f
            translationY = if (isWordActive) -4f * 1f * pulse else 0f
            scale = 1f + 0.015f * 1f * pulse
            if (isWordActive) {
                val glowProgress = (progress * 2f).coerceAtMost(1f)
                // Keep the timing effect but avoid a per-frame text-shadow blur pass.
                alpha = alpha * (0.92f + 0.08f * glowProgress * 1f)
            }
        }

        LyricsAnimationStyle.ENHANCED -> {
            // Enhanced keeps ArchiveTune's smooth focus treatment but uses the
            // same character clock so it remains genuinely letter-synced.
            color = if (isLineCurrent) EchoSungWordColor else EchoPendingWordColor
            val focus = smoothstep(progress)
            alpha = if (isLineCurrent) 0.40f + 0.60f * focus else 0.28f
            translationY = if (isWordActive) (1f - focus) * 2.5f else 0f
            scale = 0.985f + 0.015f * focus
            if (isWordActive) {
                alpha = alpha * (0.94f + 0.06f * focus)
            }
        }
    }

    // One Row + one frame clock for the entire word. No per-letter coroutine.
    LetterSyncedWord(
        text = word,
        style = renderedStyle,
        progress = if (isLineCurrent) progress else 0f,
        baseColor = color,
        baseAlpha = alpha,
        translationY = translationY,
        scale = scale,
        overflow = TextOverflow.Visible,
    )
}

/**
 * Frame-synced character renderer shared by every selectable animation style.
 *
 * The word timing is the outer clock; each visible grapheme receives an equal
 * slice of that word's interval. This gives true letter-by-letter progression
 * even when the provider only exposes word timing. No per-letter coroutine or
 * animation clock is created, so the entire line follows the player's clock.
 */
@Composable
private fun LetterSyncedWord(
    text: String,
    style: TextStyle,
    progress: Float,
    baseColor: Color,
    baseAlpha: Float,
    translationY: Float,
    scale: Float,
    overflow: TextOverflow,
) {
    val coreEnd = text.indexOfLast { !it.isWhitespace() } + 1
    val coreText = if (coreEnd > 0) text.substring(0, coreEnd) else text
    val trailingWhitespace = if (coreEnd < text.length) text.substring(coreEnd) else ""
    val ranges = remember(coreText) {
        val iterator = java.text.BreakIterator.getCharacterInstance(java.util.Locale.getDefault())
        iterator.setText(coreText)
        buildList {
            var start = iterator.first()
            while (start != java.text.BreakIterator.DONE) {
                val end = iterator.next()
                if (end == java.text.BreakIterator.DONE) break
                if (end > start) add(start until end)
                start = end
            }
        }
    }
    val safeProgress = progress.coerceIn(0f, 1f)
    val characterCount = ranges.size.coerceAtLeast(1)
    val annotated = androidx.compose.ui.text.buildAnnotatedString {
        ranges.forEachIndexed { index, range ->
            val characterProgress = ((safeProgress * characterCount) - index).coerceIn(0f, 1f)
            val eased = smoothstep(characterProgress)
            val glyphColor = lerpColor(EchoPendingWordColor, baseColor, eased)
            withStyle(
                androidx.compose.ui.text.SpanStyle(
                    color = glyphColor.copy(alpha = baseAlpha),
                ),
            ) {
                append(coreText.substring(range))
            }
        }
        append(trailingWhitespace)
    }

    Text(
        text = annotated,
        style = style,
        overflow = overflow,
        softWrap = false,
        modifier = Modifier.graphicsLayer {
            this.translationY = translationY
            this.scaleX = scale
            this.scaleY = scale
        },
    )
}

/**
 * Builds a line-level timing model when a provider gives only line timestamps.
 * Character cadence is then calculated inside LetterSyncedWord, so the same renderer
 * is shared by every animation style.
 */
fun synthesizeCharacterTimedLine(
    text: String,
    startMs: Long,
    endMs: Long,
): ParsedRichSyncLine {
    val safeStart = startMs.coerceAtLeast(0L)
    val safeEnd = endMs.coerceAtLeast(safeStart + 1L)
    return ParsedRichSyncLine(
        words = listOf(
            com.maxrave.simpmusic.extension.WordTiming(
                text = text,
                startTimeMs = safeStart,
                endTimeMs = safeEnd,
            ),
        ),
        lineStartTimeMs = safeStart,
        lineEndTimeMs = safeEnd,
    )
}

@Composable
private fun MetroEchoLine(
    parsedLine: ParsedRichSyncLine,
    playhead: State<Long>,
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
                // Draw-phase state read: the Canvas redraws per frame with no
                // recomposition, exactly like the reference's rAF render.
                val t = playhead.value
                val progress = ((t - slot.startMs).toFloat() / (slot.endMs - slot.startMs).coerceAtLeast(1L))
                    .coerceIn(0f, 1f)
                var charX = x
                slot.characters.forEachIndexed { charIndex, character ->
                    val letterCount = slot.characters.size.coerceAtLeast(1)
                    val letterProgress = ((progress * letterCount) - charIndex).coerceIn(0f, 1f)
                    val paint = if (!isCurrent) {
                        EchoPendingWordColor
                    } else {
                        lerpColor(
                            EchoPendingWordColor,
                            EchoSungWordColor,
                            smoothstep(letterProgress),
                        )
                    }
                    drawText(
                        textLayoutResult = character.layout,
                        color = paint,
                        topLeft = Offset(charX, row.y),
                    )
                    charX += character.widthPx
                }
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

private data class MetroCharacterSlot(
    val layout: TextLayoutResult,
    val widthPx: Float,
    val startMs: Long,
    val endMs: Long,
)

private data class MetroWordSlot(
    val characters: List<MetroCharacterSlot>,
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

private fun characterTimingWeight(character: String): Float {
    val cp = character.codePointAt(0)
    if (Character.isWhitespace(cp)) return 0.12f
    if (Character.getType(cp) == Character.NON_SPACING_MARK || Character.getType(cp) == Character.COMBINING_SPACING_MARK) return 0.08f
    if (Character.isDigit(cp)) return 0.80f
    if (Character.isLetter(cp)) {
        val c = character.lowercase()
        // Give sustained vowel sounds a little more temporal room. This is only a fallback when
        // the lyric provider has word timing but no phoneme timing; it never pretends to be audio
        // forced alignment.
        if (c in setOf("a", "e", "i", "o", "u", "ā", "ē", "ī", "ō", "ū", "অ", "আ", "ই", "ঈ", "উ", "ঊ", "এ", "ঐ", "ও", "ঔ")) {
            return 1.18f
        }
        return 0.92f
    }
    return 0.22f
}

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
        // Word spacing is supplied by layout.spacePx, so the separator is not
        // treated as a timed character.
        val text = word.text
        val characterParts = buildList {
            var charIndex = 0
            while (charIndex < text.length) {
                val codePoint = text.codePointAt(charIndex)
                val charCount = Character.charCount(codePoint)
                val character = text.substring(charIndex, (charIndex + charCount).coerceAtMost(text.length))
                val characterLayout = textMeasurer.measure(
                    text = character,
                    style = style,
                    maxLines = 1,
                )
                add(character to characterLayout)
                charIndex += charCount
            }
        }
        val wordStartMs = word.startTimeMs
        val wordEndMs = (word.endTimeMs ?: parsedLine.lineEndTimeMs)
            .coerceAtLeast(wordStartMs + 1L)
        val totalWordDuration = (wordEndMs - wordStartMs).coerceAtLeast(1L)
        // A letter does not consume equal amounts of singing time. Vowels, syllabic nuclei and
        // CJK/Bengali-style full glyphs tend to occupy more of a sung word; punctuation and
        // combining marks should not steal a full letter's slice. This is a deterministic fallback
        // for providers that expose word timing but not phoneme/character timing. When the source
        // contains true character timing, that source timing should be preferred upstream.
        val weights = characterParts.map { (character, _) -> characterTimingWeight(character) }
        val weightTotal = weights.sum().coerceAtLeast(0.001f)
        var cursorMs = wordStartMs
        val characters = characterParts.mapIndexed { index, (character, characterLayout) ->
            val isLast = index == characterParts.lastIndex
            val endMs = if (isLast) {
                wordEndMs
            } else {
                cursorMs + (totalWordDuration * (weights[index] / weightTotal)).toLong()
            }.coerceAtLeast(cursorMs + 1L).coerceAtMost(wordEndMs)
            val slot = MetroCharacterSlot(
                layout = characterLayout,
                widthPx = characterLayout.size.width.toFloat(),
                startMs = cursorMs,
                endMs = endMs,
            )
            cursorMs = endMs
            slot
        }
        val wordWidth = characters.sumOf { it.widthPx.toDouble() }.toFloat()
        if (x > 0f && x + wordWidth > maxWidthPx) {
            rows.add(MetroRow(y = y, words = current))
            current = mutableListOf()
            x = 0f
            y += lineHeightPx
        }
        val startMs = word.startTimeMs
        val endMs = word.endTimeMs
            ?: words.getOrNull(index + 1)?.startTimeMs
            ?: parsedLine.lineEndTimeMs.coerceAtLeast(startMs + 1L)
        current.add(
            MetroWordSlot(
                characters = characters,
                widthPx = wordWidth,
                startMs = startMs,
                endMs = endMs,
            ),
        )
        x += wordWidth + spacePx
    }
    if (current.isNotEmpty()) rows.add(MetroRow(y = y, words = current))
    val heightDp = with(density) { ((y + lineHeightPx) / density.density).dp }
    return MetroLayout(rows = rows, heightDp = heightDp, spacePx = spacePx)
}


/**
 * Legacy helper retained for compatibility with older call sites; the current picker does not expose this style.
 * The Apple Music V2 word-by-word lyric line, rendered OVER a playing video
 * (user spec): while a video track plays, the lyric overlay on the screen uses
 * the SAME animation the app's lyrics view uses - not a plain subtitle.
 * Shows only the CURRENT line, so it stays a caption, not a lyrics sheet.
 */
@Composable
fun VideoEchoLyricsOverlay(
    lyrics: com.maxrave.domain.data.model.metadata.Lyrics?,
    currentMs: Long,
    modifier: Modifier = Modifier,
) {
    if (lyrics == null) return
    val lines = lyrics.lines.orEmpty()
    if (lines.isEmpty()) return

    val lyricsOffsetMs by org.koin.compose.koinInject<com.maxrave.domain.manager.DataStoreManager>()
        .lyricsOffsetMs.collectAsState(0)
    val selectedStyle by com.muso.music.utils.rememberEnumPreference(
        key = com.muso.music.constants.LyricsAnimationStyleKey,
        defaultValue = LyricsAnimationStyle.LYRICS_V2,
    )
    val wordByWordEnabled by com.muso.music.utils.rememberPreference(
        com.muso.music.constants.WordByWordLyricsEnabledKey,
        true,
    )
    val effectiveStyle = if (wordByWordEnabled) selectedStyle else LyricsAnimationStyle.NONE
    val player = com.muso.music.LocalPlayerConnectionOrNull.current?.player
    val durationMs = player?.duration?.takeIf { it > 0L } ?: (currentMs + 1_000L)
    val nowMs = (currentMs - lyricsOffsetMs).coerceAtLeast(0L)

    val windows = remember(lines, durationMs) {
        buildResolvedLyricWindows(lines, durationMs)
    }
    val lineIndex = windows.indexOfLast { nowMs >= it.first }
    if (lineIndex < 0) return
    val line = lines[lineIndex]
    val window = windows.getOrNull(lineIndex) ?: return

    val parsed = remember(line) {
        if (lyrics.syncType == "RICH_SYNCED") {
            com.maxrave.simpmusic.extension.parseRichSyncWords(
                line.words,
                line.startTimeMs,
                line.endTimeMs,
            )
        } else {
            null
        }
    }

    if (effectiveStyle == LyricsAnimationStyle.NONE) {
        Text(
            text = line.words.stripRichSyncTimestamps(),
            color = Color.White,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.75f),
                    blurRadius = 8f,
                ),
            ),
            modifier = modifier,
        )
        return
    }

    EchoLyricsLine(
        parsedLine = parsed ?: synthesizeCharacterTimedLine(
            line.words.stripRichSyncTimestamps(),
            window.first,
            window.second,
        ),
        translatedWords = null,
        romanizedWords = null,
        currentTimeMs = nowMs,
        isCurrent = true,
        style = effectiveStyle,
        modifier = modifier,
    )
}
