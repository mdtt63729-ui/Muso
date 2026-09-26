package com.muso.music

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.content.ServiceConnection
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.imageLoader
import coil.request.ImageRequest
import com.valentinilk.shimmer.LocalShimmerTheme
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.models.SongItem
import com.muso.music.constants.AppBarHeight
import com.muso.music.constants.DarkModeKey
import com.muso.music.constants.DefaultOpenTabKey
import com.muso.music.constants.AnimationsEnabledKey
import com.muso.music.constants.CustomThemeColorKey
import com.muso.music.constants.LastAutoBackupKey
import com.muso.music.constants.AutoBackupFrequencyKey
import com.muso.music.constants.AutoBackupFrequency
import com.muso.music.constants.AutoBackupKey
import com.muso.music.constants.DisableScreenshotKey
import com.muso.music.constants.DynamicThemeKey
import com.muso.music.constants.MiniPlayerHeight
import com.muso.music.constants.NavigationBarAnimationSpec
import com.muso.music.constants.NavigationBarHeight
import com.muso.music.constants.PauseSearchHistoryKey
import com.muso.music.constants.PureBlackKey
import com.muso.music.constants.SearchSource
import com.muso.music.constants.SearchSourceKey
import com.muso.music.constants.StopMusicOnTaskClearKey
import com.muso.music.db.MusicDatabase
import com.muso.music.db.entities.SearchHistory
import com.muso.music.extensions.toEnum
import com.muso.music.playback.DownloadUtil
import com.muso.music.playback.MusicService
import com.muso.music.playback.MusicService.MusicBinder
import com.muso.music.playback.PlayerConnection
import com.muso.music.ui.component.BottomSheetMenu
import com.muso.music.ui.component.BounceIconButton
import com.muso.music.ui.component.UpdateDialog
import com.muso.music.ui.component.IconButton
import com.muso.music.ui.component.LocalMenuState
import com.muso.music.ui.component.SearchBar
import com.muso.music.ui.component.rememberBottomSheetState
import com.muso.music.ui.component.shimmer.ShimmerTheme
import com.muso.music.ui.menu.YouTubeSongMenu
import com.muso.music.ui.player.BottomSheetPlayer
import com.muso.music.ui.screens.MusoSplash
import com.muso.music.constants.HighRefreshRateKey
import com.muso.music.ui.screens.Screens
import com.muso.music.ui.screens.splashAlreadyShown
import com.muso.music.ui.screens.navigationBuilder
import com.muso.music.ui.screens.search.LocalSearchScreen
import com.muso.music.ui.screens.search.OnlineSearchScreen
import com.muso.music.ui.screens.settings.DarkMode
import com.muso.music.ui.screens.settings.NavigationTab
import com.muso.music.ui.theme.ColorSaver
import com.muso.music.ui.theme.DefaultThemeColor
import com.muso.music.ui.theme.InnerTuneTheme
import com.muso.music.ui.theme.extractThemeColor
import com.muso.music.ui.utils.appBarScrollBehavior
import com.muso.music.ui.utils.backToMain
import com.muso.music.ui.utils.resetHeightOffset
import com.muso.music.utils.Updater
import com.muso.music.utils.AutoBackup
import android.content.res.Configuration
import com.muso.music.constants.AppLanguageKey
import com.muso.music.constants.SYSTEM_DEFAULT
import java.util.Locale
import kotlinx.coroutines.runBlocking
import com.muso.music.utils.dataStore
import androidx.datastore.preferences.core.edit
import com.muso.music.utils.get
import com.muso.music.utils.rememberEnumPreference
import com.muso.music.utils.rememberPreference
import com.muso.music.constants.TranslucentNavigationBarKey
import com.muso.music.utils.reportException
import com.muso.music.utils.urlEncode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.delay

