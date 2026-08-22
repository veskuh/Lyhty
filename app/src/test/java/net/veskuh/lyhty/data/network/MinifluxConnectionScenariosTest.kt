package net.veskuh.lyhty.data.network

import kotlinx.coroutines.runBlocking
import net.veskuh.lyhty.data.repository.FakeMinifluxConfigRepository
import net.veskuh.lyhty.util.LyhtyErrorClassifier
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class MinifluxConnectionScenariosTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var configRepository: FakeMinifluxConfigRepository
    private lateinit var okHttpClient: OkHttpClient

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        configRepository = FakeMinifluxConfigRepository(
            initialUrl = "http://${mockWebServer.hostName}:${mockWebServer.port}",
            initialKey = "valid_test_token_123"
        )

        okHttpClient = OkHttpClient.Builder()
            .addInterceptor(DynamicHostInterceptor(configRepository))
            .addInterceptor(MinifluxAuthInterceptor(configRepository))
            .build()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    // --- Category 1: Server URL Formatting & Subpaths ---

    @Test
    fun `scenario 1 - domain root URL maps path correctly`() = runBlocking {
        configRepository.saveConfig("http://${mockWebServer.hostName}:${mockWebServer.port}", "token_123")
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        val request = Request.Builder().url("https://placeholder.invalid/v1/categories").build()
        okHttpClient.newCall(request).execute()

        val recorded = mockWebServer.takeRequest()
        assertEquals("/v1/categories", recorded.path)
        assertEquals("token_123", recorded.getHeader("X-Auth-Token"))
    }

    @Test
    fun `scenario 2 - subpath without trailing slash maps correctly`() = runBlocking {
        configRepository.saveConfig("http://${mockWebServer.hostName}:${mockWebServer.port}/miniflux", "token_123")
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        val request = Request.Builder().url("https://placeholder.invalid/v1/feeds").build()
        okHttpClient.newCall(request).execute()

        val recorded = mockWebServer.takeRequest()
        assertEquals("/miniflux/v1/feeds", recorded.path)
    }

    @Test
    fun `scenario 3 - subpath with trailing slash maps correctly`() = runBlocking {
        configRepository.saveConfig("http://${mockWebServer.hostName}:${mockWebServer.port}/miniflux/", "token_123")
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        val request = Request.Builder().url("https://placeholder.invalid/v1/entries").build()
        okHttpClient.newCall(request).execute()

        val recorded = mockWebServer.takeRequest()
        assertEquals("/miniflux/v1/entries", recorded.path)
    }

    @Test
    fun `scenario 4 - deeply nested subpath maps correctly`() = runBlocking {
        configRepository.saveConfig("http://${mockWebServer.hostName}:${mockWebServer.port}/rss/reader/miniflux/", "token_123")
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        val request = Request.Builder().url("https://placeholder.invalid/v1/me").build()
        okHttpClient.newCall(request).execute()

        val recorded = mockWebServer.takeRequest()
        assertEquals("/rss/reader/miniflux/v1/me", recorded.path)
    }

    @Test
    fun `scenario 5 - URL with whitespace is automatically trimmed in outgoing request`() = runBlocking {
        configRepository.saveConfig("  http://${mockWebServer.hostName}:${mockWebServer.port}/miniflux/   ", "  secret_token   ")
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        val request = Request.Builder().url("https://placeholder.invalid/v1/categories").build()
        okHttpClient.newCall(request).execute()

        val recorded = mockWebServer.takeRequest()
        assertEquals("/miniflux/v1/categories", recorded.path)
        assertEquals("secret_token", recorded.getHeader("X-Auth-Token"))
    }

    // --- Category 2: HTTP Status Code Errors & Classification ---

    @Test
    fun `scenario 6 - HTTP 401 Unauthorized generates ERR-AUTH-401`() {
        val httpException = HttpException(Response.error<String>(401, okhttp3.ResponseBody.create(null, "Unauthorized")))
        val error = LyhtyErrorClassifier.classify(httpException)

        assertEquals("ERR-AUTH-401", error.code)
        assertEquals("Authentication Failed", error.title)
    }

    @Test
    fun `scenario 7 - HTTP 403 Forbidden generates ERR-AUTH-403`() {
        val httpException = HttpException(Response.error<String>(403, okhttp3.ResponseBody.create(null, "Forbidden")))
        val error = LyhtyErrorClassifier.classify(httpException)

        assertEquals("ERR-AUTH-403", error.code)
        assertEquals("Access Forbidden", error.title)
    }

    @Test
    fun `scenario 8 - HTTP 404 Not Found generates ERR-NET-404`() {
        val httpException = HttpException(Response.error<String>(404, okhttp3.ResponseBody.create(null, "Not Found")))
        val error = LyhtyErrorClassifier.classify(httpException)

        assertEquals("ERR-NET-404", error.code)
        assertEquals("Route Not Found", error.title)
    }

    @Test
    fun `scenario 9 - HTTP 500 Internal Server Error generates ERR-SERVER-500`() {
        val httpException = HttpException(Response.error<String>(500, okhttp3.ResponseBody.create(null, "Internal Server Error")))
        val error = LyhtyErrorClassifier.classify(httpException)

        assertEquals("ERR-SERVER-500", error.code)
        assertEquals("Server Error", error.title)
    }

    @Test
    fun `scenario 10 - HTTP 502 Bad Gateway generates ERR-SERVER-502`() {
        val httpException = HttpException(Response.error<String>(502, okhttp3.ResponseBody.create(null, "Bad Gateway")))
        val error = LyhtyErrorClassifier.classify(httpException)

        assertEquals("ERR-SERVER-502", error.code)
        assertEquals("Server Error", error.title)
    }

    // --- Category 3: Network Connection Exception Classification ---

    @Test
    fun `scenario 11 - UnknownHostException generates ERR-DNS-101`() {
        val error = LyhtyErrorClassifier.classify(UnknownHostException("miniflux.invalid.domain"))

        assertEquals("ERR-DNS-101", error.code)
        assertEquals("Host Resolution Failed", error.title)
    }

    @Test
    fun `scenario 12 - ConnectException generates ERR-CONN-102`() {
        val error = LyhtyErrorClassifier.classify(ConnectException("Connection refused"))

        assertEquals("ERR-CONN-102", error.code)
        assertEquals("Connection Refused", error.title)
    }

    @Test
    fun `scenario 13 - SocketTimeoutException generates ERR-TIMEOUT-103`() {
        val error = LyhtyErrorClassifier.classify(SocketTimeoutException("Read timed out"))

        assertEquals("ERR-TIMEOUT-103", error.code)
        assertEquals("Connection Timed Out", error.title)
    }

    @Test
    fun `scenario 14 - SSLException generates ERR-SSL-201`() {
        val error = LyhtyErrorClassifier.classify(SSLException("Certificate validation failed"))

        assertEquals("ERR-SSL-201", error.code)
        assertEquals("SSL/TLS Handshake Failed", error.title)
    }

    // --- Category 4: Real-World Edge Cases ---

    @Test
    fun `scenario 15 - double trailing slash in subpath is normalized cleanly`() = runBlocking {
        configRepository.saveConfig("http://${mockWebServer.hostName}:${mockWebServer.port}/miniflux//", "token_123")
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        val request = Request.Builder().url("https://placeholder.invalid/v1/categories").build()
        okHttpClient.newCall(request).execute()

        val recorded = mockWebServer.takeRequest()
        assertEquals("/miniflux/v1/categories", recorded.path)
    }

    @Test
    fun `scenario 16 - API key with special symbols attaches uncorrupted`() = runBlocking {
        configRepository.saveConfig("http://${mockWebServer.hostName}:${mockWebServer.port}", "token_!@#$%^&*()_+~")
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        val request = Request.Builder().url("https://placeholder.invalid/v1/me").build()
        okHttpClient.newCall(request).execute()

        val recorded = mockWebServer.takeRequest()
        assertEquals("token_!@#$%^&*()_+~", recorded.getHeader("X-Auth-Token"))
    }

    @Test
    fun `scenario 17 - HTTP 429 Rate Limit generates ERR-RATE-429`() {
        val httpException = HttpException(Response.error<String>(429, okhttp3.ResponseBody.create(null, "Too Many Requests")))
        val error = LyhtyErrorClassifier.classify(httpException)

        assertEquals("ERR-RATE-429", error.code)
        assertEquals("Rate Limit Exceeded", error.title)
    }

    @Test
    fun `scenario 18 - SerializationException generates ERR-PARSE-401`() {
        val exception = kotlinx.serialization.SerializationException("Invalid JSON payload")
        val error = LyhtyErrorClassifier.classify(exception)

        assertEquals("ERR-PARSE-401", error.code)
        assertEquals("JSON Parse Failure", error.title)
    }

    @Test
    fun `scenario 19 - explicit loopback IP host with subpath maps path correctly`() = runBlocking {
        configRepository.saveConfig("http://127.0.0.1:${mockWebServer.port}/miniflux", "token_123")
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        val request = Request.Builder().url("https://placeholder.invalid/v1/categories").build()
        okHttpClient.newCall(request).execute()

        val recorded = mockWebServer.takeRequest()
        assertEquals("/miniflux/v1/categories", recorded.path)
    }

    @Test
    fun `scenario 20 - HTTP 400 Bad Request generates ERR-REQ-400`() {
        val httpException = HttpException(Response.error<String>(400, okhttp3.ResponseBody.create(null, "Bad Request")))
        val error = LyhtyErrorClassifier.classify(httpException)

        assertEquals("ERR-REQ-400", error.code)
        assertEquals("Bad Request", error.title)
    }

    @Test
    fun `scenario 21 - HTTP 504 Gateway Timeout generates ERR-SERVER-504`() {
        val httpException = HttpException(Response.error<String>(504, okhttp3.ResponseBody.create(null, "Gateway Timeout")))
        val error = LyhtyErrorClassifier.classify(httpException)

        assertEquals("ERR-SERVER-504", error.code)
        assertEquals("Gateway Timeout", error.title)
    }

    @Test
    fun `scenario 22 - SSLPeerUnverifiedException generates ERR-SSL-202`() {
        val exception = javax.net.ssl.SSLPeerUnverifiedException("Peer not authenticated")
        val error = LyhtyErrorClassifier.classify(exception)

        assertEquals("ERR-SSL-202", error.code)
        assertEquals("Untrusted SSL Certificate", error.title)
    }

    @Test
    fun `scenario 23 - server URL with accidental trailing v1 is automatically stripped to avoid v1-v1 double pathing`() = runBlocking {
        configRepository.saveConfig("http://${mockWebServer.hostName}:${mockWebServer.port}/miniflux/v1", "token_123")
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        val request = Request.Builder().url("https://placeholder.invalid/v1/categories").build()
        okHttpClient.newCall(request).execute()

        val recorded = mockWebServer.takeRequest()
        assertEquals("/miniflux/v1/categories", recorded.path)
    }
}
