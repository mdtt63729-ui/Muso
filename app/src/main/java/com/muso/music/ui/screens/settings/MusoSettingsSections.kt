/*
 * Muso custom settings sections, rendered inside the ArchiveTune settings kit
 * (Muso port, Phase 3). Every row writes a real muso DataStore preference that
 * the app actually reads — the kit UI is the shell, the behavior is muso's.
 */

package com.muso.music.ui.screens.settings

import android.app.Activity
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.utils.parseCookieString
import com.muso.music.R
import com.muso.music.constants.AccountChannelHandleKey
import com.muso.music.constants.AccountEmailKey
import com.muso.music.constants.AccountNameKey
import com.muso.music.constants.AnimatedArtworkKey
import com.muso.music.constants.AppLanguageKey
import com.muso.music.constants.AppLanguageToName
import com.muso.music.constants.AudioOffloadKey
import com.muso.music.constants.AudioQuality
import com.muso.music.constants.AudioQualityKey
import com.muso.music.constants.AudioNormalizationKey
import com.muso.music.constants.AutoDownloadLikedSongsKey
import com.muso.music.constants.AutoLoadMoreKey
import com.muso.music.constants.AutoSkipNextOnErrorKey
import com.muso.music.constants.AutomixKey
import com.muso.music.constants.CountryCodeToName
import com.muso.music.constants.CropAlbumArtKey
import com.muso.music.constants.CrossfadeDurationKey
import com.muso.music.constants.CrossfadeEnabledKey
import com.muso.music.constants.DarkModeKey
import com.muso.music.constants.DataSaverKey
import com.muso.music.constants.DefaultOpenTabKey
import com.muso.music.constants.DisableScreenshotKey
import com.muso.music.constants.DownloadOnWifiOnlyKey
import com.muso.music.constants.DownloadQualityKey
import com.muso.music.constants.DynamicThemeKey
import com.muso.music.constants.EnableBetterLyricsKey
import com.muso.music.constants.EnableKugouKey
import com.muso.music.constants.EnableLrcLibKey
import com.muso.music.constants.EnablePaxsenixKey
import com.muso.music.constants.EnableSimpMusicKey
import com.muso.music.constants.EnableUnisonKey
import com.muso.music.constants.EnableYouLyPlusKey
import com.muso.music.constants.GridCellSize
import com.muso.music.constants.GridCellSizeKey
import com.muso.music.constants.HideExplicitKey
import com.muso.music.constants.HidePlayerSliderKey
import com.muso.music.constants.HidePlayerThumbnailKey
import com.muso.music.constants.HighRefreshRateKey
import com.muso.music.constants.HistoryDurationKey
import com.muso.music.constants.InnerTubeCookieKey
import com.muso.music.constants.KeepScreenOnKey
import com.muso.music.constants.LanguageCodeToName
import com.muso.music.constants.LiquidGlassNavBarKey
import com.muso.music.constants.LoudnessPreset
import com.muso.music.constants.LoudnessPresetKey
import com.muso.music.constants.LyricsAnimationStyle
import com.muso.music.constants.LyricsAnimationStyleKey
import com.muso.music.constants.LyricsAutoScrollKey
import com.muso.music.constants.LyricsBlurEnabledKey
import com.muso.music.constants.LyricsLineSpacingKey
import com.muso.music.constants.LyricsOffsetKey
import com.muso.music.constants.LyricsPosition
import com.muso.music.constants.LyricsProviderOrderKey
import com.muso.music.constants.LyricsRomanizationKey
import com.muso.music.constants.LyricsStyle
import com.muso.music.constants.LyricsStyleKey
import com.muso.music.constants.LyricsTextPositionKey
import com.muso.music.constants.LyricsTextSizeKey
import com.muso.music.constants.MaxImageCacheSizeKey
import com.muso.music.constants.MaxSongCacheSizeKey
import com.muso.music.constants.MiniPlayerStyle
import com.muso.music.constants.MiniPlayerStyleKey
import com.muso.music.constants.PauseListenHistoryKey
import com.muso.music.constants.PauseOnMuteKey
import com.muso.music.constants.PauseSearchHistoryKey
import com.muso.music.constants.PersistentQueueKey
import com.muso.music.constants.PlayerBackgroundStyle
import com.muso.music.constants.PlayerBackgroundStyleKey
import com.muso.music.constants.PlayerButtonsStyle
import com.muso.music.constants.PlayerButtonsStyleKey
import com.muso.music.constants.PlayerStyle
import com.muso.music.constants.PlayerStyleKey
import com.muso.music.constants.PlayerTextAlignmentKey
import com.muso.music.constants.PreloadLyricsKey
import com.muso.music.constants.PreloadNextSongKey
import com.muso.music.constants.PreventDuplicateTracksKey
import com.muso.music.constants.ProxyEnabledKey
import com.muso.music.constants.ProxyTypeKey
import com.muso.music.constants.ProxyUrlKey
import com.muso.music.constants.RotatingArtworkKey
import com.muso.music.constants.SeekExtraSecondsKey
import com.muso.music.constants.ShowCachedPlaylistKey
import com.muso.music.constants.ShowCodecOnPlayerKey
import com.muso.music.constants.ShowDownloadedPlaylistKey
import com.muso.music.constants.ShowLikedPlaylistKey
import com.muso.music.constants.ShowUploadedPlaylistKey
import com.muso.music.constants.ShowVideoInPlayerKey
import com.muso.music.constants.SkipSilenceKey
import com.muso.music.constants.SpatialAudioKey
import com.muso.music.constants.StopMusicOnTaskClearKey
import com.muso.music.constants.SYSTEM_DEFAULT
import com.muso.music.constants.TranslucentNavigationBarKey
import com.muso.music.constants.UseLoginForBrowse
import com.muso.music.constants.VideoQuality
import com.muso.music.constants.VideoQualityKey
import com.muso.music.lyrics.LyricsProviderRegistry
import com.muso.music.ui.component.DefaultDialog
import com.muso.music.utils.rememberEnumPreference
import com.muso.music.utils.rememberPreference
import moe.rukamori.archivetune.ui.component.EnumListPreference
import moe.rukamori.archivetune.ui.component.ListPreference
import moe.rukamori.archivetune.ui.component.PreferenceEntry
import moe.rukamori.archivetune.ui.component.PreferenceGroupScope
import moe.rukamori.archivetune.ui.component.SwitchPreference
import moe.rukamori.archivetune.ui.component.TextFieldDialog
import java.net.Proxy

// ---------------------------------------------------------------------------
// Shared enums (moved from the removed muso AppearanceSettings).
// ---------------------------------------------------------------------------

enum class DarkMode {
    ON, OFF, AUTO,
}

enum class NavigationTab {
    HOME, LIBRARY,
}

enum class PlayerTextAlignment {
    CENTER, SIDED,
}

val THEME_COLORS: List<Pair<Int, String>> = listOf(
    0 to "Default",
    0xFF4285F4.toInt() to "Blue",
    0xFF1DB954.toInt() to "Green",
    0xFFF44336.toInt() to "Red",
    0xFFE91E63.toInt() to "Pink",
    0xFF9C27B0.toInt() to "Purple",
    0xFF673AB7.toInt() to "Deep Purple",
    0xFF3F51B5.toInt() to "Indigo",
    0xFF00BCD4.toInt() to "Cyan",
    0xFF009688.toInt() to "Teal",
    0xFF4CAF50.toInt() to "Light Green",
    0xFFFF9800.toInt() to "Orange",
    0xFF795548.toInt() to "Brown",
    0xFF607D8B.toInt() to "Blue Grey",
)
val THEME_COLOR_NAMES: Map<Int, String> = THEME_COLORS.toMap()

