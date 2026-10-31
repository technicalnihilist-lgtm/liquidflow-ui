package dev.liquidflow.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import dev.liquidflow.adaptive.FrameMetricsCollector
import dev.liquidflow.core.LiquidFlow
import dev.liquidflow.core.LiquidFlowConfig
import dev.liquidflow.core.RenderingProfile
import dev.liquidflow.glass.GlassBottomSheet
import dev.liquidflow.glass.GlassButton
import dev.liquidflow.glass.GlassCard
import dev.liquidflow.glass.GlassDialog
import dev.liquidflow.glass.GlassPanel
import dev.liquidflow.glass.GlassSwitch
import dev.liquidflow.glass.GlassToolbar
import dev.liquidflow.glass.LocalGlassColors
import dev.liquidflow.motion.LiquidExpandable
import dev.liquidflow.motion.SpringPresets
import dev.liquidflow.motion.currentMotionConfig
import dev.liquidflow.motion.rememberLiquidSpring
import dev.liquidflow.widgets.GlassAvatar
import dev.liquidflow.widgets.GlassBadge
import dev.liquidflow.widgets.GlassChip
import dev.liquidflow.widgets.GlassDivider
import dev.liquidflow.widgets.GlassListItem
import dev.liquidflow.widgets.GlassProgressIndicator
import dev.liquidflow.widgets.GlassSearchBar
import dev.liquidflow.widgets.GlassSegmentedControl
import dev.liquidflow.widgets.GlassSettingRow
import dev.liquidflow.widgets.GlassSlider
import dev.liquidflow.widgets.GlassTextField

