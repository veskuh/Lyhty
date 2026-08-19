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

@Composable
fun CategoryFeedTreePane(
    categories: List<CategoryEntity>,
    feeds: List<FeedEntity>,
    selectedCategory: CategoryEntity?,
    selectedFeed: FeedEntity?,
    unreadCountsByCategory: Map<Long, Int> = emptyMap(),
    unreadCountsByFeed: Map<Long, Int> = emptyMap(),
    onSelectCategory: (CategoryEntity?) -> Unit,
    onSelectFeed: (FeedEntity?) -> Unit,
    onSync: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Header with Settings and Sync Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Feeds & Categories",
                    style = MaterialTheme.typography.titleLarge
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onOpenSettings != null) {
                        androidx.compose.material3.IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Server Settings"
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Button(onClick = onSync) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Sync")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                            Text("⚙️ Configure Server")
                        }
                    }
                }
            } else {
                // Categories & Feeds Tree List
                val totalUnreadCount = unreadCountsByFeed.values.sum()
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "All Unread" Entry Shortcut
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
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
                    items(categories, key = { it.id }) { category ->
                        val isCatSelected = selectedCategory?.id == category.id && selectedFeed == null
                        val childFeeds = feeds.filter { it.categoryId == category.id }
                        val dbCatCount = unreadCountsByCategory[category.id] ?: 0
                        val childFeedsCount = childFeeds.sumOf { unreadCountsByFeed[it.id] ?: 0 }
                        val catUnreadCount = maxOf(dbCatCount, childFeedsCount)

                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Category Header Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectCategory(category) },
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
}
