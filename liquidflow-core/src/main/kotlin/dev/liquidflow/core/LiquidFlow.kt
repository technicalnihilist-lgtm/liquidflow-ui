package dev.liquidflow.core

import android.animation.ValueAnimator
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The resolved runtime state of the framework. */
public data class LiquidFlowState(
    val config: LiquidFlowConfig,
    val capabilities: DeviceCapabilities,
    /** Concrete profile in effect (never [RenderingProfile.AUTO]). */
    val profile: RenderingProfile,
    val limits: RenderingLimits,
    /** True when motion should be reduced, taking the system setting into account. */
    val reducedMotion: Boolean,
    val initialized: Boolean,
) {
    public companion object {
        /** Pre-initialization state: the cheapest possible configuration. */
        public val INITIAL: LiquidFlowState = LiquidFlowState(
            config = LiquidFlowConfig.DEFAULT,
            capabilities = DeviceCapabilities.UNKNOWN,
            profile = RenderingProfile.LITE,
            limits = RenderingLimits.forProfile(RenderingProfile.LITE, DeviceCapabilities.UNKNOWN),
            reducedMotion = false,
            initialized = false,
        )
    }
}

/**
 * Framework entry point.
 *
 * ```
 * LiquidFlow.initialize(context, LiquidFlowConfig { renderingProfile = RenderingProfile.AUTO })
 * ```
 *
 * Calling any LiquidFlow component without initializing first is safe: the framework falls back
 * to [LiquidFlowState.INITIAL] (LITE profile, no blur) and logs a warning once.
 */
public object LiquidFlow {

    private const val TAG = "LiquidFlow"

    private val lock = Any()

    private val _state = MutableStateFlow(LiquidFlowState.INITIAL)
    public val state: StateFlow<LiquidFlowState> = _state.asStateFlow()

    private val _isVisible = MutableStateFlow(true)

    /** Whether the attached host lifecycle is at least `STARTED`. Drives animation pausing. */
    public val isVisible: StateFlow<Boolean> = _isVisible.asStateFlow()

    private var appContext: Context? = null
    private var adaptiveOverride: RenderingProfile? = null
    private var lifecycleObserver: LifecycleEventObserver? = null
    private var observedLifecycle: Lifecycle? = null
    private var warnedUninitialized = false

    private val diagnosticProviders = LinkedHashMap<String, () -> Map<String, String>>()
    private val resourceReleasers = LinkedHashMap<String, () -> Unit>()

    public val config: LiquidFlowConfig get() = _state.value.config
    public val capabilities: DeviceCapabilities get() = _state.value.capabilities
    public val activeProfile: RenderingProfile get() = _state.value.profile
    public val limits: RenderingLimits get() = _state.value.limits
    public val reducedMotion: Boolean get() = _state.value.reducedMotion
    public val isInitialized: Boolean get() = _state.value.initialized

    /**
     * Initializes the framework. Safe to call more than once: subsequent calls update the
     * configuration and re-resolve the profile, but capabilities are probed only once per process.
     */
    @JvmOverloads
    public fun initialize(context: Context, config: LiquidFlowConfig = LiquidFlowConfig.DEFAULT) {
        val normalized = config.normalized()
        synchronized(lock) {
            appContext = context.applicationContext ?: context
            LiquidFlowLogger.configure(
                level = normalized.logging.level,
                tagPrefix = normalized.logging.tagPrefix,
                retainedEventCount = normalized.logging.retainedEventCount,
            )

            val existing = _state.value
            val capabilities = if (existing.initialized) {
                existing.capabilities
            } else {
                runCatching { CapabilityDetector.detect(context) }
                    .onFailure { error ->
                        LiquidFlowLogger.e(TAG, error) { "Capability detection failed; assuming weakest device" }
                    }
                    .getOrDefault(DeviceCapabilities.UNKNOWN)
            }

            val resolution = RenderingProfileResolver.resolveRequested(normalized.renderingProfile, capabilities)
            resolution.note?.let { note ->
                LiquidFlowLogger.i(TAG) { note }
            }

            adaptiveOverride = null
            _state.value = buildState(
                config = normalized,
                capabilities = capabilities,
                profile = resolution.profile,
                initialized = true,
            )

            LiquidFlowLogger.i(TAG) {
                "Initialized: profile=${resolution.profile} nativeBlur=${capabilities.nativeBlurAvailable} " +
                    "ram=${capabilities.totalRamMb}MB gles=${capabilities.glesMajor}.${capabilities.glesMinor} " +
                    "refresh=${capabilities.refreshRateHz}Hz"
            }
        }
    }

    /** Applies a configuration change and re-resolves the rendering profile. */
    public fun update(transform: (LiquidFlowConfig) -> LiquidFlowConfig) {
        updateConfig(transform(_state.value.config))
    }

    /** Applies a configuration change and re-resolves the rendering profile. */
    public fun updateConfig(config: LiquidFlowConfig) {
        val normalized = config.normalized()
        synchronized(lock) {
            val current = _state.value
            if (current.config.logging != normalized.logging) {
                LiquidFlowLogger.configure(
                    level = normalized.logging.level,
                    tagPrefix = normalized.logging.tagPrefix,
                    retainedEventCount = normalized.logging.retainedEventCount,
                )
            }
            // A manual profile choice clears any adaptive override so the developer stays in control.
            if (normalized.renderingProfile != current.config.renderingProfile) {
                adaptiveOverride = null
            }
            val requested = adaptiveOverride ?: normalized.renderingProfile
            val resolution = RenderingProfileResolver.resolveRequested(requested, current.capabilities)
            _state.value = buildState(
                config = normalized,
                capabilities = current.capabilities,
                profile = resolution.profile,
                initialized = current.initialized,
            )
        }
    }

