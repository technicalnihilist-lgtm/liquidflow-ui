package dev.liquidflow.sample

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal fun readCrash(context: Context): String? {
    val file = crashFile(context)
    if (!file.exists()) return null
    return runCatching { file.readText().takeIf { it.isNotBlank() } }.getOrNull()
}

internal fun clearCrash(context: Context) {
    runCatching { crashFile(context).delete() }
}

/** Full-screen crash report, deliberately plain so it cannot itself crash. */
@Composable
internal fun CrashScreen(text: String, onContinue: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "LiquidFlow crashed last time",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Screenshot this and send it. Tap Continue to open the app anyway.",
                style = MaterialTheme.typography.bodySmall,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(text = text, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onContinue() }) { Text("Continue") }
            }
        }
    }
}
