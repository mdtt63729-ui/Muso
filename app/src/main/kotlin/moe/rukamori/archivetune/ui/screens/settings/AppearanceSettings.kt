/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package moe.rukamori.archivetune.ui.screens.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import moe.rukamori.archivetune.LocalPlayerAwareWindowInsets
import com.muso.music.R
import moe.rukamori.archivetune.constants.AppFontPreference
import moe.rukamori.archivetune.constants.BackdropBlurAmountKey
import moe.rukamori.archivetune.constants.BackdropEnabledKey
import moe.rukamori.archivetune.constants.BlurRadiusKey
import moe.rukamori.archivetune.constants.ChipSortTypeKey
import moe.rukamori.archivetune.constants.CropThumbnailToSquareKey
import moe.rukamori.archivetune.constants.CustomFontNameKey
import moe.rukamori.archivetune.constants.CustomFontUriKey
import moe.rukamori.archivetune.constants.DarkModeKey
import moe.rukamori.archivetune.constants.DefaultLibraryFilterOrder
import moe.rukamori.archivetune.constants.DefaultLibraryFilterOrderPreference
import moe.rukamori.archivetune.constants.DefaultOpenTabKey
import moe.rukamori.archivetune.constants.DisableAnimationsKey
import moe.rukamori.archivetune.constants.DisableBlurKey
import moe.rukamori.archivetune.constants.DynamicThemeKey
import moe.rukamori.archivetune.constants.FontPreferenceKey
import moe.rukamori.archivetune.constants.ForceHighRefreshRateKey
import moe.rukamori.archivetune.constants.GridItemSize
import moe.rukamori.archivetune.constants.GridItemsSizeKey
import moe.rukamori.archivetune.constants.HidePlayerThumbnailKey
import moe.rukamori.archivetune.constants.LibraryChipOrderKey
import moe.rukamori.archivetune.constants.LibraryFilter
import moe.rukamori.archivetune.constants.LyricsBackgroundStyle
import moe.rukamori.archivetune.constants.LyricsBackgroundStyleKey
import moe.rukamori.archivetune.constants.MiniPlayerBackgroundStyle
import moe.rukamori.archivetune.constants.MiniPlayerBackgroundStyleKey
import moe.rukamori.archivetune.constants.PlayerBackgroundStyle
import moe.rukamori.archivetune.constants.PlayerBackgroundStyleKey
import moe.rukamori.archivetune.constants.PlayerButtonsStyle
import moe.rukamori.archivetune.constants.PlayerButtonsStyleKey
import moe.rukamori.archivetune.constants.PlayerDesignStyle
import moe.rukamori.archivetune.constants.PlayerDesignStyleKey
import moe.rukamori.archivetune.constants.PlaylistTagOrderKey
import moe.rukamori.archivetune.constants.PureBlackKey
import moe.rukamori.archivetune.constants.QuickPicksDisplayMode
import moe.rukamori.archivetune.constants.QuickPicksDisplayModeKey
import moe.rukamori.archivetune.constants.RandomThemeOnStartupKey
import moe.rukamori.archivetune.constants.ShowHomeCategoryChipsKey
import moe.rukamori.archivetune.constants.ShowPlayerVolumeBarKey
import moe.rukamori.archivetune.constants.ShowTagsInLibraryKey
import moe.rukamori.archivetune.constants.SliderStyle
import moe.rukamori.archivetune.constants.SliderStyleKey
import moe.rukamori.archivetune.constants.SwipeSensitivityKey
import moe.rukamori.archivetune.constants.SwipeThumbnailKey
import moe.rukamori.archivetune.constants.SwipeToSongKey
import moe.rukamori.archivetune.constants.ThumbnailCornerRadiusKey
import moe.rukamori.archivetune.constants.WallpaperExtractionFailedKey
import moe.rukamori.archivetune.constants.toLibraryFilterOrder
import moe.rukamori.archivetune.constants.toLibraryFilterPreference
import moe.rukamori.archivetune.constants.toPlaylistTagOrder
import moe.rukamori.archivetune.constants.toPlaylistTagPreference
import moe.rukamori.archivetune.ui.component.DefaultDialog
import moe.rukamori.archivetune.ui.component.EnumListPreference
import moe.rukamori.archivetune.ui.component.IconButton
import moe.rukamori.archivetune.ui.component.LibraryChipOrderDialog
import moe.rukamori.archivetune.ui.component.ListPreference
import moe.rukamori.archivetune.ui.component.PlaylistTagOrderDialog
import moe.rukamori.archivetune.ui.component.PreferenceEntry
import moe.rukamori.archivetune.ui.component.PreferenceGroup
import moe.rukamori.archivetune.ui.component.SwitchPreference
import com.muso.music.ui.screens.settings.musoThemeRows
import com.muso.music.ui.screens.settings.musoPlayerStyleRows
import com.muso.music.ui.screens.settings.musoVideoRows
import com.muso.music.ui.screens.settings.musoLayoutRows
import moe.rukamori.archivetune.ui.component.TagsManagementDialog
import moe.rukamori.archivetune.ui.component.ThumbnailCornerRadiusSelectorButton
import moe.rukamori.archivetune.ui.player.StyledPlaybackSlider
import moe.rukamori.archivetune.ui.theme.CustomFontLoader
import moe.rukamori.archivetune.ui.utils.backToMain
import moe.rukamori.archivetune.utils.isLowRamDevice
import moe.rukamori.archivetune.utils.rememberEnumPreference
import moe.rukamori.archivetune.utils.rememberPreference
import moe.rukamori.archivetune.viewmodels.PlaylistTagUiModel
import moe.rukamori.archivetune.viewmodels.PlaylistTagsScreenState
import moe.rukamori.archivetune.viewmodels.PlaylistTagsViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(navController: NavController) {
    val playlistTagsViewModel: PlaylistTagsViewModel = hiltViewModel()
    val playlistTagsState by playlistTagsViewModel.screenState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val defaultDisableAnimations = remember(context) { context.isLowRamDevice() }
    val (dynamicTheme, onDynamicThemeChange) =
        rememberPreference(
            DynamicThemeKey,
            defaultValue = true,
        )
    val (wallpaperExtractionFailed) =
        rememberPreference(
            WallpaperExtractionFailedKey,
            defaultValue = false,
        )
    val (randomThemeOnStartup, onRandomThemeOnStartupChange) =
        rememberPreference(
            RandomThemeOnStartupKey,
            defaultValue = false,
        )
    val (darkMode, onDarkModeChange) =
        rememberEnumPreference(
            DarkModeKey,
            defaultValue = DarkMode.AUTO,
        )
    val (playerDesignStyle, onPlayerDesignStyleChange) =
        rememberEnumPreference(
            PlayerDesignStyleKey,
            defaultValue = PlayerDesignStyle.V4,
        )
    val (showPlayerVolumeBar, onShowPlayerVolumeBarChange) =
        rememberPreference(
            ShowPlayerVolumeBarKey,
            defaultValue = true,
        )
    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) =
        rememberPreference(
            HidePlayerThumbnailKey,
            defaultValue = false,
        )
    val (thumbnailCornerRadius, onThumbnailCornerRadiusChange) =
        rememberPreference(
            key = ThumbnailCornerRadiusKey,
            defaultValue = 16f, // default dp
        )
    val (cropThumbnailToSquare, onCropThumbnailToSquareChange) =
        rememberPreference(
            CropThumbnailToSquareKey,
            defaultValue = false,
        )
    val (playerBackground, onPlayerBackgroundChange) =
        rememberEnumPreference(
            PlayerBackgroundStyleKey,
            defaultValue = PlayerBackgroundStyle.DEFAULT,
        )
    val (configuredLyricsBackground, onLyricsBackgroundChange) =
        rememberEnumPreference(
            LyricsBackgroundStyleKey,
            defaultValue = LyricsBackgroundStyle.DEFAULT,
        )
    val (miniPlayerBackground, onMiniPlayerBackgroundChange) =
        rememberEnumPreference(
            MiniPlayerBackgroundStyleKey,
            defaultValue = MiniPlayerBackgroundStyle.THEME,
        )
    val (pureBlack, onPureBlackChange) = rememberPreference(PureBlackKey, defaultValue = false)
    val (disableBlur, onDisableBlurChange) = rememberPreference(DisableBlurKey, defaultValue = false)
    val (disableAnimations, onDisableAnimationsChange) =
        rememberPreference(
            DisableAnimationsKey,
            defaultValue = defaultDisableAnimations,
        )
    val (forceHighRefreshRate, onForceHighRefreshRateChange) =
        rememberPreference(
            ForceHighRefreshRateKey,
            defaultValue = false,
        )
    val (blurRadius, onBlurRadiusChange) = rememberPreference(BlurRadiusKey, defaultValue = 48f)
    val (backdropEnabled, onBackdropEnabledChange) = rememberPreference(BackdropEnabledKey, defaultValue = true)
    val (backdropBlurAmount, onBackdropBlurAmountChange) = rememberPreference(BackdropBlurAmountKey, defaultValue = 60)
    val (fontPreference, onFontPreferenceChange) =
        rememberEnumPreference(
            FontPreferenceKey,
            defaultValue = AppFontPreference.DEFAULT,
        )
    val (customFontUri, onCustomFontUriChange) = rememberPreference(CustomFontUriKey, defaultValue = "")
    val (customFontName, onCustomFontNameChange) = rememberPreference(CustomFontNameKey, defaultValue = "")
    val (defaultOpenTab, onDefaultOpenTabChange) =
        rememberEnumPreference(
            DefaultOpenTabKey,
            defaultValue = NavigationTab.HOME,
        )
    val (playerButtonsStyle, onPlayerButtonsStyleChange) =
        rememberEnumPreference(
            PlayerButtonsStyleKey,
            defaultValue = PlayerButtonsStyle.DEFAULT,
        )
    val (sliderStyle, onSliderStyleChange) =
        rememberEnumPreference(
            SliderStyleKey,
            defaultValue = SliderStyle.Standard,
        )
    val (swipeThumbnail, onSwipeThumbnailChange) =
        rememberPreference(
            SwipeThumbnailKey,
            defaultValue = true,
        )
    val (swipeSensitivity, onSwipeSensitivityChange) =
        rememberPreference(
            SwipeSensitivityKey,
            defaultValue = 0.73f,
        )
    val (gridItemSize, onGridItemSizeChange) =
        rememberEnumPreference(
            GridItemsSizeKey,
            defaultValue = GridItemSize.SMALL,
        )

    val (swipeToSong, onSwipeToSongChange) =
        rememberPreference(
            SwipeToSongKey,
            defaultValue = false,
        )

    val (showTagsInLibrary, onShowTagsInLibraryChange) =
        rememberPreference(
            ShowTagsInLibraryKey,
            defaultValue = true,
        )
    val (showHomeCategoryChips, onShowHomeCategoryChipsChange) =
        rememberPreference(
            ShowHomeCategoryChipsKey,
            defaultValue = true,
        )
    val (libraryChipOrderPreference, onLibraryChipOrderChange) =
        rememberPreference(
            LibraryChipOrderKey,
            defaultValue = DefaultLibraryFilterOrderPreference,
        )
    val libraryChipOrder =
        remember(libraryChipOrderPreference) {
            libraryChipOrderPreference.toLibraryFilterOrder()
        }
    val (playlistTagOrderPreference, onPlaylistTagOrderChange) =
        rememberPreference(
            PlaylistTagOrderKey,
            defaultValue = "",
        )
    val availablePlaylistTags =
        (playlistTagsState as? PlaylistTagsScreenState.Success)?.tags.orEmpty()
    val playlistTagOrder =
        remember(availablePlaylistTags, playlistTagOrderPreference) {
            val tagsById = availablePlaylistTags.associateBy(PlaylistTagUiModel::id)
            playlistTagOrderPreference
                .toPlaylistTagOrder(availablePlaylistTags.map(PlaylistTagUiModel::id))
                .mapNotNull { tagId -> tagsById[tagId] }
        }
    val (quickPicksDisplayMode, onQuickPicksDisplayModeChange) =
        rememberEnumPreference(
            QuickPicksDisplayModeKey,
            defaultValue = QuickPicksDisplayMode.CARD,
        )

    val customFontPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            if (!CustomFontLoader.isSupportedTtf(context, uri)) {
                Toast.makeText(context, context.getString(R.string.custom_font_invalid), Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }

            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            if (customFontUri.isNotBlank() && customFontUri != uri.toString()) {
                runCatching {
                    context.contentResolver.releasePersistableUriPermission(
                        Uri.parse(customFontUri),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
            }

            onCustomFontUriChange(uri.toString())
            onCustomFontNameChange(CustomFontLoader.displayName(context, uri))
            onFontPreferenceChange(AppFontPreference.CUSTOM)
        }
    val pickCustomFont =
        remember(customFontPickerLauncher) {
            {
                customFontPickerLauncher.launch(CustomFontLoader.supportedMimeTypes)
            }
        }
    val onFontPreferenceSelected =
        remember(customFontUri, onFontPreferenceChange, pickCustomFont) {
            { value: AppFontPreference ->
                onFontPreferenceChange(value)
                if (value == AppFontPreference.CUSTOM && customFontUri.isBlank()) {
                    pickCustomFont()
                }
            }
        }

    val availableBackgroundStyles =
        PlayerBackgroundStyle.entries.filter {
            it != PlayerBackgroundStyle.BLUR || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        }
    val availableLyricsBackgroundStyles =
        remember {
            listOf(
                LyricsBackgroundStyle.DEFAULT,
                LyricsBackgroundStyle.FOLLOW_THEME,
                LyricsBackgroundStyle.COLORING,
            )
        }
    val lyricsBackground = configuredLyricsBackground.resolveFor(playerBackground)
    val isPlayerStyleCustomizationEnabled =
        when (playerDesignStyle) {
            PlayerDesignStyle.V7,
            PlayerDesignStyle.V8,
            PlayerDesignStyle.V9,
            PlayerDesignStyle.V10,
            -> false

            else -> true
        }
    val isVolumeBarSupported =
        playerDesignStyle == PlayerDesignStyle.V7 ||
            playerDesignStyle == PlayerDesignStyle.V8
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme =
        remember(darkMode, isSystemInDarkTheme) {
            if (darkMode == DarkMode.AUTO) isSystemInDarkTheme else darkMode == DarkMode.ON
        }

    val (defaultChip, onDefaultChipChange) =
        rememberEnumPreference(
            key = ChipSortTypeKey,
            defaultValue = LibraryFilter.LIBRARY,
        )
    val supportedHighestFps = rememberSupportedHighestFps()
    val isHighRefreshRateSupported = supportedHighestFps > HIGH_REFRESH_RATE_THRESHOLD_FPS

    ApplyRefreshRate(
        isEnabled = forceHighRefreshRate && isHighRefreshRateSupported,
        targetFps = supportedHighestFps,
    )

    var showSliderOptionDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showLibraryChipOrderDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showPlaylistTagOrderDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showTagsManagementDialog by rememberSaveable {
        mutableStateOf(false)
    }

    LaunchedEffect(isPlayerStyleCustomizationEnabled, playerBackground) {
        if (!isPlayerStyleCustomizationEnabled && playerBackground != PlayerBackgroundStyle.DEFAULT) {
            onPlayerBackgroundChange(PlayerBackgroundStyle.DEFAULT)
        }
    }

    LaunchedEffect(isPlayerStyleCustomizationEnabled) {
        if (!isPlayerStyleCustomizationEnabled) {
            showSliderOptionDialog = false
        }
    }

    if (showSliderOptionDialog && isPlayerStyleCustomizationEnabled) {
        val sliderStyles =
            remember {
                listOf(
                    SliderStyle.Standard,
                    SliderStyle.Wavy,
                    SliderStyle.Thick,
                    SliderStyle.Circular,
                    SliderStyle.Simple,
                )
            }
        DefaultDialog(
            buttons = {
                TextButton(
                    onClick = { showSliderOptionDialog = false },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
            onDismiss = {
                showSliderOptionDialog = false
            },
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                sliderStyles.chunked(3).forEach { styleRow ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        styleRow.forEach { style ->
                            SliderStyleOptionCard(
                                sliderStyle = style,
                                selected = sliderStyle == style,
                                onClick = {
                                    onSliderStyleChange(style)
                                    showSliderOptionDialog = false
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(3 - styleRow.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    if (showLibraryChipOrderDialog) {
        LibraryChipOrderDialog(
            initialOrder = libraryChipOrder,
            onDismiss = { showLibraryChipOrderDialog = false },
            onConfirm = { newOrder ->
                onLibraryChipOrderChange(newOrder.toLibraryFilterPreference())
                showLibraryChipOrderDialog = false
            },
        )
    }

    if (showPlaylistTagOrderDialog) {
        PlaylistTagOrderDialog(
            state = playlistTagsState,
            initialOrder = playlistTagOrder,
            onDismiss = { showPlaylistTagOrderDialog = false },
            onConfirm = { newOrder ->
                onPlaylistTagOrderChange(
                    newOrder.map(PlaylistTagUiModel::id).toPlaylistTagPreference(),
                )
                showPlaylistTagOrderDialog = false
            },
        )
    }

    if (showTagsManagementDialog) {
        TagsManagementDialog(
            onDismiss = { showTagsManagementDialog = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.appearance)) },
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
            )
        },
    ) { innerPadding ->
        val topPadding = innerPadding.calculateTopPadding()

        Column(
            Modifier
                .padding(top = topPadding)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState())
                .padding(bottom = SettingsDimensions.ScreenBottomPadding),
        ) {
            PreferenceGroup(title = stringResource(R.string.theme)) {
                musoThemeRows()

                /* muso-integration: kit dynamic theme row — replaced by the Muso-backed row below. */

                item(visible = dynamicTheme && Build.VERSION.SDK_INT < Build.VERSION_CODES.S && wallpaperExtractionFailed) {
                    PreferenceEntry(
                        title = { Text(stringResource(R.string.wallpaper_permission)) },
                        description = stringResource(R.string.wallpaper_permission_desc),
                        icon = { Icon(painterResource(R.drawable.storage), null) },
                        onClick = {
                            val intent =
                                android.content.Intent(
                                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    android.net.Uri.fromParts("package", context.packageName, null),
                                )
                            context.startActivity(intent)
                        },
                    )
                }

                item(visible = !dynamicTheme) {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.random_theme_on_startup)) },
                        description = stringResource(R.string.random_theme_on_startup_desc),
                        icon = { Icon(painterResource(R.drawable.shuffle), null) },
                        checked = randomThemeOnStartup,
                        onCheckedChange = onRandomThemeOnStartupChange,
                    )
                }

                /* muso-integration: kit color palette row (no Muso counterpart) */

                /* muso-integration: kit app icon row (no Muso counterpart) */

                /* muso-integration: kit dark theme row — replaced by the Muso-backed row below. */

                /* muso-integration: kit pure black row — replaced by the Muso-backed row below. */

                /* muso-integration: kit disable blur row (no Muso counterpart) */

                /* muso-integration: kit disable animations row — replaced by the Muso-backed row below. */

                /* muso-integration: kit high refresh rate row — replaced by the Muso-backed row below. */

                /* muso-integration: kit blur intensity row (no Muso counterpart) */

                /* muso-integration: kit album backdrop row (no Muso counterpart) */

                /* muso-integration: kit backdrop blur row (no Muso counterpart) */

                /* muso-integration: kit font preference row (no Muso counterpart) */

                /* muso-integration: kit custom font row (no Muso counterpart) */
            }

            PreferenceGroup(title = stringResource(R.string.player)) {
                musoPlayerStyleRows()
                musoVideoRows()

                /* muso-integration: kit player design row — replaced by the Muso-backed row below. */

                /* muso-integration: kit volume bar row (no Muso counterpart) */

                /* muso-integration: kit player background row — replaced by the Muso-backed row below. */

                /* muso-integration: kit lyrics background row (no Muso counterpart) */

                /* muso-integration: kit customized background row (no Muso counterpart) */

                /* muso-integration: kit mini player background row — replaced by the Muso-backed row below. */

                /* muso-integration: kit hide thumbnail row — replaced by the Muso-backed row below. */

                /* muso-integration: kit thumbnail corner radius row (no Muso counterpart) */

                /* muso-integration: kit crop thumbnail row — replaced by the Muso-backed row below. */

                /* muso-integration: kit AOD row (no Muso counterpart) */

                /* muso-integration: kit buttons style row — replaced by the Muso-backed row below. */

                /* muso-integration: kit slider style row — replaced by the Muso-backed row below. */

                /* muso-integration: kit swipe thumbnail row (no Muso counterpart) */

                /* muso-integration: kit swipe sensitivity row (no Muso counterpart) */
            }

            PreferenceGroup(title = stringResource(R.string.misc)) {
                musoLayoutRows()

                item {
                    EnumListPreference(
                        title = { Text(stringResource(R.string.quick_picks_display_mode)) },
                        icon = { Icon(painterResource(R.drawable.grid_view), null) },
                        selectedValue = quickPicksDisplayMode,
                        onValueSelected = onQuickPicksDisplayModeChange,
                        valueText = {
                            when (it) {
                                QuickPicksDisplayMode.CARD -> stringResource(R.string.quick_picks_display_mode_card)
                                QuickPicksDisplayMode.LIST -> stringResource(R.string.quick_picks_display_mode_list)
                            }
                        },
                    )
                }

                item {
                    EnumListPreference(
                        title = { Text(stringResource(R.string.default_open_tab)) },
                        icon = { Icon(painterResource(R.drawable.nav_bar), null) },
                        selectedValue = defaultOpenTab,
                        onValueSelected = onDefaultOpenTabChange,
                        valueText = {
                            when (it) {
                                NavigationTab.HOME -> stringResource(R.string.home)
                                NavigationTab.SEARCH -> stringResource(R.string.search)
                                NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                            }
                        },
                    )
                }

                item {
                    ListPreference(
                        title = { Text(stringResource(R.string.default_lib_chips)) },
                        icon = { Icon(painterResource(R.drawable.tab), null) },
                        selectedValue = defaultChip,
                        values = DefaultLibraryFilterOrder,
                        valueText = {
                            when (it) {
                                LibraryFilter.SONGS -> stringResource(R.string.songs)
                                LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                                LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                                LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                                LibraryFilter.PODCASTS -> stringResource(R.string.podcast)
                                LibraryFilter.SPOTIFY -> stringResource(R.string.spotify_playlists)
                                LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                            }
                        },
                        onValueSelected = onDefaultChipChange,
                    )
                }

                item {
                    PreferenceEntry(
                        title = { Text(stringResource(R.string.arrange_library_chips)) },
                        description = stringResource(R.string.arrange_library_chips_desc),
                        icon = { Icon(painterResource(R.drawable.tab), null) },
                        onClick = { showLibraryChipOrderDialog = true },
                    )
                }

                item {
                    PreferenceEntry(
                        title = { Text(stringResource(R.string.arrange_playlist_tags)) },
                        description = stringResource(R.string.arrange_playlist_tags_desc),
                        icon = { Icon(painterResource(R.drawable.style), null) },
                        onClick = { showPlaylistTagOrderDialog = true },
                    )
                }

                item {
                    PreferenceEntry(
                        title = { Text(stringResource(R.string.manage_playlist_tags)) },
                        description = stringResource(R.string.manage_playlist_tags_desc),
                        icon = { Icon(painterResource(R.drawable.style), null) },
                        onClick = { showTagsManagementDialog = true },
                    )
                }

                item {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.show_home_category_chips)) },
                        description = stringResource(R.string.show_home_category_chips_desc),
                        icon = { Icon(painterResource(R.drawable.home_outlined), null) },
                        checked = showHomeCategoryChips,
                        onCheckedChange = onShowHomeCategoryChipsChange,
                    )
                }

                item {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.show_tags_in_library)) },
                        description = stringResource(R.string.show_tags_in_library_desc),
                        icon = { Icon(painterResource(R.drawable.filter_alt), null) },
                        checked = showTagsInLibrary,
                        onCheckedChange = onShowTagsInLibraryChange,
                    )
                }

                item {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.swipe_song_to_add)) },
                        icon = { Icon(painterResource(R.drawable.swipe), null) },
                        checked = swipeToSong,
                        onCheckedChange = onSwipeToSongChange,
                    )
                }
            }
        }
    }
}

