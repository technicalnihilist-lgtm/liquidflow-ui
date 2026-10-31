package dev.liquidflow.glass

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.liquidflow.motion.currentMotionConfig
import dev.liquidflow.motion.liquidClickable

/**
 * A floating glass navigation bar.
 *
 * It is a container only: items are supplied by the caller so the bar can hold any number of
 * destinations, with or without labels. The bar itself draws one glass surface, which keeps the
 * number of composited layers constant regardless of item count.
 */
@Composable
public fun GlassNavigationBar(
    modifier: Modifier = Modifier,
    style: GlassStyle = LocalGlassStyle.current.copy(cornerRadius = 28.dp, transparency = 0.3f),
    colors: GlassColors = LocalGlassColors.current,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    content: @Composable RowScope.() -> Unit,
) {
    GlassSurface(
        modifier = modifier,
        style = style,
        colors = colors,
        shape = RoundedCornerShape(style.cornerRadius),
    ) {
        Row(
            modifier = Modifier
                .padding(contentPadding)
                .defaultMinSize(minHeight = 56.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/**
 * One destination in a [GlassNavigationBar].
 *
 * The selection indicator is a glass pill that grows behind the icon with a spring, so switching
 * tabs reads as one continuous object moving rather than two independent fades.
 */
@Composable
public fun RowScope.GlassNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
    style: GlassStyle = GlassStyle.compact(),
    colors: GlassColors = LocalGlassColors.current,
    selectedColor: Color = colors.accent,
    unselectedColor: Color = colors.contentSecondary,
) {
    val motion = currentMotionConfig()

    val indicatorAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = motion.preset.animationSpec(motion),
        label = "navIndicatorAlpha",
    )
    val indicatorRadius by animateDpAsState(
        targetValue = if (selected) 18.dp else 0.dp,
        animationSpec = if (motion.shouldReduceMotion) {
            snap()
        } else {
            spring(
                dampingRatio = motion.preset.dampingRatio,
                stiffness = motion.preset.stiffnessForSpeed(motion.speedScale),
            )
        },
        label = "navIndicatorRadius",
    )

    val contentColor = if (selected) selectedColor else unselectedColor
    val shape: Shape = RoundedCornerShape(indicatorRadius)

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 56.dp, minHeight = 48.dp)
            .semantics(mergeDescendants = true) { this.selected = selected }
            .liquidClickable(
                enabled = enabled,
                role = Role.Tab,
                config = motion,
                onClick = onClick,
            )
            .clip(shape)
            .then(
                if (indicatorAlpha > 0.01f) {
                    Modifier.drawBehind {
                        drawRect(color = colors.accent.copy(alpha = 0.18f * indicatorAlpha))
                    }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CompositionLocalProvider(
                LocalContentColor provides contentColor.copy(alpha = if (enabled) 1f else 0.45f),
            ) {
                Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                    icon()
                }
                if (label != null) {
                    Text(
                        text = label,
                        color = contentColor.copy(alpha = if (enabled) 1f else 0.45f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

/** A toolbar with a glass background, for the top of a screen. */
@Composable
public fun GlassToolbar(
    modifier: Modifier = Modifier,
    title: String = "",
    style: GlassStyle = LocalGlassStyle.current.copy(cornerRadius = 24.dp, transparency = 0.3f),
    colors: GlassColors = LocalGlassColors.current,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    titleContent: (@Composable () -> Unit)? = null,
    height: Dp = 64.dp,
) {
    GlassSurface(
        modifier = modifier,
        style = style,
        colors = colors,
        shape = RoundedCornerShape(style.cornerRadius),
    ) {
        Row(
            modifier = Modifier
                .height(height)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (navigationIcon != null) {
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    navigationIcon()
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (titleContent != null) {
                    titleContent()
                } else {
                    Text(
                        text = title,
                        color = style.resolvedContentColor(colors),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}
