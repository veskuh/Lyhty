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

    var isSettingsOpen by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    // Single-Pane / Settings Back Gesture Handler
    BackHandler(enabled = isSettingsOpen || uiState.selectedEntry != null || navigator.canNavigateBack()) {
        if (isSettingsOpen) {
            isSettingsOpen = false
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
                        unreadCountsByCategory = uiState.unreadCountsByCategory,
                        unreadCountsByFeed = uiState.unreadCountsByFeed,
                        onSelectCategory = { category ->
                            isSettingsOpen = false
                            viewModel.selectCategory(category)
                        },
                        onSelectFeed = { feed ->
                            isSettingsOpen = false
                            viewModel.selectFeed(feed)
                        },
                        onSync = { viewModel.refreshAll() },
                        onOpenSettings = { isSettingsOpen = true },
                        modifier = Modifier.weight(0.75f)
                    )

                    Box(modifier = Modifier.weight(1.45f).fillMaxSize()) {
                        if (isSettingsOpen) {
                            SettingsPane(
                                initialServerUrl = viewModel.getServerUrl(),
                                initialApiKey = viewModel.getApiKey(),
                                currentLogLevel = viewModel.getLogLevel(),
                                fontSizeScale = uiState.fontSizeScale,
                                readerTheme = uiState.readerTheme,
                                onSaveConfig = { url, key -> viewModel.updateServerConfig(url, key) },
                                onSaveLogLevel = { level -> viewModel.setLogLevel(level) },
                                onSetTheme = { theme -> viewModel.setReaderTheme(theme) },
                                onSetFontSizeScale = { scale -> viewModel.setFontSizeScale(scale) },
                                onBack = { isSettingsOpen = false }
                            )
                        } else if (uiState.selectedEntry != null) {
                            EntryReaderPane(
                                entry = uiState.selectedEntry,
                                postureInfo = postureInfo,
                                fontSizeScale = uiState.fontSizeScale,
                                readerTheme = uiState.readerTheme,
                                onFetchFullText = { id -> viewModel.fetchOriginalContent(id) },
                                onMarkRead = { id -> viewModel.markAsRead(id) },
                                onMarkUnread = { id -> viewModel.markAsUnread(id) },
                                onNextEntry = { viewModel.selectNextEntry() },
                                onPreviousEntry = { viewModel.selectPreviousEntry() },
                                onSetTheme = { theme -> viewModel.setReaderTheme(theme) },
                                onSetFontSizeScale = { scale -> viewModel.setFontSizeScale(scale) },
                                onBack = { viewModel.selectEntry(null) }
                            )
                        } else {
                            EntryListPane(
                                entries = uiState.entries,
                                selectedEntry = uiState.selectedEntry,
                                statusFilter = uiState.statusFilter,
                                searchQuery = uiState.searchQuery,
                                onSelectEntry = { entry ->
                                    viewModel.selectEntry(entry.id)
                                },
                                onSetStatusFilter = { filter -> viewModel.setStatusFilter(filter) },
                                onSearchQueryChange = { query -> viewModel.setSearchQuery(query) }
                            )
                        }
                    }
                }
            } else {
                // Folded Single-Pane Cover Screen Layout
                if (isSettingsOpen) {
                    SettingsPane(
                        initialServerUrl = viewModel.getServerUrl(),
                        initialApiKey = viewModel.getApiKey(),
                        currentLogLevel = viewModel.getLogLevel(),
                        fontSizeScale = uiState.fontSizeScale,
                        readerTheme = uiState.readerTheme,
                        onSaveConfig = { url, key -> viewModel.updateServerConfig(url, key) },
                        onSaveLogLevel = { level -> viewModel.setLogLevel(level) },
                        onSetTheme = { theme -> viewModel.setReaderTheme(theme) },
                        onSetFontSizeScale = { scale -> viewModel.setFontSizeScale(scale) },
                        onBack = { isSettingsOpen = false }
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
                                    unreadCountsByCategory = uiState.unreadCountsByCategory,
                                    unreadCountsByFeed = uiState.unreadCountsByFeed,
                                    onSelectCategory = { category ->
                                        viewModel.selectCategory(category)
                                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
                                    },
                                    onSelectFeed = { feed ->
                                        viewModel.selectFeed(feed)
                                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
                                    },
                                    onSync = { viewModel.refreshAll() },
                                    onOpenSettings = { isSettingsOpen = true }
                                )
                            }
                        },
                        detailPane = {
                            AnimatedPane {
                                if (uiState.selectedEntry != null) {
                                    EntryReaderPane(
                                        entry = uiState.selectedEntry,
                                        postureInfo = postureInfo,
                                        fontSizeScale = uiState.fontSizeScale,
                                        readerTheme = uiState.readerTheme,
                                        onFetchFullText = { id -> viewModel.fetchOriginalContent(id) },
                                        onMarkRead = { id -> viewModel.markAsRead(id) },
                                        onMarkUnread = { id -> viewModel.markAsUnread(id) },
                                        onNextEntry = { viewModel.selectNextEntry() },
                                        onPreviousEntry = { viewModel.selectPreviousEntry() },
                                        onSetTheme = { theme -> viewModel.setReaderTheme(theme) },
                                        onSetFontSizeScale = { scale -> viewModel.setFontSizeScale(scale) },
                                        onBack = { viewModel.selectEntry(null) }
                                    )
                                } else {
                                    EntryListPane(
                                        entries = uiState.entries,
                                        selectedEntry = uiState.selectedEntry,
                                        statusFilter = uiState.statusFilter,
                                        searchQuery = uiState.searchQuery,
                                        onSelectEntry = { entry ->
                                            viewModel.selectEntry(entry.id)
                                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, entry.id)
                                        },
                                        onSetStatusFilter = { filter -> viewModel.setStatusFilter(filter) },
                                        onSearchQueryChange = { query -> viewModel.setSearchQuery(query) },
                                        onBack = {
                                            if (navigator.canNavigateBack()) {
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
