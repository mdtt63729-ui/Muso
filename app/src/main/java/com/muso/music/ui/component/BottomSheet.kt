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
import kotlin.math.abs
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
                    .pointerInput(state, collapsedHitHeight) {
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
class BottomSheetState(
    draggableState: DraggableState,
    private val coroutineScope: CoroutineScope,
    private val animatable: Animatable<Dp, AnimationVector1D>,
    private val onAnchorChanged: (Int) -> Unit,
    collapsedBound: Dp,
) : DraggableState by draggableState {
    /** The collapsed anchor. Mutable: bounds update IN PLACE (see
     *  [rememberBottomSheetState]) instead of recreating the state, which
     *  restarted gesture handlers mid-navigation and misplaced the collapsed
     *  touch catcher for a frame - taps meant for screen content landed on it
     *  and the player "opened by itself". */
    var collapsedBound: Dp = collapsedBound
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
        collapse(spring(stiffness = Spring.StiffnessMediumLow))
    }

    fun expandSoft() {
        expand(spring(stiffness = Spring.StiffnessMediumLow))
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
    val animatable = remember {
        Animatable(0.dp, Dp.VectorConverter)
    }

    val state = remember {
        // Bounds are set HERE, synchronously, BEFORE the state is returned:
        // the very first composition already reads state.isDismissed /
        // progress (animatable.lowerBound!!/upperBound!!), and setting the
        // bounds only in the LaunchedEffect below left them null for that
        // first read - the instant crash on app open in v0.5.155.
        animatable.updateBounds(dismissedBound.coerceAtMost(expandedBound), expandedBound)
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

    // The state object is created ONCE. Bounds (and the collapsed anchor)
    // update IN PLACE: recreating it on every navigation-bar/inset change
    // restarted its gesture handlers mid-transition and left the collapsed
    // touch catcher misplaced for a frame - taps meant for screen content
    // landed on the invisible catcher and the player "opened by itself" -
    // and re-snapped the sheet a frame late, flashing an opaque grey plate
    // under the glass bar (the "box below the player"). In place there is
    // nothing to restart and nothing to flash.
    LaunchedEffect(dismissedBound, expandedBound, collapsedBound) {
        val anchorValue = when (previousAnchor) {
            expandedAnchor -> expandedBound
            collapsedAnchor -> collapsedBound
            dismissedAnchor -> dismissedBound
            else -> collapsedBound
        }
        val wasCollapsed =
            abs((state.collapsedBound - animatable.value).value) < 0.01f
        animatable.updateBounds(dismissedBound.coerceAtMost(expandedBound), expandedBound)
        state.collapsedBound = collapsedBound
        // Re-anchor only when the sheet is actually resting on an anchor: a
        // sheet mid-drag keeps its live value, one resting at the OLD
        // collapsed bound snaps to the new one.
        if (wasCollapsed || previousAnchor != collapsedAnchor) {
            animatable.snapTo(anchorValue)
        }
    }

    return state
}
