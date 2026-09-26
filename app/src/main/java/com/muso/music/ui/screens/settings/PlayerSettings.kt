package com.muso.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.navigation.NavController
import com.muso.music.LocalPlayerAwareWindowInsets
import com.muso.music.constants.ReducedMotionKey
import com.muso.music.constants.GestureAnimationsKey
import com.muso.music.constants.AnimationsEnabledKey
import com.muso.music.R
import com.muso.music.constants.AudioNormalizationKey
import com.muso.music.constants.AutoLoadMoreKey
import com.muso.music.constants.AutoSkipNextOnErrorKey
import com.muso.music.constants.PersistentQueueKey
import com.muso.music.constants.PreloadLyricsKey
import com.muso.music.constants.PreloadNextSongKey
import com.muso.music.constants.AutomixKey
import com.muso.music.constants.SpatialAudioKey
import com.muso.music.ui.component.ListPreference
import com.muso.music.constants.LoudnessPreset
import com.muso.music.constants.LoudnessPresetKey
import com.muso.music.constants.HistoryDurationKey
import com.muso.music.constants.SeekExtraSecondsKey
import com.muso.music.constants.PauseOnMuteKey
import com.muso.music.constants.PreventDuplicateTracksKey
import com.muso.music.constants.AudioOffloadKey
import com.muso.music.constants.DownloadOnWifiOnlyKey
import com.muso.music.constants.DataSaverKey
import com.muso.music.constants.CrossfadeDurationKey
import com.muso.music.constants.CrossfadeEnabledKey
import com.muso.music.constants.KeepScreenOnKey
import com.muso.music.constants.AnimatedArtworkKey
import com.muso.music.constants.PlayerTextAlignmentKey

