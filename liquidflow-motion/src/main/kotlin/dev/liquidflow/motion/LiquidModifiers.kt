package dev.liquidflow.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role

/**
 * Press feedback that springs back with the ambient preset.
 *
 * The scale is driven by `animateFloatAsState`, which is state-driven and interruptible: releasing
 * mid-press reverses from the current value instead of restarting, and nothing runs while idle.
 */
@Composable
public fun Modifier.liquidPressScale(
    pressed: Boolean,
    config: MotionConfig = currentMotionConfig(),
    scaleWhenPressed: Float = config.pressScale,
): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleWhenPressed else 1f,
        animationSpec = config.preset.animationSpec(config),
        label = "liquidPressScale",
    )
    return this.scale(scale)
}

/** Animated press feedback derived from an interaction source you already own. */
@Composable
public fun Modifier.liquidPressScale(
    interactionSource: MutableInteractionSource,
    config: MotionConfig = currentMotionConfig(),
    scaleWhenPressed: Float = config.pressScale,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    return liquidPressScale(pressed = pressed, config = config, scaleWhenPressed = scaleWhenPressed)
}

/**
 * Clickable with LiquidFlow press feedback and no Material ripple, for use on glass surfaces where
 * a ripple would look wrong.
 *
 * Accessibility is preserved: the click semantics, role and `enabled` state are all forwarded to
 * `clickable`, so TalkBack and keyboard/switch access behave normally.
 */
@Composable
public fun Modifier.liquidClickable(
    enabled: Boolean = true,
    role: Role? = null,
    config: MotionConfig = currentMotionConfig(),
    scaleWhenPressed: Float = config.pressScale,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit,
): Modifier {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) scaleWhenPressed else 1f,
        animationSpec = config.preset.animationSpec(config),
        label = "liquidClickableScale",
    )
    return this
        .scale(scale)
        .then(
            Modifier.clickable(
                enabled = enabled,
                role = role,
                interactionSource = source,
                indication = null,
                onClick = onClick,
            ),
        )
}

/** A scale pulse, useful for confirmation feedback (e.g. a toggle flipping). */
@Composable
public fun Modifier.liquidPulse(
    pulseKey: Any?,
    config: MotionConfig = currentMotionConfig(),
    peakScale: Float = config.bounceScale,
): Modifier {
    val spring = rememberLiquidSpring(initialValue = 1f, config = config)
    LaunchedEffect(pulseKey) {
        if (config.shouldReduceMotion) {
            spring.snapTo(1f)
        } else {
            spring.animateToAwait(peakScale)
            spring.animateToAwait(1f)
        }
    }
    return this.scale(spring.value)
}