private const val THEME_COLOR_MODE_DEFAULT = "default"
private const val THEME_COLOR_MODE_WALLPAPER = "wallpaper"
private const val THEME_COLOR_MODE_CUSTOM = "custom"

// ---------------------------------------------------------------------------
// Theme rows (inside the kit AppearanceSettings theme group).
// ---------------------------------------------------------------------------

@Composable
fun PreferenceGroupScope.musoThemeRows() {
    val (dynamicTheme, onDynamicThemeChange) = rememberPreference(key = DynamicThemeKey, defaultValue = true)
    val (customThemeColor, onCustomThemeColorChange) = rememberPreference(key = com.muso.music.constants.CustomThemeColorKey, defaultValue = 0)
    val (darkMode, onDarkModeChange) = rememberEnumPreference(key = DarkModeKey, defaultValue = DarkMode.AUTO)
    val (pureBlack, onPureBlackChange) = rememberPreference(key = com.muso.music.constants.PureBlackKey, defaultValue = false)
    val (translucentNavBar, onTranslucentNavBarChange) = rememberPreference(key = TranslucentNavigationBarKey, defaultValue = false)
    val (playerBackgroundStyle, onPlayerBackgroundStyleChange) = rememberEnumPreference(key = PlayerBackgroundStyleKey, defaultValue = PlayerBackgroundStyle.DEFAULT)
    val (liquidGlassNavBar, onLiquidGlassNavBarChange) = rememberPreference(key = LiquidGlassNavBarKey, defaultValue = true)
    val (highRefreshRate, onHighRefreshRateChange) = rememberPreference(key = HighRefreshRateKey, defaultValue = false)

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = if (darkMode == DarkMode.AUTO) isSystemInDarkTheme else darkMode == DarkMode.ON

    var showCustomColorDialog by remember { mutableStateOf(false) }
    if (showCustomColorDialog) {
        DefaultDialog(
            onDismiss = { showCustomColorDialog = false },
            content = {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.custom_color),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    THEME_COLORS.drop(1).chunked(5).forEach { rowColors ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(top = 12.dp),
                        ) {
                            rowColors.forEach { (argb, _) ->
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(argb))
                                        .clickable {
                                            onCustomThemeColorChange(argb)
                                            showCustomColorDialog = false
                                        },
                                ) {
                                    if (argb == customThemeColor) {
                                        Icon(
                                            painter = painterResource(R.drawable.close),
                                            contentDescription = null,
                                            tint = Color.White,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            buttons = {
                TextButton(onClick = { showCustomColorDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    val themeColorMode = when {
        dynamicTheme -> THEME_COLOR_MODE_WALLPAPER
        customThemeColor != 0 -> THEME_COLOR_MODE_CUSTOM
        else -> THEME_COLOR_MODE_DEFAULT
    }

    item {
        ListPreference(
            title = { Text(stringResource(R.string.theme_color)) },
            icon = { Icon(painterResource(R.drawable.palette), null) },
            selectedValue = themeColorMode,
            values = listOf(THEME_COLOR_MODE_DEFAULT, THEME_COLOR_MODE_WALLPAPER, THEME_COLOR_MODE_CUSTOM),
            valueText = {
                when (it) {
                    THEME_COLOR_MODE_DEFAULT -> stringResource(R.string.theme_color_default)
                    THEME_COLOR_MODE_WALLPAPER -> stringResource(R.string.theme_color_wallpaper)
                    else -> stringResource(R.string.theme_color_custom)
                }
            },
            onValueSelected = { mode ->
                when (mode) {
                    THEME_COLOR_MODE_DEFAULT -> {
                        onDynamicThemeChange(false)
                        onCustomThemeColorChange(0)
                    }
                    THEME_COLOR_MODE_WALLPAPER -> onDynamicThemeChange(true)
                    THEME_COLOR_MODE_CUSTOM -> {
                        onDynamicThemeChange(false)
                        if (customThemeColor == 0) {
                            onCustomThemeColorChange(THEME_COLORS[1].first)
                        }
                        showCustomColorDialog = true
                    }
                }
            },
        )
    }
    item {
        EnumListPreference(
            title = { Text(stringResource(R.string.theme)) },
            icon = { Icon(painterResource(R.drawable.dark_mode), null) },
            selectedValue = darkMode,
            onValueSelected = onDarkModeChange,
            valueText = {
                when (it) {
                    DarkMode.ON -> stringResource(R.string.dark_theme_on)
                    DarkMode.OFF -> stringResource(R.string.dark_theme_off)
                    DarkMode.AUTO -> stringResource(R.string.dark_theme_follow_system)
                }
            },
        )
    }
    item(visible = useDarkTheme) {
        SwitchPreference(
            title = { Text(stringResource(R.string.pure_black)) },
            icon = { Icon(painterResource(R.drawable.contrast), null) },
            checked = pureBlack,
            onCheckedChange = onPureBlackChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.liquid_glass_effect)) },
            description = stringResource(R.string.liquid_glass_effect_desc),
            icon = { Icon(painterResource(R.drawable.water_drop), null) },
            checked = translucentNavBar && playerBackgroundStyle == PlayerBackgroundStyle.BLURRED_ARTWORK,
            onCheckedChange = { on ->
                onTranslucentNavBarChange(on)
                onPlayerBackgroundStyleChange(
                    if (on) PlayerBackgroundStyle.BLURRED_ARTWORK else PlayerBackgroundStyle.DEFAULT,
                )
            },
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.liquid_glass_nav_bar)) },
            description = stringResource(R.string.liquid_glass_nav_bar_desc),
            icon = { Icon(painterResource(R.drawable.clear_all), null) },
            checked = liquidGlassNavBar,
            onCheckedChange = onLiquidGlassNavBarChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.enable_high_refresh_rate)) },
            description = stringResource(R.string.enable_high_refresh_rate_desc),
            icon = { Icon(painterResource(R.drawable.update), null) },
            checked = highRefreshRate,
            onCheckedChange = onHighRefreshRateChange,
        )
    }
}

// ---------------------------------------------------------------------------
// Now playing / player style rows (inside the kit AppearanceSettings player
// group).
// ---------------------------------------------------------------------------

