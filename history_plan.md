# Reading History Feature Plan — Lyhty

## 1. Overview & Requirements

The **Reading History** feature provides chronological tracking of articles opened and read locally on the device. It allows users to quickly rediscover articles they have read in the past, independent of original RSS publication dates or sync cycles.

### User Requirements
1. **Sidebar Navigation**:
   - Add a **"History"** shortcut card in the sidebar (`CategoryFeedTreePane`), positioned below *Bookmarks*.
   - **No count indicator/badge** is displayed on the sidebar card (clean visual style).
   - Selection state must be mutually exclusive with "All Unread Feeds" and "Bookmarks".
2. **Article List Pane**:
   - Displays recently read articles ordered chronologically by when they were opened/read (`readAt DESC`).
   - Pass `statusFilter` to `EntryListPane` to display appropriate title (*"Reading History"*) and empty state message (*"No reading history yet. Articles you read will appear here."*).
   - **No clear history button in the list header** (maintains a minimal, clutter-free reading view).
3. **Settings Screen**:
   - Add a **Reading History** section displaying the stored history item count (e.g., *"48 articles in history"* or *"History is empty"*).
   - Include a **"Clear Reading History"** button with a confirmation dialog to wipe local history without affecting server read status.

---

## 2. Architecture & Special Status Centralization

### Centralized Status Filter Rules
Miniflux API supports `"unread"` and `"read"` (and `"starred"` via query param). `"history"` is a **local-only virtual filter**.

| Status Filter | Scope | Server Synced in `refreshAll()` | Sidebar Item | Header Title |
|---|---|---|---|---|
| `"unread"` / `null` | Local & Server | Yes (`syncEntries("unread")`) | All Unread Feeds | Articles |
| `"starred"` | Local & Server | Yes (`syncEntries("starred")`) | Bookmarks | Bookmarks |
| `"history"` | **Local Only** | **No** (skipped during sync) | History | Reading History |

### Server Sync Rule in `refreshAll()`
`refreshAll()` always synchronizes categories/feeds and bookmarks (`"starred"`). It only calls `syncEntries(statusFilter)` if `statusFilter` is a server-synced filter:
```kotlin
fun isServerSyncableStatus(status: String?): Boolean {
    return status != "starred" && status != "history" && status != null
}

fun refreshAll() {
    viewModelScope.launch {
        _isLoading.value = true
        try {
            repository.flushPendingSyncs()
            repository.syncCategoriesAndFeeds()
            repository.syncEntries("starred")
            val currentFilter = _statusFilter.value
            if (isServerSyncableStatus(currentFilter)) {
                repository.syncEntries(currentFilter)
            }
        } catch (e: Exception) {
            handleException(e)
        } finally {
            _isLoading.value = false
        }
    }
}
```

---

## 3. Database Schema & Migration (Room v3 → v4)

### Dedicated `reading_history` Table
```sql
CREATE TABLE IF NOT EXISTS `reading_history` (
    `entryId` INTEGER PRIMARY KEY NOT NULL,
    `readAt` INTEGER NOT NULL,
    FOREIGN KEY(`entryId`) REFERENCES `entries`(`id`) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS `index_reading_history_readAt` ON `reading_history`(`readAt`);
```

### Migration Steps:
1. **`LyhtyDatabase.kt`**: Bump version from `3` to `4`, register `ReadingHistoryEntity::class`.
2. **`RoomMigrations.kt`**: Implement `MIGRATION_3_4`.
3. **`DatabaseModule.kt`**: Add `MIGRATION_3_4` to database builder.

---

## 4. DAO & Repository Layer

### `HistoryDao.kt`
```kotlin
@Dao
interface HistoryDao {
    @Query("""
        SELECT entries.* FROM entries
        INNER JOIN reading_history ON entries.id = reading_history.entryId
        ORDER BY reading_history.readAt DESC
        LIMIT :limit OFFSET :offset
    """)
    fun getHistoryEntries(limit: Int = 100, offset: Int = 0): Flow<List<EntryEntity>>

    @Query("INSERT OR REPLACE INTO reading_history (entryId, readAt) VALUES (:entryId, :readAt)")
    suspend fun recordHistory(entryId: Long, readAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM reading_history")
    fun getHistoryCount(): Flow<Int>

    @Query("DELETE FROM reading_history")
    suspend fun clearHistory()
}
```

### `MinifluxRepository.kt` & `MinifluxRepositoryImpl.kt`
- `fun getHistoryEntries(limit: Int = 100, offset: Int = 0): Flow<List<EntryEntity>>`
- `fun getHistoryCount(): Flow<Int>`
- `suspend fun recordHistory(entryId: Long)`
- `suspend fun clearHistory()`

