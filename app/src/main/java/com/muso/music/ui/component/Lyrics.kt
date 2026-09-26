package com.muso.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muso.music.BuildConfig
import com.muso.music.LocalPlayerConnection
import com.muso.music.R
import com.muso.music.constants.LyricsRomanizationKey
import com.muso.music.constants.LyricsStyle
import com.muso.music.constants.LyricsAutoScrollKey
import com.muso.music.constants.ReducedMotionKey
import com.muso.music.constants.LyricsBlurEnabledKey
import com.muso.music.constants.LyricsLineSpacingKey
import com.muso.music.constants.LyricsPosition
import com.muso.music.constants.LyricsTextSizeKey
import com.muso.music.constants.TranslateLyricsKey
import com.muso.music.constants.LyricsStyleKey
import com.muso.music.constants.LyricsTextPositionKey
import com.muso.music.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.muso.music.lyrics.LyricsEntry
import com.muso.music.lyrics.LyricsUtils
import com.muso.music.lyrics.LyricsEntry.Companion.HEAD_LYRICS_ENTRY
import com.muso.music.lyrics.LyricsUtils.findCurrentLineIndex
import com.muso.music.lyrics.LyricsUtils.parseLyrics
import com.muso.music.ui.menu.LyricsMenu
import com.muso.music.ui.utils.fadingEdge
import com.muso.music.utils.rememberEnumPreference
import com.muso.music.utils.Romanizer
import com.muso.music.lyrics.LyricsWord
import echo.music.iad1tya.betterlyrics.TTMLParser
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.PI
import kotlin.math.abs
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.min
import kotlin.math.cos
import kotlin.math.sin
import com.muso.music.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.seconds

