package com.muso.music.ui.player

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.ui.component.AppBottomNavigationBar
import com.maxrave.simpmusic.ui.component.LiquidGlassAppBottomNavigationBar
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.muso.music.constants.LiquidGlassNavBarKey
import com.muso.music.constants.NavigationBarHeight
import com.muso.music.constants.MiniPlayerHeight
import com.muso.music.playback.PlayerConnection
import com.muso.music.ui.component.BottomSheetState
import com.muso.music.utils.rememberPreference
import org.koin.compose.koinInject
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
    val liquidGlass by rememberPreference(LiquidGlassNavBarKey, defaultValue = true)

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
                val fullStack = bottomInset + NavigationBarHeight + MiniPlayerHeight
                if (visibleHeight <= 0.dp) {
                    // Navbar hidden: the PILL alone survives, lifted by the gesture
                    // inset and still sliding away when the player sheet expands.
                    // The old rule parked the whole stack off-screen - Settings had
                    // no mini player at all, and the retired strip stood in for it.
                    val slideOffset = fullStack * playerBottomSheetState.progress.coerceIn(0f, 1f)
                    IntOffset(x = 0, y = (bottomInset + slideOffset).roundToPx())
                } else {
                    val slideOffset = fullStack * playerBottomSheetState.progress.coerceIn(0f, 1f)
                    val hideOffset = (bottomInset + NavigationBarHeight) * (1 - visibleHeight / NavigationBarHeight)
                    IntOffset(x = 0, y = (slideOffset + hideOffset).roundToPx())
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
                com.maxrave.simpmusic.ui.screen.MiniPlayer(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .height(56.dp),
                    backdrop = backdrop,
                    onClick = { playerBottomSheetState.expandSoft() },
                    onClose = {
                        sharedViewModel.stopPlayer()
                        sharedViewModel.isServiceRunning = false
                    },
                )
            }
        } else if (liquidGlass) {
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
        } else {
            // Liquid glass OFF: the SAME pill mini player, flat variant - round
            // artwork, controls in filled circles, theme-surface card - riding
            // above the flat capsule exactly like the glass one does. Reference
            // (SimpMusic navbar files): light theme = white pill on the light
            // theme, dark theme = dark pill on the dark theme.
            // Same visibility rule the glass bar uses: no track, no pill.
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            ) {
                if (isShowMiniPlayer) com.maxrave.simpmusic.ui.screen.MiniPlayer(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .height(56.dp),
                    backdrop = backdrop,
                    onClick = { playerBottomSheetState.expandSoft() },
                    onClose = {
                        sharedViewModel.stopPlayer()
                        sharedViewModel.isServiceRunning = false
                    },
                )
                AppBottomNavigationBar(
                    navController = navController,
                    showAnalyticsTab = false,
                    showMixForYouTab = false,
                    reloadDestinationIfNeeded = { onReloadTab() },
                )
            }
        }
    }
}
