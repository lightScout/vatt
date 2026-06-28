package org.lightscout.vatt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.lightscout.vatt.core.di.KoinStarter
import org.lightscout.vatt.platform.AndroidAppContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Provide the app context to platform code (reminders) and start DI before any composition.
        AndroidAppContext.init(applicationContext)
        KoinStarter.startOnce()

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }
}
