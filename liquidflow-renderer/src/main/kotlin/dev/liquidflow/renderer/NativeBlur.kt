package dev.liquidflow.renderer

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.liquidflow.core.DeviceCapabilities

/**
 * Wraps the platform blur API so that callers never have to think about API levels.
 *
 * `RenderEffect` blur exists from Android 12 (API 31). Even there it is not guaranteed to work:
 * some devices and some window configurations (software layers, certain OEM skins) silently ignore
 * it. LiquidFlow therefore checks [DeviceCapabilities.nativeBlurAvailable], which is only true when
 * a blur effect was actually constructed on this device at startup.
 */
public object NativeBlur {

    /** True when this device can blur with a GPU `RenderEffect`. */
    public fun isSupported(capabilities: DeviceCapabilities): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && capabilities.nativeBlurAvailable

    /**
     * Applies a GPU blur to the layer this modifier is attached to.
     *
     * Note what this does and does not do: it blurs *this* composable's content, not whatever is
     * painted behind it. To get a blurred glass backdrop, put your background content inside
     * [GlassBackdrop] (or apply this modifier to the background composable) and draw glass surfaces
     * on top of it.
     */
    public fun Modifier.nativeBlur(
        radiusDp: Float,
        enabled: Boolean = true,
        density: Density,
    ): Modifier {
        if (!enabled || radiusDp <= 0f || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return this
        }
        val radiusPx = with(density) { radiusDp.dp.toPx() }
        return this.graphicsLayer {
            renderEffect = BlurEffect(radiusPx, radiusPx, TileMode.Clamp)
        }
    }

    /** Applies a downscale to the layer before blurring: cheaper blur, slightly softer edges. */
    public fun Modifier.nativeBlurWithDownscale(
        radiusDp: Float,
        downscale: Float,
        enabled: Boolean = true,
        density: Density,
    ): Modifier {
        if (!enabled || radiusDp <= 0f || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return this
        val radiusPx = with(density) { radiusDp.dp.toPx() }
        val scale = (1f / downscale.coerceIn(1f, 4f))
        return this.graphicsLayer {
            scaleX = scale
            scaleY = scale
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
            renderEffect = BlurEffect(radiusPx, radiusPx, TileMode.Clamp)
        }
    }
}
