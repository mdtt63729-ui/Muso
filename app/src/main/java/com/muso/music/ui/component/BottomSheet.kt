package com.muso.music.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.muso.music.constants.NavigationBarAnimationSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Bottom Sheet
 * Modified from [ViMusic](https://github.com/vfsfitvnm/ViMusic)
 */
@Composable
fun BottomSheet(
    state: BottomSheetState,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    onDismiss: (() -> Unit)? = null,
    // When set, only the top `collapsedHitHeight` of the collapsed sheet is
    // interactive (tap + drag). With the floating glass navbar the collapsed
    // sheet is invisible except for the suite's glass MiniPlayer, and its old
    // full-bleed touch catcher sat over live scrolling content around the
    // glass capsule, stealing vertical drags from the feed. The mini player
    // zone is the only part that should grab gestures, like the reference.
    collapsedHitHeight: Dp? = null,
    // False when the collapsed sheet renders no content of its own (the glass
    // navbar draws the mini player): the sheet's own collapsed hit box is then
    // an INVISIBLE full-width strip whose drag/click handlers fired on list
    // scrolls and stray taps - the player "opened by itself" while scrolling
    // or right after launch. The external mini player handles its own tap, so
    // the ghost box needs no gestures at all.
    collapsedInteractive: Boolean = true,
    collapsedContent: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .offset {
                val y = (state.expandedBound - state.value)
                    .roundToPx()
                    .coerceAtLeast(0)
                IntOffset(x = 0, y = y)
            }
            .clip(
                RoundedCornerShape(
                    topStart = if (!state.isExpanded) 16.dp else 0.dp,
                    topEnd = if (!state.isExpanded) 16.dp else 0.dp
                )
            )
            .background(backgroundColor)
    ) {
        if (!state.isCollapsed && !state.isDismissed) {
            BackHandler(onBack = state::collapseSoft)
        }

        if (!state.isCollapsed) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(state) {
                        val velocityTracker = VelocityTracker()

                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                velocityTracker.addPointerInputChange(change)
                                state.dispatchRawDelta(dragAmount)
                            },
                            onDragCancel = {
                                velocityTracker.resetTracking()
                                state.snapTo(state.collapsedBound)
                            },
                            onDragEnd = {
                                val velocity = -velocityTracker.calculateVelocity().y
                                velocityTracker.resetTracking()
                                state.performFling(velocity, onDismiss)
                            }
                        )
                    }
                    .graphicsLayer {
                        alpha = ((state.progress - 0.25f) * 4).coerceIn(0f, 1f)
                    },
                content = content
            )
        }

        if (!state.isExpanded && (onDismiss == null || !state.isDismissed)) {
            Box(
                modifier = Modifier
                    .pointerInput(state, collapsedHitHeight, collapsedInteractive) {
                        if (!collapsedInteractive) return@pointerInput
                        val velocityTracker = VelocityTracker()

                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                velocityTracker.addPointerInputChange(change)
                                state.dispatchRawDelta(dragAmount)
                            },
                            onDragCancel = {
                                velocityTracker.resetTracking()
                                state.snapTo(state.collapsedBound)
                            },
                            onDragEnd = {
                                val velocity = -velocityTracker.calculateVelocity().y
                                velocityTracker.resetTracking()
                                state.performFling(velocity, onDismiss)
                            }
                        )
                    }
                    .graphicsLayer {
                        alpha = 1f - (state.progress * 4).coerceAtMost(1f)
                    }
                    .clickable(
                        enabled = collapsedInteractive,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = state::expandSoft
                    )
                    .fillMaxWidth()
                    .height(collapsedHitHeight ?: state.collapsedBound),
                content = collapsedContent
            )
        }
    }
}

@Stable
/** Process-wide one-shot: only the FIRST sheet creation of a process clamps
 * a restored expanded anchor; later recreations (rotation) keep the anchor. */
private object PlayerSheetBootState {
    var corrected = false
    /** Set by expand()/expandSoft() - the ONLY user-driven ways the sheet grows. */
    var userExpanded = false
    var bootGuarded = false
}

