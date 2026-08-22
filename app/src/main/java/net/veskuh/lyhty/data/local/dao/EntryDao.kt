package net.veskuh.lyhty.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntriesRaw(entries: List<EntryEntity>)

    @Query("DELETE FROM entries_fts WHERE rowid IN (SELECT rowid FROM entries WHERE id IN (:entryIds))")
    suspend fun deleteFtsIndex(entryIds: List<Long>)

    @Query("""
        INSERT INTO entries_fts(rowid, title, content)
        SELECT rowid, title, content FROM entries WHERE id IN (:entryIds)
    """)
    suspend fun insertFtsIndex(entryIds: List<Long>)

    @Transaction
    suspend fun upsertEntriesWithFts(entries: List<EntryEntity>) {
        if (entries.isEmpty()) return
        val pendingBefore = getPendingSyncEntries().associateBy { it.id }
        val entryIds = entries.map { it.id }
        deleteFtsIndex(entryIds)
        insertEntriesRaw(entries)
        pendingBefore.forEach { (id, pendingEntity) ->
            updateEntryStatus(id, pendingEntity.status)
            updateEntryStarred(id, pendingEntity.starred)
        }
        insertFtsIndex(entryIds)
    }

    @Query("UPDATE entries SET status = :status, isSyncPending = 1 WHERE id = :entryId")
    suspend fun updateEntryStatus(entryId: Long, status: String)

    @Query("UPDATE entries SET starred = :starred, isSyncPending = 1 WHERE id = :entryId")
    suspend fun updateEntryStarred(entryId: Long, starred: Boolean)

    @Query("UPDATE entries SET starred = CASE WHEN starred = 1 THEN 0 ELSE 1 END, isSyncPending = 1 WHERE id = :entryId")
    suspend fun toggleEntryStarred(entryId: Long)

    @Query("UPDATE entries SET content = :content WHERE id = :entryId")
    suspend fun updateEntryContent(entryId: Long, content: String)

    @Query("""
        SELECT entries.* FROM entries
        JOIN entries_fts ON entries.rowid = entries_fts.rowid
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