@Composable
fun ApplyRefreshRate(
    isEnabled: Boolean,
    targetFps: Float,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context) { context.findActivity() }
    val requestedFps = if (isEnabled) targetFps else DEFAULT_REFRESH_RATE_REQUEST

    DisposableEffect(view, activity, requestedFps) {
        applyRefreshRate(
            view = view,
            activity = activity,
            requestedFps = requestedFps,
        )

        onDispose {
            applyRefreshRate(
                view = view,
                activity = activity,
                requestedFps = DEFAULT_REFRESH_RATE_REQUEST,
            )
        }
    }
}

@Composable
private fun rememberSupportedHighestFps(): Float {
    val view = LocalView.current

    return remember(view) {
        val display = view.display
        display
            ?.supportedModes
            ?.maxOfOrNull { mode -> mode.refreshRate }
            ?: display?.refreshRate
            ?: DEFAULT_STANDARD_REFRESH_RATE_FPS
    }
}

private fun applyRefreshRate(
    view: View,
    activity: Activity?,
    requestedFps: Float,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        view.setRequestedFrameRate(requestedFps)
        return
    }

    activity?.window?.let { window ->
        val attributes = window.attributes
        if (attributes.preferredRefreshRate != requestedFps) {
            attributes.preferredRefreshRate = requestedFps
            window.attributes = attributes
        }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private const val HIGH_REFRESH_RATE_THRESHOLD_FPS = 60.5f
private const val DEFAULT_STANDARD_REFRESH_RATE_FPS = 60f
private const val DEFAULT_REFRESH_RATE_REQUEST = 0f

@Composable
private fun SliderStyleOptionCard(
    sliderStyle: SliderStyle,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var sliderValue by remember {
        mutableFloatStateOf(0.5f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
            modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    1.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(16.dp),
                ).clickable(onClick = onClick)
                .padding(16.dp),
    ) {
        StyledPlaybackSlider(
            sliderStyle = sliderStyle,
            value = sliderValue,
            valueRange = 0f..1f,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = {},
            activeColor = MaterialTheme.colorScheme.primary,
            isPlaying = true,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
        )

        Text(
            text = sliderStyleLabel(sliderStyle),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun sliderStyleLabel(sliderStyle: SliderStyle): String =
    when (sliderStyle) {
        SliderStyle.Standard -> stringResource(R.string.slider_style_standard)
        SliderStyle.Wavy -> stringResource(R.string.slider_style_wavy)
        SliderStyle.Thick -> stringResource(R.string.slider_style_thick)
        SliderStyle.Circular -> stringResource(R.string.slider_style_circular)
        SliderStyle.Simple -> stringResource(R.string.slider_style_simple)
    }

enum class DarkMode {
    ON,
    OFF,
    AUTO,
}

enum class NavigationTab {
    HOME,
    SEARCH,
    LIBRARY,
}

enum class PlayerTextAlignment {
    SIDED,
    CENTER,
}

enum class LyricsPosition {
    LEFT,
    CENTER,
    RIGHT,
}
