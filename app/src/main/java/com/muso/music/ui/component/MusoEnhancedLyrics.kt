/*
 * Muso bridge for ArchiveTune's ENHANCED lyrics renderer.
 *
 * ArchiveTune's `LyricsMode.ENHANCED` renders `ui/component/LyricsEnhanced.kt`. That file is
 * vendored here verbatim (only the R import and the lifecycle-aware collector differ from
 * upstream), but it never ran: its only caller is ArchiveTune's own player screen, which has
 * no caller in this app, and it reads `moe.rukamori.archivetune.LocalPlayerConnection`, which
 * the app provides as null because ArchiveTune's `PlayerConnection` constructor needs
 * ArchiveTune's own `MusicBinder` from a `MusicService` that is not registered here.
 *
 * This file closes that gap: it builds an ArchiveTune `PlayerConnection` around Muso's live
 * player (the bridge constructor on that class), and feeds the renderer the lyrics THIS app
 * already fetched.
 *
 * It deliberately does NOT use ArchiveTune's `LyricsRenderViewModel`. That ViewModel runs
 * `PrepareLyricsUseCase`, which reads `database.lyrics(mediaId)` - ArchiveTune's OWN lyrics
 * table, which is empty in this app, because Muso stores lyrics in its own database. So the
 * ViewModel could only ever emit `Loading`, which is exactly the "spinner forever" the
 * Enhanced style used to show. The suite already hands the parsed lyrics to `LyricsView`, so
 * they are mapped straight into the renderer's own model here instead.
 */

package com.muso.music.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.common.collect.ImmutableList
import com.maxrave.domain.data.model.metadata.Line
import com.maxrave.domain.data.model.metadata.Lyrics
import com.maxrave.simpmusic.viewModel.NowPlayingScreenData
import com.muso.music.LocalPlayerConnection
import com.muso.music.constants.LyricsOffsetKey
import com.muso.music.constants.LyricsTextSizeKey
import com.muso.music.utils.rememberPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import moe.rukamori.archivetune.KitRuntimeAccess
import moe.rukamori.archivetune.LocalAnimationsDisabled
import moe.rukamori.archivetune.LocalPlayerConnection as ArchiveTuneLocalPlayerConnection
import moe.rukamori.archivetune.lyrics.LyricsLineAlignment
import moe.rukamori.archivetune.lyrics.LyricsRenderingPreferences
import moe.rukamori.archivetune.lyrics.LyricsRomanizationPreferences
import moe.rukamori.archivetune.lyrics.LyricsSourceFormat
import moe.rukamori.archivetune.lyrics.LyricsSyncType
import moe.rukamori.archivetune.lyrics.LyricsTextDirection
import moe.rukamori.archivetune.lyrics.PreparedLyrics
import moe.rukamori.archivetune.lyrics.PreparedLyricsLine
import moe.rukamori.archivetune.lyrics.PreparedLyricsTrack
import moe.rukamori.archivetune.lyrics.PreparedLyricsWord
import moe.rukamori.archivetune.models.MediaMetadata as ArchiveTuneMediaMetadata
import moe.rukamori.archivetune.playback.PlayerConnection as ArchiveTunePlayerConnection
import moe.rukamori.archivetune.ui.component.LyricsEnhanced
import moe.rukamori.archivetune.viewmodels.LyricsRenderScreenState

/**
 * Renders ArchiveTune's ENHANCED lyrics sheet against Muso's live playback.
 *
 * Drop-in replacement for the suite's lyrics view when the user has selected the
 * **Enhanced** lyrics animation style.
 */