/** Landing screen: live framework status plus the primary interaction demos. */
@Composable
fun HomeScreen(navController: NavHostController) {
    val state by LiquidFlow.state.collectAsState()
    val colors = LocalGlassColors.current

    var showDialog by rememberSaveable { mutableStateOf(false) }
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var tapCount by remember { mutableStateOf(0) }

    SamplePage(
        title = "LiquidFlow UI",
        subtitle = "Profile ${state.profile} • API ${state.capabilities.sdkInt}",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassToolbar(
                title = "Live configuration",
                modifier = Modifier.fillMaxWidth(),
                actions = { GlassBadge(text = state.profile.name, tint = colors.accent) },
            )

            GlassPanel {
                InfoLine("Native blur", state.capabilities.nativeBlurAvailable.toString())
                InfoLine("Device", "${state.capabilities.manufacturer} ${state.capabilities.model}")
                InfoLine("RAM", "${state.capabilities.totalRamMb} MB")
                InfoLine("GLES", "${state.capabilities.glesMajor}.${state.capabilities.glesMinor}")
                InfoLine("Refresh", "${state.capabilities.refreshRateHz.toInt()} Hz")
                InfoLine("Reduced motion", state.reducedMotion.toString())
                InfoLine("Max blur", "${state.limits.maxBlurRadiusDp} dp")
            }

            GlassCard(onClick = { showDialog = true }) {
                Text("Glass dialog", color = colors.content, style = MaterialTheme.typography.titleSmall)
                Text(
                    "A real platform Dialog with a glass panel inside.",
                    color = colors.contentSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            GlassCard(onClick = { showSheet = true }) {
                Text("Glass bottom sheet", color = colors.content, style = MaterialTheme.typography.titleSmall)
                Text(
                    "Drag it down to dismiss.",
                    color = colors.contentSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Glass rendering", color = colors.content)
                        Text(
                            "Off replaces every glass layer with a plain scrim.",
                            color = colors.contentSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    GlassSwitch(
                        checked = state.config.glassEnabled,
                        onCheckedChange = { enabled ->
                            LiquidFlow.update { it.copy(glassEnabled = enabled) }
                        },
                        contentDescription = "Toggle glass rendering",
                    )
                }
            }

            GlassCard {
                Text("Rapid tap safety", color = colors.content)
                Text(
                    "Each tap interrupts the spring, which continues from its current velocity.",
                    color = colors.contentSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassButton(onClick = { tapCount++ }) { Text("Tap me") }
                    Text("$tapCount taps", color = colors.content, style = MaterialTheme.typography.bodyMedium)
                }
            }

            GlassSettingRow(
                title = "Component gallery",
                description = "Every reusable component",
                onClick = { navController.navigate(SampleDestination.Gallery.route) },
            )
            GlassSettingRow(
                title = "Motion playground",
                description = "Presets, interruption, reduced motion",
                onClick = { navController.navigate(SampleDestination.Motion.route) },
            )
            GlassSettingRow(
                title = "Performance dashboard",
                description = "Live frame times on this device",
                onClick = { navController.navigate(SampleDestination.Performance.route) },
            )
        }
    }

    GlassDialog(
        visible = showDialog,
        onDismissRequest = { showDialog = false },
        title = "Glass dialog",
        text = "Dismisses on back press and outside taps. The panel uses the same glass layers.",
        confirmLabel = "Got it",
        onConfirm = { showDialog = false },
        dismissLabel = "Close",
    )

    GlassBottomSheet(
        visible = showSheet,
        onDismissRequest = { showSheet = false },
        title = "Glass bottom sheet",
    ) {
        Text(
            text = "Drag the sheet down past the threshold to dismiss. The drag feeds a spring, so " +
                "releasing mid-drag continues smoothly.",
            color = LocalGlassColors.current.contentSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
        GlassProgressIndicator(progress = 0.65f)
        GlassButton(onClick = { showSheet = false }, modifier = Modifier.fillMaxWidth()) {
            Text("Close sheet")
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    val colors = LocalGlassColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = colors.contentSecondary, style = MaterialTheme.typography.bodySmall)
        Text(value, color = colors.content, style = MaterialTheme.typography.bodySmall)
    }
}

/** Gallery of every reusable component. */
@Composable
fun GalleryScreen() {
    val colors = LocalGlassColors.current
    var chipSelected by remember { mutableStateOf(1) }
    var switchA by remember { mutableStateOf(true) }
    var textValue by remember { mutableStateOf("") }
    var sliderValue by remember { mutableStateOf(0.35f) }
    var segment by remember { mutableStateOf(0) }
    var progress by remember { mutableStateOf(0.25f) }

    SamplePage(title = "Component gallery", subtitle = "Every component is a real, reusable API") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel("BUTTONS AND CHIPS")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton(onClick = {}) { Text("Primary") }
                GlassButton(onClick = {}, enabled = false) { Text("Disabled") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("Design", "Motion", "Render").forEachIndexed { index, label ->
                    GlassChip(
                        label = label,
                        selected = chipSelected == index,
                        onClick = { chipSelected = index },
                    )
                }
            }

            SectionLabel("TOGGLES")
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Notifications", color = colors.content)
                    GlassSwitch(checked = switchA, onCheckedChange = { switchA = it })
                }
            }

            SectionLabel("INPUT")
            GlassTextField(
                value = textValue,
                onValueChange = { textValue = it },
                label = "Glass text field",
                placeholder = "Type something",
            )
            GlassSlider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                label = "Glass slider",
                valueLabel = { "${(it * 100).toInt()}%" },
            )
            GlassSegmentedControl(
                options = listOf("Day", "Week", "Month"),
                selectedIndex = segment,
                onSelect = { segment = it },
            )

            SectionLabel("DISPLAY")
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassAvatar(initials = "LF", contentDescription = "LiquidFlow avatar")
                GlassAvatar(initials = "UI", size = 32.dp)
                GlassBadge(text = "New")
                GlassBadge(text = "Beta", tint = colors.accent)
            }
            GlassProgressIndicator(progress = progress)
            GlassSlider(
                value = progress,
                onValueChange = { progress = it },
                label = "Drive the progress bar",
            )

            SectionLabel("LIST")
            GlassListItem(
                title = "Glass list item",
                subtitle = "With a leading avatar and a trailing badge",
                leading = { GlassAvatar(initials = "AB", size = 28.dp) },
                trailing = { GlassBadge(text = "3") },
                onClick = {},
            )
            GlassDivider()
            GlassSearchBar(
                query = textValue,
                onQueryChange = { textValue = it },
                placeholder = "Search components",
            )
        }
    }
}