@Composable
fun Lyrics(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val density = LocalDensity.current

    val lyricsPosition by rememberEnumPreference(LyricsTextPositionKey, LyricsPosition.CENTER)
    val lyricsTextAlign = when (lyricsPosition) {
        LyricsPosition.LEFT -> TextAlign.Start
        LyricsPosition.CENTER -> TextAlign.Center
        LyricsPosition.RIGHT -> TextAlign.End
    }
    val lyricsBoxAlignment = when (lyricsPosition) {
        LyricsPosition.LEFT -> Alignment.CenterStart
        LyricsPosition.CENTER -> Alignment.Center
        LyricsPosition.RIGHT -> Alignment.CenterEnd
    }
    val lyricsLineSpacing by rememberPreference(LyricsLineSpacingKey, 1.3f)
    val lyricsStyle by rememberEnumPreference(LyricsStyleKey, LyricsStyle.APPLE_MUSIC)
    var translationEnabled by rememberPreference(TranslateLyricsKey, false)
    val lyricsTextSize by rememberPreference(LyricsTextSizeKey, 26)
    val lyricsBlurEnabled by rememberPreference(LyricsBlurEnabledKey, true)
    val lyricsAutoScroll by rememberPreference(LyricsAutoScrollKey, true)
    val romanizeLyrics by rememberPreference(LyricsRomanizationKey, false)

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val translating by playerConnection.translating.collectAsState()
    val lyricsEntity by playerConnection.currentLyrics.collectAsState(initial = null)
    val lyrics = remember(lyricsEntity, translating) {
        if (translating) null
        else lyricsEntity?.lyrics
    }

    val isTTML = remember(lyrics) {
        // TTML carries per-word timings (BetterLyrics / SimpMusic lyrics providers).
        !lyrics.isNullOrEmpty() &&
                (lyrics.trimStart().startsWith("<?xml") || lyrics.trimStart().startsWith("<tt"))
    }
    // The parse and the "is this actually synced" verdict are computed together:
    // SimpMusic's sync-type sanity demotes a file whose timestamps are all
    // identical (every row "0") to unsynced - no fake active line, nothing
    // highlighted - and any badly merged LRC (time tags inside the text,
    // metadata rows) is cleaned so ONLY lyric text can ever render.
    val hasLrcTags = remember(lyrics, isTTML) {
        !isTTML && !lyrics.isNullOrEmpty() && lyrics != LYRICS_NOT_FOUND &&
                LyricsUtils.hasTimestampedLines(lyrics)
    }
    val (lines, isSynced) = remember(lyrics, romanizeLyrics, hasLrcTags) {
        fun plain(raw: String): List<LyricsEntry> = LyricsUtils.sanitizeUnsynced(raw)
            .mapIndexed { index, line ->
                LyricsEntry(index * 100L, if (romanizeLyrics) Romanizer.romanize(line) else line)
            }
        when {
            lyrics == null || lyrics == LYRICS_NOT_FOUND -> emptyList<LyricsEntry>() to false
            // TTML karaoke: line entries with per-word start/end times. Romanizing would
            // break the word timing, so it falls back to plain romanized line text.
            isTTML && !romanizeLyrics -> (listOf(HEAD_LYRICS_ENTRY) + TTMLParser.parseTTML(lyrics).map { line ->
                LyricsEntry(
                    time = (line.startTime * 1000).toLong(),
                    text = line.text,
                    words = line.words.map { w ->
                        LyricsWord(
                            text = w.text,
                            startMs = (w.startTime * 1000).toLong(),
                            endMs = (w.endTime * 1000).toLong(),
                        )
                    },
                )
            }) to true
            isTTML -> (listOf(HEAD_LYRICS_ENTRY) + TTMLParser.parseTTML(lyrics).map { line ->
                LyricsEntry((line.startTime * 1000).toLong(), Romanizer.romanize(line.text))
            }) to true
            hasLrcTags -> {
                val parsed = parseLyrics(lyrics)
                if (parsed.size > 1 && parsed.map { it.time }.distinct().size > 1) {
                    val withHead = listOf(HEAD_LYRICS_ENTRY) + parsed
                    (if (romanizeLyrics) withHead.map { LyricsEntry(it.time, Romanizer.romanize(it.text)) } else withHead) to true
                } else {
                    // Every timestamp identical - the file is unsynced text.
                    plain(lyrics) to false
                }
            }
            else -> plain(lyrics) to false
        }
    }

    var currentLineIndex by remember {
        mutableIntStateOf(-1)
    }
    // Playback position for the word-by-word karaoke fill.
    var playbackPosition by remember { mutableLongStateOf(0L) }
    // Because LaunchedEffect has delay, which leads to inconsistent with current line color and scroll animation,
    // we use deferredCurrentLineIndex when user is scrolling
    var deferredCurrentLineIndex by rememberSaveable {
        mutableIntStateOf(0)
    }

    var lastPreviewTime by rememberSaveable {
        mutableLongStateOf(0L)
    }
    var isSeeking by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(lyrics) {
        if (lyrics.isNullOrEmpty() || (!isSynced && !isTTML)) {
            currentLineIndex = -1
            return@LaunchedEffect
        }
        while (isActive) {
            delay(50)
            val sliderPosition = sliderPositionProvider()
            isSeeking = sliderPosition != null
            playbackPosition = sliderPosition ?: playerConnection.player.currentPosition
            currentLineIndex = findCurrentLineIndex(lines, playbackPosition)
        }
    }

    LaunchedEffect(isSeeking, lastPreviewTime) {
        if (isSeeking) {
            lastPreviewTime = 0L
        } else if (lastPreviewTime != 0L) {
            delay(LyricsPreviewTime)
            lastPreviewTime = 0L
        }
    }

    val lazyListState = rememberLazyListState()

    LaunchedEffect(currentLineIndex, lastPreviewTime) {
        if (!isSynced || !lyricsAutoScroll) return@LaunchedEffect
        if (currentLineIndex != -1) {
            deferredCurrentLineIndex = currentLineIndex
            if (lastPreviewTime == 0L) {
                if (isSeeking) {
                    lazyListState.scrollToItem(currentLineIndex, with(density) { 36.dp.toPx().toInt() })
                } else {
                    lazyListState.animateScrollToItem(currentLineIndex, with(density) { 36.dp.toPx().toInt() })
                }
            }
        }
    }

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 12.dp)
    ) {
        LazyColumn(
            state = lazyListState,
            verticalArrangement = Arrangement.spacedBy(24.dp * (lyricsLineSpacing - 1f)),
            contentPadding = WindowInsets.systemBars
                .only(WindowInsetsSides.Top)
                .add(WindowInsets(top = maxHeight / 2, bottom = maxHeight / 2))
                .asPaddingValues(),
            modifier = Modifier
                .fadingEdge(vertical = 64.dp)
                .nestedScroll(remember {
                    object : NestedScrollConnection {
                        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                            lastPreviewTime = System.currentTimeMillis()
                            return super.onPostScroll(consumed, available, source)
                        }

                        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                            lastPreviewTime = System.currentTimeMillis()
                            return super.onPostFling(consumed, available)
                        }
                    }
                })
        ) {
            val displayedCurrentLineIndex = if (isSeeking) deferredCurrentLineIndex else currentLineIndex

            if (lyrics == null || translating) {
                item {
                    // SimpMusic-style loading: the animated neon equalizer - no
                    // text skeletons anywhere in the lyrics view.
                    LyricsMorphLoading(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                    )
                }
            } else {
                itemsIndexed(
                    items = lines
                ) { index, item ->
                    val isCurrentLine = index == displayedCurrentLineIndex
                    val hasActiveLine = isSynced && displayedCurrentLineIndex != -1

                    // Karaoke (word-by-word) line: words fill in as they are sung.
                    if (isSynced && item.words.isNotEmpty()) {
                        KaraokeLyricsLine(
                            words = item.words,
                            isCurrentLine = isCurrentLine,
                            isPastLine = hasActiveLine && index < displayedCurrentLineIndex,
                            lineAlpha = when {
                                isCurrentLine || !hasActiveLine -> 1f
                                else -> {
                                    val d = index - displayedCurrentLineIndex
                                    val dist = if (d < 0) -d else d
                                    when (dist) {
                                        1, 2 -> 0.2f
                                        3 -> 0.15f
                                        4 -> 0.1f
                                        else -> 0.08f
                                    }
                                }
                            },
                            // Only the active line and its neighbours need the live
                            // 50 ms karaoke position; distant lines are frozen at their
                            // boundary values (identical visual output), so a position
                            // tick no longer recomposes every visible lyric line.
                            position = if (abs(index - displayedCurrentLineIndex) <= 1) playbackPosition
                            else if (hasActiveLine && index < displayedCurrentLineIndex) Long.MAX_VALUE
                            else 0L,
                            fontSize = lyricsTextSize,
                            accent = Color.White,
                            inactiveColor = Color.White,
                            textAlign = lyricsTextAlign,
                            onTapLine = {
                                playerConnection.player.seekTo(item.time)
                                lastPreviewTime = 0L
                            },
                        )
                        return@itemsIndexed
                    }
                    // Apple Music style: the active line is lit, the rest dim and blur with
                    // distance (depth of field), color animates smoothly on line change.
                    val isAppleMusicStyle = lyricsStyle == LyricsStyle.APPLE_MUSIC
                    val lineColor by animateColorAsState(
                        targetValue = when {
                            isCurrentLine -> Color.White
                            isAppleMusicStyle -> AppleMusicInactiveLineColor
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        },
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                        label = "lyricsLineColor"
                    )
                    Text(
                        text = item.text,
                        fontSize = if (isAppleMusicStyle) lyricsTextSize.sp else (lyricsTextSize - 10).sp,
                        color = lineColor,
                        textAlign = lyricsTextAlign,
                        fontWeight = if (isAppleMusicStyle) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isSynced) {
                                playerConnection.player.seekTo(item.time)
                                lastPreviewTime = 0L
                            }
                            .padding(
                                horizontal = 24.dp,
                                vertical = if (isAppleMusicStyle) 10.dp else 6.dp
                            )
                            .then(
                                if (isAppleMusicStyle) {
                                    Modifier.appleMusicLyricFocus(
                                        distanceFromCurrent = index - displayedCurrentLineIndex,
                                        hasActiveLine = hasActiveLine,
                                        allLinesCurrent = !isSynced,
                                        fontSize = lyricsTextSize.sp,
                                        blurEnabled = lyricsBlurEnabled
                                    )
                                } else {
                                    Modifier
                                }
                            )
                    )
                }
            }
        }

        if (lyrics == LYRICS_NOT_FOUND) {
            Text(
                text = stringResource(R.string.lyrics_not_found),
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = lyricsTextAlign,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .alpha(0.5f)
            )
        }

        // SimpMusic floating lyrics actions: white-24% circles bottom-end.
        mediaMetadata?.let { mediaMetadata ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 16.dp)
            ) {
                if (BuildConfig.FLAVOR != "foss") {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.24f))
                            .clickable { translationEnabled = !translationEnabled },
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.translate),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = if (translationEnabled) 1f else 0.5f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.24f))
                        .clickable {
                            menuState.show {
                                LyricsMenu(
                                    lyricsProvider = { lyricsEntity },
                                    mediaMetadataProvider = { mediaMetadata },
                                    onDismiss = menuState::dismiss
                                )
                            }
                        },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.more_horiz),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

