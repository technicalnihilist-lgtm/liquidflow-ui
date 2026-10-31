package dev.liquidflow.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.liquidflow.core.LiquidFlow
import dev.liquidflow.core.LiquidFlowConfig
import dev.liquidflow.core.RenderingProfile

/**
 * Single activity host for the sample application.
 *
 * If the previous launch crashed, the framework starts in safe mode (LITE profile, glass and
 * animation disabled) so the app can still be used while the crash report is shown.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val crashedLastTime = readCrash(this) != null
        val config = if (crashedLastTime) {
            LiquidFlowConfig {
                renderingProfile = RenderingProfile.LITE
                glassEnabled = false
                animationsEnabled = false
            }
        } else {
            LiquidFlowConfig {
                renderingProfile = RenderingProfile.AUTO
                glassEnabled = true
                animationsEnabled = true
                reducedMotion = false
            }
        }

        try {
            LiquidFlow.initialize(context = applicationContext, config = config)
            LiquidFlow.attach(this)
        } catch (t: Throwable) {
            saveCrash(this, "initialize failed:\n\n" + t.stackTraceToString())
        }

        setContent {
            CrashGate {
                LiquidFlowRoot()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) LiquidFlow.detach()
    }
}