@Composable
fun PreferenceGroupScope.musoPlayerStyleRows() {
    val (playerStyle, onPlayerStyleChange) = rememberEnumPreference(key = PlayerStyleKey, defaultValue = PlayerStyle.EXPRESSIVE)
    val (miniPlayerStyle, onMiniPlayerStyleChange) = rememberEnumPreference(key = MiniPlayerStyleKey, defaultValue = MiniPlayerStyle.GLASS)
    val (playerBackgroundStyle, onPlayerBackgroundStyleChange) = rememberEnumPreference(key = PlayerBackgroundStyleKey, defaultValue = PlayerBackgroundStyle.DEFAULT)
    val (sliderStyle, onSliderStyleChange) = rememberEnumPreference(key = com.muso.music.constants.SliderStyleKey, defaultValue = com.muso.music.constants.SliderStyle.SQUIGGLY)
    val (playerButtonsStyle, onPlayerButtonsStyleChange) = rememberEnumPreference(key = PlayerButtonsStyleKey, defaultValue = PlayerButtonsStyle.DEFAULT)
    val (hidePlayerSlider, onHidePlayerSliderChange) = rememberPreference(key = HidePlayerSliderKey, defaultValue = false)
    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) = rememberPreference(key = HidePlayerThumbnailKey, defaultValue = false)
    val (cropAlbumArt, onCropAlbumArtChange) = rememberPreference(key = CropAlbumArtKey, defaultValue = false)
    val (rotatingArtwork, onRotatingArtworkChange) = rememberPreference(key = RotatingArtworkKey, defaultValue = false)
    val (showCodecOnPlayer, onShowCodecOnPlayerChange) = rememberPreference(key = ShowCodecOnPlayerKey, defaultValue = true)

    var showSliderOptionDialog by rememberSaveable { mutableStateOf(false) }
    if (showSliderOptionDialog) {
        DefaultDialog(
            buttons = {
                TextButton(
                    onClick = { showSliderOptionDialog = false },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
            onDismiss = { showSliderOptionDialog = false },
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, if (sliderStyle == com.muso.music.constants.SliderStyle.DEFAULT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .clickable {
                            onSliderStyleChange(com.muso.music.constants.SliderStyle.DEFAULT)
                            showSliderOptionDialog = false
                        }
                        .padding(16.dp),
                ) {
                    var sliderValue by remember { mutableFloatStateOf(0.5f) }
                    Slider(
                        value = sliderValue,
                        valueRange = 0f..1f,
                        onValueChange = { sliderValue = it },
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {},
                                )
                            },
                    )
                    Text(
                        text = stringResource(R.string.default_),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, if (sliderStyle == com.muso.music.constants.SliderStyle.SQUIGGLY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .clickable {
                            onSliderStyleChange(com.muso.music.constants.SliderStyle.SQUIGGLY)
                            showSliderOptionDialog = false
                        }
                        .padding(16.dp),
                ) {
                    var sliderValue by remember { mutableFloatStateOf(0.5f) }
                    // Player parity: the player's SQUIGGLY is
                    // PlayerSliderByStyle's squiggle branch.
                    com.maxrave.simpmusic.ui.component.PlayerSliderByStyle(
                        style = com.muso.music.constants.SliderStyle.SQUIGGLY,
                        position = sliderValue,
                        onSeek = { sliderValue = it },
                        onSeekFinished = { },
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(R.string.squiggly),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, if (sliderStyle == com.muso.music.constants.SliderStyle.WAVY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .clickable {
                            onSliderStyleChange(com.muso.music.constants.SliderStyle.WAVY)
                            showSliderOptionDialog = false
                        }
                        .padding(16.dp),
                ) {
                    com.maxrave.simpmusic.ui.component.PlayerSliderByStyle(
                        style = com.muso.music.constants.SliderStyle.WAVY,
                        position = 0.5f,
                        onSeek = {},
                        onSeekFinished = {},
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(R.string.wavy),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, if (sliderStyle == com.muso.music.constants.SliderStyle.SLIM) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .clickable {
                            onSliderStyleChange(com.muso.music.constants.SliderStyle.SLIM)
                            showSliderOptionDialog = false
                        }
                        .padding(16.dp),
                ) {
                    com.maxrave.simpmusic.ui.component.PlayerSliderByStyle(
                        style = com.muso.music.constants.SliderStyle.SLIM,
                        position = 0.5f,
                        onSeek = {},
                        onSeekFinished = {},
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(R.string.slim),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }

    item {
        EnumListPreference(
            title = { Text(stringResource(R.string.now_playing_style)) },
            icon = { Icon(painterResource(R.drawable.play), null) },
            selectedValue = playerStyle,
            onValueSelected = onPlayerStyleChange,
            valueText = {
                when (it) {
                    // Renamed per user spec: classic -> Classic V2,
                    // expressive -> M3 Expressive, immersive -> Immersive Nightly.
                    PlayerStyle.CLASSIC -> stringResource(R.string.player_style_classic_v2)
                    PlayerStyle.EXPRESSIVE -> stringResource(R.string.player_style_m3_expressive)
                    PlayerStyle.IMMERSIVE -> stringResource(R.string.player_style_immersive_nightly)
                }
            },
        )
    }
    item {
        EnumListPreference(
            title = { Text(stringResource(R.string.mini_player_style)) },
            icon = { Icon(painterResource(R.drawable.pip), null) },
            selectedValue = miniPlayerStyle,
            onValueSelected = onMiniPlayerStyleChange,
            valueText = {
                when (it) {
                    MiniPlayerStyle.GLASS -> stringResource(R.string.mini_player_style_glass)
                    MiniPlayerStyle.FLAT -> stringResource(R.string.mini_player_style_flat)
                }
            },
        )
    }
    item {
        EnumListPreference(
            title = { Text(stringResource(R.string.player_background)) },
            icon = { Icon(painterResource(R.drawable.image), null) },
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
    item {
        PreferenceEntry(
            title = { Text(stringResource(R.string.player_slider_style)) },
            description = when (sliderStyle) {
                com.muso.music.constants.SliderStyle.DEFAULT -> stringResource(R.string.default_)
                com.muso.music.constants.SliderStyle.WAVY -> stringResource(R.string.wavy)
                com.muso.music.constants.SliderStyle.SLIM -> stringResource(R.string.slim)
                com.muso.music.constants.SliderStyle.SQUIGGLY -> stringResource(R.string.squiggly)
            },
            icon = { Icon(painterResource(R.drawable.tune), null) },
            onClick = { showSliderOptionDialog = true },
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.hide_player_slider)) },
            icon = { Icon(painterResource(R.drawable.sliders), null) },
            checked = hidePlayerSlider,
            onCheckedChange = onHidePlayerSliderChange,
        )
    }
    item {
        EnumListPreference(
            title = { Text(stringResource(R.string.player_buttons_style)) },
            icon = { Icon(painterResource(R.drawable.skip_next), null) },
            selectedValue = playerButtonsStyle,
            onValueSelected = onPlayerButtonsStyleChange,
            valueText = {
                when (it) {
                    PlayerButtonsStyle.DEFAULT -> stringResource(R.string.player_buttons_style_default)
                    PlayerButtonsStyle.PRIMARY -> stringResource(R.string.player_buttons_style_primary)
                    PlayerButtonsStyle.TERTIARY -> stringResource(R.string.player_buttons_style_tertiary)
                }
            },
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.hide_player_thumbnail)) },
            icon = { Icon(painterResource(R.drawable.music_note), null) },
            checked = hidePlayerThumbnail,
            onCheckedChange = onHidePlayerThumbnailChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.crop_album_art)) },
            icon = { Icon(painterResource(R.drawable.crop), null) },
            checked = cropAlbumArt,
            onCheckedChange = onCropAlbumArtChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.rotating_artwork)) },
            description = stringResource(R.string.rotating_artwork_desc),
            icon = { Icon(painterResource(R.drawable.disc), null) },
            checked = rotatingArtwork,
            onCheckedChange = onRotatingArtworkChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.show_codec_on_player)) },
            icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
            checked = showCodecOnPlayer,
            onCheckedChange = onShowCodecOnPlayerChange,
        )
    }
}

// ---------------------------------------------------------------------------
// Video rows (player group of kit AppearanceSettings).
// ---------------------------------------------------------------------------

