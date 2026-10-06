package com.muso.music.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.graphics.Color
import com.maxrave.simpmusic.ui.component.liquidGlass
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.layoutId
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.ui.component.AppBottomNavigationBar
import com.maxrave.simpmusic.ui.component.LiquidGlassAppBottomNavigationBar
import com.maxrave.simpmusic.ui.component.BottomNavScreen
import com.maxrave.simpmusic.ui.navigation.destination.home.HomeDestination
import com.maxrave.simpmusic.ui.navigation.destination.search.SearchDestination
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.muso.music.constants.LiquidGlassNavBarKey
import com.muso.music.constants.NavigationBarHeight
import com.muso.music.constants.PureBlackKey
import com.muso.music.playback.PlayerConnection
import com.muso.music.ui.component.BottomSheetState
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.clickable
import com.muso.music.constants.MiniPlayerStyle
import com.muso.music.constants.MiniPlayerStyleKey
import com.muso.music.utils.rememberEnumPreference
import com.muso.music.utils.rememberPreference
import org.koin.compose.koinInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.muso.music.ui.player.classic.MusoClassicMiniPlayer
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlin.math.roundToInt

/**
 * MUSO NAVBAR HOST — replaces Muso's old hand-built NavigationBar with the
 * real SimpMusic floating navigation bar (PRD section 12).
 *
 * Liquid glass ON (the default) renders [LiquidGlassAppBottomNavigationBar]
 * with its own glass MiniPlayer riding above the capsule; in that mode Muso's
 * collapsed sheet hides its own mini player so the two never duplicate. With
 * glass OFF the flat [AppBottomNavigationBar] renders the same
 * capsule-and-FAB form, and Muso's sheet keeps its mini player.
 *
 * The bar keeps the old bottom-bar motion: it slides under the player sheet as
 * it expands and animates away on screens that should not show it.
 */
