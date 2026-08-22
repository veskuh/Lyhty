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
    }
}
