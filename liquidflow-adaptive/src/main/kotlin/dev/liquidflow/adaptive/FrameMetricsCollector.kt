package dev.liquidflow.adaptive

import android.view.Choreographer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Collects frame timings from [Choreographer].
 *
 * Why `Choreographer` rather than `Window.OnFrameMetricsAvailableListener`: the frame-metrics
 * listener needs a window and only reports frames the window drew, while Choreographer reports the
 * vsync cadence the app actually experiences and works from any thread with a Looper. The cost is
 * one callback per frame and an array write, which is negligible.
 *
 * The collector is strictly opt-in: it runs only while [start] has been called and the host is
 * visible, so an idle app does no work.
 */
public class FrameMetricsCollector(
    private val targetFrameMs: Float = 1000f / 60f,
    private val windowFrames: Int = 120,
) {
    private val _stats = MutableStateFlow(FrameWindow.EMPTY.copy(targetFrameMs = targetFrameMs))

    /** Stats for the most recently completed window. */
    public val stats: StateFlow<FrameWindow> = _stats.asStateFlow()

    /** Stats for the window currently being filled, for live dashboards. */
    private val _live = MutableStateFlow(FrameWindow.EMPTY.copy(targetFrameMs = targetFrameMs))
    public val liveStats: StateFlow<FrameWindow> = _live.asStateFlow()

    private val buffer = FloatArray(windowFrames)
    private var count = 0
    private var lastFrameNanos = 0L
    private var running = false
    private var choreographer: Choreographer? = null

    private val callback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            onFrame(frameTimeNanos)
            choreographer?.postFrameCallback(this)
        }
    }

    /** Starts collecting. Must be called on a thread with a Looper (the main thread in practice). */
    public fun start() {
        if (running) return
        running = true
        lastFrameNanos = 0L
        count = 0
        choreographer = Choreographer.getInstance()
        choreographer?.postFrameCallback(callback)
    }

    /** Stops collecting and releases the frame callback. */
    public fun stop() {
        running = false
        choreographer?.removeFrameCallback(callback)
        choreographer = null
    }

    public val isRunning: Boolean get() = running

    private fun onFrame(frameTimeNanos: Long) {
        val previous = lastFrameNanos
        lastFrameNanos = frameTimeNanos
        if (previous == 0L) return

        val deltaMs = (frameTimeNanos - previous) / 1_000_000f
        // Ignore absurd deltas (process suspended, debugger paused) instead of poisoning the window.
        if (deltaMs <= 0f || deltaMs > 1000f) return

        if (count < buffer.size) {
            buffer[count++] = deltaMs
        }
        if (count >= buffer.size) {
            publishWindow()
        } else {
            _live.value = FrameWindow.fromDurations(
                durationsMs = buffer.take(count),
                targetFrameMs = targetFrameMs,
            )
        }
    }

    private fun publishWindow() {
        val durations = buffer.take(count)
        val window = FrameWindow.fromDurations(durations, targetFrameMs)
        _stats.value = window
        count = 0
    }

    /** Clears measured data, e.g. after a profile change so old frames do not bias the next window. */
    public fun reset() {
        count = 0
        lastFrameNanos = 0L
        _stats.value = FrameWindow.EMPTY.copy(targetFrameMs = targetFrameMs)
        _live.value = FrameWindow.EMPTY.copy(targetFrameMs = targetFrameMs)
    }
}