@Composable
fun BoxScope.MusoNavbarHost(
    backdrop: com.maxrave.simpmusic.expect.ui.PlatformBackdrop,
    navController: NavHostController,
    playerConnection: PlayerConnection?,
    playerBottomSheetState: BottomSheetState,
    bottomInset: Dp,
    visibleHeight: Dp,
    isScrolledToTop: Boolean,
    onReloadTab: () -> Unit,
) {
    val liquidGlass by rememberPreference(LiquidGlassNavBarKey, defaultValue = false)

    // The pill DESIGN is the user's choice now (Appearance), decoupled from the
    // Liquid Glass effect: glass style keeps the bar-integrated pill when glass
    // is on, flat style always uses the standalone pill above the bar. Either
    // way the material follows the effect setting (glass pill / same design
    // flat; flat design / flat design with glass).
    val miniPlayerStyle by rememberEnumPreference(MiniPlayerStyleKey, defaultValue = MiniPlayerStyle.MINIFY)
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val density = LocalDensity.current
    val miniPlayerHeight = when (miniPlayerStyle) {
        MiniPlayerStyle.M3_FLEX -> 72.dp
        MiniPlayerStyle.CLASSIC -> 70.dp
        else -> 56.dp
    }

    // Keep the suite's DataStoreManager shim in sync so the glass MiniPlayer
    // styles itself to match the bar variant the user picked.
    val dataStoreManager: DataStoreManager = koinInject()
    LaunchedEffect(liquidGlass) {
        dataStoreManager.enableLiquidGlass.value =
            if (liquidGlass) DataStoreManager.TRUE else DataStoreManager.FALSE
    }

    // Feed the suite SharedViewModel shim with live player state (the glass
    // bar's MiniPlayer reads it and sends its transport events through it).
    playerConnection?.let { MusoSuiteBridge(playerConnection = it) }

    val sharedViewModel: SharedViewModel = koinInject()

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            // Fade while the stack slides off-screen: without this the glass
            // MiniPlayer (with its colored wavy progress ring) visibly sinks
            // below the navigation bar on screens that hide the bar.
            .graphicsLayer {
                // Navbar-hidden screens keep the pill at full alpha; while the
                // bar itself is coming or going the whole stack fades together.
                alpha =
                    if (visibleHeight <= 0.dp) 1f
                    else (visibleHeight / NavigationBarHeight).coerceIn(0f, 1f)
            }
            .offset {
                // The bar's full stack is capsule + glass MiniPlayer above it.
                // When the player sheet expands, the WHOLE stack must slide off
                // the bottom - stopping after just the capsule height left the
                // MiniPlayer floating over the expanded player, covering its
                // bottom controls and eating their touches.
                val fullStack = bottomInset + NavigationBarHeight + miniPlayerHeight
                if (visibleHeight <= 0.dp) {
                    // Navbar hidden: the PILL alone survives and still slides away
                    // when the player sheet expands. Its lift above the gesture bar
                    // comes from its own navigationBarsPadding (the manual inset
                    // offset here proved fragile on some devices - the pill ended
                    // up flush against the display edge).
                    val slideOffset = fullStack * playerBottomSheetState.progress.coerceIn(0f, 1f)
                    IntOffset(x = 0, y = with(density) { slideOffset.roundToPx() })
                } else {
                    val slideOffset = fullStack * playerBottomSheetState.progress.coerceIn(0f, 1f)
                    val hideOffset = (bottomInset + NavigationBarHeight) * (1 - visibleHeight / NavigationBarHeight)
                    IntOffset(x = 0, y = with(density) { (slideOffset + hideOffset).roundToPx() })
                }
            },
    ) {
        // Hoisted: the flat branch and the pill-only branch below both need it.
        // The mini-player is a UI surface of Muso's real PlayerConnection.
        // Do not gate it on the SimpMusic bridge's nowPlayingState: that bridge is
        // asynchronous and can briefly (or permanently after process restore) lag
        // behind the actual Media3 player. That was the reason non-Classic styles
        // could render an empty/blank pill while Classic still worked.
        val liveMediaMetadata =
            playerConnection?.mediaMetadata?.collectAsStateWithLifecycle()?.value
        val isShowMiniPlayer = liveMediaMetadata != null
        if (visibleHeight <= 0.dp) {
            // Navbar-hidden screens (Settings and friends): the suite MiniPlayer
            // alone, self-styled glass or flat by the LiquidGlass setting - the
            // same two variants the navbar itself shows (user spec: only these
            // two exist anywhere).
            if (isShowMiniPlayer) {
                if (miniPlayerStyle == MiniPlayerStyle.CLASSIC) {
                    ClassicArchiveTuneMiniPlayer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .navigationBarsPadding()
                            .height(miniPlayerHeight),
                        playerConnection = playerConnection,
                        onOpenNowPlaying = { playerBottomSheetState.expandSoft() },
                        navigationProximity = 0f,
                        backdrop = backdrop,
                        useLiquidGlass = liquidGlass,
                        pureBlack = pureBlack,
                    )
                } else {
                    com.maxrave.simpmusic.ui.screen.MiniPlayer(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .navigationBarsPadding()
                            .height(miniPlayerHeight),
                        backdrop = backdrop,
                            playerConnection = playerConnection,
                            liquidGlassOverride = liquidGlass,
                        onClick = { playerBottomSheetState.expandSoft() },
                        onClose = {
                            sharedViewModel.stopPlayer()
                            sharedViewModel.isServiceRunning = false
                        },
                    )
                }
            }
        } else androidx.compose.animation.Crossfade(
            // Round 194 (user request: "ekdom smooth change"): the glass <-> flat
            // bar and mini-player swap used to be an instant cut - two completely
            // different clusters replaced in one frame, which read as a jolt. A
            // short crossfade makes the change glide instead.
            targetState = liquidGlass to miniPlayerStyle,
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 220),
            label = "miniPlayerStyleAndGlassSwap",
        ) { styleAndGlass ->
        val glassOn = styleAndGlass.first
        val activeMiniPlayerStyle = styleAndGlass.second
        if (glassOn && activeMiniPlayerStyle == MiniPlayerStyle.MINIFY) {
            // Glass style: the bar's integrated glass pill, exactly as before.
            LiquidGlassAppBottomNavigationBar(
                startDestination = com.maxrave.simpmusic.ui.navigation.destination.home.HomeDestination,
                navController = navController,
                backdrop = backdrop,
                viewModel = sharedViewModel,
                isScrolledToTop = isScrolledToTop,
                showAnalyticsTab = false,
                showMixForYouTab = false,
                onOpenNowPlaying = { playerBottomSheetState.expandSoft() },
                reloadDestinationIfNeeded = { onReloadTab() },
            )
        } else if (glassOn) {
            // Flat style with glass on: the standalone pill (rendered with the
            // glass material by itself) above the glass bar, whose integrated
            // pill is off.
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            ) {
                if (isShowMiniPlayer) {
                    if (activeMiniPlayerStyle == MiniPlayerStyle.CLASSIC) {
                        ClassicArchiveTuneMiniPlayer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .height(miniPlayerHeight),
                            playerConnection = playerConnection,
                            onOpenNowPlaying = { playerBottomSheetState.expandSoft() },
                            navigationProximity = if (isScrolledToTop) 0f else 1f,
                            backdrop = backdrop,
                            useLiquidGlass = glassOn,
                            pureBlack = pureBlack,
                        )
                    } else {
                        com.maxrave.simpmusic.ui.screen.MiniPlayer(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .height(miniPlayerHeight),
                            backdrop = backdrop,
                            playerConnection = playerConnection,
                            liquidGlassOverride = glassOn,
                            onClick = { playerBottomSheetState.expandSoft() },
                            onClose = {
                                sharedViewModel.stopPlayer()
                                sharedViewModel.isServiceRunning = false
                            },
                        )
                    }
                }
                LiquidGlassAppBottomNavigationBar(
                    startDestination = com.maxrave.simpmusic.ui.navigation.destination.home.HomeDestination,
                    navController = navController,
                    backdrop = backdrop,
                    viewModel = sharedViewModel,
                    isScrolledToTop = isScrolledToTop,
                    showAnalyticsTab = false,
                    showMixForYouTab = false,
                    onOpenNowPlaying = { playerBottomSheetState.expandSoft() },
                    reloadDestinationIfNeeded = { onReloadTab() },
                    showMiniPlayer = false,
                )
            }
        } else {
            // Liquid glass OFF: the SAME pill mini player, flat variant - round
            // artwork, controls in filled circles, theme-surface card - riding
            // above the flat capsule exactly like the glass one does. Reference
            // (SimpMusic navbar files): light theme = white pill on the light
            // theme, dark theme = dark pill on the dark theme.
            // Same visibility rule the glass bar uses: no track, no pill.
            FlatNavigationMiniPlayerCluster(
                navController = navController,
                backdrop = backdrop,
                sharedViewModel = sharedViewModel,
                isShowMiniPlayer = isShowMiniPlayer,
                miniPlayerHeight = miniPlayerHeight,
                isScrolledToTop = isScrolledToTop,
                onOpenNowPlaying = { playerBottomSheetState.expandSoft() },
                onClosePlayer = {
                    sharedViewModel.stopPlayer()
                    sharedViewModel.isServiceRunning = false
                },
                onReloadTab = onReloadTab,
                playerConnection = playerConnection,
                miniPlayerStyle = activeMiniPlayerStyle,
                pureBlack = pureBlack,
            )
        }
        }
    }
}


