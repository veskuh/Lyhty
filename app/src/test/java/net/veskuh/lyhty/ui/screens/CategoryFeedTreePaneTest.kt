package net.veskuh.lyhty.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import net.veskuh.lyhty.data.local.entity.CategoryEntity
import net.veskuh.lyhty.data.local.entity.FeedEntity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CategoryFeedTreePaneTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun categoryFeedTreePaneRendersSyncButtonAndCategories() {
        var syncTapped = false

        composeTestRule.setContent {
            CategoryFeedTreePane(
                categories = listOf(CategoryEntity(1, "Tech")),
                feeds = listOf(FeedEntity(10, "TechCrunch")),
                selectedCategory = null,
                selectedFeed = null,
                onSelectCategory = {},
                onSelectFeed = {},
                onSync = { syncTapped = true }
            )
        }

        composeTestRule.onNodeWithText("Feeds & Categories").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sync").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tech").assertIsDisplayed()

        composeTestRule.onNodeWithText("Sync").performClick()
        assertTrue(syncTapped)
    }
}
