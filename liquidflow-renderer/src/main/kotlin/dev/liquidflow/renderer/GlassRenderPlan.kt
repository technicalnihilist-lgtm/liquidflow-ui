package dev.liquidflow.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs

/**
 * A fully resolved description of one glass surface.
 *
 * Plans are built inside `Modifier.drawWithCache`, so brushes, paths and shaders are created only
 * when the surface size or style actually changes — never per frame. Drawing then costs a handful
 * of `drawPath` calls and no allocations.
 */
public class GlassRenderPlan internal constructor(
    public val outline: Path,
    public val size: Size,
    public val cornerRadiusPx: Float,
    public val layers: Set<GlassLayer>,
    public val tintColor: Color,
    public val gradientBrush: Brush,
    public val specularBrush: Brush,
    public val borderBrush: Brush,
    public val innerShadowBrush: Brush,
    public val noiseBrush: Brush?,
    public val borderWidthPx: Float,
)

/** Draws a [GlassRenderPlan] in the correct layer order. */
public fun DrawScope.drawGlass(plan: GlassRenderPlan) {
    if (plan.size.width <= 0f || plan.size.height <= 0f) return

    if (GlassLayer.TINT in plan.layers) {
        drawPath(plan.outline, plan.tintColor)
    }
    if (GlassLayer.GRADIENT in plan.layers) {
        drawPath(plan.outline, plan.gradientBrush)
    }
    if (GlassLayer.SPECULAR in plan.layers) {
        drawPath(plan.outline, plan.specularBrush)
    }
    if (GlassLayer.INNER_SHADOW in plan.layers) {
        clipPath(plan.outline) {
            drawRect(
                brush = plan.innerShadowBrush,
                topLeft = Offset.Zero,
                size = Size(plan.size.width, plan.size.height),
                style = Fill,
            )
        }
    }
    if (GlassLayer.NOISE in plan.layers) {
        plan.noiseBrush?.let { brush -> drawPath(plan.outline, brush) }
    }
    if (GlassLayer.BORDER in plan.layers && plan.borderWidthPx > 0f) {
        drawPath(
            path = plan.outline,
            brush = plan.borderBrush,
            style = Stroke(width = plan.borderWidthPx),
        )
    }
}

/** Builds a plan. Call from `drawWithCache` only. */
public fun buildGlassRenderPlan(
    shape: Shape,
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density,
    layers: Set<GlassLayer>,
    tintColor: Color,
    gradientTop: Color,
    gradientBottom: Color,
    specularColor: Color,
    borderColor: Color,
    innerShadowColor: Color,
    borderWidthPx: Float,
    cornerRadiusPx: Float,
    noiseOpacity: Float = 0.035f,
): GlassRenderPlan {
    val path = Path()
    when (val outline = shape.createOutline(size, layoutDirection, density)) {
        is Outline.Rounded -> path.addRoundRect(outline.roundRect)
        is Outline.Generic -> path.addPath(outline.path)
        is Outline.Rectangle -> path.addRect(outline.rect)
    }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(gradientTop, gradientBottom),
        startY = 0f,
        endY = size.height,
    )

    val specularBrush = Brush.verticalGradient(
        0f to specularColor,
        0.35f to specularColor.copy(alpha = specularColor.alpha * 0.25f),
        1f to Color.Transparent,
        startY = 0f,
        endY = size.height * 0.6f,
    )

    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            borderColor,
            borderColor.copy(alpha = borderColor.alpha * 0.35f),
            borderColor.copy(alpha = borderColor.alpha * 0.8f),
        ),
        startY = 0f,
        endY = size.height,
    )

    val innerShadowBrush = Brush.verticalGradient(
        0f to innerShadowColor,
        1f to Color.Transparent,
        startY = 0f,
        endY = size.height.coerceAtLeast(1f) * 0.5f,
    )

    val noiseBrush: Brush? = if (GlassLayer.NOISE in layers && noiseOpacity > 0f) {
        val tile: ImageBitmap = NoiseTexture.tile()
        ShaderBrush(
            ImageShader(
                image = tile,
                tileModeX = TileMode.Repeated,
                tileModeY = TileMode.Repeated,
            ),
        )
    } else {
        null
    }

    return GlassRenderPlan(
        outline = path,
        size = size,
        cornerRadiusPx = cornerRadiusPx,
        layers = layers,
        tintColor = tintColor,
        gradientBrush = gradientBrush,
        specularBrush = specularBrush,
        borderBrush = borderBrush,
        innerShadowBrush = innerShadowBrush,
        noiseBrush = noiseBrush,
        borderWidthPx = borderWidthPx,
    )
}

/**
 * A small deterministic noise tile, generated once per process.
 *
 * Using a tiled 64x64 texture instead of per-pixel noise keeps the cost to a single shader draw
 * and avoids the banding that plain gradients show on 8-bit displays.
 */
public object NoiseTexture {

    private const val TILE_SIZE = 64

    @Volatile
    private var cached: ImageBitmap? = null

    public fun tile(): ImageBitmap {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val pixels = IntArray(TILE_SIZE * TILE_SIZE)
            var seed = 0x9E3779B9.toInt()
            for (index in pixels.indices) {
                // xorshift: deterministic, no allocation, no Random dependency
                seed = seed xor (seed shl 13)
                seed = seed xor (seed ushr 17)
                seed = seed xor (seed shl 5)
                val value = abs(seed % 255)
                val alpha = (value / 6).coerceIn(0, 42)
                pixels[index] = (alpha shl 24) or (value shl 16) or (value shl 8) or value
            }
            val bitmap = ImageBitmap(TILE_SIZE, TILE_SIZE)
            bitmap.asAndroidBitmap().setPixels(pixels, 0, TILE_SIZE, 0, 0, TILE_SIZE, TILE_SIZE)
            cached = bitmap
            return bitmap
        }
    }

    /** Drops the cached tile; the next call regenerates it. */
    public fun release() {
        synchronized(this) { cached = null }
    }
}
