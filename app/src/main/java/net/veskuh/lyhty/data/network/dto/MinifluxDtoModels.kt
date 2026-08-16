package net.veskuh.lyhty.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: Long,
    val username: String,
    @SerialName("is_admin") val isAdmin: Boolean = false,
    val theme: String = "default",
    val language: String = "en_US",
    val timezone: String = "UTC"
)

@Serializable
data class CategoryDto(
    val id: Long,
    val title: String,
    @SerialName("user_id") val userId: Long = 0
)

@Serializable
data class FeedDto(
    val id: Long,
    val title: String,
    @SerialName("site_url") val siteUrl: String = "",
    @SerialName("feed_url") val feedUrl: String = "",
    val category: CategoryDto? = null,
    @SerialName("parsing_error_count") val parsingErrorCount: Int = 0
)

@Serializable
data class EntryDto(
    val id: Long,
    @SerialName("user_id") val userId: Long = 0,
    @SerialName("feed_id") val feedId: Long,
    val status: String, // "unread" or "read"
    val title: String,
    val url: String = "",
    @SerialName("comments_url") val commentsUrl: String = "",
    val author: String = "",
    val content: String = "",
    @SerialName("published_at") val publishedAt: String = "",
    @SerialName("created_at") val createdAt: String = "",
    val feed: FeedDto? = null
)

@Serializable
data class EntriesResponseDto(
    val total: Int = 0,
    val entries: List<EntryDto> = emptyList()
)

@Serializable
data class UpdateStatusRequestDto(
    @SerialName("entry_ids") val entryIds: List<Long>,
    val status: String // "read" or "unread"
)

@Serializable
data class FetchContentResponseDto(
    val content: String = ""
)
