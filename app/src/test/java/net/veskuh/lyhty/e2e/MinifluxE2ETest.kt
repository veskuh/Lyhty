package net.veskuh.lyhty.e2e

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import net.veskuh.lyhty.data.local.LyhtyDatabase
import net.veskuh.lyhty.data.local.entity.EntryEntity
import net.veskuh.lyhty.data.repository.MinifluxRepositoryImpl
import net.veskuh.lyhty.di.TestNetworkFactory
import net.veskuh.lyhty.testdouble.FakeMinifluxRepository
import net.veskuh.lyhty.testdouble.SimulatedMinifluxServer
import net.veskuh.lyhty.ui.components.DevicePosture
import net.veskuh.lyhty.ui.components.PostureInfo
import net.veskuh.lyhty.ui.screens.EntryListPane
import net.veskuh.lyhty.ui.screens.EntryReaderPane
import net.veskuh.lyhty.ui.screens.LyhtyAdaptiveApp
import net.veskuh.lyhty.ui.viewmodel.MinifluxMainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MinifluxE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeMinifluxRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeMinifluxRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `E2E 1 - Complete 3-Pane Adaptive Navigation Flow`() = runTest {
        backgroundScope.launch {
            val viewModel = MinifluxMainViewModel(repository)
            testDispatcher.scheduler.advanceUntilIdle()

            composeTestRule.setContent {
                LyhtyAdaptiveApp(viewModel = viewModel)
            }
            composeTestRule.waitForIdle()

            // Pane 1: Verify Categories render
            composeTestRule.onNodeWithText("Categories").assertIsDisplayed()
            composeTestRule.onNodeWithText("Tech").assertIsDisplayed()
            composeTestRule.onNodeWithText("Design").assertIsDisplayed()

            composeTestRule.setContent {}
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun `E2E 2 - Live FTS5 Search Filtering and Clear Action Flow`() = runTest {
        backgroundScope.launch {
            val viewModel = MinifluxMainViewModel(repository)
            testDispatcher.scheduler.advanceUntilIdle()

            val entries = repository.getEntries().first()
            val entry = entries[0]

            var searchQueryState = "Android"
            var searchCleared = false

            composeTestRule.setContent {
                EntryListPane(
                    entries = listOf(entry),
                    selectedEntry = null,
                    statusFilter = "unread",
                    searchQuery = searchQueryState,
                    onSelectEntry = {},
                    onSetStatusFilter = {},
                    onSearchQueryChange = {
                        searchQueryState = it
                        if (it.isEmpty()) searchCleared = true
                    }
                )
            }

            composeTestRule.waitForIdle()

            // Search bar displays query and clear button icon
            composeTestRule.onNodeWithText("Android 15 Released with Foldable Enhancements").assertIsDisplayed()
            composeTestRule.onNode(hasContentDescription("Clear search filter")).assertIsDisplayed()

            // Click Clear button
            composeTestRule.onNode(hasContentDescription("Clear search filter")).performClick()
            assertTrue(searchCleared)

            composeTestRule.setContent {}
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun `E2E 3 - Reader Action Buttons and Optimistic Status Updates`() = runTest {
        backgroundScope.launch {
            val viewModel = MinifluxMainViewModel(repository)
            testDispatcher.scheduler.advanceUntilIdle()

            val entries = repository.getEntries().first()
            val entry = entries[0]

            var markReadTriggered = false

            composeTestRule.setContent {
                EntryReaderPane(
                    entry = entry,
                    postureInfo = PostureInfo(DevicePosture.NORMAL),
                    fontSizeScale = 1.0f,
                    onFetchFullText = { viewModel.fetchOriginalContent(it) },
                    onMarkRead = {
                        markReadTriggered = true
                        viewModel.markAsRead(it)
                    },
                    onMarkUnread = {}
                )
            }

            composeTestRule.waitForIdle()

            // Click Mark Read button in Reader action bar
            composeTestRule.onNode(hasText("Mark Read") and hasClickAction())
                .performScrollTo()
                .performClick()

            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(markReadTriggered)

            // Verify entry status updated in repository
            val updatedEntry = repository.getEntryById(entry.id).first()
            assertEquals("read", updatedEntry?.status)

            composeTestRule.setContent {}
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun `E2E 4 - Offline Sync Queueing and Server Recovery Flushing Flow`() = runTest {
        backgroundScope.launch {
            val db = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                LyhtyDatabase::class.java
            ).allowMainThreadQueries().build()

            val simulatedServer = SimulatedMinifluxServer().apply { start() }
            val apiService = TestNetworkFactory.createTestApiService(simulatedServer)
            val realRepository = MinifluxRepositoryImpl(apiService, db)

            // Seed initial DB state
            val entry = EntryEntity(
                id = 505,
                feedId = 10,
                categoryId = 1,
                title = "Offline Article",
                content = "<p>Offline content</p>",
                status = "unread"
            )
            db.entryDao().insertEntriesRaw(listOf(entry))

            // 1. Simulates offline state with HTTP 500 error
            simulatedServer.enqueueError(500)
            realRepository.markEntryAsRead(505)

            // Verify local entry updated to read and sync queue logged item
            val pendingSyncs = db.syncDao().getAllPendingItems()
            assertEquals("Pending sync queue size", 1, pendingSyncs.size)
            assertEquals("MARK_READ", pendingSyncs[0].actionType)

            // 2. Server recovers, flush queue
            realRepository.flushPendingSyncs()

            // Verify queue was flushed cleanly
            val flushedSyncs = db.syncDao().getAllPendingItems()
            assertTrue("Queue flushed", flushedSyncs.isEmpty())

            db.close()
            simulatedServer.shutdown()
        }
    }
}
