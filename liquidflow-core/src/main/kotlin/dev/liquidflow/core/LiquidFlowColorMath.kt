package dev.liquidflow.core

/**
 * Pure ARGB colour maths used for accessible contrast.
 *
 * Deliberately dependency-free (no `android.graphics`, no Compose) so that the contrast rules the
 * framework ships with are covered by plain JVM unit tests.
 */
public object LiquidFlowColorMath {

    /** WCAG 2.1 minimum contrast for large text and non-text UI. */
    public const val CONTRAST_AA_LARGE: Double = 3.0

    /** WCAG 2.1 minimum contrast for body text. */
    public const val CONTRAST_AA: Double = 4.5

    /** WCAG 2.1 minimum contrast for AAA body text. */
    public const val CONTRAST_AAA: Double = 7.0

    public fun alphaOf(argb: Int): Int = (argb ushr 24) and 0xFF

    public fun redOf(argb: Int): Int = (argb shr 16) and 0xFF

    public fun greenOf(argb: Int): Int = (argb shr 8) and 0xFF

    public fun blueOf(argb: Int): Int = argb and 0xFF

    public fun argb(a: Int, r: Int, g: Int, b: Int): Int =
        ((a and 0xFF) shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)

    /** Composites [foreground] over [background]; both opaque-aware, alpha is preserved. */
    public fun blend(foreground: Int, background: Int, fraction: Double = 1.0): Int {
        val f = fraction.coerceIn(0.0, 1.0)
        val bgA = alphaOf(background) / 255.0
        val fgA = alphaOf(foreground) / 255.0
        val outA = fgA * f + bgA * (1 - fgA * f)
        if (outA <= 0.0) return 0
        fun channel(fg: Int, bg: Int): Int {
            val mixed = (fg * fgA * f + bg * bgA * (1 - fgA * f)) / outA
            return mixed.toInt().coerceIn(0, 255)
        }
        return argb(
            a = (outA * 255).toInt().coerceIn(0, 255),
            r = channel(redOf(foreground), redOf(background)),
            g = channel(greenOf(foreground), greenOf(background)),
            b = channel(blueOf(foreground), blueOf(background)),
        )
    }

    private fun linearize(channel: Int): Double {
        val c = channel / 255.0
        return if (c <= 0.04045) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
    }

    /** WCAG relative luminance of an opaque colour. */
    public fun relativeLuminance(argb: Int): Double {
        val r = linearize(redOf(argb))
        val g = linearize(greenOf(argb))
        val b = linearize(blueOf(argb))
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /** WCAG contrast ratio between two colours, in `1.0 .. 21.0`. */
    public fun contrastRatio(foreground: Int, background: Int): Double {
        val l1 = relativeLuminance(foreground)
        val l2 = relativeLuminance(background)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    /**
     * Returns [foreground] when it already meets [minimumRatio] against [background], otherwise
     * the closest colour on the black/white axis that does.
     *
     * This is what keeps glass surfaces readable when a busy photo scrolls behind them: rather
     * than guessing, the framework measures the real backdrop colour and adjusts the text.
     */
    public fun ensureContrast(foreground: Int, background: Int, minimumRatio: Double = CONTRAST_AA): Int {
        val opaqueBackground = if (alphaOf(background) < 255) {
            blend(background, argb(255, 0, 0, 0))
        } else {
            background
        }
        if (contrastRatio(foreground, opaqueBackground) >= minimumRatio) return foreground

        val black = argb(alphaOf(foreground), 0, 0, 0)
        val white = argb(alphaOf(foreground), 255, 255, 255)
        val blackIsBetter =
            contrastRatio(black, opaqueBackground) >= contrastRatio(white, opaqueBackground)
        val target = if (blackIsBetter) black else white

        // Walk from the original colour toward the target until the ratio is met, so the result
        // stays as close to the design colour as possible.
        var best = target
        val steps = 20
        for (i in 1..steps) {
            val candidate = blend(target, foreground, i.toDouble() / steps)
            if (contrastRatio(candidate, opaqueBackground) >= minimumRatio) {
                best = candidate
                break
            }
        }
        return best
    }

    /** Perceived brightness in `0 .. 255`, used to decide light-on-dark vs dark-on-light. */
    public fun perceivedBrightness(argb: Int): Double =
        (redOf(argb) * 0.299 + greenOf(argb) * 0.587 + blueOf(argb) * 0.114)

    public fun isDark(argb: Int, threshold: Double = 128.0): Boolean = perceivedBrightness(argb) < threshold
}
