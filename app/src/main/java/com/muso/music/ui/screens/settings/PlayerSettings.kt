package com.muso.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.muso.music.LocalPlayerAwareWindowInsets
import com.muso.music.R
import com.muso.music.constants.AudioNormalizationKey
import com.muso.music.constants.AudioQuality
import com.muso.music.constants.AudioQualityKey
import com.muso.music.constants.AutoLoadMoreKey
import com.muso.music.constants.AutoSkipNextOnErrorKey
import com.muso.music.constants.PersistentQueueKey
import com.muso.music.constants.CrossfadeDurationKey
import com.muso.music.constants.CrossfadeEnabledKey
import com.muso.music.constants.PlayerBackgroundStyle
import com.muso.music.constants.PlayerStyleKey
import com.muso.music.constants.PlayerStyle
import com.muso.music.constants.PlayerBackgroundStyleKey
import com.muso.music.constants.HighQualityVideoKey
import com.muso.music.constants.KeepScreenOnKey
import com.muso.music.constants.LyricsAutoScrollKey
import com.muso.music.constants.LyricsRomanizationKey
import com.muso.music.constants.AnimatedArtworkKey
import com.muso.music.constants.LyricsBlurEnabledKey
import com.muso.music.constants.LyricsTextSizeKey
import com.muso.music.constants.LyricsStyle
import com.muso.music.constants.LyricsStyleKey
import com.muso.music.constants.ShowVideoInPlayerKey
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (audioQuality, onAudioQualityChange) = rememberEnumPreference(AudioQualityKey, defaultValue = AudioQuality.AUTO)
    val (persistentQueue, onPersistentQueueChange) = rememberPreference(PersistentQueueKey, defaultValue = true)
    val (skipSilence, onSkipSilenceChange) = rememberPreference(SkipSilenceKey, defaultValue = false)
    val (audioNormalization, onAudioNormalizationChange) = rememberPreference(AudioNormalizationKey, defaultValue = true)
    val (showVideoInPlayer, onShowVideoInPlayerChange) = rememberPreference(ShowVideoInPlayerKey, defaultValue = true)
    val (highQualityVideo, onHighQualityVideoChange) = rememberPreference(HighQualityVideoKey, defaultValue = true)
    val (lyricsStyle, onLyricsStyleChange) = rememberEnumPreference(LyricsStyleKey, defaultValue = LyricsStyle.APPLE_MUSIC)
    val (lyricsTextSize, onLyricsTextSizeChange) = rememberPreference(LyricsTextSizeKey, 26)
    val (lyricsBlurEnabled, onLyricsBlurEnabledChange) = rememberPreference(LyricsBlurEnabledKey, true)
    val (lyricsAutoScroll, onLyricsAutoScrollChange) = rememberPreference(LyricsAutoScrollKey, true)
    val (romanizeLyrics, onRomanizeLyricsChange) = rememberPreference(LyricsRomanizationKey, defaultValue = false)
    val (animatedArtwork, onAnimatedArtworkChange) = rememberPreference(AnimatedArtworkKey, defaultValue = false)
    val (keepScreenOn, onKeepScreenOnChange) = rememberPreference(KeepScreenOnKey, false)
    val (autoLoadMore, onAutoLoadMoreChange) = rememberPreference(AutoLoadMoreKey, defaultValue = true)
    val (autoSkipNextOnError, onAutoSkipNextOnErrorChange) = rememberPreference(AutoSkipNextOnErrorKey, defaultValue = false)
    val (stopMusicOnTaskClear, onStopMusicOnTaskClearChange) = rememberPreference(StopMusicOnTaskClearKey, defaultValue = false)
    val (crossfadeEnabled, onCrossfadeEnabledChange) = rememberPreference(CrossfadeEnabledKey, defaultValue = false)
    val (crossfadeDuration, onCrossfadeDurationChange) = rememberPreference(CrossfadeDurationKey, defaultValue = 4)
    val (playerBackgroundStyle, onPlayerBackgroundStyleChange) = rememberEnumPreference(PlayerBackgroundStyleKey, defaultValue = PlayerBackgroundStyle.DEFAULT)

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))

        PreferenceGroupTitle(
            title = stringResource(R.string.player)
        )

        val (playerStyle, onPlayerStyleChange) = rememberEnumPreference(PlayerStyleKey, PlayerStyle.CLASSIC)
        EnumListPreference(
            title = { Text(stringResource(R.string.player_style)) },
            icon = { Icon(painterResource(R.drawable.play), null) },
            selectedValue = playerStyle,
            onValueSelected = onPlayerStyleChange,
            valueText = {
                when (it) {
                    PlayerStyle.CLASSIC -> stringResource(R.string.player_style_classic)
                    PlayerStyle.EXPRESSIVE -> stringResource(R.string.player_style_expressive)
                    PlayerStyle.IMMERSIVE -> stringResource(R.string.player_style_immersive)
                    PlayerStyle.APPLE -> stringResource(R.string.player_style_apple)
                }
            }
        )

        EnumListPreference(
            title = { Text(stringResource(R.string.audio_quality)) },
            icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
            selectedValue = audioQuality,
            onValueSelected = onAudioQualityChange,
            valueText = {
                when (it) {
                    AudioQuality.AUTO -> stringResource(R.string.audio_quality_auto)
                    AudioQuality.HIGH -> stringResource(R.string.audio_quality_high)
                    AudioQuality.LOW -> stringResource(R.string.audio_quality_low)
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

        SwitchPreference(
            title = { Text(stringResource(R.string.show_video_in_player)) },
            description = stringResource(R.string.show_video_in_player_desc),
            icon = { Icon(painterResource(R.drawable.music_note), null) },
            checked = showVideoInPlayer,
            onCheckedChange = onShowVideoInPlayerChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.high_quality_video)) },
            description = stringResource(R.string.high_quality_video_desc),
            icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
            checked = highQualityVideo,
            onCheckedChange = onHighQualityVideoChange
        )

        EnumListPreference(
            title = { Text(stringResource(R.string.lyrics_style)) },
            icon = { Icon(painterResource(R.drawable.format_align_center), null) },
            selectedValue = lyricsStyle,
            onValueSelected = onLyricsStyleChange,
            valueText = {
                when (it) {
                    LyricsStyle.APPLE_MUSIC -> stringResource(R.string.lyrics_style_apple_music)
                    LyricsStyle.CLASSIC -> stringResource(R.string.lyrics_style_classic)
                }
            }
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.lyrics_text_size)) },
            icon = { Icon(painterResource(R.drawable.format_align_center), null) },
            content = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Slider(
                        value = lyricsTextSize.toFloat(),
                        onValueChange = { onLyricsTextSizeChange(it.toInt()) },
                        valueRange = 16f..36f,
                        steps = 19,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.lyrics_blur)) },
            description = stringResource(R.string.lyrics_blur_desc),
            icon = { Icon(painterResource(R.drawable.palette), null) },
            checked = lyricsBlurEnabled,
            onCheckedChange = onLyricsBlurEnabledChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.lyrics_auto_scroll)) },
            description = stringResource(R.string.lyrics_auto_scroll_desc),
            icon = { Icon(painterResource(R.drawable.sync), null) },
            checked = lyricsAutoScroll,
            onCheckedChange = onLyricsAutoScrollChange
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.romanize_lyrics)) },
            description = stringResource(R.string.romanize_lyrics_desc),
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = romanizeLyrics,
            onCheckedChange = onRomanizeLyricsChange
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

        PreferenceGroupTitle(title = stringResource(R.string.player_background))
        EnumListPreference(
            title = { Text(stringResource(R.string.player_background)) },
            icon = { Icon(painterResource(R.drawable.palette), null) },
            selectedValue = playerBackgroundStyle,
            onValueSelected = onPlayerBackgroundStyleChange,
            valueText = {
                when (it) {
                    PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.player_background_default)
                    PlayerBackgroundStyle.BLURRED_ARTWORK -> stringResource(R.string.player_background_blurred)
                }
            },
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.player_and_audio)) },
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
