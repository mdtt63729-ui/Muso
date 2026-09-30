package com.muso.music

import com.muso.music.ui.player.MusoNavbarHost
import com.muso.music.constants.LiquidGlassNavBarKey
import com.maxrave.simpmusic.ui.navigation.destination.search.SearchDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.HomeDestination
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.expect.ui.layerBackdrop
import com.maxrave.simpmusic.ui.theme.LocalLiquidGlassEnabled
import androidx.compose.ui.graphics.luminance
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination
import androidx.compose.runtime.derivedStateOf
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import android.content.ServiceConnection
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.muso.music.ui.component.UpdatePopup
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
import com.muso.music.ui.navigation.iosEnter
import com.muso.music.ui.navigation.iosExit
import com.muso.music.ui.navigation.iosPopEnter
import com.muso.music.ui.navigation.iosPopExit
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
import com.muso.music.utils.UpdateNotification
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
import androidx.compose.foundation.LocalIndication
import com.muso.music.ui.animation.MotionIndication
import com.muso.music.constants.TranslucentNavigationBarKey
import com.muso.music.constants.UpdateDismissedVersionKey
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
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.delay
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog

// Echo's emphasized easing for page transitions.
val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

// Synchronous startup mirror for the in-app language (see attachBaseContext).
private const val STARTUP_PREFS = "muso_startup"
private const val APP_LANGUAGE_MIRROR = "appLanguage"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var downloadUtil: DownloadUtil

    // Kit onboarding flag reader (DataStore only - safe to construct eagerly
    // via Hilt field injection; used after the main UI renders).
    @Inject
    lateinit var onboardingRepository: moe.rukamori.archivetune.onboarding.OnboardingRepository

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

    // Set when the update notification is tapped: force-shows the update popup
    // even if that same version was previously dismissed with "Later".
    private var forceShowUpdate by mutableStateOf(false)

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
        // Startup fast path: the language is read from a tiny synchronous SharedPreferences
        // mirror instead of blocking DataStore I/O before the first frame. The mirror is
        // seeded once (first launch) and kept in sync by the language setting itself.
        val startupPrefs = newBase.getSharedPreferences(STARTUP_PREFS, Context.MODE_PRIVATE)
        val language = if (startupPrefs.contains(APP_LANGUAGE_MIRROR)) {
            startupPrefs.getString(APP_LANGUAGE_MIRROR, SYSTEM_DEFAULT) ?: SYSTEM_DEFAULT
        } else {
            runBlocking { newBase.dataStore.get(AppLanguageKey, SYSTEM_DEFAULT) }.also {
                startupPrefs.edit().putString(APP_LANGUAGE_MIRROR, it).apply()
            }
        }
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
        // Kill the cold-start flash: the window carries the splash's dark
        // background before Compose's first frame lands.
        window.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(0xFF101014.toInt())
        )

        // Launched by tapping the update notification: fetch the latest release
        // right away and bring the update popup straight up.
        handleUpdateNotificationTap(intent)

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
            // Ultra-premium waveform splash: plays once per process.
            var showSplash by remember { mutableStateOf(!splashAlreadyShown) }

            LaunchedEffect(showSplash) {
                // Check for a new release at most every 6 hours while the app is
                // running; when one is found, the in-app popup shows AND the update
                // notification is posted (once per version). The background worker
                // covers the time while the app is closed. Runs AFTER the splash:
                // the first Ktor/network use class-loads on the main thread, which
                // used to stall the animation frames.
                if (showSplash) return@LaunchedEffect
                if (System.currentTimeMillis() - Updater.lastCheckTime > 6.hours.inWholeMilliseconds) {
                    Updater.getLatestVersionName(force = true).onSuccess { latest ->
                        latestVersionName = latest
                        if (latest != BuildConfig.VERSION_NAME) {
                            UpdateNotification.notifyIfNew(this@MainActivity, latest)
                        }
                    }
                }
            }
            // The main UI is composed only when the splash asks for it (during its
            // quiet settled phase), or immediately when there is no splash. Composing
            // the whole app WHILE the animation plays is what froze the splash on real
            // devices: the startup composition burst is the heaviest main-thread work
            // of the entire launch, and the animation only gets frames when the main
            // thread is free.
            var composeMainUi by remember { mutableStateOf(!showSplash) }
            // The splash holds its settled frame until the main UI has
            // actually RENDERED, so the handoff never flashes black.
            var splashAnimationDone by remember { mutableStateOf(false) }
            var mainUiRendered by remember { mutableStateOf(!showSplash) }

            // Android 13+ requires asking before ANY notification can appear -
            // needed both for the music notification and the update notification.
            // Asked once the splash is over so it never interrupts the animation.
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }
            LaunchedEffect(showSplash) {
                if (
                    !showSplash &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            // One-time ask (Android 11+): grant All Files Access so the Muso
            // log folder is visible at /storage/emulated/0/Muso. Declining
            // keeps the logs inside Android/data (still fully functional).
            var showLogFolderDialog by remember { mutableStateOf(false) }
            LaunchedEffect(showSplash) {
                if (
                    !showSplash &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    !android.os.Environment.isExternalStorageManager() &&
                    !com.muso.music.utils.MusoLog.isPublicDirActive() &&
                    !com.muso.music.utils.MusoLog.storagePromptDone(this@MainActivity)
                ) {
                    showLogFolderDialog = true
                }
            }
            if (showLogFolderDialog) {
                AlertDialog(
                    onDismissRequest = {
                        com.muso.music.utils.MusoLog.markStoragePromptDone(this@MainActivity)
                        showLogFolderDialog = false
                    },
                    title = { Text(stringResource(R.string.log_folder_title)) },
                    text = { Text(stringResource(R.string.log_folder_desc)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                com.muso.music.utils.MusoLog.markStoragePromptDone(this@MainActivity)
                                showLogFolderDialog = false
                                runCatching {
                                    startActivity(
                                        android.content.Intent(
                                            android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                            android.net.Uri.parse("package:$packageName"),
                                        ),
                                    )
                                }
                            },
                        ) { Text(stringResource(R.string.log_folder_allow)) }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                com.muso.music.utils.MusoLog.markStoragePromptDone(this@MainActivity)
                                showLogFolderDialog = false
                            },
                        ) { Text(stringResource(R.string.cancel)) }
                    },
                )
            }

            if (composeMainUi) {
                // First-frame reporter: the splash's exit fade waits for this,
                // so home is genuinely on screen before the splash lets go.
                LaunchedEffect(Unit) {
                    withFrameNanos { }
                    withFrameNanos { }
                    mainUiRendered = true
                }
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

                    val defaultOpenTab = remember {
                        dataStore[DefaultOpenTabKey].toEnum(defaultValue = NavigationTab.HOME)
                    }
                    val tabOpenedFromShortcut = remember {
                        when (intent?.action) {
                            ACTION_SONGS, ACTION_ALBUMS, ACTION_PLAYLISTS -> NavigationTab.LIBRARY
                            else -> null
                        }
                    }

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
                            if (isTopLevelTab(navBackStackEntry?.destination)) {
                                onQueryChange(TextFieldValue())
                            }
                        }
                    }
                    var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)

                    // User spec: the search bar ON THE LIBRARY TAB searches ONLY the
                    // local library; everywhere else (including the navbar's Search
                    // button, which resets to ONLINE below) the chosen source applies.
                    val onLibraryTab = navBackStackEntry?.destination?.hasRoute(LibraryDestination::class) == true
                    val effectiveSearchSource = if (onLibraryTab) SearchSource.LOCAL else searchSource

                    val searchBarFocusRequester = remember { FocusRequester() }

                    val onSearch: (String) -> Unit = {
                        // LOCAL search (library tab, or the user's own LOCAL choice):
                        // results are already live below; submitting never goes online.
                        if (it.isNotEmpty() && effectiveSearchSource == SearchSource.ONLINE) {
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
                                isTopLevelTab(navBackStackEntry?.destination) ||
                                navBackStackEntry?.destination?.route?.startsWith("search/") == true) &&
                                inSelectMode?.value != true
                    }

                    // SimpMusic-style home: no search bar on the home tab - a header with the
                    // app name takes its place, keeping the small history + settings buttons.
                    val onHomeTop = remember(navBackStackEntry, active) {
                        !active && navBackStackEntry?.destination?.hasRoute(HomeDestination::class) == true
                    }
                    val shouldShowNavigationBar = remember(navBackStackEntry, active) {
                        navBackStackEntry?.destination?.route == null ||
                                isTopLevelTab(navBackStackEntry?.destination) && !active
                    }
                    val navigationBarHeight by animateDpAsState(
                        targetValue = if (shouldShowNavigationBar) NavigationBarHeight else 0.dp,
                        animationSpec = NavigationBarAnimationSpec,
                        label = ""
                    )

                    val (translucentNavBar, onTranslucentNavBarChange) = rememberPreference(TranslucentNavigationBarKey, defaultValue = false)
                    val liquidGlassNavBar by rememberPreference(LiquidGlassNavBarKey, defaultValue = false)

                    // One-time: M3 Expressive is the default player style now (user
                    // request). Only a stored CLASSIC (the old default) migrates; after
                    // this runs once, whatever the user picks is respected.
                    val migrationContext = androidx.compose.ui.platform.LocalContext.current
                    LaunchedEffect(Unit) {
                        val prefs = migrationContext.dataStore.data.first()
                        if (prefs[com.muso.music.constants.PlayerStyleMigratedKey] != true) {
                            migrationContext.dataStore.edit {
                                if (it[com.muso.music.constants.PlayerStyleKey] ==
                                    com.muso.music.constants.PlayerStyle.CLASSIC.name
                                ) {
                                    it[com.muso.music.constants.PlayerStyleKey] =
                                        com.muso.music.constants.PlayerStyle.EXPRESSIVE.name
                                }
                                it[com.muso.music.constants.PlayerStyleMigratedKey] = true
                            }
                        }
                    }

                    val playerBottomSheetState = rememberBottomSheetState(
                        dismissedBound = 0.dp,
                        collapsedBound = bottomInset + (if (shouldShowNavigationBar) NavigationBarHeight else 0.dp) + MiniPlayerHeight,
                        expandedBound = maxHeight,
                    )

                    // Never restore the main player sheet as expanded on app startup.
                    // The sheet is a transient UI surface, not a navigation destination;
                    // restoring its previous expanded anchor can make a fresh launch open
                    // directly into a blank/fullscreen player before any song is selected.
                    // This runs once for this Activity and does not affect intentional user
                    // expansion after the app is ready.
                    LaunchedEffect(playerBottomSheetState) {
                        playerBottomSheetState.snapTo(playerBottomSheetState.dismissedBound)
                    }

                    val playerAwareWindowInsets = remember(bottomInset, shouldShowNavigationBar, playerBottomSheetState.isDismissed, translucentNavBar, liquidGlassNavBar) {
                        var bottom = bottomInset
                        // With the translucent navigation bar the content scrolls behind it,
                        // so its height is no longer part of the content's bottom inset.
                        // Same for the floating glass bar + glass mini player: content
                        // scrolls behind both (reference behaviour), otherwise every list
                        // ended in a dead black strip under the mini player.
                        val behindFloatingGlass = translucentNavBar || liquidGlassNavBar
                        if (shouldShowNavigationBar && !behindFloatingGlass) bottom += NavigationBarHeight
                        // The floating pill mini player rides above the bar in BOTH
                        // modes now (glass and flat), so content always reserves
                        // its height - no list item hides behind the flat pill.
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
                    // The suite glass bar collapses while content is scrolled away from the top.
                    val isScrolledToTop by remember {
                        derivedStateOf { searchBarScrollBehavior.state.overlappedFraction == 0f }
                    }

                    LaunchedEffect(navBackStackEntry) {
                        if (navBackStackEntry?.destination?.route?.startsWith("search/") == true) {
                            val searchQuery = withContext(Dispatchers.IO) {
                                URLDecoder.decode(navBackStackEntry?.arguments?.getString("query")!!, "UTF-8")
                            }
                            onQueryChange(TextFieldValue(searchQuery, TextRange(searchQuery.length)))
                        } else if (isTopLevelTab(navBackStackEntry?.destination)) {
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
                    val animationsPref by rememberPreference(AnimationsEnabledKey, defaultValue = true)
                    // Motion System PRD §15: also respect the SYSTEM "remove
                    // animations" accessibility setting - when the OS animator
                    // scale is 0, every transition snaps to its final state.
                    // The app keeps working fully; only the movement goes away.
                    val systemAnimationsOn = remember {
                        android.provider.Settings.Global.getFloat(
                            contentResolver,
                            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                            1f,
                        ) != 0f
                    }
                    val animationsEnabled = animationsPref && systemAnimationsOn

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

                    // Liquid glass engine: the backdrop the floating bar and its
                    // MiniPlayer refract. Base colour must match the theme (white on
                    // light, black on dark) or the glass reads as a muddy overlay.
                    val isSuiteDarkTheme = MaterialTheme.colorScheme.surface.luminance() <= 0.5f
                    val glassBackdrop = rememberBackdrop(
                        if (!isSuiteDarkTheme) Color.White else Color.Black,
                    )
                    CompositionLocalProvider(
                        // Motion System PRD §4.2/§7.1: one app-wide press
                        // indication - every plain clickable scales to 0.97
                        // while pressed and springs back. Collapses to a
                        // no-op when animations are off (app toggle OR the
                        // system "remove animations" setting, PRD §15).
                        LocalIndication provides MotionIndication(animationsEnabled),
                        LocalDatabase provides database,
                        LocalContentColor provides contentColorFor(MaterialTheme.colorScheme.surface),
                        LocalPlayerConnection provides playerConnection,
                        LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                        LocalDownloadUtil provides downloadUtil,
                        LocalShimmerTheme provides ShimmerTheme,
                        LocalLiquidGlassEnabled provides liquidGlassNavBar,
                        // Suite theme locals (the SimpMusic components read these
                        // instead of MaterialTheme): they were never provided, so
                        // LocalIsDarkTheme stayed on its static default TRUE - the
                        // flat/glass navbar accents and the pill MiniPlayer always
                        // took the DARK variant, which read as light/dark REVERSED
                        // in light mode. Derive them from the live color scheme.
                        com.maxrave.simpmusic.ui.theme.LocalIsDarkTheme provides isSuiteDarkTheme,
                        com.maxrave.simpmusic.ui.theme.LocalAppColors provides
                            if (isSuiteDarkTheme) {
                                com.maxrave.simpmusic.ui.theme.DarkAppColors
                            } else {
                                com.maxrave.simpmusic.ui.theme.LightAppColors
                            },
                            // ArchiveTune kit locals are NOT provided at startup
                            // anymore: KitSettingsHost (around every kit screen in
                            // NavigationBuilder) provides them lazily. Startup must
                            // never construct the kit database / SyncUtils /
                            // DownloadUtil — the app always opens even if the kit
                            // graph fails (Round 169 crash-loop fix).
                    ) {
                        NavHost(
                            // The content layer the glass surfaces sample; the bar and
                            // its MiniPlayer stay SIBLINGS of this NavHost (never inside
                            // it — nesting a glass surface in its own source crashes the
                            // RuntimeShader with a render-feedback loop).
                            modifier = Modifier
                                .layerBackdrop(glassBackdrop)
                                .nestedScroll(
                                    if (isTopLevelTab(navBackStackEntry?.destination) ||
                                        navBackStackEntry?.destination?.route?.startsWith("search/") == true) {
                                        searchBarScrollBehavior.nestedScrollConnection
                                    } else {
                                        topAppBarScrollBehavior.nestedScrollConnection
                                    }
                                ),
                            navController = navController,
                            startDestination = when (tabOpenedFromShortcut ?: defaultOpenTab) {
                                NavigationTab.HOME -> HomeDestination
                                NavigationTab.LIBRARY -> LibraryDestination
                            },
                            // Echo-style page motion: emphasized-easing slide + fade,
                            // direction-aware from the tab order; an instant cut when animations are off.
                            // iOS-style page motion (NavigationTransitions.kt):
                            // 420/400ms push/pop, iOS cubic-bezier curve, incoming
                            // full-width slide, outgoing -30% parallax + dim, zero
                            // spring bounce. Top-level tabs crossfade (150ms) with
                            // a touch of scale instead of sliding. Glass ON keeps
                            // the lighter translucent-plate look; OFF adds the
                            // 0.96 settle scale — same timing, same smoothness.
                            // Android 14+ predictive back rides navigation-compose's
                            // built-in seekable support with the same motion.
                            enterTransition = { iosEnter(liquidGlassNavBar, animationsEnabled) },
                            exitTransition = { iosExit(liquidGlassNavBar, animationsEnabled) },
                            popEnterTransition = { iosPopEnter(liquidGlassNavBar, animationsEnabled) },
                            popExitTransition = { iosPopExit(liquidGlassNavBar, animationsEnabled) }
                        ) {
                            navigationBuilder(
                                navController,
                                topAppBarScrollBehavior,
                                latestVersionName,
                                onOpenSearch = {
                                    // Exactly what the old bar's Search entry did: open the
                                    // SearchBar overlay and pull the keyboard up once it settles.
                                    // The Search BUTTON always means ONLINE search (user spec) -
                                    // the library tab still forces LOCAL through onLibraryTab.
                                    searchSource = SearchSource.ONLINE
                                    onActiveChange(true)
                                    coroutineScope.launch {
                                        withFrameNanos { }
                                        delay(250)
                                        runCatching { searchBarFocusRequester.requestFocus() }
                                    }
                                },
                            )
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
                                            else when (effectiveSearchSource) {
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
                                                !isTopLevelTab(navBackStackEntry?.destination) -> {
                                                    navController.navigateUp()
                                                }

                                                else -> onActiveChange(true)
                                            }
                                        },
                                        onLongClick = {
                                            when {
                                                active -> {}
                                                !isTopLevelTab(navBackStackEntry?.destination) -> {
                                                    navController.backToMain()
                                                }

                                                else -> {}
                                            }
                                        }
                                    ) {
                                        Icon(
                                            painterResource(
                                                if (active || !isTopLevelTab(navBackStackEntry?.destination)) {
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
                                        // Library tab: library-only search, no source toggle.
                                        if (!onLibraryTab) IconButton(
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
                                    } else if (isTopLevelTab(navBackStackEntry?.destination) ||
                                        navBackStackEntry?.destination?.route == "settings") {
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
                                ) { effectiveSearchSource ->
                                    when (effectiveSearchSource) {
                                        SearchSource.LOCAL -> LocalSearchScreen(
                                            query = query.text,
                                            navController = navController,
                                            onDismiss = { onActiveChange(false) }
                                        )

                                        SearchSource.ONLINE -> OnlineSearchScreen(
                                            query = query.text,
                                            onQueryChange = onQueryChange,
                                            navController = navController,
                                            // Instant results (reference behaviour): the
                                            // debounced auto-search in the screen calls this;
                                            // it navigates WITHOUT writing search history, so
                                            // half-typed queries never pollute it.
                                            onAutoSearch = {
                                                navController.navigate("search/${it.urlEncode()}") {
                                                    launchSingleTop = true
                                                }
                                            },
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
                                    fontFamily = FontFamily(Font(R.font.josefin_sans)),
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
                            navController = navController,
                            // The glass bar draws its own MiniPlayer; avoid two of them.
                            // Both modes get the floating pill from the navbar host
                            // now (glass or flat variant); Muso's old strip mini
                            // player is gone.
                            // The old strip mini player is RETIRED everywhere (user
                            // spec): only the suite's two pill variants exist, and the
                            // navbar host keeps one alive on navbar-hidden screens too,
                            // so the collapsed sheet never paints anything of its own.
                            showCollapsedMiniPlayer = false,
                        )

                        // === SimpMusic floating navigation bar (PRD section 12) ============
                        // The old hand-built NavigationBar is gone: the suite's own glass
                        // (default) or flat capsule-and-FAB bar takes over, sliding under
                        // the player sheet and away on screens that hide it, exactly like
                        // the bar it replaces.
                        MusoNavbarHost(
                            backdrop = glassBackdrop,
                            navController = navController,
                            playerConnection = playerConnection,
                            playerBottomSheetState = playerBottomSheetState,
                            bottomInset = bottomInset,
                            visibleHeight = navigationBarHeight,
                            isScrolledToTop = isScrolledToTop,
                            onReloadTab = {
                                navBackStackEntry?.savedStateHandle?.set("scrollToTop", true)
                                coroutineScope.launch {
                                    searchBarScrollBehavior.state.resetHeightOffset()
                                }
                            },
                        )

                        // Premium Material 3 update popup: slides up from the bottom
                        // of the screen when a newer GitHub release exists. "Later"
                        // persists the dismissed version in DataStore, so the popup
                        // never comes back for that version (only for the next one),
                        // and it is never shown over the splash animation.
                        val (updateDismissedVersion, onUpdateDismissedVersionChange) =
                            rememberPreference(UpdateDismissedVersionKey, defaultValue = "")
                        if (latestVersionName != BuildConfig.VERSION_NAME &&
                            (forceShowUpdate || updateDismissedVersion != latestVersionName) &&
                            !showSplash
                        ) {
                            UpdatePopup(
                                version = latestVersionName,
                                onDismiss = {
                                    onUpdateDismissedVersionChange(latestVersionName)
                                    forceShowUpdate = false
                                },
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

                    // Previous-crash report: if the app died last session, the saved
                    // stack trace is offered here so it can be screenshotted or shared
                    // without adb. Delete-on-show keeps it a one-time dialog.
                    val context = LocalContext.current
                    val lastCrashLog = remember {
                        runCatching {
                            val f = java.io.File(context.filesDir, "crash.log")
                            if (f.exists()) f.readText() else null
                        }.getOrNull()
                    }
                    if (lastCrashLog != null) {
                        AlertDialog(
                            onDismissRequest = {},
                            title = { Text(stringResource(R.string.crash_report_title)) },
                            text = {
                                Text(
                                    text = lastCrashLog.takeLast(1200),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        runCatching { java.io.File(context.filesDir, "crash.log").delete() }
                                        // Round 169: share the FULL log files from the
                                        // Muso folder (crash_log_N.txt, crash_log.txt,
                                        // main.txt) via FileProvider - no storage
                                        // permission needed; fall back to plain text.
                                        val shared =
                                            runCatching {
                                                context.startActivity(
                                                    com.muso.music.utils.MusoLog.shareLogsIntent(context)
                                                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                                                )
                                            }.isSuccess
                                        if (!shared) {
                                            runCatching {
                                                context.startActivity(
                                                    android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(
                                                            android.content.Intent.EXTRA_TEXT,
                                                            lastCrashLog,
                                                        )
                                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                )
                                            }
                                        }
                                    },
                                ) { Text(stringResource(R.string.share)) }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        runCatching { java.io.File(context.filesDir, "crash.log").delete() }
                                    },
                                ) { Text(stringResource(R.string.close)) }
                            },
                        )
                    }
                    // --- ArchiveTune onboarding (Phase 4, kit port) ---------------
                    // Round 170 fix: the onboarding used to construct its
                    // HiltViewModel + kit DataStore flow DURING the startup
                    // composition (the heaviest, most fragile moment of the
                    // app). It now waits until the main UI has fully rendered,
                    // reads the completion flag once (guarded - any failure
                    // simply skips the onboarding), and only then composes the
                    // kit screen. If the onboarding itself ever fails, the app
                    // is already open, the crash is captured by MusoLog and
                    // shown in the crash dialog on the next launch.
                    var showOnboarding by remember { mutableStateOf(false) }
                    LaunchedEffect(mainUiRendered) {
                        if (!mainUiRendered) return@LaunchedEffect
                        val shouldShow =
                            runCatching {
                                onboardingRepository.observeShouldShowOnboarding().first()
                            }.getOrDefault(false)
                        if (shouldShow) showOnboarding = true
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showOnboarding,
                        enter = fadeIn(tween(220)),
                        exit = fadeOut(tween(260)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(androidx.compose.ui.graphics.Color.Black),
                        ) {
                            moe.rukamori.archivetune.ui.screens.onboarding.OnboardingRoute(
                                onLoginRequested = { navController.navigate("login") },
                                onCompleted = { showOnboarding = false },
                            )
                        }
                    }
                }
            }

            } // composeMainUi

            // The splash overlay composes OUTSIDE the app's UI tree: it never competes
            // with the startup composition for frames, and it outlives the app's first
            // frame so the reveal is seamless.
            if (showSplash) {
                MusoSplash(
                    onContentNeeded = { composeMainUi = true },
                    onFinish = { splashAnimationDone = true },
                    contentRendered = { mainUiRendered },
                )
            }
            LaunchedEffect(splashAnimationDone, composeMainUi) {
                if (splashAnimationDone && composeMainUi) {
                    // One more frame so the freshly composed UI is genuinely on screen
                    // before the splash overlay is removed - no black flash, no pop.
                    withFrameNanos { }
                    showSplash = false
                    splashAlreadyShown = true
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Notification tap while the app was already running (singleTask).
        handleUpdateNotificationTap(intent)
    }

    private fun handleUpdateNotificationTap(intent: Intent?) {
        if (intent?.getBooleanExtra(UpdateNotification.EXTRA_SHOW_UPDATE, false) != true) return
        lifecycleScope.launch {
            Updater.getLatestVersionName(force = true).onSuccess {
                latestVersionName = it
                forceShowUpdate = true
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

// --- Suite navigation helpers -------------------------------------------------
// The tab routes are SimpMusic's type-safe destinations (HomeDestination /
// LibraryDestination); these replace the old string-route checks.
private fun isTopLevelTab(destination: NavDestination?): Boolean =
    destination?.hasRoute(HomeDestination::class) == true ||
            destination?.hasRoute(LibraryDestination::class) == true

private fun tabIndexOf(destination: NavDestination?): Int = when {
    destination?.hasRoute(HomeDestination::class) == true -> 0
    destination?.hasRoute(LibraryDestination::class) == true -> 1
    else -> -1
}