    /** Explicitly selects a rendering profile, clearing any adaptive override. */
    public fun setRenderingProfile(profile: RenderingProfile) {
        updateConfig(_state.value.config.copy(renderingProfile = profile))
    }

    /**
     * Used by `liquidflow-adaptive` to switch the concrete profile from measured frame times.
     * Ignored while the developer has pinned a concrete profile manually.
     */
    public fun applyAdaptiveProfile(profile: RenderingProfile) {
        if (!profile.isConcrete) return
        synchronized(lock) {
            val current = _state.value
            if (current.config.renderingProfile.isConcrete) return
            if (adaptiveOverride == profile) return
            adaptiveOverride = profile
            _state.value = buildState(
                config = current.config,
                capabilities = current.capabilities,
                profile = profile,
                initialized = current.initialized,
            )
            LiquidFlowLogger.i(TAG) { "Adaptive profile switched to $profile" }
        }
    }

    /** The profile the adaptive controller is currently overriding, if any. */
    public val adaptiveProfileOverride: RenderingProfile?
        get() = synchronized(lock) { adaptiveOverride }

    /**
     * Observes [owner] so that the framework knows when the UI is visible. Animations stop while
     * the host is stopped, which is what keeps an idle backgrounded app from burning frames.
     */
    public fun attach(owner: LifecycleOwner) {
        synchronized(lock) {
            detachLocked()
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> _isVisible.value = true
                    Lifecycle.Event.ON_STOP -> _isVisible.value = false
                    Lifecycle.Event.ON_DESTROY -> _isVisible.value = false
                    else -> Unit
                }
            }
            owner.lifecycle.addObserver(observer)
            lifecycleObserver = observer
            observedLifecycle = owner.lifecycle
            _isVisible.value = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
    }

    /** Stops observing the previously attached lifecycle, if any. */
    public fun detach() {
        synchronized(lock) { detachLocked() }
    }

    private fun detachLocked() {
        lifecycleObserver?.let { observer -> observedLifecycle?.removeObserver(observer) }
        lifecycleObserver = null
        observedLifecycle = null
    }

    /** Registers a source of diagnostic values (used by renderer and adaptive modules). */
    public fun registerDiagnosticsProvider(name: String, provider: () -> Map<String, String>) {
        synchronized(lock) { diagnosticProviders[name] = provider }
    }

    public fun unregisterDiagnosticsProvider(name: String) {
        synchronized(lock) { diagnosticProviders.remove(name) }
    }

    /** Registers a callback that releases resources on [shutdown] (bitmap caches, collectors). */
    public fun registerResourceReleaser(name: String, releaser: () -> Unit) {
        synchronized(lock) { resourceReleasers[name] = releaser }
    }

    /** Builds a point-in-time report suitable for bug reports and the sample performance screen. */
    public fun diagnostics(): DiagnosticsReport {
        val snapshot = _state.value
        val extra = synchronized(lock) {
            diagnosticProviders.mapValues { (_, provider) ->
                runCatching { provider() }.getOrElse { mapOf("error" to it.toString()) }
            }
        }
        return DiagnosticsReport(
            state = snapshot,
            capabilities = snapshot.capabilities.toDiagnosticMap(),
            providers = extra,
            events = LiquidFlowLogger.events(),
            recordedEventCount = LiquidFlowLogger.recordedEventCount,
        )
    }

    /**
     * Releases everything the framework holds. Intended for tests and for hosts that tear the
     * framework down deliberately; the sample app never calls this in production.
     */
    public fun shutdown() {
        synchronized(lock) {
            detachLocked()
            resourceReleasers.values.forEach { releaser -> runCatching { releaser() } }
            resourceReleasers.clear()
            diagnosticProviders.clear()
            adaptiveOverride = null
            appContext = null
            warnedUninitialized = false
            _isVisible.value = true
            _state.value = LiquidFlowState.INITIAL
        }
        LiquidFlowLogger.clear()
        LiquidFlowLogger.i(TAG) { "Shut down" }
    }

    /**
     * Logs a one-time warning when a component is used before [initialize].
     *
     * Public because components live in other modules (`liquidflow-glass`, `liquidflow-widgets`) and
     * must be able to call it from their composition entry points.
     */
    public fun warnIfUninitialized() {
        synchronized(lock) {
            if (_state.value.initialized || warnedUninitialized) return
            warnedUninitialized = true
        }
        LiquidFlowLogger.w(TAG) {
            "A LiquidFlow component was composed before LiquidFlow.initialize(); " +
                "falling back to the LITE profile with no blur"
        }
    }

    private fun buildState(
        config: LiquidFlowConfig,
        capabilities: DeviceCapabilities,
        profile: RenderingProfile,
        initialized: Boolean,
    ): LiquidFlowState = LiquidFlowState(
        config = config,
        capabilities = capabilities,
        profile = profile,
        limits = RenderingLimits.forProfile(profile, capabilities),
        reducedMotion = resolveReducedMotion(config),
        initialized = initialized,
    )

    /**
     * Motion is reduced when the developer asks for it, or when the platform reports that
     * animators are disabled (Developer options / accessibility "remove animations").
     */
    private fun resolveReducedMotion(config: LiquidFlowConfig): Boolean {
        if (config.reducedMotion) return true
        if (!config.followSystemReducedMotion) return false
        return runCatching { !ValueAnimator.areAnimatorsEnabled() }.getOrDefault(false)
    }
}
