package dev.liquidflow.glass

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.liquidflow.core.LiquidFlow
import dev.liquidflow.core.LiquidFlowColorMath
import dev.liquidflow.motion.MotionConfig
import dev.liquidflow.motion.rememberMotionConfig

/**
 * Colours for glass surfaces.
 *
 * [backdropSample] is the colour the framework assumes is behind the glass when it checks contrast.
 * It is not used for drawing: it exists so text can be kept readable over a busy background without
 * sampling pixels at runtime.
 */
@Immutable
public data class GlassColors(
    val tint: Color,
    val gradientTop: Color,
    val gradientBottom: Color,
    val specular: Color,
    val border: Color,
    val innerShadow: Color,
    val content: Color,
    val contentSecondary: Color,
    val accent: Color,
    val backdropSample: Color,
) {
    public companion object {
        public fun light(
            accent: Color = Color(0xFF1D6FF2),
            tint: Color = Color(0xFFFFFFFF),
        ): GlassColors = GlassColors(
            tint = tint,
            gradientTop = Color(0x66FFFFFF),
            gradientBottom = Color(0x1AFFFFFF),
            specular = Color(0x99FFFFFF),
            border = Color(0x59FFFFFF),
            innerShadow = Color(0x14000000),
            content = Color(0xFF10131A),
            contentSecondary = Color(0xFF3C4353),
            accent = accent,
            backdropSample = Color(0xFFE8ECF4),
        )

        public fun dark(
            accent: Color = Color(0xFF7FB4FF),
            tint: Color = Color(0xFF10131A),
        ): GlassColors = GlassColors(
            tint = tint,
            gradientTop = Color(0x33FFFFFF),
            gradientBottom = Color(0x0DFFFFFF),
            specular = Color(0x66FFFFFF),
            border = Color(0x40FFFFFF),
            innerShadow = Color(0x33000000),
            content = Color(0xFFF3F6FC),
            contentSecondary = Color(0xFFB6BECF),
            accent = accent,
            backdropSample = Color(0xFF0B0E14),
        )
    }
}

/**
 * Appearance of a single glass surface.
 *
 * Everything a developer is likely to want to change per component lives here, so a component can
 * be restyled with `style = GlassStyle(transparency = 0.4f, cornerRadius = 8.dp)` rather than by
 * wrapping it in modifiers.
 */
@Immutable
public data class GlassStyle(
    /** How opaque the surface is, `0f` = invisible, `1f` = fully tinted. */
    val transparency: Float = 0.26f,
    /** Overrides the theme tint for this surface only. */
    val tint: Color? = null,
    val cornerRadius: Dp = 24.dp,
    val borderThickness: Dp = 1.dp,
    /** Strength of the top specular highlight, `0f .. 1f`. */
    val highlightIntensity: Float = 0.85f,
    /** Requested blur radius. Clamped by the active rendering profile. */
    val blurRadius: Dp = 20.dp,
    /** Overrides the theme content colour for this surface only. */
    val contentColor: Color? = null,
) {
    public fun withTransparency(value: Float): GlassStyle = copy(transparency = value.coerceIn(0f, 1f))

    public fun withCornerRadius(value: Dp): GlassStyle = copy(cornerRadius = value)

    public fun withTint(value: Color): GlassStyle = copy(tint = value)

    /** Tint actually used for drawing, after applying the global transparency multiplier. */
    public fun resolvedTint(colors: GlassColors, globalTransparency: Float): Color {
        val base = tint ?: colors.tint
        val alpha = (transparency * globalTransparency).coerceIn(0f, 1f)
        return base.copy(alpha = base.alpha * alpha)
    }

    /**
     * Content colour that meets WCAG AA against [GlassColors.backdropSample].
     * Glass over a busy photo is exactly where contrast usually fails, so this is measured, not guessed.
     */
    public fun resolvedContentColor(colors: GlassColors): Color =
        contentColor ?: GlassAccessibility.ensureReadable(colors.content, colors.backdropSample)

    public companion object {
        public val Default: GlassStyle = GlassStyle()

        /** Compact style for chips, buttons and small controls. */
        public fun compact(): GlassStyle = GlassStyle(
            cornerRadius = 16.dp,
            transparency = 0.22f,
            highlightIntensity = 0.7f,
            blurRadius = 14.dp,
        )

        /** Large style for panels and sheets. */
        public fun panel(): GlassStyle = GlassStyle(
            cornerRadius = 28.dp,
            transparency = 0.3f,
            highlightIntensity = 1f,
            blurRadius = 24.dp,
        )
    }
}

