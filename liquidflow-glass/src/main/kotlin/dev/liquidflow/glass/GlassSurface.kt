package dev.liquidflow.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.liquidflow.core.LiquidFlow
import dev.liquidflow.renderer.GlassLayer
import dev.liquidflow.renderer.buildGlassRenderPlan
import dev.liquidflow.renderer.drawGlass

/**
 * The drawing half of glass: clipping, layer selection and compositing.
 *
 * Layer selection is where the rendering profile does its real work. A LITE profile keeps tint,
 * gradient and border and drops blur, specular and noise, so the same component code produces a
 * cheaper surface instead of a differently-broken one.
 */
@Composable
internal fun Modifier.glassBackground(
    style: GlassStyle,
    colors: GlassColors,
    shape: Shape,
    alphaMultiplier: Float = 1f,
    glassEnabled: Boolean = true,
): Modifier {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val state by LiquidFlow.state.collectAsState()

    val limits = state.limits
    val layers: Set<GlassLayer> = if (!glassEnabled) {
        setOf(GlassLayer.TINT)
    } else {
        GlassLayer.forLimits(limits)
    }

    val surfaceAlpha = (style.transparency * alphaMultiplier * state.config.transparency).coerceIn(0f, 1f)
    val tint = style.tint ?: colors.tint
    val highlight = style.highlightIntensity.coerceIn(0f, 1f)
    val borderWidthPx = with(density) {
        if (GlassLayer.BORDER in layers) style.borderThickness.toPx() else 0f
    }
    val cornerRadiusPx = with(density) { style.cornerRadius.toPx() }

    // When glass is disabled entirely the surface becomes a plain translucent scrim: readable, cheap.
    val tintAlpha = if (glassEnabled) tint.alpha * surfaceAlpha else (0.88f * alphaMultiplier)

    return this
        .clip(shape)
        .drawWithCache {
            val plan = buildGlassRenderPlan(
                shape = shape,
                size = size,
                layoutDirection = layoutDirection,
                density = this,
                layers = layers,
                tintColor = tint.copy(alpha = tintAlpha),
                gradientTop = colors.gradientTop.copy(alpha = colors.gradientTop.alpha * surfaceAlpha),
                gradientBottom = colors.gradientBottom.copy(alpha = colors.gradientBottom.alpha * surfaceAlpha),
                specularColor = colors.specular.copy(alpha = colors.specular.alpha * surfaceAlpha * highlight),
                borderColor = colors.border.copy(
                    alpha = colors.border.alpha * (0.55f + 0.45f * surfaceAlpha),
                ),
                innerShadowColor = colors.innerShadow.copy(alpha = colors.innerShadow.alpha * surfaceAlpha),
                borderWidthPx = borderWidthPx,
                cornerRadiusPx = cornerRadiusPx,
            )
            onDrawBehind { drawGlass(plan) }
        }
}

/**
 * The base glass surface. Every other glass component is built from this.
 *
 * ```
 * GlassSurface {
 *     Text("Hello LiquidFlow")
 * }
 * ```
 *
 * @param style appearance of this surface; defaults to the theme's style
 * @param shape outline; defaults to a rounded rectangle using [GlassStyle.cornerRadius]
 * @param alphaMultiplier extra opacity multiplier, used by parents that fade their children
 */
@Composable
public fun GlassSurface(
    modifier: Modifier = Modifier,
    style: GlassStyle = LocalGlassStyle.current,
    colors: GlassColors = LocalGlassColors.current,
    shape: Shape = RoundedCornerShape(style.cornerRadius),
    alphaMultiplier: Float = 1f,
    contentAlignment: Alignment = Alignment.TopStart,
    contentDescription: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    LiquidFlow.warnIfUninitialized()
    val state by LiquidFlow.state.collectAsState()
    val contentColor = style.resolvedContentColor(colors)

    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            modifier = modifier
                .then(semanticsModifier)
                .glassBackground(
                    style = style,
                    colors = colors,
                    shape = shape,
                    alphaMultiplier = alphaMultiplier,
                    glassEnabled = state.config.glassEnabled,
                ),
            contentAlignment = contentAlignment,
            content = content,
        )
    }
}

/** Internal helper so components can paint a glass layer without a Box. */
@Composable
internal fun Modifier.glassSurfaceModifier(
    style: GlassStyle,
    colors: GlassColors,
    shape: Shape,
    alphaMultiplier: Float = 1f,
): Modifier {
    val state by LiquidFlow.state.collectAsState()
    return glassBackground(
        style = style,
        colors = colors,
        shape = shape,
        alphaMultiplier = alphaMultiplier,
        glassEnabled = state.config.glassEnabled,
    )
}
