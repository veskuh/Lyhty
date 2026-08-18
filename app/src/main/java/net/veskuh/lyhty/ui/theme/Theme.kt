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
import androidx.compose.ui.platform.LocalContext
import net.veskuh.lyhty.ui.state.ReaderTheme

private val OledColorScheme = darkColorScheme(
    background = OledBackground,
    surface = OledSurface,
    surfaceContainerHigh = OledSurfaceHigh,
    onSurface = OledOnSurface,
    primary = OledPrimary,
    secondary = OledSecondary
)

private val SepiaColorScheme = lightColorScheme(
    background = SepiaBackground,
    surface = SepiaSurface,
    surfaceContainerHigh = SepiaSurfaceHigh,
    onSurface = SepiaOnSurface,
    primary = SepiaPrimary
)

private val LightColorScheme = lightColorScheme(
    background = LightBackground,
    surface = LightSurface,
    surfaceContainerHigh = LightSurfaceHigh,
    onSurface = LightOnSurface,
    primary = LightPrimary
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
            activity.window.statusBarColor = android.graphics.Color.TRANSPARENT
            activity.window.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                activity.window.isStatusBarContrastEnforced = false
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
