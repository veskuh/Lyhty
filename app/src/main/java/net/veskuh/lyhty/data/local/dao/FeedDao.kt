package net.veskuh.lyhty.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import net.veskuh.lyhty.data.local.entity.FeedEntity
import net.veskuh.lyhty.data.local.model.FeedUnreadCount
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {

    @Query("SELECT * FROM feeds ORDER BY title ASC")
    fun getFeeds(): Flow<List<FeedEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeeds(feeds: List<FeedEntity>)

    @Query("DELETE FROM feeds WHERE id = :feedId")
    suspend fun deleteFeed(feedId: Long)

    @Query("SELECT feedId, COUNT(*) AS count FROM entries WHERE status = 'unread' GROUP BY feedId")
    fun getUnreadCountsByFeed(): Flow<List<FeedUnreadCount>>

    @Query("DELETE FROM feeds")
    suspend fun clearAll()
}
