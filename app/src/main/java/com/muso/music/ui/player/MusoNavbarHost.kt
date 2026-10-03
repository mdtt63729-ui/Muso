package com.muso.music.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.graphics.Color
import com.maxrave.simpmusic.ui.component.liquidGlass
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
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
        val nowPlayingData by sharedViewModel.nowPlayingState.collectAsState()
        val isShowMiniPlayer by androidx.compose.runtime.remember {
            androidx.compose.runtime.derivedStateOf {
                val item = nowPlayingData?.mediaItem
                item != null && item != com.maxrave.domain.data.player.GenericMediaItem.EMPTY
            }
        }
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
            targetState = liquidGlass,
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 180),
            label = "liquidGlassBarSwap",
        ) { glassOn ->
        if (glassOn && miniPlayerStyle == MiniPlayerStyle.MINIFY) {
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
                    if (miniPlayerStyle == MiniPlayerStyle.CLASSIC) {
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
                miniPlayerStyle = miniPlayerStyle,
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
            delay(100L)
        }
    }

    Box(
        modifier = modifier
            .then(
                if (useLiquidGlass) {
                    // Use the shared backdrop directly. The old implementation recorded this
                    // MiniPlayer into its own GraphicsLayer and then sampled that layer for
                    // luminance, which made Classic's glass path unreliable (and added a
                    // readback on the render pipeline). The shared primitive already has the
                    // correct backdrop and setting gate.
                    Modifier.liquidGlass(
                        backdrop = backdrop,
                        shape = miniPlayerShape,
                        interactive = true,
                    )
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onOpenNowPlaying),
    ) {
        MusoClassicMiniPlayer(
            position = position,
            duration = duration,
            modifier = Modifier.fillMaxWidth(),
            pureBlack = pureBlack,
            navigationProximityProvider = { navigationProximity.coerceIn(0f, 1f) },
            playerConnection = connection,
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
    // Use the same two layout states as the SimpMusic glass bar. The important
    // difference from the old Muso flat branch is that the mini player and the
    // navigation bar now transition as ONE layout, so a scroll cannot animate
    // one of them while leaving the other behind for a frame.
    AnimatedContent(
        targetState = isScrolledToTop,
        transitionSpec = {
            (slideInVertically(
                animationSpec = tween(300),
                initialOffsetY = { it / 5 },
            ) + fadeIn(tween(220))).togetherWith(
                slideOutVertically(
                    animationSpec = tween(260),
                    targetOffsetY = { -it / 5 },
                ) + fadeOut(tween(180))
            ).using(SizeTransform(clip = false))
        },
        label = "flatNavMiniScrollTransition",
    ) { expanded ->
        if (expanded) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (isShowMiniPlayer) {
                    if (miniPlayerStyle == MiniPlayerStyle.CLASSIC) {
                        ClassicArchiveTuneMiniPlayer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .height(miniPlayerHeight),
                            playerConnection = playerConnection,
                            onOpenNowPlaying = onOpenNowPlaying,
                            navigationProximity = 0f,
                            backdrop = backdrop,
                            useLiquidGlass = false,
                            pureBlack = pureBlack,
                        )
                    } else {
                        com.maxrave.simpmusic.ui.screen.MiniPlayer(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .height(miniPlayerHeight),
                            backdrop = backdrop,
                            onClick = onOpenNowPlaying,
                            onClose = onClosePlayer,
                        )
                    }
                }
                AppBottomNavigationBar(
                    navController = navController,
                    showAnalyticsTab = false,
                    showMixForYouTab = false,
                    reloadDestinationIfNeeded = { onReloadTab() },
                    isExpanded = true,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppBottomNavigationBar(
                    navController = navController,
                    showAnalyticsTab = false,
                    showMixForYouTab = false,
                    reloadDestinationIfNeeded = { onReloadTab() },
                    isExpanded = false,
                )
                if (isShowMiniPlayer) {
                    Spacer(Modifier.width(8.dp))
                    if (miniPlayerStyle == MiniPlayerStyle.CLASSIC) {
                        ClassicArchiveTuneMiniPlayer(
                            modifier = Modifier
                                .weight(1f)
                                .height(miniPlayerHeight),
                            playerConnection = playerConnection,
                            onOpenNowPlaying = onOpenNowPlaying,
                            navigationProximity = 1f,
                            backdrop = backdrop,
                            useLiquidGlass = false,
                            pureBlack = pureBlack,
                        )
                    } else {
                        com.maxrave.simpmusic.ui.screen.MiniPlayer(
                            Modifier
                                .weight(1f)
                                .height(miniPlayerHeight),
                            backdrop = backdrop,
                            onClick = onOpenNowPlaying,
                            onClose = onClosePlayer,
                        )
                    }
                }
            }
        }
    }
}

