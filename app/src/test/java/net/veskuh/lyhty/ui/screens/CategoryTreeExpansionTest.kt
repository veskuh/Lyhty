package net.veskuh.lyhty.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import net.veskuh.lyhty.data.local.entity.CategoryEntity
import net.veskuh.lyhty.data.local.entity.FeedEntity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CategoryTreeExpansionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val categories = listOf(
        CategoryEntity(id = 10, title = "Technology"),
        CategoryEntity(id = 20, title = "Design")
    )

    private val feeds = listOf(
        FeedEntity(id = 101, title = "Ars Technica", categoryId = 10),
        FeedEntity(id = 201, title = "UX Collective", categoryId = 20)
    )

    @Test
    fun `CategoryFeedTreePane tests category selection and child feed tree expansion`() {
        var selectedCategory: CategoryEntity? = null
        var selectedFeed: FeedEntity? = null

        composeTestRule.setContent {
            CategoryFeedTreePane(
                categories = categories,
                feeds = feeds,
                selectedCategory = null,
                selectedFeed = null,
                unreadCountsByCategory = mapOf(10L to 5, 20L to 2),
                unreadCountsByFeed = mapOf(101L to 5, 201L to 2),
                onSelectCategory = { selectedCategory = it },
                onSelectFeed = { selectedFeed = it },
                onSync = {}
            )
        }

        composeTestRule.waitForIdle()

        // Verify categories render
        composeTestRule.onNodeWithText("Technology").assertIsDisplayed()
        composeTestRule.onNodeWithText("Design").assertIsDisplayed()

        // Click parent category
        composeTestRule.onNodeWithText("Technology").performClick()
        assertEquals(10L, selectedCategory?.id)

        // Click child feed item under expanded tree
        composeTestRule.onNodeWithText("Ars Technica").performClick()
        assertEquals(101L, selectedFeed?.id)
    }
}
