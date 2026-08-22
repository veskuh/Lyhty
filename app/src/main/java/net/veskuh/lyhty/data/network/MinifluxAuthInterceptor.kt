package net.veskuh.lyhty.data.network

import net.veskuh.lyhty.data.repository.MinifluxConfigRepository
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton
import net.veskuh.lyhty.util.LyhtyLogger

@Singleton
class MinifluxAuthInterceptor @Inject constructor(
    private val configRepository: MinifluxConfigRepository
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val apiKey = configRepository.getApiKeySync().trim()

        val requestBuilder = originalRequest.newBuilder()

        if (apiKey.isNotEmpty()) {
            LyhtyLogger.debug("AuthInterceptor", "Attaching auth headers (key len=${apiKey.length}) to ${originalRequest.url.host}")
            requestBuilder.header("X-Auth-Token", apiKey)
            requestBuilder.header("X-Miniflux-API-Key", apiKey)
            if (apiKey.contains(":")) {
                val encoded = java.util.Base64.getEncoder().encodeToString(apiKey.toByteArray(Charsets.UTF_8))
                requestBuilder.header("Authorization", "Basic $encoded")
            } else if (!apiKey.startsWith("Bearer ", ignoreCase = true)) {
                requestBuilder.header("Authorization", "Bearer $apiKey")
            }
        } else {
            LyhtyLogger.warn("AuthInterceptor", "No API key configured!")
        }

        return chain.proceed(requestBuilder.build())
    }
}
