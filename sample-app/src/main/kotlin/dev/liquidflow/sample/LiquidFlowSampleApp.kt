package dev.liquidflow.sample

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.liquidflow.adaptive.AdaptiveProfiles
import dev.liquidflow.glass.GlassNavigationBar
import dev.liquidflow.glass.GlassNavigationItem
import dev.liquidflow.glass.LiquidFlowTheme
import dev.liquidflow.glass.LocalGlassColors
import dev.liquidflow.motion.rememberLiquidNavTransitions
import dev.liquidflow.renderer.GlassBackdrop

/** Destinations of the sample application. */
enum class SampleDestination(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Home", Icons.Filled.Home),
    Gallery("gallery", "Gallery", Icons.Filled.Star),
    Motion("motion", "Motion", Icons.Filled.PlayArrow),
    Performance("performance", "Perf", Icons.Filled.Refresh),
    Settings("settings", "Settings", Icons.Filled.Settings),
}

/**
 * Root of the sample application.
 *
 * The artwork sits *inside* [GlassBackdrop] so the framework can blur it with one GPU effect on
 * Android 12+, while everything above it is glass. The bottom bar is a single glass surface, so
 * adding destinations does not add composited layers.
 */
@Composable
fun LiquidFlowSampleApp() {
    var darkTheme by rememberSaveable { mutableStateOf(true) }
    val navController = rememberNavController()
    val transitions = rememberLiquidNavTransitions()

    AdaptiveProfiles()

    LiquidFlowTheme(darkTheme = darkTheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            GlassBackdrop(modifier = Modifier.fillMaxSize()) {
                BackgroundArtwork(modifier = Modifier.fillMaxSize(), dark = darkTheme)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = SampleDestination.Home.route,
                        enterTransition = { transitions.enter },
                        exitTransition = { transitions.exit },
                        popEnterTransition = { transitions.popEnter },
                        popExitTransition = { transitions.popExit },
                    ) {
                        composable(SampleDestination.Home.route) { HomeScreen(navController) }
                        composable(SampleDestination.Gallery.route) { GalleryScreen() }
                        composable(SampleDestination.Motion.route) { MotionPlaygroundScreen() }
                        composable(SampleDestination.Performance.route) { PerformanceScreen() }
                        composable(SampleDestination.Settings.route) {
                            SettingsScreen(darkTheme = darkTheme, onToggleTheme = { darkTheme = !darkTheme })
                        }
                    }
                }
                SampleBottomBar(navController)
            }
        }
    }
}

@Composable
private fun SampleBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    GlassNavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        SampleDestination.entries.forEach { destination ->
            GlassNavigationItem(
                selected = currentRoute == destination.route,
                onClick = {
                    if (currentRoute != destination.route) {
                        navController.navigate(destination.route) {
                            popUpTo(SampleDestination.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                label = destination.label,
                icon = {
                    Icon(imageVector = destination.icon, contentDescription = destination.label)
                },
            )
        }
    }
}

/**
 * Decorative background: one gradient plus four radial washes, drawn in a single Canvas pass.
 * No bitmaps, no per-frame work, and it looks good blurred, which is what the glass samples.
 */
@Composable
private fun BackgroundArtwork(modifier: Modifier = Modifier, dark: Boolean) {
    val top = if (dark) Color(0xFF0B1020) else Color(0xFFEFF3FB)
    val bottom = if (dark) Color(0xFF131A2B) else Color(0xFFF8FAFF)
    val blobs = remember(dark) {
        listOf(
            Triple(Color(0xFF3B6FF5), Offset(0.18f, 0.16f), 0.42f),
            Triple(Color(0xFF9B5CF6), Offset(0.86f, 0.28f), 0.36f),
            Triple(Color(0xFF22C4A6), Offset(0.30f, 0.82f), 0.40f),
            Triple(Color(0xFFF26E7E), Offset(0.78f, 0.88f), 0.30f),
        )
    }

    Box(modifier = modifier.background(Brush.verticalGradient(listOf(top, bottom)))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val shortest = minOf(size.width, size.height)
            blobs.forEach { (color, position, radiusFraction) ->
                val centre = Offset(size.width * position.x, size.height * position.y)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = if (dark) 0.55f else 0.35f),
                            Color.Transparent,
                        ),
                        center = centre,
                        radius = shortest * radiusFraction,
                    ),
                    radius = shortest * radiusFraction,
                    center = centre,
                )
            }
        }
    }
}

/** Shared page scaffold: title plus scrollable content with consistent spacing. */
@Composable
fun SamplePage(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    val colors = LocalGlassColors.current
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
        ) {
            Text(
                text = title,
                color = colors.content,
                style = MaterialTheme.typography.headlineSmall,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = colors.contentSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { content() }
        }
    }
}

/** Small label used above demo sections. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    val colors = LocalGlassColors.current
    Box(modifier = modifier.fillMaxWidth()) {
        Text(
            text = text,
            color = colors.contentSecondary,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
