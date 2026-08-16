package net.veskuh.lyhty.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "entries",
    foreignKeys = [
        ForeignKey(
            entity = FeedEntity::class,
            parentColumns = ["id"],
            childColumns = ["feedId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["feedId"]),
        Index(value = ["status"]),
        Index(value = ["categoryId"])
    ]
)
data class EntryEntity(
    @PrimaryKey val id: Long,
    val feedId: Long,
    val feedTitle: String = "",
    val categoryId: Long? = null,
    val title: String,
    val url: String = "",
    val commentsUrl: String = "",
    val author: String = "",
    val content: String = "",
    val status: String = "unread", // "unread" or "read"
    val publishedAt: String = "",
    val createdAt: String = "",
    val isSyncPending: Boolean = false
)
