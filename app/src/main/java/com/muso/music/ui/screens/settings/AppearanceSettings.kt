package com.muso.music.ui.screens.settings

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
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.muso.music.LocalPlayerAwareWindowInsets
import com.muso.music.R
import com.muso.music.constants.CropAlbumArtKey
import com.muso.music.constants.CustomThemeColorKey
import com.muso.music.constants.DarkModeKey
import com.muso.music.constants.DefaultOpenTabKey
import com.muso.music.constants.GridCellSize
import com.muso.music.constants.GridCellSizeKey
import com.muso.music.constants.HidePlayerSliderKey
import com.muso.music.constants.HidePlayerThumbnailKey
import com.muso.music.constants.HighRefreshRateKey
import com.muso.music.constants.LyricsAutoScrollKey
import com.muso.music.constants.LyricsBlurEnabledKey
import com.muso.music.constants.LyricsLineSpacingKey
import com.muso.music.constants.LyricsPosition
import com.muso.music.constants.LyricsRomanizationKey
import com.muso.music.constants.LyricsStyle
import com.muso.music.constants.LyricsStyleKey
import com.muso.music.constants.LyricsTextSizeKey
import com.muso.music.constants.LyricsTextPositionKey
import com.muso.music.constants.PlayerBackgroundStyle
import com.muso.music.constants.PlayerBackgroundStyleKey
import com.muso.music.constants.PlayerButtonsStyle
import com.muso.music.constants.PlayerButtonsStyleKey
import com.muso.music.constants.PlayerStyle
import com.muso.music.constants.PlayerStyleKey
import com.muso.music.constants.PureBlackKey
import com.muso.music.constants.RotatingArtworkKey
import com.muso.music.constants.ShowCachedPlaylistKey
import com.muso.music.constants.ShowCodecOnPlayerKey
import com.muso.music.constants.ShowDownloadedPlaylistKey
import com.muso.music.constants.ShowLikedPlaylistKey
import com.muso.music.constants.ShowUploadedPlaylistKey
import com.muso.music.constants.SliderStyle
import com.muso.music.constants.SliderStyleKey
import com.muso.music.constants.DynamicThemeKey
import com.muso.music.constants.TranslucentNavigationBarKey
import com.muso.music.ui.component.DefaultDialog
import com.muso.music.ui.component.EnumListPreference
import com.muso.music.ui.component.IconButton
import com.muso.music.ui.component.ListPreference
import com.muso.music.ui.component.PreferenceEntry
import com.muso.music.ui.component.PreferenceGroupTitle
import com.muso.music.ui.component.SwitchPreference
import com.muso.music.ui.utils.backToMain
import com.muso.music.utils.rememberEnumPreference
import com.muso.music.utils.rememberPreference
import me.saket.squiggles.SquigglySlider
import androidx.compose.foundation.layout.height

// SimpMusic-style theme color presets (Interface settings). 0 = default.
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

