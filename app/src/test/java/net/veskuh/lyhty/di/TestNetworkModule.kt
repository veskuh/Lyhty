package net.veskuh.lyhty.di

import net.veskuh.lyhty.data.network.MinifluxApiService
import net.veskuh.lyhty.data.network.MinifluxAuthInterceptor
import net.veskuh.lyhty.data.repository.FakeMinifluxConfigRepository
import net.veskuh.lyhty.data.repository.MinifluxConfigRepository
import net.veskuh.lyhty.testdouble.SimulatedMinifluxServer
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object TestNetworkFactory {

    fun createTestConfigRepository(server: SimulatedMinifluxServer): MinifluxConfigRepository {
        return FakeMinifluxConfigRepository(
            initialUrl = server.baseUrl,
            initialKey = SimulatedMinifluxServer.VALID_TEST_API_KEY
        )
    }

    fun createTestApiService(
        server: SimulatedMinifluxServer,
        configRepository: MinifluxConfigRepository = createTestConfigRepository(server)
    ): MinifluxApiService {
        val interceptor = MinifluxAuthInterceptor(configRepository)
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val json = Json { ignoreUnknownKeys = true }
        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl(server.baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(MinifluxApiService::class.java)
    }
}
