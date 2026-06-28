package org.lightscout.vatt

import androidx.compose.runtime.Composable
import org.koin.compose.KoinContext
import org.lightscout.vatt.presentation.nav.AppNavHost
import org.lightscout.vatt.presentation.theme.VattTheme

/**
 * Root composable for both platforms. DI is started by the platform entry points (Android: MainActivity,
 * iOS: MainViewController) before this is shown.
 */
@Composable
fun App() {
    KoinContext {
        VattTheme {
            AppNavHost()
        }
    }
}