@Composable
fun PreferenceGroupScope.musoVideoRows() {
    val (videoQuality, onVideoQualityChange) = rememberEnumPreference(VideoQualityKey, defaultValue = VideoQuality.Q720)
    val (showVideoInPlayer, onShowVideoInPlayerChange) = rememberPreference(ShowVideoInPlayerKey, defaultValue = true)

    item {
        EnumListPreference(
            title = { Text(stringResource(R.string.video_quality)) },
            icon = { Icon(painterResource(R.drawable.slow_motion_video), null) },
            selectedValue = videoQuality,
            onValueSelected = onVideoQualityChange,
            valueText = {
                when (it) {
                    VideoQuality.Q360 -> stringResource(R.string.video_quality_360)
                    VideoQuality.Q720 -> stringResource(R.string.video_quality_720)
                    VideoQuality.Q1080 -> stringResource(R.string.video_quality_1080)
                }
            },
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.show_video_in_player)) },
            description = stringResource(R.string.show_video_in_player_desc),
            icon = { Icon(painterResource(R.drawable.fullscreen), null) },
            checked = showVideoInPlayer,
            onCheckedChange = onShowVideoInPlayerChange,
        )
    }
}

// ---------------------------------------------------------------------------
// Layout + auto playlists rows (misc group of kit AppearanceSettings).
// ---------------------------------------------------------------------------

@Composable
fun PreferenceGroupScope.musoLayoutRows() {
    val (defaultOpenTab, onDefaultOpenTabChange) = rememberEnumPreference(key = DefaultOpenTabKey, defaultValue = NavigationTab.HOME)
    val (gridCellSize, onGridCellSizeChange) = rememberEnumPreference(key = GridCellSizeKey, defaultValue = GridCellSize.SMALL)
    val (showLikedPlaylist, onShowLikedPlaylistChange) = rememberPreference(key = ShowLikedPlaylistKey, defaultValue = true)
    val (showDownloadedPlaylist, onShowDownloadedPlaylistChange) = rememberPreference(key = ShowDownloadedPlaylistKey, defaultValue = true)
    val (showUploadedPlaylist, onShowUploadedPlaylistChange) = rememberPreference(key = ShowUploadedPlaylistKey, defaultValue = true)
    val (showCachedPlaylist, onShowCachedPlaylistChange) = rememberPreference(key = ShowCachedPlaylistKey, defaultValue = true)

    item {
        EnumListPreference(
            title = { Text(stringResource(R.string.default_open_tab)) },
            icon = { Icon(painterResource(R.drawable.tab), null) },
            selectedValue = defaultOpenTab,
            onValueSelected = onDefaultOpenTabChange,
            valueText = {
                when (it) {
                    NavigationTab.HOME -> stringResource(R.string.home)
                    NavigationTab.LIBRARY -> stringResource(R.string.library)
                }
            },
        )
    }
    item {
        EnumListPreference(
            title = { Text(stringResource(R.string.grid_cell_size)) },
            icon = { Icon(painterResource(R.drawable.grid_view), null) },
            selectedValue = gridCellSize,
            onValueSelected = onGridCellSizeChange,
            valueText = {
                when (it) {
                    GridCellSize.SMALL -> stringResource(R.string.small)
                    GridCellSize.BIG -> stringResource(R.string.big)
                }
            },
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.show_liked_playlist)) },
            icon = { Icon(painterResource(R.drawable.favorite), null) },
            checked = showLikedPlaylist,
            onCheckedChange = onShowLikedPlaylistChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.show_downloaded_playlist)) },
            icon = { Icon(painterResource(R.drawable.offline), null) },
            checked = showDownloadedPlaylist,
            onCheckedChange = onShowDownloadedPlaylistChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.show_uploaded_playlist)) },
            icon = { Icon(painterResource(R.drawable.upload), null) },
            checked = showUploadedPlaylist,
            onCheckedChange = onShowUploadedPlaylistChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.show_cached_playlist)) },
            icon = { Icon(painterResource(R.drawable.cached), null) },
            checked = showCachedPlaylist,
            onCheckedChange = onShowCachedPlaylistChange,
        )
    }
}

// ---------------------------------------------------------------------------
// Streaming quality rows (player group of kit PlayerSettings).
// ---------------------------------------------------------------------------

@Composable
fun PreferenceGroupScope.musoAudioQualityRows() {
    val (audioQuality, onAudioQualityChange) = rememberEnumPreference(AudioQualityKey, defaultValue = AudioQuality.HIGH_OPUS)
    val (downloadQuality, onDownloadQualityChange) = rememberEnumPreference(key = DownloadQualityKey, defaultValue = AudioQuality.MEDIUM)

    item {
        EnumListPreference(
            title = { Text(stringResource(R.string.streaming_quality)) },
            icon = { Icon(painterResource(R.drawable.speed), null) },
            selectedValue = audioQuality,
            onValueSelected = onAudioQualityChange,
            valueText = {
                when (it) {
                    AudioQuality.LOW -> stringResource(R.string.audio_quality_low_66)
                    AudioQuality.MEDIUM -> stringResource(R.string.audio_quality_medium_129)
                    AudioQuality.HIGH_OPUS -> stringResource(R.string.audio_quality_high_opus)
                    AudioQuality.HIGH_AAC -> stringResource(R.string.audio_quality_high_aac)
                }
            },
        )
    }
    item {
        EnumListPreference(
            title = { Text(stringResource(R.string.download_quality)) },
            icon = { Icon(painterResource(R.drawable.cloud), null) },
            selectedValue = downloadQuality,
            onValueSelected = onDownloadQualityChange,
            valueText = {
                when (it) {
                    AudioQuality.LOW -> stringResource(R.string.audio_quality_low_66)
                    AudioQuality.MEDIUM -> stringResource(R.string.audio_quality_medium_129)
                    AudioQuality.HIGH_OPUS -> stringResource(R.string.audio_quality_high_opus)
                    AudioQuality.HIGH_AAC -> stringResource(R.string.audio_quality_high_aac)
                }
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Audio behaviour rows (player group of kit PlayerSettings).
// ---------------------------------------------------------------------------

@Composable
fun PreferenceGroupScope.musoAudioBehaviourRows() {
    val (skipSilence, onSkipSilenceChange) = rememberPreference(SkipSilenceKey, defaultValue = false)
    val (audioNormalization, onAudioNormalizationChange) = rememberPreference(AudioNormalizationKey, defaultValue = false)
    val (loudnessPreset, onLoudnessPresetChange) = rememberEnumPreference(LoudnessPresetKey, defaultValue = LoudnessPreset.NORMAL)
    val (spatialAudio, onSpatialAudioChange) = rememberPreference(SpatialAudioKey, defaultValue = false)
    val (dataSaver, onDataSaverChange) = rememberPreference(DataSaverKey, defaultValue = false)
    val (seekExtraSeconds, onSeekExtraSecondsChange) = rememberPreference(SeekExtraSecondsKey, defaultValue = false)
    val (playerTextAlignment, onPlayerTextAlignmentChange) = rememberEnumPreference(key = PlayerTextAlignmentKey, defaultValue = PlayerTextAlignment.CENTER)
    val (automix, onAutomixChange) = rememberPreference(AutomixKey, defaultValue = false)
    val (preloadNextSong, onPreloadNextSongChange) = rememberPreference(PreloadNextSongKey, defaultValue = false)
    val (preloadLyrics, onPreloadLyricsChange) = rememberPreference(PreloadLyricsKey, defaultValue = false)
    val (audioOffload, onAudioOffloadChange) = rememberPreference(AudioOffloadKey, defaultValue = false)
    val (crossfadeEnabled, onCrossfadeEnabledChange) = rememberPreference(CrossfadeEnabledKey, defaultValue = false)
    val (crossfadeDuration, onCrossfadeDurationChange) = rememberPreference(CrossfadeDurationKey, defaultValue = 4)

    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.skip_silence)) },
            icon = { Icon(painterResource(R.drawable.fast_forward), null) },
            checked = skipSilence,
            onCheckedChange = onSkipSilenceChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.audio_normalization)) },
            icon = { Icon(painterResource(R.drawable.discover_tune), null) },
            checked = audioNormalization,
            onCheckedChange = onAudioNormalizationChange,
        )
    }
    item {
        EnumListPreference(
            title = { Text(stringResource(R.string.audio_loudness_preset)) },
            icon = { Icon(painterResource(R.drawable.more_horiz), null) },
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
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.spatial_audio)) },
            description = stringResource(R.string.spatial_audio_desc),
            icon = { Icon(painterResource(R.drawable.surround_sound), null) },
            checked = spatialAudio,
            onCheckedChange = onSpatialAudioChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.data_saver)) },
            description = stringResource(R.string.data_saver_desc),
            icon = { Icon(painterResource(R.drawable.storage), null) },
            checked = dataSaver,
            onCheckedChange = onDataSaverChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.seek_seconds_addup)) },
            description = stringResource(R.string.seek_seconds_addup_desc),
            icon = { Icon(painterResource(R.drawable.replay), null) },
            checked = seekExtraSeconds,
            onCheckedChange = onSeekExtraSecondsChange,
        )
    }
    item {
        EnumListPreference(
            title = { Text(stringResource(R.string.player_text_alignment)) },
            icon = {
                Icon(
                    painter = painterResource(
                        when (playerTextAlignment) {
                            PlayerTextAlignment.CENTER -> R.drawable.format_align_center
                            PlayerTextAlignment.SIDED -> R.drawable.format_align_left
                        },
                    ),
                    contentDescription = null,
                )
            },
            selectedValue = playerTextAlignment,
            onValueSelected = onPlayerTextAlignmentChange,
            valueText = {
                when (it) {
                    PlayerTextAlignment.SIDED -> stringResource(R.string.sided)
                    PlayerTextAlignment.CENTER -> stringResource(R.string.center)
                }
            },
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.automix)) },
            description = stringResource(R.string.automix_desc),
            icon = { Icon(painterResource(R.drawable.playlist_play), null) },
            checked = automix,
            onCheckedChange = onAutomixChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.preload_next_song)) },
            description = stringResource(R.string.preload_next_song_desc),
            icon = { Icon(painterResource(R.drawable.arrow_forward), null) },
            checked = preloadNextSong,
            onCheckedChange = onPreloadNextSongChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.preload_lyrics)) },
            description = stringResource(R.string.preload_lyrics_desc),
            icon = { Icon(painterResource(R.drawable.search), null) },
            checked = preloadLyrics,
            onCheckedChange = onPreloadLyricsChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.crossfade)) },
            description = stringResource(R.string.crossfade_desc),
            icon = { Icon(painterResource(R.drawable.share), null) },
            checked = crossfadeEnabled,
            onCheckedChange = onCrossfadeEnabledChange,
        )
    }
    item(visible = crossfadeEnabled) {
        PreferenceEntry(
            title = { Text(stringResource(R.string.crossfade_duration)) },
            description = "${crossfadeDuration}s",
            icon = { Icon(painterResource(R.drawable.timer), null) },
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
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.audio_offload)) },
            description = stringResource(R.string.audio_offload_desc),
            icon = { Icon(painterResource(R.drawable.input), null) },
            checked = audioOffload,
            onCheckedChange = onAudioOffloadChange,
            isEnabled = !crossfadeEnabled,
        )
    }
}

