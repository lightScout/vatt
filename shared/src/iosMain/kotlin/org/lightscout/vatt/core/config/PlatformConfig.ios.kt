package org.lightscout.vatt.core.config

/** The iOS simulator shares the host network, so the loopback address reaches the mock directly. */
actual object PlatformConfig {
    actual val baseUrl: String = "http://127.0.0.1:8080"
}
