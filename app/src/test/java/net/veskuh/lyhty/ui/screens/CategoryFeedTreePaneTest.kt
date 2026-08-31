package net.veskuh.lyhty.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import net.veskuh.lyhty.data.local.entity.CategoryEntity
import net.veskuh.lyhty.data.local.entity.FeedEntity
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTextInput

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
                unreadCountsByFeed = mapOf(10L to 3),
                unreadCountsByCategory = mapOf(1L to 3),
                showOnlyUnreadFeeds = true,
                onSelectCategory = {},
                onSelectFeed = {},
                onSync = { syncTapped = true }
            )
        }

        composeTestRule.onNodeWithContentDescription("Lyhty").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Refresh Feeds").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tech").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Refresh Feeds").performClick()
        assertTrue(syncTapped)
    }

    @Test
    fun categoryFeedTreePaneFiltersEmptyFeedsWhenShowOnlyUnreadIsTrue() {
        composeTestRule.setContent {
            CategoryFeedTreePane(
                categories = listOf(
                    CategoryEntity(1, "Unread Cat"),
                    CategoryEntity(2, "Empty Cat")
                ),
                feeds = listOf(
                    FeedEntity(10, "Unread Feed", categoryId = 1),
                    FeedEntity(20, "Empty Feed", categoryId = 2)
                ),
                selectedCategory = null,
                selectedFeed = null,
                unreadCountsByFeed = mapOf(10L to 5),
                unreadCountsByCategory = mapOf(1L to 5),
                showOnlyUnreadFeeds = true,
                onSelectCategory = {},
                onSelectFeed = {},
                onSync = {}
            )
        }

        composeTestRule.onNodeWithText("Unread Cat").assertIsDisplayed()
        composeTestRule.onNodeWithText("Empty Cat").assertDoesNotExist()
    }

    @Test
    fun categoryFeedTreePaneShowsEmptyFeedsWhenShowOnlyUnreadIsFalse() {
        composeTestRule.setContent {
            CategoryFeedTreePane(
                categories = listOf(
                    CategoryEntity(1, "Unread Cat"),
                    CategoryEntity(2, "Empty Cat")
                ),
                feeds = listOf(
                    FeedEntity(10, "Unread Feed", categoryId = 1),
                    FeedEntity(20, "Empty Feed", categoryId = 2)
                ),
                selectedCategory = null,
                selectedFeed = null,
                unreadCountsByFeed = mapOf(10L to 5),
                unreadCountsByCategory = mapOf(1L to 5),
                showOnlyUnreadFeeds = false,
                onSelectCategory = {},
                onSelectFeed = {},
                onSync = {}
            )
        }

        composeTestRule.onNodeWithText("Unread Cat").assertIsDisplayed()
        composeTestRule.onNodeWithText("Empty Cat").assertIsDisplayed()
    }

    @Test
    fun categoryFeedTreePaneRendersUncategorizedFeeds() {
        composeTestRule.setContent {
            CategoryFeedTreePane(
                categories = listOf(CategoryEntity(1, "Tech")),
                feeds = listOf(
                    FeedEntity(10, "TechCrunch", categoryId = 1),
                    FeedEntity(20, "Orphan Feed", categoryId = null)
                ),
                selectedCategory = null,
                selectedFeed = null,
                unreadCountsByFeed = mapOf(10L to 2, 20L to 4),
                unreadCountsByCategory = mapOf(1L to 2),
                showOnlyUnreadFeeds = true,
                onSelectCategory = {},
                onSelectFeed = {},
                onSync = {}
            )
        }

        composeTestRule.onNodeWithText("Uncategorized").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Orphan Feed").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun categoryFeedTreePaneKeepsActiveFeedVisibleWhenUnreadReachesZero() {
        composeTestRule.setContent {
            CategoryFeedTreePane(
                categories = listOf(CategoryEntity(1, "Tech")),
                feeds = listOf(
                    FeedEntity(10, "TechCrunch", categoryId = 1)
                ),
                selectedCategory = null,
                selectedFeed = FeedEntity(10, "TechCrunch", categoryId = 1),
                unreadCountsByFeed = mapOf(10L to 0),
                unreadCountsByCategory = mapOf(1L to 0),
                showOnlyUnreadFeeds = true,
                onSelectCategory = {},
                onSelectFeed = {},
                onSync = {}
            )
        }

        composeTestRule.onNodeWithText("Tech").assertIsDisplayed()
        composeTestRule.onNodeWithText("TechCrunch").assertIsDisplayed()
    }

    @Test
    fun categoryFeedTreePaneLongPressShowsMarkAsReadMenu() {
        var markFeedCalled: Long? = null
        var markCatCalled: Long? = null

        composeTestRule.setContent {
            CategoryFeedTreePane(
                categories = listOf(CategoryEntity(1, "Tech")),
                feeds = listOf(
                    FeedEntity(10, "TechCrunch", categoryId = 1)
                ),
                selectedCategory = null,
                selectedFeed = null,
                unreadCountsByFeed = mapOf(10L to 3),
                unreadCountsByCategory = mapOf(1L to 3),
                showOnlyUnreadFeeds = true,
                onSelectCategory = {},
                onSelectFeed = {},
                onMarkCategoryAsRead = { markCatCalled = it },
                onMarkFeedAsRead = { markFeedCalled = it },
                onSync = {}
            )
        }

        // Long press category
        composeTestRule.onNodeWithText("Tech").performTouchInput { longClick() }
        composeTestRule.onNodeWithText("Mark category as read").performClick()
        assertEquals(1L, markCatCalled)

        // Long press feed
        composeTestRule.onNodeWithText("TechCrunch").performTouchInput { longClick() }
        composeTestRule.onNodeWithText("Mark feed as read").performClick()
        assertEquals(10L, markFeedCalled)
    }

    @Test
    fun `categoryFeedTreePane renders Bookmarks item and handles click`() {
        var bookmarksClicked = false

        composeTestRule.setContent {
            CategoryFeedTreePane(
                categories = listOf(CategoryEntity(1, "Tech")),
                feeds = listOf(FeedEntity(10, "TechCrunch", categoryId = 1)),
                selectedCategory = null,
                selectedFeed = null,
                starredCount = 7,
                statusFilter = "unread",
                unreadCountsByFeed = mapOf(10L to 3),
                unreadCountsByCategory = mapOf(1L to 3),
                showOnlyUnreadFeeds = true,
                onSelectCategory = {},
                onSelectFeed = {},
                onSelectBookmarks = { bookmarksClicked = true },
                onSync = {}
            )
        }

        composeTestRule.onNodeWithText("Bookmarks").assertIsDisplayed()
        composeTestRule.onNodeWithText("7").assertIsDisplayed()

        composeTestRule.onNodeWithText("Bookmarks").performClick()
        assertTrue(bookmarksClicked)
    }

    @Test
    fun `categoryFeedTreePane bottom toolbar renders Search and invokes onOpenSearch`() {
        var searchClicked = false

        composeTestRule.setContent {
            CategoryFeedTreePane(
                categories = listOf(CategoryEntity(1, "Tech")),
                feeds = listOf(FeedEntity(10, "TechCrunch", categoryId = 1)),
                selectedCategory = null,
                selectedFeed = null,
                starredCount = 0,
                statusFilter = "unread",
                unreadCountsByFeed = emptyMap(),
                unreadCountsByCategory = emptyMap(),
                showOnlyUnreadFeeds = false,
                onSelectCategory = {},
                onSelectFeed = {},
                onOpenSearch = { searchClicked = true },
                onSync = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Search Articles").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Search Articles").performClick()
        assertTrue(searchClicked)
    }

    @Test
    fun `categoryFeedTreePane renders History shortcut without count badge and invokes onSelectHistory`() {
        var historyClicked = false

        composeTestRule.setContent {
            CategoryFeedTreePane(
                categories = listOf(CategoryEntity(1, "Tech")),
                feeds = listOf(FeedEntity(10, "TechCrunch", categoryId = 1)),
                selectedCategory = null,
                selectedFeed = null,
                starredCount = 0,
                statusFilter = "unread",
                unreadCountsByFeed = emptyMap(),
                unreadCountsByCategory = emptyMap(),
                showOnlyUnreadFeeds = false,
                onSelectCategory = {},
                onSelectFeed = {},
                onSelectHistory = { historyClicked = true },
                onSync = {}
            )
        }

        composeTestRule.onNodeWithText("History").assertIsDisplayed()
        composeTestRule.onNodeWithText("History").performClick()
        assertTrue(historyClicked)
    }
}
