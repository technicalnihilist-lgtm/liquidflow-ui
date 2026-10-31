package dev.liquidflow.sample

import android.app.Application

/**
 * Installs a process-wide crash handler.
 *
 * Without adb, a crash on a phone is otherwise invisible; this makes the trace readable in the
 * Downloads folder and on the next launch.
 */
class LiquidFlowApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { saveCrash(this, throwable.stackTraceToString()) }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