// ---------------------------------------------------------------------------
// Audio effects row (player group of kit PlayerSettings).
// ---------------------------------------------------------------------------

@Composable
fun PreferenceGroupScope.musoAudioEffectsRow(navController: NavController) {
    item {
        PreferenceEntry(
            title = { Text(stringResource(R.string.audio_effects)) },
            description = stringResource(R.string.audio_effects_desc),
            icon = { Icon(painterResource(R.drawable.equalizer), null) },
            onClick = { navController.navigate("settings/audio_effects") },
        )
    }
}

// ---------------------------------------------------------------------------
// Queue rows (queue group of kit PlayerSettings).
// ---------------------------------------------------------------------------

@Composable
fun PreferenceGroupScope.musoQueueRows() {
    val (persistentQueue, onPersistentQueueChange) = rememberPreference(PersistentQueueKey, defaultValue = true)
    val (autoLoadMore, onAutoLoadMoreChange) = rememberPreference(AutoLoadMoreKey, defaultValue = true)
    val (preventDuplicateTracks, onPreventDuplicateTracksChange) = rememberPreference(PreventDuplicateTracksKey, defaultValue = false)
    val (autoSkipNextOnError, onAutoSkipNextOnErrorChange) = rememberPreference(AutoSkipNextOnErrorKey, defaultValue = false)

    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.persistent_queue)) },
            description = stringResource(R.string.persistent_queue_desc),
            icon = { Icon(painterResource(R.drawable.queue_music), null) },
            checked = persistentQueue,
            onCheckedChange = onPersistentQueueChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.auto_load_more)) },
            description = stringResource(R.string.auto_load_more_desc),
            icon = { Icon(painterResource(R.drawable.playlist_add), null) },
            checked = autoLoadMore,
            onCheckedChange = onAutoLoadMoreChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.prevent_duplicate_tracks)) },
            description = stringResource(R.string.prevent_duplicate_tracks_desc),
            icon = { Icon(painterResource(R.drawable.playlist_remove), null) },
            checked = preventDuplicateTracks,
            onCheckedChange = onPreventDuplicateTracksChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.auto_skip_next_on_error)) },
            description = stringResource(R.string.auto_skip_next_on_error_desc),
            icon = { Icon(painterResource(R.drawable.remove), null) },
            checked = autoSkipNextOnError,
            onCheckedChange = onAutoSkipNextOnErrorChange,
        )
    }
}

// ---------------------------------------------------------------------------
// Misc player rows (misc group of kit PlayerSettings).
// ---------------------------------------------------------------------------