@Composable
private fun ClassicArchiveTuneMiniPlayer(
    modifier: Modifier,
    playerConnection: PlayerConnection?,
    onOpenNowPlaying: () -> Unit,
    navigationProximity: Float,
    backdrop: com.maxrave.simpmusic.expect.ui.PlatformBackdrop,
    useLiquidGlass: Boolean,
    pureBlack: Boolean,
) {
    val connection = playerConnection ?: return
    var position by remember { mutableLongStateOf(connection.player.currentPosition.coerceAtLeast(0L)) }
    var duration by remember { mutableLongStateOf(connection.player.duration.takeIf { it > 0L } ?: 0L) }

    LaunchedEffect(connection) {
        while (isActive) {
            val player = connection.player
            position = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.takeIf { it > 0L } ?: 0L
            delay(250L)
        }
    }

    Box(
        modifier = modifier.clickable(onClick = onOpenNowPlaying),
    ) {
        MusoClassicMiniPlayer(
            position = position,
            duration = duration,
            modifier = Modifier.fillMaxWidth(),
            pureBlack = pureBlack,
            navigationProximityProvider = { navigationProximity.coerceIn(0f, 1f) },
            playerConnection = connection,
            useLiquidGlass = useLiquidGlass,
            backdrop = backdrop,
        )
    }
}

