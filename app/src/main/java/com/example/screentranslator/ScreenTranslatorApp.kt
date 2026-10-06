package com.example.screentranslator

import android.app.Application
import com.example.screentranslator.diagnostics.CrashLogger

/**
 * Application entry point. Holds a global reference to the application context so that
 * singletons (DataStore, caches) can be reached from the accessibility service and UI.
 */
class ScreenTranslatorApp : Application() {

    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
        instance = this
    }

    companion object {
        lateinit var instance: ScreenTranslatorApp
            private set
    }
}
