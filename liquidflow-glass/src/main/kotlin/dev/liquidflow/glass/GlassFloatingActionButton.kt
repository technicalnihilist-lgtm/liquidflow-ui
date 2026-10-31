package dev.liquidflow.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.liquidflow.motion.currentMotionConfig
import dev.liquidflow.motion.liquidClickable

/**
 * A circular glass action button.
 *
 * Sized 56dp by default with a 48dp minimum, so it is reachable and readable by accessibility
 * services. Pass [contentDescription] whenever the icon is not self-explanatory.
 */
@Composable
public fun GlassFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: GlassStyle = LocalGlassStyle.current.copy(cornerRadius = 28.dp, transparency = 0.34f),
    colors: GlassColors = LocalGlassColors.current,
    size: Dp = 56.dp,
    contentDescription: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val motion = currentMotionConfig()
    val contentColor = style.resolvedContentColor(colors)
    val alpha = if (enabled) 1f else 0.45f

    CompositionLocalProvider(LocalContentColor provides contentColor.copy(alpha = contentColor.alpha * alpha)) {
        GlassSurface(
            modifier = modifier
                .size(size)
                .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
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
            shape = CircleShape,
            alphaMultiplier = alpha,
            contentAlignment = Alignment.Center,
            contentDescription = contentDescription,
        ) {
            Box(contentAlignment = Alignment.Center, content = content)
        }
    }
}
