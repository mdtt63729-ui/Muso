package com.muso.music.ui.screens

import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.toRoute
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.maxrave.simpmusic.ui.navigation.destination.home.HomeDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDestination
import com.maxrave.simpmusic.ui.navigation.destination.search.SearchDestination
import com.muso.music.ui.screens.artist.ArtistItemsScreen
import com.muso.music.ui.screens.artist.ArtistScreen
import com.muso.music.ui.screens.artist.ArtistSongsScreen
import com.muso.music.ui.screens.library.AutoPlaylistScreen
import com.muso.music.ui.screens.library.UploadedScreen
import com.muso.music.ui.screens.library.PodcastsScreen
import com.muso.music.ui.screens.settings.SpotifyLoginScreen
import com.muso.music.ui.screens.settings.SpotifySettings
import com.muso.music.ui.screens.settings.AISettings
import com.muso.music.ui.screens.settings.ListeningHistorySettings
import com.muso.music.ui.screens.library.PodcastScreen
import com.muso.music.ui.screens.library.CachedScreen
import com.muso.music.ui.screens.library.LibraryAlbumsScreen
import com.muso.music.ui.screens.library.LibraryScreen
import com.muso.music.ui.screens.library.LibraryArtistsScreen
import com.muso.music.ui.screens.library.LibraryPlaylistsScreen
import com.muso.music.ui.screens.library.LibrarySongsScreen
import com.muso.music.ui.screens.playlist.LocalPlaylistScreen
import com.muso.music.ui.screens.playlist.OnlinePlaylistScreen
import com.muso.music.ui.screens.search.OnlineSearchResult
import com.muso.music.ui.screens.settings.AboutScreen
import com.muso.music.ui.screens.settings.BackupAndRestore
import com.muso.music.ui.screens.settings.DiscordLoginScreen
import com.muso.music.ui.screens.settings.DiscordSettings
import com.muso.music.ui.screens.settings.AudioEffectsSettings

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
    onOpenSearch: () -> Unit = {},
) {
    // The three tab routes are SimpMusic's own type-safe destinations now —
    // the suite navigation bar navigates with these objects.
    composable<HomeDestination> {
        HomeScreen(navController)
    }
    // Muso's search is the SearchBar overlay, not a destination: this proxy
    // opens the overlay (exactly what the old bar's Search entry did) and
    // immediately pops itself with instant transitions.
    composable<SearchDestination>(
        enterTransition = { fadeIn(snap()) },
        exitTransition = { fadeOut(snap()) },
        popEnterTransition = { fadeIn(snap()) },
        popExitTransition = { fadeOut(snap()) },
    ) {
        LaunchedEffect(Unit) {
            onOpenSearch()
            navController.popBackStack()
        }
        Box(Modifier.fillMaxSize())
    }
    composable(
        route = "auto_playlist/{playlist}",
        arguments = listOf(navArgument("playlist") { type = NavType.StringType }),
    ) {
        AutoPlaylistScreen(navController)
    }
    composable(
        route = "podcast/{podcastId}",
        arguments = listOf(navArgument("podcastId") { type = NavType.StringType }),
    ) {
        PodcastScreen(navController)
    }
    composable("uploaded") {
        UploadedScreen(navController)
    }
    composable("cached") {
        CachedScreen(navController)
    }
    composable<com.maxrave.simpmusic.ui.navigation.destination.player.FullscreenDestination> {
        com.muso.music.ui.player.FullscreenVideoScreen(navController)
    }
    composable<LibraryDestination> {
        LibraryScreen(navController)
    }
    composable(Screens.Songs.route) {
        LibrarySongsScreen(navController)
    }
    composable(Screens.Artists.route) {
        LibraryArtistsScreen(navController)
    }
    composable(Screens.Albums.route) {
        LibraryAlbumsScreen(navController)
    }
    composable(Screens.Playlists.route) {
        LibraryPlaylistsScreen(navController)
    }
    composable("history") {
        HistoryScreen(navController)
    }
    composable("stats") {
        StatsScreen(navController)
    }
    composable("mood_and_genres") {
        MoodAndGenresScreen(navController, scrollBehavior)
    }
    composable("account") {
        AccountScreen(navController, scrollBehavior)
    }
    composable("new_release") {
        NewReleaseScreen(navController, scrollBehavior)
    }
    composable(
        route = "search/{query}",
        arguments = listOf(
            navArgument("query") {
                type = NavType.StringType
            }
        ),
        enterTransition = {
            fadeIn(tween(250))
        },
        exitTransition = {
            if (targetState.destination.route?.startsWith("search/") == true) {
                fadeOut(tween(200))
            } else {
                fadeOut(tween(200)) + slideOutHorizontally { -it / 2 }
            }
        },
        popEnterTransition = {
            if (initialState.destination.route?.startsWith("search/") == true) {
                fadeIn(tween(250))
            } else {
                fadeIn(tween(250)) + slideInHorizontally { -it / 2 }
            }
        },
        popExitTransition = {
            fadeOut(tween(200))
        }
    ) {
        OnlineSearchResult(navController)
    }
    // The SimpMusic suite navigates with type-safe destination objects;
    // these thin proxies forward into Muso's own routes.
    composable<com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination> { entry ->
        val channelId = entry.toRoute<com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination>().channelId
        LaunchedEffect(channelId) {
            navController.navigate("artist/$channelId") { launchSingleTop = true }
            navController.popBackStack()
        }
    }
    composable<com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination> { entry ->
        val browseId = entry.toRoute<com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination>().browseId
        LaunchedEffect(browseId) {
            navController.navigate("album/$browseId") { launchSingleTop = true }
            navController.popBackStack()
        }
    }
    composable<com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination> { entry ->
        val destination = entry.toRoute<com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination>()
        LaunchedEffect(destination) {
            navController.navigate("online_playlist/${destination.playlistId}") { launchSingleTop = true }
            navController.popBackStack()
        }
    }
    composable(
        route = "album/{albumId}",
        arguments = listOf(
            navArgument("albumId") {
                type = NavType.StringType
            },
        )
    ) {
        AlbumScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            }
        )
    ) { backStackEntry ->
        val artistId = backStackEntry.arguments?.getString("artistId")!!
        if (artistId.startsWith("LA")) {
            ArtistSongsScreen(navController, scrollBehavior)
        } else {
            ArtistScreen(navController, scrollBehavior)
        }
    }
    composable(
        route = "artist/{artistId}/songs",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            }
        )
    ) {
        ArtistSongsScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}/items?browseId={browseId}?params={params}",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
            navArgument("browseId") {
                type = NavType.StringType
                nullable = true
            },
            navArgument("params") {
                type = NavType.StringType
                nullable = true
            }
        )
    ) {
        ArtistItemsScreen(navController, scrollBehavior)
    }
    composable(
        route = "online_playlist/{playlistId}",
        arguments = listOf(
            navArgument("playlistId") {
                type = NavType.StringType
            }
        )
    ) { entry ->
        // The SimpMusic playlist screen replaces Muso's own: hero header,
        // action cluster, track list, in-page search and selection mode.
        val playlistId = entry.arguments?.getString("playlistId") ?: ""
        com.maxrave.simpmusic.ui.theme.ForceDarkContent {
            com.maxrave.simpmusic.ui.screen.other.PlaylistScreen(
                playlistId = playlistId,
                isYourYouTubePlaylist = true,
                navController = navController,
            )
        }
    }
    composable(
        route = "local_playlist/{playlistId}",
        arguments = listOf(
            navArgument("playlistId") {
                type = NavType.StringType
            }
        )
    ) {
        LocalPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "youtube_browse/{browseId}?params={params}",
        arguments = listOf(
            navArgument("browseId") {
                type = NavType.StringType
                nullable = true
            },
            navArgument("params") {
                type = NavType.StringType
                nullable = true
            }
        )
    ) {
        YouTubeBrowseScreen(navController, scrollBehavior)
    }
    // ================= ArchiveTune settings system (Phase 2) =================
    // The kit hub replaces the old muso settings home; entry points that
    // navigate("settings") — navbar, SearchBar — land here unchanged.
    // Kit screens are invoked fully-qualified: the old muso screens keep
    // their names until Phase 3 removes them.
    composable("settings") {
        moe.rukamori.archivetune.KitSettingsHost {
            moe.rukamori.archivetune.ui.screens.settings.SettingsScreen(
                navController, latestVersionName,
            )
        }
    }
    composable("settings/account") {
        // Muso's own YouTube login flow (the kit AccountSettings needs the
        // kit HomeViewModel wiring — revisited in Phase 5).
        LoginScreen(navController)
    }
    composable("settings/hidden_playlists") {
        moe.rukamori.archivetune.KitSettingsHost {
            moe.rukamori.archivetune.ui.screens.settings.HiddenPlaylistsScreen(navController)
        }
    }
    composable("settings/appearance") {
        moe.rukamori.archivetune.KitSettingsHost {
            moe.rukamori.archivetune.ui.screens.settings.AppearanceSettings(navController)
        }
    }
    composable("settings/appearance/icon") {
        moe.rukamori.archivetune.KitSettingsHost {
            moe.rukamori.archivetune.ui.screens.settings.IconScreen(navController)
        }
    }
    composable("settings/appearance/aod_customized") {
        moe.rukamori.archivetune.KitSettingsHost {
            moe.rukamori.archivetune.ui.screens.settings.AodCustomizedScreen(navController)
        }
    }
    composable("settings/appearance/palette_picker") {
        moe.rukamori.archivetune.KitSettingsHost {
            moe.rukamori.archivetune.ui.screens.settings.PalettePickerScreen(navController)
        }
    }
    composable("settings/appearance/lyrics_animations") {
        moe.rukamori.archivetune.KitSettingsHost {
            moe.rukamori.archivetune.ui.screens.settings.LyricsAnimationSettings(navController)
        }
    }
    composable("settings/appearance/theme_creator") {
        moe.rukamori.archivetune.KitSettingsHost {
            moe.rukamori.archivetune.ui.screens.settings.ThemeCreatorScreen(navController)
        }
    }
    composable("settings/content") {
        moe.rukamori.archivetune.KitSettingsHost {
            moe.rukamori.archivetune.ui.screens.settings.ContentSettings(navController)
        }
    }
    composable("settings/lyrics") {
        moe.rukamori.archivetune.KitSettingsHost {
            moe.rukamori.archivetune.ui.screens.settings.LyricsSettings(navController)
        }
    }
    composable("settings/internet") {
        moe.rukamori.archivetune.KitSettingsHost {
            moe.rukamori.archivetune.ui.screens.settings.InternetSettings(navController)
        }
    }
    composable("settings/player") {
        moe.rukamori.archivetune.KitSettingsHost {
            moe.rukamori.archivetune.ui.screens.settings.PlayerSettings(navController)
        }
    }
    composable("settings/canvas") {
        moe.rukamori.archivetune.KitSettingsHost {
            moe.rukamori.archivetune.ui.screens.settings.CanvasSettings(navController)
        }
    }
    composable("settings/android_auto") {
        moe.rukamori.archivetune.KitSettingsHost {
            moe.rukamori.archivetune.ui.screens.settings.AndroidAutoSettings(navController)
        }
    }
    composable("settings/storage") {
        moe.rukamori.archivetune.KitSettingsHost {
            moe.rukamori.archivetune.ui.screens.settings.StorageSettings(navController)
        }
    }
    composable("settings/privacy") {
        moe.rukamori.archivetune.KitSettingsHost {
            moe.rukamori.archivetune.ui.screens.settings.PrivacySettings(navController)
        }
    }
    composable("settings/backup_restore") {
        moe.rukamori.archivetune.KitSettingsHost {
            moe.rukamori.archivetune.ui.screens.settings.BackupAndRestore(navController)
        }
    }
    composable("settings/discord") {
        moe.rukamori.archivetune.KitSettingsHost {
            moe.rukamori.archivetune.ui.screens.settings.DiscordSettings(navController)
        }
    }
    composable("settings/integration") {
        moe.rukamori.archivetune.KitSettingsHost {
            moe.rukamori.archivetune.ui.screens.settings.IntegrationScreen(navController)
        }
    }
    composable("settings/ai_integration") {
        moe.rukamori.archivetune.KitSettingsHost {
            moe.rukamori.archivetune.ui.screens.settings.AiIntegrationSettings(navController)
        }
    }
    composable("settings/music_together") {
        moe.rukamori.archivetune.KitSettingsHost {
            moe.rukamori.archivetune.ui.screens.settings.MusicTogetherScreen(navController)
        }
    }
    composable("settings/lastfm") {
        moe.rukamori.archivetune.KitSettingsHost {
            moe.rukamori.archivetune.ui.screens.settings.LastFMSettings(navController)
        }
    }
    composable("settings/discord/experimental") {
        moe.rukamori.archivetune.KitSettingsHost {
            moe.rukamori.archivetune.ui.screens.settings.DiscordExperimental(navController)
        }
    }
    composable("settings/misc") {
        moe.rukamori.archivetune.KitSettingsHost {
            moe.rukamori.archivetune.ui.screens.settings.DebugSettings(navController)
        }
    }
    composable("settings/logcat") {
        moe.rukamori.archivetune.KitSettingsHost {
            moe.rukamori.archivetune.ui.screens.settings.LogcatScreen(navController)
        }
    }
    composable("settings/update") {
        moe.rukamori.archivetune.KitSettingsHost {
            moe.rukamori.archivetune.ui.screens.settings.UpdateScreen(navController)
        }
    }
    composable(
        route = "settings/changelog?channel={channel}",
        arguments = listOf(
            navArgument("channel") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
        ),
    ) { backStackEntry ->
        moe.rukamori.archivetune.KitSettingsHost {
            val channelName = backStackEntry.arguments?.getString("channel")
            val channel = moe.rukamori.archivetune.constants.UpdateChannel
                .fromStoredName(channelName, moe.rukamori.archivetune.constants.UpdateChannel.STABLE)
            moe.rukamori.archivetune.ui.screens.settings.ChangelogScreen(navController, channel = channel)
        }
    }
    composable("settings/about") {
        // OUR About page stays (user spec).
        AboutScreen(navController, scrollBehavior)
    }
    composable("customize_background") {
        moe.rukamori.archivetune.KitSettingsHost {
            moe.rukamori.archivetune.ui.screens.settings.CustomizeBackground(navController)
        }
    }
    // Old muso-only settings routes, kept until Phase 3 removes the screens.
    composable("settings/audio_effects") {
        AudioEffectsSettings(navController, scrollBehavior)
    }
    composable("settings/muso_discord") {
        DiscordSettings(navController, scrollBehavior)
    }
    composable("settings/muso_backup") {
        BackupAndRestore(navController, scrollBehavior)
    }
    composable("settings/listening_history") {
        ListeningHistorySettings(navController, scrollBehavior)
    }
    composable("settings/ai") {
        AISettings(navController, scrollBehavior)
    }
    composable("settings/spotify") {
        SpotifySettings(navController, scrollBehavior)
    }
    composable("spotify_login") {
        SpotifyLoginScreen(navController)
    }
    composable("settings/discord/login") {
        DiscordLoginScreen(navController)
    }
    composable("login") {
        LoginScreen(navController)
    }
}