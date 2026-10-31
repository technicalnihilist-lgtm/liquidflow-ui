package dev.liquidflow.core

import android.util.Log
import java.util.ArrayDeque

public enum class LiquidFlowLogLevel(public val priority: Int) {
    VERBOSE(2),
    DEBUG(3),
    INFO(4),
    WARN(5),
    ERROR(6),
    NONE(Int.MAX_VALUE),
}

/** One retained diagnostic event. Throwables are flattened to a message to avoid leaking them. */
public data class DiagnosticEvent(
    val timestampMs: Long,
    val level: LiquidFlowLogLevel,
    val tag: String,
    val message: String,
    val error: String? = null,
)

/**
 * Small logging facade with a bounded in-memory ring buffer.
 *
 * The buffer exists so that `LiquidFlow.diagnostics()` can explain what the framework decided
 * on a device you do not own, without requiring a debugger. It is bounded so that enabling
 * `INFO` logging in production cannot grow memory without limit.
 */
public object LiquidFlowLogger {

    private val lock = Any()
    private var level: LiquidFlowLogLevel = LiquidFlowLogLevel.WARN
    private var prefix: String = "LiquidFlow"
    private var capacity: Int = 200
    private var events: ArrayDeque<DiagnosticEvent> = ArrayDeque(200)
    private var sequence: Long = 0L

    public fun configure(level: LiquidFlowLogLevel, tagPrefix: String, retainedEventCount: Int) {
        synchronized(lock) {
            this.level = level
            this.prefix = tagPrefix
            this.capacity = retainedEventCount.coerceIn(0, 2000)
            if (events.size > capacity) {
                while (events.size > capacity) events.removeFirst()
            }
        }
    }

    public fun v(tag: String, message: () -> String) = log(LiquidFlowLogLevel.VERBOSE, tag, message, null)
    public fun d(tag: String, message: () -> String) = log(LiquidFlowLogLevel.DEBUG, tag, message, null)
    public fun i(tag: String, message: () -> String) = log(LiquidFlowLogLevel.INFO, tag, message, null)
    public fun w(tag: String, error: Throwable? = null, message: () -> String) =
        log(LiquidFlowLogLevel.WARN, tag, message, error)

    public fun e(tag: String, error: Throwable? = null, message: () -> String) =
        log(LiquidFlowLogLevel.ERROR, tag, message, error)

    /** Snapshot of retained events, oldest first. */
    public fun events(): List<DiagnosticEvent> = synchronized(lock) { events.toList() }

    public fun clear() {
        synchronized(lock) { events.clear() }
    }

    /** Total number of events ever recorded, including those evicted from the ring buffer. */
    public val recordedEventCount: Long
        get() = synchronized(lock) { sequence }

    private fun log(
        eventLevel: LiquidFlowLogLevel,
        tag: String,
        message: () -> String,
        error: Throwable?,
    ) {
        val configuredLevel: LiquidFlowLogLevel
        val fullTag: String
        synchronized(lock) {
            configuredLevel = level
            fullTag = prefix + ":" + tag
        }
        if (eventLevel.priority < configuredLevel.priority) return

        val text = runCatching { message() }.getOrElse { "message() threw: $it" }
        val errorText = error?.let { "${it.javaClass.simpleName}: ${it.message}" }

        when (eventLevel) {
            LiquidFlowLogLevel.VERBOSE -> Log.v(fullTag, text)
            LiquidFlowLogLevel.DEBUG -> Log.d(fullTag, text)
            LiquidFlowLogLevel.INFO -> Log.i(fullTag, text)
            LiquidFlowLogLevel.WARN -> Log.w(fullTag, text)
            LiquidFlowLogLevel.ERROR -> Log.e(fullTag, text)
            LiquidFlowLogLevel.NONE -> Unit
        }

        synchronized(lock) {
            sequence++
            if (capacity == 0) return
            if (events.size >= capacity) events.removeFirst()
            events.addLast(
                DiagnosticEvent(
                    timestampMs = System.currentTimeMillis(),
                    level = eventLevel,
                    tag = tag,
                    message = text,
                    error = errorText,
                ),
            )
        }
    }
}
