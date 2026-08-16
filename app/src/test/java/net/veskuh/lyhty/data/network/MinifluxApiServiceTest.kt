package net.veskuh.lyhty.data.network

import net.veskuh.lyhty.data.network.dto.CreateCategoryRequestDto
import net.veskuh.lyhty.data.network.dto.CreateFeedRequestDto
import net.veskuh.lyhty.data.network.dto.DiscoverRequestDto
import net.veskuh.lyhty.data.network.dto.UpdateCategoryRequestDto
import net.veskuh.lyhty.data.network.dto.UpdateFeedRequestDto
import net.veskuh.lyhty.data.network.dto.UpdateStatusRequestDto
import net.veskuh.lyhty.di.TestNetworkFactory
import net.veskuh.lyhty.testdouble.SimulatedMinifluxServer
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MinifluxApiServiceTest {

    private lateinit var simulatedServer: SimulatedMinifluxServer
    private lateinit var apiService: MinifluxApiService

    @Before
    fun setUp() {
        simulatedServer = SimulatedMinifluxServer().apply { start() }
        apiService = TestNetworkFactory.createTestApiService(simulatedServer)
    }

    @After
    fun tearDown() {
        simulatedServer.shutdown()
    }

    @Test
    fun `getMe returns current user info`() = runTest {
        val user = apiService.getMe()
        assertEquals("testuser", user.username)
        assertTrue(user.isAdmin)
    }

    @Test
    fun `feed endpoints work as expected`() = runTest {
        val feeds = apiService.getFeeds()
        assertEquals(2, feeds.size)

        val createdFeed = apiService.createFeed(CreateFeedRequestDto("https://example.com/rss", 1))
        assertEquals(10L, createdFeed.id)

        val singleFeed = apiService.getFeed(10L)
        assertEquals("TechCrunch", singleFeed.title)

        val updatedFeed = apiService.updateFeed(10L, UpdateFeedRequestDto(title = "Updated Tech"))
        assertEquals("TechCrunch", updatedFeed.title)

        apiService.deleteFeed(10L)
        apiService.refreshFeed(10L)
        apiService.refreshAllFeeds()

        val icon = apiService.getFeedIcon(10L)
        assertEquals("image/png", icon.mimeType)

        val discovered = apiService.discoverFeeds(DiscoverRequestDto("https://example.com"))
        assertEquals(1, discovered.size)
    }

    @Test
    fun `entry endpoints work as expected`() = runTest {
        val entriesResponse = apiService.getEntries(status = "unread")
        assertEquals(2, entriesResponse.entries.size)

        val singleEntry = apiService.getEntry(101L)
        assertEquals(101L, singleEntry.id)

        apiService.updateEntriesStatus(UpdateStatusRequestDto(entryIds = listOf(101L), status = "read"))

        val feedEntries = apiService.getFeedEntries(10L)
        assertEquals(2, feedEntries.entries.size)

        val categoryEntries = apiService.getCategoryEntries(1L)
        assertEquals(2, categoryEntries.entries.size)

        val fullText = apiService.fetchOriginalContent(101L)
        assertTrue(fullText.content.contains("Full Extracted Article Content"))
    }

    @Test
    fun `category endpoints work as expected`() = runTest {
        val categories = apiService.getCategories()
        assertEquals(2, categories.size)

        val created = apiService.createCategory(CreateCategoryRequestDto("Science"))
        assertEquals(1L, created.id)

        val updated = apiService.updateCategory(1L, UpdateCategoryRequestDto("New Tech"))
        assertEquals("Tech", updated.title)

        apiService.deleteCategory(1L)

        val catFeeds = apiService.getCategoryFeeds(1L)
        assertEquals(2, catFeeds.size)

        apiService.markCategoryAsRead(1L)
    }

    @Test
    fun `import and export OPML work as expected`() = runTest {
        val filePart = MultipartBody.Part.createFormData(
            "file",
            "feeds.opml",
            "<opml></opml>".toRequestBody("application/xml".toMediaType())
        )
        apiService.importOpml(filePart)

        val exportResponseBody = apiService.exportOpml()
        val xmlContent = exportResponseBody.string()
        assertTrue(xmlContent.contains("opml"))
    }
}
