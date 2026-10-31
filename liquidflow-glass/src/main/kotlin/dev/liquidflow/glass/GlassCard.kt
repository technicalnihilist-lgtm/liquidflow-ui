package dev.liquidflow.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.liquidflow.motion.currentMotionConfig
import dev.liquidflow.motion.liquidClickable

/**
 * A glass container for related content.
 *
 * When [onClick] is provided the card becomes a button: it gains press feedback, the `Button` role
 * and a 48dp minimum touch target. When it is not, the card is purely presentational and carries no
 * click semantics, so screen readers do not announce a dead control.
 */
@Composable
public fun GlassCard(
    modifier: Modifier = Modifier,
    style: GlassStyle = LocalGlassStyle.current.copy(cornerRadius = 24.dp),
    colors: GlassColors = LocalGlassColors.current,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    shape: Shape = RoundedCornerShape(style.cornerRadius),
    content: @Composable ColumnScope.() -> Unit,
) {
    val motion = currentMotionConfig()
    val clickModifier = if (onClick != null) {
        Modifier.liquidClickable(
            enabled = enabled,
            role = Role.Button,
            config = motion,
            onClick = onClick,
        )
    } else {
        Modifier
    }

    GlassSurface(
        modifier = modifier.then(clickModifier),
        style = style,
        colors = colors,
        shape = shape,
        contentAlignment = Alignment.TopStart,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

/**
 * A larger glass container with an optional header, for grouping cards.
 *
 * The header is deliberately a slot rather than a `title: String` so callers keep full control of
 * typography and trailing actions.
 */
@Composable
public fun GlassPanel(
    modifier: Modifier = Modifier,
    style: GlassStyle = GlassStyle.panel(),
    colors: GlassColors = LocalGlassColors.current,
    header: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    GlassSurface(
        modifier = modifier,
        style = style,
        colors = colors,
        shape = RoundedCornerShape(style.cornerRadius),
    ) {
        Column {
            if (header != null) {
                Box(
                    modifier = Modifier.padding(
                        start = contentPadding.calculateStartPadding(layoutDirection),
                        end = contentPadding.calculateEndPadding(layoutDirection),
                        top = contentPadding.calculateTopPadding(),
                        bottom = 0.dp,
                    ),
                ) {
                    header()
                }
            }
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content,
            )
        }
    }
}