/** Contrast helpers, shared by every glass component. */
public object GlassAccessibility {

    public fun contrastRatio(foreground: Color, background: Color): Double =
        LiquidFlowColorMath.contrastRatio(foreground.toArgbCompat(), background.toArgbCompat())

    /**
     * Returns [foreground] if it already meets [minimumRatio] against [background], otherwise the
     * nearest readable variant of it.
     */
    public fun ensureReadable(
        foreground: Color,
        background: Color,
        minimumRatio: Double = LiquidFlowColorMath.CONTRAST_AA,
    ): Color {
        val adjusted = LiquidFlowColorMath.ensureContrast(
            foreground = foreground.toArgbCompat(),
            background = background.toArgbCompat(),
            minimumRatio = minimumRatio,
        )
        return Color(adjusted)
    }

    /** True when the pairing is readable; useful in tests and in the sample's audit screen. */
    public fun isReadable(
        foreground: Color,
        background: Color,
        minimumRatio: Double = LiquidFlowColorMath.CONTRAST_AA,
    ): Boolean = contrastRatio(foreground, background) >= minimumRatio
}

internal fun Color.toArgbCompat(): Int = this.toArgb()

public val LocalGlassColors: ProvidableCompositionLocal<GlassColors> =
    staticCompositionLocalOf { GlassColors.dark() }

public val LocalGlassStyle: ProvidableCompositionLocal<GlassStyle> =
    staticCompositionLocalOf { GlassStyle.Default }

public val LocalGlassDarkTheme: ProvidableCompositionLocal<Boolean> =
    staticCompositionLocalOf { true }

/**
 * Theme for LiquidFlow components.
 *
 * It provides glass colours and style, the ambient [MotionConfig], and a matching Material 3
 * [MaterialTheme] so Material components placed next to LiquidFlow ones do not look foreign.
 *
 * ```
 * LiquidFlowTheme(darkTheme = isSystemInDarkTheme()) {
 *     GlassCard { Text("Hello") }
 * }
 * ```
 */
@Composable
public fun LiquidFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colors: GlassColors = if (darkTheme) GlassColors.dark() else GlassColors.light(),
    style: GlassStyle = GlassStyle.Default,
    motion: MotionConfig = rememberMotionConfig(),
    content: @Composable () -> Unit,
) {
    // A concrete profile is applied once per process, so this read is cheap and stable.
    LiquidFlow.warnIfUninitialized()

    val materialColors = remember(colors, darkTheme) {
        val base = if (darkTheme) darkColorScheme() else lightColorScheme()
        base.copy(
            primary = colors.accent,
            onPrimary = if (darkTheme) Color(0xFF04121F) else Color.White,
            background = colors.backdropSample,
            onBackground = colors.content,
            surface = colors.tint,
            onSurface = colors.content,
            surfaceVariant = colors.gradientTop,
            onSurfaceVariant = colors.contentSecondary,
        )
    }

    CompositionLocalProvider(
        LocalGlassColors provides colors,
        LocalGlassStyle provides style,
        LocalGlassDarkTheme provides darkTheme,
        dev.liquidflow.motion.LocalMotionConfig provides motion,
    ) {
        MaterialTheme(colorScheme = materialColors, content = content)
    }
}
