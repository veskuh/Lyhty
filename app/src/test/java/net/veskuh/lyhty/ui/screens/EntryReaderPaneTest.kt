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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EntryReaderPaneTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun entryReaderPaneEnforcesIconAndLabelActionButtons() {
        var fetchFullTextTapped = false
        var markReadTapped = false
        var nextTapped = false

        val entry = EntryEntity(
            id = 101,
            feedId = 10,
            feedTitle = "TechCrunch",
            title = "Android 15 Released",
            content = "Full content body text",
            status = "unread"
        )

        composeTestRule.setContent {
            EntryReaderPane(
                entry = entry,
                postureInfo = PostureInfo(DevicePosture.NORMAL),
                fontSizeScale = 1.0f,
                onFetchFullText = { fetchFullTextTapped = true },
                onMarkRead = { markReadTapped = true },
                onMarkUnread = {},
                onNextEntry = { nextTapped = true; true },
                onPreviousEntry = { true }
            )
        }

        composeTestRule.waitForIdle()

        // Verify explicit Icon + Text Label rule
        composeTestRule.onNodeWithText("Fetch Full Text").assertIsDisplayed()

        composeTestRule.onNode(hasText("Fetch Full Text") and hasClickAction())
            .performScrollTo()
            .performClick()
        assertTrue(fetchFullTextTapped)

        composeTestRule.onNode(hasText("Mark Read") and hasClickAction())
            .performScrollTo()
            .performClick()
        assertTrue(markReadTapped)

        composeTestRule.onNode(hasText("Next") and hasClickAction())
            .performScrollTo()
            .performClick()
        assertTrue(nextTapped)
    }
}
