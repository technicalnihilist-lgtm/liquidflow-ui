package dev.liquidflow.motion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize

/**
 * Scale + fade transitions shared by every LiquidFlow surface.
 *
 * When motion is reduced these collapse to `EnterTransition.None` / `ExitTransition.None` rather
 * than a fast animation: an instant, non-animated state change is what the platform's
 * "remove animations" setting asks for.
 */
@Immutable
public data class LiquidTransitions(
    val enter: EnterTransition,
    val exit: ExitTransition,
) {
    public companion object {
        public fun of(config: MotionConfig, initialScale: Float = 0.92f): LiquidTransitions {
            if (config.shouldReduceMotion) {
                return LiquidTransitions(EnterTransition.None, ExitTransition.None)
            }
            val fadeMs = config.fadeDurationMs
            return LiquidTransitions(
                enter = fadeIn(tween(fadeMs)) + scaleIn(
                    initialScale = initialScale,
                    animationSpec = config.preset.spec(config.speedScale),
                ),
                exit = fadeOut(tween((fadeMs * 0.7f).toInt())) + scaleOut(
                    targetScale = 0.96f,
                    animationSpec = config.preset.spec(config.speedScale),
                ),
            )
        }
    }
}

/** AnimatedVisibility wired to LiquidFlow motion. */
@Composable
public fun LiquidAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    config: MotionConfig = currentMotionConfig(),
    initialScale: Float = 0.92f,
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    val transitions = LiquidTransitions.of(config, initialScale)
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = transitions.enter,
        exit = transitions.exit,
        content = content,
    )
}

/**
 * A spring typed for size-based transitions.
 *
 * `SpringSpec<Float>` cannot be passed to `expandVertically`, which needs `IntSize`; the same
 * damping and stiffness produce the same motion, so only the type parameter differs.
 */
private fun sizeSpring(config: MotionConfig) = spring<IntSize>(
    dampingRatio = config.preset.dampingRatio,
    stiffness = config.preset.stiffnessForSpeed(config.speedScale),
)

/** Expand / collapse with a spring, for disclosure rows and inline panels. */
@Composable
public fun LiquidExpandable(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    config: MotionConfig = currentMotionConfig(),
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = expanded,
        modifier = modifier,
        enter = if (config.shouldReduceMotion) {
            EnterTransition.None
        } else {
            expandVertically(animationSpec = sizeSpring(config)) +
                fadeIn(tween(config.fadeDurationMs))
        },
        exit = if (config.shouldReduceMotion) {
            ExitTransition.None
        } else {
            shrinkVertically(animationSpec = sizeSpring(config)) +
                fadeOut(tween(config.fadeDurationMs))
        },
        content = content,
    )
}

/**
 * Transitions for `NavHost`.
 *
 * The motion module deliberately does not depend on `navigation-compose`: these are plain
 * [EnterTransition] values, so you pass them into whichever navigation library you already use.
 *
 * ```
 * val nav = rememberLiquidNavTransitions()
 * NavHost(
 *     navController,
 *     startDestination = "home",
 *     enterTransition = { nav.enter },
 *     exitTransition = { nav.exit },
 *     popEnterTransition = { nav.popEnter },
 *     popExitTransition = { nav.popExit },
 * )
 * ```
 */
@Immutable
public data class LiquidNavTransitions(
    val enter: EnterTransition,
    val exit: ExitTransition,
    val popEnter: EnterTransition,
    val popExit: ExitTransition,
) {
    public companion object {
        public fun of(config: MotionConfig): LiquidNavTransitions {
            if (config.shouldReduceMotion) {
                return LiquidNavTransitions(
                    enter = EnterTransition.None,
                    exit = ExitTransition.None,
                    popEnter = EnterTransition.None,
                    popExit = ExitTransition.None,
                )
            }
            val spec = config.preset.spec(config.speedScale)
            val fadeMs = config.fadeDurationMs
            return LiquidNavTransitions(
                enter = fadeIn(tween(fadeMs)) + scaleIn(initialScale = 0.94f, animationSpec = spec),
                exit = fadeOut(tween((fadeMs * 0.6f).toInt())) + scaleOut(targetScale = 1.02f, animationSpec = spec),
                popEnter = fadeIn(tween(fadeMs)) + scaleIn(initialScale = 1.02f, animationSpec = spec),
                popExit = fadeOut(tween((fadeMs * 0.6f).toInt())) + scaleOut(targetScale = 0.94f, animationSpec = spec),
            )
        }
    }
}

/** Remembers navigation transitions for the ambient motion configuration. */
@Composable
public fun rememberLiquidNavTransitions(
    config: MotionConfig = currentMotionConfig(),
): LiquidNavTransitions = remember(config) { LiquidNavTransitions.of(config) }
