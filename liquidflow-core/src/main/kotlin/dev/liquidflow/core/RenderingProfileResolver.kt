package dev.liquidflow.core

/**
 * Maps [DeviceCapabilities] onto a starting [RenderingProfile].
 *
 * The rules are intentionally conservative: the cost of under-delivering on an expensive device
 * is a slightly plainer effect, while the cost of over-committing on a cheap device is dropped
 * frames on every scroll. When in doubt this resolver picks the cheaper profile, and
 * `liquidflow-adaptive` can promote it later if measured frame times allow.
 */
public object RenderingProfileResolver {

    /** Below this much RAM a device is treated as memory-constrained regardless of its flags. */
    public const val MIN_RAM_MB_FOR_CACHED_BLUR: Int = 2048

    /** Minimum RAM for the full effect stack. */
    public const val MIN_RAM_MB_FOR_HIGH: Int = 4096

    /** Minimum RAM for the balanced profile. */
    public const val MIN_RAM_MB_FOR_BALANCED: Int = 2560

    /** Below this refresh rate a device is assumed to be a 60 Hz budget panel. */
    public const val MIN_REFRESH_RATE_HZ: Float = 60f

    public fun resolve(capabilities: DeviceCapabilities): RenderingProfile {
        // A device that cannot even afford a cached CPU blur gets the cheapest path.
        if (!capabilities.canAffordCachedBlur) return RenderingProfile.LITE

        // Native GPU blur is the single biggest win, but it is not sufficient on its own:
        // a low-RAM device with RenderEffect still stutters when compositing many layers.
        return when {
            capabilities.nativeBlurAvailable &&
                capabilities.isHighEnd &&
                !capabilities.isThermallyThrottled -> RenderingProfile.HIGH

            capabilities.isMidRange -> RenderingProfile.BALANCED

            capabilities.nativeBlurAvailable && capabilities.totalRamMb >= MIN_RAM_MB_FOR_CACHED_BLUR ->
                RenderingProfile.BALANCED

            else -> RenderingProfile.LITE
        }
    }

    /**
     * Resolves a developer-requested profile.
     *
     * Explicit requests are honoured. When an explicit request cannot be served at full fidelity
     * the framework keeps the requested profile (so the visual intent is preserved) and the
     * renderer degrades individual effects internally instead.
     *
     * @return the profile to apply, plus a human readable note when the request was downgraded.
     */
    public fun resolveRequested(
        requested: RenderingProfile,
        capabilities: DeviceCapabilities,
    ): Resolution {
        val resolved = RenderingProfile.resolve(requested, capabilities)
        val note = when {
            requested == RenderingProfile.AUTO ->
                "AUTO resolved to $resolved from device capabilities"

            requested == RenderingProfile.HIGH && !capabilities.nativeBlurAvailable ->
                "HIGH requested without native blur support; using cached blur fallback"

            else -> null
        }
        return Resolution(profile = resolved, note = note)
    }

    public data class Resolution(val profile: RenderingProfile, val note: String?)
}
