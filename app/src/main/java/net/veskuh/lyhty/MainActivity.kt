package net.veskuh.lyhty

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.veskuh.lyhty.data.repository.MinifluxConfigRepository
import net.veskuh.lyhty.ui.screens.LyhtyAdaptiveApp
import net.veskuh.lyhty.ui.state.ReaderTheme
import net.veskuh.lyhty.ui.theme.LightBackground
import net.veskuh.lyhty.ui.theme.OledBackground
import net.veskuh.lyhty.ui.theme.SepiaBackground
import net.veskuh.lyhty.ui.viewmodel.MinifluxMainViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var configRepository: MinifluxConfigRepository

    private val viewModel: MinifluxMainViewModel by viewModels()
    private var foldingFeature by mutableStateOf<FoldingFeature?>(null)
    private var isReady by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Set dynamic window background matching persisted reader theme to avoid any color flash
        val persistedTheme = configRepository.getReaderThemeSync()
        val bgColor = when (persistedTheme) {
            ReaderTheme.OLED_DARK -> OledBackground.toArgb()
            ReaderTheme.SEPIA -> SepiaBackground.toArgb()
            ReaderTheme.LIGHT -> LightBackground.toArgb()
        }
        window.setBackgroundDrawable(ColorDrawable(bgColor))

        enableEdgeToEdge()

        // Keep splash screen visible until initial Room database snapshot is ready
        splashScreen.setKeepOnScreenCondition { !isReady }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.isLocalCacheReady.collect { isReady = it }
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                WindowInfoTracker.getOrCreate(this@MainActivity)
                    .windowLayoutInfo(this@MainActivity)
                    .collect { layoutInfo ->
                        foldingFeature = layoutInfo.displayFeatures
                            .filterIsInstance<FoldingFeature>()
                            .firstOrNull()
                    }
            }
        }

        setContent {
            LyhtyAdaptiveApp(
                foldingFeature = foldingFeature,
                viewModel = viewModel
            )
        }
    }
}
