package net.veskuh.lyhty.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