@Composable
fun MusoEnhancedLyrics(
    lyricsData: NowPlayingScreenData.LyricsData?,
    modifier: Modifier = Modifier,
    textColor: Color? = null,
    sliderPositionProvider: () -> Long? = { null },
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val lyricsOffset by rememberPreference(LyricsOffsetKey, defaultValue = 0)
    val lyricsTextSize by rememberPreference(LyricsTextSizeKey, defaultValue = 26)

    // The suite's lyrics, mapped into the renderer's model. Absent lyrics mean the fetch is
    // still running (Loading) or concluded without any (Empty) - the same two states
    // ArchiveTune's own ViewModel produces, just driven by this app's data.
    val lyricsState: LyricsRenderScreenState =
        remember(lyricsData, lyricsTextSize) {
            val lyrics = lyricsData?.lyrics
            val lines = lyrics?.lines
            when {
                lyrics == null -> LyricsRenderScreenState.Loading
                lines.isNullOrEmpty() -> LyricsRenderScreenState.Empty
                else -> LyricsRenderScreenState.Success(lyrics.toPreparedLyrics(lyricsTextSize.toFloat()))
            }
        }

    // ArchiveTune's renderer reads `mediaMetadata` for the song id and the artwork, so the
    // bridge keeps an ArchiveTune-shaped metadata flow in step with Muso's.
    val archiveTuneMetadata = remember {
        MutableStateFlow<ArchiveTuneMediaMetadata?>(null)
    }
    LaunchedEffect(mediaMetadata?.id, mediaMetadata?.title, mediaMetadata?.thumbnailUrl) {
        val metadata = mediaMetadata ?: return@LaunchedEffect
        archiveTuneMetadata.value =
            ArchiveTuneMediaMetadata(
                id = metadata.id,
                title = metadata.title,
                artists =
                    metadata.artists.map {
                        ArchiveTuneMediaMetadata.Artist(id = it.id, name = it.name)
                    },
                duration = metadata.duration,
                thumbnailUrl = metadata.thumbnailUrl,
                explicit = metadata.explicit,
            )
    }

    val bridgeScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
    // Built once per player. The database is only touched for LOCAL media ids, which the
    // streaming path never produces; a failure here degrades to "no Enhanced renderer"
    // rather than taking the player down.
    val archiveTuneConnection = remember(playerConnection) {
        runCatching {
            ArchiveTunePlayerConnection(
                context = context,
                player = playerConnection.player,
                mediaMetadata = archiveTuneMetadata,
                database = KitRuntimeAccess.database(),
                scope = bridgeScope,
            )
        }.getOrNull()
    }

    CompositionLocalProvider(
        ArchiveTuneLocalPlayerConnection provides archiveTuneConnection,
        LocalAnimationsDisabled provides false,
    ) {
        LyricsEnhanced(
            lyricsState = lyricsState,
            sliderPositionProvider = sliderPositionProvider,
            lyricsSyncOffset = lyricsOffset,
            modifier = modifier,
            textColorOverride = textColor,
        )
    }
}

/**
 * Maps the suite's parsed lyrics into ArchiveTune's render model.
 *
 * Word-synced when any line carries syllables, otherwise line-synced; `PLAIN` (no highlight
 * at all) when the source says the lyrics are unsynced, which is what the suite's own
 * renderer keys off too.
 */
private fun Lyrics.toPreparedLyrics(textSizeSp: Float): PreparedLyrics {
    val sourceLines = lines.orEmpty()
    val unsynced = syncType?.contains("UNSYNCED", ignoreCase = true) == true
    val wordSynced = !unsynced && sourceLines.any { !it.syllables.isNullOrEmpty() }

    val prepared =
        sourceLines.mapIndexed { index, line ->
            val startMs = line.startTimeMs.toLongOrNull() ?: -1L
            val endMs = line.endTimeMs.toLongOrNull() ?: -1L
            val words = line.toPreparedWords(startMs, endMs)
            PreparedLyricsLine(
                id = "muso:$index",
                startMs = startMs,
                endMs = endMs,
                text = line.words,
                alignment = LyricsLineAlignment.CENTER,
                direction = LyricsTextDirection.LTR,
                main = PreparedLyricsTrack(line.words, null, ImmutableList.copyOf(words)),
                backgrounds = ImmutableList.of(),
                translation = null,
                romanizedText = null,
                phonetics = ImmutableList.of(),
                isInstrumental = line.words.isBlank(),
            )
        }

    return PreparedLyrics(
        sourceFormat = LyricsSourceFormat.LRC,
        syncType =
            when {
                unsynced -> LyricsSyncType.PLAIN
                wordSynced -> LyricsSyncType.WORD
                else -> LyricsSyncType.LINE
            },
        lines = ImmutableList.copyOf(prepared),
        preferences =
            LyricsRenderingPreferences(
                clickEnabled = true,
                scrollEnabled = true,
                textSizeSp = textSizeSp,
                lineSpacing = 1.3f,
                lineBlurEnabled = true,
                v2BounceFactor = 1f,
                v2GlowFactor = 1f,
                v2FillTransitionWidthDp = 8f,
                v2LrcBounceEnabled = true,
                // Muso has its own romanizer; the renderer's is left off so a line is not
                // romanized twice.
                romanization =
                    LyricsRomanizationPreferences(
                        romanizeJapanese = false,
                        romanizeKorean = false,
                        romanizeChinese = false,
                        romanizeHindi = false,
                        romanizeOther = false,
                    ),
            ),
    )
}

/**
 * The suite gives a line either its syllables (word-level timing) or one plain string. With
 * syllables the line's span is divided evenly between them; without, the whole line is a
 * single word, which is what the line-synced renderer highlights as one block.
 */
private fun Line.toPreparedWords(startMs: Long, endMs: Long): List<PreparedLyricsWord> {
    val syllables = syllables.orEmpty().filter { it.isNotEmpty() }
    if (syllables.isEmpty()) return listOf(PreparedLyricsWord(words, startMs, endMs))

    val span = (endMs - startMs).coerceAtLeast(syllables.size.toLong())
    val per = span / syllables.size
    return syllables.mapIndexed { index, syllable ->
        PreparedLyricsWord(
            text = syllable,
            startMs = startMs + per * index,
            endMs = if (index == syllables.lastIndex) endMs else startMs + per * (index + 1),
        )
    }
}
