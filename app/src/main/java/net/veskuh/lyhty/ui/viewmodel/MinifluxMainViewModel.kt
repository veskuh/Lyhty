package net.veskuh.lyhty.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import net.veskuh.lyhty.data.local.entity.CategoryEntity
import net.veskuh.lyhty.data.local.entity.EntryEntity
import net.veskuh.lyhty.data.local.entity.FeedEntity
import net.veskuh.lyhty.data.repository.MinifluxRepository
import net.veskuh.lyhty.ui.state.MinifluxUiState
import net.veskuh.lyhty.ui.state.ReaderTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import net.veskuh.lyhty.data.repository.MinifluxConfigRepository

import net.veskuh.lyhty.util.NetworkMonitor

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MinifluxMainViewModel @Inject constructor(
    private val repository: MinifluxRepository,
    private val configRepository: MinifluxConfigRepository? = null,
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = networkMonitor?.isOnline ?: MutableStateFlow(true)

    init {
        viewModelScope.launch {
            var wasOffline = false
            isOnline.collect { online ->
                if (online) {
                    net.veskuh.lyhty.util.LyhtyLogger.info("ViewModel", "Network active/restored (isOnline=true).")
                    if (wasOffline) {
                        _currentError.value = null
                        _errorMessage.value = null
                        refreshAll()
                    } else {
                        try {
                            repository.flushPendingSyncs()
                        } catch (_: Exception) {}
                    }
                    wasOffline = false
                } else {
                    wasOffline = true
                    net.veskuh.lyhty.util.LyhtyLogger.warn("ViewModel", "Network lost (isOnline=false).")
                }
            }
        }
    }

    fun getServerUrl(): String = configRepository?.getServerUrlSync() ?: ""
    fun getApiKey(): String = configRepository?.getApiKeySync() ?: ""
    fun getLogLevel(): net.veskuh.lyhty.util.LogLevel = configRepository?.getLogLevelSync() ?: net.veskuh.lyhty.util.LogLevel.DEBUG

    fun setLogLevel(level: net.veskuh.lyhty.util.LogLevel) {
        viewModelScope.launch {
            configRepository?.saveLogLevel(level)
        }
    }

    fun updateServerConfig(serverUrl: String, apiKey: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _currentError.value = null
            net.veskuh.lyhty.util.LyhtyLogger.error("ViewModel", "updateServerConfig called -> URL: '$serverUrl', Key length: ${apiKey.length}")
            try {
                configRepository?.saveConfig(serverUrl, apiKey)
                repository.clearLocalDatabase()
                repository.syncCategoriesAndFeeds()
                repository.syncEntries("starred")
                if (_statusFilter.value != "starred") {
                    repository.syncEntries(_statusFilter.value)
                }
            } catch (e: Exception) {
                handleException(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private val _statusFilter = MutableStateFlow<String?>("unread")
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    private val _selectedFeedId = MutableStateFlow<Long?>(null)
    private val _selectedEntryId = MutableStateFlow<Long?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _readerTheme = MutableStateFlow(configRepository?.getReaderThemeSync() ?: ReaderTheme.OLED_DARK)
    private val _fontSizeScale = MutableStateFlow(configRepository?.getFontSizeScaleSync() ?: 1.0f)
    private val _showOnlyUnreadFeeds = MutableStateFlow(configRepository?.getShowOnlyUnreadFeedsSync() ?: true)
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _currentError = MutableStateFlow<net.veskuh.lyhty.util.LyhtyError?>(null)

    private val _categories = repository.getCategories()
    private val _feeds = repository.getFeeds()
    private val _unreadCountsFeed = repository.getUnreadCountsByFeed().map { list -> list.associate { it.feedId to it.count } }
    private val _unreadCountsCategory = repository.getUnreadCountsByCategory().map { list -> list.associate { it.categoryId to it.count } }

    private val _entries = combine(
        _statusFilter,
        _selectedCategoryId,
        _selectedFeedId,
        _searchQuery
    ) { status, catId, feedId, query ->
        QueryFilterParams(status, catId, feedId, query)
    }.flatMapLatest { params ->
        if (params.query.isNotBlank()) {
            repository.searchEntries(params.query)
        } else {
            repository.getEntries(params.statusFilter, params.categoryId, params.feedId)
        }
    }

    private val _selectedCategory = combine(_categories, _selectedCategoryId) { categories, id ->
        categories.find { it.id == id }
    }

    private val _selectedFeed = combine(_feeds, _selectedFeedId) { feeds, id ->
        feeds.find { it.id == id }
    }

    private val _selectedEntry = _selectedEntryId.flatMapLatest { id ->
        if (id != null) repository.getEntryById(id) else flowOf(null)
    }

    private val _feedTreeData = combine(
        _categories,
        _feeds,
        _unreadCountsFeed,
        _unreadCountsCategory
    ) { categories, feeds, feedCounts, catCounts ->
        FeedTreeData(categories, feeds, feedCounts, catCounts)
    }

    private val _readerPreferences = combine(
        _readerTheme,
        _fontSizeScale,
        _showOnlyUnreadFeeds
    ) { theme, fontScale, showOnlyUnread ->
        ReaderPreferences(theme, fontScale, showOnlyUnread)
    }

    private val _errorState = combine(
        _errorMessage,
        _currentError
    ) { message, currentError ->
        ErrorState(message, currentError)
    }

    private val _subState1 = combine(
        _isLoading,
        _entries,
        _feedTreeData
    ) { loading, entries, tree ->
        PartialState1(
            loading = loading,
            categories = tree.categories,
            feeds = tree.feeds,
            entries = entries,
            feedCounts = tree.feedCounts,
            catCounts = tree.catCounts
        )
    }

    private val _subState2 = combine(
        _selectedCategory,
        _selectedFeed,
        _selectedEntry,
        _statusFilter
    ) { category, feed, entry, status ->
        PartialState2(category, feed, entry, status)
    }

    private val _subState3 = combine(
        _searchQuery,
        _readerPreferences,
        _errorState
    ) { query, prefs, err ->
        PartialState3(
            query = query,
            theme = prefs.theme,
            fontScale = prefs.fontScale,
            showOnlyUnread = prefs.showOnlyUnread,
            error = err.message,
            currentErr = err.currentError
        )
    }

    val uiState: StateFlow<MinifluxUiState> = combine(
        _subState1,
        _subState2,
        _subState3
    ) { s1, s2, s3 ->
        MinifluxUiState(
            isLoading = s1.loading,
            categories = s1.categories,
            feeds = s1.feeds,
            entries = s1.entries,
            selectedCategory = s2.category,
            selectedFeed = s2.feed,
            selectedEntry = s2.entry,
            statusFilter = s2.status,
            searchQuery = s3.query,
            errorMessage = s3.error,
            currentError = s3.currentErr,
            fontSizeScale = s3.fontScale,
            readerTheme = s3.theme,
            showOnlyUnreadFeeds = s3.showOnlyUnread,
            unreadCountsByFeed = s1.feedCounts,
            unreadCountsByCategory = s1.catCounts
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MinifluxUiState(isLoading = true)
    )

    init {
        refreshAll()
    }

    fun setShowOnlyUnreadFeeds(showOnlyUnreadFeeds: Boolean) {
        _showOnlyUnreadFeeds.value = showOnlyUnreadFeeds
        viewModelScope.launch {
            configRepository?.saveShowOnlyUnreadFeeds(showOnlyUnreadFeeds)
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _currentError.value = null
            try {
                repository.flushPendingSyncs()
                repository.syncCategoriesAndFeeds()
                repository.syncEntries("starred")
                if (_statusFilter.value != "starred") {
                    repository.syncEntries(_statusFilter.value)
                }
            } catch (e: Exception) {
                handleException(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun handleException(e: Throwable) {
        val classified = net.veskuh.lyhty.util.LyhtyErrorClassifier.classify(e)
        _currentError.value = classified
        _errorMessage.value = classified.displayMessage
    }

    private var activeReadingList: List<EntryEntity> = emptyList()

    fun selectCategory(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
        _selectedFeedId.value = null
        _selectedEntryId.value = null
        activeReadingList = emptyList()
    }

    fun selectCategory(category: CategoryEntity?) {
        selectCategory(category?.id)
    }

    fun selectFeed(feedId: Long?) {
        _selectedFeedId.value = feedId
        _selectedEntryId.value = null
        activeReadingList = emptyList()
        if (feedId != null) {
            val feed = uiState.value.feeds.find { it.id == feedId }
            if (feed?.categoryId != null) {
                _selectedCategoryId.value = feed.categoryId
            }
        }
    }

    fun selectFeed(feed: FeedEntity?) {
        selectFeed(feed?.id)
    }

    fun selectEntry(entryId: Long?) {
        _selectedEntryId.value = entryId
        if (entryId == null) {
            activeReadingList = uiState.value.entries
        } else {
            if (activeReadingList.isEmpty() || activeReadingList.none { it.id == entryId }) {
                activeReadingList = uiState.value.entries
            }
        }
    }

    fun selectNextEntry(): Boolean {
        val navList = if (activeReadingList.isNotEmpty()) activeReadingList else uiState.value.entries
        val currentId = _selectedEntryId.value ?: return false
        val index = navList.indexOfFirst { it.id == currentId }
        if (index != -1 && index + 1 < navList.size) {
            selectEntry(navList[index + 1].id)
            return true
        }
        return false
    }

    fun selectPreviousEntry(): Boolean {
        val navList = if (activeReadingList.isNotEmpty()) activeReadingList else uiState.value.entries
        val currentId = _selectedEntryId.value ?: return false
        val index = navList.indexOfFirst { it.id == currentId }
        if (index > 0) {
            selectEntry(navList[index - 1].id)
            return true
        }
        return false
    }

    fun advanceToNextUnreadFeed(): String? {
        val state = uiState.value
        if (state.feeds.isEmpty()) return null

        // Follow the exact visual sidebar order: feeds grouped by category, followed by uncategorized
        val orderedFeeds = buildList {
            for (category in state.categories) {
                val childFeeds = state.feeds.filter { it.categoryId == category.id }
                addAll(childFeeds)
            }
            val uncategorized = state.feeds.filter { feed -> state.categories.none { it.id == feed.categoryId } }
            addAll(uncategorized)
        }

        val currentFeedId = state.selectedFeed?.id ?: state.selectedEntry?.feedId
        val currentFeedIdx = if (currentFeedId != null) orderedFeeds.indexOfFirst { it.id == currentFeedId } else -1

        // Search sequentially starting after current feed
        val candidateFeeds = if (currentFeedIdx != -1) {
            orderedFeeds.drop(currentFeedIdx + 1) + orderedFeeds.take(currentFeedIdx)
        } else {
            orderedFeeds
        }

        val nextFeed = candidateFeeds.firstOrNull { feed ->
            (state.unreadCountsByFeed[feed.id] ?: 0) > 0
        }

        return if (nextFeed != null) {
            selectFeed(nextFeed.id)
            nextFeed.title
        } else {
            null
        }
    }

    fun setStatusFilter(filter: String?) {
        _statusFilter.value = filter
        activeReadingList = emptyList()
        if (filter == "starred") {
            viewModelScope.launch {
                try {
                    repository.syncEntries("starred")
                } catch (e: Exception) {
                    handleException(e)
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        activeReadingList = emptyList()
    }

    fun markAsRead(entryId: Long) {
        viewModelScope.launch {
            repository.markEntryAsRead(entryId)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            val state = uiState.value
            when {
                state.selectedFeed != null -> repository.markFeedAsRead(state.selectedFeed.id)
                state.selectedCategory != null -> repository.markCategoryAsRead(state.selectedCategory.id)
                else -> repository.markAllAsRead()
            }
        }
    }

    fun markFeedAsRead(feedId: Long) {
        viewModelScope.launch {
            repository.markFeedAsRead(feedId)
        }
    }

    fun markCategoryAsRead(categoryId: Long) {
        viewModelScope.launch {
            repository.markCategoryAsRead(categoryId)
        }
    }

    fun markAsUnread(entryId: Long) {
        viewModelScope.launch {
            repository.markEntryAsUnread(entryId)
        }
    }

    fun toggleBookmark(entryId: Long) {
        viewModelScope.launch {
            repository.toggleBookmark(entryId)
        }
    }

    fun fetchOriginalContent(entryId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.fetchServerFullText(entryId)
            } catch (e: Exception) {
                handleException(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
        _currentError.value = null
    }

    fun setReaderTheme(theme: ReaderTheme) {
        _readerTheme.value = theme
        viewModelScope.launch {
            configRepository?.saveReaderTheme(theme)
        }
    }

    fun setFontSizeScale(scale: Float) {
        _fontSizeScale.value = scale
        viewModelScope.launch {
            configRepository?.saveFontSizeScale(scale)
        }
    }

    private data class QueryFilterParams(
        val statusFilter: String?,
        val categoryId: Long?,
        val feedId: Long?,
        val query: String
    )

    private data class FeedTreeData(
        val categories: List<CategoryEntity>,
        val feeds: List<FeedEntity>,
        val feedCounts: Map<Long, Int>,
        val catCounts: Map<Long, Int>
    )

    private data class ReaderPreferences(
        val theme: ReaderTheme,
        val fontScale: Float,
        val showOnlyUnread: Boolean
    )

    private data class ErrorState(
        val message: String?,
        val currentError: net.veskuh.lyhty.util.LyhtyError?
    )

    private data class PartialState1(
        val loading: Boolean,
        val categories: List<CategoryEntity>,
        val feeds: List<FeedEntity>,
        val entries: List<EntryEntity>,
        val feedCounts: Map<Long, Int>,
        val catCounts: Map<Long, Int>
    )

    private data class PartialState2(
        val category: CategoryEntity?,
        val feed: FeedEntity?,
        val entry: EntryEntity?,
        val status: String?
    )

    private data class PartialState3(
        val query: String,
        val theme: ReaderTheme,
        val fontScale: Float,
        val showOnlyUnread: Boolean,
        val error: String?,
        val currentErr: net.veskuh.lyhty.util.LyhtyError? = null
    )
}
