package dev.liquidflow.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import dev.liquidflow.core.LiquidFlow
import dev.liquidflow.core.LiquidFlowState

/**
 * Everything a component needs in order to animate consistently.
 *
 * A component never invents its own timing: it asks for the ambient [MotionConfig], which is
 * derived from [LiquidFlow] configuration, the active rendering profile and the host's visibility.
 * That is what makes the whole framework feel like one system.
 */
@Immutable
public data class MotionConfig(
    val preset: SpringPreset = SpringPresets.NaturalSpring,
    /** True when the developer or the platform asked for reduced motion. */
    val reducedMotion: Boolean = false,
    /** Global speed multiplier, combining `animationSpeed` with the profile's motion scale. */
    val speedScale: Float = 1f,
    /** False when animations are disabled entirely, or when the host is not visible. */
    val enabled: Boolean = true,
    val pressScale: Float = 0.96f,
    val bounceScale: Float = 1.03f,
    /** Duration used by the few non-spring transitions (fades). */
    val fadeDurationMs: Int = 180,
) {
    /** True when any animation should be replaced by an instant state change. */
    public val shouldReduceMotion: Boolean
        get() = reducedMotion || !enabled

    public fun withPreset(preset: SpringPreset): MotionConfig = copy(preset = preset)

    public companion object {
        /** Reduced-motion friendly default: springs are replaced by snaps. */
        public val Reduced: MotionConfig = MotionConfig(reducedMotion = true)

        public val Default: MotionConfig = MotionConfig()

        /**
         * Derives the ambient motion configuration from framework state.
         *
         * When the host is not visible animations are disabled rather than merely slowed: a
         * backgrounded screen must not keep a spring running for no one.
         */
        public fun from(state: LiquidFlowState, visible: Boolean, base: MotionConfig = Default): MotionConfig {
            val performance = state.config.performance
            val speed = state.config.animationSpeed * state.limits.motionScale
            return base.copy(
                preset = SpringPresets.fromNameOrDefault(state.config.springPreset),
                reducedMotion = state.reducedMotion,
                speedScale = speed.coerceIn(0.1f, 5f),
                enabled = state.config.animationsEnabled &&
                    (!performance.pauseAnimationsWhenNotVisible || visible),
                fadeDurationMs = (base.fadeDurationMs / speed.coerceAtLeast(0.1f)).toInt().coerceIn(0, 1000),
            )
        }
    }
}

/**
 * Ambient motion configuration. `LiquidFlowTheme` provides this, and every LiquidFlow component
 * reads it, so a theme change reconfigures the whole tree at once.
 */
public val LocalMotionConfig: ProvidableCompositionLocal<MotionConfig> =
    staticCompositionLocalOf { MotionConfig.Default }

/** Reads framework state and returns the motion configuration that should be in effect. */
@Composable
public fun rememberMotionConfig(base: MotionConfig = MotionConfig.Default): MotionConfig {
    val state by LiquidFlow.state.collectAsState()
    val visible by LiquidFlow.isVisible.collectAsState()
    return remember(state, visible, base) { MotionConfig.from(state, visible, base) }
}

/** The motion configuration currently in effect: the theme's, or one derived from framework state. */
@Composable
public fun currentMotionConfig(): MotionConfig = LocalMotionConfig.current
