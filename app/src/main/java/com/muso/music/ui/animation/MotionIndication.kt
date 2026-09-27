package com.muso.music.ui.animation

import androidx.compose.animation.core.animate
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.node.DrawModifierNode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
 * READ during drawing, so Compose redraws only while a press animates and
 * DrawModifierNode's automatic draw invalidation does the rest. Interrupted
 * presses (press -> release -> press) animate from the current value, so
 * rapid taps can never stack animations (PRD §7.3).
 *
 * When animations are disabled (app preference or the system "remove
 * animations" setting, PRD §15) the provider swaps in a disabled instance
 * that just draws the content unchanged.
 */
class MotionIndication(
    private val enabled: Boolean = true,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): Modifier.Node =
        MotionIndicationNode(interactionSource, enabled)

    override fun hashCode(): Int = enabled.hashCode()
    override fun equals(other: Any?): Boolean = other is MotionIndication && other.enabled == enabled
}

private class MotionIndicationNode(
    private val interactionSource: InteractionSource,
    private val enabled: Boolean,
) : Modifier.Node(), DrawModifierNode {

    private var progress by mutableFloatStateOf(0f)
    private var animationJob: Job? = null

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collectLatest { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> animateTo(1f)
                    is PressInteraction.Release,
                    is PressInteraction.Cancel,
                    -> animateTo(0f)
                }
            }
        }
    }

    override fun onDetach() {
        animationJob?.cancel()
    }

    private fun animateTo(target: Float) {
        animationJob?.cancel()
        animationJob = coroutineScope.launch {
            animate(
                initialValue = progress,
                targetValue = target,
                animationSpec = Motion.lightPressSpring(),
            ) { value, _ ->
                progress = value
            }
        }
    }

    override fun ContentDrawScope.draw() {
        if (!enabled || progress < 0.005f) {
            drawContent()
            return
        }
        scale(
            scale = 1f - (1f - Motion.LIGHT_PRESS_SCALE) * progress,
            pivot = center,
        ) {
            // Explicit receiver: the scale block's receiver is a plain
            // DrawScope, while drawContent() lives on the ContentDrawScope
            // of this draw() extension.
            this@draw.drawContent()
        }
    }
}
