package net.veskuh.lyhty.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import net.veskuh.lyhty.data.local.entity.CategoryEntity
import net.veskuh.lyhty.data.local.entity.EntryEntity
import net.veskuh.lyhty.data.local.entity.FeedEntity
import net.veskuh.lyhty.ui.state.ReaderTheme

@Composable
fun CategoryFeedTreePane(
    categories: List<CategoryEntity>,
    feeds: List<FeedEntity>,
    selectedCategory: CategoryEntity?,
    selectedFeed: FeedEntity?,
    selectedEntry: EntryEntity? = null,
    unreadCountsByCategory: Map<Long, Int> = emptyMap(),
    unreadCountsByFeed: Map<Long, Int> = emptyMap(),
    starredCount: Int = 0,
    statusFilter: String? = "unread",
    showOnlyUnreadFeeds: Boolean = true,
    readerTheme: ReaderTheme = ReaderTheme.OLED_DARK,
    isLoading: Boolean = false,
    onSelectCategory: (CategoryEntity?) -> Unit,
    onSelectFeed: (FeedEntity?) -> Unit,
    onSelectAllUnread: (() -> Unit)? = null,
    onSelectBookmarks: (() -> Unit)? = null,
    onSelectHistory: (() -> Unit)? = null,
    onOpenSearch: (() -> Unit)? = null,
    onMarkCategoryAsRead: ((Long) -> Unit)? = null,
    onMarkFeedAsRead: ((Long) -> Unit)? = null,
    onMarkAllAsRead: (() -> Unit)? = null,
    onSync: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val activeFeedId = selectedFeed?.id ?: selectedEntry?.feedId

    val visibleFeeds = remember(feeds, unreadCountsByFeed, showOnlyUnreadFeeds, activeFeedId) {
        if (!showOnlyUnreadFeeds) {
            feeds
        } else {
            feeds.filter { feed ->
                val unread = (unreadCountsByFeed[feed.id] ?: 0) > 0
                val isActive = feed.id == activeFeedId
                unread || isActive
            }
        }
    }

    val visibleCategories = remember(categories, feeds, visibleFeeds, unreadCountsByCategory, unreadCountsByFeed, showOnlyUnreadFeeds, selectedCategory) {
        if (!showOnlyUnreadFeeds) {
            categories
        } else {
            categories.filter { category ->
                val hasVisibleChildFeed = visibleFeeds.any { it.categoryId == category.id }
                val isSelectedCat = selectedCategory?.id == category.id
                val dbCatCount = unreadCountsByCategory[category.id] ?: 0
                hasVisibleChildFeed || isSelectedCat || dbCatCount > 0
            }
        }
    }

    val uncategorizedFeeds = remember(visibleFeeds, categories) {
        visibleFeeds.filter { feed -> categories.none { cat -> cat.id == feed.categoryId } }
    }

    val headerDrawable = when (readerTheme) {
        ReaderTheme.LIGHT -> net.veskuh.lyhty.R.drawable.header_white
        ReaderTheme.SEPIA -> net.veskuh.lyhty.R.drawable.header_cream
        ReaderTheme.OLED_DARK -> net.veskuh.lyhty.R.drawable.header
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Header Image (Theme-aware and Centered)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(headerDrawable),
                    contentDescription = "Lyhty",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Content Area (Feeds & Categories Tree)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (categories.isEmpty() && feeds.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "📡 No Feeds or Categories",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Configure your Miniflux server URL and API key in Settings to sync your RSS feeds.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { onOpenSettings?.invoke() }) {
                                Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Configure Server")
                            }
                        }
                    }
                } else {
                    val totalUnreadCount = unreadCountsByFeed.values.sum()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // "All Unread" Entry Shortcut
                        item {
                            var showAllMenu by remember { mutableStateOf(false) }
                            val isAllUnreadSelected = selectedCategory == null && selectedFeed == null && (statusFilter == "unread" || statusFilter == null)
                            FeedTreeCard(
                                title = "All Unread Feeds",
                                icon = Icons.Default.RssFeed,
                                unreadCount = totalUnreadCount,
                                isSelected = isAllUnreadSelected,
                                containerColor = if (isAllUnreadSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (onSelectAllUnread != null) {
                                        onSelectAllUnread()
                                    } else {
                                        onSelectCategory(null)
                                        onSelectFeed(null)
                                    }
                                },
                                onLongClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showAllMenu = true
                                },
                                menuExpanded = showAllMenu,
                                onDismissMenu = { showAllMenu = false },
                                menuActionText = "Mark all as read",
                                onMenuAction = { onMarkAllAsRead?.invoke() }
                            )
                        }

                        // "Bookmarks" Entry Shortcut
                        item {
                            val isBookmarksSelected = selectedCategory == null && selectedFeed == null && statusFilter == "starred"
                            FeedTreeCard(
                                title = "Bookmarks",
                                icon = Icons.Default.Star,
                                unreadCount = starredCount,
                                isSelected = isBookmarksSelected,
                                containerColor = if (isBookmarksSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSelectBookmarks?.invoke()
                                }
                            )
                        }

                        // "History" Entry Shortcut
                        item {
                            val isHistorySelected = selectedCategory == null && selectedFeed == null && statusFilter == "history"
                            FeedTreeCard(
                                title = "History",
                                icon = Icons.Default.History,
                                unreadCount = 0,
                                isSelected = isHistorySelected,
                                containerColor = if (isHistorySelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSelectHistory?.invoke()
                                }
                            )
                        }

                        // Categories & Nested Feeds Tree
                        items(visibleCategories, key = { it.id }) { category ->
                            val isCatSelected = selectedCategory?.id == category.id && selectedFeed == null
                            val childFeeds = visibleFeeds.filter { it.categoryId == category.id }
                            val dbCatCount = unreadCountsByCategory[category.id] ?: 0
                            val childFeedsCount = childFeeds.sumOf { unreadCountsByFeed[it.id] ?: 0 }
                            val catUnreadCount = maxOf(dbCatCount, childFeedsCount)
                            var showCatMenu by remember { mutableStateOf(false) }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                FeedTreeCard(
                                    title = category.title,
                                    icon = Icons.Default.Folder,
                                    unreadCount = catUnreadCount,
                                    isSelected = isCatSelected,
                                    containerColor = if (isCatSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    },
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSelectCategory(category)
                                    },
                                    onLongClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showCatMenu = true
                                    },
                                    menuExpanded = showCatMenu,
                                    onDismissMenu = { showCatMenu = false },
                                    menuActionText = "Mark category as read",
                                    onMenuAction = { onMarkCategoryAsRead?.invoke(category.id) }
                                )

                                // Child Feeds List under Category
                                childFeeds.forEach { feed ->
                                    val isFeedSelected = selectedFeed?.id == feed.id
                                    val feedUnreadCount = unreadCountsByFeed[feed.id] ?: 0
                                    var showFeedMenu by remember { mutableStateOf(false) }

                                    FeedTreeCard(
                                        title = feed.title,
                                        icon = Icons.Default.RssFeed,
                                        unreadCount = feedUnreadCount,
                                        isSelected = isFeedSelected,
                                        containerColor = if (isFeedSelected) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerLow
                                        },
                                        textStyle = MaterialTheme.typography.bodyMedium,
                                        padding = PaddingValues(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 24.dp, top = 4.dp),
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onSelectFeed(feed)
                                        },
                                        onLongClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showFeedMenu = true
                                        },
                                        menuExpanded = showFeedMenu,
                                        onDismissMenu = { showFeedMenu = false },
                                        menuActionText = "Mark feed as read",
                                        onMenuAction = { onMarkFeedAsRead?.invoke(feed.id) }
                                    )
                                }
                            }
                        }

                        // Uncategorized Feeds Section
                        if (uncategorizedFeeds.isNotEmpty()) {
                            item {
                                val uncategorizedUnreadCount = uncategorizedFeeds.sumOf { unreadCountsByFeed[it.id] ?: 0 }
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    FeedTreeCard(
                                        title = "Uncategorized",
                                        icon = Icons.Default.FolderOpen,
                                        unreadCount = uncategorizedUnreadCount,
                                        isSelected = false,
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        onClick = {
                                            onSelectCategory(null)
                                            onSelectFeed(null)
                                        }
                                    )

                                    // Child Feeds under Uncategorized
                                    uncategorizedFeeds.forEach { feed ->
                                        val isFeedSelected = selectedFeed?.id == feed.id
                                        val feedUnreadCount = unreadCountsByFeed[feed.id] ?: 0
                                        var showUncatFeedMenu by remember { mutableStateOf(false) }

                                        FeedTreeCard(
                                            title = feed.title,
                                            icon = Icons.Default.RssFeed,
                                            unreadCount = feedUnreadCount,
                                            isSelected = isFeedSelected,
                                            containerColor = if (isFeedSelected) {
                                                MaterialTheme.colorScheme.secondaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerLow
                                            },
                                            textStyle = MaterialTheme.typography.bodyMedium,
                                            padding = PaddingValues(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 24.dp, top = 4.dp),
                                            onClick = {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onSelectFeed(feed)
                                            },
                                            onLongClick = {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                showUncatFeedMenu = true
                                            },
                                            menuExpanded = showUncatFeedMenu,
                                            onDismissMenu = { showUncatFeedMenu = false },
                                            menuActionText = "Mark feed as read",
                                            onMenuAction = { onMarkFeedAsRead?.invoke(feed.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Bottom Toolbar with Sync, Search & Settings Buttons
            val syncRotation = remember { Animatable(0f) }
            LaunchedEffect(isLoading) {
                if (isLoading) {
                    while (true) {
                        syncRotation.snapTo(0f)
                        syncRotation.animateTo(
                            targetValue = 360f,
                            animationSpec = tween(durationMillis = 1400, easing = LinearEasing)
                        )
                    }
                } else {
                    val current = syncRotation.value
                    if (current in 1f..359f) {
                        val remaining = 360f - current
                        val duration = ((remaining / 360f) * 600).toInt().coerceAtLeast(150)
                        syncRotation.animateTo(
                            targetValue = 360f,
                            animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing)
                        )
                    }
                    syncRotation.snapTo(0f)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onSync,
                    enabled = !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = if (isLoading) "Refreshing Feeds..." else "Refresh Feeds",
                        modifier = if (isLoading) Modifier.graphicsLayer { rotationZ = syncRotation.value } else Modifier
                    )
                }

                if (onOpenSearch != null) {
                    IconButton(onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenSearch()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Articles"
                        )
                    }
                }

                if (onOpenSettings != null) {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeedTreeCard(
    title: String,
    icon: ImageVector,
    unreadCount: Int = 0,
    isSelected: Boolean = false,
    containerColor: Color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(12.dp),
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    menuExpanded: Boolean = false,
    onDismissMenu: () -> Unit = {},
    menuActionText: String? = null,
    onMenuAction: (() -> Unit)? = null
) {
    val haptics = LocalHapticFeedback.current

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            colors = CardDefaults.cardColors(containerColor = containerColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = textStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge {
                        Text(text = "$unreadCount")
                    }
                }
            }
        }

        if (menuActionText != null && onMenuAction != null) {
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = onDismissMenu
            ) {
                DropdownMenuItem(
                    text = { Text(menuActionText) },
                    leadingIcon = { Icon(Icons.Default.DoneAll, contentDescription = null) },
                    onClick = {
                        onDismissMenu()
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onMenuAction()
                    }
                )
            }
        }
    }
}
