package net.veskuh.lyhty.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "feeds",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class FeedEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val siteUrl: String = "",
    val feedUrl: String = "",
    val categoryId: Long? = null,
    val categoryTitle: String = "",
    val unreadCount: Int = 0
)
