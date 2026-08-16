package net.veskuh.lyhty.testdouble

import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SimulatedMinifluxServerTest {

    private lateinit var simulatedServer: SimulatedMinifluxServer
    private val client = OkHttpClient()

    @Before
    fun setUp() {
        simulatedServer = SimulatedMinifluxServer()
        simulatedServer.start()
    }

    @After
    fun tearDown() {
        simulatedServer.shutdown()
    }

    @Test
    fun `getMe returns 200 OK and valid JSON when X-Auth-Token is valid`() = runTest {
        val request = Request.Builder()
            .url(simulatedServer.baseUrl + "v1/me")
            .addHeader("X-Auth-Token", SimulatedMinifluxServer.VALID_TEST_API_KEY)
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            assertEquals(200, response.code)
            assertTrue(body.contains(""""username": "testuser""""))
        }
    }

    @Test
    fun `getMe returns 401 Unauthorized when X-Auth-Token is invalid`() = runTest {
        val request = Request.Builder()
            .url(simulatedServer.baseUrl + "v1/me")
            .addHeader("X-Auth-Token", "invalid_token")
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(401, response.code)
        }
    }

    @Test
    fun `postImport handles OPML upload and returns 201 Created`() = runTest {
        val opmlXml = """<opml version="2.0"><body><outline text="Tech" xmlUrl="https://example.com/rss"/></body></opml>"""
        val request = Request.Builder()
            .url(simulatedServer.baseUrl + "v1/import")
            .addHeader("X-Auth-Token", SimulatedMinifluxServer.VALID_TEST_API_KEY)
            .post(opmlXml.toRequestBody("application/xml".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(201, response.code)
        }
    }

    @Test
    fun `postFeed subscribes to feed and returns 201 Created`() = runTest {
        val jsonBody = """{"feed_url": "https://example.com/rss", "category_id": 1}"""
        val request = Request.Builder()
            .url(simulatedServer.baseUrl + "v1/feeds")
            .addHeader("X-Auth-Token", SimulatedMinifluxServer.VALID_TEST_API_KEY)
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(201, response.code)
        }
    }

    @Test
    fun `deleteFeed unsubscribes from feed and returns 204 No Content`() = runTest {
        val request = Request.Builder()
            .url(simulatedServer.baseUrl + "v1/feeds/10")
            .addHeader("X-Auth-Token", SimulatedMinifluxServer.VALID_TEST_API_KEY)
            .delete()
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(204, response.code)
        }
    }

    @Test
    fun `enqueueError returns forced HTTP 500 response`() = runTest {
        simulatedServer.enqueueError(500, "Internal Server Error")

        val request = Request.Builder()
            .url(simulatedServer.baseUrl + "v1/me")
            .addHeader("X-Auth-Token", SimulatedMinifluxServer.VALID_TEST_API_KEY)
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(500, response.code)
        }
    }
}
