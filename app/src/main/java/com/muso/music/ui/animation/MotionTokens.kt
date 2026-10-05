package com.muso.music.ui.animation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * Central iOS-inspired motion system (Motion System PRD §2, §13).
 *
 * ONE source of truth for every duration, easing curve, spring and scale in
 * the app. Screens and components take their values from here - no
 * per-page, contradictory animation systems (PRD §2).
 *
 * Design principles the numbers encode (PRD §1.2):
 *  - predictable: a fixed token per interaction class, the same everywhere;
 *  - restrained: no excess bounce or decoration;
 *  - unified: navigation, press states, dialogs and sheets all read these
 *    tokens, so the whole app follows one motion language;
 *  - performant: everything animates graphics-layer properties only
 *    (translation/alpha/scale) - render-thread motion, zero per-frame
 *    recomposition, no layout passes while animating (PRD §14.2).
 *
 * Reduced motion (PRD §15): when the user disables the in-app animation
 * preference or the SYSTEM animator scale is 0, call sites already pass
 * animationsEnabled=false and use snap/instant variants - the app keeps
 * working fully, just without movement.
 */
object Motion {
    // ---------- §2.1 duration tokens (mid-range of the PRD bands) ----------

    /** 120-180 ms: instant feedback, small elements (press, toggle). */
    const val MICRO: Int = 150

    /** 200-280 ms: small elements, small state changes (chips). */
    const val SMALL: Int = 240

    /** 280-380 ms: common UI state/element transitions. */
    const val NORMAL: Int = 330

    /** 300-450 ms: full/partial sheet and screen transitions. */
    const val SHEETS: Int = 380

    /** 350-500 ms: large elements or hero transitions. */
    const val HERO: Int = 430

    /** 150-240 ms: closing/dismissal transitions. */
    const val EXIT: Int = 195

    // ---------- §3 navigation ----------

    /** Push: PRD band 300-350 ms. */
    const val PUSH: Int = 330

    /** Pop: PRD band 220-320 ms - exits are always faster than entries. */
    const val POP: Int = 280

    /**
     * Top-level tab switch: short crossfade, never a slide (PRD §3.5).
     * Round 195 (user request: the button-to-button switch felt laggy): 150 ms
     * left the incoming screen's first composition - the heaviest frame of the
     * whole switch - sitting inside a blink. 210 ms keeps it a crossfade while
     * giving that frame room, which is what reads as smooth.
     */
    const val TAB: Int = 210

    /** Outgoing page parallax: PRD band 15-25% of screen width. */
    const val PARALLAX: Float = 0.20f

    /** Dialog/popover open: PRD §6.1 band 150-220 ms. */
    const val DIALOG: Int = 180

    // ---------- §2.2 easings ----------

    /**
     * The iOS standard curve. Entrances START fast and DECELERATE
     * (ease-out); the tail stays gentle so nothing snaps.
     */
    val EnterEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

    /** Symmetric standard curve for state-to-state interpolation. */
    val StandardEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1f)

    /** Exits accelerate towards the end (ease-in) - PRD §2.2. */
    val ExitEasing = CubicBezierEasing(0.4f, 0.0f, 1f, 1f)

    // ---------- §2.2 springs (sheet-like components) ----------

    /**
     * Sheet spring: 300-500 ms settle, realistic physics, overshoot very
     * small or near zero (PRD §2.2 bounce table). A damping ratio just
     * under critical gives the single gentle settle iOS sheets have.
     */
    fun <T> sheetSpring(): SpringSpec<T> = spring(
        dampingRatio = 0.9f,
        stiffness = Spring.StiffnessMediumLow,
    )

    /**
     * Press spring (PRD §4.1): scale 0.92 -> 1.0 on release with at most
     * 4-8% overshoot. dampingRatio 0.78 sits inside that band - one subtle
     * bounce, never a rubber ball.
     */
    fun <T> pressSpring(): SpringSpec<T> = spring(
        dampingRatio = 0.78f,
        stiffness = Spring.StiffnessMedium,
    )

    /**
     * Universal light-touch spring (PRD §7.1): rows, cards, nav items and
     * every other clickable scale 1.0 -> 0.97 and back. At this amplitude a
     * critically damped spring reads as instant-but-soft - fast start, no
     * visible bounce, ~200 ms settle.
     */
    fun <T> lightPressSpring(): SpringSpec<T> = spring(
        dampingRatio = 1f,
        stiffness = 1500f,
    )

    // ---------- §4/§6/§7 scales ----------

    /** Standard pressed scale for buttons (PRD §4.1). */
    const val PRESS_SCALE: Float = 0.92f

    /** Light touch-feedback scale for rows and nav items (PRD §7.1). */
    const val LIGHT_PRESS_SCALE: Float = 0.97f

    /** Dialog/popover enter scale (PRD §6.1: 0.96 -> 1.0). */
    const val DIALOG_SCALE: Float = 0.96f

    /** Screen enter settle scale (PRD §3.1 "0.96 -> 1.0, if needed"). */
    const val SCREEN_SCALE: Float = 0.96f
}
