package net.veskuh.lyhty.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import net.veskuh.lyhty.data.local.entity.EntryEntity
import net.veskuh.lyhty.data.local.entity.ReadingHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("""
        SELECT entries.* FROM entries
        INNER JOIN reading_history ON entries.id = reading_history.entryId
        ORDER BY reading_history.readAt DESC
        LIMIT :limit OFFSET :offset
    """)
    fun getHistoryEntries(limit: Int = 100, offset: Int = 0): Flow<List<EntryEntity>>

    @Upsert
    suspend fun recordHistory(history: ReadingHistoryEntity)

    @Query("SELECT COUNT(*) FROM reading_history")
    fun getHistoryCount(): Flow<Int>

    @Query("DELETE FROM reading_history")
    suspend fun clearHistory()
}
