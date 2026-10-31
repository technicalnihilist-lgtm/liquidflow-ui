package dev.liquidflow.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.liquidflow.motion.currentMotionConfig
import dev.liquidflow.motion.liquidDragToDismiss
import dev.liquidflow.motion.rememberLiquidDragState

/**
 * A glass bottom sheet with real drag-to-dismiss behaviour.
 *
 * Written directly on `Dialog` + pointer input rather than wrapping Material's experimental
 * `ModalBottomSheet`, so there is no experimental opt-in in the public API and the drag is wired to
 * LiquidFlow springs: dragging down past the threshold dismisses, anything less springs back.
 */
@Composable
public fun GlassBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    style: GlassStyle = GlassStyle.panel().copy(cornerRadius = 28.dp),
    colors: GlassColors = LocalGlassColors.current,
    contentPadding: PaddingValues = PaddingValues(24.dp),
    dismissThreshold: Dp = 120.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!visible) return

    val motion = currentMotionConfig()
    val dragState = rememberLiquidDragState(motion)

    val scrimAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = motion.preset.animationSpec(motion),
        label = "sheetScrim",
    )

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics { paneTitle = title ?: "Bottom sheet" },
        ) {
            // Scrim: also the tap-outside target.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f * scrimAlpha))
                    .liquidDragToDismiss(
                        state = dragState,
                        onDismiss = onDismissRequest,
                        config = motion,
                        dismissThreshold = dismissThreshold,
                    ),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = dragState.offset
                        alpha = (1f - dragState.progress * 0.25f).coerceIn(0.4f, 1f)
                    }
                    .liquidDragToDismiss(
                        state = dragState,
                        onDismiss = onDismissRequest,
                        config = motion,
                        dismissThreshold = dismissThreshold,
                    ),
            ) {
                GlassSurface(
                    modifier = modifier.fillMaxWidth(),
                    style = style,
                    colors = colors,
                    shape = RoundedCornerShape(topStart = style.cornerRadius, topEnd = style.cornerRadius),
                ) {
                    Column(modifier = Modifier.padding(contentPadding)) {
                        // Drag handle
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 12.dp)
                                .width(40.dp)
                                .height(4.dp)
                                .background(
                                    color = colors.contentSecondary.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(2.dp),
                                ),
                        )
                        if (title != null) {
                            Text(
                                text = title,
                                color = style.resolvedContentColor(colors),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 12.dp),
                            )
                        }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            content = content,
                        )
                    }
                }
            }
        }
    }
}
