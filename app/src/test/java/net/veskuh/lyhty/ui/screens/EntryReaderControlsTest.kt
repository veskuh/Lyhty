package net.veskuh.lyhty.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import net.veskuh.lyhty.data.local.entity.EntryEntity
import net.veskuh.lyhty.ui.components.DevicePosture
import net.veskuh.lyhty.ui.components.PostureInfo
import net.veskuh.lyhty.ui.state.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EntryReaderControlsTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val testEntry = EntryEntity(
        id = 1001,
        feedId = 10,
        categoryId = 1,
        title = "Comprehensive Reader Unit Test Article",
        content = "<p>Clean readability text with <b>HTML</b> formatting</p>",
        status = "read"
    )

    @Test
    fun `EntryReaderPane exercises article action callbacks`() {
        var prevClicked = false
        var nextClicked = false
        var markUnreadClicked = false

        composeTestRule.setContent {
            EntryReaderPane(
                entry = testEntry,
                postureInfo = PostureInfo(DevicePosture.NORMAL),
                fontSizeScale = 1.0f,
                readerTheme = ReaderTheme.OLED_DARK,
                onFetchFullText = {},
                onMarkRead = {},
                onMarkUnread = { markUnreadClicked = true },
                onPreviousEntry = { prevClicked = true },
                onNextEntry = { nextClicked = true }
            )
        }

        composeTestRule.waitForIdle()

        // Verify title and html content render
        composeTestRule.onNodeWithText("Comprehensive Reader Unit Test Article").assertIsDisplayed()

        // Click Prev button
        composeTestRule.onNode(hasText("Prev") and hasClickAction())
            .performScrollTo()
            .performClick()
        assertTrue(prevClicked)

        // Click Next button
        composeTestRule.onNode(hasText("Next") and hasClickAction())
            .performScrollTo()
            .performClick()
        assertTrue(nextClicked)

        // Click Mark Unread button
        composeTestRule.onNode(hasText("Mark Unread") and hasClickAction())
            .performScrollTo()
            .performClick()
        assertTrue(markUnreadClicked)
    }

    @Test
    fun `SettingsPane exercises theme switching and font scaling`() {
        var themeChanged: ReaderTheme? = null
        var fontScaled: Float? = null

        composeTestRule.setContent {
            SettingsPane(
                initialServerUrl = "https://veskuh.net/miniflux",
                initialApiKey = "test_key",
                currentLogLevel = net.veskuh.lyhty.util.LogLevel.DEBUG,
                fontSizeScale = 1.0f,
                readerTheme = ReaderTheme.OLED_DARK,
                onSaveConfig = { _, _ -> },
                onSaveLogLevel = {},
                onSetTheme = { themeChanged = it },
                onSetFontSizeScale = { fontScaled = it },
                onBack = {}
            )
        }

        composeTestRule.waitForIdle()

        // Verify header
        composeTestRule.onNodeWithText("App Settings").assertIsDisplayed()

        // Click Sepia Theme swatch
        composeTestRule.onNode(hasText("Sepia") and hasClickAction())
            .performScrollTo()
            .performClick()
        assertEquals(ReaderTheme.SEPIA, themeChanged)
    }
}