/**
 * Appearance settings: the SimpMusic Interface items plus the Echo Music Appearance
 * items, grouped the Echo way - Interface, Player, Lyrics and Auto playlists. Every
 * row writes a real DataStore preference that the UI actually reads.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (darkMode, onDarkModeChange) = rememberEnumPreference(key = DarkModeKey, defaultValue = DarkMode.AUTO)
    val (pureBlack, onPureBlackChange) = rememberPreference(key = PureBlackKey, defaultValue = false)
    val (dynamicTheme, onDynamicThemeChange) = rememberPreference(key = DynamicThemeKey, defaultValue = true)
    val (customThemeColor, onCustomThemeColorChange) = rememberPreference(key = CustomThemeColorKey, defaultValue = 0)
    val (translucentNavBar, onTranslucentNavBarChange) = rememberPreference(key = TranslucentNavigationBarKey, defaultValue = false)
    val (highRefreshRate, onHighRefreshRateChange) = rememberPreference(key = HighRefreshRateKey, defaultValue = false)
    val (defaultOpenTab, onDefaultOpenTabChange) = rememberEnumPreference(key = DefaultOpenTabKey, defaultValue = NavigationTab.HOME)
    val (gridCellSize, onGridCellSizeChange) = rememberEnumPreference(key = GridCellSizeKey, defaultValue = GridCellSize.SMALL)
    val (playerStyle, onPlayerStyleChange) = rememberEnumPreference(key = PlayerStyleKey, defaultValue = PlayerStyle.EXPRESSIVE)

    val (playerBackgroundStyle, onPlayerBackgroundStyleChange) = rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.BLURRED_ARTWORK,
    )
    val (sliderStyle, onSliderStyleChange) = rememberEnumPreference(key = SliderStyleKey, defaultValue = SliderStyle.SQUIGGLY)
    val (playerButtonsStyle, onPlayerButtonsStyleChange) = rememberEnumPreference(key = PlayerButtonsStyleKey, defaultValue = PlayerButtonsStyle.DEFAULT)
    val (hidePlayerSlider, onHidePlayerSliderChange) = rememberPreference(key = HidePlayerSliderKey, defaultValue = false)
    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) = rememberPreference(key = HidePlayerThumbnailKey, defaultValue = false)
    val (cropAlbumArt, onCropAlbumArtChange) = rememberPreference(key = CropAlbumArtKey, defaultValue = false)
    val (rotatingArtwork, onRotatingArtworkChange) = rememberPreference(key = RotatingArtworkKey, defaultValue = false)
    val (showCodecOnPlayer, onShowCodecOnPlayerChange) = rememberPreference(key = ShowCodecOnPlayerKey, defaultValue = true)

    val (lyricsStyle, onLyricsStyleChange) = rememberEnumPreference(key = LyricsStyleKey, defaultValue = LyricsStyle.APPLE_MUSIC)
    val (lyricsTextPosition, onLyricsTextPositionChange) = rememberEnumPreference(key = LyricsTextPositionKey, defaultValue = LyricsPosition.CENTER)
    val (lyricsTextSize, onLyricsTextSizeChange) = rememberPreference(key = LyricsTextSizeKey, defaultValue = 26)
    val (lyricsLineSpacing, onLyricsLineSpacingChange) = rememberPreference(key = LyricsLineSpacingKey, defaultValue = 1.3f)
    val (romanizeLyrics, onRomanizeLyricsChange) = rememberPreference(key = LyricsRomanizationKey, defaultValue = false)
    val (lyricsBlurEnabled, onLyricsBlurEnabledChange) = rememberPreference(key = LyricsBlurEnabledKey, defaultValue = true)
    val (lyricsAutoScroll, onLyricsAutoScrollChange) = rememberPreference(key = LyricsAutoScrollKey, defaultValue = true)

    val (showLikedPlaylist, onShowLikedPlaylistChange) = rememberPreference(key = ShowLikedPlaylistKey, defaultValue = true)
    val (showDownloadedPlaylist, onShowDownloadedPlaylistChange) = rememberPreference(key = ShowDownloadedPlaylistKey, defaultValue = true)
    val (showUploadedPlaylist, onShowUploadedPlaylistChange) = rememberPreference(key = ShowUploadedPlaylistKey, defaultValue = true)
    val (showCachedPlaylist, onShowCachedPlaylistChange) = rememberPreference(key = ShowCachedPlaylistKey, defaultValue = true)

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = if (darkMode == DarkMode.AUTO) isSystemInDarkTheme else darkMode == DarkMode.ON

    var showCustomColorDialog by remember { mutableStateOf(false) }
    var showSliderOptionDialog by rememberSaveable { mutableStateOf(false) }

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

    if (showSliderOptionDialog) {
        DefaultDialog(
            buttons = {
                TextButton(
                    onClick = { showSliderOptionDialog = false }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
            onDismiss = {
                showSliderOptionDialog = false
            }
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, if (sliderStyle == SliderStyle.DEFAULT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .clickable {
                            onSliderStyleChange(SliderStyle.DEFAULT)
                            showSliderOptionDialog = false
                        }
                        .padding(16.dp)
                ) {
                    var sliderValue by remember {
                        mutableFloatStateOf(0.5f)
                    }
                    Slider(
                        value = sliderValue,
                        valueRange = 0f..1f,
                        onValueChange = {
                            sliderValue = it
                        },
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {}
                                )
                            }
                    )

                    Text(
                        text = stringResource(R.string.default_),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, if (sliderStyle == SliderStyle.SQUIGGLY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .clickable {
                            onSliderStyleChange(SliderStyle.SQUIGGLY)
                            showSliderOptionDialog = false
                        }
                        .padding(16.dp)
                ) {
                    var sliderValue by remember {
                        mutableFloatStateOf(0.5f)
                    }
                    SquigglySlider(
                        value = sliderValue,
                        valueRange = 0f..1f,
                        onValueChange = {
                            sliderValue = it
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = stringResource(R.string.squiggly),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    val themeColorMode = when {
        dynamicTheme -> THEME_COLOR_MODE_WALLPAPER
        customThemeColor != 0 -> THEME_COLOR_MODE_CUSTOM
        else -> THEME_COLOR_MODE_DEFAULT
    }

    val scrollState = rememberScrollState()

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(scrollState),
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))
        Spacer(Modifier.height(64.dp))

        EnumListPreference(
            title = { Text(stringResource(R.string.now_playing_style)) },
            icon = { Icon(painterResource(R.drawable.play), null) },
            selectedValue = playerStyle,
            onValueSelected = onPlayerStyleChange,
            valueText = {
                when (it) {
                    PlayerStyle.SPOTIFY -> stringResource(R.string.player_style_spotify)
                    PlayerStyle.EXPRESSIVE -> stringResource(R.string.player_style_expressive)
                    PlayerStyle.APPLE -> stringResource(R.string.player_style_apple)
                }
            },
        )

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

        AnimatedVisibility(useDarkTheme) {
            SwitchPreference(
                title = { Text(stringResource(R.string.pure_black)) },
                icon = { Icon(painterResource(R.drawable.contrast), null) },
                checked = pureBlack,
                onCheckedChange = onPureBlackChange,
            )
        }

        SwitchPreference(
            title = { Text(stringResource(R.string.liquid_glass_effect)) },
            description = stringResource(R.string.liquid_glass_effect_desc),
            icon = { Icon(painterResource(R.drawable.tune), null) },
            checked = translucentNavBar && playerBackgroundStyle == PlayerBackgroundStyle.BLURRED_ARTWORK,
            onCheckedChange = { on ->
                onTranslucentNavBarChange(on)
                onPlayerBackgroundStyleChange(
                    if (on) PlayerBackgroundStyle.BLURRED_ARTWORK else PlayerBackgroundStyle.DEFAULT,
                )
            },
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.enable_high_refresh_rate)) },
            description = stringResource(R.string.enable_high_refresh_rate_desc),
            icon = { Icon(painterResource(R.drawable.tune), null) },
            checked = highRefreshRate,
            onCheckedChange = onHighRefreshRateChange,
        )

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

        // ============================ Interface ============================
        PreferenceGroupTitle(
            title = stringResource(R.string.user_interface),
        )

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

        SwitchPreference(
            title = { Text(stringResource(R.string.show_codec_on_player)) },
            icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
            checked = showCodecOnPlayer,
            onCheckedChange = onShowCodecOnPlayerChange,
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.player_slider_style)) },
            description = when (sliderStyle) {
                SliderStyle.DEFAULT -> stringResource(R.string.default_)
                SliderStyle.SQUIGGLY -> stringResource(R.string.squiggly)
            },
            icon = { Icon(painterResource(R.drawable.sliders), null) },
            onClick = { showSliderOptionDialog = true },
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.hide_player_slider)) },
            icon = { Icon(painterResource(R.drawable.sliders), null) },
            checked = hidePlayerSlider,
            onCheckedChange = onHidePlayerSliderChange,
        )

        EnumListPreference(
            title = { Text(stringResource(R.string.player_buttons_style)) },
            icon = { Icon(painterResource(R.drawable.play), null) },
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

        SwitchPreference(
            title = { Text(stringResource(R.string.hide_player_thumbnail)) },
            icon = { Icon(painterResource(R.drawable.music_note), null) },
            checked = hidePlayerThumbnail,
            onCheckedChange = onHidePlayerThumbnailChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.crop_album_art)) },
            icon = { Icon(painterResource(R.drawable.crop), null) },
            checked = cropAlbumArt,
            onCheckedChange = onCropAlbumArtChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.rotating_artwork)) },
            description = stringResource(R.string.rotating_artwork_desc),
            icon = { Icon(painterResource(R.drawable.disc), null) },
            checked = rotatingArtwork,
            onCheckedChange = onRotatingArtworkChange,
        )

        // ============================ Player ============================
        PreferenceGroupTitle(
            title = stringResource(R.string.player),
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
            },
        )

        EnumListPreference(
            title = { Text(stringResource(R.string.lyrics_text_position)) },
            icon = { Icon(painterResource(R.drawable.format_align_center), null) },
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

        PreferenceEntry(
            title = { Text(stringResource(R.string.lyrics_line_spacing)) },
            description = "x%.1f".format(lyricsLineSpacing),
            icon = { Icon(painterResource(R.drawable.format_align_center), null) },
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

        SwitchPreference(
            title = { Text(stringResource(R.string.lyrics_auto_scroll)) },
            description = stringResource(R.string.lyrics_auto_scroll_desc),
            icon = { Icon(painterResource(R.drawable.sync), null) },
            checked = lyricsAutoScroll,
            onCheckedChange = onLyricsAutoScrollChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.lyrics_blur)) },
            description = stringResource(R.string.lyrics_blur_desc),
            icon = { Icon(painterResource(R.drawable.palette), null) },
            checked = lyricsBlurEnabled,
            onCheckedChange = onLyricsBlurEnabledChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.romanize_lyrics)) },
            description = stringResource(R.string.romanize_lyrics_desc),
            icon = { Icon(painterResource(R.drawable.translate), null) },
            checked = romanizeLyrics,
            onCheckedChange = onRomanizeLyricsChange,
        )

        // ============================ Lyrics ============================
        PreferenceGroupTitle(
            title = stringResource(R.string.lyrics),
        )

        // ============================ Auto playlists ============================
        PreferenceGroupTitle(
            title = stringResource(R.string.auto_playlists),
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.show_liked_playlist)) },
            icon = { Icon(painterResource(R.drawable.favorite), null) },
            checked = showLikedPlaylist,
            onCheckedChange = onShowLikedPlaylistChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.show_downloaded_playlist)) },
            icon = { Icon(painterResource(R.drawable.download), null) },
            checked = showDownloadedPlaylist,
            onCheckedChange = onShowDownloadedPlaylistChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.show_uploaded_playlist)) },
            icon = { Icon(painterResource(R.drawable.upload), null) },
            checked = showUploadedPlaylist,
            onCheckedChange = onShowUploadedPlaylistChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.show_cached_playlist)) },
            icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
            checked = showCachedPlaylist,
            onCheckedChange = onShowCachedPlaylistChange,
        )
    }

    TopAppBar(
        title = {
            // Echo-style collapse: the big in-content title hands over to the top bar while scrolling.
                            Text(stringResource(R.string.appearance))

        },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

enum class DarkMode {
    ON, OFF, AUTO
}

enum class NavigationTab {
    HOME, LIBRARY
}

enum class PlayerTextAlignment {
    SIDED, CENTER
}
