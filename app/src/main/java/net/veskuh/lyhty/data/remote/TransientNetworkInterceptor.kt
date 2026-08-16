package net.veskuh.lyhty.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

import net.veskuh.lyhty.util.LyhtyLogger

class TransientNetworkInterceptor(
    private val maxRetries: Int = 3,
    private val initialDelayMs: Long = 500L
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var exception: IOException? = null
        var attempt = 0

        while (attempt < maxRetries) {
            try {
                LyhtyLogger.debug("Network", "HTTP ${request.method} ${request.url} (Attempt ${attempt + 1})")
                response = chain.proceed(request)
                if (response.isSuccessful || !isTransientHttpError(response.code)) {
                    LyhtyLogger.debug("Network", "HTTP Response ${response.code} for ${request.url}")
                    return response
                }
                LyhtyLogger.warn("Network", "Transient HTTP error ${response.code} for ${request.url}")
            } catch (e: IOException) {
                LyhtyLogger.warn("Network", "Network IO exception on ${request.url}", e)
                exception = e
            }

            attempt++
            if (attempt < maxRetries) {
                response?.close()
                try {
                    val delay = initialDelayMs * (1 shl (attempt - 1))
                    LyhtyLogger.info("Network", "Retrying ${request.url} in ${delay}ms...")
                    Thread.sleep(delay)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }

        if (response != null) {
            return response
        }
        throw exception ?: IOException("Network request failed after $maxRetries retries")
    }

    private fun isTransientHttpError(code: Int): Boolean {
        return code == 429 || code == 502 || code == 503 || code == 504
    }
}