/** Motion playground: presets, interruption, expand/collapse and reduced motion. */
@Composable
fun MotionPlaygroundScreen() {
    val state by LiquidFlow.state.collectAsState()
    val colors = LocalGlassColors.current
    val motion = currentMotionConfig()

    var expanded by remember { mutableStateOf(false) }
    var interruptions by remember { mutableStateOf(0) }
    val spring = rememberLiquidSpring(initialValue = 1f, config = motion)

    SamplePage(title = "Motion playground", subtitle = "Spring physics you can feel and interrupt") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel("SPRING PRESET")
            GlassCard {
                Text(
                    text = "Active: ${motion.preset.name}",
                    color = colors.content,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "damping ${motion.preset.dampingRatio} • stiffness ${motion.preset.stiffness}",
                    color = colors.contentSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SpringPresets.all.chunked(2).forEach { rowPresets ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowPresets.forEach { preset ->
                                GlassChip(
                                    label = preset.name,
                                    selected = state.config.springPreset == preset.name,
                                    onClick = {
                                        LiquidFlow.update { it.copy(springPreset = preset.name) }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            SectionLabel("SPEED")
            GlassSlider(
                value = state.config.animationSpeed,
                onValueChange = { speed -> LiquidFlow.update { it.copy(animationSpeed = speed) } },
                valueRange = 0.25f..3f,
                label = "Animation speed",
                valueLabel = { String.format("%.2fx", it) },
            )

            SectionLabel("INTERRUPTIBLE SPRING")
            GlassCard {
                Text(
                    text = "Each tap redirects the spring. Because it carries its current velocity, " +
                        "rapid taps never jump.",
                    color = colors.contentSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    GlassButton(onClick = {
                        interruptions++
                        spring.animateTo(if (spring.value > 1.5f) 1f else 2f)
                    }) {
                        Text("Interrupt")
                    }
                    Text(
                        text = "value ${"%.2f".format(spring.value)} • $interruptions taps",
                        color = colors.content,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .height(80.dp),
                ) {
                    val radius = (12.dp.toPx() * spring.value).coerceAtMost(size.height / 2f)
                    drawCircle(
                        color = colors.accent.copy(alpha = 0.85f),
                        radius = radius,
                        center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f),
                    )
                }
            }

            SectionLabel("EXPAND / COLLAPSE")
            GlassCard(onClick = { expanded = !expanded }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Tap to expand", color = colors.content)
                    Text(if (expanded) "−" else "+", color = colors.content)
                }
                LiquidExpandable(expanded = expanded) {
                    Text(
                        text = "Revealed with a spring-driven vertical expansion; collapsing reverses " +
                            "from wherever it is.",
                        color = colors.contentSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }

            SectionLabel("REDUCED MOTION")
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reduced motion", color = colors.content)
                        Text(
                            "Springs become instant snaps and transitions are disabled.",
                            color = colors.contentSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    GlassSwitch(
                        checked = state.config.reducedMotion,
                        onCheckedChange = { reduced ->
                            LiquidFlow.update { it.copy(reducedMotion = reduced) }
                        },
                        contentDescription = "Toggle reduced motion",
                    )
                }
                Text(
                    text = if (motion.shouldReduceMotion) {
                        "Reducing motion (also true when the system animation scale is 0)."
                    } else {
                        "Animating normally."
                    },
                    color = colors.contentSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/**
 * Performance dashboard. Frame times are measured on the device with `Choreographer`, and the
 * collector runs only while this screen is visible. Nothing here is hard-coded.
 */
@Composable
fun PerformanceScreen() {
    val state by LiquidFlow.state.collectAsState()
    val colors = LocalGlassColors.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val collector = remember(state.config.performance.adaptationWindowFrames) {
        FrameMetricsCollector(
            targetFrameMs = state.config.performance.frameBudgetMs,
            windowFrames = state.config.performance.adaptationWindowFrames,
        )
    }
    val stats by collector.stats.collectAsState()
    val live by collector.liveStats.collectAsState()

    DisposableEffect(lifecycleOwner, collector) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> collector.start()
                Lifecycle.Event.ON_STOP -> collector.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) collector.start()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            collector.stop()
        }
    }

    var diagnosticsText by remember { mutableStateOf<String?>(null) }

    SamplePage(title = "Performance", subtitle = "Measured on this device, not simulated") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel("FRAME TIMES (LIVE)")
            GlassCard {
                val window = if (live.frameCount > 0) live else stats
                Text(
                    text = "≈ ${"%.1f".format(window.fps)} FPS",
                    color = colors.content,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "avg ${"%.2f".format(window.averageFrameMs)} ms • " +
                        "p95 ${"%.2f".format(window.p95FrameMs)} ms • " +
                        "worst ${"%.2f".format(window.worstFrameMs)} ms",
                    color = colors.contentSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "budget ${"%.2f".format(window.targetFrameMs)} ms • " +
                        "janky ${(window.jankRatio * 100).toInt()}% • sampled ${window.frameCount}",
                    color = colors.contentSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                GlassProgressIndicator(
                    progress = (window.jankRatio * 4f).coerceIn(0f, 1f),
                    modifier = Modifier.padding(top = 12.dp),
                    contentDescription = "Jank ratio",
                )
            }

            SectionLabel("RENDERING PROFILE")
            GlassCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        RenderingProfile.AUTO,
                        RenderingProfile.HIGH,
                        RenderingProfile.BALANCED,
                        RenderingProfile.LITE,
                    ).forEach { profile ->
                        GlassChip(
                            label = profile.name,
                            selected = state.config.renderingProfile == profile,
                            onClick = { LiquidFlow.setRenderingProfile(profile) },
                        )
                    }
                }
                Text(
                    text = "Requested ${state.config.renderingProfile} • active ${state.profile}",
                    color = colors.contentSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Text(
                    text = "Max blur ${state.limits.maxBlurRadiusDp}dp • downscale " +
                        "${state.limits.blurDownscaleFactor}x • native ${state.limits.allowNativeBlur}",
                    color = colors.contentSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            SectionLabel("MEMORY")
            GlassCard {
                val runtime = Runtime.getRuntime()
                val usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                val maxMb = runtime.maxMemory() / (1024 * 1024)
                Text(
                    text = "Heap $usedMb MB of $maxMb MB",
                    color = colors.content,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "System reports ${state.capabilities.availableRamMb} MB available of " +
                        "${state.capabilities.totalRamMb} MB.",
                    color = colors.contentSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            SectionLabel("DIAGNOSTICS")
            GlassCard {
                GlassButton(onClick = { diagnosticsText = LiquidFlow.diagnostics().toText() }) {
                    Text("Capture diagnostics")
                }
                diagnosticsText?.let { text ->
                    Text(
                        text = text,
                        color = colors.contentSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }
}

/** Settings: every global knob, wired to real configuration. */
@Composable
fun SettingsScreen(darkTheme: Boolean, onToggleTheme: () -> Unit) {
    val state by LiquidFlow.state.collectAsState()
    val colors = LocalGlassColors.current
    val config = state.config

    SamplePage(title = "Settings", subtitle = "Changes apply immediately") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel("APPEARANCE")
            GlassCard {
                SettingToggle(
                    title = "Dark theme",
                    description = "Switches the glass palette",
                    checked = darkTheme,
                    onCheckedChange = { onToggleTheme() },
                )
                SettingToggle(
                    title = "Glass rendering",
                    description = "Off = solid scrim, lowest cost",
                    checked = config.glassEnabled,
                    onCheckedChange = { enabled -> LiquidFlow.update { it.copy(glassEnabled = enabled) } },
                )
            }

            SectionLabel("TRANSPARENCY AND BLUR")
            GlassCard {
                GlassSlider(
                    value = config.transparency,
                    onValueChange = { value -> LiquidFlow.update { it.copy(transparency = value) } },
                    label = "Global transparency",
                    valueLabel = { "${(it * 100).toInt()}%" },
                )
                GlassSlider(
                    value = config.blurIntensity,
                    onValueChange = { value -> LiquidFlow.update { it.copy(blurIntensity = value) } },
                    label = "Blur intensity",
                    valueLabel = { "${(it * 100).toInt()}%" },
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    text = if (state.limits.allowBackdropBlur) {
                        "Blur is available on this profile, capped at ${state.limits.maxBlurRadiusDp}dp."
                    } else {
                        "This profile disables backdrop blur; glass uses tint and gradient only."
                    },
                    color = colors.contentSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            SectionLabel("RENDERING PROFILE")
            GlassCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        RenderingProfile.AUTO,
                        RenderingProfile.HIGH,
                        RenderingProfile.BALANCED,
                        RenderingProfile.LITE,
                    ).forEach { profile ->
                        GlassChip(
                            label = profile.name,
                            selected = config.renderingProfile == profile,
                            onClick = { LiquidFlow.setRenderingProfile(profile) },
                        )
                    }
                }
            }

            SectionLabel("MOTION")
            GlassCard {
                GlassSlider(
                    value = config.animationSpeed,
                    onValueChange = { value -> LiquidFlow.update { it.copy(animationSpeed = value) } },
                    valueRange = 0.25f..3f,
                    label = "Animation speed",
                    valueLabel = { String.format("%.2fx", it) },
                )
                SettingToggle(
                    title = "Animations",
                    description = "Master switch for all motion",
                    checked = config.animationsEnabled,
                    onCheckedChange = { enabled -> LiquidFlow.update { it.copy(animationsEnabled = enabled) } },
                )
                SettingToggle(
                    title = "Reduced motion",
                    description = "Replaces springs with instant state changes",
                    checked = config.reducedMotion,
                    onCheckedChange = { reduced -> LiquidFlow.update { it.copy(reducedMotion = reduced) } },
                )
                SettingToggle(
                    title = "Follow system reduced motion",
                    description = "Honour the platform animation scale",
                    checked = config.followSystemReducedMotion,
                    onCheckedChange = { follow ->
                        LiquidFlow.update { it.copy(followSystemReducedMotion = follow) }
                    },
                )
            }

            SectionLabel("PERFORMANCE")
            GlassCard {
                SettingToggle(
                    title = "Pause animations when hidden",
                    description = "Stops motion in the background",
                    checked = config.performance.pauseAnimationsWhenNotVisible,
                    onCheckedChange = { pause ->
                        LiquidFlow.update {
                            it.copy(performance = it.performance.copy(pauseAnimationsWhenNotVisible = pause))
                        }
                    },
                )
                SettingToggle(
                    title = "Adaptive profiles",
                    description = "Adjust the profile from measured frame times",
                    checked = config.performance.enableAdaptiveProfiles,
                    onCheckedChange = { enabled ->
                        LiquidFlow.update {
                            it.copy(performance = it.performance.copy(enableAdaptiveProfiles = enabled))
                        }
                    },
                )
            }

            GlassCard {
                Text(
                    text = "Reset everything to framework defaults.",
                    color = colors.contentSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                GlassButton(
                    onClick = { LiquidFlow.updateConfig(LiquidFlowConfig.DEFAULT) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    Text("Reset configuration")
                }
            }
        }
    }
}

@Composable
private fun SettingToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = LocalGlassColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.content, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                color = colors.contentSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        GlassSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            contentDescription = title,
        )
    }
}
