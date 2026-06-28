package org.lightscout.vatt.platform

import android.content.Context

/**
 * Holds the application [Context] for platform code that needs it (reminders). Set once from the Android
 * entry point before Koin starts. Kept deliberately tiny to avoid pulling koin-android into the build.
 */
object AndroidAppContext {
    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun get(): Context =
        appContext ?: error("AndroidAppContext not initialised — call AndroidAppContext.init() at startup")
}