import com.muso.music.constants.SkipSilenceKey
import com.muso.music.constants.StopMusicOnTaskClearKey
import com.muso.music.ui.component.EnumListPreference
import com.muso.music.ui.component.PreferenceEntry
import com.muso.music.ui.component.IconButton
import com.muso.music.ui.component.PreferenceGroupTitle
import com.muso.music.ui.component.SwitchPreference
import com.muso.music.ui.utils.backToMain
import com.muso.music.utils.rememberEnumPreference
import com.muso.music.utils.rememberPreference
import androidx.compose.foundation.layout.height

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (persistentQueue, onPersistentQueueChange) = rememberPreference(PersistentQueueKey, defaultValue = true)
    val (skipSilence, onSkipSilenceChange) = rememberPreference(SkipSilenceKey, defaultValue = false)
    val (audioNormalization, onAudioNormalizationChange) = rememberPreference(AudioNormalizationKey, defaultValue = true)
    val (animatedArtwork, onAnimatedArtworkChange) = rememberPreference(AnimatedArtworkKey, defaultValue = false)
    val (keepScreenOn, onKeepScreenOnChange) = rememberPreference(KeepScreenOnKey, false)
    val (autoLoadMore, onAutoLoadMoreChange) = rememberPreference(AutoLoadMoreKey, defaultValue = true)
    val (autoSkipNextOnError, onAutoSkipNextOnErrorChange) = rememberPreference(AutoSkipNextOnErrorKey, defaultValue = false)
    val (stopMusicOnTaskClear, onStopMusicOnTaskClearChange) = rememberPreference(StopMusicOnTaskClearKey, defaultValue = false)
    val (crossfadeEnabled, onCrossfadeEnabledChange) = rememberPreference(CrossfadeEnabledKey, defaultValue = false)
    val (crossfadeDuration, onCrossfadeDurationChange) = rememberPreference(CrossfadeDurationKey, defaultValue = 4)
    val (playerTextAlignment, onPlayerTextAlignmentChange) = rememberEnumPreference(PlayerTextAlignmentKey, defaultValue = PlayerTextAlignment.CENTER)
    val (animationsEnabled, onAnimationsEnabledChange) = rememberPreference(AnimationsEnabledKey, defaultValue = true)
    val (gestureAnimations, onGestureAnimationsChange) = rememberPreference(GestureAnimationsKey, defaultValue = true)
    val (reducedMotion, onReducedMotionChange) = rememberPreference(ReducedMotionKey, defaultValue = false)
    val (dataSaver, onDataSaverChange) = rememberPreference(DataSaverKey, defaultValue = false)
    val (downloadOnWifiOnly, onDownloadOnWifiOnlyChange) = rememberPreference(DownloadOnWifiOnlyKey, defaultValue = false)
    val (audioOffload, onAudioOffloadChange) = rememberPreference(AudioOffloadKey, defaultValue = false)
    val (preventDuplicateTracks, onPreventDuplicateTracksChange) = rememberPreference(PreventDuplicateTracksKey, defaultValue = false)
    val (pauseOnMute, onPauseOnMuteChange) = rememberPreference(PauseOnMuteKey, defaultValue = false)
    val (seekExtraSeconds, onSeekExtraSecondsChange) = rememberPreference(SeekExtraSecondsKey, defaultValue = false)
    val (historyDuration, onHistoryDurationChange) = rememberPreference(HistoryDurationKey, defaultValue = 0)
    val (loudnessPreset, onLoudnessPresetChange) = rememberEnumPreference(LoudnessPresetKey, defaultValue = LoudnessPreset.NORMAL)
    val (spatialAudio, onSpatialAudioChange) = rememberPreference(SpatialAudioKey, defaultValue = false)
    val (automix, onAutomixChange) = rememberPreference(AutomixKey, defaultValue = false)
    val (preloadNextSong, onPreloadNextSongChange) = rememberPreference(PreloadNextSongKey, defaultValue = false)
    val (preloadLyrics, onPreloadLyricsChange) = rememberPreference(PreloadLyricsKey, defaultValue = false)

    val scrollState = rememberScrollState()

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(scrollState)
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))
        Spacer(Modifier.height(64.dp))

        PreferenceGroupTitle(
            title = stringResource(R.string.player)
        )

        EnumListPreference(
            title = { Text(stringResource(R.string.player_text_alignment)) },
            icon = {
                Icon(
                    painter = painterResource(
                        when (playerTextAlignment) {
                            PlayerTextAlignment.CENTER -> R.drawable.format_align_center
                            PlayerTextAlignment.SIDED -> R.drawable.format_align_left
                        }
                    ),
                    contentDescription = null
                )
            },
            selectedValue = playerTextAlignment,
            onValueSelected = onPlayerTextAlignmentChange,
            valueText = {
                when (it) {
                    PlayerTextAlignment.SIDED -> stringResource(R.string.sided)
                    PlayerTextAlignment.CENTER -> stringResource(R.string.center)
                }
            }
        )


        SwitchPreference(
            title = { Text(stringResource(R.string.skip_silence)) },
            icon = { Icon(painterResource(R.drawable.fast_forward), null) },
            checked = skipSilence,
            onCheckedChange = onSkipSilenceChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.audio_normalization)) },
            icon = { Icon(painterResource(R.drawable.volume_up), null) },
            checked = audioNormalization,
            onCheckedChange = onAudioNormalizationChange
        )

        EnumListPreference(
            title = { Text(stringResource(R.string.audio_loudness_preset)) },
            icon = { Icon(painterResource(R.drawable.volume_up), null) },
            selectedValue = loudnessPreset,
            onValueSelected = onLoudnessPresetChange,
            valueText = {
                when (it) {
                    LoudnessPreset.OFF -> stringResource(R.string.loudness_off)
                    LoudnessPreset.NORMAL -> stringResource(R.string.loudness_normal)
                    LoudnessPreset.STRONG -> stringResource(R.string.loudness_strong)
                }
            },
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.data_saver)) },
            description = stringResource(R.string.data_saver_desc),
            icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
            checked = dataSaver,
            onCheckedChange = onDataSaverChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.seek_seconds_addup)) },
            description = stringResource(R.string.seek_seconds_addup_desc),
            icon = { Icon(painterResource(R.drawable.fast_forward), null) },
            checked = seekExtraSeconds,
            onCheckedChange = onSeekExtraSecondsChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.spatial_audio)) },
            description = stringResource(R.string.spatial_audio_desc),
            icon = { Icon(painterResource(R.drawable.surround_sound), null) },
            checked = spatialAudio,
            onCheckedChange = onSpatialAudioChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.automix)) },
            description = stringResource(R.string.automix_desc),
            icon = { Icon(painterResource(R.drawable.playlist_play), null) },
            checked = automix,
            onCheckedChange = onAutomixChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.preload_next_song)) },
            description = stringResource(R.string.preload_next_song_desc),
            icon = { Icon(painterResource(R.drawable.fast_forward), null) },
            checked = preloadNextSong,
            onCheckedChange = onPreloadNextSongChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.preload_lyrics)) },
            description = stringResource(R.string.preload_lyrics_desc),
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = preloadLyrics,
            onCheckedChange = onPreloadLyricsChange
        )



        SwitchPreference(
            title = { Text(stringResource(R.string.animated_artwork)) },
            description = stringResource(R.string.animated_artwork_desc),
            icon = { Icon(painterResource(R.drawable.palette), null) },
            checked = animatedArtwork,
            onCheckedChange = onAnimatedArtworkChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.keep_screen_on)) },
            description = stringResource(R.string.keep_screen_on_desc),
            icon = { Icon(painterResource(R.drawable.lock), null) },
            checked = keepScreenOn,
            onCheckedChange = onKeepScreenOnChange
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.queue)
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.persistent_queue)) },
            description = stringResource(R.string.persistent_queue_desc),
            icon = { Icon(painterResource(R.drawable.queue_music), null) },
            checked = persistentQueue,
            onCheckedChange = onPersistentQueueChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.auto_load_more)) },
            description = stringResource(R.string.auto_load_more_desc),
            icon = { Icon(painterResource(R.drawable.playlist_add), null) },
            checked = autoLoadMore,
            onCheckedChange = onAutoLoadMoreChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.prevent_duplicate_tracks)) },
            description = stringResource(R.string.prevent_duplicate_tracks_desc),
            icon = { Icon(painterResource(R.drawable.queue_music), null) },
            checked = preventDuplicateTracks,
            onCheckedChange = onPreventDuplicateTracksChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.auto_skip_next_on_error)) },
            description = stringResource(R.string.auto_skip_next_on_error_desc),
            icon = { Icon(painterResource(R.drawable.skip_next), null) },
            checked = autoSkipNextOnError,
            onCheckedChange = onAutoSkipNextOnErrorChange
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.misc)
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.stop_music_on_task_clear)) },
            icon = { Icon(painterResource(R.drawable.clear_all), null) },
            checked = stopMusicOnTaskClear,
            onCheckedChange = onStopMusicOnTaskClearChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.pause_music_when_media_is_muted)) },
            icon = { Icon(painterResource(R.drawable.volume_up), null) },
            checked = pauseOnMute,
            onCheckedChange = onPauseOnMuteChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.download_on_wifi_only)) },
            icon = { Icon(painterResource(R.drawable.download), null) },
            checked = downloadOnWifiOnly,
            onCheckedChange = onDownloadOnWifiOnlyChange
        )

        ListPreference(
            title = { Text(stringResource(R.string.history_duration)) },
            icon = { Icon(painterResource(R.drawable.delete_history), null) },
            selectedValue = historyDuration.toString(),
            values = listOf("0", "12", "24", "168", "720"),
            valueText = {
                when (it) {
                    "12" -> stringResource(R.string.hours_12)
                    "24" -> stringResource(R.string.days_1)
                    "168" -> stringResource(R.string.days_7)
                    "720" -> stringResource(R.string.days_30)
                    else -> stringResource(R.string.history_keep_forever)
                }
            },
            onValueSelected = { onHistoryDurationChange(it.toInt()) },
        )



        PreferenceGroupTitle(
            title = stringResource(R.string.animation)
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.animations)) },
            description = stringResource(R.string.animations_desc),
            icon = { Icon(painterResource(R.drawable.tune), null) },
            checked = animationsEnabled,
            onCheckedChange = onAnimationsEnabledChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.gesture_animations)) },
            description = stringResource(R.string.gesture_animations_desc),
            icon = { Icon(painterResource(R.drawable.swipe), null) },
            checked = gestureAnimations,
            onCheckedChange = onGestureAnimationsChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.reduced_motion)) },
            description = stringResource(R.string.reduced_motion_desc),
            icon = { Icon(painterResource(R.drawable.discover_tune), null) },
            checked = reducedMotion,
            onCheckedChange = onReducedMotionChange
        )

        PreferenceGroupTitle(title = stringResource(R.string.crossfade))
        SwitchPreference(
            title = { Text(stringResource(R.string.crossfade)) },
            description = stringResource(R.string.crossfade_desc),
            icon = { Icon(painterResource(R.drawable.playlist_play), null) },
            checked = crossfadeEnabled,
            onCheckedChange = onCrossfadeEnabledChange
        )
        if (crossfadeEnabled) {
            PreferenceEntry(
                title = { Text(stringResource(R.string.crossfade_duration)) },
                description = "${crossfadeDuration}s",
                icon = { Icon(painterResource(R.drawable.playlist_play), null) },
                content = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Slider(
                            value = crossfadeDuration.toFloat(),
                            onValueChange = { onCrossfadeDurationChange(it.toInt()) },
                            valueRange = 1f..12f,
                            steps = 10,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
            )
        }

        SwitchPreference(
            title = { Text(stringResource(R.string.audio_offload)) },
            description = stringResource(R.string.audio_offload_desc),
            icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
            checked = audioOffload,
            onCheckedChange = onAudioOffloadChange,
            isEnabled = !crossfadeEnabled,
        )

    }

    TopAppBar(
        title = {
            // Echo-style collapse: the big in-content title hands over to the top bar while scrolling.
                            Text(stringResource(R.string.player_and_audio))

        },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}
