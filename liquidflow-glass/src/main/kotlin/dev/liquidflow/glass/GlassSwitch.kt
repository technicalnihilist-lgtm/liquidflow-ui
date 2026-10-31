package dev.liquidflow.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.liquidflow.motion.currentMotionConfig

/**
 * A glass switch drawn from primitives, with a spring-driven thumb.
 *
 * The thumb position and the accent wash animate with the ambient preset, so a switch feels the
 * same as every other LiquidFlow control. The control exposes [Role.Switch] semantics and a 48dp
 * minimum touch height.
 */
@Composable
public fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: GlassStyle = LocalGlassStyle.current.copy(cornerRadius = 16.dp, transparency = 0.34f),
    colors: GlassColors = LocalGlassColors.current,
    trackWidth: Dp = 52.dp,
    trackHeight: Dp = 32.dp,
    contentDescription: String? = null,
) {
    val motion = currentMotionConfig()
    val trackAlpha = if (enabled) 1f else 0.45f

    val thumbProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = motion.preset.animationSpec(motion),
        label = "glassSwitchThumb",
    )
    val accentAlpha by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = motion.preset.animationSpec(motion),
        label = "glassSwitchAccent",
    )

    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .size(width = trackWidth, height = trackHeight)
            .then(semanticsModifier)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
    ) {
        val corner = CornerRadius(size.height / 2f, size.height / 2f)

        val accentWash = Color(
            red = colors.accent.red,
            green = colors.accent.green,
            blue = colors.accent.blue,
            alpha = 0.75f * accentAlpha * trackAlpha,
        )
        val baseTrack = colors.tint.copy(alpha = (1f - accentAlpha) * style.transparency * trackAlpha)

        // Track: glass base, then an accent wash that fades in as the switch turns on.
        drawRoundRect(color = baseTrack, cornerRadius = corner)
        drawRoundRect(color = accentWash, cornerRadius = corner)
        drawRoundRect(
            color = colors.border.copy(alpha = colors.border.alpha * trackAlpha),
            cornerRadius = corner,
            style = Stroke(width = 1.dp.toPx()),
        )

        // Thumb
        val thumbRadius = (size.height - 6.dp.toPx()) / 2f
        val startX = thumbRadius + 3.dp.toPx()
        val endX = size.width - thumbRadius - 3.dp.toPx()
        val centreX = startX + (endX - startX) * thumbProgress

        drawCircle(
            color = Color.Black.copy(alpha = 0.16f * trackAlpha),
            radius = thumbRadius,
            center = Offset(centreX, size.height / 2f + 1.dp.toPx()),
        )
        drawCircle(
            color = colors.content.copy(alpha = if (enabled) 0.96f else 0.6f),
            radius = thumbRadius,
            center = Offset(centreX, size.height / 2f),
        )
    }
}
