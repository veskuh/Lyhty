package net.veskuh.lyhty.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TransientNetworkInterceptorTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        client = OkHttpClient.Builder()
            .addInterceptor(TransientNetworkInterceptor(maxRetries = 3, initialDelayMs = 50L))
            .build()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `retries transient HTTP 503 error and succeeds on subsequent attempt`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(503))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"status": "ok"}"""))

        val request = Request.Builder()
            .url(mockWebServer.url("/v1/me"))
            .build()

        val response = client.newCall(request).execute()

        assertEquals(200, response.code)
        assertEquals(2, mockWebServer.requestCount)
    }

    @Test
    fun `returns initial successful response without extra retries`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"status": "ok"}"""))

        val request = Request.Builder()
            .url(mockWebServer.url("/v1/me"))
            .build()

        val response = client.newCall(request).execute()

        assertEquals(200, response.code)
        assertEquals(1, mockWebServer.requestCount)
    }
}
