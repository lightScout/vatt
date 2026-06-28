package org.lightscout.vatt.core.config

/**
 * Platform-specific configuration that can't live in common code.
 *
 * The base URL differs per platform because of how each emulator reaches the host machine where the
 * mock API runs:
 *  - Android emulator: `localhost` is the emulator itself; the host is reachable at `10.0.2.2`.
 *  - iOS simulator: shares the host network, so `127.0.0.1` works.
 *
 * Kept behind expect/actual so the rest of the app depends on a single [baseUrl] value and never has to
 * branch on platform.
 */
expect object PlatformConfig {
    val baseUrl: String
}
