package dev.liquidflow.sample

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * Crash storage.
 *
 * Writes the trace to two places on purpose: the app's own files directory (so the app can show it
 * on the next launch) and, via MediaStore, into the public Downloads folder as
 * `liquidflow-crash.txt`, which any file manager or text editor can open even if the app itself
 * cannot render anything.
 */
internal fun crashFile(context: Context): File {
    val dir = context.getExternalFilesDir(null) ?: context.filesDir
    return File(dir, "last-crash.txt")
}

internal fun saveCrash(context: Context, text: String) {
    runCatching { crashFile(context).writeText(text) }
    runCatching {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "liquidflow-crash.txt")
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(text.toByteArray())
            }
        }
    }
}

internal fun readCrash(context: Context): String? {
    val file = crashFile(context)
    if (!file.exists()) return null
    return runCatching { file.readText().takeIf { it.isNotBlank() } }.getOrNull()
}

internal fun clearCrash(context: Context) {
    runCatching { crashFile(context).delete() }
}
