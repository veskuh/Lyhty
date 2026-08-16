package net.veskuh.lyhty.data.network

import net.veskuh.lyhty.data.network.dto.CategoryDto
import net.veskuh.lyhty.data.network.dto.CreateCategoryRequestDto
import net.veskuh.lyhty.data.network.dto.CreateFeedRequestDto
import net.veskuh.lyhty.data.network.dto.DiscoverRequestDto
import net.veskuh.lyhty.data.network.dto.DiscoveredFeedDto
import net.veskuh.lyhty.data.network.dto.EntriesResponseDto
import net.veskuh.lyhty.data.network.dto.EntryDto
import net.veskuh.lyhty.data.network.dto.FeedDto
import net.veskuh.lyhty.data.network.dto.FeedIconDto
import net.veskuh.lyhty.data.network.dto.FetchContentResponseDto
import net.veskuh.lyhty.data.network.dto.UpdateCategoryRequestDto
import net.veskuh.lyhty.data.network.dto.UpdateFeedRequestDto
import net.veskuh.lyhty.data.network.dto.UpdateStatusRequestDto
import net.veskuh.lyhty.data.network.dto.UserDto
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface MinifluxApiService {

    // Users
    @GET("v1/me")
    suspend fun getMe(): UserDto

    // Feeds
    @GET("v1/feeds")
    suspend fun getFeeds(): List<FeedDto>

    @POST("v1/feeds")
    suspend fun createFeed(@Body request: CreateFeedRequestDto): FeedDto

    @GET("v1/feeds/{feed_id}")
    suspend fun getFeed(@Path("feed_id") feedId: Long): FeedDto

    @PUT("v1/feeds/{feed_id}")
    suspend fun updateFeed(@Path("feed_id") feedId: Long, @Body request: UpdateFeedRequestDto): FeedDto

    @DELETE("v1/feeds/{feed_id}")
    suspend fun deleteFeed(@Path("feed_id") feedId: Long)

    @PUT("v1/feeds/{feed_id}/refresh")
    suspend fun refreshFeed(@Path("feed_id") feedId: Long)

    @PUT("v1/feeds/refresh")
    suspend fun refreshAllFeeds()

    @GET("v1/feeds/{feed_id}/icon")
    suspend fun getFeedIcon(@Path("feed_id") feedId: Long): FeedIconDto

    @POST("v1/discover")
    suspend fun discoverFeeds(@Body request: DiscoverRequestDto): List<DiscoveredFeedDto>

    // Entries (Articles)
    @GET("v1/entries")
    suspend fun getEntries(
        @Query("status") status: String? = null,
        @Query("direction") direction: String = "desc",
        @Query("order") order: String = "published_at",
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("category_id") categoryId: Long? = null,
        @Query("feed_id") feedId: Long? = null,
        @Query("search") search: String? = null
    ): EntriesResponseDto

    @GET("v1/entries/{entry_id}")
    suspend fun getEntry(@Path("entry_id") entryId: Long): EntryDto

    @PUT("v1/entries")
    suspend fun updateEntriesStatus(@Body request: UpdateStatusRequestDto)

    @GET("v1/feeds/{feed_id}/entries")
    suspend fun getFeedEntries(
        @Path("feed_id") feedId: Long,
        @Query("status") status: String? = null,
        @Query("limit") limit: Int = 100
    ): EntriesResponseDto

    @GET("v1/categories/{category_id}/entries")
    suspend fun getCategoryEntries(
        @Path("category_id") categoryId: Long,
        @Query("status") status: String? = null,
        @Query("limit") limit: Int = 100
    ): EntriesResponseDto

    @GET("v1/entries/{entry_id}/fetch-content")
    suspend fun fetchOriginalContent(@Path("entry_id") entryId: Long): FetchContentResponseDto

    // Categories
    @GET("v1/categories")
    suspend fun getCategories(): List<CategoryDto>

    @POST("v1/categories")
    suspend fun createCategory(@Body request: CreateCategoryRequestDto): CategoryDto

    @PUT("v1/categories/{category_id}")
    suspend fun updateCategory(@Path("category_id") categoryId: Long, @Body request: UpdateCategoryRequestDto): CategoryDto

    @DELETE("v1/categories/{category_id}")
    suspend fun deleteCategory(@Path("category_id") categoryId: Long)

    @GET("v1/categories/{category_id}/feeds")
    suspend fun getCategoryFeeds(@Path("category_id") categoryId: Long): List<FeedDto>

    @PUT("v1/categories/{category_id}/mark-all-as-read")
    suspend fun markCategoryAsRead(@Path("category_id") categoryId: Long)

    // Import / Export OPML
    @Multipart
    @POST("v1/import")
    suspend fun importOpml(@Part file: MultipartBody.Part)

    @GET("v1/export")
    suspend fun exportOpml(): ResponseBody
}
