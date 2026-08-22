package net.veskuh.lyhty.ui.viewmodel

import app.cash.turbine.test
import net.veskuh.lyhty.data.local.entity.CategoryEntity
import net.veskuh.lyhty.data.local.entity.EntryEntity
import net.veskuh.lyhty.data.local.entity.FeedEntity
import net.veskuh.lyhty.data.local.model.CategoryUnreadCount
import net.veskuh.lyhty.data.local.model.FeedUnreadCount
import net.veskuh.lyhty.data.repository.MinifluxRepository
import net.veskuh.lyhty.ui.state.ReaderTheme
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
    fun `advanceToNextUnreadFeed selects next unread feed when available`() = runTest {
        val feedsWithUnread = listOf(
            FeedEntity(10, "TechCrunch", categoryId = 1),
            FeedEntity(20, "Ars Technica", categoryId = 1)
        )
        every { repository.getFeeds() } returns flowOf(feedsWithUnread)
        every { repository.getUnreadCountsByFeed() } returns flowOf(listOf(
            FeedUnreadCount(10, 0),
            FeedUnreadCount(20, 3)
        ))

        val viewModel = MinifluxMainViewModel(repository)
        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem() // initial
            awaitItem() // loaded

            viewModel.selectFeed(feedsWithUnread[0])
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem() // selection updated

            val nextTitle = viewModel.advanceToNextUnreadFeed()
            assertEquals("Ars Technica", nextTitle)
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
}
