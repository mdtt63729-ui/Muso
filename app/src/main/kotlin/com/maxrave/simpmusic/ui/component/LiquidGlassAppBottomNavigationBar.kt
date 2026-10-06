package com.maxrave.simpmusic.ui.component

import androidx.compose.material3.Icon
import com.maxrave.simpmusic.ui.icon.AutoGraph
import com.maxrave.simpmusic.ui.icon.Home
import com.maxrave.simpmusic.ui.icon.LibraryMusic
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.Sensors
import com.maxrave.simpmusic.ui.icon.SimpIcons
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import androidx.core.graphics.scale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.maxrave.domain.data.player.GenericMediaItem
import com.maxrave.simpmusic.expect.ui.PlatformBackdrop
import com.maxrave.simpmusic.ui.navigation.destination.home.AnalyticsDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.HomeDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.MixForYouDestination
import com.maxrave.simpmusic.ui.navigation.destination.search.SearchDestination
import com.maxrave.simpmusic.ui.screen.MiniPlayer
import com.maxrave.simpmusic.ui.theme.LocalIsDarkTheme
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.muso.music.R

sealed class BottomNavScreen(
    val ordinal: Int,
    val destination: Any,
    val title: Int, // Android resource id (Muso port: was StringResource)
    val icon: @Composable () -> Unit,
) {
    data object Home : BottomNavScreen(
        ordinal = 0,
        destination = HomeDestination,
        title = R.string.simp_home,
        icon = {
            Icon(
                SimpIcons.Home,
                contentDescription = null,
            )
        },
    )

    data object Search : BottomNavScreen(
        ordinal = 1,
        destination = SearchDestination,
        title = R.string.simp_search,
        icon = {
            Icon(
                SimpIcons.Search,
                contentDescription = null,
            )
        },
    )

    data object Library : BottomNavScreen(
        ordinal = 2,
        destination = LibraryDestination,
        title = R.string.simp_library,
        icon = {
            Icon(
                imageVector = SimpIcons.LibraryMusic,
                contentDescription = null,
            )
        },
    )

    // Only shown when local tracking is enabled.
    data object Analytics : BottomNavScreen(
        ordinal = 3,
        destination = AnalyticsDestination,
        title = R.string.simp_analytics,
        icon = {
            Icon(
                imageVector = SimpIcons.AutoGraph,
                contentDescription = null,
            )
        },
    )

    // Only shown while signed in to YouTube — an anonymous session gets no mixes.
    // Labelled "Mix", not "Mix for you": the full title is the widest label in the bar and forces
    // every tab to be that wide. The screen itself still uses the full title.
    data object MixForYou : BottomNavScreen(
        ordinal = 4,
        destination = MixForYouDestination,
        title = R.string.simp_mix,
        icon = {
            Icon(
                imageVector = SimpIcons.Sensors,
                contentDescription = null,
            )
        },
    )
}
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private const val TAG = "LiquidGlassAppBottomNavigationBar"

