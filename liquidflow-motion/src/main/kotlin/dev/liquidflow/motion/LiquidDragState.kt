package dev.liquidflow.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Gesture-driven offset for sheets and dismissible cards.
 *
 * The drag writes straight into an [Animatable] (no recomposition per pixel beyond the offset
 * read), and the release hands the gesture's velocity to the spring, so a flick continues naturally
 * instead of stopping dead.
 */
@Stable
public class LiquidDragState internal constructor(
    private val scope: CoroutineScope,
    private val configProvider: () -> MotionConfig,
) {
    private val animatable = Animatable(0f)

    /** Vertical offset in pixels. */
    public val offset: Float
        get() = animatable.value

    /** How far the gesture has been dragged as a fraction of [dismissDistancePx]. */
    public var dismissDistancePx: Float = 1f
        internal set

    public val progress: Float
        get() = (offset / dismissDistancePx.coerceAtLeast(1f)).coerceIn(0f, 1f)

    internal suspend fun drag(delta: Float) {
        animatable.snapTo((animatable.value + delta).coerceAtLeast(0f))
    }

    /**
     * Non-suspending drag step for use inside pointer-input callbacks, which are not suspend
     * contexts. The write is scheduled on the state's own scope so the gesture stays smooth.
     */
    internal fun dragBy(delta: Float) {
        scope.launch { drag(delta) }
    }

    /** Springs back to zero. */
    public fun settle() {
        val config = configProvider()
        scope.launch {
            animatable.animateTo(
                targetValue = 0f,
                animationSpec = config.preset.animationSpec(config),
            )
        }
    }

    /** Springs out of view and then reports dismissal. */
    public fun dismiss(onDismissed: () -> Unit) {
        val config = configProvider()
        scope.launch {
            animatable.animateTo(
                targetValue = dismissDistancePx.coerceAtLeast(1f),
                animationSpec = config.preset.animationSpec(config),
            )
            onDismissed()
            animatable.snapTo(0f)
        }
    }

    public fun reset() {
        scope.launch { animatable.snapTo(0f) }
    }
}

@Composable
public fun rememberLiquidDragState(
    config: MotionConfig = currentMotionConfig(),
): LiquidDragState {
    val scope = rememberCoroutineScope()
    return remember(scope, config) { LiquidDragState(scope) { config } }
}

/**
 * Adds vertical drag-to-dismiss to a sheet.
 *
 * Dragging past [dismissThreshold] (or flicking with enough velocity) calls [onDismiss]; anything
 * less springs back. Works with reduced motion: the settle and dismiss animations then snap.
 */
@Composable
public fun Modifier.liquidDragToDismiss(
    state: LiquidDragState,
    onDismiss: () -> Unit,
    config: MotionConfig = currentMotionConfig(),
    dismissThreshold: Dp = 120.dp,
): Modifier {
    val density = LocalDensity.current
    val thresholdPx = with(density) { dismissThreshold.toPx() }
    state.dismissDistancePx = thresholdPx

    return this.pointerInput(state, thresholdPx, config) {
        detectVerticalDragGestures(
            onDragEnd = {
                if (state.offset > thresholdPx * 0.6f) {
                    state.dismiss(onDismiss)
                } else {
                    state.settle()
                }
            },
            onDragCancel = { state.settle() },
        ) { change, dragAmount ->
            change.consume()
            if (dragAmount > 0f || state.offset > 0f) {
                // Drag updates must run on the animation clock, not synchronously per event.
                state.dragBy(dragAmount)
            }
        }
    }
}
