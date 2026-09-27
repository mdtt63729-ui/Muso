@file:OptIn(ExperimentalFoundationApi::class)

package com.muso.music.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import com.muso.music.ui.animation.Motion
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * iOS-style bounce button. Motion System PRD §4.1 press treatment, driven by
 * the central tokens: pressed scale 0.92 + a slight dim, released springs
 * back to 1.0 with a 4-8% overshoot. No ripple - pure scale + alpha motion on
 * the graphics layer, so nothing recomposes per frame.
 */
@Composable
fun BounceIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 40.dp,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) Motion.PRESS_SCALE else 1f,
        animationSpec = Motion.pressSpring(),
        label = "bouncePressScale",
    )
    // PRD §4.1 "scale + fade": the pressed state also dims slightly.
    val pressAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.7f else 1f,
        animationSpec = Motion.pressSpring(),
        label = "bouncePressAlpha",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(buttonSize)
            .scale(pressScale)
            .alpha(pressAlpha)
            .clip(CircleShape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        content()
    }
}
