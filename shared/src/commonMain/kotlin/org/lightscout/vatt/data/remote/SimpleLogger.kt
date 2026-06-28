package org.lightscout.vatt.data.remote

import io.ktor.client.plugins.logging.Logger

/** Minimal multiplatform logger for Ktor — prints to stdout, visible in Logcat / Xcode console. */
object SimpleLogger : Logger {
    override fun log(message: String) {
        println("[Ktor] $message")
    }
}
