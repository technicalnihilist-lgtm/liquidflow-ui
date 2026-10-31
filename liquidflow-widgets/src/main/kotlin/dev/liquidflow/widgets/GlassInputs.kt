package dev.liquidflow.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.liquidflow.glass.GlassColors
import dev.liquidflow.glass.GlassStyle
import dev.liquidflow.glass.GlassSurface
import dev.liquidflow.glass.LocalGlassColors
import dev.liquidflow.motion.currentMotionConfig
import dev.liquidflow.motion.liquidClickable

/**
 * A glass text field built on [BasicTextField].
 *
 * Uses the platform text field rather than reimplementing input, so IME behaviour, autofill,
 * copy/paste and accessibility are all standard. The glass is the container, not the input.
 */
@Composable
public fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    label: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    style: GlassStyle = GlassStyle.compact(),
    colors: GlassColors = LocalGlassColors.current,
    contentDescription: String? = null,
) {
    val contentColor = style.resolvedContentColor(colors)
    val errorColor = Color(0xFFE5484D)
    val effectiveContent = if (isError) errorColor else contentColor

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                color = colors.contentSecondary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
        }
        GlassSurface(
            style = style,
            colors = colors,
            shape = RoundedCornerShape(style.cornerRadius),
            alphaMultiplier = if (enabled) 1f else 0.5f,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (leadingIcon != null) leadingIcon()

                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            color = colors.contentSecondary.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    CompositionLocalProvider(LocalContentColor provides effectiveContent) {
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            enabled = enabled,
                            singleLine = singleLine,
                            textStyle = LocalTextStyle.current.merge(
                                TextStyle(color = effectiveContent),
                            ),
                            cursorBrush = SolidColor(effectiveContent),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = keyboardType,
                                imeAction = imeAction,
                            ),
                            keyboardActions = keyboardActions,
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (contentDescription != null) {
                                        Modifier.semantics { this.contentDescription = contentDescription }
                                    } else {
                                        Modifier
                                    },
                                ),
                        )
                    }
                }

                if (trailingIcon != null) trailingIcon()
            }
        }
    }
}

/**
 * A glass slider.
 *
 * Drawn and dragged directly so the track can be a glass gradient rather than a flat colour. It
 * exposes `ProgressBarRangeInfo` and a `setProgress` action so TalkBack can operate it.
 */
@Composable
public fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    steps: Int = 0,
    label: String? = null,
    valueLabel: ((Float) -> String)? = null,
    style: GlassStyle = GlassStyle.compact(),
    colors: GlassColors = LocalGlassColors.current,
    trackHeight: Dp = 8.dp,
    thumbRadius: Dp = 11.dp,
) {
    val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start))
        .coerceIn(0f, 1f)

    Column(modifier = modifier) {
        if (label != null || valueLabel != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = label.orEmpty(),
                    color = colors.content,
                    style = MaterialTheme.typography.labelLarge,
                )
                if (valueLabel != null) {
                    Text(
                        text = valueLabel(value),
                        color = colors.contentSecondary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }

        val semanticsModifier = Modifier.semantics {
            progressBarRangeInfo = ProgressBarRangeInfo(
                current = value,
                range = valueRange,
                steps = steps,
            )
            if (enabled) {
                setProgress { target ->
                    onValueChange(target.coerceIn(valueRange.start, valueRange.endInclusive))
                    true
                }
            }
        }

        val dragModifier = Modifier.pointerInput(enabled, valueRange) {
            if (!enabled) return@pointerInput
            detectHorizontalDragGestures(
                onDragStart = { offset ->
                    onValueChange(valueAt(offset.x, size.width.toFloat(), valueRange))
                },
                onHorizontalDrag = { change, _ ->
                    change.consume()
                    onValueChange(valueAt(change.position.x, size.width.toFloat(), valueRange))
                },
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(thumbRadius * 2 + 8.dp)
                .then(semanticsModifier)
                .then(dragModifier),
        ) {
            val trackTop = (size.height - trackHeight.toPx()) / 2f
            val radius = CornerRadius(trackHeight.toPx() / 2f, trackHeight.toPx() / 2f)

            drawRoundRect(
                color = colors.tint.copy(alpha = 0.4f * style.transparency),
                topLeft = Offset(0f, trackTop),
                size = Size(size.width, trackHeight.toPx()),
                cornerRadius = radius,
            )
            drawRoundRect(
                color = colors.accent.copy(alpha = if (enabled) 0.9f else 0.4f),
                topLeft = Offset(0f, trackTop),
                size = Size(size.width * fraction, trackHeight.toPx()),
                cornerRadius = radius,
            )
            drawRoundRect(
                color = colors.border,
                topLeft = Offset(0f, trackTop),
                size = Size(size.width, trackHeight.toPx()),
                cornerRadius = radius,
                style = Stroke(width = 1.dp.toPx()),
            )

            val thumbX = (size.width * fraction).coerceIn(thumbRadius.toPx(), size.width - thumbRadius.toPx())
            val centre = Offset(thumbX, size.height / 2f)
            drawCircle(
                color = Color.Black.copy(alpha = 0.18f),
                radius = thumbRadius.toPx(),
                center = Offset(centre.x, centre.y + 1.dp.toPx()),
            )
            drawCircle(
                color = colors.content.copy(alpha = if (enabled) 1f else 0.6f),
                radius = thumbRadius.toPx(),
                center = centre,
            )
        }
    }
}

/** A selectable chip. */
@Composable
public fun GlassChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    style: GlassStyle = GlassStyle.compact().copy(cornerRadius = 20.dp),
    colors: GlassColors = LocalGlassColors.current,
) {
    val motion = currentMotionConfig()
    val contentColor = if (selected) colors.accent else colors.content

    GlassSurface(
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .then(
                Modifier.liquidClickable(
                    enabled = enabled,
                    role = Role.Checkbox,
                    config = motion,
                    onClick = onClick,
                ),
            ),
        style = style.copy(
            transparency = if (selected) 0.5f else style.transparency,
            tint = if (selected) colors.accent else style.tint,
        ),
        colors = colors,
        shape = RoundedCornerShape(style.cornerRadius),
        alphaMultiplier = if (enabled) 1f else 0.5f,
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(PaddingValues(horizontal = 16.dp, vertical = 8.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (leadingIcon != null) leadingIcon()
            Text(
                text = label,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/** Converts a touch x position on the track into a value in [range]. */
private fun valueAt(x: Float, width: Float, range: ClosedFloatingPointRange<Float>): Float {
    if (width <= 0f) return range.start
    val fraction = (x / width).coerceIn(0f, 1f)
    return range.start + fraction * (range.endInclusive - range.start)
}
