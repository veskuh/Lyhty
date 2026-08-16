package net.veskuh.lyhty.util

import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

object LyhtyErrorClassifier {

    fun classify(throwable: Throwable): LyhtyError {
        val techDetails = throwable.stackTraceToString().take(1000)

        val error = when (throwable) {
            is HttpException -> {
                val code = throwable.code()
                val errorBodyStr = try {
                    throwable.response()?.errorBody()?.string() ?: ""
                } catch (_: Exception) {
                    ""
                }
                val fullTechDetails = "HTTP $code Error Body:\n$errorBodyStr\n\nStackTrace:\n$techDetails"
                LyhtyLogger.error("ErrorClassifier", "HTTP $code Response Body: '$errorBodyStr'")

                when (code) {
                    404 -> LyhtyError(
                        code = "ERR-NET-404",
                        title = "Route Not Found",
                        explanation = "The Miniflux server path was not found (HTTP 404).",
                        actionableHint = "Verify your Miniflux Server URL and subpath prefix (e.g. https://domain.com/miniflux) in Settings.",
                        technicalDetails = fullTechDetails
                    )
                    401 -> LyhtyError(
                        code = "ERR-AUTH-401",
                        title = "Authentication Failed",
                        explanation = if (errorBodyStr.isNotBlank()) "Server response: $errorBodyStr" else "Miniflux API key was rejected (HTTP 401 Unauthorized).",
                        actionableHint = "Verify your API Key/Token under Settings -> API Key.",
                        technicalDetails = fullTechDetails
                    )
                    400 -> LyhtyError(
                        code = "ERR-REQ-400",
                        title = "Bad Request",
                        explanation = "Server rejected request parameters (HTTP 400).",
                        actionableHint = "Verify endpoint configuration and request filters.",
                        technicalDetails = techDetails
                    )
                    403 -> LyhtyError(
                        code = "ERR-AUTH-403",
                        title = "Access Forbidden",
                        explanation = "Your API Key lacks permission to perform this action (HTTP 403).",
                        actionableHint = "Check API key user privileges on your Miniflux admin dashboard.",
                        technicalDetails = techDetails
                    )
                    405 -> LyhtyError(
                        code = "ERR-REQ-405",
                        title = "Method Not Allowed",
                        explanation = "HTTP verb not allowed for target endpoint (HTTP 405).",
                        actionableHint = "Check reverse proxy or server API route configuration.",
                        technicalDetails = techDetails
                    )
                    429 -> LyhtyError(
                        code = "ERR-RATE-429",
                        title = "Rate Limit Exceeded",
                        explanation = "Too many requests sent to Miniflux server (HTTP 429).",
                        actionableHint = "Wait a few moments before syncing again.",
                        technicalDetails = techDetails
                    )
                    504 -> LyhtyError(
                        code = "ERR-SERVER-504",
                        title = "Gateway Timeout",
                        explanation = "Upstream gateway timed out waiting for Miniflux (HTTP 504).",
                        actionableHint = "Check server proxy timeout settings.",
                        technicalDetails = techDetails
                    )
                    in 500..599 -> LyhtyError(
                        code = "ERR-SERVER-$code",
                        title = "Server Error",
                        explanation = "Miniflux remote server reported an internal failure (HTTP $code).",
                        actionableHint = "Check server logs, reverse proxy setup, or try refreshing later.",
                        technicalDetails = techDetails
                    )
                    else -> LyhtyError(
                        code = "ERR-HTTP-$code",
                        title = "HTTP Error $code",
                        explanation = throwable.message() ?: "Server returned HTTP status $code",
                        actionableHint = "Check server settings and connection details.",
                        technicalDetails = techDetails
                    )
                }
            }
            is UnknownHostException -> LyhtyError(
                code = "ERR-DNS-101",
                title = "Host Resolution Failed",
                explanation = "Could not resolve server hostname.",
                actionableHint = "Check server domain spelling or internet connection.",
                technicalDetails = techDetails
            )
            is ConnectException -> LyhtyError(
                code = "ERR-CONN-102",
                title = "Connection Refused",
                explanation = "Server actively refused connection.",
                actionableHint = "Verify server IP/hostname, port number, and firewall/VPN rules.",
                technicalDetails = techDetails
            )
            is SocketTimeoutException -> LyhtyError(
                code = "ERR-TIMEOUT-103",
                title = "Connection Timed Out",
                explanation = "Request timed out waiting for server response.",
                actionableHint = "Check server load and network connection latency.",
                technicalDetails = techDetails
            )
            is javax.net.ssl.SSLPeerUnverifiedException -> LyhtyError(
                code = "ERR-SSL-202",
                title = "Untrusted SSL Certificate",
                explanation = "Server SSL certificate could not be verified by trust store.",
                actionableHint = "Ensure server SSL certificate is valid and not self-signed without root CA trust.",
                technicalDetails = techDetails
            )
            is SSLException -> LyhtyError(
                code = "ERR-SSL-201",
                title = "SSL/TLS Handshake Failed",
                explanation = "Secure TLS connection could not be established.",
                actionableHint = "Check server HTTPS certificate validity and date/time settings.",
                technicalDetails = techDetails
            )
            is kotlinx.serialization.SerializationException -> LyhtyError(
                code = "ERR-PARSE-401",
                title = "JSON Parse Failure",
                explanation = "Failed to parse JSON response from server.",
                actionableHint = "Verify server compatibility with Miniflux API v1.",
                technicalDetails = techDetails
            )
            is IllegalArgumentException -> LyhtyError(
                code = "ERR-URL-301",
                title = "Invalid Server URL",
                explanation = throwable.localizedMessage ?: "Malformed URL structure.",
                actionableHint = "Enter a valid URL starting with http:// or https://",
                technicalDetails = techDetails
            )
            else -> LyhtyError(
                code = "ERR-UNKNOWN-999",
                title = "Unexpected Error",
                explanation = throwable.localizedMessage ?: throwable.javaClass.simpleName,
                actionableHint = "Use 'Share Diagnostic Logs' in Settings to inspect details.",
                technicalDetails = techDetails
            )
        }

        LyhtyLogger.error("ErrorClassifier", "[${error.code}] ${error.title}: ${error.explanation}", throwable)
        return error
    }
}