@Composable
fun LiquidGlassAppBottomNavigationBar(
    startDestination: Any,
    navController: NavController,
    backdrop: PlatformBackdrop,
    viewModel: SharedViewModel,
    isScrolledToTop: Boolean,
    showAnalyticsTab: Boolean,
    showMixForYouTab: Boolean,
    onOpenNowPlaying: () -> Unit,
    reloadDestinationIfNeeded: (KClass<*>) -> Unit,
    // The bar's integrated glass mini player can be turned off (the standalone
    // pill rides above the bar instead) without touching anything else.
    showMiniPlayer: Boolean = true,
) {
    // Performance mode: Liquid Glass no longer performs graphics-layer pixel readbacks.
    // The previous 5x5 bitmap sampling loop forced GPU->CPU synchronization every second,
    // which is especially expensive while the backdrop shader is active. A stable mid-luminance
    // keeps the material deterministic and lets the glass stay entirely on the render path.
    val glassLuminance = 0.5f
    val searchFabInteraction = rememberGlassInteraction()
    val toolbarInteraction = rememberGlassInteraction()

    val nowPlayingData by viewModel.nowPlayingState.collectAsStateWithLifecycle()
    // MiniPlayer visibility: derived, never stored.
    //
    // This is a second copy of the rule App.kt applies to the plain bottom bar, and it carried the
    // same two faults. rememberSaveable(true) makes the first composition assert "a track is
    // playing" before anything knows — nowPlayingState starts null and only fills in once the
    // service has connected and the queue has been restored — and the LaunchedEffect that
    // corrected it could only run AFTER that frame had already been drawn. So the bar laid itself
    // out with the mini player, dropped it, then brought it back, animating each step through
    // decoupledConstraints.
    //
    // Fixing the copy in App.kt did nothing here, because with liquid glass on it is THIS file
    // that draws the mini player.
    val isShowMiniPlayer by remember {
        derivedStateOf {
            val item = nowPlayingData?.mediaItem
            showMiniPlayer && item != null && item != GenericMediaItem.EMPTY
        }
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val bottomNavScreens =
        listOfNotNull(
            BottomNavScreen.Home,
            BottomNavScreen.MixForYou.takeIf { showMixForYouTab },
            BottomNavScreen.Analytics.takeIf { showAnalyticsTab },
            BottomNavScreen.Library,
            BottomNavScreen.Search,
        )
    // Tabs shown in the sliding bar (Apple Music style); Search lives in its own FAB.
    val barTabs =
        listOfNotNull(
            BottomNavScreen.Home,
            BottomNavScreen.MixForYou.takeIf { showMixForYouTab },
            BottomNavScreen.Analytics.takeIf { showAnalyticsTab },
            BottomNavScreen.Library,
        )
    var selectedIndex by rememberSaveable {
        mutableIntStateOf(
            when (startDestination) {
                is HomeDestination -> BottomNavScreen.Home.ordinal
                is SearchDestination -> BottomNavScreen.Search.ordinal
                is LibraryDestination -> BottomNavScreen.Library.ordinal
                is AnalyticsDestination -> BottomNavScreen.Analytics.ordinal
                is MixForYouDestination -> BottomNavScreen.MixForYou.ordinal
                else -> BottomNavScreen.Home.ordinal // Default to Home if not recognized
            },
        )
    }
    // A tab can disappear from the bar under the user: tracking gets turned off while Analytics is
    // selected, or the YouTube session ends while Mix for you is. Fall back to Home in both cases so
    // nothing is left highlighted.
    LaunchedEffect(showAnalyticsTab, showMixForYouTab) {
        if ((!showAnalyticsTab && selectedIndex == BottomNavScreen.Analytics.ordinal) ||
            (!showMixForYouTab && selectedIndex == BottomNavScreen.MixForYou.ordinal)
        ) {
            selectedIndex = BottomNavScreen.Home.ordinal
        }
    }
    var isExpanded by rememberSaveable {
        mutableStateOf(true)
    }

    var isInSearchDestination by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(currentBackStackEntry) {
        currentBackStackEntry?.destination?.let { current ->
            isInSearchDestination = current.hasRoute(SearchDestination::class)
        }
    }

    LaunchedEffect(isInSearchDestination) {
        isExpanded = !isInSearchDestination
    }

    // Derive the constraint set directly from the two visual states. The old
    // mutable constraint + onGloballyPositioned feedback loop could race with
    // scroll-driven isExpanded changes: one frame would keep the previous
    // constraint set and the mini player/nav animation appeared to work only
    // intermittently. A remembered immutable target gives ConstraintLayout a
    // new target on every state change, so its animateChangesSpec always runs.
    val constraintSet = remember(isShowMiniPlayer, isExpanded) {
        decoupledConstraints(isShowMiniPlayer, isExpanded)
    }

    LaunchedEffect(isScrolledToTop) {
        if (!isInSearchDestination) {
            isExpanded = isScrolledToTop
        }
    }

    fun selectTab(index: Int) {
        val screen = bottomNavScreens.find { it.ordinal == index } ?: return
        if (selectedIndex == index) {
            if (currentBackStackEntry?.destination?.hierarchy?.any {
                    it.hasRoute(screen.destination::class)
                } == true
            ) {
                reloadDestinationIfNeeded(screen.destination::class)
            } else {
                navController.navigate(screen.destination)
            }
        } else {
            selectedIndex = index
            navController.navigate(screen.destination) {
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    ConstraintLayout(
        constraintSet = constraintSet,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(WindowInsets.navigationBars.asPaddingValues())
                .padding(bottom = 8.dp)
                .imePadding(),
        // Use a gentle spring instead of a fixed tween. The same MiniPlayer instance stays alive
        // while its bounds move from "above the navbar" to "between Home and Search". This prevents
        // the old teardown/recompose jump and gives the requested smooth + slightly bouncy motion.
        animateChangesSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.86f,
            stiffness = 420f,
        ),
    ) {
        val selectedScreen =
            bottomNavScreens.find { it.ordinal == selectedIndex } ?: BottomNavScreen.Home

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .layoutId("toolbar"),
        ) {
            if (isExpanded) {
                BoxWithConstraints(Modifier.weight(1f, fill = false)) {
                    LiquidGlassTabBar(
                        tabs = barTabs,
                        selectedTab = barTabs.indexOfFirst { it.ordinal == selectedIndex },
                        backdrop = backdrop,
                        luminance = glassLuminance,
                        availableWidth = maxWidth,
                        onTabSelected = { position -> selectTab(barTabs[position].ordinal) },
                    )
                }
                Spacer(Modifier.size(12.dp))
                Box(
                    modifier =
                        Modifier
                            .size(56.dp)
                            .drawInteractiveGlass(
                                LocalIsDarkTheme.current,
                                backdrop,
                                null,
                                glassLuminance,
                                CircleShape,
                                searchFabInteraction,
                            )
                            .clickable { selectTab(BottomNavScreen.Search.ordinal) },
                    contentAlignment = Alignment.Center,
                ) {
                    BottomNavScreen.Search.icon()
                }
            } else {
                // Collapsed state: Home/Library/etc. collapse to one selected Home-style button;
                // the single persistent MiniPlayer occupies the centre; Search remains on the right.
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .drawInteractiveGlass(
                                LocalIsDarkTheme.current,
                                backdrop,
                                null,
                                glassLuminance,
                                CircleShape,
                                toolbarInteraction,
                            )
                            .clickable {
                                if (currentBackStackEntry?.destination?.hierarchy?.any {
                                        it.hasRoute(selectedScreen.destination::class)
                                    } == true
                                ) {
                                    reloadDestinationIfNeeded(selectedScreen.destination::class)
                                } else {
                                    navController.navigate(selectedScreen.destination)
                                }
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    selectedScreen.icon()
                }
                Spacer(Modifier.weight(1f))
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .drawInteractiveGlass(
                                LocalIsDarkTheme.current,
                                backdrop,
                                null,
                                glassLuminance,
                                CircleShape,
                                searchFabInteraction,
                            )
                            .clickable { selectTab(BottomNavScreen.Search.ordinal) },
                    contentAlignment = Alignment.Center,
                ) {
                    BottomNavScreen.Search.icon()
                }
            }
        }

        // IMPORTANT: this is the only MiniPlayer instance in both states. ConstraintLayout moves
        // and resizes this exact surface instead of destroying one player and creating another.
        if (isShowMiniPlayer) {
            MiniPlayer(
                Modifier
                    .layoutId("miniPlayer")
                    .then(if (isExpanded) Modifier.height(56.dp) else Modifier.height(48.dp)),
                backdrop = backdrop,
                onClick = onOpenNowPlaying,
                onClose = {
                    viewModel.stopPlayer()
                    viewModel.isServiceRunning = false
                },
            )
        }
    }
}

private fun decoupledConstraints(
    isMiniplayerShow: Boolean = true,
    isExpanded: Boolean,
): ConstraintSet =
    ConstraintSet {
        val toolbar = createRefFor("toolbar")
        constrain(toolbar) {
            start.linkTo(parent.start)
            end.linkTo(parent.end)
            bottom.linkTo(parent.bottom)
            width = Dimension.fillToConstraints
            height = Dimension.wrapContent
        }

        val miniPlayer = createRefFor("miniPlayer")
        constrain(miniPlayer) {
            visibility = if (isMiniplayerShow) Visibility.Visible else Visibility.Gone
            if (isExpanded) {
                start.linkTo(parent.start, 12.dp)
                end.linkTo(parent.end, 12.dp)
                bottom.linkTo(toolbar.top, 12.dp)
                width = Dimension.fillToConstraints
                height = Dimension.value(56.dp)
            } else {
                // Home button = 48dp + 8dp gap. Search button = 48dp + 8dp gap.
                // The player therefore lands exactly between the two buttons.
                start.linkTo(parent.start, 68.dp)
                end.linkTo(parent.end, 68.dp)
                bottom.linkTo(toolbar.bottom)
                width = Dimension.fillToConstraints
                height = Dimension.value(48.dp)
            }
        }
    }

