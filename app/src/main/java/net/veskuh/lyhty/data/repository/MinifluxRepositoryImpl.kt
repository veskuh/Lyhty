package net.veskuh.lyhty.data.repository

import net.veskuh.lyhty.data.local.LyhtyDatabase
import net.veskuh.lyhty.data.local.entity.CategoryEntity
import net.veskuh.lyhty.data.local.entity.EntryEntity
import net.veskuh.lyhty.data.local.entity.FeedEntity
import net.veskuh.lyhty.data.local.model.CategoryUnreadCount
import net.veskuh.lyhty.data.local.model.FeedUnreadCount
import net.veskuh.lyhty.data.network.MinifluxApiService
import net.veskuh.lyhty.data.network.dto.UpdateStatusRequestDto
import net.veskuh.lyhty.util.LyhtyLogger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class MinifluxRepositoryImpl @Inject constructor(
    private val apiService: MinifluxApiService,
    private val database: LyhtyDatabase
) : MinifluxRepository {

    override fun getCategories(): Flow<List<CategoryEntity>> =
        database.categoryDao().getCategories()

    override fun getFeeds(): Flow<List<FeedEntity>> =
        database.feedDao().getFeeds()

    override fun getUnreadCountsByFeed(): Flow<List<FeedUnreadCount>> =
        database.feedDao().getUnreadCountsByFeed()

    override fun getUnreadCountsByCategory(): Flow<List<CategoryUnreadCount>> =
        database.categoryDao().getUnreadCountsByCategory()

    override fun getEntries(
        statusFilter: String?,
        categoryId: Long?,
        feedId: Long?,
        limit: Int,
        offset: Int
    ): Flow<List<EntryEntity>> =
        database.entryDao().getEntries(statusFilter, categoryId, feedId, limit, offset)

    override fun getEntryById(entryId: Long): Flow<EntryEntity?> =
        database.entryDao().getEntryById(entryId)

    override fun searchEntries(query: String): Flow<List<EntryEntity>> =
        database.entryDao().searchEntries(query)

    override suspend fun syncCategoriesAndFeeds() {
        LyhtyLogger.info("Repository", "Starting syncCategoriesAndFeeds...")
        try {
            val categoriesDto = apiService.getCategories()
            val feedsDto = apiService.getFeeds()
            LyhtyLogger.info("Repository", "Fetched ${categoriesDto.size} categories and ${feedsDto.size} feeds from Miniflux server.")

            val categoryEntities = categoriesDto.map {
                CategoryEntity(id = it.id, title = it.title, userId = it.userId)
            }
            val feedEntities = feedsDto.map {
                FeedEntity(
                    id = it.id,
                    title = it.title,
                    siteUrl = it.siteUrl,
                    feedUrl = it.feedUrl,
                    categoryId = it.category?.id,
                    categoryTitle = it.category?.title.orEmpty()
                )
            }

            database.categoryDao().insertCategories(categoryEntities)
            database.feedDao().insertFeeds(feedEntities)
            LyhtyLogger.debug("Repository", "Successfully persisted categories and feeds to Room DB.")
        } catch (e: Exception) {
            LyhtyLogger.error("Repository", "Failed syncCategoriesAndFeeds", e)
            throw e
        }
    }

    override suspend fun syncEntries(status: String?, offset: Int, limit: Int) {
        LyhtyLogger.info("Repository", "Starting syncEntries(status=$status, offset=$offset, limit=$limit)...")
        try {
            val response = when (status) {
                "starred" -> apiService.getEntries(starred = "1", offset = offset, limit = limit)
                else -> apiService.getEntries(status = status, offset = offset, limit = limit)
            }
            LyhtyLogger.info("Repository", "Fetched ${response.entries.size} entries (total server count: ${response.total}).")

            val entryEntities = response.entries.map { dto ->
                EntryEntity(
                    id = dto.id,
                    feedId = dto.feedId,
                    feedTitle = dto.feed?.title.orEmpty(),
                    categoryId = dto.feed?.category?.id,
                    title = dto.title,
                    url = dto.url,
                    commentsUrl = dto.commentsUrl,
                    author = dto.author,
                    content = dto.content,
                    status = dto.status,
                    starred = dto.starred,
                    publishedAt = dto.publishedAt,
                    createdAt = dto.createdAt,
                    isSyncPending = false
                )
            }
            database.entryDao().upsertEntriesWithFts(entryEntities)
            LyhtyLogger.debug("Repository", "Successfully upserted ${entryEntities.size} entries into Room DB & FTS5.")
        } catch (e: Exception) {
            LyhtyLogger.error("Repository", "Failed syncEntries", e)
            throw e
        }
    }

    override suspend fun markEntryAsRead(entryId: Long) {
        LyhtyLogger.info("Repository", "Marking entry $entryId as READ...")
        database.entryDao().updateEntryStatus(entryId, "read")
        try {
            apiService.updateEntriesStatus(UpdateStatusRequestDto(listOf(entryId), status = "read"))
            database.entryDao().clearPendingSyncFlag(listOf(entryId))
            LyhtyLogger.debug("Repository", "Server successfully confirmed entry $entryId marked READ.")
        } catch (e: Exception) {
            LyhtyLogger.warn("Repository", "Server update failed for entry $entryId (read). Retaining isSyncPending flag for offline batch flush.", e)
        }
    }

    override suspend fun markEntryAsUnread(entryId: Long) {
        LyhtyLogger.info("Repository", "Marking entry $entryId as UNREAD...")
        database.entryDao().updateEntryStatus(entryId, "unread")
        try {
            apiService.updateEntriesStatus(UpdateStatusRequestDto(listOf(entryId), status = "unread"))
            database.entryDao().clearPendingSyncFlag(listOf(entryId))
            LyhtyLogger.debug("Repository", "Server successfully confirmed entry $entryId marked UNREAD.")
        } catch (e: Exception) {
            LyhtyLogger.warn("Repository", "Server update failed for entry $entryId (unread). Retaining isSyncPending flag for offline batch flush.", e)
        }
    }

    override suspend fun markEntriesAsRead(entryIds: List<Long>) {
        if (entryIds.isEmpty()) return
        LyhtyLogger.info("Repository", "Bulk marking ${entryIds.size} entries as READ...")
        entryIds.forEach { id ->
            database.entryDao().updateEntryStatus(id, "read")
        }
        try {
            apiService.updateEntriesStatus(UpdateStatusRequestDto(entryIds, status = "read"))
            database.entryDao().clearPendingSyncFlag(entryIds)
            LyhtyLogger.debug("Repository", "Server confirmed bulk mark READ for ${entryIds.size} entries.")
        } catch (e: Exception) {
            LyhtyLogger.warn("Repository", "Server bulk update failed for ${entryIds.size} entries. Retaining isSyncPending flag for offline batch flush.", e)
        }
    }

    override suspend fun markFeedAsRead(feedId: Long) {
        LyhtyLogger.info("Repository", "Marking all unread entries in feed $feedId as READ...")
        database.entryDao().markFeedEntriesAsRead(feedId)
        try {
            apiService.markFeedAsRead(feedId)
            database.entryDao().clearPendingSyncFlagForFeed(feedId)
            LyhtyLogger.debug("Repository", "Server successfully confirmed feed $feedId marked as READ.")
        } catch (e: Exception) {
            LyhtyLogger.warn("Repository", "Server mark feed $feedId as read failed. Retaining isSyncPending flag for offline batch flush.", e)
        }
    }

    override suspend fun markCategoryAsRead(categoryId: Long) {
        LyhtyLogger.info("Repository", "Marking all unread entries in category $categoryId as READ...")
        database.entryDao().markCategoryEntriesAsRead(categoryId)
        try {
            apiService.markCategoryAsRead(categoryId)
            database.entryDao().clearPendingSyncFlagForCategory(categoryId)
            LyhtyLogger.debug("Repository", "Server successfully confirmed category $categoryId marked as READ.")
        } catch (e: Exception) {
            LyhtyLogger.warn("Repository", "Server mark category $categoryId as read failed. Retaining isSyncPending flag for offline batch flush.", e)
        }
    }

    override suspend fun markAllAsRead() {
        LyhtyLogger.info("Repository", "Marking all unread entries as READ...")
        database.entryDao().markAllEntriesAsRead()
        try {
            val unreadPending = database.entryDao().getPendingSyncEntries().filter { it.status == "read" }.map { it.id }
            if (unreadPending.isNotEmpty()) {
                apiService.updateEntriesStatus(UpdateStatusRequestDto(entryIds = unreadPending, status = "read"))
                database.entryDao().clearPendingSyncFlag(unreadPending)
            }
            LyhtyLogger.debug("Repository", "Successfully marked all unread entries as READ.")
        } catch (e: Exception) {
            LyhtyLogger.warn("Repository", "Server bulk mark all as read failed. Retaining isSyncPending flag for offline batch flush.", e)
        }
    }

    override suspend fun toggleBookmark(entryId: Long) {
        LyhtyLogger.info("Repository", "Toggling bookmark for entry $entryId...")
        database.entryDao().toggleEntryStarred(entryId)
        try {
            apiService.toggleBookmark(entryId)
            database.entryDao().clearPendingSyncFlag(listOf(entryId))
            LyhtyLogger.debug("Repository", "Server successfully confirmed entry $entryId bookmark toggled.")
        } catch (e: Exception) {
            LyhtyLogger.warn("Repository", "Server toggle bookmark failed for entry $entryId. Retaining isSyncPending flag for offline batch flush.", e)
        }
    }

    override suspend fun fetchServerFullText(entryId: Long): String {
        LyhtyLogger.info("Repository", "Fetching server-side readability content for entry $entryId...")
        return try {
            val fullContent = apiService.fetchOriginalContent(entryId).content
            database.entryDao().updateEntryContent(entryId, fullContent)
            LyhtyLogger.info("Repository", "Successfully extracted full content (${fullContent.length} chars) for entry $entryId.")
            fullContent
        } catch (e: Exception) {
            LyhtyLogger.error("Repository", "Failed to fetch original content for entry $entryId", e)
            throw e
        }
    }

    override suspend fun flushPendingSyncs() {
        LyhtyLogger.info("Repository", "Flushing pending offline syncs...")
        val pendingEntries = database.entryDao().getPendingSyncEntries()
        if (pendingEntries.isNotEmpty()) {
            val readIds = pendingEntries.filter { it.status == "read" }.map { it.id }
            val unreadIds = pendingEntries.filter { it.status == "unread" }.map { it.id }
            val starredIds = pendingEntries.filter { it.starred }.map { it.id }
            val unstarredIds = pendingEntries.filter { !it.starred }.map { it.id }

            if (readIds.isNotEmpty()) {
                try {
                    apiService.updateEntriesStatus(UpdateStatusRequestDto(entryIds = readIds, status = "read"))
                    database.entryDao().clearPendingSyncFlag(readIds)
                    LyhtyLogger.info("Repository", "Flushed ${readIds.size} pending read entries to server.")
                } catch (e: Exception) {
                    LyhtyLogger.warn("Repository", "Failed flushing pending read entries", e)
                }
            }
            if (unreadIds.isNotEmpty()) {
                try {
                    apiService.updateEntriesStatus(UpdateStatusRequestDto(entryIds = unreadIds, status = "unread"))
                    database.entryDao().clearPendingSyncFlag(unreadIds)
                    LyhtyLogger.info("Repository", "Flushed ${unreadIds.size} pending unread entries to server.")
                } catch (e: Exception) {
                    LyhtyLogger.warn("Repository", "Failed flushing pending unread entries", e)
                }
            }
            if (starredIds.isNotEmpty()) {
                try {
                    apiService.updateEntriesStatus(UpdateStatusRequestDto(entryIds = starredIds, starred = true))
                    database.entryDao().clearPendingSyncFlag(starredIds)
                    LyhtyLogger.info("Repository", "Flushed ${starredIds.size} pending starred entries to server.")
                } catch (e: Exception) {
                    LyhtyLogger.warn("Repository", "Failed flushing pending starred entries", e)
                }
            }
            if (unstarredIds.isNotEmpty()) {
                try {
                    apiService.updateEntriesStatus(UpdateStatusRequestDto(entryIds = unstarredIds, starred = false))
                    database.entryDao().clearPendingSyncFlag(unstarredIds)
                    LyhtyLogger.info("Repository", "Flushed ${unstarredIds.size} pending unstarred entries to server.")
                } catch (e: Exception) {
                    LyhtyLogger.warn("Repository", "Failed flushing pending unstarred entries", e)
                }
            }
        }
    }

    override suspend fun clearLocalDatabase() {
        LyhtyLogger.warn("Repository", "Clearing local Room database tables...")
        database.categoryDao().clearAll()
        database.feedDao().clearAll()
        database.entryDao().clearAll()
        LyhtyLogger.info("Repository", "Local database successfully cleared.")
    }
}
