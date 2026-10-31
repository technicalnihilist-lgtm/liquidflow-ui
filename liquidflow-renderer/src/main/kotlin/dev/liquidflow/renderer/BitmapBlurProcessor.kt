package dev.liquidflow.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache

/**
 * Bounded cache of blurred backdrops.
 *
 * Blur is the most expensive thing LiquidFlow does, so results are cached by
 * (source identity, source size, radius, downscale). Entries are evicted by byte size, and the
 * cache is cleared whenever the host asks the framework to release resources.
 */
public class BlurCache(maxBytes: Int = DEFAULT_MAX_BYTES) {

    public data class Key(
        val sourceIdentity: Int,
        val sourceWidth: Int,
        val sourceHeight: Int,
        val radiusPx: Int,
        val downscale: Int,
    )

    private val lru = object : LruCache<Key, Bitmap>(maxBytes.coerceAtLeast(256 * 1024)) {
        override fun sizeOf(key: Key, value: Bitmap): Int = value.allocationByteCount
    }

    public fun get(key: Key): Bitmap? = lru.get(key)?.takeIf { !it.isRecycled }

    public fun put(key: Key, bitmap: Bitmap) {
        lru.put(key, bitmap)
    }

    public fun clear() = lru.evictAll()

    public fun sizeBytes(): Int = lru.size()

    public fun maxSizeBytes(): Int = lru.maxSize()

    public fun hitCount(): Int = lru.hitCount()

    public fun missCount(): Int = lru.missCount()

    public fun toDiagnosticMap(): Map<String, String> = mapOf(
        "blurCacheBytes" to sizeBytes().toString(),
        "blurCacheMaxBytes" to maxSizeBytes().toString(),
        "blurCacheHits" to hitCount().toString(),
        "blurCacheMisses" to missCount().toString(),
        "blurCacheEntries" to lru.size().toString(),
    )

    public companion object {
        public const val DEFAULT_MAX_BYTES: Int = 8 * 1024 * 1024
    }
}

/**
 * Produces blurred copies of a [Bitmap] for use as a static glass backdrop.
 *
 * The pipeline is: downscale -> box blur (3 passes) -> cache. Downscaling first is what makes this
 * affordable on Android 10/11: a 4x downscale is roughly 16x fewer pixels to blur, and the result
 * is upscaled by the GPU when drawn, which is visually indistinguishable for a blur.
 */
public class BitmapBlurProcessor(
    private val cache: BlurCache = BlurCache(),
    private val maxDimension: Int = 2048,
) {

    /** Blurs [source]. Safe to call from a background thread; never call it on the main thread. */
    public fun blur(source: Bitmap, radiusPx: Int, downscale: Int = 4): Bitmap {
        require(!source.isRecycled) { "source bitmap has been recycled" }
        val safeDownscale = downscale.coerceIn(1, 16)
        val safeRadius = radiusPx.coerceIn(0, 200)
        val key = BlurCache.Key(
            sourceIdentity = System.identityHashCode(source),
            sourceWidth = source.width,
            sourceHeight = source.height,
            radiusPx = safeRadius,
            downscale = safeDownscale,
        )
        cache.get(key)?.let { return it }

        // Composite onto an opaque canvas so the blur never has to reason about premultiplied alpha.
        val opaque = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(opaque).apply {
            drawColor(android.graphics.Color.BLACK)
            drawBitmap(source, 0f, 0f, null)
        }

        val scaledWidth = (opaque.width / safeDownscale).coerceAtLeast(1)
        val scaledHeight = (opaque.height / safeDownscale).coerceAtLeast(1)
        val cappedWidth = scaledWidth.coerceAtMost(maxDimension)
        val cappedHeight = scaledHeight.coerceAtMost(maxDimension)

        val scaled = if (cappedWidth != opaque.width || cappedHeight != opaque.height) {
            Bitmap.createScaledBitmap(opaque, cappedWidth, cappedHeight, true)
        } else {
            opaque
        }
        if (scaled !== opaque) opaque.recycle()

        val pixels = IntArray(scaled.width * scaled.height)
        scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
        val blurredPixels = BoxBlur.blur(
            pixels = pixels,
            width = scaled.width,
            height = scaled.height,
            radius = (safeRadius / safeDownscale).coerceAtLeast(1),
        )
        val output = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888)
        output.setPixels(blurredPixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
        scaled.recycle()

        cache.put(key, output)
        return output
    }

    public fun cache(): BlurCache = cache

    public fun release() {
        cache.clear()
    }
}
