package dev.liquidflow.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.liquidflow.core.LiquidFlow
import dev.liquidflow.core.RenderingProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Wires frame measurement to profile adaptation.
 *
 * Responsibilities kept in one place:
 * - run the collector only while the host is visible and the developer allows adaptation;
 * - feed completed windows to [ProfileAdaptationPolicy];
 * - apply the decision through [LiquidFlow.applyAdaptiveProfile], which ignores the change when the
 *   developer has pinned a concrete profile.
 */
public class AdaptiveProfileController(
    public val policy: ProfileAdaptationPolicy = ProfileAdaptationPolicy(),
    public val collector: FrameMetricsCollector = FrameMetricsCollector(
        targetFrameMs = LiquidFlow.config.performance.frameBudgetMs,
        windowFrames = LiquidFlow.config.performance.adaptationWindowFrames,
    ),
) {
    private var scope: CoroutineScope? = null
    private var job: Job? = null
    private var started = false

    public fun start() {
        if (started) return
        started = true
        val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = controllerScope
        collector.start()
        job = controllerScope.launch {
            collector.stats.collectLatest { window ->
                if (window.frameCount == 0) return@collectLatest
                val decision = policy.evaluate(window, LiquidFlow.activeProfile)
                if (decision.changed && decision.profile.isConcrete) {
                    LiquidFlow.applyAdaptiveProfile(decision.profile)
                    // Old frames were measured under the previous profile; start clean.
                    collector.reset()
                }
            }
        }
        LiquidFlow.registerDiagnosticsProvider("adaptive") {
            val window = collector.stats.value
            buildMap {
                put("adaptationRunning", collector.isRunning.toString())
                put("windowFrames", window.frameCount.toString())
                put("avgFrameMs", window.averageFrameMs.toString())
                put("p95FrameMs", window.p95FrameMs.toString())
                put("worstFrameMs", window.worstFrameMs.toString())
                put("jankRatio", window.jankRatio.toString())
                put("estimatedFps", window.fps.toString())
                put("downgrades", policy.downgradeCount.toString())
                put("upgrades", policy.upgradeCount.toString())
                policy.currentReason?.let { put("lastDecision", it) }
                put("pinnedProfile", LiquidFlow.config.renderingProfile.toString())
                put("activeProfile", LiquidFlow.activeProfile.toString())
            }
        }
    }

    public fun stop() {
        if (!started) return
        started = false
        job?.cancel()
        job = null
        collector.stop()
        scope?.cancel()
        scope = null
        LiquidFlow.unregisterDiagnosticsProvider("adaptive")
    }

    public fun reset() {
        policy.reset()
        collector.reset()
    }
}

/**
 * Enables automatic profile adaptation for the current composition.
 *
 * It is lifecycle-aware: measurement starts when the host is started and stops on stop, so a
 * backgrounded app never pays for frame callbacks. Disabled automatically when the developer sets a
 * concrete profile or turns `enableAdaptiveProfiles` off.
 */
@Composable
public fun AdaptiveProfiles(
    controller: AdaptiveProfileController = rememberAdaptiveProfileController(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, controller) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    if (shouldAdapt()) controller.start() else controller.stop()
                }

                Lifecycle.Event.ON_STOP -> controller.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) && shouldAdapt()) {
            controller.start()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.stop()
        }
    }
}

private fun shouldAdapt(): Boolean =
    LiquidFlow.config.performance.enableAdaptiveProfiles &&
        LiquidFlow.config.renderingProfile == RenderingProfile.AUTO

/** Remembers an [AdaptiveProfileController] for the lifetime of the composition. */
@Composable
public fun rememberAdaptiveProfileController(): AdaptiveProfileController {
    val performance = LiquidFlow.config.performance
    return remember(performance.adaptationWindowFrames, performance.frameBudgetMs) {
        AdaptiveProfileController(
            collector = FrameMetricsCollector(
                targetFrameMs = performance.frameBudgetMs,
                windowFrames = performance.adaptationWindowFrames,
            ),
        )
    }
}
