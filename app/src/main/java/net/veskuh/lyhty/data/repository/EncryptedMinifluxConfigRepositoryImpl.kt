package net.veskuh.lyhty.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

import net.veskuh.lyhty.ui.state.ReaderTheme
import net.veskuh.lyhty.util.LogLevel
import net.veskuh.lyhty.util.LyhtyLogger

@Singleton
class EncryptedMinifluxConfigRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : MinifluxConfigRepository {

    private val prefs: SharedPreferences = try {
        createEncryptedPrefs(context)
    } catch (e: Exception) {
        LyhtyLogger.error("ConfigRepo", "EncryptedSharedPreferences initial creation failed: ${e.message}")
        try {
            context.deleteSharedPreferences("lyhty_secure_miniflux_config")
            createEncryptedPrefs(context)
        } catch (_: Exception) {
            context.getSharedPreferences("lyhty_miniflux_config", Context.MODE_PRIVATE)
        }
    }

    private val _config = MutableStateFlow(
        MinifluxConfig(
            serverUrl = prefs.getString("KEY_SERVER_URL", "") ?: "",
            apiKey = prefs.getString("KEY_API_KEY", "") ?: "",
            logLevel = LogLevel.fromName(prefs.getString("KEY_LOG_LEVEL", "DEBUG") ?: "DEBUG"),
            readerTheme = try {
                ReaderTheme.valueOf(prefs.getString("KEY_READER_THEME", "OLED_DARK") ?: "OLED_DARK")
            } catch (_: Exception) {
                ReaderTheme.OLED_DARK
            },
            fontSizeScale = prefs.getFloat("KEY_FONT_SIZE_SCALE", 1.0f),
            showOnlyUnreadFeeds = prefs.getBoolean("KEY_SHOW_ONLY_UNREAD_FEEDS", true)
        )
    )

    override val config: Flow<MinifluxConfig> = _config.asStateFlow()

    override suspend fun saveConfig(serverUrl: String, apiKey: String) {
        val cleanUrl = serverUrl.trim()
        val cleanKey = apiKey.trim()
        LyhtyLogger.error("ConfigRepo", "saveConfig called -> URL: '$cleanUrl', Key length: ${cleanKey.length}")

        prefs.edit()
            .putString("KEY_SERVER_URL", cleanUrl)
            .putString("KEY_API_KEY", cleanKey)
            .apply()

        _config.value = _config.value.copy(serverUrl = cleanUrl, apiKey = cleanKey)
    }

    override suspend fun saveLogLevel(logLevel: LogLevel) {
        prefs.edit()
            .putString("KEY_LOG_LEVEL", logLevel.name)
            .apply()

        LyhtyLogger.setLogLevel(logLevel)
        _config.value = _config.value.copy(logLevel = logLevel)
    }

    override suspend fun saveReaderTheme(readerTheme: ReaderTheme) {
        prefs.edit()
            .putString("KEY_READER_THEME", readerTheme.name)
            .apply()

        _config.value = _config.value.copy(readerTheme = readerTheme)
    }

    override suspend fun saveFontSizeScale(fontSizeScale: Float) {
        prefs.edit()
            .putFloat("KEY_FONT_SIZE_SCALE", fontSizeScale)
            .apply()

        _config.value = _config.value.copy(fontSizeScale = fontSizeScale)
    }

    override suspend fun saveShowOnlyUnreadFeeds(showOnlyUnreadFeeds: Boolean) {
        prefs.edit()
            .putBoolean("KEY_SHOW_ONLY_UNREAD_FEEDS", showOnlyUnreadFeeds)
            .apply()

        _config.value = _config.value.copy(showOnlyUnreadFeeds = showOnlyUnreadFeeds)
    }

    override fun getServerUrlSync(): String = (prefs.getString("KEY_SERVER_URL", "") ?: "").trim()

    override fun getApiKeySync(): String {
        val key = (prefs.getString("KEY_API_KEY", "") ?: "").trim()
        LyhtyLogger.error("ConfigRepo", "getApiKeySync -> key length: ${key.length}")
        return key
    }

    override fun getLogLevelSync(): LogLevel = LogLevel.fromName(
        prefs.getString("KEY_LOG_LEVEL", "DEBUG") ?: "DEBUG"
    )

    override fun getReaderThemeSync(): ReaderTheme {
        return try {
            ReaderTheme.valueOf(prefs.getString("KEY_READER_THEME", "OLED_DARK") ?: "OLED_DARK")
        } catch (_: Exception) {
            ReaderTheme.OLED_DARK
        }
    }

    override fun getFontSizeScaleSync(): Float {
        return prefs.getFloat("KEY_FONT_SIZE_SCALE", 1.0f)
    }

    override fun getShowOnlyUnreadFeedsSync(): Boolean {
        return prefs.getBoolean("KEY_SHOW_ONLY_UNREAD_FEEDS", true)
    }

    companion object {
        private fun createEncryptedPrefs(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            return EncryptedSharedPreferences.create(
                context,
                "lyhty_secure_miniflux_config",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }
}
