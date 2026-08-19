package net.veskuh.lyhty.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import net.veskuh.lyhty.data.local.entity.CategoryEntity
import net.veskuh.lyhty.data.local.entity.EntryEntity
import net.veskuh.lyhty.data.local.entity.FeedEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomDatabaseTest {

    private lateinit var database: LyhtyDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LyhtyDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insert and query categories and feeds`() = runTest {
        val category = CategoryEntity(1, "Tech")
        val feed = FeedEntity(10, "TechCrunch", categoryId = 1)

        database.categoryDao().insertCategories(listOf(category))
        database.feedDao().insertFeeds(listOf(feed))

        val categories = database.categoryDao().getCategories().first()
        val feeds = database.feedDao().getFeeds().first()

        assertEquals(1, categories.size)
        assertEquals(1, feeds.size)
        assertEquals("Tech", categories[0].title)
    }

    @Test
    fun `upsertEntriesWithFts indexes text and searchEntries returns matching result`() = runTest {
        val category = CategoryEntity(1, "Tech")
        val feed = FeedEntity(10, "TechCrunch", categoryId = 1)
        val entry = EntryEntity(
            id = 101,
            feedId = 10,
            categoryId = 1,
            title = "Android 15 Foldable Enhancements",
            content = "Compose Material 3 Adaptive Scaffolds",
            status = "unread"
        )

        database.categoryDao().insertCategories(listOf(category))
        database.feedDao().insertFeeds(listOf(feed))
        database.entryDao().upsertEntriesWithFts(listOf(entry))

        val searchResults = database.entryDao().searchEntries("Foldable").first()
        assertEquals(1, searchResults.size)
        assertEquals("Android 15 Foldable Enhancements", searchResults[0].title)
    }



    @Test
    fun `unread count SQL queries return group by counts`() = runTest {
        val category = CategoryEntity(1, "Tech")
        val feed = FeedEntity(10, "TechCrunch", categoryId = 1)
        val entry1 = EntryEntity(id = 101, feedId = 10, categoryId = 1, title = "A1", status = "unread")
        val entry2 = EntryEntity(id = 102, feedId = 10, categoryId = 1, title = "A2", status = "unread")

        database.categoryDao().insertCategories(listOf(category))
        database.feedDao().insertFeeds(listOf(feed))
        database.entryDao().upsertEntriesWithFts(listOf(entry1, entry2))

        val feedCounts = database.feedDao().getUnreadCountsByFeed().first()
        val categoryCounts = database.categoryDao().getUnreadCountsByCategory().first()

        assertEquals(1, feedCounts.size)
        assertEquals(2, feedCounts[0].count)
        assertEquals(2, categoryCounts[0].count)
    }
}
