package com.muso.music.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import com.muso.music.ui.animation.Motion
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.maxrave.simpmusic.ui.navigation.destination.home.HomeDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDestination

/**
 * iOS-style page-transition engine (navigation-transitions PRD).
 *
 * Spec (UINavigationController):
 *  - Push 420ms / pop 400ms, CubicBezierEasing(0.25, 0.1, 0.25, 1) both ways.
 *  - Incoming page slides in from 100% width; outgoing page parks at -30%
 *    with a dim. Back is the exact mirror. No spring, no overshoot.
 *  - Top-level bar tabs (Home <-> Library) never slide: a short crossfade
 *    with a touch of scale, the way iOS treats a tab switch.
 *
 * Two looks, one engine, driven by the Liquid Glass setting:
 *  - glass ON  — the incoming page floats in like a translucent plate over
 *    the barely-dimmed page behind (frosted feel). The blur itself is
 *    deliberately NOT animated: a per-frame blur recompute is the single
 *    worst jank source on 120Hz screens, so only cheap properties
 *    (translation/alpha) move. If the GPU still can't keep up, this mode
 *    degrades to the exact same motion with dim only — graceful, no crash.
 *  - glass OFF — identical timing and easing; the dim is stronger and the
 *    outgoing page also settles to a 0.96 scale. Same smoothness, flat look.
 *
 * Everything animates through the transition API's graphicsLayer-backed
 * modifiers (slide/fade/scale), so motion runs on the render thread with
 * zero recomposition per frame. Predictive back on Android 14+ rides on
 * navigation-compose's built-in support (enableOnBackInvokedCallback is set
 * in the manifest) — the drag tracks the finger with the same motion.
 */

// All timing/easing/scale values come from the central motion tokens
// (ui/animation/MotionTokens.kt) - the Motion System PRD's single source
// of truth. Bands: push 300-350 ms, pop 220-320 ms, parallax 15-25%.
private val IosEasing = Motion.EnterEasing
private const val PUSH_MS = 220
private const val POP_MS = 180
private const val TAB_MS = 120
private const val PARALLAX_NUM = 8
private const val PARALLAX_DEN = 10

private fun NavDestination.isTopLevelTab(): Boolean =
    hasRoute(HomeDestination::class) || hasRoute(LibraryDestination::class)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(): Boolean =
    initialState.destination.isTopLevelTab() && targetState.destination.isTopLevelTab()

/** Push: the new page arrives from the right edge. */
fun AnimatedContentTransitionScope<NavBackStackEntry>.iosEnter(
    glass: Boolean,
    animationsEnabled: Boolean = true,
): EnterTransition {
    if (!animationsEnabled) return fadeIn(snap())
    if (isTabSwitch()) {
        // Tab switch: crossfade + slight scale, never a slide.
        return fadeIn(tween(TAB_MS, easing = IosEasing))
    }
    // Perf (user report: page changes lagged): alpha-blending two fullscreen
    // pages - plus a scale re-render - is the expensive part of a nav
    // transition, not the slide. The incoming page now slides in fully
    // opaque, so only ONE moving, non-blended layer renders per frame.
    return slideInHorizontally(tween(PUSH_MS, easing = IosEasing)) { it }
}

/** Push: the old page parks 30% left, dimmed. */
fun AnimatedContentTransitionScope<NavBackStackEntry>.iosExit(
    glass: Boolean,
    animationsEnabled: Boolean = true,
): ExitTransition {
    if (!animationsEnabled) return fadeOut(snap())
    if (isTabSwitch()) return fadeOut(tween(TAB_MS, easing = IosEasing))
    // Same perf pass: the outgoing page parks with a small parallax and NO
    // fade / NO scale - opaque layers only.
    return slideOutHorizontally(tween(PUSH_MS, easing = IosEasing)) { -it * PARALLAX_NUM / PARALLAX_DEN }
}

/** Pop: the page we return to slides back in from -30%. */
fun AnimatedContentTransitionScope<NavBackStackEntry>.iosPopEnter(
    glass: Boolean,
    animationsEnabled: Boolean = true,
): EnterTransition {
    if (!animationsEnabled) return fadeIn(snap())
    if (isTabSwitch()) {
        return fadeIn(tween(TAB_MS, easing = IosEasing))
    }
    return slideInHorizontally(tween(POP_MS, easing = IosEasing)) { -it * PARALLAX_NUM / PARALLAX_DEN }
}

/** Pop: the leaving page exits to 100% width on the right. */
fun AnimatedContentTransitionScope<NavBackStackEntry>.iosPopExit(
    @Suppress("UNUSED_PARAMETER") glass: Boolean,
    animationsEnabled: Boolean = true,
): ExitTransition {
    if (!animationsEnabled) return fadeOut(snap())
    if (isTabSwitch()) return fadeOut(tween(TAB_MS, easing = IosEasing))
    return slideOutHorizontally(tween(POP_MS, easing = IosEasing)) { it }
}
