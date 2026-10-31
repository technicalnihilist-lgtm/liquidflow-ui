package dev.liquidflow.core

/**
 * The rendering profile decides how much visual work LiquidFlow is allowed to do.
 *
 * [AUTO] is not a real profile: it asks LiquidFlow to pick a concrete profile from
 * [DeviceCapabilities] once, at initialization time, and to let `liquidflow-adaptive`
 * adjust it later using measured frame times.
 */
public enum class RenderingProfile {
    /** Let the framework choose, then adapt at runtime using measured frame times. */
    AUTO,

    /** Full visual fidelity: native or cached blur, all glass layers, full motion presets. */
    HIGH,

    /** Balanced: cached blur, fewer composited layers, slightly tightened motion. */
    BALANCED,

    /** Lite: minimal blur, minimal layers, short transitions, lowest rendering overhead. */
    LITE;

    /** True for [HIGH], [BALANCED] and [LITE]. */
    public val isConcrete: Boolean
        get() = this != AUTO

    public companion object {
        /**
         * Resolves [profile] into a concrete profile for [capabilities].
         * Explicit profiles are returned unchanged: a developer who asks for [HIGH] gets [HIGH],
         * even on hardware where it will fall back to cheaper effects internally.
         */
        public fun resolve(profile: RenderingProfile, capabilities: DeviceCapabilities): RenderingProfile =
            if (profile.isConcrete) profile else RenderingProfileResolver.resolve(capabilities)

        /** The next cheaper profile, or [LITE] if already there. */
        public fun cheaperThan(profile: RenderingProfile): RenderingProfile = when (profile) {
            AUTO, HIGH -> BALANCED
            BALANCED -> LITE
            LITE -> LITE
        }

        /** The next richer profile, or [HIGH] if already there. */
        public fun richerThan(profile: RenderingProfile): RenderingProfile = when (profile) {
            AUTO, LITE -> BALANCED
            BALANCED, HIGH -> HIGH
        }
    }
}

/**
 * Concrete rendering budget attached to a [RenderingProfile].
 *
 * Components read these values instead of hard-coding effect budgets, which is what makes
 * profile switching a real behaviour change rather than a label.
 */
public data class RenderingLimits(
    /** Upper bound for any blur radius requested by a component, in dp. */
    val maxBlurRadiusDp: Float,
    /** Integer downscale factor applied before blurring a backdrop. Higher is cheaper. */
    val blurDownscaleFactor: Int,
    /** Whether `RenderEffect`-based GPU blur may be used when the device reports support. */
    val allowNativeBlur: Boolean,
    /** Whether a backdrop is blurred at all. When false, glass uses tint only. */
    val allowBackdropBlur: Boolean,
    /** Whether the moving specular highlight layer is drawn. */
    val allowSpecularHighlight: Boolean,
    /** Whether the inner shadow / refraction band is drawn. */
    val allowInnerShadow: Boolean,
    /** Whether the fine grain noise layer is drawn (breaks up banding on gradients). */
    val allowNoiseGrain: Boolean,
    /** Multiplier applied to every motion duration and spring stiffness. */
    val motionScale: Float,
    /** Soft cap on simultaneously animating surfaces; extra surfaces snap instead. */
    val maxConcurrentAnimatedSurfaces: Int,
) {
    public companion object {
        public fun forProfile(profile: RenderingProfile, capabilities: DeviceCapabilities): RenderingLimits {
            val concrete = RenderingProfile.resolve(profile, capabilities)
            return when (concrete) {
                RenderingProfile.HIGH -> RenderingLimits(
                    maxBlurRadiusDp = 28f,
                    blurDownscaleFactor = 2,
                    allowNativeBlur = capabilities.nativeBlurAvailable,
                    allowBackdropBlur = true,
                    allowSpecularHighlight = true,
                    allowInnerShadow = true,
                    allowNoiseGrain = true,
                    motionScale = 1f,
                    maxConcurrentAnimatedSurfaces = 12,
                )

                RenderingProfile.BALANCED -> RenderingLimits(
                    maxBlurRadiusDp = 18f,
                    blurDownscaleFactor = 4,
                    allowNativeBlur = capabilities.nativeBlurAvailable,
                    allowBackdropBlur = true,
                    allowSpecularHighlight = true,
                    allowInnerShadow = false,
                    allowNoiseGrain = false,
                    motionScale = 0.92f,
                    maxConcurrentAnimatedSurfaces = 6,
                )

                RenderingProfile.LITE, RenderingProfile.AUTO -> RenderingLimits(
                    maxBlurRadiusDp = 10f,
                    blurDownscaleFactor = 8,
                    allowNativeBlur = false,
                    allowBackdropBlur = false,
                    allowSpecularHighlight = false,
                    allowInnerShadow = false,
                    allowNoiseGrain = false,
                    motionScale = 0.85f,
                    maxConcurrentAnimatedSurfaces = 3,
                )
            }
        }
    }
}
