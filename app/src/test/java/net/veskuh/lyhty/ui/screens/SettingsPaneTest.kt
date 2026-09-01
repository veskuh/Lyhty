package net.veskuh.lyhty.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import net.veskuh.lyhty.ui.state.ReaderTheme
import net.veskuh.lyhty.util.LogLevel
import net.veskuh.lyhty.util.LyhtyLogger
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsPaneTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        LyhtyLogger.init(composeTestRule.activity, LogLevel.VERBOSE)
        LyhtyLogger.clearLogs()
    }

    @Test
    fun `SettingsPane renders Diagnostics section with Preview and Clear log buttons`() {
        composeTestRule.setContent {
            SettingsPane(
                initialServerUrl = "https://reader.example.com",
                initialApiKey = "api-key-123",
                currentLogLevel = LogLevel.DEBUG,
                fontSizeScale = 1.0f,
                readerTheme = ReaderTheme.OLED_DARK,
                historyCount = 5,
                onSaveConfig = { _, _ -> },
                onSaveLogLevel = {},
                onSetTheme = {},
                onSetFontSizeScale = {},
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Diagnostics & System").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Share Diagnostic Logs").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Preview Latest Logs").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Clear Diagnostic Logs").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `clicking Preview Latest Logs opens preview dialog`() {
        LyhtyLogger.info("TestLogger", "Sample log entry for preview test")

        composeTestRule.setContent {
            SettingsPane(
                initialServerUrl = "https://reader.example.com",
                initialApiKey = "api-key-123",
                currentLogLevel = LogLevel.DEBUG,
                fontSizeScale = 1.0f,
                readerTheme = ReaderTheme.OLED_DARK,
                onSaveConfig = { _, _ -> },
                onSaveLogLevel = {},
                onSetTheme = {},
                onSetFontSizeScale = {},
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Preview Latest Logs").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Latest Logs (100 lines)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Copy").assertIsDisplayed()
        composeTestRule.onNodeWithText("Close").assertIsDisplayed()
    }

    @Test
    fun `clicking Clear Diagnostic Logs opens confirmation and clears logs on confirm`() {
        var clearCalled = false

        composeTestRule.setContent {
            SettingsPane(
                initialServerUrl = "https://reader.example.com",
                initialApiKey = "api-key-123",
                currentLogLevel = LogLevel.DEBUG,
                fontSizeScale = 1.0f,
                readerTheme = ReaderTheme.OLED_DARK,
                onSaveConfig = { _, _ -> },
                onSaveLogLevel = {},
                onSetTheme = {},
                onSetFontSizeScale = {},
                onClearLogs = { clearCalled = true },
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Clear Diagnostic Logs").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Clear Diagnostic Logs?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clear").performClick()
        assertTrue(clearCalled)
    }
}
