package dev.liquidflow.glass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.liquidflow.motion.LiquidTransitions
import dev.liquidflow.motion.currentMotionConfig

/**
 * A glass dialog.
 *
 * Implemented on top of the platform [Dialog] so it gets its own window, correct focus handling,
 * back-button dismissal and TalkBack modal semantics for free. The glass panel inside animates with
 * the ambient preset.
 */
@Composable
public fun GlassDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    text: String? = null,
    confirmLabel: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissLabel: String? = null,
    style: GlassStyle = GlassStyle.panel(),
    colors: GlassColors = LocalGlassColors.current,
    properties: DialogProperties = DialogProperties(),
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    if (!visible) return

    val motion = currentMotionConfig()
    val transitions = LiquidTransitions.of(motion, initialScale = 0.9f)
    val contentColor = style.resolvedContentColor(colors)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = transitions.enter,
            exit = transitions.exit,
        ) {
            GlassSurface(
                modifier = modifier
                    .widthIn(min = 280.dp, max = 420.dp)
                    .fillMaxWidth(),
                style = style,
                colors = colors,
                shape = RoundedCornerShape(style.cornerRadius),
            ) {
                Column(
                    modifier = Modifier.padding(PaddingValues(24.dp)),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (title != null) {
                        Text(
                            text = title,
                            color = contentColor,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    if (text != null) {
                        Text(
                            text = text,
                            color = colors.contentSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    content?.invoke(this)

                    if (confirmLabel != null || dismissLabel != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (dismissLabel != null) {
                                GlassButton(
                                    onClick = onDismissRequest,
                                    style = GlassStyle.compact().copy(transparency = 0.18f),
                                    colors = colors,
                                ) {
                                    Text(dismissLabel)
                                }
                            }
                            if (confirmLabel != null && onConfirm != null) {
                                GlassButton(
                                    onClick = onConfirm,
                                    style = GlassStyle.compact().copy(transparency = 0.5f, tint = colors.accent),
                                    colors = colors,
                                ) {
                                    Text(confirmLabel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
