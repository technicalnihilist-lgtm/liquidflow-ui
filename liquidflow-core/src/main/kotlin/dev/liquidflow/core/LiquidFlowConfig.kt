package dev.liquidflow.core

/**
 * Framework configuration.
 *
 * Instances are immutable; change configuration at runtime with
 * `LiquidFlow.update { copy(transparency = 0.8f) }`, which re-emits a new snapshot to every
 * component that reads it.
 */
public data class LiquidFlowConfig(
    val renderingProfile: RenderingProfile = RenderingProfile.AUTO,
    val glassEnabled: Boolean = true,
    val animationsEnabled: Boolean = true,
    val reducedMotion: Boolean = false,
    /**
     * Global multiplier on glass opacity, `0f` (fully transparent) to `1f` (framework default).
     * Individual components may set their own transparency; this multiplies it.
     */
    val transparency: Float = 1f,
    /**
     * Global multiplier on blur radius, `0f` (no blur) to `1f` (profile maximum).
     * Ignored when the active profile disallows backdrop blur.
     */
    val blurIntensity: Float = 1f,
    /** Global animation speed multiplier. `0.25` = quarter speed, `3` = triple speed. */
    val animationSpeed: Float = 1f,
    /**
     * Name of the spring preset every component uses, e.g. "Soft Bounce".
     * Names match `dev.liquidflow.motion.SpringPresets`; an unknown name falls back to the default.
     */
    val springPreset: String = "Natural Spring",
    /** Follow the platform's "remove animations" accessibility setting. */
    val followSystemReducedMotion: Boolean = true,
    val performance: PerformanceConfig = PerformanceConfig(),
    val logging: LoggingConfig = LoggingConfig(),
) {
    /** Clamps every numeric field into its documented range. */
    public fun normalized(): LiquidFlowConfig = copy(
        transparency = transparency.coerceIn(0f, 1f),
        blurIntensity = blurIntensity.coerceIn(0f, 1f),
        animationSpeed = animationSpeed.coerceIn(0.25f, 3f),
    )

    public companion object {
        public val DEFAULT: LiquidFlowConfig = LiquidFlowConfig()
    }
}

/**
 * DSL entry point so that configuration reads like the documented API:
 *
 * ```
 * LiquidFlow.initialize(context, LiquidFlowConfig {
 *     renderingProfile = RenderingProfile.AUTO
 *     glassEnabled = true
 *     animationsEnabled = true
 *     reducedMotion = false
 * })
 * ```
 */
public fun LiquidFlowConfig(block: LiquidFlowConfigBuilder.() -> Unit): LiquidFlowConfig =
    LiquidFlowConfigBuilder().apply(block).build()

public class LiquidFlowConfigBuilder {
    public var renderingProfile: RenderingProfile = RenderingProfile.AUTO
    public var glassEnabled: Boolean = true
    public var animationsEnabled: Boolean = true
    public var reducedMotion: Boolean = false
    public var transparency: Float = 1f
    public var blurIntensity: Float = 1f
    public var animationSpeed: Float = 1f
    public var springPreset: String = "Natural Spring"
    public var followSystemReducedMotion: Boolean = true
    public var performance: PerformanceConfig = PerformanceConfig()
    public var logging: LoggingConfig = LoggingConfig()

    public fun performance(block: PerformanceConfigBuilder.() -> Unit) {
        performance = PerformanceConfigBuilder().apply(block).build()
    }

    public fun build(): LiquidFlowConfig = LiquidFlowConfig(
        renderingProfile = renderingProfile,
        glassEnabled = glassEnabled,
        animationsEnabled = animationsEnabled,
        reducedMotion = reducedMotion,
        transparency = transparency,
        blurIntensity = blurIntensity,
        animationSpeed = animationSpeed,
        springPreset = springPreset,
        followSystemReducedMotion = followSystemReducedMotion,
        performance = performance,
        logging = logging,
    ).normalized()
}

/** Performance-related knobs. Defaults are tuned for a 60 Hz mid-range device. */
public data class PerformanceConfig(
    /** The frame rate the framework budgets against. Used for jank classification. */
    val targetFrameRate: Int = 60,
    /** Frame budget derived from [targetFrameRate]. */
    val frameBudgetMs: Float = 1000f / 60f,
    /** Upper bound on memory used by cached blurred backdrops. */
    val backdropCacheSizeBytes: Int = 8 * 1024 * 1024,
    /** Let `liquidflow-adaptive` switch profiles from measured frame times. */
    val enableAdaptiveProfiles: Boolean = true,
    /** Number of frames in one adaptation window. */
    val adaptationWindowFrames: Int = 120,
    /** Stop animations while the host is not visible. */
    val pauseAnimationsWhenNotVisible: Boolean = true,
    /** Collect frame metrics at all. Disable in production if you do not need the dashboard. */
    val enableFrameMetrics: Boolean = true,
) {
    public companion object {
        public fun forFrameRate(fps: Int): PerformanceConfig {
            val safeFps = fps.coerceIn(30, 144)
            return PerformanceConfig(targetFrameRate = safeFps, frameBudgetMs = 1000f / safeFps)
        }
    }
}

public class PerformanceConfigBuilder {
    public var targetFrameRate: Int = 60
    public var frameBudgetMs: Float = 1000f / 60f
    public var backdropCacheSizeBytes: Int = 8 * 1024 * 1024
    public var enableAdaptiveProfiles: Boolean = true
    public var adaptationWindowFrames: Int = 120
    public var pauseAnimationsWhenNotVisible: Boolean = true
    public var enableFrameMetrics: Boolean = true

    public fun build(): PerformanceConfig = PerformanceConfig(
        targetFrameRate = targetFrameRate.coerceIn(30, 144),
        frameBudgetMs = frameBudgetMs,
        backdropCacheSizeBytes = backdropCacheSizeBytes.coerceAtLeast(1024 * 1024),
        enableAdaptiveProfiles = enableAdaptiveProfiles,
        adaptationWindowFrames = adaptationWindowFrames.coerceIn(30, 600),
        pauseAnimationsWhenNotVisible = pauseAnimationsWhenNotVisible,
        enableFrameMetrics = enableFrameMetrics,
    )
}

public data class LoggingConfig(
    val level: LiquidFlowLogLevel = LiquidFlowLogLevel.WARN,
    /** Prefix applied to every log tag: `<prefix><tag>`. */
    val tagPrefix: String = "LiquidFlow",
    /** Number of events retained in memory for [LiquidFlow.diagnostics]. */
    val retainedEventCount: Int = 200,
)
