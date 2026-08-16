package net.veskuh.lyhty.ui.dialogs

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ServerSettingsDialogTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `ServerSettingsDialog renders initial credentials and triggers save callback`() {
        var savedUrl = ""
        var savedKey = ""
        var dismissed = false

        composeTestRule.setContent {
            ServerSettingsDialog(
                initialServerUrl = "https://reader.example.com",
                initialApiKey = "test-token-123",
                onDismiss = { dismissed = true },
                onSaveConfig = { url, key ->
                    savedUrl = url
                    savedKey = key
                }
            )
        }

        composeTestRule.waitForIdle()

        // Verify updated title renders
        composeTestRule.onNodeWithText("Settings & Diagnostics").assertIsDisplayed()

        // Click Save & Connect button
        composeTestRule.onNodeWithText("Save & Connect").performClick()

        assertEquals("https://reader.example.com", savedUrl)
        assertEquals("test-token-123", savedKey)
    }
}