class BottomSheetState(
    draggableState: DraggableState,
    private val coroutineScope: CoroutineScope,
    private val animatable: Animatable<Dp, AnimationVector1D>,
    private val onAnchorChanged: (Int) -> Unit,
    val collapsedBound: Dp,
) : DraggableState by draggableState {
    val dismissedBound: Dp
        get() = animatable.lowerBound!!

    val expandedBound: Dp
        get() = animatable.upperBound!!

    val value by animatable.asState()

    val isDismissed by derivedStateOf {
        value == animatable.lowerBound!!
    }

    val isCollapsed by derivedStateOf {
        value == collapsedBound
    }

    val isExpanded by derivedStateOf {
        value == animatable.upperBound
    }

    val progress by derivedStateOf {
        1f - (animatable.upperBound!! - animatable.value) / (animatable.upperBound!! - collapsedBound)
    }

    fun collapse(animationSpec: AnimationSpec<Dp>) {
        onAnchorChanged(collapsedAnchor)
        coroutineScope.launch {
            animatable.animateTo(collapsedBound, animationSpec)
        }
    }

    fun expand(animationSpec: AnimationSpec<Dp>) {
        PlayerSheetBootState.userExpanded = true
        onAnchorChanged(expandedAnchor)
        coroutineScope.launch {
            animatable.animateTo(animatable.upperBound!!, animationSpec)
        }
    }

    private fun collapse() {
        collapse(SpringSpec())
    }

    private fun expand() {
        expand(SpringSpec())
    }

    fun collapseSoft() {
        // iOS-sheet feel (user report: full -> mini felt laggy): critically
        // damped medium spring - fast settle, zero bounce.
        collapse(spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium))
    }

    fun expandSoft() {
        expand(spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium))
    }

    fun dismiss() {
        onAnchorChanged(dismissedAnchor)
        coroutineScope.launch {
            animatable.animateTo(animatable.lowerBound!!)
        }
    }

    fun snapTo(value: Dp) {
        coroutineScope.launch {
            animatable.snapTo(value)
        }
    }

    fun performFling(velocity: Float, onDismiss: (() -> Unit)?) {
        // A deliberate swipe is well above 750; 250 let ordinary scrolls and
        // stray gesture-nav flings pop the player open "by itself".
        if (velocity > 750) {
            expand()
        } else if (velocity < -250) {
            if (value < collapsedBound && onDismiss != null) {
                dismiss()
                onDismiss.invoke()
            } else {
                collapse()
            }
        } else {
            val l0 = dismissedBound
            val l1 = (collapsedBound - dismissedBound) / 2
            val l2 = (expandedBound - collapsedBound) / 2
            val l3 = expandedBound

            when (value) {
                in l0..l1 -> {
                    if (onDismiss != null) {
                        dismiss()
                        onDismiss.invoke()
                    } else {
                        collapse()
                    }
                }

                in l1..l2 -> collapse()
                in l2..l3 -> expand()
                else -> Unit
            }
        }
    }

    val preUpPostDownNestedScrollConnection
        get() = object : NestedScrollConnection {
            var isTopReached = false

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isExpanded && available.y < 0) {
                    isTopReached = false
                }

                return if (isTopReached && available.y < 0 && source == NestedScrollSource.UserInput) {
                    dispatchRawDelta(available.y)
                    available
                } else {
                    Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!isTopReached) {
                    isTopReached = consumed.y == 0f && available.y > 0
                }

                return if (isTopReached && source == NestedScrollSource.UserInput) {
                    dispatchRawDelta(available.y)
                    available
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                return if (isTopReached) {
                    val velocity = -available.y
                    performFling(velocity, null)

                    available
                } else {
                    Velocity.Zero
                }
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                isTopReached = false
                return Velocity.Zero
            }
        }
}

const val expandedAnchor = 2
const val collapsedAnchor = 1
const val dismissedAnchor = 0

@Composable
fun rememberBottomSheetState(
    dismissedBound: Dp,
    expandedBound: Dp,
    collapsedBound: Dp = dismissedBound,
    initialAnchor: Int = dismissedAnchor,
): BottomSheetState {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    var previousAnchor by rememberSaveable {
        mutableIntStateOf(initialAnchor)
    }

    // Blank fullscreen player on cold start (user report): rememberSaveable
    // restored the sheet as EXPANDED from the previous session, so the app
    // opened straight into an empty player. Only ever restore expanded
    // within a process (rotation/config change); a fresh process always
    // starts at the mini player.
    val bootCorrected = remember { PlayerSheetBootState.corrected }
    if (!bootCorrected && previousAnchor == expandedAnchor) {
        previousAnchor = collapsedAnchor
    }
    PlayerSheetBootState.corrected = true
    val initialValue = when (previousAnchor) {
        expandedAnchor -> expandedBound
        collapsedAnchor -> collapsedBound
        dismissedAnchor -> dismissedBound
        else -> collapsedBound
    }

    // A FRESH Animatable for every bounds/anchor change, constructed AT the
    // anchor: anchoring is synchronous, so no frame ever renders the sheet
    // between anchors. That one late frame was the whole glitch family - the
    // grey plate flashing under the glass bar, and the collapsed touch
    // catcher sitting misplaced over screen content eating taps (the player
    // "opening by itself" on back navigation). The state object below is
    // still re-created per bounds change exactly like before, and dragging
    // within one bounds set keeps the live Animatable untouched.
    val animatable = remember(initialValue, dismissedBound, expandedBound) {
        Animatable(initialValue, Dp.VectorConverter).apply {
            updateBounds(dismissedBound.coerceAtMost(expandedBound), expandedBound)
        }
    }

    // HARD boot guard (user report, video Record_2026-09-29-04-10-22: splash
    // went STRAIGHT to a blank expanded player, home never appeared): whatever
    // restored the sheet as expanded - saved anchor, animatable state, bounds
    // churn, any path - a fresh process NEVER opens expanded. User expansion
    // always goes through expand()/expandSoft() (which set the flag above);
    // anything else that lands expanded on the first frames of a process is a
    // restore artifact and gets snapped back to the mini player.
    LaunchedEffect(animatable, expandedBound, collapsedBound) {
        if (!PlayerSheetBootState.bootGuarded) {
            PlayerSheetBootState.bootGuarded = true
            withFrameNanos { }
            withFrameNanos { }
            if (!PlayerSheetBootState.userExpanded &&
                animatable.value >= expandedBound - 1.dp
            ) {
                animatable.snapTo(collapsedBound)
            }
        }
    }

    return remember(dismissedBound, expandedBound, collapsedBound, coroutineScope) {
        BottomSheetState(
            draggableState = DraggableState { delta ->
                coroutineScope.launch {
                    animatable.snapTo(animatable.value - with(density) { delta.toDp() })
                }
            },
            onAnchorChanged = { previousAnchor = it },
            coroutineScope = coroutineScope,
            animatable = animatable,
            collapsedBound = collapsedBound,
        )
    }
}
