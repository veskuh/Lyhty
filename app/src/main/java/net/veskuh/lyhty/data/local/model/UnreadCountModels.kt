package net.veskuh.lyhty.data.local.model

data class FeedUnreadCount(
    val feedId: Long,
    val count: Int
)

data class CategoryUnreadCount(
    val categoryId: Long,
    val count: Int
)