@Composable
fun PreferenceGroupScope.musoMiscPlayerRows() {
    val (stopMusicOnTaskClear, onStopMusicOnTaskClearChange) = rememberPreference(StopMusicOnTaskClearKey, defaultValue = false)
    val (pauseOnMute, onPauseOnMuteChange) = rememberPreference(PauseOnMuteKey, defaultValue = false)
    val (downloadOnWifiOnly, onDownloadOnWifiOnlyChange) = rememberPreference(DownloadOnWifiOnlyKey, defaultValue = false)
    val (historyDuration, onHistoryDurationChange) = rememberPreference(HistoryDurationKey, defaultValue = 0)
    val (animatedArtwork, onAnimatedArtworkChange) = rememberPreference(AnimatedArtworkKey, defaultValue = false)
    val (keepScreenOn, onKeepScreenOnChange) = rememberPreference(KeepScreenOnKey, defaultValue = false)
    val (animationsEnabled, onAnimationsEnabledChange) = rememberPreference(com.muso.music.constants.AnimationsEnabledKey, defaultValue = true)
    val (gestureAnimations, onGestureAnimationsChange) = rememberPreference(com.muso.music.constants.GestureAnimationsKey, defaultValue = true)
    val (reducedMotion, onReducedMotionChange) = rememberPreference(com.muso.music.constants.ReducedMotionKey, defaultValue = false)

    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.stop_music_on_task_clear)) },
            icon = { Icon(painterResource(R.drawable.close), null) },
            checked = stopMusicOnTaskClear,
            onCheckedChange = onStopMusicOnTaskClearChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.pause_music_when_media_is_muted)) },
            icon = { Icon(painterResource(R.drawable.pause), null) },
            checked = pauseOnMute,
            onCheckedChange = onPauseOnMuteChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.download_on_wifi_only)) },
            icon = { Icon(painterResource(R.drawable.wifi), null) },
            checked = downloadOnWifiOnly,
            onCheckedChange = onDownloadOnWifiOnlyChange,
        )
    }
    item {
        ListPreference(
            title = { Text(stringResource(R.string.history_duration)) },
            icon = { Icon(painterResource(R.drawable.bedtime), null) },
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
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.animated_artwork)) },
            description = stringResource(R.string.animated_artwork_desc),
            icon = { Icon(painterResource(R.drawable.brush), null) },
            checked = animatedArtwork,
            onCheckedChange = onAnimatedArtworkChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.keep_screen_on)) },
            description = stringResource(R.string.keep_screen_on_desc),
            icon = { Icon(painterResource(R.drawable.lock), null) },
            checked = keepScreenOn,
            onCheckedChange = onKeepScreenOnChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.animations)) },
            description = stringResource(R.string.animations_desc),
            icon = { Icon(painterResource(R.drawable.bolt), null) },
            checked = animationsEnabled,
            onCheckedChange = onAnimationsEnabledChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.gesture_animations)) },
            description = stringResource(R.string.gesture_animations_desc),
            icon = { Icon(painterResource(R.drawable.swipe), null) },
            checked = gestureAnimations,
            onCheckedChange = onGestureAnimationsChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.reduced_motion)) },
            description = stringResource(R.string.reduced_motion_desc),
            icon = { Icon(painterResource(R.drawable.expand_less), null) },
            checked = reducedMotion,
            onCheckedChange = onReducedMotionChange,
        )
    }
}

// ---------------------------------------------------------------------------
// Lyrics rows (display group of kit LyricsSettings).
// ---------------------------------------------------------------------------

@Composable
fun PreferenceGroupScope.musoLyricsRows() {
    val (lyricsStyle, onLyricsStyleChange) = rememberEnumPreference(key = LyricsStyleKey, defaultValue = LyricsStyle.APPLE_MUSIC)
    val (lyricsAnimationStyle, onLyricsAnimationStyleChange) = rememberEnumPreference(key = LyricsAnimationStyleKey, defaultValue = LyricsAnimationStyle.FLARE)
    val (lyricsTextPosition, onLyricsTextPositionChange) = rememberEnumPreference(key = LyricsTextPositionKey, defaultValue = LyricsPosition.CENTER)
    val (lyricsTextSize, onLyricsTextSizeChange) = rememberPreference(key = LyricsTextSizeKey, defaultValue = 26)
    val (lyricsLineSpacing, onLyricsLineSpacingChange) = rememberPreference(key = LyricsLineSpacingKey, defaultValue = 1.3f)
    val (lyricsAutoScroll, onLyricsAutoScrollChange) = rememberPreference(key = LyricsAutoScrollKey, defaultValue = true)
    val (lyricsBlurEnabled, onLyricsBlurEnabledChange) = rememberPreference(key = LyricsBlurEnabledKey, defaultValue = true)
    val (romanizeLyrics, onRomanizeLyricsChange) = rememberPreference(key = LyricsRomanizationKey, defaultValue = false)
    val (lyricsOffset, onLyricsOffsetChange) = rememberPreference(key = LyricsOffsetKey, defaultValue = 0)

    item {
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
            },
        )
    }
    item {
        EnumListPreference(
            title = { Text(stringResource(R.string.word_by_word_animation_style)) },
            icon = { Icon(painterResource(R.drawable.text_fields), null) },
            selectedValue = lyricsAnimationStyle,
            onValueSelected = onLyricsAnimationStyleChange,
            valueText = {
                when (it) {
                    LyricsAnimationStyle.FLARE -> stringResource(R.string.lyrics_style_flare)
                    LyricsAnimationStyle.NONE -> stringResource(R.string.lyrics_style_none)
                    LyricsAnimationStyle.FADE -> stringResource(R.string.lyrics_style_fade)
                    LyricsAnimationStyle.GLOW -> stringResource(R.string.lyrics_style_glow)
                    LyricsAnimationStyle.SLIDE -> stringResource(R.string.lyrics_style_slide)
                    LyricsAnimationStyle.KARAOKE -> stringResource(R.string.lyrics_style_karaoke)
                    LyricsAnimationStyle.APPLE -> stringResource(R.string.lyrics_style_apple)
                    LyricsAnimationStyle.APPLE_V2 -> stringResource(R.string.lyrics_style_apple_v2)
                    LyricsAnimationStyle.ECHOMUSIC_1 -> stringResource(R.string.lyrics_style_echomusic_1)
                    LyricsAnimationStyle.LYRICS_V2 -> stringResource(R.string.lyrics_style_lyrics_v2)
                    LyricsAnimationStyle.METRO_LYRICS -> stringResource(R.string.lyrics_style_metro)
                }
            },
        )
    }
    item {
        EnumListPreference(
            title = { Text(stringResource(R.string.lyrics_text_position)) },
            icon = { Icon(painterResource(R.drawable.format_align_left), null) },
            selectedValue = lyricsTextPosition,
            onValueSelected = onLyricsTextPositionChange,
            valueText = {
                when (it) {
                    LyricsPosition.LEFT -> stringResource(R.string.left)
                    LyricsPosition.CENTER -> stringResource(R.string.center)
                    LyricsPosition.RIGHT -> stringResource(R.string.right)
                }
            },
        )
    }
    item {
        PreferenceEntry(
            title = { Text(stringResource(R.string.lyrics_text_size)) },
            icon = { Icon(painterResource(R.drawable.text_fields), null) },
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
    }
    item {
        PreferenceEntry(
            title = { Text(stringResource(R.string.lyrics_line_spacing)) },
            description = "x%.1f".format(lyricsLineSpacing),
            icon = { Icon(painterResource(R.drawable.list), null) },
            content = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Slider(
                        value = lyricsLineSpacing,
                        onValueChange = { onLyricsLineSpacingChange(((it * 100).toInt() / 100f)) },
                        valueRange = 1f..2f,
                        steps = 19,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.lyrics_auto_scroll)) },
            description = stringResource(R.string.lyrics_auto_scroll_desc),
            icon = { Icon(painterResource(R.drawable.sync), null) },
            checked = lyricsAutoScroll,
            onCheckedChange = onLyricsAutoScrollChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.lyrics_blur)) },
            description = stringResource(R.string.lyrics_blur_desc),
            icon = { Icon(painterResource(R.drawable.blur_on), null) },
            checked = lyricsBlurEnabled,
            onCheckedChange = onLyricsBlurEnabledChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.romanize_lyrics)) },
            description = stringResource(R.string.romanize_lyrics_desc),
            icon = { Icon(painterResource(R.drawable.swap_horiz), null) },
            checked = romanizeLyrics,
            onCheckedChange = onRomanizeLyricsChange,
        )
    }
    item {
        PreferenceEntry(
            title = { Text(stringResource(R.string.lyrics_offset)) },
            description = stringResource(R.string.lyrics_offset_desc) +
                "  (" + (if (lyricsOffset > 0) "+" else "") + "${lyricsOffset}ms)",
            icon = { Icon(painterResource(R.drawable.swap_vert), null) },
            content = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Slider(
                        value = lyricsOffset.toFloat(),
                        onValueChange = { onLyricsOffsetChange(it.toInt()) },
                        valueRange = -5000f..5000f,
                        steps = 99,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Lyrics provider rows (providers group of kit LyricsSettings).
// ---------------------------------------------------------------------------

@Composable
fun PreferenceGroupScope.musoLyricsProviderRows() {
    val defaultProviderOrder = remember { LyricsProviderRegistry.serializeProviderOrder(LyricsProviderRegistry.getDefaultProviderOrder()) }
    val (lyricsProviderOrder, onLyricsProviderOrderChange) = rememberPreference(key = LyricsProviderOrderKey, defaultValue = defaultProviderOrder)
    val (enableKugou, onEnableKugouChange) = rememberPreference(key = EnableKugouKey, defaultValue = true)
    val (enableLrcLib, onEnableLrcLibChange) = rememberPreference(key = EnableLrcLibKey, defaultValue = true)
    val (enableBetterLyrics, onEnableBetterLyricsChange) = rememberPreference(key = EnableBetterLyricsKey, defaultValue = true)
    val (enableSimpMusic, onEnableSimpMusicChange) = rememberPreference(key = EnableSimpMusicKey, defaultValue = true)
    val (enableYouLyPlus, onEnableYouLyPlusChange) = rememberPreference(key = EnableYouLyPlusKey, defaultValue = true)
    val (enablePaxsenix, onEnablePaxsenixChange) = rememberPreference(key = EnablePaxsenixKey, defaultValue = true)
    val (enableUnison, onEnableUnisonChange) = rememberPreference(key = EnableUnisonKey, defaultValue = true)

    item {
        ListPreference(
            title = { Text(stringResource(R.string.preferred_lyrics_provider)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            selectedValue = LyricsProviderRegistry.deserializeProviderOrder(lyricsProviderOrder).firstOrNull() ?: "",
            values = LyricsProviderRegistry.providerNames,
            valueText = { LyricsProviderRegistry.getDisplayName(it) },
            onValueSelected = { providerName ->
                val order = listOf(providerName) + LyricsProviderRegistry.getDefaultProviderOrder().filter { it != providerName }
                onLyricsProviderOrderChange(LyricsProviderRegistry.serializeProviderOrder(order))
            },
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.enable_lrclib)) },
            icon = { Icon(painterResource(R.drawable.library_music), null) },
            checked = enableLrcLib,
            onCheckedChange = onEnableLrcLibChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.enable_kugou)) },
            icon = { Icon(painterResource(R.drawable.bookmark), null) },
            checked = enableKugou,
            onCheckedChange = onEnableKugouChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.enable_youlyplus)) },
            icon = { Icon(painterResource(R.drawable.artist), null) },
            checked = enableYouLyPlus,
            onCheckedChange = onEnableYouLyPlusChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.enable_paxsenix)) },
            icon = { Icon(painterResource(R.drawable.album), null) },
            checked = enablePaxsenix,
            onCheckedChange = onEnablePaxsenixChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.enable_unison)) },
            icon = { Icon(painterResource(R.drawable.shuffle), null) },
            checked = enableUnison,
            onCheckedChange = onEnableUnisonChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.enable_better_lyrics)) },
            icon = { Icon(painterResource(R.drawable.edit), null) },
            checked = enableBetterLyrics,
            onCheckedChange = onEnableBetterLyricsChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.enable_simpmusic)) },
            icon = { Icon(painterResource(R.drawable.mood), null) },
            checked = enableSimpMusic,
            onCheckedChange = onEnableSimpMusicChange,
        )
    }
}