const val animateScrollDuration = 300L
val LyricsPreviewTime = 4.seconds

/**
 * Echo-Music style karaoke line (ported): the line keeps the same Apple Music look -
 * bold primary when active, dim inactive colour otherwise - but every word fills in
 * with a soft left-to-right wipe exactly while it is sung, lifting a touch and
 * glowing as it goes. Lines without word timings render as before.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KaraokeLyricsLine(
    words: List<LyricsWord>,
    isCurrentLine: Boolean,
    isPastLine: Boolean,
    lineAlpha: Float,
    position: Long,
    fontSize: Int,
    accent: Color,
    inactiveColor: Color,
    textAlign: TextAlign,
    onTapLine: () -> Unit,
) {
    // kimi_5.html parity: no line zoom - distance focus is a pure alpha fade
    // (1/2 -> .2, 3 -> .15, 4 -> .1, 5+ -> .08), active line fully opaque.
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(lineAlpha)
            .clickable(onClick = onTapLine)
            .padding(
                horizontal = 24.dp,
                vertical = if (isCurrentLine) 12.dp else 10.dp,
            ),
        horizontalArrangement = when (textAlign) {
            TextAlign.Center -> Arrangement.Center
            TextAlign.Right -> Arrangement.End
            else -> Arrangement.Start
        },
    ) {
        words.forEachIndexed { wordIndex, word ->
            KaraokeWord(
                word = word,
                isLineActive = isCurrentLine,
                isLinePast = isPastLine,
                position = position,
                fontSize = fontSize,
                accent = accent,
                inactiveColor = inactiveColor,
            )
            if (wordIndex < words.lastIndex) {
                Text(text = " ", fontSize = fontSize.sp)
            }
        }
    }
}

@Composable
private fun KaraokeWord(
    word: LyricsWord,
    isLineActive: Boolean,
    isLinePast: Boolean,
    position: Long,
    fontSize: Int,
    accent: Color,
    inactiveColor: Color,
) {
    val isWordComplete = isLinePast || position >= word.endMs
    val isWordActive = isLineActive && position >= word.startMs && position < word.endMs
    val wordDuration = (word.endMs - word.startMs).coerceAtLeast(1L)

    val progress = when {
        isWordComplete -> 1f
        !isLineActive || position <= word.startMs -> 0f
        else -> ((position - word.startMs).toFloat() / wordDuration).coerceIn(0f, 1f)
    }

    // Reduced Motion (Animation settings) keeps the word fill but drops the lift/glow
    // movement, so karaoke stays readable without motion.
    val reducedMotion by rememberPreference(ReducedMotionKey, false)

    val sinProgress = sin(progress * PI).toFloat()
    val wordScale = if (reducedMotion) 1f else 1f + (0.02f * sinProgress)

    val targetFloat = if (reducedMotion) 0f else if (isWordActive) -4f * sinProgress else 0f
    val floatOffset by animateFloatAsState(
        targetValue = targetFloat,
        animationSpec = tween(
            durationMillis = if (isWordActive) 50 else 350,
            easing = FastOutSlowInEasing,
        ),
        label = "WordFloatOffset",
    )

    val glowAlpha = if (isWordActive) (progress * 2f).coerceAtMost(1f) * 0.35f else 0f
    val glowRadius = if (isWordActive) (progress * 2f).coerceAtMost(1f) * 12f else 0f

    val style = MaterialTheme.typography.headlineMedium.copy(
        fontSize = fontSize.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.5).sp,
        shadow = if (glowAlpha > 0f) {
            Shadow(color = accent.copy(alpha = glowAlpha), offset = Offset.Zero, blurRadius = glowRadius.coerceAtLeast(1f))
        } else null,
    )

    Box(
        modifier = Modifier.graphicsLayer {
            translationY = floatOffset.dp.toPx()
            scaleX = wordScale
            scaleY = wordScale
        }
    ) {
        Text(
            text = word.text,
            style = style,
            color = if (isLineActive) inactiveColor.copy(alpha = 0.3f) else inactiveColor,
        )

        // kimi_5.html parity: the accent fill layer renders only inside the singing
        // line; once the line passes, words return to the dim white base.
        if (isLineActive && (isWordComplete || isWordActive)) {
            Text(
                text = word.text,
                style = style,
                color = accent,
                modifier = if (isWordActive) {
                    Modifier
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            val edgeWidth = 8.dp.toPx()
                            val center = (size.width + edgeWidth * 2) * progress - edgeWidth
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.Black, Color.Transparent),
                                    startX = center - edgeWidth,
                                    endX = center + edgeWidth,
                                ),
                                blendMode = BlendMode.DstIn,
                            )
                        }
                } else Modifier,
            )
        }
    }
}


/**
 * SimpMusic / Material-3 expressive lyrics loading: a single morphing shape
 * indicator - the blob continuously morphs between circle and rounded square
 * (corner radius + breathing size out of phase) while it rotates, drawn in the
 * theme's primary color and centered on the lyrics area while they load.
 */
