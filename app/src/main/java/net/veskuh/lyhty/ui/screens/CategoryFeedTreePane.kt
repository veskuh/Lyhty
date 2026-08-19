package net.veskuh.lyhty.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.veskuh.lyhty.data.local.entity.CategoryEntity
import net.veskuh.lyhty.data.local.entity.FeedEntity

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.runtime.remember

import androidx.compose.material.icons.filled.FolderOpen

import net.veskuh.lyhty.data.local.entity.EntryEntity

import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
fun CategoryFeedTreePane(
    categories: List<CategoryEntity>,
    feeds: List<FeedEntity>,
    selectedCategory: CategoryEntity?,
    selectedFeed: FeedEntity?,
    selectedEntry: EntryEntity? = null,
    unreadCountsByCategory: Map<Long, Int> = emptyMap(),
    unreadCountsByFeed: Map<Long, Int> = emptyMap(),
    showOnlyUnreadFeeds: Boolean = true,
    onSelectCategory: (CategoryEntity?) -> Unit,
    onSelectFeed: (FeedEntity?) -> Unit,
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

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Header Image (header.png)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(net.veskuh.lyhty.R.drawable.header),
                    contentDescription = "Lyhty",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Content Area (Feeds & Categories Tree)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (visibleCategories.isEmpty() && uncategorizedFeeds.isEmpty()) {
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
                                text = if (categories.isEmpty() && feeds.isEmpty()) "📡 No Feeds or Categories" else "✨ All Catch Up!",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (categories.isEmpty() && feeds.isEmpty()) {
                                    "Configure your Miniflux server URL and API key in Settings to sync your RSS feeds."
                                } else {
                                    "No unread items in any feed. You can toggle 'Hide feeds with no unread items' in Settings to view all feeds."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { onOpenSettings?.invoke() }) {
                                Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (categories.isEmpty() && feeds.isEmpty()) "Configure Server" else "Settings")
                            }
                        }
                    }
                } else {
                    // Categories & Feeds Tree List
                    val totalUnreadCount = unreadCountsByFeed.values.sum()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // "All Unread" Entry Shortcut
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSelectCategory(null)
                                        onSelectFeed(null)
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedCategory == null && selectedFeed == null) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.RssFeed, contentDescription = null)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "All Unread Feeds",
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (totalUnreadCount > 0) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Badge {
                                            Text(text = "$totalUnreadCount")
                                        }
                                    }
                                }
                            }
                        }

                        // Categories & Nested Feeds Tree
                        items(visibleCategories, key = { it.id }) { category ->
                            val isCatSelected = selectedCategory?.id == category.id && selectedFeed == null
                            val childFeeds = visibleFeeds.filter { it.categoryId == category.id }
                            val dbCatCount = unreadCountsByCategory[category.id] ?: 0
                            val childFeedsCount = childFeeds.sumOf { unreadCountsByFeed[it.id] ?: 0 }
                            val catUnreadCount = maxOf(dbCatCount, childFeedsCount)

                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Category Header Card
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onSelectCategory(category)
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCatSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        }
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Folder, contentDescription = null)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = category.title,
                                                style = MaterialTheme.typography.bodyLarge,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (catUnreadCount > 0) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Badge {
                                                Text(text = "$catUnreadCount")
                                            }
                                        }
                                    }
                                }

                                // Child Feeds List under Category
                                childFeeds.forEach { feed ->
                                    val isFeedSelected = selectedFeed?.id == feed.id
                                    val feedUnreadCount = unreadCountsByFeed[feed.id] ?: 0

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 24.dp, top = 4.dp)
                                            .clickable {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onSelectFeed(feed)
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isFeedSelected) {
                                                MaterialTheme.colorScheme.secondaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerLow
                                            }
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.RssFeed, contentDescription = null)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = feed.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (feedUnreadCount > 0) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Badge {
                                                    Text(text = "$feedUnreadCount")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Uncategorized Feeds Section
                        if (uncategorizedFeeds.isNotEmpty()) {
                            item {
                                val uncategorizedUnreadCount = uncategorizedFeeds.sumOf { unreadCountsByFeed[it.id] ?: 0 }
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Uncategorized Header Card
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onSelectCategory(null)
                                                onSelectFeed(null)
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.FolderOpen, contentDescription = null)
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = "Uncategorized",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (uncategorizedUnreadCount > 0) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Badge {
                                                    Text(text = "$uncategorizedUnreadCount")
                                                }
                                            }
                                        }
                                    }

                                    // Child Feeds under Uncategorized
                                    uncategorizedFeeds.forEach { feed ->
                                        val isFeedSelected = selectedFeed?.id == feed.id
                                        val feedUnreadCount = unreadCountsByFeed[feed.id] ?: 0

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 24.dp, top = 4.dp)
                                                .clickable { onSelectFeed(feed) },
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isFeedSelected) {
                                                    MaterialTheme.colorScheme.secondaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceContainerLow
                                                }
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.RssFeed, contentDescription = null)
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        text = feed.title,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                if (feedUnreadCount > 0) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Badge {
                                                        Text(text = "$feedUnreadCount")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Bottom Toolbar with Refresh & Settings Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSync) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Refresh Feeds"
                    )
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
