package dev.liquidflow.sample

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.liquidflow.glass.GlassButton
import dev.liquidflow.glass.GlassStyle
import dev.liquidflow.glass.GlassSurface
import dev.liquidflow.glass.LiquidFlowTheme
import dev.liquidflow.glass.LocalGlassColors
import dev.liquidflow.motion.LiquidAnimatedVisibility
import dev.liquidflow.motion.currentMotionConfig
import kotlinx.coroutines.delay

/** Stages of the launch experience. */
enum class StartupStage { START, LOGO, APP }

/**
 * Root of the sample app: a start screen, then a short Android-logo stage, then the framework demo.
 *
 * The three stages are plain saved state, so a configuration change (rotation) keeps whichever stage
 * the user is on instead of throwing them back to the start.
 */
@Composable
fun LiquidFlowRoot() {
    var stage by rememberSaveable { mutableStateOf(StartupStage.START) }

    LiquidFlowTheme(darkTheme = true) {
        when (stage) {
            StartupStage.START -> StartScreen(onStart = { stage = StartupStage.LOGO })
            StartupStage.LOGO -> AndroidLogoStage(onFinished = { stage = StartupStage.APP })
            StartupStage.APP -> LiquidFlowSampleApp()
        }
    }
}

@Composable
private fun StartupBackdrop(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(Color(0xFF0B1020), Color(0xFF141C31), Color(0xFF0B1020))),
        ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val shortest = minOf(size.width, size.height)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF3B6FF5).copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(size.width * 0.2f, size.height * 0.18f),
                    radius = shortest * 0.5f,
                ),
                radius = shortest * 0.5f,
                center = Offset(size.width * 0.2f, size.height * 0.18f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF22C4A6).copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(size.width * 0.82f, size.height * 0.82f),
                    radius = shortest * 0.45f,
                ),
                radius = shortest * 0.45f,
                center = Offset(size.width * 0.82f, size.height * 0.82f),
            )
        }
    }
}

/** Screen 1: a glass card with the product name and a Start button. */
@Composable
private fun StartScreen(onStart: () -> Unit) {
    val colors = LocalGlassColors.current
    Box(modifier = Modifier.fillMaxSize()) {
        StartupBackdrop(modifier = Modifier.fillMaxSize())
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            GlassSurface(
                modifier = Modifier.widthIn(max = 380.dp),
                style = GlassStyle.panel(),
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = "LiquidFlow UI",
                        color = colors.content,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Liquid glass components with real spring physics, " +
                            "adaptive rendering profiles and honest fallbacks.",
                        color = colors.contentSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    GlassButton(
                        onClick = onStart,
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .widthIn(min = 200.dp),
                    ) {
                        Text("Start")
                    }
                }
            }
        }
    }
}

/** Screen 2: the Android mark, then on to the demo. */
@Composable
private fun AndroidLogoStage(onFinished: () -> Unit) {
    val colors = LocalGlassColors.current
    val motion = currentMotionConfig()
    var visible by rememberSaveable { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.6f,
        animationSpec = motion.preset.animationSpec(motion),
        label = "logoScale",
    )

    LaunchedEffect(Unit) {
        visible = true
        // Long enough to read, short enough not to annoy. Reduced motion still waits this long,
        // because this is a deliberate stage rather than decoration.
        delay(1600)
        onFinished()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        StartupBackdrop(modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            AndroidMark(
                modifier = Modifier
                    .size(140.dp)
                    .scale(scale),
            )
            LiquidAnimatedVisibility(visible = visible, config = motion) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Android",
                        color = colors.content,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Starting LiquidFlow UI…",
                        color = colors.contentSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            GlassSurface(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .widthIn(min = 180.dp),
                style = GlassStyle.compact(),
            ) {
                Box(modifier = Modifier.height(6.dp))
            }
        }
    }
}

/**
 * A generic Android-style droid drawn with Canvas primitives.
 *
 * Deliberately our own drawing rather than a bundled asset: the platform robot is a licensed
 * trademark, and a demo app has no business shipping someone else's logo.
 */
@Composable
private fun AndroidMark(modifier: Modifier = Modifier) {
    val droidGreen = Color(0xFF3DDC84)
    val dark = Color(0xFF0B1020)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bodyLeft = w * 0.18f
        val bodyTop = h * 0.40f
        val bodyWidth = w * 0.64f
        val bodyHeight = h * 0.34f
        val corner = CornerRadius(w * 0.10f, w * 0.10f)

        // Antennae
        val antennaStroke = w * 0.022f
        drawLine(
            color = droidGreen,
            start = Offset(w * 0.38f, h * 0.30f),
            end = Offset(w * 0.30f, h * 0.14f),
            strokeWidth = antennaStroke,
        )
        drawLine(
            color = droidGreen,
            start = Offset(w * 0.62f, h * 0.30f),
            end = Offset(w * 0.70f, h * 0.14f),
            strokeWidth = antennaStroke,
        )

        // Head
        drawRoundRect(
            color = droidGreen,
            topLeft = Offset(w * 0.22f, h * 0.28f),
            size = Size(w * 0.56f, h * 0.20f),
            cornerRadius = CornerRadius(w * 0.28f, w * 0.28f),
        )

        // Eyes
        drawCircle(color = dark, radius = w * 0.032f, center = Offset(w * 0.40f, h * 0.37f))
        drawCircle(color = dark, radius = w * 0.032f, center = Offset(w * 0.60f, h * 0.37f))

        // Body
        drawRoundRect(
            color = droidGreen,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = corner,
        )

        // Arms
        drawRoundRect(
            color = droidGreen,
            topLeft = Offset(w * 0.10f, bodyTop + h * 0.02f),
            size = Size(w * 0.06f, bodyHeight * 0.78f),
            cornerRadius = CornerRadius(w * 0.03f, w * 0.03f),
        )
        drawRoundRect(
            color = droidGreen,
            topLeft = Offset(w * 0.84f, bodyTop + h * 0.02f),
            size = Size(w * 0.06f, bodyHeight * 0.78f),
            cornerRadius = CornerRadius(w * 0.03f, w * 0.03f),
        )

        // Legs
        drawRoundRect(
            color = droidGreen,
            topLeft = Offset(w * 0.28f, h * 0.78f),
            size = Size(w * 0.08f, h * 0.14f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f),
        )
        drawRoundRect(
            color = droidGreen,
            topLeft = Offset(w * 0.64f, h * 0.78f),
            size = Size(w * 0.08f, h * 0.14f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f),
        )

        // Soft glass ring around the mark
        drawCircle(
            color = Color.White.copy(alpha = 0.12f),
            radius = w * 0.48f,
            center = Offset(w / 2f, h / 2f),
            style = Stroke(width = w * 0.012f),
        )
    }
}
