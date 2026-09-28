package com.muso.music.ui.player

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.muso.music.LocalDatabase
import com.muso.music.LocalPlayerConnection
import com.muso.music.R
import com.muso.music.constants.DarkModeKey
import com.muso.music.constants.MiniPlayerHeight
import com.muso.music.constants.NavigationBarHeight
import com.muso.music.constants.KeepScreenOnKey
import com.muso.music.constants.PlayerStyle
import com.muso.music.constants.PlayerStyleKey
import com.muso.music.constants.PureBlackKey
import com.muso.music.db.entities.FormatEntity
import com.muso.music.models.MediaMetadata
import com.muso.music.ui.component.BottomSheet
import com.muso.music.ui.component.BottomSheetState
import com.muso.music.ui.component.LocalMenuState
import com.muso.music.ui.menu.AddToPlaylistDialog
import com.muso.music.ui.menu.SongMenu
import com.muso.music.ui.screens.settings.DarkMode
import com.muso.music.utils.makeTimeString
import com.muso.music.utils.rememberEnumPreference
import com.muso.music.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * MUSO PLAYER — SimpMusic suite only.
 *
 * All three now-playing styles (Classic / Expressive / Immersive) render the
 * real SimpMusic player suite through [MusoSuiteHost]; Muso's hand-built
 * native players were removed. This sheet only keeps the shared shell
 * (mini-player, background, dialogs) and the codec readout the suite
 * displays in its info pill.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
    /** False when the suite's glass navigation bar renders its own MiniPlayer. */
    showCollapsedMiniPlayer: Boolean = true,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val useBlackBackground = remember(isSystemInDarkTheme, darkTheme, pureBlack) {
        val useDarkTheme = if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
        useDarkTheme && pureBlack
    }
    val backgroundColor = if (useBlackBackground && state.value > state.collapsedBound) {
        lerp(MaterialTheme.colorScheme.surfaceContainer, Color.Black, state.progress)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    val keepScreenOn by rememberPreference(KeepScreenOnKey, defaultValue = false)

    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)

    var position by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.currentPosition)
    }
    var duration by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.duration)
    }

    LaunchedEffect(playbackState) {
        if (playbackState == Player.STATE_READY) {
            while (isActive) {
                delay(100)
                position = playerConnection.player.currentPosition
                duration = playerConnection.player.duration
            }
        }
    }

    BottomSheet(
        state = state,
        modifier = modifier,
        // Glass navbar mode: no Muso mini player rides here (the suite glass
        // bar draws its own), so the collapsed sheet must not paint the solid
        // plate the old mini player sat on - it would box over the glass bar.
        // Only while COLLAPSED, though: the sheet goes opaque again the moment
        // it leaves the collapsed anchor, or the expanded player would be see
        // -through - the feed showed through it and dragging it flickered.
        // The sheet plate stays transparent through the whole collapsed zone:
        // anything below 25% expansion progress shows NOTHING (the expanded
        // content is alpha-0 there anyway), so a re-anchored or
        // between-insets frame can never flash the grey plate under the
        // glass bar - that was the "box below the navbar/player".
        // The plate fades in on the SAME curve as the expanded content
        // ((progress - 0.25) * 4). Sharing the curve, no frame ever shows a
        // bare plate box riding down with the collapsing player - it used to
        // turn fully opaque at 25% while the content was still half-faded.
        // With the strip mini player (navbar-hidden screens like Settings)
        // the plate IS the strip's surface, so it stays opaque there.
        backgroundColor = if (showCollapsedMiniPlayer) backgroundColor
        else backgroundColor.copy(
            alpha = backgroundColor.alpha * ((state.progress - 0.25f) * 4f).coerceIn(0f, 1f),
        ),
        collapsedHitHeight = if (showCollapsedMiniPlayer) null
        // Glass mode: ONLY the mini player card itself is interactive. The
        // old full-width strip (collapsedBound - NavigationBarHeight) also
        // covered the 24dp above the card, so taps on list content scrolling
        // behind the glass opened the player instead of the screen - felt
        // like the player opened "automatically".
        else MiniPlayerHeight,
        // Reference behaviour: dismissing (swiping away) the mini player is
        // purely visual - playback keeps running and the sheet returns when
        // the connection or queue changes. The old stop()+clearMediaItems()
        // here is what froze songs mid-navigation with dead controls.
        onDismiss = { },
        collapsedContent = {
            if (showCollapsedMiniPlayer) {
                MiniPlayer(
                    position = position,
                    duration = duration,
                )
            } else {
                // The suite glass bar draws its own MiniPlayer above the bar.
                androidx.compose.foundation.layout.Spacer(Modifier)
            }
        },
    ) {
        val playerStyle by rememberEnumPreference(PlayerStyleKey, PlayerStyle.EXPRESSIVE)

        // === Real audio codec detection (Echo Music port): the player's currently
        // selected audio track, observed through onTracksChanged. Nothing is faked
        // when unavailable - the pill simply stays empty and the UI reads clean.
        var currentAudioFormat by remember { mutableStateOf<Format?>(null) }
        DisposableEffect(playerConnection.player) {
            val playerToListen = playerConnection.player
            val listener = object : Player.Listener {
                override fun onTracksChanged(tracks: Tracks) {
                    currentAudioFormat = tracks.groups.firstOrNull { it.type == C.TRACK_TYPE_AUDIO }
                        ?.getTrackFormat(0)
                }
            }
            playerToListen.addListener(listener)
            currentAudioFormat = playerToListen.currentTracks.groups
                .firstOrNull { it.type == C.TRACK_TYPE_AUDIO }
                ?.getTrackFormat(0)
            onDispose { playerToListen.removeListener(listener) }
        }

        var showSongInfoDialog by rememberSaveable { mutableStateOf(false) }
        var showAddToPlaylistDialog by rememberSaveable { mutableStateOf(false) }
        val database = LocalDatabase.current
        // The stored format (written on first play) is the reliable source of the
        // stream's real bitrate - the media3 track format often carries none for
        // progressive streams, which is why the pill used to show no kbps.
        val dbFormat by database.format(mediaMetadata?.id).collectAsState(initial = null)
        val codecLabel = remember(currentAudioFormat, dbFormat) {
            formatAudioInfo(currentAudioFormat, dbFormat)
        }

        // Keep-screen-on while the sheet is expanded, plus the immersive
        // system-bar behaviour shared with the old native player.
        val immersiveView = LocalView.current
        LaunchedEffect(state.isExpanded, keepScreenOn) {
            val window = (immersiveView.context as? Activity)?.window ?: return@LaunchedEffect
            if (keepScreenOn && state.isExpanded) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            val insetsController = WindowCompat.getInsetsController(window, immersiveView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
        DisposableEffect(immersiveView) {
            onDispose {
                val window = (immersiveView.context as? Activity)?.window ?: return@onDispose
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                WindowCompat.getInsetsController(window, immersiveView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }

        // === THE REAL SIMPMUSIC PLAYER (reference NowPlayingScreen) =============
        // The reference's own player screen, ported byte-for-byte: it builds
        // its state from the shared view models (fed by MusoSuiteBridge in
        // the navbar host), does its own palette extraction and renders
        // edge-to-edge inside its own full-black modal sheet - true
        // fullscreen, exactly like the reference app. Muso's player sheet
        // below it stays collapsed around this content.
        com.maxrave.simpmusic.ui.screen.player.NowPlayingScreen(
            navController = navController as NavHostController,
            onDismiss = { state.collapseSoft() },
        )

        // Dialogs (now rendered for the suite path too - previously they only
        // existed inside the removed native layouts).
        mediaMetadata?.let { metadata ->
            if (showSongInfoDialog) {
                SongInfoDialog(
                    mediaMetadata = metadata,
                    onDismiss = { showSongInfoDialog = false },
                )
            }
            AddToPlaylistDialog(
                isVisible = showAddToPlaylistDialog,
                onGetSong = {
                    database.transaction {
                        insert(metadata)
                    }
                    listOf(metadata.id)
                },
                onDismiss = { showAddToPlaylistDialog = false },
            )
        }
    }
}

private fun formatAudioInfo(format: Format?, dbFormat: FormatEntity? = null): String {
    val codecName = when (format?.sampleMimeType) {
        "audio/mp4a-latm", "audio/mp4a" -> "AAC"
        "audio/opus" -> "OPUS"
        "audio/mpeg" -> "MP3"
        "audio/flac" -> "FLAC"
        "audio/alac" -> "ALAC"
        "audio/vorbis" -> "VORBIS"
        "audio/raw", "audio/l16", "audio/pcm" -> "PCM"
        else -> format?.sampleMimeType?.substringAfter("audio/")?.uppercase()
            // fall back to the stored codecs string ("mp4a.40.2", "opus"...)
            ?: dbFormat?.codecs?.substringBefore(".")?.uppercase()
    } ?: return ""
    val bitrate = format?.takeIf { it.bitrate > 0 }?.bitrate
        ?: dbFormat?.takeIf { it.bitrate > 0 }?.bitrate
    val parts = mutableListOf(codecName)
    if (bitrate != null && bitrate > 0) {
        parts += "${bitrate / 1000} kbps"
    }
    return parts.joinToString(" \u2022 ")
}

@Composable
fun SongInfoDialog(
    mediaMetadata: MediaMetadata,
    onDismiss: () -> Unit,
) {
    val database = LocalDatabase.current
    var format by remember { mutableStateOf<FormatEntity?>(null) }
    LaunchedEffect(mediaMetadata.id) {
        database.format(mediaMetadata.id).collect { format = it }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.details)) },
        text = {
            Column {
                SongInfoRow("Title", mediaMetadata.title)
                SongInfoRow("Artist", mediaMetadata.artists.joinToString { it.name })
                SongInfoRow("Album", mediaMetadata.album?.title)
                SongInfoRow("Duration", makeTimeString(mediaMetadata.duration.toLong()))
                SongInfoRow("Codec", format?.codecs)
                SongInfoRow("MIME", format?.mimeType)
                SongInfoRow("Bitrate", format?.bitrate?.let { "${it / 1000} kbps" })
                SongInfoRow("Sample rate", format?.sampleRate?.let { "$it Hz" })
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

/** One label/value line of the Details dialog. */
@Composable
private fun SongInfoRow(label: String, value: String?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp),
        )
        Text(
            text = value ?: "-",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}
