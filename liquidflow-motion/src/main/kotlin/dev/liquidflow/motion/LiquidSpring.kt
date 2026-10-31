package dev.liquidflow.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A single interruptible float driven by a spring.
 *
 * Interruption is the whole point: starting a new animation cancels the one in flight and carries
 * the current value *and velocity* into the next spring, so a user who taps twice quickly gets one
 * continuous motion instead of a jump. `Animatable` provides that behaviour; this class adds the
 * preset plumbing, cancellation bookkeeping and a non-suspend entry point for use in click handlers.
 */
public class LiquidSpring internal constructor(
    initialValue: Float,
    private val scope: CoroutineScope,
    initialConfig: MotionConfig,
) {
    private val animatable = Animatable(initialValue)

    @Volatile
    internal var config: MotionConfig = initialConfig

    private var job: Job? = null

    /** Current value. Reading this inside a composable is observable. */
    public val value: Float
        get() = animatable.value

    /** Current velocity, in units per second. */
    public val velocity: Float
        get() = animatable.velocity

    public val isAnimating: Boolean
        get() = animatable.isRunning

    /**
     * Animates to [target]. Any animation already running is cancelled first, which makes rapid
     * repeated taps safe: the last one wins and motion stays continuous.
     */
    public fun animateTo(target: Float, initialVelocity: Float = 0f) {
        job?.cancel()
        job = scope.launch {
            animatable.animateTo(
                targetValue = target,
                animationSpec = config.preset.animationSpec(config),
                initialVelocity = initialVelocity,
            )
        }
    }

    /** Suspending variant, for use inside `LaunchedEffect` or a gesture callback. */
    public suspend fun animateToAwait(target: Float, initialVelocity: Float = 0f) {
        job?.cancel()
        animatable.animateTo(
            targetValue = target,
            animationSpec = config.preset.animationSpec(config),
            initialVelocity = initialVelocity,
        )
    }

    /** Jumps to [target] without animating. */
    public suspend fun snapTo(target: Float) {
        job?.cancel()
        animatable.snapTo(target)
    }

    /** Stops the current animation where it is. */
    public fun stop() {
        job?.cancel()
        scope.launch { animatable.stop() }
    }

    /** Animates toward [target] using a velocity handed over from a gesture. */
    public fun flingTo(target: Float, velocity: Float) = animateTo(target, velocity)
}

/**
 * Remembers a [LiquidSpring] for the lifetime of the composition.
 *
 * The spring keeps running only while it is animating: it is a state-driven animation, never a
 * loop, so an idle screen produces no work.
 */
@Composable
public fun rememberLiquidSpring(
    initialValue: Float = 0f,
    config: MotionConfig = currentMotionConfig(),
): LiquidSpring {
    val scope = rememberCoroutineScope()
    val spring = remember(scope) { LiquidSpring(initialValue, scope, config) }
    SideEffect { spring.config = config }
    return spring
}

/**
 * Remembers a spring whose target follows [target] automatically.
 *
 * Named separately from [rememberLiquidSpring] so the two overloads cannot be confused at a
 * call site: this one animates whenever [target] changes.
 */
@Composable
public fun rememberLiquidSpringFollowing(
    target: Float,
    config: MotionConfig = currentMotionConfig(),
    initialValue: Float = target,
): LiquidSpring {
    val spring = rememberLiquidSpring(initialValue = initialValue, config = config)
    LaunchedEffect(target, config.shouldReduceMotion) {
        spring.animateToAwait(target)
    }
    return spring
}
