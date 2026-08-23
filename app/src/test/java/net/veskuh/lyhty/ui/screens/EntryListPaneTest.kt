package net.veskuh.lyhty.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import net.veskuh.lyhty.data.local.entity.EntryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EntryListPaneTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun entryListPaneRendersEntriesAndClearSearch() {
        var selectedEntryId: Long? = null
        var searchCleared = false

        val entry = EntryEntity(
            id = 101,
            feedId = 10,
            feedTitle = "TechCrunch",
            title = "Android 15 Released",
            content = "Compose M3 Adaptive support",
            status = "unread"
        )

        composeTestRule.setContent {
            EntryListPane(
                entries = listOf(entry),
                selectedEntry = null,
                searchQuery = "Android",
                onSelectEntry = { selectedEntryId = it.id },
                onSearchQueryChange = { if (it.isEmpty()) searchCleared = true }
            )
        }

        composeTestRule.onNodeWithText("Articles").assertIsDisplayed()
        composeTestRule.onNodeWithText("Android 15 Released").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Clear search").performClick()
        assertTrue(searchCleared)

        composeTestRule.onNodeWithText("Android 15 Released").performClick()
        assertEquals(101L, selectedEntryId)
    }

    @Test
    fun `swipe left selects next unread entry in feed list`() {
        var selectedEntryId: Long? = null
        val entries = listOf(
            EntryEntity(
                id = 101,
                feedId = 10,
                feedTitle = "TechCrunch",
                title = "Article 1 (Read)",
                content = "Read content",
                status = "read"
            ),
            EntryEntity(
                id = 102,
                feedId = 10,
                feedTitle = "TechCrunch",
                title = "Article 2 (Unread)",
                content = "Unread content",
                status = "unread"
            )
        )

        composeTestRule.setContent {
            EntryListPane(
                entries = entries,
                selectedEntry = null,
                searchQuery = "",
                onSelectEntry = { selectedEntryId = it.id },
                onSearchQueryChange = {}
            )
        }

        composeTestRule.onNodeWithTag("EntryListPane").performTouchInput {
            swipeLeft()
        }

        assertEquals(102L, selectedEntryId)
    }

    @Test
    fun `swipe right invokes onBack`() {
        var backCalled = false
        val entries = listOf(
            EntryEntity(
                id = 101,
                feedId = 10,
                feedTitle = "TechCrunch",
                title = "Article 1",
                content = "Content",
                status = "read"
            )
        )

        composeTestRule.setContent {
            EntryListPane(
                entries = entries,
                selectedEntry = null,
                searchQuery = "",
                onSelectEntry = {},
                onSearchQueryChange = {},
                onBack = { backCalled = true }
            )
        }

        composeTestRule.onNodeWithTag("EntryListPane").performTouchInput {
            swipeRight()
        }

        assertTrue(backCalled)
    }
}
