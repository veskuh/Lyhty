package net.veskuh.lyhty.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import net.veskuh.lyhty.data.local.LyhtyDatabase
import net.veskuh.lyhty.data.network.MinifluxApiService
import net.veskuh.lyhty.di.TestNetworkFactory
import net.veskuh.lyhty.testdouble.SimulatedMinifluxServer
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
class MinifluxRepositoryTest {

    private lateinit var simulatedServer: SimulatedMinifluxServer
    private lateinit var apiService: MinifluxApiService
    private lateinit var database: LyhtyDatabase
    private lateinit var repository: MinifluxRepository

    @Before
    fun setUp() {
        simulatedServer = SimulatedMinifluxServer().apply { start() }
        apiService = TestNetworkFactory.createTestApiService(simulatedServer)

        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LyhtyDatabase::class.java
        ).allowMainThreadQueries().build()

        repository = MinifluxRepositoryImpl(apiService, database)
    }

    @After
    fun tearDown() {
        database.close()
        simulatedServer.shutdown()
    }

    @Test
    fun `syncCategoriesAndFeeds stores Categories and Feeds in Room`() = runTest {
        repository.syncCategoriesAndFeeds()

        val categories = repository.getCategories().first()
        val feeds = repository.getFeeds().first()

        assertEquals(2, categories.size)
        assertEquals(2, feeds.size)
    }

    @Test
    fun `syncEntries populates Room and enables FTS search`() = runTest {
        repository.syncCategoriesAndFeeds()
        repository.syncEntries(status = "unread")

        val entries = repository.getEntries().first()
        assertEquals(2, entries.size)

        val searchResults = repository.searchEntries("Android").first()
        assertEquals(1, searchResults.size)
        assertTrue(searchResults[0].title.contains("Android"))
    }

    @Test
    fun `markEntryAsRead optimistic update and server sync`() = runTest {
        repository.syncCategoriesAndFeeds()
        repository.syncEntries(status = "unread")

        repository.markEntryAsRead(101L)

        val entry = repository.getEntryById(101L).first()
        assertEquals("read", entry?.status)
    }

    @Test
    fun `offline markEntryAsRead flags isSyncPending on entry for batch flush`() = runTest {
        repository.syncCategoriesAndFeeds()
        repository.syncEntries(status = "unread")

        // Force 500 error to simulate offline/server failure
        simulatedServer.enqueueError(500, "Offline")

        repository.markEntryAsRead(101L)

        val pendingSyncEntries = database.entryDao().getPendingSyncEntries()
        assertEquals(1, pendingSyncEntries.size)
        assertEquals(101L, pendingSyncEntries[0].id)
        assertEquals("read", pendingSyncEntries[0].status)

        // Flush pending syncs once server is available
        repository.flushPendingSyncs()
        val emptyPending = database.entryDao().getPendingSyncEntries()
        assertTrue(emptyPending.isEmpty())
    }

    @Test
    fun `fetchServerFullText updates local entry content`() = runTest {
        repository.syncCategoriesAndFeeds()
        repository.syncEntries()

        val content = repository.fetchServerFullText(101L)
        assertTrue(content.contains("Full Extracted Article Content"))

        val updatedEntry = repository.getEntryById(101L).first()
        assertEquals(content, updatedEntry?.content)
    }

    @Test
    fun `toggleBookmark updates local starred and handles offline isSyncPending`() = runTest {
        repository.syncCategoriesAndFeeds()
        repository.syncEntries(status = "unread")

        // 1. Online toggle bookmark
        repository.toggleBookmark(101L)
        val starredEntry = repository.getEntryById(101L).first()
        assertTrue(starredEntry?.starred == true)

        // 2. Offline toggle bookmark (simulated failure)
        simulatedServer.enqueueError(500, "Offline")
        repository.toggleBookmark(102L)
        val pendingSyncEntries = database.entryDao().getPendingSyncEntries()
        assertEquals(1, pendingSyncEntries.size)
        assertEquals(102L, pendingSyncEntries[0].id)
        assertTrue(pendingSyncEntries[0].starred)

        // 3. Flush pending syncs once back online
        repository.flushPendingSyncs()
        val emptyPending = database.entryDao().getPendingSyncEntries()
        assertTrue(emptyPending.isEmpty())

        // 4. Offline UNSTAR toggle (simulated failure)
        simulatedServer.enqueueError(500, "Offline")
        repository.toggleBookmark(102L)
        val pendingUnstar = database.entryDao().getPendingSyncEntries()
        assertEquals(1, pendingUnstar.size)
        assertEquals(102L, pendingUnstar[0].id)
        assertTrue(!pendingUnstar[0].starred)

        // 5. Flush pending unstar syncs
        repository.flushPendingSyncs()
        val emptyPendingAfterUnstar = database.entryDao().getPendingSyncEntries()
        assertTrue(emptyPendingAfterUnstar.isEmpty())
    }

    @Test
    fun `markFeedAsRead and markCategoryAsRead update unread entries to read`() = runTest {
        repository.syncCategoriesAndFeeds()
        repository.syncEntries(status = "unread")

        // Mark feed 10 as read
        repository.markFeedAsRead(10L)
        val feedEntries = repository.getEntries(feedId = 10L).first()
        assertTrue(feedEntries.all { it.status == "read" })

        // Mark category 1 as read
        repository.markCategoryAsRead(1L)
        val catEntries = repository.getEntries(categoryId = 1L).first()
        assertTrue(catEntries.all { it.status == "read" })
    }

    @Test
    fun `markEntryAsUnread marks entry as unread and handles offline sync`() = runTest {
        repository.syncCategoriesAndFeeds()
        repository.syncEntries()

        // 1. Online mark as unread
        repository.markEntryAsUnread(101L)
        val unreadEntry = repository.getEntryById(101L).first()
        assertEquals("unread", unreadEntry?.status)

        // 2. Offline mark as unread
        simulatedServer.enqueueError(500, "Offline")
        repository.markEntryAsUnread(102L)
        val pending = database.entryDao().getPendingSyncEntries()
        assertEquals(1, pending.size)
        assertEquals("unread", pending[0].status)

        repository.flushPendingSyncs()
        assertTrue(database.entryDao().getPendingSyncEntries().isEmpty())
    }

    @Test
    fun `markEntriesAsRead and markAllAsRead update database`() = runTest {
        repository.syncCategoriesAndFeeds()
        repository.syncEntries(status = "unread")

        // Empty list does nothing
        repository.markEntriesAsRead(emptyList())

        // Bulk mark as read
        repository.markEntriesAsRead(listOf(101L))
        assertEquals("read", repository.getEntryById(101L).first()?.status)

        // Mark all as read
        repository.markAllAsRead()
        val allEntries = repository.getEntries().first()
        assertTrue(allEntries.all { it.status == "read" })
    }

    @Test
    fun `clearLocalDatabase clears all tables in Room`() = runTest {
        repository.syncCategoriesAndFeeds()
        repository.syncEntries()

        repository.clearLocalDatabase()

        assertTrue(repository.getCategories().first().isEmpty())
        assertTrue(repository.getFeeds().first().isEmpty())
        assertTrue(repository.getEntries().first().isEmpty())
    }

    @Test(expected = Exception::class)
    fun `fetchServerFullText propagates exception on server failure`() = runTest {
        simulatedServer.enqueueError(500, "Server Error")
        repository.fetchServerFullText(999L)
    }
}
