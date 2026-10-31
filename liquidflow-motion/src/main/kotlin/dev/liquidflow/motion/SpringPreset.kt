package dev.liquidflow.motion

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import kotlin.math.sqrt

/**
 * A named spring. Damping ratio and stiffness are the two numbers that decide how a component
 * feels, so they are exposed directly instead of hiding behind opaque "durations".
 *
 * - `dampingRatio < 1` overshoots (bouncy), `= 1` is critically damped, `> 1` is sluggish.
 * - `stiffness` is the spring constant: higher is faster and snappier.
 */
@Immutable
public data class SpringPreset(
    val name: String,
    val dampingRatio: Float,
    val stiffness: Float,
    val initialVelocity: Float = 0f,
) {
    init {
        require(dampingRatio > 0f) { "dampingRatio must be positive, was $dampingRatio" }
        require(stiffness > 0f) { "stiffness must be positive, was $stiffness" }
    }

    /**
     * Natural frequency in radians per second. Used to scale stiffness for speed changes:
     * halving the perceived duration requires `4x` the stiffness, because time scales with
     * `1 / sqrt(stiffness)`.
     */
    public val naturalFrequency: Float
        get() = sqrt(stiffness)

    /** Stiffness adjusted for [speedScale], preserving the damping ratio and therefore the feel. */
    public fun stiffnessForSpeed(speedScale: Float): Float {
        val scale = speedScale.coerceIn(0.1f, 5f)
        return stiffness * scale * scale
    }

    public fun spec(speedScale: Float = 1f): SpringSpec<Float> =
        spring(
            dampingRatio = dampingRatio,
            stiffness = stiffnessForSpeed(speedScale),
            visibilityThreshold = 0.001f,
        )

    /** The animation spec for this preset, honouring reduced motion by snapping instead. */
    public fun animationSpec(config: MotionConfig): AnimationSpec<Float> =
        if (config.shouldReduceMotion) snap() else spec(config.speedScale)
}

/** The six presets the framework ships with. Every component defaults to one of these. */
public object SpringPresets {

    /** Small, friendly overshoot. Good for cards and buttons. */
    public val SoftBounce: SpringPreset = SpringPreset(
        name = "Soft Bounce",
        dampingRatio = 0.55f,
        stiffness = 260f,
    )

    /** The framework default: a little life, no wobble. */
    public val NaturalSpring: SpringPreset = SpringPreset(
        name = "Natural Spring",
        dampingRatio = 0.78f,
        stiffness = 420f,
    )

    /** Smooth and weighty, for large surfaces such as sheets and panels. */
    public val FluidMotion: SpringPreset = SpringPreset(
        name = "Fluid Motion",
        dampingRatio = 0.92f,
        stiffness = 300f,
    )

    /** Deliberate overshoot, for playful toggles and the motion playground. */
    public val ElasticMotion: SpringPreset = SpringPreset(
        name = "Elastic Motion",
        dampingRatio = 0.35f,
        stiffness = 500f,
    )

    /** Almost no visible spring: for high-frequency UI such as navigation taps. */
    public val QuickResponse: SpringPreset = SpringPreset(
        name = "Quick Response",
        dampingRatio = 0.95f,
        stiffness = 900f,
    )

    /** Slow and calm, for full-screen transitions. */
    public val GentleTransition: SpringPreset = SpringPreset(
        name = "Gentle Transition",
        dampingRatio = 1.0f,
        stiffness = 180f,
    )

    public val all: List<SpringPreset> = listOf(
        SoftBounce,
        NaturalSpring,
        FluidMotion,
        ElasticMotion,
        QuickResponse,
        GentleTransition,
    )

    public fun byName(name: String): SpringPreset? = all.firstOrNull { it.name.equals(name, ignoreCase = true) }

    public fun fromNameOrDefault(name: String?): SpringPreset =
        name?.let { byName(it) } ?: NaturalSpring
}
