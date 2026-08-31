package net.veskuh.lyhty.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import net.veskuh.lyhty.data.local.entity.EntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {

    @Query("""
        SELECT entries.* FROM entries 
        LEFT JOIN feeds ON entries.feedId = feeds.id
        WHERE (:statusFilter IS NULL 
               OR (:statusFilter = 'starred' AND entries.starred = 1)
               OR (:statusFilter != 'starred' AND entries.status = :statusFilter))
          AND (:categoryId IS NULL OR feeds.categoryId = :categoryId)
          AND (:feedId IS NULL OR entries.feedId = :feedId)
        ORDER BY entries.publishedAt DESC
        LIMIT :limit OFFSET :offset
    """)
    fun getEntries(
        statusFilter: String? = null,
        categoryId: Long? = null,
        feedId: Long? = null,
        limit: Int = 100,
        offset: Int = 0
    ): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE id = :entryId LIMIT 1")
    fun getEntryById(entryId: Long): Flow<EntryEntity?>

    @Query("SELECT COUNT(*) FROM entries WHERE starred = 1")
    fun getStarredCount(): Flow<Int>

    @Upsert
    suspend fun insertEntriesRaw(entries: List<EntryEntity>)

    @Transaction
    suspend fun upsertEntriesWithFts(entries: List<EntryEntity>) {
        if (entries.isEmpty()) return
        val pendingBefore = getPendingSyncEntries().associateBy { it.id }
        insertEntriesRaw(entries)
        pendingBefore.forEach { (id, pendingEntity) ->
            updateEntryStatus(id, pendingEntity.status)
            updateEntryStarred(id, pendingEntity.starred)
        }
    }

    @Query("UPDATE entries SET status = :status, isSyncPending = 1 WHERE id = :entryId")
    suspend fun updateEntryStatus(entryId: Long, status: String)

    @Query("UPDATE entries SET status = 'read', isSyncPending = 1 WHERE feedId = :feedId AND status = 'unread'")
    suspend fun markFeedEntriesAsRead(feedId: Long)

    @Query("""
        UPDATE entries SET status = 'read', isSyncPending = 1 
        WHERE (categoryId = :categoryId OR feedId IN (SELECT id FROM feeds WHERE categoryId = :categoryId)) 
          AND status = 'unread'
    """)
    suspend fun markCategoryEntriesAsRead(categoryId: Long)

    @Query("UPDATE entries SET status = 'read', isSyncPending = 1 WHERE status = 'unread'")
    suspend fun markAllEntriesAsRead()

    @Query("UPDATE entries SET isSyncPending = 0 WHERE feedId = :feedId AND status = 'read'")
    suspend fun clearPendingSyncFlagForFeed(feedId: Long)

    @Query("""
        UPDATE entries SET isSyncPending = 0 
        WHERE (categoryId = :categoryId OR feedId IN (SELECT id FROM feeds WHERE categoryId = :categoryId)) 
          AND status = 'read'
    """)
    suspend fun clearPendingSyncFlagForCategory(categoryId: Long)

    @Query("UPDATE entries SET starred = :starred, isSyncPending = 1 WHERE id = :entryId")
    suspend fun updateEntryStarred(entryId: Long, starred: Boolean)

    @Query("UPDATE entries SET starred = CASE WHEN starred = 1 THEN 0 ELSE 1 END, isSyncPending = 1 WHERE id = :entryId")
    suspend fun toggleEntryStarred(entryId: Long)

    @Query("UPDATE entries SET content = :content WHERE id = :entryId")
    suspend fun updateEntryContent(entryId: Long, content: String)

    @Query("""
        SELECT entries.* FROM entries
        JOIN entries_fts ON entries.id = entries_fts.docid
        WHERE entries_fts MATCH :query
        ORDER BY publishedAt DESC
    """)
    fun searchEntries(query: String): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE isSyncPending = 1")
    suspend fun getPendingSyncEntries(): List<EntryEntity>

    @Query("UPDATE entries SET isSyncPending = 0 WHERE id IN (:entryIds)")
    suspend fun clearPendingSyncFlag(entryIds: List<Long>)

    @Query("DELETE FROM entries")
    suspend fun clearAll()
}
