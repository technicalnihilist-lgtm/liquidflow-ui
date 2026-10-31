package dev.liquidflow.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Shows the last crash report before the app starts, when one exists.
 *
 * Wrapping the whole app in this keeps the crash path independent of the theme, the glass layers and
 * the framework itself: if any of those threw, this screen still renders.
 */
@Composable
fun CrashGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var crashText by remember { mutableStateOf(readCrash(context)) }
    val current = crashText
    if (current != null) {
        CrashScreen(
            text = current,
            onContinue = {
                clearCrash(context)
                crashText = null
            },
        )
    } else {
        content()
    }
}
