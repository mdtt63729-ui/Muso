/*
 * Muso bridge for ArchiveTune's ENHANCED lyrics renderer.
 *
 * ArchiveTune's `LyricsMode.ENHANCED` renders `ui/component/LyricsEnhanced.kt`. That file
 * is vendored in this app verbatim (only the R import and the lifecycle-aware collector
 * differ from upstream), but it never ran: its only caller is ArchiveTune's own player
 * screen, which has no caller here, and it reads `moe.rukamori.archivetune.LocalPlayerConnection`,
 * which the app provides as null because ArchiveTune's `PlayerConnection` constructor needs
 * ArchiveTune's own `MusicBinder` from a `MusicService` that is not registered.
 *
 * This file closes that gap: it builds an ArchiveTune `PlayerConnection` around Muso's live
 * player (the bridge constructor added to that class), converts the metadata, binds the
 * `LyricsRenderViewModel` to the current song, and renders `LyricsEnhanced` with the two
 * locals it needs. Everything else in the app is untouched.
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muso.music.LocalPlayerConnection
import com.muso.music.constants.LyricsOffsetKey
import com.muso.music.utils.rememberPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import moe.rukamori.archivetune.KitRuntimeAccess
import moe.rukamori.archivetune.LocalAnimationsDisabled
import moe.rukamori.archivetune.LocalPlayerConnection as ArchiveTuneLocalPlayerConnection
import moe.rukamori.archivetune.models.MediaMetadata as ArchiveTuneMediaMetadata
import moe.rukamori.archivetune.playback.PlayerConnection as ArchiveTunePlayerConnection
import moe.rukamori.archivetune.ui.component.LyricsEnhanced
import moe.rukamori.archivetune.viewmodels.LyricsRenderViewModel

/**
 * Renders ArchiveTune's ENHANCED lyrics sheet against Muso's live playback.
 *
 * Drop-in replacement for the suite's lyrics view when the user has selected the
 * **Enhanced** lyrics animation style.
 */
@Composable
fun MusoEnhancedLyrics(
    modifier: Modifier = Modifier,
    textColor: Color? = null,
    sliderPositionProvider: () -> Long? = { null },
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val lyricsOffset by rememberPreference(LyricsOffsetKey, defaultValue = 0)
    val renderViewModel: LyricsRenderViewModel = hiltViewModel()
    val lyricsState by renderViewModel.state.collectAsStateWithLifecycle()

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

    // Drive ArchiveTune's own lyrics pipeline: it observes the song in the ArchiveTune
    // database and prepares the render model the sheet consumes.
    LaunchedEffect(mediaMetadata?.id, playerConnection) {
        val mediaId = mediaMetadata?.id ?: return@LaunchedEffect
        renderViewModel.bind(
            mediaId = mediaId,
            durationMs = playerConnection.player.duration.coerceAtLeast(0L),
        )
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
