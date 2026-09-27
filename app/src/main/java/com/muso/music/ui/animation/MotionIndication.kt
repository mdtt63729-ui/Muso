package com.muso.music.ui.animation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale

/**
 * App-wide iOS-style touch feedback (Motion System PRD §4.2, §7.1).
 *
 * Provided ONCE as the LocalIndication, so every plain clickable and
 * combinedClickable in the app - list rows, cards, nav tiles, buttons -
 * gets the same light press: content scales to 0.97 while pressed and
 * springs back on release. No per-call-site wiring, no retrofit of the
 * 30+ screens; the effect flows from the single composition-local.
 *
 * How it stays at 60 fps (PRD §14.2): the scale is applied inside the
 * DRAW pass as a canvas transform - no recomposition, no layout pass, no
 * graphics-layer allocation. The progress float is snapshot state that is
 * READ during drawing, so Compose redraws only while a press animates.
 * Interrupted presses (press -> release -> press) animate from the current
 * value, so rapid taps can never stack animations (PRD §7.3).
 *
 * When animations are disabled (app preference or the system "remove
 * animations" setting, PRD §15) the provider swaps in a disabled instance
 * that just draws the content unchanged.
 */
class MotionIndication(
    private val enabled: Boolean = true,
) : Indication {
    override fun hashCode(): Int = enabled.hashCode()
    override fun equals(other: Any?): Boolean = other is MotionIndication && other.enabled == enabled

    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance {
        val pressed by interactionSource.collectIsPressedAsState()
        val progress = animateFloatAsState(
            targetValue = if (enabled && pressed) 1f else 0f,
            animationSpec = Motion.lightPressSpring(),
            label = "motionPressProgress",
        )
        return remember {
            object : IndicationInstance {
                override fun ContentDrawScope.drawIndication() {
                    val p = progress.value
                    if (p < 0.005f) {
                        drawContent()
                        return
                    }
                    scale(
                        scale = 1f - (1f - Motion.LIGHT_PRESS_SCALE) * p,
                        pivot = center,
                    ) {
                        drawContent()
                    }
                }
            }
        }
    }
}
