package dev.liquidflow.renderer

import dev.liquidflow.core.LiquidFlow

/**
 * Owns the renderer's shared, process-wide resources: the blur pipeline and its cache.
 *
 * A single shared cache matters: without it, every glass surface would keep its own blurred copy
 * of the same backdrop, which is the classic way a "glass" UI ends up using hundreds of megabytes.
 *
 * Resources are registered with [LiquidFlow] so that `LiquidFlow.shutdown()` (and the test suite)
 * can release them deterministically.
 */
public object LiquidFlowRenderer {

    private val lock = Any()
    private var processor: BitmapBlurProcessor? = null
    private var registered = false

    /** Returns the shared blur pipeline, creating it on first use. */
    public fun blurProcessor(): BitmapBlurProcessor {
        synchronized(lock) {
            processor?.let { return it }
            val maxBytes = LiquidFlow.config.performance.backdropCacheSizeBytes
            val created = BitmapBlurProcessor(cache = BlurCache(maxBytes))
            processor = created
            registerLocked()
            return created
        }
    }

    private fun registerLocked() {
        if (registered) return
        registered = true
        LiquidFlow.registerResourceReleaser("renderer") { release() }
        LiquidFlow.registerDiagnosticsProvider("renderer") {
            val current = processor
            buildMap {
                put("nativeBlurSupported", NativeBlur.isSupported(LiquidFlow.capabilities).toString())
                put("profile", LiquidFlow.activeProfile.toString())
                put(
                    "glassLayers",
                    GlassLayer.forLimits(LiquidFlow.limits).joinToString(",") { it.name },
                )
                if (current != null) putAll(current.cache().toDiagnosticMap())
            }
        }
    }

    /** Releases the blur cache and the noise tile. */
    public fun release() {
        synchronized(lock) {
            processor?.release()
            processor = null
        }
        NoiseTexture.release()
    }
}
