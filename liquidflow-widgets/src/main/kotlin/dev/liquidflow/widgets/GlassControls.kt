package dev.liquidflow.widgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.liquidflow.glass.GlassColors
import dev.liquidflow.glass.GlassStyle
import dev.liquidflow.glass.GlassSurface
import dev.liquidflow.glass.LocalGlassColors
import dev.liquidflow.motion.currentMotionConfig
import dev.liquidflow.motion.liquidClickable

/**
 * A segmented control with a sliding glass indicator.
 *
 * Segments share the available width equally, so it works from a 320dp phone to a tablet without
 * extra configuration. The indicator is one moving object, which reads better than cross-fading.
 */
@Composable
public fun GlassSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: GlassStyle = GlassStyle.compact().copy(cornerRadius = 20.dp, transparency = 0.2f),
    colors: GlassColors = LocalGlassColors.current,
    contentDescription: String? = null,
) {
    if (options.isEmpty()) return
    val motion = currentMotionConfig()
    val safeIndex = selectedIndex.coerceIn(0, options.lastIndex)

    val animatedPosition by animateFloatAsState(
        targetValue = safeIndex.toFloat(),
        animationSpec = motion.preset.animationSpec(motion),
        label = "segmentedPosition",
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            ),
    ) {
        val segmentWidth = maxWidth / options.size
        val density = LocalDensity.current
        val offsetPx = animatedPosition * with(density) { segmentWidth.toPx() }

        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            style = style,
            colors = colors,
            shape = RoundedCornerShape(style.cornerRadius),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .offset(x = with(density) { offsetPx.toDp() })
                        .width(segmentWidth)
                        .height(48.dp)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(style.cornerRadius - 4.dp))
                        .background(colors.accent.copy(alpha = 0.28f)),
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, option ->
                        val isSelected = index == safeIndex
                        Box(
                            modifier = Modifier
                                .width(segmentWidth)
                                .height(48.dp)
                                .semantics(mergeDescendants = true) { this.selected = isSelected }
                                .liquidClickable(
                                    enabled = enabled,
                                    role = Role.Tab,
                                    config = motion,
                                    onClick = { onSelect(index) },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = option,
                                textAlign = TextAlign.Center,
                                color = if (isSelected) colors.content else colors.contentSecondary,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A search field with a clear affordance. */
@Composable
public fun GlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    onSearch: (String) -> Unit = {},
    enabled: Boolean = true,
    style: GlassStyle = GlassStyle.compact().copy(cornerRadius = 22.dp),
    colors: GlassColors = LocalGlassColors.current,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = placeholder,
            enabled = enabled,
            singleLine = true,
            style = style,
            colors = colors,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Search,
            keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
            leadingIcon = {
                Text(
                    text = "\u2315",
                    color = colors.contentSecondary,
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 32.dp, minHeight = 32.dp)
                            .liquidClickable(
                                config = currentMotionConfig(),
                                onClick = { onQueryChange("") },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "\u00D7", color = colors.contentSecondary)
                    }
                }
            } else {
                null
            },
            contentDescription = placeholder,
        )
    }
}
