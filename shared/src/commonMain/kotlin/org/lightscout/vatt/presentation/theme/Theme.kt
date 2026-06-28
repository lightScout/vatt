package org.lightscout.vatt.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VirginRed = Color(0xFFE4002B)
private val VirginRedDark = Color(0xFFB00020)

private val LightColors = lightColorScheme(
    primary = VirginRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9DC),
    onPrimaryContainer = Color(0xFF40000A),
    secondary = Color(0xFF1A1A1A),
    onSecondary = Color.White,
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1A1A1A),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF1F1F1),
    onSurfaceVariant = Color(0xFF5F5F5F),
    error = VirginRedDark,
    outline = Color(0xFFDDDDDD),
)

@Composable
fun VattTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}

/** Brand-coordinated accent colours used for the imageRef fallback tiles. */
object BrandColors {
    val tiles = listOf(
        Color(0xFFE4002B),
        Color(0xFF2D6CDF),
        Color(0xFF1D9E75),
        Color(0xFFEF9F27),
        Color(0xFF7F77DD),
        Color(0xFFD85A30),
    )
}
