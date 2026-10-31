package dev.liquidflow.core

/** A point-in-time snapshot of framework state, suitable for a bug report or a debug screen. */
public data class DiagnosticsReport(
    val state: LiquidFlowState,
    val capabilities: Map<String, String>,
    /** Extra sections contributed by optional modules, keyed by module name. */
    val providers: Map<String, Map<String, String>>,
    val events: List<DiagnosticEvent>,
    val recordedEventCount: Long,
) {
    public fun toText(): String = buildString {
        appendLine("LiquidFlow diagnostics")
        appendLine("---------------------")
        appendLine("profile        : ${state.profile} (requested: ${state.config.renderingProfile})")
        appendLine("initialized    : ${state.initialized}")
        appendLine("reducedMotion  : ${state.reducedMotion}")
        appendLine("transparency   : ${state.config.transparency}")
        appendLine("blurIntensity  : ${state.config.blurIntensity}")
        appendLine("animationSpeed : ${state.config.animationSpeed}")
        appendLine()
        appendLine("Device")
        capabilities.forEach { (key, value) -> appendLine("  $key = $value") }
        providers.forEach { (module, values) ->
            appendLine()
            appendLine(module)
            values.forEach { (key, value) -> appendLine("  $key = $value") }
        }
        appendLine()
        appendLine("Limits")
        appendLine("  maxBlurRadiusDp = ${state.limits.maxBlurRadiusDp}")
        appendLine("  blurDownscale   = ${state.limits.blurDownscaleFactor}")
        appendLine("  nativeBlur      = ${state.limits.allowNativeBlur}")
        appendLine("  backdropBlur    = ${state.limits.allowBackdropBlur}")
        appendLine("  specular        = ${state.limits.allowSpecularHighlight}")
        appendLine("  innerShadow     = ${state.limits.allowInnerShadow}")
        appendLine("  motionScale     = ${state.limits.motionScale}")
        appendLine()
        appendLine("Recent events (${events.size} of $recordedEventCount)")
        events.forEach { event ->
            appendLine("  ${event.level} ${event.tag}: ${event.message}${event.error?.let { " [$it]" } ?: ""}")
        }
    }
}

/** Errors surfaced by the framework. None of these are fatal by design. */
public sealed class LiquidFlowError(public val detail: String, public val cause: Throwable? = null) {
    public class InitializationFailed(detail: String, cause: Throwable? = null) : LiquidFlowError(detail, cause)
    public class CapabilityDetectionFailed(detail: String, cause: Throwable? = null) : LiquidFlowError(detail, cause)
    public class RenderingUnavailable(detail: String, cause: Throwable? = null) : LiquidFlowError(detail, cause)
    public class ConfigurationInvalid(detail: String) : LiquidFlowError(detail)

    override fun toString(): String =
        "${this::class.simpleName}: $detail${cause?.let { " (caused by ${it.javaClass.simpleName})" } ?: ""}"
}