// Echo's emphasized easing for page transitions.
val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var downloadUtil: DownloadUtil

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MusicBinder) {
                playerConnection = PlayerConnection(this@MainActivity, service, database, lifecycleScope)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playerConnection?.dispose()
            playerConnection = null
        }
    }

    private var latestVersionName by mutableStateOf(BuildConfig.VERSION_NAME)

    override fun onStart() {
        super.onStart()
        startService(Intent(this, MusicService::class.java))
        bindService(Intent(this, MusicService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        unbindService(serviceConnection)
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (dataStore.get(StopMusicOnTaskClearKey, false) && playerConnection?.isPlaying?.value == true && isFinishing) {
            stopService(Intent(this, MusicService::class.java))
            unbindService(serviceConnection)
            playerConnection = null
        }
    }

    /**
     * In-app language switch: when a language (other than "System default") is picked in
     * settings, the whole activity is recreated over a configuration context carrying
     * that locale, so every stringResource follows it. Runs before anything is created,
     * so the read is a tiny blocking DataStore read.
     */
    override fun attachBaseContext(newBase: Context) {
        val language = runBlocking { newBase.dataStore.get(AppLanguageKey, SYSTEM_DEFAULT) }
        super.attachBaseContext(
            if (language != SYSTEM_DEFAULT) {
                val locale = Locale.forLanguageTag(language)
                Locale.setDefault(locale)
                val config = Configuration(newBase.resources.configuration)
                config.setLocale(locale)
                newBase.createConfigurationContext(config)
            } else {
                newBase
            }
        )
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        lifecycleScope.launch {
            dataStore.data
                .map { it[DisableScreenshotKey] ?: false }
                .distinctUntilChanged()
                .collectLatest {
                    if (it) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
        }

        // High refresh rate (Echo appearance): pick the fastest display mode matching
        // the current resolution. Re-applied whenever the setting changes.
        fun applyHighRefreshRate() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                runCatching {
                    val mode = display?.supportedModes?.maxByOrNull { it.refreshRate } ?: return
                    window.attributes = window.attributes.apply {
                        preferredDisplayModeId = mode.modeId
                    }
                }
            }
        }
        lifecycleScope.launch {
            dataStore.data.collectLatest { settings ->
                if (settings[HighRefreshRateKey] == true) applyHighRefreshRate()
            }
        }

        setContent {
            LaunchedEffect(Unit) {
                if (System.currentTimeMillis() - Updater.lastCheckTime > 1.days.inWholeMilliseconds) {
                    Updater.getLatestVersionName().onSuccess {
                        latestVersionName = it
                    }
                }
            }

            // Ultra-premium waveform splash: plays once per process, over the main UI.
            var showSplash by remember { mutableStateOf(!splashAlreadyShown) }

            val enableDynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = true)
            val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
            val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
            val customThemeColor by rememberPreference(CustomThemeColorKey, defaultValue = 0)
            val isSystemInDarkTheme = isSystemInDarkTheme()
            val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
                if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
            }
            LaunchedEffect(useDarkTheme) {
                setSystemBarAppearance(useDarkTheme)
            }
            var themeColor by rememberSaveable(stateSaver = ColorSaver) {
                mutableStateOf(DefaultThemeColor)
            }


            LaunchedEffect(playerConnection, enableDynamicTheme, isSystemInDarkTheme, customThemeColor) {
                val playerConnection = playerConnection
                if (!enableDynamicTheme || playerConnection == null) {
                    // SimpMusic-style custom theme color: used only when the artwork-based
                    // dynamic theme is off, so the two never fight over the palette.
                    themeColor = if (customThemeColor != 0) Color(customThemeColor) else DefaultThemeColor
                    return@LaunchedEffect
                }
                playerConnection.service.currentMediaMetadata.collectLatest { song ->
                    themeColor = if (song != null) {
                        withContext(Dispatchers.IO) {
                            val result = imageLoader.execute(
                                ImageRequest.Builder(this@MainActivity)
                                    .data(song.thumbnailUrl)
                                    .allowHardware(false) // pixel access is not supported on Config#HARDWARE bitmaps
                                    .build()
                            )
                            (result.drawable as? BitmapDrawable)?.bitmap?.extractThemeColor() ?: DefaultThemeColor
                        }
                    } else DefaultThemeColor
                }
            }

            InnerTuneTheme(
                darkTheme = useDarkTheme,
                pureBlack = pureBlack,
                themeColor = themeColor
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    val focusManager = LocalFocusManager.current
                    val density = LocalDensity.current
                    val windowsInsets = WindowInsets.systemBars
                    val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }

                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val inSelectMode = navBackStackEntry?.savedStateHandle?.getStateFlow("inSelectMode", false)?.collectAsState()

                    val navigationItems = remember { Screens.MainScreens }
                    val defaultOpenTab = remember {
                        dataStore[DefaultOpenTabKey].toEnum(defaultValue = NavigationTab.HOME)
                    }
                    val tabOpenedFromShortcut = remember {
                        when (intent?.action) {
                            ACTION_SONGS, ACTION_ALBUMS, ACTION_PLAYLISTS -> NavigationTab.LIBRARY
                            else -> null
                        }
                    }
                    val topLevelScreens = listOf(
                        Screens.Home.route,
                        Screens.Library.route,
                        "settings"
                    )

                    val (query, onQueryChange) = rememberSaveable(stateSaver = TextFieldValue.Saver) {
                        mutableStateOf(TextFieldValue())
                    }
                    var active by rememberSaveable {
                        mutableStateOf(false)
                    }
                    val onActiveChange: (Boolean) -> Unit = { newActive ->
                        active = newActive
                        if (!newActive) {
                            focusManager.clearFocus()
                            if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                                onQueryChange(TextFieldValue())
                            }
                        }
                    }
                    var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)

                    val searchBarFocusRequester = remember { FocusRequester() }

                    val onSearch: (String) -> Unit = {
                        if (it.isNotEmpty()) {
                            onActiveChange(false)
                            navController.navigate("search/${it.urlEncode()}")
                            if (dataStore[PauseSearchHistoryKey] != true) {
                                database.query {
                                    insert(SearchHistory(query = it))
                                }
                            }
                        }
                    }

                    var openSearchImmediately: Boolean by remember {
                        mutableStateOf(intent?.action == ACTION_SEARCH)
                    }

                    val shouldShowSearchBar = remember(active, navBackStackEntry, inSelectMode?.value) {
                        (active ||
                                navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                                navBackStackEntry?.destination?.route?.startsWith("search/") == true) &&
                                inSelectMode?.value != true
                    }

                    // SimpMusic-style home: no search bar on the home tab - a header with the
                    // app name takes its place, keeping the small history + settings buttons.
                    val onHomeTop = remember(navBackStackEntry, active) {
                        !active && navBackStackEntry?.destination?.route == Screens.Home.route
                    }
                    val shouldShowNavigationBar = remember(navBackStackEntry, active) {
                        navBackStackEntry?.destination?.route == null ||
                                navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } && !active
                    }
                    val navigationBarHeight by animateDpAsState(
                        targetValue = if (shouldShowNavigationBar) NavigationBarHeight else 0.dp,
                        animationSpec = NavigationBarAnimationSpec,
                        label = ""
                    )

                    val (translucentNavBar, onTranslucentNavBarChange) = rememberPreference(TranslucentNavigationBarKey, defaultValue = false)

                    val playerBottomSheetState = rememberBottomSheetState(
                        dismissedBound = 0.dp,
                        collapsedBound = bottomInset + (if (shouldShowNavigationBar) NavigationBarHeight else 0.dp) + MiniPlayerHeight,
                        expandedBound = maxHeight,
                    )

                    val playerAwareWindowInsets = remember(bottomInset, shouldShowNavigationBar, playerBottomSheetState.isDismissed, translucentNavBar) {
                        var bottom = bottomInset
                        // With the translucent navigation bar the content scrolls behind it,
                        // so its height is no longer part of the content's bottom inset.
                        if (shouldShowNavigationBar && !translucentNavBar) bottom += NavigationBarHeight
                        if (!playerBottomSheetState.isDismissed) bottom += MiniPlayerHeight
                        windowsInsets
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                            .add(WindowInsets(top = AppBarHeight, bottom = bottom))
                    }

                    val searchBarScrollBehavior = appBarScrollBehavior(
                        canScroll = {
                            navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                    (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                        }
                    )
                    val topAppBarScrollBehavior = appBarScrollBehavior(
                        canScroll = {
                            navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                    (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                        }
                    )

                    LaunchedEffect(navBackStackEntry) {
                        if (navBackStackEntry?.destination?.route?.startsWith("search/") == true) {
                            val searchQuery = withContext(Dispatchers.IO) {
                                URLDecoder.decode(navBackStackEntry?.arguments?.getString("query")!!, "UTF-8")
                            }
                            onQueryChange(TextFieldValue(searchQuery, TextRange(searchQuery.length)))
                        } else if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                            onQueryChange(TextFieldValue())
                        }
                        searchBarScrollBehavior.state.resetHeightOffset()
                        topAppBarScrollBehavior.state.resetHeightOffset()
                    }
                    LaunchedEffect(active) {
                        if (active) {
                            searchBarScrollBehavior.state.resetHeightOffset()
                            topAppBarScrollBehavior.state.resetHeightOffset()
                        }
                    }

                    LaunchedEffect(playerConnection) {
                        val player = playerConnection?.player ?: return@LaunchedEffect
                        if (player.currentMediaItem == null) {
                            if (!playerBottomSheetState.isDismissed) {
                                playerBottomSheetState.dismiss()
                            }
                        } else {
                            if (playerBottomSheetState.isDismissed) {
                                playerBottomSheetState.collapseSoft()
                            }
                        }
                    }

                    DisposableEffect(playerConnection, playerBottomSheetState) {
                        val player = playerConnection?.player ?: return@DisposableEffect onDispose { }
                        val listener = object : Player.Listener {
                            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED && mediaItem != null && playerBottomSheetState.isDismissed) {
                                    playerBottomSheetState.collapseSoft()
                                }
                            }
                        }
                        player.addListener(listener)
                        onDispose {
                            player.removeListener(listener)
                        }
                    }

                    val coroutineScope = rememberCoroutineScope()
                    var sharedSong: SongItem? by remember {
                        mutableStateOf(null)
                    }
                    DisposableEffect(Unit) {
                        val listener = Consumer<Intent> { intent ->
                            val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)?.toUri() ?: return@Consumer
                            when (val path = uri.pathSegments.firstOrNull()) {
                                "playlist" -> uri.getQueryParameter("list")?.let { playlistId ->
                                    if (playlistId.startsWith("OLAK5uy_")) {
                                        coroutineScope.launch {
                                            YouTube.albumSongs(playlistId).onSuccess { songs ->
                                                songs.firstOrNull()?.album?.id?.let { browseId ->
                                                    navController.navigate("album/$browseId")
                                                }
                                            }.onFailure {
                                                reportException(it)
                                            }
                                        }
                                    } else {
                                        navController.navigate("online_playlist/$playlistId")
                                    }
                                }

                                "channel", "c" -> uri.lastPathSegment?.let { artistId ->
                                    navController.navigate("artist/$artistId")
                                }

                                else -> when {
                                    path == "watch" -> uri.getQueryParameter("v")
                                    uri.host == "youtu.be" -> path
                                    else -> null
                                }?.let { videoId ->
                                    coroutineScope.launch {
                                        withContext(Dispatchers.IO) {
                                            YouTube.queue(listOf(videoId))
                                        }.onSuccess {
                                            sharedSong = it.firstOrNull()
                                        }.onFailure {
                                            reportException(it)
                                        }
                                    }
                                }
                            }
                        }

                        addOnNewIntentListener(listener)
                        onDispose { removeOnNewIntentListener(listener) }
                    }

                    // Navigation motion (SimpMusic-style): spring-physics slides with a soft
                    // fade, shared-axis feel between tabs; an instant cut when animations are off.
                    val animationsEnabled by rememberPreference(AnimationsEnabledKey, defaultValue = true)

                    // === SimpMusic-style automatic backup: on start, if enabled and due,
                    // write the backup zip into the public Downloads folder. Runs fully off
                    // the main thread; failures are silent (best-effort, no crash paths).
                    val autoBackupContext = LocalContext.current
                    LaunchedEffect(Unit) {
                        runCatching {
                            val prefs = autoBackupContext.dataStore.data.first()
                            if (prefs[AutoBackupKey] != true) return@LaunchedEffect
                            val frequency = prefs[AutoBackupFrequencyKey].toEnum(AutoBackupFrequency.DAILY)
                            val intervalMillis = when (frequency) {
                                AutoBackupFrequency.DAILY -> 24L * 60L * 60L * 1000L
                                AutoBackupFrequency.WEEKLY -> 7L * 24L * 60L * 60L * 1000L
                            }
                            val last = prefs[LastAutoBackupKey] ?: 0L
                            if (System.currentTimeMillis() - last < intervalMillis) return@LaunchedEffect
                            if (AutoBackup.writeBackup(autoBackupContext, database)) {
                                autoBackupContext.dataStore.edit {
                                    it[LastAutoBackupKey] = System.currentTimeMillis()
                                }
                                Toast.makeText(
                                    autoBackupContext,
                                    R.string.auto_backup_saved,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    }

                    CompositionLocalProvider(
                        LocalDatabase provides database,
                        LocalContentColor provides contentColorFor(MaterialTheme.colorScheme.surface),
                        LocalPlayerConnection provides playerConnection,
                        LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                        LocalDownloadUtil provides downloadUtil,
                        LocalShimmerTheme provides ShimmerTheme
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = when (tabOpenedFromShortcut ?: defaultOpenTab) {
                                NavigationTab.HOME -> Screens.Home
                                NavigationTab.LIBRARY -> Screens.Library
                            }.route,
                            // Echo-style page motion: emphasized-easing slide + fade,
                            // direction-aware from the tab order; an instant cut when animations are off.
                            enterTransition = {
                                if (!animationsEnabled) {
                                    fadeIn(snap())
                                } else {
                                    val targetIndex = navigationItems.indexOfFirst { it.route == targetState.destination.route }
                                    val initialIndex = navigationItems.indexOfFirst { it.route == initialState.destination.route }
                                    if (targetIndex == -1 || targetIndex > initialIndex) {
                                        slideInHorizontally(tween(400, easing = EmphasizedEasing)) { it / 8 } +
                                                fadeIn(tween(400, easing = EmphasizedEasing))
                                    } else {
                                        slideInHorizontally(tween(400, easing = EmphasizedEasing)) { -it / 8 } +
                                                fadeIn(tween(400, easing = EmphasizedEasing))
                                    }
                                }
                            },
                            exitTransition = {
                                if (!animationsEnabled) {
                                    fadeOut(snap())
                                } else {
                                    val targetIndex = navigationItems.indexOfFirst { it.route == targetState.destination.route }
                                    val initialIndex = navigationItems.indexOfFirst { it.route == initialState.destination.route }
                                    if (targetIndex == -1 || targetIndex > initialIndex) {
                                        slideOutHorizontally(tween(400, easing = EmphasizedEasing)) { -it / 8 } +
                                                fadeOut(tween(400, easing = EmphasizedEasing))
                                    } else {
                                        slideOutHorizontally(tween(400, easing = EmphasizedEasing)) { it / 8 } +
                                                fadeOut(tween(400, easing = EmphasizedEasing))
                                    }
                                }
                            },
                            popEnterTransition = {
                                if (!animationsEnabled) {
                                    fadeIn(snap())
                                } else {
                                    val targetIndex = navigationItems.indexOfFirst { it.route == targetState.destination.route }
                                    val initialIndex = navigationItems.indexOfFirst { it.route == initialState.destination.route }
                                    if (initialIndex != -1 && initialIndex < targetIndex) {
                                        slideInHorizontally(tween(400, easing = EmphasizedEasing)) { it / 8 } +
                                                fadeIn(tween(400, easing = EmphasizedEasing))
                                    } else {
                                        slideInHorizontally(tween(400, easing = EmphasizedEasing)) { -it / 8 } +
                                                fadeIn(tween(400, easing = EmphasizedEasing))
                                    }
                                }
                            },
                            popExitTransition = {
                                if (!animationsEnabled) {
                                    fadeOut(snap())
                                } else {
                                    val targetIndex = navigationItems.indexOfFirst { it.route == targetState.destination.route }
                                    val initialIndex = navigationItems.indexOfFirst { it.route == initialState.destination.route }
                                    if (initialIndex != -1 && initialIndex < targetIndex) {
                                        slideOutHorizontally(tween(400, easing = EmphasizedEasing)) { -it / 8 } +
                                                fadeOut(tween(400, easing = EmphasizedEasing))
                                    } else {
                                        slideOutHorizontally(tween(400, easing = EmphasizedEasing)) { it / 8 } +
                                                fadeOut(tween(400, easing = EmphasizedEasing))
                                    }
                                }
                            },
                            modifier = Modifier
                                .nestedScroll(
                                    if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                                        navBackStackEntry?.destination?.route?.startsWith("search/") == true) {
                                        searchBarScrollBehavior.nestedScrollConnection
                                    } else {
                                        topAppBarScrollBehavior.nestedScrollConnection
                                    }
                                )
                        ) {
                            navigationBuilder(navController, topAppBarScrollBehavior, latestVersionName)
                        }

                        AnimatedVisibility(
                            visible = shouldShowSearchBar && !onHomeTop,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            SearchBar(
                                query = query,
                                onQueryChange = onQueryChange,
                                onSearch = onSearch,
                                active = active,
                                onActiveChange = onActiveChange,
                                scrollBehavior = searchBarScrollBehavior,
                                placeholder = {
                                    Text(
                                        text = stringResource(
                                            if (!active) R.string.search
                                            else when (searchSource) {
                                                SearchSource.LOCAL -> R.string.search_library
                                                SearchSource.ONLINE -> R.string.search_yt_music
                                            }
                                        )
                                    )
                                },
                                leadingIcon = {
                                    IconButton(
                                        onClick = {
                                            when {
                                                active -> onActiveChange(false)
                                                !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } -> {
                                                    navController.navigateUp()
                                                }

                                                else -> onActiveChange(true)
                                            }
                                        },
                                        onLongClick = {
                                            when {
                                                active -> {}
                                                !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } -> {
                                                    navController.backToMain()
                                                }

                                                else -> {}
                                            }
                                        }
                                    ) {
                                        Icon(
                                            painterResource(
                                                if (active || !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                                                    R.drawable.arrow_back
                                                } else {
                                                    R.drawable.search
                                                }
                                            ),
                                            contentDescription = null
                                        )
                                    }
                                },
                                trailingIcon = {
                                    if (active) {
                                        if (query.text.isNotEmpty()) {
                                            IconButton(
                                                onClick = { onQueryChange(TextFieldValue("")) }
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.close),
                                                    contentDescription = null
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                searchSource = searchSource.toggle()
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(
                                                    when (searchSource) {
                                                        SearchSource.LOCAL -> R.drawable.library_music
                                                        SearchSource.ONLINE -> R.drawable.language
                                                    }
                                                ),
                                                contentDescription = null
                                            )
                                        }
                                    } else if (navBackStackEntry?.destination?.route in topLevelScreens) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    navController.navigate("settings")
                                                }
                                        ) {
                                            BadgedBox(
                                                badge = {
                                                    if (latestVersionName != BuildConfig.VERSION_NAME) {
                                                        Badge()
                                                    }
                                                }
                                            ) {

                                                Icon(
                                                    painter = painterResource(R.drawable.settings),
                                                    contentDescription = null
                                                )
                                            }
                                        }
                                    }
                                },
                                focusRequester = searchBarFocusRequester,
                                modifier = Modifier.align(Alignment.TopCenter),
                            ) {
                                Crossfade(
                                    targetState = searchSource,
                                    label = "",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = if (!playerBottomSheetState.isDismissed) MiniPlayerHeight else 0.dp)
                                        .navigationBarsPadding()
                                ) { searchSource ->
                                    when (searchSource) {
                                        SearchSource.LOCAL -> LocalSearchScreen(
                                            query = query.text,
                                            navController = navController,
                                            onDismiss = { onActiveChange(false) }
                                        )

                                        SearchSource.ONLINE -> OnlineSearchScreen(
                                            query = query.text,
                                            onQueryChange = onQueryChange,
                                            navController = navController,
                                            onSearch = {
                                                navController.navigate("search/${it.urlEncode()}")
                                                if (dataStore[PauseSearchHistoryKey] != true) {
                                                    database.query {
                                                        insert(SearchHistory(query = it))
                                                    }
                                                }
                                            },
                                            onDismiss = { onActiveChange(false) }
                                        )
                                    }
                                }
                            }
                        }

                        // Permanent home header: the app name with the history and settings
                        // buttons, drawn on an opaque bar that never hides or scrolls away -
                        // content passes UNDER it instead of through it.
                        if (onHomeTop) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .windowInsetsPadding(WindowInsets.statusBars)
                                    .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
                            ) {
                                Text(
                                    text = "Muso",
                                    // Brand wordmark: Gochi Hand, weight 400, no effects.
                                    fontFamily = FontFamily(Font(R.font.gochi_hand)),
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 32.sp,
                                    modifier = Modifier.weight(1f),
                                )
                                BounceIconButton(
                                    onClick = { navController.navigate("history") },
                                    buttonSize = 44.dp,
                                ) {
                                    Icon(
                                        painterResource(R.drawable.history),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                                BounceIconButton(
                                    onClick = { navController.navigate("settings") },
                                    buttonSize = 48.dp,
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (latestVersionName != BuildConfig.VERSION_NAME) {
                                                Badge()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.settings),
                                            contentDescription = null,
                                        )
                                    }
                                }
                            }
                        }

                        BottomSheetPlayer(
                            state = playerBottomSheetState,
                            navController = navController
                        )

                        NavigationBar(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset {
                                    if (navigationBarHeight == 0.dp) {
                                        IntOffset(x = 0, y = (bottomInset + NavigationBarHeight).roundToPx())
                                    } else {
                                        val slideOffset = (bottomInset + NavigationBarHeight) * playerBottomSheetState.progress.coerceIn(0f, 1f)
                                        val hideOffset = (bottomInset + NavigationBarHeight) * (1 - navigationBarHeight / NavigationBarHeight)
                                        IntOffset(
                                            x = 0,
                                            y = (slideOffset + hideOffset).roundToPx()
                                        )
                                    }
                                },
                            containerColor = if (translucentNavBar) {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                        ) {
                            navigationItems.fastForEach { screen ->
                                NavigationBarItem(
                                    selected = screen.route != Screens.Search.route &&
                                            navBackStackEntry?.destination?.hierarchy?.any { it.route == screen.route } == true,
                                    icon = {
                                        Icon(
                                            painter = painterResource(screen.iconId),
                                            contentDescription = null
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(screen.titleId),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    onClick = {
                                        if (screen.route == Screens.Search.route) {
                                            // The Search entry is an action, not a destination:
                                            // open the search field like the ACTION_SEARCH intent does.
                                            // The focus request must be deferred to the next frame -
                                            // requesting it synchronously crashes with
                                            // "FocusRequester is not initialized" whenever the
                                            // SearchBar is not composed yet (e.g. on the home tab).
                                            onActiveChange(true)
                                            coroutineScope.launch {
                                                withFrameNanos { }
                                                // Let the search bar expansion settle before
                                                // pulling the keyboard up - both at once janks.
                                                delay(250)
                                                runCatching { searchBarFocusRequester.requestFocus() }
                                            }
                                        } else if (navBackStackEntry?.destination?.hierarchy?.any { it.route == screen.route } == true) {
                                            navBackStackEntry?.savedStateHandle?.set("scrollToTop", true)
                                            coroutineScope.launch {
                                                searchBarScrollBehavior.state.resetHeightOffset()
                                            }
                                        } else {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        var showUpdateDialog by rememberSaveable { mutableStateOf(false) }
                        var updateDismissedForVersion by rememberSaveable { mutableStateOf("") }
                        LaunchedEffect(latestVersionName) {
                            showUpdateDialog = latestVersionName != BuildConfig.VERSION_NAME &&
                                    updateDismissedForVersion != latestVersionName
                        }
                        if (showUpdateDialog) {
                            UpdateDialog(
                                version = latestVersionName,
                                onDismiss = {
                                    showUpdateDialog = false
                                    updateDismissedForVersion = latestVersionName
                                }
                            )
                        }

                        BottomSheetMenu(
                            state = LocalMenuState.current,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )

                        sharedSong?.let { song ->
                            playerConnection?.let { playerConnection ->
                                Dialog(
                                    onDismissRequest = { sharedSong = null },
                                    properties = DialogProperties(usePlatformDefaultWidth = false)
                                ) {
                                    Surface(
                                        modifier = Modifier.padding(24.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        color = AlertDialogDefaults.containerColor,
                                        tonalElevation = AlertDialogDefaults.TonalElevation
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            YouTubeSongMenu(
                                                song = song,
                                                navController = navController,
                                                onDismiss = { sharedSong = null }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    LaunchedEffect(shouldShowSearchBar, openSearchImmediately) {
                        if (shouldShowSearchBar && openSearchImmediately) {
                            onActiveChange(true)
                            delay(250)
                            runCatching { searchBarFocusRequester.requestFocus() }
                            openSearchImmediately = false
                        }
                    }

                    // Waveform splash overlay: cross-fades away into the app underneath.
                    if (showSplash) {
                        MusoSplash(
                            onFinish = {
                                showSplash = false
                                splashAlreadyShown = true
                            },
                        )
                    }
                }
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun setSystemBarAppearance(isDark: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView.rootView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
            // Immersive app: the status and navigation bars auto-hide; a swipe from the
            // edge brings them back transiently.
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            window.statusBarColor = (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            window.navigationBarColor = (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
    }

    companion object {
        const val ACTION_SEARCH = "com.muso.music.action.SEARCH"
        const val ACTION_SONGS = "com.muso.music.action.SONGS"
        const val ACTION_ALBUMS = "com.muso.music.action.ALBUMS"
        const val ACTION_PLAYLISTS = "com.muso.music.action.PLAYLISTS"
    }
}

val LocalDatabase = staticCompositionLocalOf<MusicDatabase> { error("No database provided") }
val LocalPlayerConnection = staticCompositionLocalOf<PlayerConnection?> { error("No PlayerConnection provided") }
val LocalPlayerAwareWindowInsets = compositionLocalOf<WindowInsets> { error("No WindowInsets provided") }
val LocalDownloadUtil = staticCompositionLocalOf<DownloadUtil> { error("No DownloadUtil provided") }
