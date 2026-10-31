package dev.liquidflow.widgets

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.liquidflow.glass.GlassColors
import dev.liquidflow.glass.GlassStyle
import dev.liquidflow.glass.GlassSurface
import dev.liquidflow.glass.LocalGlassColors
import dev.liquidflow.motion.currentMotionConfig
import dev.liquidflow.motion.liquidClickable

/** A circular glass avatar holding initials, an icon, or custom content. */
@Composable
public fun GlassAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    style: GlassStyle = GlassStyle.compact(),
    colors: GlassColors = LocalGlassColors.current,
    contentDescription: String? = null,
    content: (@Composable () -> Unit)? = null,
) {
    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }
    GlassSurface(
        modifier = modifier
            .size(size)
            .then(semanticsModifier),
        style = style,
        colors = colors,
        shape = CircleShape,
        contentAlignment = Alignment.Center,
    ) {
        if (content != null) {
            content()
        } else {
            Text(
                text = initials.take(2).uppercase(),
                color = colors.content,
                style = if (size >= 40.dp) {
                    MaterialTheme.typography.labelLarge
                } else {
                    MaterialTheme.typography.labelMedium
                },
            )
        }
    }
}

/** A small status badge. */
@Composable
public fun GlassBadge(
    text: String,
    modifier: Modifier = Modifier,
    style: GlassStyle = GlassStyle.compact().copy(cornerRadius = 10.dp, transparency = 0.45f),
    colors: GlassColors = LocalGlassColors.current,
    tint: Color? = null,
) {
    GlassSurface(
        modifier = modifier,
        style = if (tint != null) style.copy(tint = tint) else style,
        colors = colors,
        shape = RoundedCornerShape(style.cornerRadius),
    ) {
        Text(
            text = text,
            color = colors.content,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/**
 * A glass progress bar.
 *
 * Indeterminate mode only animates while this composable is in the tree, and it renders a static
 * partial bar under reduced motion instead of sweeping forever.
 */
@Composable
public fun GlassProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    style: GlassStyle = GlassStyle.compact(),
    colors: GlassColors = LocalGlassColors.current,
    height: Dp = 8.dp,
    indeterminate: Boolean = false,
    contentDescription: String? = null,
) {
    val motion = currentMotionConfig()
    val sweep = if (indeterminate && !motion.shouldReduceMotion) {
        val transition = rememberInfiniteTransition(label = "glassProgress")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200),
                repeatMode = RepeatMode.Restart,
            ),
            label = "glassProgressSweep",
        )
        value
    } else if (indeterminate) {
        0.35f
    } else {
        progress.coerceIn(0f, 1f)
    }

    val semanticsModifier = Modifier.semantics {
        if (contentDescription != null) this.contentDescription = contentDescription
        if (!indeterminate) {
            progressBarRangeInfo = ProgressBarRangeInfo(
                current = progress.coerceIn(0f, 1f),
                range = 0f..1f,
                steps = 0,
            )
        }
    }

    androidx.compose.foundation.Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .then(semanticsModifier),
    ) {
        val radius = CornerRadius(size.height / 2f, size.height / 2f)
        drawRoundRect(color = colors.tint.copy(alpha = 0.35f), cornerRadius = radius)
        val widthFraction = if (indeterminate) 0.3f else sweep
        val startX = if (indeterminate) (size.width - size.width * widthFraction) * sweep else 0f
        drawRoundRect(
            color = colors.accent.copy(alpha = 0.9f),
            topLeft = Offset(startX, 0f),
            size = Size(size.width * widthFraction, size.height),
            cornerRadius = radius,
        )
    }
}

/** A list row with leading and trailing slots. */
@Composable
public fun GlassListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    style: GlassStyle = GlassStyle.compact().copy(cornerRadius = 18.dp, transparency = 0.18f),
    colors: GlassColors = LocalGlassColors.current,
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
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .then(clickModifier),
        style = style,
        colors = colors,
        shape = RoundedCornerShape(style.cornerRadius),
        alphaMultiplier = if (enabled) 1f else 0.5f,
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (leading != null) {
                Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) { leading() }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = colors.content,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = colors.contentSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(4.dp))
                trailing()
            }
        }
    }
}

/** A settings row: label, optional description and a trailing control. */
@Composable
public fun GlassSettingRow(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    style: GlassStyle = GlassStyle.compact().copy(cornerRadius = 18.dp, transparency = 0.16f),
    colors: GlassColors = LocalGlassColors.current,
) {
    GlassListItem(
        title = title,
        subtitle = description,
        onClick = onClick,
        trailing = trailing,
        style = style,
        colors = colors,
        modifier = modifier,
    )
}

/** A thin divider tinted for glass surfaces. */
@Composable
public fun GlassDivider(
    modifier: Modifier = Modifier,
    colors: GlassColors = LocalGlassColors.current,
    thickness: Dp = 1.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .clip(CircleShape)
            .background(colors.border.copy(alpha = colors.border.alpha * 0.6f)),
    )
}
