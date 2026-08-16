package net.veskuh.lyhty.util

import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class LyhtyErrorClassifierTest {

    @Test
    fun `classify HTTP 404 returns ERR-NET-404 with route guidance`() {
        val httpException = HttpException(Response.error<String>(404, "".toResponseBody()))
        val error = LyhtyErrorClassifier.classify(httpException)

        assertEquals("ERR-NET-404", error.code)
        assertEquals("Route Not Found", error.title)
    }

    @Test
    fun `classify HTTP 401 returns ERR-AUTH-401 with API key guidance`() {
        val httpException = HttpException(Response.error<String>(401, "".toResponseBody()))
        val error = LyhtyErrorClassifier.classify(httpException)

        assertEquals("ERR-AUTH-401", error.code)
        assertEquals("Authentication Failed", error.title)
    }

    @Test
    fun `classify UnknownHostException returns ERR-DNS-101`() {
        val error = LyhtyErrorClassifier.classify(UnknownHostException("invalid.domain.xyz"))

        assertEquals("ERR-DNS-101", error.code)
        assertEquals("Host Resolution Failed", error.title)
    }

    @Test
    fun `classify ConnectException returns ERR-CONN-102`() {
        val error = LyhtyErrorClassifier.classify(ConnectException("Connection refused"))

        assertEquals("ERR-CONN-102", error.code)
        assertEquals("Connection Refused", error.title)
    }

    @Test
    fun `classify SocketTimeoutException returns ERR-TIMEOUT-103`() {
        val error = LyhtyErrorClassifier.classify(SocketTimeoutException("Read timed out"))

        assertEquals("ERR-TIMEOUT-103", error.code)
        assertEquals("Connection Timed Out", error.title)
    }

    @Test
    fun `classify SSLException returns ERR-SSL-201`() {
        val error = LyhtyErrorClassifier.classify(SSLException("Handshake failed"))

        assertEquals("ERR-SSL-201", error.code)
        assertEquals("SSL/TLS Handshake Failed", error.title)
    }
}
