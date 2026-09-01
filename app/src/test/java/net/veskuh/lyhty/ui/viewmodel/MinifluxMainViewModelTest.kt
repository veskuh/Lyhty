package net.veskuh.lyhty.ui.viewmodel

import app.cash.turbine.test
import net.veskuh.lyhty.data.local.entity.CategoryEntity
import net.veskuh.lyhty.data.local.entity.EntryEntity
import net.veskuh.lyhty.data.local.entity.FeedEntity
import net.veskuh.lyhty.data.local.model.CategoryUnreadCount
import net.veskuh.lyhty.data.local.model.FeedUnreadCount
import net.veskuh.lyhty.data.repository.MinifluxRepository
import net.veskuh.lyhty.ui.state.ReaderTheme
import net.veskuh.lyhty.util.NetworkMonitor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MinifluxMainViewModelTest {

    private val repository: MinifluxRepository = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private val testCategories = listOf(CategoryEntity(1, "Tech"))
    private val testFeeds = listOf(FeedEntity(10, "TechCrunch", categoryId = 1))
    private val testEntries = listOf(
        EntryEntity(
            id = 101,
            feedId = 10,
            categoryId = 1,
            title = "Android 15 Released",
            content = "Compose M3 Adaptive support",
            status = "unread",
            publishedAt = "2026-08-16T12:00:00Z"
        ),
        EntryEntity(
            id = 102,
            feedId = 10,
            categoryId = 1,
            title = "Designing for Foldables",
            content = "Multi-pane desktop layouts",
            status = "unread",
            publishedAt = "2026-08-16T10:00:00Z"
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { repository.getCategories() } returns flowOf(testCategories)
        every { repository.getFeeds() } returns flowOf(testFeeds)
        every { repository.getUnreadCountsByFeed() } returns flowOf(listOf(FeedUnreadCount(10, 2)))
        every { repository.getUnreadCountsByCategory() } returns flowOf(listOf(CategoryUnreadCount(1, 2)))
        every { repository.getStarredCount() } returns flowOf(5)
        every { repository.getHistoryCount() } returns flowOf(3)
        every { repository.getHistoryEntries(any(), any()) } returns flowOf(testEntries)
        coEvery { repository.recordHistory(any()) } returns Unit
        coEvery { repository.clearHistory() } returns Unit
        every { repository.getEntries(any(), any(), any(), any(), any()) } returns flowOf(testEntries)
        every { repository.getEntryById(101) } returns flowOf(testEntries[0])
        every { repository.getEntryById(102) } returns flowOf(testEntries[1])
        every { repository.searchEntries(any()) } returns flowOf(testEntries)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads categories, feeds, unread counts, and entries`() = runTest {
        val viewModel = MinifluxMainViewModel(repository)

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val initialLoading = awaitItem()
            val loadedState = awaitItem()

            assertEquals(1, loadedState.categories.size)
            assertEquals(1, loadedState.feeds.size)
            assertEquals(2, loadedState.entries.size)
            assertEquals(2, loadedState.unreadCountsByFeed[10L])
            assertEquals(2, loadedState.unreadCountsByCategory[1L])
        }
    }

    @Test
    fun `selecting entry updates selectedEntry state`() = runTest {
        val viewModel = MinifluxMainViewModel(repository)

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val initialLoading = awaitItem()
            val loadedState = awaitItem()

            viewModel.selectEntry(101)
            testDispatcher.scheduler.advanceUntilIdle()

            val stateAfter = awaitItem()
            assertNotNull(stateAfter.selectedEntry)
            assertEquals(101L, stateAfter.selectedEntry?.id)
        }
    }

    @Test
    fun `selectNextEntry advances to next entry in list`() = runTest {
        val viewModel = MinifluxMainViewModel(repository)

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val initialLoading = awaitItem()
            val loadedState = awaitItem()

            viewModel.selectEntry(101)
            testDispatcher.scheduler.advanceUntilIdle()
            val state101 = awaitItem()

            viewModel.selectNextEntry()
            testDispatcher.scheduler.advanceUntilIdle()
            val state102 = awaitItem()

            assertEquals(102L, state102.selectedEntry?.id)
        }
    }

    @Test
    fun `selectPreviousEntry navigates backwards in entry list`() = runTest {
        val viewModel = MinifluxMainViewModel(repository)

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem() // initial
            awaitItem() // loaded

            viewModel.selectEntry(102)
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()

            viewModel.selectPreviousEntry()
            testDispatcher.scheduler.advanceUntilIdle()
            val state101 = awaitItem()

            assertEquals(101L, state101.selectedEntry?.id)
        }
    }

    @Test
    fun `selectCategory and selectFeed update selection state`() = runTest {
        val viewModel = MinifluxMainViewModel(repository)

        viewModel.selectCategory(testCategories[0])
        viewModel.selectFeed(testFeeds[0])

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()

            assertEquals(1L, state.selectedCategory?.id)
            assertEquals(10L, state.selectedFeed?.id)
        }
    }

    @Test
    fun `setReaderTheme and setFontSizeScale update state`() = runTest {
        val viewModel = MinifluxMainViewModel(repository)

        viewModel.setReaderTheme(ReaderTheme.SEPIA)
        viewModel.setFontSizeScale(1.25f)
        viewModel.setSearchQuery("Android")

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()

            assertEquals(ReaderTheme.SEPIA, state.readerTheme)
            assertEquals(1.25f, state.fontSizeScale, 0.01f)
            assertEquals("Android", state.searchQuery)
        }
    }

    @Test
    fun `clearError resets errorMessage state to null`() = runTest {
        val viewModel = MinifluxMainViewModel(repository)
        viewModel.clearError()

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            assertNull(state.errorMessage)
        }
    }

    @Test
    fun `fetchOriginalContent triggers repository server extraction`() = runTest {
        coEvery { repository.fetchServerFullText(101) } returns "<p>Full Extracted</p>"
        val viewModel = MinifluxMainViewModel(repository)

        viewModel.fetchOriginalContent(101)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.fetchServerFullText(101) }
    }

    @Test
    fun `markAsRead and markAsUnread call repository`() = runTest {
        val viewModel = MinifluxMainViewModel(repository)

        viewModel.markAsRead(101)
        viewModel.markAsUnread(102)
        viewModel.toggleBookmark(101)
        viewModel.markFeedAsRead(10)
        viewModel.markCategoryAsRead(1)
        viewModel.markAllAsRead()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.markEntryAsRead(101) }
        coVerify { repository.markEntryAsUnread(102) }
        coVerify { repository.toggleBookmark(101) }
        coVerify { repository.markFeedAsRead(10) }
        coVerify { repository.markCategoryAsRead(1) }
        coVerify { repository.markAllAsRead() }
    }

    @Test
    fun `advanceToNextUnreadFeed follows visual sidebar tree order`() = runTest {
        // Category 1: AlphaCat (Feed 10: TechCrunch [0], Feed 20: ZDNet [3])
        // Category 2: BetaCat (Feed 30: AppleInsider [2])
        // Uncategorized (Feed 40: BBC News [4])
        val cats = listOf(
            CategoryEntity(1, "AlphaCat"),
            CategoryEntity(2, "BetaCat")
        )
        val feedsWithUnread = listOf(
            FeedEntity(30, "AppleInsider", categoryId = 2), // Alphabetically first in flat list, but under Category 2
            FeedEntity(40, "BBC News", categoryId = 0),     // Uncategorized
            FeedEntity(10, "TechCrunch", categoryId = 1),   // Under Category 1
            FeedEntity(20, "ZDNet", categoryId = 1)         // Under Category 1
        )
        every { repository.getCategories() } returns flowOf(cats)
        every { repository.getFeeds() } returns flowOf(feedsWithUnread)
        every { repository.getUnreadCountsByFeed() } returns flowOf(listOf(
            FeedUnreadCount(10, 0),
            FeedUnreadCount(20, 3),
            FeedUnreadCount(30, 2),
            FeedUnreadCount(40, 4)
        ))

        val viewModel = MinifluxMainViewModel(repository)
        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem() // initial
            awaitItem() // loaded

            // Start at Feed 10 (under AlphaCat)
            viewModel.selectFeed(feedsWithUnread.first { it.id == 10L })
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()

            // Next in AlphaCat should be ZDNet (Feed 20), NOT AppleInsider (which is first in flat alphabetical)
            val nextTitle1 = viewModel.advanceToNextUnreadFeed()
            assertEquals("ZDNet", nextTitle1)
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()

            // Next after ZDNet in sidebar tree should be Category 2's AppleInsider (Feed 30)
            val nextTitle2 = viewModel.advanceToNextUnreadFeed()
            assertEquals("AppleInsider", nextTitle2)
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()

            // Next after AppleInsider in sidebar tree should be Uncategorized's BBC News (Feed 40)
            val nextTitle3 = viewModel.advanceToNextUnreadFeed()
            assertEquals("BBC News", nextTitle3)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setStatusFilter and setShowOnlyUnreadFeeds update state`() = runTest {
        val viewModel = MinifluxMainViewModel(repository)

        viewModel.setStatusFilter("starred")
        viewModel.setShowOnlyUnreadFeeds(false)

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()

            assertEquals("starred", state.statusFilter)
            assertEquals(false, state.showOnlyUnreadFeeds)
        }
    }

    @Test
    fun `selectBookmarks updates statusFilter to starred and clears selections`() = runTest {
        val viewModel = MinifluxMainViewModel(repository)

        viewModel.selectCategory(1L)
        viewModel.selectFeed(10L)
        viewModel.selectBookmarks()

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()

            assertEquals("starred", state.statusFilter)
            assertNull(state.selectedCategory)
            assertNull(state.selectedFeed)
            assertEquals(5, state.starredCount)
        }
    }

    @Test
    fun `selectFeed, selectCategory, and selectAllUnread reset statusFilter to unread`() = runTest {
        val viewModel = MinifluxMainViewModel(repository)

        // First select bookmarks -> statusFilter becomes "starred"
        viewModel.selectBookmarks()
        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals("starred", expectMostRecentItem().statusFilter)
        }

        // Then select feed -> statusFilter must reset to "unread"
        viewModel.selectFeed(10L)
        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals("unread", state.statusFilter)
            assertEquals(10L, state.selectedFeed?.id)
        }

        // Select bookmarks again, then select category -> statusFilter must reset to "unread"
        viewModel.selectBookmarks()
        viewModel.selectCategory(1L)
        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals("unread", state.statusFilter)
            assertEquals(1L, state.selectedCategory?.id)
        }

        // Select bookmarks again, then select all unread -> statusFilter must reset to "unread"
        viewModel.selectBookmarks()
        viewModel.selectAllUnread()
        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals("unread", state.statusFilter)
            assertNull(state.selectedCategory)
            assertNull(state.selectedFeed)
        }
    }

    @Test
    fun `refreshAll handles repository errors gracefully`() = runTest {
        coEvery { repository.syncCategoriesAndFeeds() } throws RuntimeException("Network timeout")
        val viewModel = MinifluxMainViewModel(repository)

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            assertNotNull(state.errorMessage)
            assertNotNull(state.currentError)
        }
    }

    @Test
    fun `onAppForegrounded does not trigger refresh when backgrounded for less than 30 minutes`() = runTest {
        val viewModel = MinifluxMainViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Clear invocations from init
        io.mockk.clearMocks(repository, answers = false)

        val baseTime = 1_000_000L
        viewModel.onAppBackgrounded(timestampMs = baseTime)

        // Return to foreground 10 minutes later (< 30 min)
        viewModel.onAppForegrounded(currentTimestampMs = baseTime + 10 * 60 * 1000L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repository.syncCategoriesAndFeeds() }
    }

    @Test
    fun `onAppForegrounded triggers refreshAll when backgrounded for 30 minutes or more`() = runTest {
        val viewModel = MinifluxMainViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Clear invocations from init
        io.mockk.clearMocks(repository, answers = false)

        val baseTime = 1_000_000L
        viewModel.onAppBackgrounded(timestampMs = baseTime)

        // Return to foreground 30 minutes later (>= 30 min)
        viewModel.onAppForegrounded(currentTimestampMs = baseTime + 30 * 60 * 1000L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(atLeast = 1) { repository.syncCategoriesAndFeeds() }
        coVerify(atLeast = 1) { repository.syncEntries("starred") }
    }

    @Test
    fun `onAppForegrounded does not trigger refresh when offline even if backgrounded for over 30 minutes`() = runTest {
        val networkMonitor: NetworkMonitor = mockk()
        every { networkMonitor.isOnline } returns kotlinx.coroutines.flow.MutableStateFlow(false)

        val viewModel = MinifluxMainViewModel(
            repository = repository,
            networkMonitor = networkMonitor
        )
        testDispatcher.scheduler.advanceUntilIdle()

        io.mockk.clearMocks(repository, answers = false)

        val baseTime = 1_000_000L
        viewModel.onAppBackgrounded(timestampMs = baseTime)

        // Return to foreground 45 minutes later while offline
        viewModel.onAppForegrounded(currentTimestampMs = baseTime + 45 * 60 * 1000L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repository.syncCategoriesAndFeeds() }
    }

    @Test
    fun `onAppForegrounded recovers background timestamp from SavedStateHandle after ViewModel reconstruction`() = runTest {
        val savedStateHandle = androidx.lifecycle.SavedStateHandle()
        val baseTime = 1_000_000L
        savedStateHandle.set(MinifluxMainViewModel.KEY_LAST_BACKGROUND_TIMESTAMP, baseTime)

        val viewModel = MinifluxMainViewModel(
            repository = repository,
            savedStateHandle = savedStateHandle
        )
        testDispatcher.scheduler.advanceUntilIdle()

        io.mockk.clearMocks(repository, answers = false)

        // Return to foreground 35 minutes later
        viewModel.onAppForegrounded(currentTimestampMs = baseTime + 35 * 60 * 1000L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(atLeast = 1) { repository.syncCategoriesAndFeeds() }
        assertNull(savedStateHandle.get<Long>(MinifluxMainViewModel.KEY_LAST_BACKGROUND_TIMESTAMP))
    }

    @Test
    fun `selected entry is not reset when refreshAll runs`() = runTest {
        val entry = testEntries.first()
        val viewModel = MinifluxMainViewModel(repository)

        viewModel.selectEntry(entry.id)
        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(entry.id, expectMostRecentItem().selectedEntry?.id)
        }

        // Trigger refresh
        viewModel.refreshAll()
        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(entry.id, expectMostRecentItem().selectedEntry?.id)
        }
    }

    @Test
    fun `selected entry is restored from SavedStateHandle upon ViewModel initialization`() = runTest {
        val entry = testEntries.first()
        val savedStateHandle = androidx.lifecycle.SavedStateHandle()
        savedStateHandle.set(MinifluxMainViewModel.KEY_SELECTED_ENTRY_ID, entry.id)

        val viewModel = MinifluxMainViewModel(
            repository = repository,
            savedStateHandle = savedStateHandle
        )

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(entry.id, state.selectedEntry?.id)
        }
    }

    @Test
    fun `selectHistory sets statusFilter to history and loads history count`() = runTest {
        val viewModel = MinifluxMainViewModel(repository)

        viewModel.selectHistory()
        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals("history", state.statusFilter)
            assertEquals(3, state.historyCount)
            assertNull(state.selectedCategory)
            assertNull(state.selectedFeed)
        }
    }

    @Test
    fun `selectEntry records reading history in repository`() = runTest {
        val viewModel = MinifluxMainViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectEntry(101L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(atLeast = 1) { repository.recordHistory(101L) }
    }

    @Test
    fun `clearHistory invokes repository clearHistory`() = runTest {
        val viewModel = MinifluxMainViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearHistory()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(atLeast = 1) { repository.clearHistory() }
    }

    @Test
    fun `refreshAll does not call syncEntries with history when history is selected`() = runTest {
        val viewModel = MinifluxMainViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectHistory()
        testDispatcher.scheduler.advanceUntilIdle()

        io.mockk.clearMocks(repository, answers = false)

        viewModel.refreshAll()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.syncCategoriesAndFeeds() }
        coVerify(exactly = 1) { repository.syncEntries("starred") }
        coVerify(exactly = 0) { repository.syncEntries("history") }
    }

    @Test
    fun `isLocalCacheReady emits true once local database flows emit`() = runTest {
        val viewModel = MinifluxMainViewModel(repository)

        viewModel.isLocalCacheReady.test {
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(expectMostRecentItem())
        }
    }

    @Test
    fun `initial uiState seeds preferences synchronously from configRepository`() = runTest {
        val configRepo: net.veskuh.lyhty.data.repository.MinifluxConfigRepository = mockk(relaxed = true) {
            every { getReaderThemeSync() } returns net.veskuh.lyhty.ui.state.ReaderTheme.SEPIA
            every { getFontSizeScaleSync() } returns 1.25f
            every { getShowOnlyUnreadFeedsSync() } returns false
            every { getServerUrlSync() } returns "https://example.com"
            every { getApiKeySync() } returns "key"
            every { getLogLevelSync() } returns net.veskuh.lyhty.util.LogLevel.DEBUG
        }

        val viewModel = MinifluxMainViewModel(
            repository = repository,
            configRepository = configRepo
        )

        assertEquals(net.veskuh.lyhty.ui.state.ReaderTheme.SEPIA, viewModel.uiState.value.readerTheme)
        assertEquals(1.25f, viewModel.uiState.value.fontSizeScale)
        assertEquals(false, viewModel.uiState.value.showOnlyUnreadFeeds)
    }
}
