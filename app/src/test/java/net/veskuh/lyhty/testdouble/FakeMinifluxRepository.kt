package net.veskuh.lyhty.testdouble

import net.veskuh.lyhty.data.local.entity.CategoryEntity
import net.veskuh.lyhty.data.local.entity.EntryEntity
import net.veskuh.lyhty.data.local.entity.FeedEntity
import net.veskuh.lyhty.data.local.model.CategoryUnreadCount
import net.veskuh.lyhty.data.local.model.FeedUnreadCount
import net.veskuh.lyhty.data.repository.MinifluxRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeMinifluxRepository : MinifluxRepository {

    private val categoriesState = MutableStateFlow<List<CategoryEntity>>(
        listOf(
            CategoryEntity(1, "Tech"),
            CategoryEntity(2, "Design")
        )
    )

    private val feedsState = MutableStateFlow<List<FeedEntity>>(
        listOf(
            FeedEntity(10, "TechCrunch", categoryId = 1),
            FeedEntity(20, "Smashing Magazine", categoryId = 2)
        )
    )

    private val entriesState = MutableStateFlow<List<EntryEntity>>(
        listOf(
            EntryEntity(
                id = 101,
                feedId = 10,
                categoryId = 1,
                title = "Android 15 Released with Foldable Enhancements",
                content = "<p>Material 3 Adaptive scaffolds support</p>",
                status = "unread"
            ),
            EntryEntity(
                id = 102,
                feedId = 20,
                categoryId = 2,
                title = "Designing for Dual Display & Foldable Screens",
                content = "<p>Multi-pane desktop layouts</p>",
                status = "unread"
            )
        )
    )

    override fun getCategories(): Flow<List<CategoryEntity>> = categoriesState
    override fun getFeeds(): Flow<List<FeedEntity>> = feedsState

    override fun getUnreadCountsByFeed(): Flow<List<FeedUnreadCount>> =
        entriesState.map { list ->
            list.filter { it.status == "unread" }
                .groupBy { it.feedId }
                .map { (feedId, entries) -> FeedUnreadCount(feedId, entries.size) }
        }

    override fun getUnreadCountsByCategory(): Flow<List<CategoryUnreadCount>> =
        entriesState.map { list ->
            list.filter { it.status == "unread" && it.categoryId != null }
                .groupBy { it.categoryId!! }
                .map { (catId, entries) -> CategoryUnreadCount(catId, entries.size) }
        }

    override fun getEntries(
        statusFilter: String?,
        categoryId: Long?,
        feedId: Long?,
        limit: Int,
        offset: Int
    ): Flow<List<EntryEntity>> = entriesState.map { list ->
        list.filter { entry ->
            (statusFilter == null || entry.status == statusFilter) &&
            (categoryId == null || entry.categoryId == categoryId) &&
            (feedId == null || entry.feedId == feedId)
        }
    }

    override fun getEntryById(entryId: Long): Flow<EntryEntity?> =
        entriesState.map { list -> list.find { it.id == entryId } }

    override fun searchEntries(query: String): Flow<List<EntryEntity>> =
        entriesState.map { list ->
            list.filter { it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true) }
        }

    override suspend fun syncCategoriesAndFeeds() {}
    override suspend fun syncEntries(status: String?, offset: Int, limit: Int) {}

    override suspend fun markEntryAsRead(entryId: Long) {
        entriesState.value = entriesState.value.map {
            if (it.id == entryId) it.copy(status = "read") else it
        }
    }

    override suspend fun markEntryAsUnread(entryId: Long) {
        entriesState.value = entriesState.value.map {
            if (it.id == entryId) it.copy(status = "unread") else it
        }
    }

    override suspend fun fetchServerFullText(entryId: Long): String {
        val extracted = "<article><h1>Full Extracted Text</h1><p>Full content text</p></article>"
        entriesState.value = entriesState.value.map {
            if (it.id == entryId) it.copy(content = extracted) else it
        }
        return extracted
    }

    override suspend fun flushPendingSyncs() {}

    override suspend fun clearLocalDatabase() {
        categoriesState.value = emptyList()
        feedsState.value = emptyList()
        entriesState.value = emptyList()
    }
}