@Composable
private fun FlatNavigationMiniPlayerCluster(
    navController: NavHostController,
    backdrop: com.maxrave.simpmusic.expect.ui.PlatformBackdrop,
    sharedViewModel: SharedViewModel,
    isShowMiniPlayer: Boolean,
    miniPlayerHeight: Dp,
    isScrolledToTop: Boolean,
    onOpenNowPlaying: () -> Unit,
    onClosePlayer: () -> Unit,
    onReloadTab: () -> Unit,
    playerConnection: PlayerConnection?,
    miniPlayerStyle: MiniPlayerStyle,
    pureBlack: Boolean,
) {
    // Keep ONE player instance alive across both scroll states. The old implementation
    // put one MiniPlayer inside the expanded column and a second one inside the collapsed
    // row, then AnimatedContent swapped the whole trees. That is why the artwork/progress
    // appeared to jump instead of physically moving.
    ConstraintLayout(
        constraintSet = flatMiniPlayerConstraints(
            isExpanded = isScrolledToTop,
            showMiniPlayer = isShowMiniPlayer,
            miniPlayerHeight = miniPlayerHeight,
        ),
        modifier = Modifier.fillMaxWidth(),
        animateChangesSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.86f,
            stiffness = 420f,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .layoutId("flatToolbar")
                .padding(horizontal = 12.dp)
                .then(if (!isScrolledToTop) Modifier.navigationBarsPadding() else Modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (isScrolledToTop) {
                // Full navigation at the top of the feed.
                AppBottomNavigationBar(
                    navController = navController,
                    showAnalyticsTab = false,
                    showMixForYouTab = false,
                    reloadDestinationIfNeeded = { onReloadTab() },
                    isExpanded = true,
                )
            } else {
                // Collapsed navigation: Home + MiniPlayer + Search. The mini player is
                // NOT inside this Row; ConstraintLayout owns its physical position.
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable { navController.navigate(HomeDestination) },
                    contentAlignment = Alignment.Center,
                ) {
                    BottomNavScreen.Home.icon()
                }
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable { navController.navigate(SearchDestination) },
                    contentAlignment = Alignment.Center,
                ) {
                    BottomNavScreen.Search.icon()
                }
            }
        }

        if (isShowMiniPlayer) {
            if (miniPlayerStyle == MiniPlayerStyle.CLASSIC) {
                ClassicArchiveTuneMiniPlayer(
                    modifier = Modifier.layoutId("flatMiniPlayer"),
                    playerConnection = playerConnection,
                    onOpenNowPlaying = onOpenNowPlaying,
                    navigationProximity = if (isScrolledToTop) 0f else 1f,
                    backdrop = backdrop,
                    useLiquidGlass = false,
                    pureBlack = pureBlack,
                )
            } else {
                com.maxrave.simpmusic.ui.screen.MiniPlayer(
                    Modifier.layoutId("flatMiniPlayer"),
                    backdrop = backdrop,
                    playerConnection = playerConnection,
                    liquidGlassOverride = false,
                    onClick = onOpenNowPlaying,
                    onClose = onClosePlayer,
                )
            }
        }
    }
}

private fun flatMiniPlayerConstraints(
    isExpanded: Boolean,
    showMiniPlayer: Boolean,
    miniPlayerHeight: Dp,
): ConstraintSet = ConstraintSet {
    val toolbar = createRefFor("flatToolbar")
    constrain(toolbar) {
        start.linkTo(parent.start)
        end.linkTo(parent.end)
        bottom.linkTo(parent.bottom)
        width = Dimension.fillToConstraints
        height = Dimension.wrapContent
    }

    val mini = createRefFor("flatMiniPlayer")
    constrain(mini) {
        visibility = if (showMiniPlayer) Visibility.Visible else Visibility.Gone
        if (isExpanded) {
            start.linkTo(parent.start, 12.dp)
            end.linkTo(parent.end, 12.dp)
            bottom.linkTo(toolbar.top, 12.dp)
            width = Dimension.fillToConstraints
            height = Dimension.value(miniPlayerHeight)
        } else {
            // Exactly between the 48dp Home and 48dp Search buttons with an 8dp visual gap.
            start.linkTo(parent.start, 68.dp)
            end.linkTo(parent.end, 68.dp)
            bottom.linkTo(toolbar.bottom)
            width = Dimension.fillToConstraints
            height = Dimension.value(48.dp)
        }
    }
}