// ---------------------------------------------------------------------------
// Content rows (general group of kit ContentSettings).
// ---------------------------------------------------------------------------

@Composable
fun PreferenceGroupScope.musoContentRows(navController: NavController) {
    val context = LocalContext.current
    val accountName by rememberPreference(AccountNameKey, "")
    val accountEmail by rememberPreference(AccountEmailKey, "")
    val accountChannelHandle by rememberPreference(AccountChannelHandleKey, "")
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val (appLanguage, onAppLanguageChange) = rememberPreference(AppLanguageKey, defaultValue = SYSTEM_DEFAULT)
    val (contentLanguage, onContentLanguageChange) = rememberPreference(key = com.muso.music.constants.ContentLanguageKey, defaultValue = "system")
    val (contentCountry, onContentCountryChange) = rememberPreference(key = com.muso.music.constants.ContentCountryKey, defaultValue = "system")
    val (hideExplicit, onHideExplicitChange) = rememberPreference(key = HideExplicitKey, defaultValue = false)
    val (autoDownloadLikedSongs, onAutoDownloadLikedSongsChange) = rememberPreference(key = AutoDownloadLikedSongsKey, defaultValue = false)
    val (proxyEnabled, onProxyEnabledChange) = rememberPreference(key = ProxyEnabledKey, defaultValue = false)
    val (proxyType, onProxyTypeChange) = rememberEnumPreference(key = ProxyTypeKey, defaultValue = Proxy.Type.HTTP)
    val (proxyUrl, onProxyUrlChange) = rememberPreference(key = ProxyUrlKey, defaultValue = "host:port")

    var showProxyUrlDialog by remember { mutableStateOf(false) }
    if (showProxyUrlDialog) {
        TextFieldDialog(
            icon = { Icon(painterResource(R.drawable.wifi_proxy), null) },
            title = { Text(stringResource(R.string.proxy_url)) },
            textFieldValue = proxyUrl,
            onTextFieldValueChange = onProxyUrlChange,
            onDone = { onProxyUrlChange(it); showProxyUrlDialog = false },
            onDismiss = { showProxyUrlDialog = false },
        )
    }

    val activity = context as? Activity
    item {
        ListPreference(
            title = { Text(stringResource(R.string.app_language)) },
            icon = { Icon(painterResource(R.drawable.language), null) },
            selectedValue = appLanguage,
            values = listOf(SYSTEM_DEFAULT) + AppLanguageToName.keys.toList(),
            valueText = {
                AppLanguageToName.getOrElse(it) {
                    stringResource(R.string.system_default)
                }
            },
            onValueSelected = { language ->
                onAppLanguageChange(language)
                // Keep the synchronous startup mirror in sync (see
                // MainActivity.attachBaseContext) so the next cold start skips
                // blocking DataStore I/O before the first frame.
                activity?.getSharedPreferences("muso_startup", Context.MODE_PRIVATE)
                    ?.edit()?.putString("appLanguage", language)?.apply()
                // Recreate so every stringResource re-resolves with the new locale.
                activity?.recreate()
            },
        )
    }
    item {
        ListPreference(
            title = { Text(stringResource(R.string.preferred_audio_language)) },
            icon = { Icon(painterResource(R.drawable.record_voice_over), null) },
            selectedValue = contentLanguage,
            values = listOf(SYSTEM_DEFAULT) + LanguageCodeToName.keys.toList(),
            valueText = {
                LanguageCodeToName.getOrElse(it) {
                    stringResource(R.string.system_default)
                }
            },
            onValueSelected = { newValue ->
                val locale = java.util.Locale.getDefault()
                val languageTag = locale.toLanguageTag().replace("-Hant", "")
                YouTube.locale =
                    YouTube.locale.copy(
                        hl =
                            newValue.takeIf { it != SYSTEM_DEFAULT }
                                ?: locale.language.takeIf { it in LanguageCodeToName }
                                ?: languageTag.takeIf { it in LanguageCodeToName }
                                ?: "en",
                    )
                onContentLanguageChange(newValue)
            },
        )
    }
    item {
        ListPreference(
            title = { Text(stringResource(R.string.content_country)) },
            icon = { Icon(painterResource(R.drawable.location_on), null) },
            selectedValue = contentCountry,
            values = listOf(SYSTEM_DEFAULT) + CountryCodeToName.keys.toList(),
            valueText = {
                CountryCodeToName.getOrElse(it) {
                    stringResource(R.string.system_default)
                }
            },
            onValueSelected = { newValue ->
                val locale = java.util.Locale.getDefault()
                YouTube.locale =
                    YouTube.locale.copy(
                        gl =
                            newValue.takeIf { it != SYSTEM_DEFAULT }
                                ?: locale.country.takeIf { it in CountryCodeToName }
                                ?: "US",
                    )
                onContentCountryChange(newValue)
            },
        )
    }
    item {
        PreferenceEntry(
            title = { Text(stringResource(R.string.youtube_account)) },
            description = if (isLoggedIn) {
                accountEmail.takeIf { it.isNotEmpty() }
                    ?: accountChannelHandle.takeIf { it.isNotEmpty() }
            } else {
                stringResource(R.string.manage_your_youtube_accounts)
            },
            icon = { Icon(painterResource(R.drawable.account_circle), null) },
            onClick = { navController.navigate("login") },
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.auto_download_liked_songs)) },
            description = stringResource(R.string.auto_download_liked_songs_desc),
            icon = { Icon(painterResource(R.drawable.add), null) },
            checked = autoDownloadLikedSongs,
            onCheckedChange = onAutoDownloadLikedSongsChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.play_explicit_content)) },
            description = stringResource(R.string.play_explicit_content_desc),
            icon = { Icon(painterResource(R.drawable.explicit), null) },
            checked = !hideExplicit,
            onCheckedChange = { onHideExplicitChange(!it) },
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.enable_proxy)) },
            icon = { Icon(painterResource(R.drawable.wifi_proxy), null) },
            checked = proxyEnabled,
            onCheckedChange = onProxyEnabledChange,
        )
    }
    item(visible = proxyEnabled) {
        EnumListPreference(
            title = { Text(stringResource(R.string.proxy_type)) },
            icon = { Icon(painterResource(R.drawable.swap_horiz), null) },
            selectedValue = proxyType,
            onValueSelected = onProxyTypeChange,
            valueText = { it.name },
        )
    }
    item(visible = proxyEnabled) {
        PreferenceEntry(
            title = { Text(stringResource(R.string.proxy_url)) },
            description = proxyUrl,
            icon = { Icon(painterResource(R.drawable.link), null) },
            onClick = { showProxyUrlDialog = true },
        )
    }
}

