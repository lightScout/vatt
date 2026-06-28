package org.lightscout.vatt.core.config

/** `10.0.2.2` is the host-machine loopback as seen from the Android emulator. */
actual object PlatformConfig {
    actual val baseUrl: String = "http://10.0.2.2:8080"
}
