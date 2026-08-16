package net.veskuh.lyhty.data.remote

import net.veskuh.lyhty.data.repository.MinifluxConfigRepository
import net.veskuh.lyhty.util.LyhtyLogger
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicHostInterceptor @Inject constructor(
    private val configRepository: MinifluxConfigRepository
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val configuredUrlStr = configRepository.getServerUrlSync()

        if (configuredUrlStr.isNotBlank()) {
            val formattedUrlStr = if (!configuredUrlStr.startsWith("http://") && !configuredUrlStr.startsWith("https://")) {
                "https://$configuredUrlStr"
            } else {
                configuredUrlStr
            }

            val newHttpUrl = formattedUrlStr.toHttpUrlOrNull()
            if (newHttpUrl != null) {
                var serverSegments = newHttpUrl.pathSegments.filter { it.isNotEmpty() }
                if (serverSegments.isNotEmpty() && serverSegments.last().equals("v1", ignoreCase = true)) {
                    serverSegments = serverSegments.dropLast(1)
                }

                val combinedPathSegments = mutableListOf<String>()
                combinedPathSegments.addAll(serverSegments)
                combinedPathSegments.addAll(request.url.pathSegments.filter { it.isNotEmpty() })

                val builder = request.url.newBuilder()
                    .scheme(newHttpUrl.scheme)
                    .host(newHttpUrl.host)
                    .port(newHttpUrl.port)

                repeat(request.url.pathSegments.size) {
                    builder.removePathSegment(0)
                }
                for (segment in combinedPathSegments) {
                    builder.addPathSegment(segment)
                }

                val updatedUrl = builder.build()
                LyhtyLogger.error("DynamicHost", "Routing request to resolved URL: $updatedUrl")
                request = request.newBuilder()
                    .url(updatedUrl)
                    .build()
            }
        }

        return chain.proceed(request)
    }
}