// ---------------------------------------------------------------------------
// Storage rows (a group of kit StorageSettings).
// ---------------------------------------------------------------------------

@Composable
fun PreferenceGroupScope.musoStorageRows() {
    val (maxImageCacheSize, onMaxImageCacheSizeChange) = rememberPreference(key = MaxImageCacheSizeKey, defaultValue = 512)
    val (maxSongCacheSize, onMaxSongCacheSizeChange) = rememberPreference(key = MaxSongCacheSizeKey, defaultValue = 1024)

    item {
        PreferenceEntry(
            title = { Text(stringResource(R.string.max_image_cache_size)) },
            description = "${maxImageCacheSize}MB",
            icon = { Icon(painterResource(R.drawable.image), null) },
            content = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Slider(
                        value = maxImageCacheSize.toFloat(),
                        onValueChange = { onMaxImageCacheSizeChange(it.toInt()) },
                        valueRange = 128f..4096f,
                        steps = 30,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
        )
    }
    item {
        PreferenceEntry(
            title = { Text(stringResource(R.string.max_song_cache_size)) },
            description = "${maxSongCacheSize}MB",
            icon = { Icon(painterResource(R.drawable.storage), null) },
            content = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Slider(
                        value = maxSongCacheSize.toFloat(),
                        onValueChange = { onMaxSongCacheSizeChange(it.toInt()) },
                        valueRange = 256f..8192f,
                        steps = 30,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Privacy rows (a group of kit PrivacySettings).
// ---------------------------------------------------------------------------

@Composable
fun PreferenceGroupScope.musoPrivacyRows() {
    val (pauseListenHistory, onPauseListenHistoryChange) = rememberPreference(key = PauseListenHistoryKey, defaultValue = false)
    val (pauseSearchHistory, onPauseSearchHistoryChange) = rememberPreference(key = PauseSearchHistoryKey, defaultValue = false)
    val (useLoginForBrowse, onUseLoginForBrowseChange) = rememberPreference(key = UseLoginForBrowse, defaultValue = false)
    val (disableScreenshot, onDisableScreenshotChange) = rememberPreference(key = DisableScreenshotKey, defaultValue = false)

    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.pause_listen_history)) },
            icon = { Icon(painterResource(R.drawable.history), null) },
            checked = pauseListenHistory,
            onCheckedChange = onPauseListenHistoryChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.pause_search_history)) },
            icon = { Icon(painterResource(R.drawable.search), null) },
            checked = pauseSearchHistory,
            onCheckedChange = onPauseSearchHistoryChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.use_login_for_browse)) },
            description = stringResource(R.string.use_login_for_browse_desc),
            icon = { Icon(painterResource(R.drawable.account_circle), null) },
            checked = useLoginForBrowse,
            onCheckedChange = onUseLoginForBrowseChange,
        )
    }
    item {
        SwitchPreference(
            title = { Text(stringResource(R.string.disable_screenshot)) },
            description = stringResource(R.string.disable_screenshot_desc),
            icon = { Icon(painterResource(R.drawable.lock), null) },
            checked = disableScreenshot,
            onCheckedChange = onDisableScreenshotChange,
        )
    }
}

// ---------------------------------------------------------------------------
// Integration rows (Spotify / Discord / AI / Listening history / Muso backup —
// muso features linked from the kit Integration and Backup screens).
// ---------------------------------------------------------------------------

@Composable
fun PreferenceGroupScope.musoIntegrationRows(navController: NavController) {
    item {
        PreferenceEntry(
            title = { Text(stringResource(R.string.spotify)) },
            description = stringResource(R.string.settings_desc_spotify),
            icon = { Icon(painterResource(R.drawable.spotify), null) },
            onClick = { navController.navigate("settings/spotify") },
        )
    }
    item {
        PreferenceEntry(
            title = { Text(stringResource(R.string.discord_integration)) },
            description = stringResource(R.string.settings_desc_discord),
            icon = { Icon(painterResource(R.drawable.discord), null) },
            onClick = { navController.navigate("settings/muso_discord") },
        )
    }
    item {
        PreferenceEntry(
            title = { Text(stringResource(R.string.ai)) },
            description = stringResource(R.string.settings_desc_ai),
            icon = { Icon(painterResource(R.drawable.auto_awesome), null) },
            onClick = { navController.navigate("settings/ai") },
        )
    }
    item {
        PreferenceEntry(
            title = { Text(stringResource(R.string.listening_history)) },
            description = stringResource(R.string.settings_desc_history),
            icon = { Icon(painterResource(R.drawable.history), null) },
            onClick = { navController.navigate("settings/listening_history") },
        )
    }
}

@Composable
fun PreferenceGroupScope.musoBackupRows(navController: NavController) {
    item {
        PreferenceEntry(
            title = { Text(stringResource(R.string.backup_restore)) },
            description = stringResource(R.string.settings_desc_backup),
            icon = { Icon(painterResource(R.drawable.backup), null) },
            onClick = { navController.navigate("settings/muso_backup") },
        )
    }
}
