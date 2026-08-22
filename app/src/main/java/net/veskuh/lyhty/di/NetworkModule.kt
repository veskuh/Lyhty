package net.veskuh.lyhty.di

import net.veskuh.lyhty.data.local.LyhtyDatabase
import net.veskuh.lyhty.data.network.DynamicHostInterceptor
import net.veskuh.lyhty.data.network.MinifluxApiService
import net.veskuh.lyhty.data.network.MinifluxAuthInterceptor
import net.veskuh.lyhty.data.network.TransientNetworkInterceptor
import net.veskuh.lyhty.data.repository.MinifluxRepository
import net.veskuh.lyhty.data.repository.MinifluxRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        configRepository: net.veskuh.lyhty.data.repository.MinifluxConfigRepository
    ): MinifluxAuthInterceptor {
        return MinifluxAuthInterceptor(configRepository)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context,
        authInterceptor: MinifluxAuthInterceptor,
        dynamicHostInterceptor: DynamicHostInterceptor
    ): OkHttpClient {
        val cacheDir = java.io.File(context.cacheDir, "http_cache")
        val cache = okhttp3.Cache(cacheDir, 10 * 1024 * 1024L)

        return OkHttpClient.Builder()
            .cache(cache)
            .connectionSpecs(
                listOf(
                    okhttp3.ConnectionSpec.RESTRICTED_TLS,
                    okhttp3.ConnectionSpec.MODERN_TLS,
                    okhttp3.ConnectionSpec.CLEARTEXT
                )
            )
            .addInterceptor(dynamicHostInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(TransientNetworkInterceptor(maxRetries = 3, initialDelayMs = 200L))
            .build()
    }

    @Provides
    @Singleton
    fun provideMinifluxApiService(
        okHttpClient: OkHttpClient,
        json: Json
    ): MinifluxApiService {
        return Retrofit.Builder()
            .baseUrl("https://miniflux.example.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MinifluxApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideMinifluxConfigRepository(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context
    ): net.veskuh.lyhty.data.repository.MinifluxConfigRepository {
        return net.veskuh.lyhty.data.repository.EncryptedMinifluxConfigRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideMinifluxRepository(
        apiService: MinifluxApiService,
        database: LyhtyDatabase
    ): MinifluxRepository {
        return MinifluxRepositoryImpl(apiService, database)
    }

    @Provides
    @Singleton
    fun provideNetworkMonitor(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context
    ): net.veskuh.lyhty.util.NetworkMonitor {
        return net.veskuh.lyhty.util.NetworkMonitorImpl(context)
    }
}
