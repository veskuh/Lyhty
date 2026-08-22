package net.veskuh.lyhty.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
    fun entryListPaneRendersEntriesFilterChipsAndClearSearch() {
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
                statusFilter = "unread",
                searchQuery = "Android",
                onSelectEntry = { selectedEntryId = it.id },
                onSetStatusFilter = {},
                onSearchQueryChange = { if (it.isEmpty()) searchCleared = true }
            )
        }

        composeTestRule.onNodeWithText("Unread (1)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Starred").assertIsDisplayed()
        composeTestRule.onNodeWithText("Android 15 Released").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Clear search").performClick()
        assertTrue(searchCleared)

        composeTestRule.onNodeWithText("Android 15 Released").performClick()
        assertEquals(101L, selectedEntryId)
    }
}
