# AGENTS.md — Lyhty

Guidance for AI coding agents (and new contributors) working in this repository.

## What this project is

Lyhty is a Kotlin/Jetpack Compose Android client for self-hosted [Miniflux](https://miniflux.app/) servers, optimized for foldables (Honor Magic V5, Samsung Galaxy Z Fold, Pixel Fold) and tablets. Offline-first, Room-backed, single-module.

- **minSdk 34, target/compileSdk 35**, JDK 21 (see `test.sh` for the pinned `JAVA_HOME`).
- Package root: `net.veskuh.lyhty`.

## Commands

```bash
./test.sh              # runs `./gradlew testDebugUnitTest` (unit + Robolectric Compose UI tests). Pins JDK 21 + Android SDK.
./gradlew assembleDebug # debug APK
./coverage.sh          # Kover coverage report + verification
```

There are **no instrumented tests**; all tests (including Compose UI via Robolectric) run on the JVM.

## Package map

```
data/
  local/     Room: LyhtyDatabase, entities, DAOs, RoomMigrations, model (count rows)
  network/   Retrofit MinifluxApiService + DTOs + OkHttp interceptors
             (DynamicHostInterceptor, MinifluxAuthInterceptor, TransientNetworkInterceptor)
  repository/ MinifluxRepository(Impl), MinifluxConfigRepository(Impl)
di/          Hilt modules (DatabaseModule, NetworkModule, etc.)
ui/
  screens/   LyhtyAdaptiveApp, CategoryFeedTreePane, EntryListPane, EntryReaderPane,
             SettingsPane, ReaderComponents, MinifluxActionRow
  components/ FoldablePostureHandler
  viewmodel/ MinifluxMainViewModel
  state/     MinifluxUiState, ReaderTheme
  theme/     LyhtyTheme + color schemes
util/        NetworkMonitor, LyhtyLogger, LyhtyErrorClassifier, DateFormatter, HtmlParserUtil
```

## Key flows (read these before touching related code)

1. **Offline-first sync (single source of truth).** Local writes come first:
   `EntryDao.updateEntryStatus` / `updateEntryStarred` set `isSyncPending = 1`, then the
   repository calls the API; on success `clearPendingSyncFlag`, on failure the flag is retained.
   `MinifluxRepositoryImpl.flushPendingSyncs()` batches pending entries by current value
   (`read` / `unread` / `starred=true`) and is triggered from `refreshAll()` and from the
   `isOnline` collector in `MinifluxMainViewModel`. **There is no separate sync-queue table
   anymore** (removed) — do not reintroduce one.
2. **Adaptive layout.** `LyhtyAdaptiveApp` branches on
   `currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass != COMPACT`:
   expanded = `Row(CategoryFeedTreePane | list/reader/settings)`, compact = `ListDetailPaneScaffold`
   single pane. Folding posture is collected in `MainActivity` → `FoldablePostureHandler` →
   `PostureInfo` (reader tabletop "flex" mode splits content + control desk).
3. **Reader HTML pipeline.** `HtmlParserUtil` turns `entry.content` into `ReaderBlock`s
   (`Text` / `Image` / `Quote` / `ListItem`); `ReaderComponents.kt` renders them
   (`ReaderContent`, `ReaderTextBlock`, `ReaderQuickJumpPill`); `MinifluxActionRow.kt` is the toolbar.
4. **Auth/networking.** `MinifluxAuthInterceptor` sends multi-header fallback
   (`X-Auth-Token`, `X-Miniflux-API-Key`, `Authorization`). `DynamicHostInterceptor` resolves the
   user's server base URL (preserves subpaths). The `Json` config uses `explicitNulls = false`
   so nullable request fields are omitted (required for `UpdateStatusRequestDto`).

## Conventions & gotchas

- **Room schema version is currently 3.** Any entity/column change must bump `LyhtyDatabase.version`
  and add a `Migration` in `RoomMigrations.kt`, registered in `DatabaseModule`.
- **`feeds.categoryId` is authoritative** for category membership. `entries.categoryId` is
  denormalized and is *not* used for filtering/counting — both `EntryDao.getEntries` and
  `CategoryDao.getUnreadCountsByCategory` `JOIN feeds`. Keep it that way.
- **Preferences use `apply()`, not `commit()`** (avoid blocking encrypted disk I/O on main).
- **`remember` keying matters**: reader scroll/gesture/drag state must reset per `entry.id`.
- **Comments are the exception, not the rule** — write self-documenting code.
- Package by layer, not by feature. State is exposed as `StateFlow` via `MinifluxUiState`;
  the ViewModel combines flows through typed intermediate data classes (no positional `as` casts).
- Miniflux API surface is exercised in tests by `SimulatedMinifluxServer` (MockWebServer);
  extend it when adding endpoints.
