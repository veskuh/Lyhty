package net.veskuh.lyhty.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

import net.veskuh.lyhty.ui.state.ReaderTheme
import net.veskuh.lyhty.util.LogLevel

data class MinifluxConfig(
    val serverUrl: String = "",
    val apiKey: String = "",
    val logLevel: LogLevel = LogLevel.DEBUG,
    val readerTheme: ReaderTheme = ReaderTheme.OLED_DARK,
    val fontSizeScale: Float = 1.0f
)

interface MinifluxConfigRepository {
    val config: Flow<MinifluxConfig>
    suspend fun saveConfig(serverUrl: String, apiKey: String)
    suspend fun saveLogLevel(logLevel: LogLevel)
    suspend fun saveReaderTheme(readerTheme: ReaderTheme)
    suspend fun saveFontSizeScale(fontSizeScale: Float)
    fun getApiKeySync(): String
    fun getServerUrlSync(): String
    fun getLogLevelSync(): LogLevel
    fun getReaderThemeSync(): ReaderTheme
    fun getFontSizeScaleSync(): Float
}

class FakeMinifluxConfigRepository(
    initialUrl: String = "",
    initialKey: String = "",
    initialLogLevel: LogLevel = LogLevel.DEBUG,
    initialReaderTheme: ReaderTheme = ReaderTheme.OLED_DARK,
    initialFontSizeScale: Float = 1.0f
) : MinifluxConfigRepository {

    private val _config = MutableStateFlow(
        MinifluxConfig(
            serverUrl = initialUrl.trim(),
            apiKey = initialKey.trim(),
            logLevel = initialLogLevel,
            readerTheme = initialReaderTheme,
            fontSizeScale = initialFontSizeScale
        )
    )
    override val config: Flow<MinifluxConfig> = _config.asStateFlow()

    override suspend fun saveConfig(serverUrl: String, apiKey: String) {
        _config.value = _config.value.copy(serverUrl = serverUrl.trim(), apiKey = apiKey.trim())
    }

    override suspend fun saveLogLevel(logLevel: LogLevel) {
        _config.value = _config.value.copy(logLevel = logLevel)
    }

    override suspend fun saveReaderTheme(readerTheme: ReaderTheme) {
        _config.value = _config.value.copy(readerTheme = readerTheme)
    }

    override suspend fun saveFontSizeScale(fontSizeScale: Float) {
        _config.value = _config.value.copy(fontSizeScale = fontSizeScale)
    }

    override fun getApiKeySync(): String = _config.value.apiKey.trim()
    override fun getServerUrlSync(): String = _config.value.serverUrl.trim()
    override fun getLogLevelSync(): LogLevel = _config.value.logLevel
    override fun getReaderThemeSync(): ReaderTheme = _config.value.readerTheme
    override fun getFontSizeScaleSync(): Float = _config.value.fontSizeScale
}
