package dev.liquidflow.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

/** Full-screen crash report, deliberately plain so it cannot itself crash. */
@Composable
internal fun CrashScreen(text: String, onContinue: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("LiquidFlow crashed last time", style = MaterialTheme.typography.titleMedium)
            Text(
                "Same text is saved as liquidflow-crash.txt in your Downloads folder. " +
                    "Screenshot this, or send that file.",
                style = MaterialTheme.typography.bodySmall,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(text = text, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            }
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                Text("Continue in safe mode")
            }
        }
    }
}
