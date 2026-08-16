package com.lyhty.app.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.lyhty.app.MainActivity
import com.lyhty.app.testdouble.SimulatedMinifluxServer
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class MinifluxE2ETest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var simulatedServer: SimulatedMinifluxServer

    @Before
    fun setUp() {
        simulatedServer = SimulatedMinifluxServer()
        simulatedServer.start()
        hiltRule.inject()
    }

    @After
    fun tearDown() {
        simulatedServer.shutdown()
    }

    @Test
    fun fullMinifluxUserJourney_SelectFeed_ReadArticle_FetchFullText() {
        // 1. Verify Feed & Category Tree is displayed
        composeTestRule.onNodeWithText("Feeds & Categories").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sync").assertIsDisplayed()

        // 2. Select "All Unread Feeds" shortcut
        composeTestRule.onNodeWithText("All Unread Feeds").performClick()

        // 3. Select first entry
        composeTestRule.onNodeWithText("Android 15 Released with Foldable Enhancements").performClick()

        // 4. Verify Reader Pane opens with mandatory Icon + Label buttons
        composeTestRule.onNodeWithText("Fetch Full Text").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mark Read").assertIsDisplayed()
        composeTestRule.onNodeWithText("Open in Browser").assertIsDisplayed()

        // 5. Trigger Server-side full text extraction
        composeTestRule.onNodeWithText("Fetch Full Text").performClick()

        // 6. Verify entry status update action
        composeTestRule.onNodeWithText("Mark Read").performClick()
        composeTestRule.onNodeWithText("Mark Unread").assertIsDisplayed()
    }
}
