package org.lightscout.vatt

import androidx.compose.ui.window.ComposeUIViewController
import org.lightscout.vatt.core.di.KoinStarter

fun MainViewController() = ComposeUIViewController {
    KoinStarter.startOnce()
    App()
}
