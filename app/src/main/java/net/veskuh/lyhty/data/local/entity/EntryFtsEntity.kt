package net.veskuh.lyhty.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Fts4(contentEntity = EntryEntity::class)
@Entity(tableName = "entries_fts")
data class EntryFtsEntity(
    @PrimaryKey val rowid: Int,
    val title: String,
    val content: String,
    val author: String,
    val feedTitle: String
)
