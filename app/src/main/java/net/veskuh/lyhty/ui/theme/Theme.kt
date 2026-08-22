package net.veskuh.lyhty.ui.theme

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import net.veskuh.lyhty.ui.state.ReaderTheme

private val OledColorScheme = darkColorScheme(
    background = OledBackground,
    surface = OledSurface,
    surfaceContainerLow = Color(0xFF0D0D0D),
    surfaceContainer = OledSurface,
    surfaceContainerHigh = OledSurfaceHigh,
    onSurface = OledOnSurface,
    primary = OledPrimary,
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = OledSecondary,
    onSecondary = Color(0xFF243240),
    secondaryContainer = Color(0xFF3B4858),
    onSecondaryContainer = Color(0xFFD7E3F8)
)

private val SepiaColorScheme = lightColorScheme(
    background = SepiaBackground,
    surface = SepiaSurface,
    surfaceContainerLow = Color(0xFFF8EFE0),
    surfaceContainer = SepiaSurface,
    surfaceContainerHigh = SepiaSurfaceHigh,
    onSurface = SepiaOnSurface,
    onSurfaceVariant = Color(0xFF5C473A),
    primary = SepiaPrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8D2BB),
    onPrimaryContainer = Color(0xFF2E1500),
    secondary = Color(0xFF6E5B4B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2D1B8),
    onSecondaryContainer = Color(0xFF27190F)
)

private val LightColorScheme = lightColorScheme(
    background = LightBackground,
    surface = LightSurface,
    surfaceContainerLow = Color(0xFFF6F6FA),
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurfaceHigh,
    onSurface = LightOnSurface,
    onSurfaceVariant = Color(0xFF44474E),
    primary = LightPrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF535F70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E3F7),
    onSecondaryContainer = Color(0xFF101C2B)
)

@Composable
fun LyhtyTheme(
    readerTheme: ReaderTheme = ReaderTheme.OLED_DARK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (readerTheme) {
        ReaderTheme.OLED_DARK -> OledColorScheme
        ReaderTheme.SEPIA -> SepiaColorScheme
        ReaderTheme.LIGHT -> LightColorScheme
    }

    val context = LocalContext.current
    DisposableEffect(readerTheme) {
        val activity = context as? ComponentActivity
        if (activity != null) {
            val isDark = readerTheme == ReaderTheme.OLED_DARK
            val statusBarStyle = if (isDark) {
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                )
            }
            activity.enableEdgeToEdge(
                statusBarStyle = statusBarStyle,
                navigationBarStyle = statusBarStyle
            )
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                activity.window.isNavigationBarContrastEnforced = false
            }
        }
        onDispose {}
    }

    MaterialTheme(
        colorScheme = colorScheme
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = content
        )
    }
}
