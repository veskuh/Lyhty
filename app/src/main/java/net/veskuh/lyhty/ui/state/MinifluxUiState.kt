package net.veskuh.lyhty.ui.state

import net.veskuh.lyhty.data.local.entity.CategoryEntity
import net.veskuh.lyhty.data.local.entity.EntryEntity
import net.veskuh.lyhty.data.local.entity.FeedEntity

enum class ReaderTheme {
    OLED_DARK, LIGHT, SEPIA
}

data class MinifluxUiState(
    val isLoading: Boolean = false,
    val categories: List<CategoryEntity> = emptyList(),
    val feeds: List<FeedEntity> = emptyList(),
    val entries: List<EntryEntity> = emptyList(),
    val selectedCategory: CategoryEntity? = null,
    val selectedFeed: FeedEntity? = null,
    val selectedEntry: EntryEntity? = null,
    val statusFilter: String? = "unread",
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val currentError: net.veskuh.lyhty.util.LyhtyError? = null,
    val fontSizeScale: Float = 1.0f,
    val readerTheme: ReaderTheme = ReaderTheme.OLED_DARK,
    val unreadCountsByFeed: Map<Long, Int> = emptyMap(),
    val unreadCountsByCategory: Map<Long, Int> = emptyMap()
)
