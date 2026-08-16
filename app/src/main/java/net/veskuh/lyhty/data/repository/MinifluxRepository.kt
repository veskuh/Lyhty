package net.veskuh.lyhty.data.repository

import net.veskuh.lyhty.data.local.entity.CategoryEntity
import net.veskuh.lyhty.data.local.entity.EntryEntity
import net.veskuh.lyhty.data.local.entity.FeedEntity
import net.veskuh.lyhty.data.local.model.CategoryUnreadCount
import net.veskuh.lyhty.data.local.model.FeedUnreadCount
import kotlinx.coroutines.flow.Flow

interface MinifluxRepository {
    fun getCategories(): Flow<List<CategoryEntity>>
    fun getFeeds(): Flow<List<FeedEntity>>
    fun getUnreadCountsByFeed(): Flow<List<FeedUnreadCount>>
    fun getUnreadCountsByCategory(): Flow<List<CategoryUnreadCount>>
    fun getEntries(
        statusFilter: String? = null,
        categoryId: Long? = null,
        feedId: Long? = null,
        limit: Int = 100,
        offset: Int = 0
    ): Flow<List<EntryEntity>>

    fun getEntryById(entryId: Long): Flow<EntryEntity?>
    fun searchEntries(query: String): Flow<List<EntryEntity>>

    suspend fun syncCategoriesAndFeeds()
    suspend fun syncEntries(status: String? = null, offset: Int = 0, limit: Int = 100)
    suspend fun markEntryAsRead(entryId: Long)
    suspend fun markEntryAsUnread(entryId: Long)
    suspend fun fetchServerFullText(entryId: Long): String
    suspend fun flushPendingSyncs()
    suspend fun clearLocalDatabase()
}
