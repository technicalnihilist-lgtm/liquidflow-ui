package dev.liquidflow.renderer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.liquidflow.core.LiquidFlow
import dev.liquidflow.renderer.NativeBlur.nativeBlur
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Hosts background content that glass surfaces sit on top of.
 *
 * On Android 12+ with a working `RenderEffect` this is a single GPU blur on the content layer.
 * On Android 10/11 (or when the active profile forbids blur) the content is drawn unchanged, and
 * glass surfaces above it rely on tint, gradient and border instead — a deliberate, cheap fallback
 * rather than a pretend blur.
 *
 * Put this *behind* your glass UI, not around it:
 *
 * ```
 * Box {
 *     GlassBackdrop(Modifier.fillMaxSize()) { BackgroundArtwork() }
 *     GlassCard { ... }
 * }
 * ```
 */
@Composable
public fun GlassBackdrop(
    modifier: Modifier = Modifier,
    blurRadius: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val state by LiquidFlow.state.collectAsState()
    val limits = state.limits
    val density = LocalDensity.current

    val supported = NativeBlur.isSupported(state.capabilities) &&
        limits.allowNativeBlur &&
        limits.allowBackdropBlur &&
        state.config.blurIntensity > 0f

    val effectiveRadius = if (supported) {
        minOf(blurRadius, limits.maxBlurRadiusDp.dp) * state.config.blurIntensity
    } else {
        0.dp
    }

    Box(
        modifier = modifier.then(
            if (supported) {
                Modifier.nativeBlur(radiusDp = effectiveRadius.value, enabled = true, density = density)
            } else {
                Modifier
            },
        ),
        content = content,
    )
}

/**
 * Draws a static image as a glass backdrop, blurred through the cached CPU pipeline.
 *
 * Blurring happens on `Dispatchers.Default`, never on the main thread, and results are cached by
 * [BitmapBlurProcessor]. This is the path used on Android 10 and 11 for photographic backdrops.
 */
@Composable
public fun BitmapGlassBackdrop(
    image: ImageBitmap,
    modifier: Modifier = Modifier,
    blurRadius: Dp = 20.dp,
    contentScale: ContentScale = ContentScale.Crop,
    downscale: Int = 4,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val state by LiquidFlow.state.collectAsState()
    val density = LocalDensity.current
    val radiusPx = with(density) { blurRadius.toPx().toInt() }.coerceAtLeast(1)

    val shouldBlur = state.limits.allowBackdropBlur && state.config.blurIntensity > 0f
    val useNative = NativeBlur.isSupported(state.capabilities) && state.limits.allowNativeBlur

    val blurred: State<ImageBitmap?> = produceState<ImageBitmap?>(
        initialValue = null,
        image,
        radiusPx,
        downscale,
        shouldBlur,
        useNative,
    ) {
        value = if (!shouldBlur || useNative) {
            null
        } else {
            withContext(Dispatchers.Default) {
                runCatching {
                    LiquidFlowRenderer.blurProcessor()
                        .blur(image.asAndroidBitmap(), radiusPx, downscale)
                        .asImageBitmap()
                }.getOrNull()
            }
        }
    }

    Box(modifier = modifier) {
        if (useNative && shouldBlur) {
            val capped = minOf(blurRadius, state.limits.maxBlurRadiusDp.dp) * state.config.blurIntensity
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nativeBlur(radiusDp = capped.value, density = density),
            ) {
                BackdropImage(image, contentScale)
            }
        } else {
            BackdropImage(blurred.value ?: image, contentScale)
        }
        overlay()
    }
}

@Composable
private fun BackdropImage(image: ImageBitmap, contentScale: ContentScale) {
    Image(
        bitmap = image,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = contentScale,
        alignment = Alignment.Center,
    )
}
