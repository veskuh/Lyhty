package net.veskuh.lyhty.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

import net.veskuh.lyhty.util.LogLevel

data class MinifluxConfig(
    val serverUrl: String = "",
    val apiKey: String = "",
    val logLevel: LogLevel = LogLevel.DEBUG
)

interface MinifluxConfigRepository {
    val config: Flow<MinifluxConfig>
    suspend fun saveConfig(serverUrl: String, apiKey: String)
    suspend fun saveLogLevel(logLevel: LogLevel)
    fun getApiKeySync(): String
    fun getServerUrlSync(): String
    fun getLogLevelSync(): LogLevel
}

class FakeMinifluxConfigRepository(
    initialUrl: String = "",
    initialKey: String = "",
    initialLogLevel: LogLevel = LogLevel.DEBUG
) : MinifluxConfigRepository {

    private val _config = MutableStateFlow(MinifluxConfig(initialUrl.trim(), initialKey.trim(), initialLogLevel))
    override val config: Flow<MinifluxConfig> = _config.asStateFlow()

    override suspend fun saveConfig(serverUrl: String, apiKey: String) {
        _config.value = _config.value.copy(serverUrl = serverUrl.trim(), apiKey = apiKey.trim())
    }

    override suspend fun saveLogLevel(logLevel: LogLevel) {
        _config.value = _config.value.copy(logLevel = logLevel)
    }

    override fun getApiKeySync(): String = _config.value.apiKey.trim()
    override fun getServerUrlSync(): String = _config.value.serverUrl.trim()
    override fun getLogLevelSync(): LogLevel = _config.value.logLevel
}
