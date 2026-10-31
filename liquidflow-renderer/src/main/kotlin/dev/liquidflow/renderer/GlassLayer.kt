package dev.liquidflow.renderer

import dev.liquidflow.core.RenderingLimits
import dev.liquidflow.core.RenderingProfile

/**
 * The individual visual layers a glass surface is built from.
 *
 * Making the layers explicit is what allows the adaptive profiles to reduce work precisely:
 * a LITE surface simply does not draw the expensive ones, instead of drawing a cheaper version of
 * everything.
 */
public enum class GlassLayer {
    /** The blurred backdrop sampled behind the surface. */
    BACKDROP_BLUR,

    /** Flat translucent tint. */
    TINT,

    /** Vertical sheen gradient that gives the surface volume. */
    GRADIENT,

    /** Moving specular highlight along the top edge. */
    SPECULAR,

    /** Hairline border. */
    BORDER,

    /** Inner shadow / refraction band just inside the border. */
    INNER_SHADOW,

    /** Fine grain that prevents banding in the gradient. */
    NOISE;

    public companion object {
        /** Layers allowed by [limits]. */
        public fun forLimits(limits: RenderingLimits): Set<GlassLayer> = buildSet {
            add(TINT)
            add(GRADIENT)
            add(BORDER)
            if (limits.allowBackdropBlur) add(BACKDROP_BLUR)
            if (limits.allowSpecularHighlight) add(SPECULAR)
            if (limits.allowInnerShadow) add(INNER_SHADOW)
            if (limits.allowNoiseGrain) add(NOISE)
        }

        /** Layers allowed by [profile] under [limits]. */
        public fun forProfile(profile: RenderingProfile, limits: RenderingLimits): Set<GlassLayer> =
            if (profile == RenderingProfile.LITE) setOf(TINT, GRADIENT, BORDER) else forLimits(limits)
    }
}
