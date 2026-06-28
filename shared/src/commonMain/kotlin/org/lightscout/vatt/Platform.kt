package org.lightscout.vatt

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform