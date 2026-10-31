package dev.liquidflow.adaptive

import dev.liquidflow.core.RenderingProfile

/**
 * One measurement window of frame timings.
 *
 * Percentiles rather than averages: a single 200 ms stall barely moves an average but is exactly
 * what users notice, so the policy looks at the 95th percentile as well.
 */
public data class FrameWindow(
    val frameCount: Int,
    val jankyFrames: Int,
    val averageFrameMs: Float,
    val p95FrameMs: Float,
    val worstFrameMs: Float,
    val targetFrameMs: Float,
) {
    /** Fraction of frames in this window that missed the budget, `0f .. 1f`. */
    public val jankRatio: Float
        get() = if (frameCount <= 0) 0f else jankyFrames.toFloat() / frameCount

    public val fps: Float
        get() = if (averageFrameMs <= 0f) 0f else 1000f / averageFrameMs

    public companion object {
        public val EMPTY: FrameWindow = FrameWindow(
            frameCount = 0,
            jankyFrames = 0,
            averageFrameMs = 0f,
            p95FrameMs = 0f,
            worstFrameMs = 0f,
            targetFrameMs = 1000f / 60f,
        )

        /** Builds a window from raw per-frame durations. */
        public fun fromDurations(
            durationsMs: List<Float>,
            targetFrameMs: Float,
            jankThresholdMs: Float = targetFrameMs * 1.2f,
        ): FrameWindow {
            if (durationsMs.isEmpty()) return EMPTY.copy(targetFrameMs = targetFrameMs)
            val sorted = durationsMs.sorted()
            val p95Index = ((sorted.size - 1) * 0.95f).toInt().coerceIn(0, sorted.lastIndex)
            return FrameWindow(
                frameCount = sorted.size,
                jankyFrames = sorted.count { it > jankThresholdMs },
                averageFrameMs = sorted.sum() / sorted.size,
                p95FrameMs = sorted[p95Index],
                worstFrameMs = sorted.last(),
                targetFrameMs = targetFrameMs,
            )
        }
    }
}

/** The outcome of evaluating one window. */
public data class AdaptationDecision(
    val profile: RenderingProfile,
    val changed: Boolean,
    val reason: String? = null,
) {
    public companion object {
        public fun keep(current: RenderingProfile): AdaptationDecision =
            AdaptationDecision(profile = current, changed = false)
    }
}

/**
 * Decides whether measured frame times justify changing the rendering profile.
 *
 * This class is deliberately hard to move: it needs a sustained problem to downgrade, a longer
 * sustained improvement to upgrade, and a cooldown between any two switches. That is what stops a
 * single scroll stutter, or a device warming up, from making the UI flip between looks.
 *
 * All state is internal and the class is single-threaded by contract: feed it from one thread (the
 * controller feeds it from the main thread, where frame callbacks arrive).
 */
public class ProfileAdaptationPolicy(
    /** Windows with fewer frames than this are ignored as too noisy to act on. */
    public val minFramesPerWindow: Int = 60,
    /** Downgrade when at least this fraction of frames miss the budget. */
    public val downgradeJankRatio: Float = 0.08f,
    /** Upgrade only when at most this fraction of frames miss the budget. */
    public val upgradeJankRatio: Float = 0.02f,
    /** Also require the 95th percentile to be under this multiple of the budget to upgrade. */
    public val upgradeP95Factor: Float = 1.35f,
    /** Windows that must be smooth in a row before an upgrade. */
    public val upgradeWindowsRequired: Int = 3,
    /** Windows that must pass after any switch before another is allowed. */
    public val cooldownWindows: Int = 3,
    /** Hard caps per session, so a pathological app cannot oscillate forever. */
    public val maxDowngrades: Int = 3,
    public val maxUpgrades: Int = 1,
) {
    private var windowsSinceSwitch: Int = Int.MAX_VALUE / 2
    private var consecutiveSmoothWindows: Int = 0
    private var downgrades: Int = 0
    private var upgrades: Int = 0
    private var lastReason: String? = null

    public val downgradeCount: Int get() = downgrades
    public val upgradeCount: Int get() = upgrades
    public val currentReason: String? get() = lastReason

    public fun evaluate(window: FrameWindow, current: RenderingProfile): AdaptationDecision {
        if (window.frameCount < minFramesPerWindow) {
            return AdaptationDecision.keep(current)
        }

        val smooth = window.jankRatio <= upgradeJankRatio &&
            window.p95FrameMs <= window.targetFrameMs * upgradeP95Factor

        if (!smooth) {
            consecutiveSmoothWindows = 0
        } else {
            consecutiveSmoothWindows++
        }

        val inCooldown = windowsSinceSwitch < cooldownWindows
        windowsSinceSwitch++

        if (inCooldown) return AdaptationDecision.keep(current)

        val janky = window.jankRatio >= downgradeJankRatio
        if (janky && current != RenderingProfile.LITE && downgrades < maxDowngrades) {
            val next = RenderingProfile.cheaperThan(current)
            downgrades++
            windowsSinceSwitch = 0
            consecutiveSmoothWindows = 0
            lastReason = "downgraded ${current} -> $next: ${(window.jankRatio * 100).toInt()}% janky frames"
            return AdaptationDecision(profile = next, changed = true, reason = lastReason)
        }

        val canUpgrade = current != RenderingProfile.HIGH &&
            consecutiveSmoothWindows >= upgradeWindowsRequired &&
            upgrades < maxUpgrades
        if (canUpgrade) {
            val next = RenderingProfile.richerThan(current)
            upgrades++
            windowsSinceSwitch = 0
            consecutiveSmoothWindows = 0
            lastReason = "upgraded ${current} -> $next: ${upgradeWindowsRequired} smooth windows"
            return AdaptationDecision(profile = next, changed = true, reason = lastReason)
        }

        return AdaptationDecision.keep(current)
    }

    /** Clears all counters. Called when the developer pins a profile or the host restarts. */
    public fun reset() {
        windowsSinceSwitch = Int.MAX_VALUE / 2
        consecutiveSmoothWindows = 0
        downgrades = 0
        upgrades = 0
        lastReason = null
    }
}
