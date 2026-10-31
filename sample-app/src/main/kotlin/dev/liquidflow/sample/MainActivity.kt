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
 * Initialization happens before the first composition so that no component ever composes against
 * the fallback profile. [LiquidFlow.attach] ties animation pausing to this activity's lifecycle.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        LiquidFlow.initialize(
            context = applicationContext,
            config = LiquidFlowConfig {
                renderingProfile = RenderingProfile.AUTO
                glassEnabled = true
                animationsEnabled = true
                reducedMotion = false
            },
        )
        LiquidFlow.attach(this)

        setContent {
            CrashGate {
                LiquidFlowRoot()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Only detach when the activity is really going away, not on configuration change.
        if (isFinishing) LiquidFlow.detach()
    }
}
