package net.veskuh.lyhty.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateFeedRequestDto(
    @SerialName("feed_url") val feedUrl: String,
    @SerialName("category_id") val categoryId: Long? = null,
    @SerialName("user_id") val userId: Long? = null
)

@Serializable
data class UpdateFeedRequestDto(
    @SerialName("title") val title: String? = null,
    @SerialName("category_id") val categoryId: Long? = null
)

@Serializable
data class DiscoverRequestDto(
    @SerialName("url") val url: String
)

@Serializable
data class DiscoveredFeedDto(
    @SerialName("title") val title: String,
    @SerialName("type") val type: String = "rss",
    @SerialName("url") val url: String
)

@Serializable
data class CreateCategoryRequestDto(
    @SerialName("title") val title: String
)

@Serializable
data class UpdateCategoryRequestDto(
    @SerialName("title") val title: String
)

@Serializable
data class FeedIconDto(
    @SerialName("id") val id: Long,
    @SerialName("mime_type") val mimeType: String,
    @SerialName("data") val data: String
)
