package net.veskuh.lyhty.data.network

import okhttp3.Interceptor
import okhttp3.Response

class MinifluxAuthInterceptor(
    private var apiKey: String
) : Interceptor {

    fun updateApiKey(newKey: String) {
        this.apiKey = newKey
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()

        if (apiKey.isNotBlank()) {
            builder.header("X-Auth-Token", apiKey)
        }

        return chain.proceed(builder.build())
    }
}
