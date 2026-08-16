package net.veskuh.lyhty.data.remote

import net.veskuh.lyhty.data.repository.FakeMinifluxConfigRepository
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DynamicHostInterceptorTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var configRepo: FakeMinifluxConfigRepository
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        configRepo = FakeMinifluxConfigRepository(
            initialUrl = "http://${mockWebServer.hostName}:${mockWebServer.port}"
        )

        client = OkHttpClient.Builder()
            .addInterceptor(DynamicHostInterceptor(configRepo))
            .build()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `DynamicHostInterceptor dynamically rewrites request URL to target configured server`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"status": "ok"}"""))

        val dummyRequest = Request.Builder()
            .url("https://placeholder.miniflux.invalid/v1/me")
            .build()

        val response = client.newCall(dummyRequest).execute()
        assertEquals(200, response.code)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/v1/me", recordedRequest.path)
    }

    @Test
    fun `DynamicHostInterceptor handles subpath URL WITH trailing slash`() {
        kotlinx.coroutines.runBlocking {
            configRepo.saveConfig("http://${mockWebServer.hostName}:${mockWebServer.port}/miniflux/", "token")
        }
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val request = Request.Builder().url("https://placeholder.invalid/v1/categories").build()
        client.newCall(request).execute()

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/miniflux/v1/categories", recordedRequest.path)
    }

    @Test
    fun `DynamicHostInterceptor handles subpath URL WITHOUT trailing slash`() {
        kotlinx.coroutines.runBlocking {
            configRepo.saveConfig("http://${mockWebServer.hostName}:${mockWebServer.port}/miniflux", "token")
        }
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val request = Request.Builder().url("https://placeholder.invalid/v1/categories").build()
        client.newCall(request).execute()

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/miniflux/v1/categories", recordedRequest.path)
    }
}
