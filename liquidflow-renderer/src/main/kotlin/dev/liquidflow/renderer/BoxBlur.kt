package dev.liquidflow.renderer

/**
 * Separable box blur, applied three times to approximate a Gaussian.
 *
 * This is the CPU fallback used for cached glass backdrops on devices without a usable
 * `RenderEffect` (Android 10 and 11, and any device that fails the runtime probe). It is
 * deliberately plain Kotlin over `IntArray` so it can be unit tested on the JVM, and so that it
 * allocates exactly two buffers regardless of radius.
 *
 * Alpha handling: channels are averaged without premultiplication, which is correct for the
 * opaque backdrops LiquidFlow captures. Translucent sources are composited onto an opaque canvas
 * before reaching this function (see [BitmapBlurProcessor]).
 */
public object BoxBlur {

    /** Number of passes. Three box passes are visually indistinguishable from a Gaussian. */
    public const val PASSES: Int = 3

    /**
     * @param pixels source ARGB_8888 pixels, row-major, length `width * height`
     * @return a new blurred array; the input is never modified
     */
    public fun blur(pixels: IntArray, width: Int, height: Int, radius: Int): IntArray {
        require(width > 0 && height > 0) { "width and height must be positive" }
        require(pixels.size >= width * height) {
            "pixels must contain at least width*height entries (${width * height}), got ${pixels.size}"
        }
        val maxRadius = minOf(width, height) / 2
        val effectiveRadius = radius.coerceIn(0, maxRadius)
        if (effectiveRadius == 0) return pixels.copyOf(width * height)

        var current = pixels.copyOf(width * height)
        var scratch = IntArray(width * height)
        repeat(PASSES) {
            blurHorizontal(current, scratch, width, height, effectiveRadius)
            blurVertical(scratch, current, width, height, effectiveRadius)
        }
        return current
    }

    private fun blurHorizontal(src: IntArray, dst: IntArray, width: Int, height: Int, radius: Int) {
        val window = 2 * radius + 1
        for (y in 0 until height) {
            val rowStart = y * width
            var sumA = 0
            var sumR = 0
            var sumG = 0
            var sumB = 0
            for (offset in -radius..radius) {
                val pixel = src[rowStart + offset.coerceIn(0, width - 1)]
                sumA += (pixel ushr 24) and 0xFF
                sumR += (pixel shr 16) and 0xFF
                sumG += (pixel shr 8) and 0xFF
                sumB += pixel and 0xFF
            }
            val half = window / 2
            for (x in 0 until width) {
                dst[rowStart + x] = (((sumA + half) / window) shl 24) or
                    (((sumR + half) / window) shl 16) or
                    (((sumG + half) / window) shl 8) or
                    ((sumB + half) / window)
                val leaving = src[rowStart + (x - radius).coerceIn(0, width - 1)]
                val entering = src[rowStart + (x + radius + 1).coerceIn(0, width - 1)]
                sumA += ((entering ushr 24) and 0xFF) - ((leaving ushr 24) and 0xFF)
                sumR += ((entering shr 16) and 0xFF) - ((leaving shr 16) and 0xFF)
                sumG += ((entering shr 8) and 0xFF) - ((leaving shr 8) and 0xFF)
                sumB += (entering and 0xFF) - (leaving and 0xFF)
            }
        }
    }

    private fun blurVertical(src: IntArray, dst: IntArray, width: Int, height: Int, radius: Int) {
        val window = 2 * radius + 1
        for (x in 0 until width) {
            var sumA = 0
            var sumR = 0
            var sumG = 0
            var sumB = 0
            for (offset in -radius..radius) {
                val pixel = src[offset.coerceIn(0, height - 1) * width + x]
                sumA += (pixel ushr 24) and 0xFF
                sumR += (pixel shr 16) and 0xFF
                sumG += (pixel shr 8) and 0xFF
                sumB += pixel and 0xFF
            }
            val half = window / 2
            for (y in 0 until height) {
                dst[y * width + x] = (((sumA + half) / window) shl 24) or
                    (((sumR + half) / window) shl 16) or
                    (((sumG + half) / window) shl 8) or
                    ((sumB + half) / window)
                val leaving = src[(y - radius).coerceIn(0, height - 1) * width + x]
                val entering = src[(y + radius + 1).coerceIn(0, height - 1) * width + x]
                sumA += ((entering ushr 24) and 0xFF) - ((leaving ushr 24) and 0xFF)
                sumR += ((entering shr 16) and 0xFF) - ((leaving shr 16) and 0xFF)
                sumG += ((entering shr 8) and 0xFF) - ((leaving shr 8) and 0xFF)
                sumB += (entering and 0xFF) - (leaving and 0xFF)
            }
        }
    }
}
