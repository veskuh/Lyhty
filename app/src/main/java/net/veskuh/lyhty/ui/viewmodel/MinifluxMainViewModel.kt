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
            isOnline.collect { online ->
                if (online) {
                    net.veskuh.lyhty.util.LyhtyLogger.info("ViewModel", "Network restored (isOnline=true). Flushing pending syncs...")
                    try {
                        repository.flushPendingSyncs()
                    } catch (_: Exception) {}
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
                repository.syncEntries(_statusFilter.value)
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
    private val _readerTheme = MutableStateFlow(ReaderTheme.OLED_DARK)
    private val _fontSizeScale = MutableStateFlow(1.0f)
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

    private val _subState1 = combine(
        _isLoading,
        _categories,
        _feeds,
        _entries,
        _unreadCountsFeed,
        _unreadCountsCategory
    ) { flows: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        PartialState1(
            loading = flows[0] as Boolean,
            categories = flows[1] as List<CategoryEntity>,
            feeds = flows[2] as List<FeedEntity>,
            entries = flows[3] as List<EntryEntity>,
            feedCounts = flows[4] as Map<Long, Int>,
            catCounts = flows[5] as Map<Long, Int>
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
        _readerTheme,
        _fontSizeScale,
        _errorMessage,
        _currentError
    ) { flows: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        PartialState3(
            query = flows[0] as String,
            theme = flows[1] as ReaderTheme,
            fontScale = flows[2] as Float,
            error = flows[3] as String?,
            currentErr = flows[4] as net.veskuh.lyhty.util.LyhtyError?
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

    fun refreshAll() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _currentError.value = null
            try {
                repository.syncCategoriesAndFeeds()
                repository.syncEntries(_statusFilter.value)
                repository.flushPendingSyncs()
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

    fun selectCategory(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
        _selectedFeedId.value = null
    }

    fun selectCategory(category: CategoryEntity?) {
        selectCategory(category?.id)
    }

    fun selectFeed(feedId: Long?) {
        _selectedFeedId.value = feedId
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
    }

    fun selectNextEntry() {
        val currentEntries = uiState.value.entries
        val currentId = _selectedEntryId.value ?: return
        val index = currentEntries.indexOfFirst { it.id == currentId }
        if (index != -1 && index + 1 < currentEntries.size) {
            selectEntry(currentEntries[index + 1].id)
        }
    }

    fun selectPreviousEntry() {
        val currentEntries = uiState.value.entries
        val currentId = _selectedEntryId.value ?: return
        val index = currentEntries.indexOfFirst { it.id == currentId }
        if (index > 0) {
            selectEntry(currentEntries[index - 1].id)
        }
    }

    fun setStatusFilter(filter: String?) {
        _statusFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun markAsRead(entryId: Long) {
        viewModelScope.launch {
            repository.markEntryAsRead(entryId)
        }
    }

    fun markAsUnread(entryId: Long) {
        viewModelScope.launch {
            repository.markEntryAsUnread(entryId)
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
    }

    fun setReaderTheme(theme: ReaderTheme) {
        _readerTheme.value = theme
    }

    fun setFontSizeScale(scale: Float) {
        _fontSizeScale.value = scale
    }

    private data class QueryFilterParams(
        val statusFilter: String?,
        val categoryId: Long?,
        val feedId: Long?,
        val query: String
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
        val error: String?,
        val currentErr: net.veskuh.lyhty.util.LyhtyError? = null
    )
}
