package com.muso.music.ui.animation

import androidx.compose.animation.core.animate
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.DrawModifierNode
import androidx.compose.ui.graphics.drawscope.scale
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
 * 30+ screens: the effect comes from the single composition-local.
 *
 * How it stays at 60 fps (PRD §14.2): the node applies the scale inside
 * the DRAW pass as a canvas transform - no recomposition, no layout pass,
 * no graphics-layer allocation. The press progress is a single float
 * animated by a critically damped spring, and an interrupted press
 * (press -> release -> press) cancels and continues from the current
 * value, so rapid taps can never stack animations (PRD §7.3).
 *
 * Disabled clicks, drags and scrolls are untouched: only real press
 * interactions drive the progress. When animations are disabled (app
 * preference or the system "remove animations" setting, PRD §15) the
 * provider swaps in a disabled instance that just draws the content.
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

    private var progress = 0f
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
                animationSpec = Motion.lightPressSpring<Float>(),
            ) { value, _ ->
                progress = value
                invalidateDraw()
            }
        }
    }

    override fun ContentDrawScope.draw() {
        if (!enabled || progress < 0.005f) {
            drawContent()
            return
        }
        val scale = 1f - (1f - Motion.LIGHT_PRESS_SCALE) * progress
        scale(scale = scale, pivot = center) {
            drawContent()
        }
    }
}
