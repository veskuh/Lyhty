package net.veskuh.lyhty.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val userId: Long = 0,
    val unreadCount: Int = 0
)