@Composable
private fun LyricsMorphLoading(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "lyricsMorph")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
        ),
        label = "lyricsMorphT",
    )
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.size(64.dp)) {
        val full = min(size.width, size.height)
        // Shape morph: corner radius swings between square-ish and circle.
        val cornerPhase = 0.5f + 0.5f * cos(t * 2f * PI).toFloat()
        // Breathing size, offset half a cycle from the corners.
        val scalePhase = 0.5f + 0.5f * sin(t * 2f * PI).toFloat()
        val side = full * (0.66f + 0.22f * scalePhase)
        val half = side / 2f
        val corner = half * (0.18f + 0.82f * cornerPhase)
        rotate(degrees = t * 180f, pivot = center) {
            drawRoundRect(
                color = color,
                topLeft = Offset(center.x - half, center.y - half),
                size = Size(side, side),
                cornerRadius = CornerRadius(corner, corner),
            )
        }
    }
}
) {
    val transition = rememberInfiniteTransition(label = "lyricsMorph")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
        ),
        label = "lyricsMorphT",
    )
    Canvas(modifier = modifier.height(120.dp)) {
        val barCount = 5
        val gap = 12.dp.toPx()
        val barWidth = 16.dp.toPx()
        val totalWidth = barCount * barWidth + (barCount - 1) * gap
        var x = (size.width - totalWidth) / 2f
        val centerY = size.height / 2f
        // Symmetric heights around the tall centre bar (SimpMusic reference).
        val shape = floatArrayOf(0.34f, 0.62f, 1f, 0.62f, 0.34f)
        val neon = Brush.horizontalGradient(
            listOf(Color(0xFF22D3EE), Color(0xFF818CF8), Color(0xFFE879F9)),
        )
        for (i in 0 until barCount) {
            val wave = 0.74f + 0.26f * sin(t + i * 0.85f)
            val h = (size.height * shape[i] * wave).coerceAtLeast(barWidth)
            val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
            // Soft outer glow (bloom): the same bar drawn larger, very low alpha.
            val grow = 5.dp.toPx()
            drawRoundRect(
                brush = neon,
                topLeft = Offset(x - grow / 2f, centerY - h / 2f - grow / 2f),
                size = Size(barWidth + grow, h + grow),
                cornerRadius = CornerRadius(barWidth / 2f + grow / 2f),
                alpha = 0.22f,
            )
            // The bar itself.
            drawRoundRect(
                brush = neon,
                topLeft = Offset(x, centerY - h / 2f),
                size = Size(barWidth, h),
                cornerRadius = radius,
            )
            // Glossy sheen: a white fade over the top third of the bar.
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.45f),
                        Color.Transparent,
                    ),
                    startY = centerY - h / 2f,
                    endY = centerY - h / 6f,
                ),
                topLeft = Offset(x, centerY - h / 2f),
                size = Size(barWidth, h),
                cornerRadius = radius,
                alpha = 0.5f,
            )
            x += barWidth + gap
        }
    }
}
