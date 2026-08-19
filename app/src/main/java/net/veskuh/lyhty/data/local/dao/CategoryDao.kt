package net.veskuh.lyhty.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import net.veskuh.lyhty.data.local.entity.CategoryEntity
import net.veskuh.lyhty.data.local.model.CategoryUnreadCount
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY title ASC")
    fun getCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategory(categoryId: Long)

    @Query("SELECT feeds.categoryId AS categoryId, COUNT(*) AS count FROM entries INNER JOIN feeds ON entries.feedId = feeds.id WHERE entries.status = 'unread' AND feeds.categoryId IS NOT NULL GROUP BY feeds.categoryId")
    fun getUnreadCountsByCategory(): Flow<List<CategoryUnreadCount>>

    @Query("DELETE FROM categories")
    suspend fun clearAll()
}
