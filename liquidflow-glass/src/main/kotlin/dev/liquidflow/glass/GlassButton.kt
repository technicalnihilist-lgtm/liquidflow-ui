package dev.liquidflow.glass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.liquidflow.motion.currentMotionConfig
import dev.liquidflow.motion.liquidClickable

/**
 * A glass button with spring press feedback.
 *
 * ```
 * GlassButton(onClick = { open() }) { Text("Open") }
 * ```
 *
 * The minimum height is 48dp and the role is [Role.Button], so it behaves like a button to
 * accessibility services even though it is drawn from scratch.
 */
@Composable
public fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: GlassStyle = LocalGlassStyle.current.copy(cornerRadius = 18.dp, transparency = 0.3f),
    colors: GlassColors = LocalGlassColors.current,
    contentPadding: PaddingValues = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
    shape: Shape = RoundedCornerShape(style.cornerRadius),
    contentDescription: String? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val motion = currentMotionConfig()
    val contentColor = style.resolvedContentColor(colors)
    val alpha = if (enabled) 1f else 0.45f

    CompositionLocalProvider(
        LocalContentColor provides contentColor.copy(alpha = contentColor.alpha * alpha),
    ) {
        GlassSurface(
            modifier = modifier
                .defaultMinSize(minHeight = 48.dp)
                .then(
                    Modifier.liquidClickable(
                        enabled = enabled,
                        role = Role.Button,
                        config = motion,
                        onClick = onClick,
                    ),
                ),
            style = style,
            colors = colors,
            shape = shape,
            alphaMultiplier = alpha,
            contentAlignment = Alignment.Center,
            contentDescription = contentDescription,
        ) {
            Row(
                modifier = Modifier.padding(contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}
