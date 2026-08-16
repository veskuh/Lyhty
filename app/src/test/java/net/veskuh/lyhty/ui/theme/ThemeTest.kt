package net.veskuh.lyhty.ui.theme

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import net.veskuh.lyhty.ui.state.ReaderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThemeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `LyhtyTheme renders with OLED_DARK theme`() {
        composeTestRule.setContent {
            LyhtyTheme(readerTheme = ReaderTheme.OLED_DARK) {
                Text("OLED Theme Active")
            }
        }
        composeTestRule.onNodeWithText("OLED Theme Active").assertIsDisplayed()
    }

    @Test
    fun `LyhtyTheme renders with SEPIA theme`() {
        composeTestRule.setContent {
            LyhtyTheme(readerTheme = ReaderTheme.SEPIA) {
                Text("Sepia Theme Active")
            }
        }
        composeTestRule.onNodeWithText("Sepia Theme Active").assertIsDisplayed()
    }

    @Test
    fun `LyhtyTheme renders with LIGHT theme`() {
        composeTestRule.setContent {
            LyhtyTheme(readerTheme = ReaderTheme.LIGHT) {
                Text("Light Theme Active")
            }
        }
        composeTestRule.onNodeWithText("Light Theme Active").assertIsDisplayed()
    }
}
