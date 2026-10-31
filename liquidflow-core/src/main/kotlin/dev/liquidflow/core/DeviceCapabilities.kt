package dev.liquidflow.core

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.RenderEffect
import android.os.Build
import android.os.PowerManager
import android.view.Display
import android.view.WindowManager

/**
 * A snapshot of what the current device can actually do.
 *
 * LiquidFlow never infers capability from the Android version alone: blur, GPU class and
 * memory pressure are all probed independently, and every probe degrades gracefully.
 */
public data class DeviceCapabilities(
    val sdkInt: Int,
    val androidRelease: String,
    val manufacturer: String,
    val model: String,
    val isLowRamDevice: Boolean,
    val totalRamMb: Int,
    val availableRamMb: Int,
    /** Packed GLES version, e.g. `0x30002` for OpenGL ES 3.2. Zero when unknown. */
    val glesVersion: Int,
    /** Highest supported texture size in pixels, or 0 when it could not be determined. */
    val maxTextureSize: Int,
    val refreshRateHz: Float,
    val screenDensity: Float,
    /** True only when a `RenderEffect` blur was actually constructed on this device. */
    val nativeBlurAvailable: Boolean,
    /** Thermal status as reported by [PowerManager], or `null` when unavailable. */
    val thermalStatus: Int?,
    /** True when the platform reports hardware acceleration for the window pipeline. */
    val hardwareAccelerated: Boolean,
) {
    public val glesMajor: Int
        get() = (glesVersion shr 16) and 0xFFFF

    public val glesMinor: Int
        get() = glesVersion and 0xFFFF

    /** True when the device reports a thermal status that means the GPU/CPU is throttled. */
    public val isThermallyThrottled: Boolean
        get() = thermalStatus != null && thermalStatus >= PowerManager.THERMAL_STATUS_MODERATE

    /** Heuristic: enough headroom for the full effect stack. */
    public val isHighEnd: Boolean
        get() = !isLowRamDevice && totalRamMb >= 4096 && glesMajor >= 3 && refreshRateHz >= 60f

    /** Heuristic: can run cached blur comfortably but not the full stack. */
    public val isMidRange: Boolean
        get() = !isLowRamDevice && totalRamMb >= 2560 && glesMajor >= 3

    /** True when cached (CPU) blur is affordable for full-screen backdrops. */
    public val canAffordCachedBlur: Boolean
        get() = !isLowRamDevice && totalRamMb >= 2048 && glesMajor >= 2

    public fun toDiagnosticMap(): Map<String, String> = buildMap {
        put("sdkInt", sdkInt.toString())
        put("androidRelease", androidRelease)
        put("device", "$manufacturer $model")
        put("lowRamDevice", isLowRamDevice.toString())
        put("ramMb", "$availableRamMb free / $totalRamMb total")
        put("gles", "$glesMajor.$glesMinor")
        put("maxTextureSize", maxTextureSize.toString())
        put("refreshRateHz", refreshRateHz.toString())
        put("density", screenDensity.toString())
        put("nativeBlur", nativeBlurAvailable.toString())
        put("thermalStatus", thermalStatus?.toString() ?: "unknown")
        put("hardwareAccelerated", hardwareAccelerated.toString())
    }

    public companion object {
        /**
         * Conservative fallback used when probing fails, or in JVM unit tests.
         * It deliberately reports the weakest hardware so that nothing expensive is enabled.
         */
        public val UNKNOWN: DeviceCapabilities = DeviceCapabilities(
            sdkInt = Build.VERSION.SDK_INT,
            androidRelease = Build.VERSION.RELEASE ?: "unknown",
            manufacturer = Build.MANUFACTURER ?: "unknown",
            model = Build.MODEL ?: "unknown",
            isLowRamDevice = true,
            totalRamMb = 0,
            availableRamMb = 0,
            glesVersion = 0,
            maxTextureSize = 0,
            refreshRateHz = 60f,
            screenDensity = 1f,
            nativeBlurAvailable = false,
            thermalStatus = null,
            hardwareAccelerated = false,
        )
    }
}

/**
 * Probes [DeviceCapabilities] from a [Context]. Every probe is individually guarded so that a
 * single failing OEM API cannot break framework initialization.
 */
public object CapabilityDetector {

    public fun detect(context: Context): DeviceCapabilities {
        val appContext = context.applicationContext ?: context

        val memory = readMemory(appContext)
        val glesVersion = readGlesVersion(appContext)
        val display = readDisplay(appContext)

        return DeviceCapabilities(
            sdkInt = Build.VERSION.SDK_INT,
            androidRelease = Build.VERSION.RELEASE ?: "unknown",
            manufacturer = Build.MANUFACTURER ?: "unknown",
            model = Build.MODEL ?: "unknown",
            isLowRamDevice = memory.lowRam,
            totalRamMb = memory.totalMb,
            availableRamMb = memory.availableMb,
            glesVersion = glesVersion,
            maxTextureSize = readMaxTextureSize(),
            refreshRateHz = display.refreshRate,
            screenDensity = display.density,
            nativeBlurAvailable = probeNativeBlur(),
            thermalStatus = readThermalStatus(appContext),
            hardwareAccelerated = readHardwareAcceleration(appContext),
        )
    }

    private data class MemorySnapshot(val lowRam: Boolean, val totalMb: Int, val availableMb: Int)

    private fun readMemory(context: Context): MemorySnapshot = runCatching {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        MemorySnapshot(
            lowRam = am.isLowRamDevice,
            totalMb = (info.totalMem / (1024 * 1024)).toInt(),
            availableMb = (info.availMem / (1024 * 1024)).toInt(),
        )
    }.getOrElse { MemorySnapshot(lowRam = true, totalMb = 0, availableMb = 0) }

    private fun readGlesVersion(context: Context): Int = runCatching {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        am.deviceConfigurationInfo.reqGlEsVersion
    }.getOrDefault(0)

    private data class DisplaySnapshot(val refreshRate: Float, val density: Float)

    @Suppress("DEPRECATION")
    private fun readDisplay(context: Context): DisplaySnapshot = runCatching {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display ?: wm.defaultDisplay
        } else {
            wm.defaultDisplay
        }
        val rate = display?.refreshRate?.takeIf { it > 1f } ?: 60f
        val density = context.resources.displayMetrics.density.takeIf { it > 0f } ?: 1f
        DisplaySnapshot(refreshRate = rate, density = density)
    }.getOrElse { DisplaySnapshot(refreshRate = 60f, density = 1f) }

    /**
     * Texture size requires a live GL context. We avoid creating one (it costs tens of
     * milliseconds at startup and is not worth it); this returns 0 to mean "unknown" and the
     * renderer simply caps bitmap dimensions conservatively instead.
     */
    private fun readMaxTextureSize(): Int = 0

    /**
     * True only if a blur `RenderEffect` can genuinely be created. Class presence is not enough:
     * some devices ship the API but fail to create an effect, so we actually construct one.
     */
    @Suppress("NewApi")
    private fun probeNativeBlur(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        return runCatching {
            val effect = RenderEffect.createBlurEffect(8f, 8f, android.graphics.Shader.TileMode.CLAMP)
            effect != null
        }.getOrDefault(false)
    }

    private fun readThermalStatus(context: Context): Int? = runCatching {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.currentThermalStatus
    }.getOrNull()

    private fun readHardwareAcceleration(context: Context): Boolean = runCatching {
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getApplicationInfo(
                context.packageName,
                PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            pm.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        }
        (info.flags and android.content.pm.ApplicationInfo.FLAG_HARDWARE_ACCELERATED) != 0
    }.getOrDefault(true)
}
