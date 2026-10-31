package dev.liquidflow.sample

import android.app.Application
import java.io.File

/**
 * Records the last uncaught exception so the app can show it on the next launch.
 *
 * Debugging a crash on a device with no adb access is otherwise guesswork; this makes the stack
 * trace visible on screen, where it can be screenshotted or copied.
 */
class LiquidFlowApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { crashFile(this).writeText(throwable.stackTraceToString()) }
            previous?.uncaughtException(thread, throwable)
        }
    }
}

internal fun crashFile(app: android.content.Context): File {
    val dir = app.getExternalFilesDir(null) ?: app.filesDir
    return File(dir, "last-crash.txt")
}
