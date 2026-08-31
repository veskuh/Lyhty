package net.veskuh.lyhty.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import net.veskuh.lyhty.data.local.dao.CategoryDao
import net.veskuh.lyhty.data.local.dao.EntryDao
import net.veskuh.lyhty.data.local.dao.FeedDao
import net.veskuh.lyhty.data.local.dao.HistoryDao
import net.veskuh.lyhty.data.local.entity.CategoryEntity
import net.veskuh.lyhty.data.local.entity.EntryEntity
import net.veskuh.lyhty.data.local.entity.EntryFtsEntity
import net.veskuh.lyhty.data.local.entity.FeedEntity
import net.veskuh.lyhty.data.local.entity.ReadingHistoryEntity

@Database(
    entities = [
        CategoryEntity::class,
        FeedEntity::class,
        EntryEntity::class,
        EntryFtsEntity::class,
        ReadingHistoryEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class LyhtyDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun feedDao(): FeedDao
    abstract fun entryDao(): EntryDao
    abstract fun historyDao(): HistoryDao
}
