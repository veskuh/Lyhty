package net.veskuh.lyhty.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.FoldingFeature
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowWidthSizeClass
import net.veskuh.lyhty.ui.components.rememberPostureInfo
import net.veskuh.lyhty.ui.theme.LyhtyTheme
import net.veskuh.lyhty.ui.viewmodel.MinifluxMainViewModel
import net.veskuh.lyhty.util.LogLevel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun LyhtyAdaptiveApp(
    foldingFeature: FoldingFeature? = null,
    viewModel: MinifluxMainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val postureInfo = rememberPostureInfo(foldingFeature = foldingFeature)
    val navigator = rememberListDetailPaneScaffoldNavigator<Long>()

    var isSettingsOpen by rememberSaveable { mutableStateOf(false) }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    LifecycleEventEffect(
        event = Lifecycle.Event.ON_STOP,
        lifecycleOwner = lifecycleOwner
    ) {
        viewModel.onAppBackgrounded()
    }
    LifecycleEventEffect(
        event = Lifecycle.Event.ON_START,
        lifecycleOwner = lifecycleOwner
    ) {
        viewModel.onAppForegrounded()
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    // Single-Pane / Settings / Search Back Gesture Handler
    BackHandler(enabled = isSettingsOpen || isSearchActive || uiState.selectedEntry != null || navigator.canNavigateBack()) {
        if (isSettingsOpen) {
            isSettingsOpen = false
        } else if (isSearchActive) {
            isSearchActive = false
            viewModel.setSearchQuery("")
        } else if (uiState.selectedEntry != null) {
            viewModel.selectEntry(null)
        } else if (navigator.canNavigateBack()) {
            navigator.navigateBack()
        }
    }

    LyhtyTheme(readerTheme = uiState.readerTheme) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            if (!isOnline) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚡ Working Offline — Actions queued. Will sync when network returns.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            uiState.currentError?.let { error ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.error,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = error.code,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = error.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "👉 ${error.actionableHint}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { isSettingsOpen = true }) {
                                Text("⚙️ Settings")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }

            val windowAdaptiveInfo = currentWindowAdaptiveInfo()
            val isExpandedWindow = windowAdaptiveInfo.windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT

            if (isExpandedWindow) {
                // Unfolded Dual-Pane Layout: Left = Categories Tree (pinned), Right = Articles / Reader / Settings
                Row(modifier = Modifier.fillMaxSize()) {
                    CategoryFeedTreePane(
                        categories = uiState.categories,
                        feeds = uiState.feeds,
                        selectedCategory = uiState.selectedCategory,
                        selectedFeed = uiState.selectedFeed,
                        selectedEntry = uiState.selectedEntry,
                        unreadCountsByCategory = uiState.unreadCountsByCategory,
                        unreadCountsByFeed = uiState.unreadCountsByFeed,
                        starredCount = uiState.starredCount,
                        statusFilter = uiState.statusFilter,
                        showOnlyUnreadFeeds = uiState.showOnlyUnreadFeeds,
                        readerTheme = uiState.readerTheme,
                        isLoading = uiState.isLoading,
                        onSelectCategory = { category ->
                            isSettingsOpen = false
                            isSearchActive = false
                            viewModel.setSearchQuery("")
                            viewModel.selectCategory(category)
                        },
                        onSelectFeed = { feed ->
                            isSettingsOpen = false
                            isSearchActive = false
                            viewModel.setSearchQuery("")
                            viewModel.selectFeed(feed)
                        },
                        onSelectAllUnread = {
                            isSettingsOpen = false
                            isSearchActive = false
                            viewModel.setSearchQuery("")
                            viewModel.selectAllUnread()
                        },
                        onSelectBookmarks = {
                            isSettingsOpen = false
                            isSearchActive = false
                            viewModel.setSearchQuery("")
                            viewModel.selectBookmarks()
                        },
                        onOpenSearch = {
                            isSettingsOpen = false
                            viewModel.selectEntry(null)
                            isSearchActive = true
                        },
                        onMarkCategoryAsRead = { categoryId -> viewModel.markCategoryAsRead(categoryId) },
                        onMarkFeedAsRead = { feedId -> viewModel.markFeedAsRead(feedId) },
                        onMarkAllAsRead = { viewModel.markAllAsRead() },
                        onSync = { viewModel.refreshAll() },
                        onOpenSettings = { isSettingsOpen = true },
                        modifier = Modifier.weight(0.75f)
                    )

                    Box(modifier = Modifier.weight(1.45f).fillMaxSize()) {
                        val selectedEntry = uiState.selectedEntry
                        if (isSettingsOpen) {
                            BoundSettingsPane(
                                viewModel = viewModel,
                                uiState = uiState,
                                onClose = { isSettingsOpen = false }
                            )
                        } else if (selectedEntry != null) {
                            BoundEntryReaderPane(
                                entry = selectedEntry,
                                viewModel = viewModel,
                                uiState = uiState,
                                postureInfo = postureInfo,
                                onBack = { viewModel.selectEntry(null) }
                            )
                        } else {
                            EntryListPane(
                                entries = uiState.entries,
                                selectedEntry = uiState.selectedEntry,
                                searchQuery = uiState.searchQuery,
                                isSearchActive = isSearchActive,
                                onSelectEntry = { entry ->
                                    viewModel.selectEntry(entry.id)
                                },
                                onSearchQueryChange = { query -> viewModel.setSearchQuery(query) },
                                onCloseSearch = {
                                    isSearchActive = false
                                    viewModel.setSearchQuery("")
                                }
                            )
                        }
                    }
                }
            } else {
                // Folded Single-Pane Cover Screen Layout
                if (isSettingsOpen) {
                    BoundSettingsPane(
                        viewModel = viewModel,
                        uiState = uiState,
                        onClose = { isSettingsOpen = false }
                    )
                } else {
                    ListDetailPaneScaffold(
                        modifier = Modifier.weight(1f),
                        directive = navigator.scaffoldDirective,
                        value = navigator.scaffoldValue,
                        listPane = {
                            AnimatedPane {
                                CategoryFeedTreePane(
                                    categories = uiState.categories,
                                    feeds = uiState.feeds,
                                    selectedCategory = uiState.selectedCategory,
                                    selectedFeed = uiState.selectedFeed,
                                    selectedEntry = uiState.selectedEntry,
                                    unreadCountsByCategory = uiState.unreadCountsByCategory,
                                    unreadCountsByFeed = uiState.unreadCountsByFeed,
                                    starredCount = uiState.starredCount,
                                    statusFilter = uiState.statusFilter,
                                    showOnlyUnreadFeeds = uiState.showOnlyUnreadFeeds,
                                    readerTheme = uiState.readerTheme,
                                    isLoading = uiState.isLoading,
                                    onSelectCategory = { category ->
                                        isSearchActive = false
                                        viewModel.setSearchQuery("")
                                        viewModel.selectCategory(category)
                                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
                                    },
                                    onSelectFeed = { feed ->
                                        isSearchActive = false
                                        viewModel.setSearchQuery("")
                                        viewModel.selectFeed(feed)
                                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
                                    },
                                    onSelectAllUnread = {
                                        isSearchActive = false
                                        viewModel.setSearchQuery("")
                                        viewModel.selectAllUnread()
                                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
                                    },
                                    onSelectBookmarks = {
                                        isSearchActive = false
                                        viewModel.setSearchQuery("")
                                        viewModel.selectBookmarks()
                                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
                                    },
                                    onOpenSearch = {
                                        viewModel.selectEntry(null)
                                        isSearchActive = true
                                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
                                    },
                                    onMarkCategoryAsRead = { categoryId -> viewModel.markCategoryAsRead(categoryId) },
                                    onMarkFeedAsRead = { feedId -> viewModel.markFeedAsRead(feedId) },
                                    onMarkAllAsRead = { viewModel.markAllAsRead() },
                                    onSync = { viewModel.refreshAll() },
                                    onOpenSettings = { isSettingsOpen = true }
                                )
                            }
                        },
                        detailPane = {
                            AnimatedPane {
                                val selectedEntry = uiState.selectedEntry
                                if (selectedEntry != null) {
                                    BoundEntryReaderPane(
                                        entry = selectedEntry,
                                        viewModel = viewModel,
                                        uiState = uiState,
                                        postureInfo = postureInfo,
                                        onBack = { viewModel.selectEntry(null) }
                                    )
                                } else {
                                    EntryListPane(
                                        entries = uiState.entries,
                                        selectedEntry = uiState.selectedEntry,
                                        searchQuery = uiState.searchQuery,
                                        isSearchActive = isSearchActive,
                                        onSelectEntry = { entry ->
                                            viewModel.selectEntry(entry.id)
                                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, entry.id)
                                        },
                                        onSearchQueryChange = { query -> viewModel.setSearchQuery(query) },
                                        onCloseSearch = {
                                            isSearchActive = false
                                            viewModel.setSearchQuery("")
                                        },
                                        onBack = {
                                            if (isSearchActive) {
                                                isSearchActive = false
                                                viewModel.setSearchQuery("")
                                            } else if (navigator.canNavigateBack()) {
                                                navigator.navigateBack()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BoundSettingsPane(
    viewModel: MinifluxMainViewModel,
    uiState: net.veskuh.lyhty.ui.state.MinifluxUiState,
    onClose: () -> Unit
) {
    SettingsPane(
        initialServerUrl = viewModel.getServerUrl(),
        initialApiKey = viewModel.getApiKey(),
        currentLogLevel = viewModel.getLogLevel(),
        fontSizeScale = uiState.fontSizeScale,
        readerTheme = uiState.readerTheme,
        showOnlyUnreadFeeds = uiState.showOnlyUnreadFeeds,
        isLoading = uiState.isLoading,
        hasError = uiState.currentError != null,
        onSaveConfig = { url, key -> viewModel.updateServerConfig(url, key) },
        onSaveLogLevel = { level -> viewModel.setLogLevel(level) },
        onSetTheme = { theme -> viewModel.setReaderTheme(theme) },
        onSetFontSizeScale = { scale -> viewModel.setFontSizeScale(scale) },
        onSetShowOnlyUnreadFeeds = { showUnread -> viewModel.setShowOnlyUnreadFeeds(showUnread) },
        onBack = onClose
    )
}

@Composable
private fun BoundEntryReaderPane(
    entry: net.veskuh.lyhty.data.local.entity.EntryEntity,
    viewModel: MinifluxMainViewModel,
    uiState: net.veskuh.lyhty.ui.state.MinifluxUiState,
    postureInfo: net.veskuh.lyhty.ui.components.PostureInfo,
    onBack: () -> Unit
) {
    EntryReaderPane(
        entry = entry,
        postureInfo = postureInfo,
        fontSizeScale = uiState.fontSizeScale,
        readerTheme = uiState.readerTheme,
        onFetchFullText = { id -> viewModel.fetchOriginalContent(id) },
        onMarkRead = { id -> viewModel.markAsRead(id) },
        onMarkUnread = { id -> viewModel.markAsUnread(id) },
        onMarkAllRead = { viewModel.markAllAsRead() },
        onToggleBookmark = { id -> viewModel.toggleBookmark(id) },
        onNextEntry = { viewModel.selectNextEntry() },
        onPreviousEntry = { viewModel.selectPreviousEntry() },
        onAdvanceToNextFeed = { viewModel.advanceToNextUnreadFeed() },
        onBack = onBack
    )
}