---

## 5. ViewModel & State Management

### `MinifluxUiState.kt`
```kotlin
data class MinifluxUiState(
    // ...
    val historyCount: Int = 0
)
```

### `MinifluxMainViewModel.kt`
1. **Auto-recording**: When an article is selected (`selectEntry(id)`), call `repository.recordHistory(id)`.
2. **History Filter**:
   ```kotlin
   fun selectHistory() {
       _selectedCategoryId.value = null
       _selectedFeedId.value = null
       _selectedEntryId.value = null
       savedStateHandle?.remove<Long>(KEY_SELECTED_ENTRY_ID)
       _statusFilter.value = "history"
       activeReadingList = emptyList()
   }
   ```
3. **`_entries` Pipeline**:
   ```kotlin
   }.flatMapLatest { params ->
       when {
           params.query.isNotBlank() -> repository.searchEntries(params.query)
           params.statusFilter == "history" -> repository.getHistoryEntries()
           else -> repository.getEntries(params.statusFilter, params.categoryId, params.feedId)
       }
   }
   ```
4. **Clear History**:
   ```kotlin
   fun clearHistory() {
       viewModelScope.launch {
           repository.clearHistory()
       }
   }
   ```

---

## 6. UI Implementation & Selection State Fixes

### 1. `CategoryFeedTreePane.kt` Mutually Exclusive Selection Predicates
```kotlin
val isAllUnreadSelected = selectedCategory == null && selectedFeed == null && (statusFilter == "unread" || statusFilter == null)
val isBookmarksSelected = selectedCategory == null && selectedFeed == null && statusFilter == "starred"
val isHistorySelected = selectedCategory == null && selectedFeed == null && statusFilter == "history"
```

Add the History card below Bookmarks:
```kotlin
// "History" Entry Shortcut
item {
    FeedTreeCard(
        title = "History",
        icon = Icons.Default.History,
        unreadCount = 0, // No count badge
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
```

### 2. `EntryListPane.kt` Header & Empty State
Add `statusFilter: String? = "unread"` parameter to `EntryListPane`:
```kotlin
@Composable
fun EntryListPane(
    entries: List<EntryEntity>,
    selectedEntry: EntryEntity?,
    statusFilter: String? = "unread",
    searchQuery: String = "",
    isSearchActive: Boolean = false,
    onSelectEntry: (EntryEntity) -> Unit,
    // ...
)
```
- **Header Text**:
  ```kotlin
  val headerText = when {
      searchQuery.isNotBlank() -> "Search Results"
      statusFilter == "starred" -> "Bookmarks"
      statusFilter == "history" -> "Reading History"
      else -> "Articles"
  }
  ```
- **Empty State Text**:
  ```kotlin
  val emptyText = when {
      searchQuery.isNotBlank() -> "No articles matching '$searchQuery'"
      statusFilter == "starred" -> "No bookmarked articles"
      statusFilter == "history" -> "No reading history yet. Articles you read will appear here."
      else -> "No articles available"
  }
  ```

### 3. `SettingsPane.kt` Reading History Section
Add a dedicated card in Settings:
- Section title: `Reading History`
- Body text: Shows formatted count (e.g. `"${historyCount} articles in reading history"` or `"No reading history recorded"`).
- Action: `OutlinedButton(onClick = { showClearHistoryDialog = true })` with icon `Icons.Default.DeleteSweep` and label `"Clear Reading History"`.

---

## 7. Testing & Verification Plan

1. **Room Migration Test (`RoomMigrationsTest.kt`)**:
   - Verify `MIGRATION_3_4` executes and creates `reading_history` table with foreign key and index.
2. **Repository Unit Test (`MinifluxRepositoryTest.kt`)**:
   - Verify `recordHistory(id)` and `getHistoryEntries()` sort by `readAt DESC`.
   - Verify `clearHistory()` empties `reading_history` without deleting `entries` rows.
3. **ViewModel Unit Test (`MinifluxMainViewModelTest.kt`)**:
   - Verify `refreshAll()` does **not** call `syncEntries("history")`.
   - Verify selecting an entry triggers `recordHistory(id)`.
   - Verify `selectHistory()` switches `statusFilter` to `"history"` and clears category/feed.
   - Verify `clearHistory()` clears the history table.
4. **UI Tests**:
   - Verify `CategoryFeedTreePane` selection state is strictly mutually exclusive between All Unread, Bookmarks, and History.
   - Verify `EntryListPane` displays *"Reading History"* title and proper empty state.
   - Verify Settings shows count and handles clear history.
5. **Full Suite**:
   - `./test.sh` passing 100%.
