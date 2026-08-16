# PLAN.md — Lyhty: Miniflux Android Foldable RSS Reader

## 🎯 Architecture & UX Overview
* **Target Devices**: Galaxy Z Fold 8 Ultra, Honor Magic V5, Pixel Fold 3 (`minSdk = 34`, Android 14+).
* **Backend**: 100% Miniflux REST API v1 complete specification (`SimulatedMinifluxServer` mock engine for TDD).
* **UI Framework**: Jetpack Compose `Material 3 Adaptive` (`ListDetailPaneScaffold`, `FoldingFeature` posture detection).
* **Strict UX Action Button Rule**: Universal icons (🔍 Search, ⚙️ Settings) may use icons alone; all other actions MUST use **Icon + Text Label**.
* **Testing Standard**: Strict TDD workflow (Write Test $\rightarrow$ Implement $\rightarrow$ Pass Test), target $\ge$80% test coverage verified by Kover.

---

## 📋 Progress Tracking Checklist

### ✅ Phase 1: Project Setup, Test Double & Complete Miniflux API v1 Harness (COMPLETED & AUDITED)
- [x] **1.1**: Initialize Gradle versions catalog (`libs.versions.toml`), root & app build scripts, configure Kover coverage plugin and modern Kotlin 2.0 `compilerOptions`.
- [x] **1.2**: Implement `MinifluxConfigRepository` to handle dynamic Miniflux Server URL and API Key / `X-Auth-Token` authentication.
- [x] **1.3**: Implement complete 24-endpoint Retrofit interface `MinifluxApiService` and serialization DTOs (`MinifluxExtendedDtos.kt`).
- [x] **1.4**: Build `SimulatedMinifluxServer` (`MockWebServer`) test double mocking 100% of Miniflux API v1 endpoints (Feeds, Entries, Categories, OPML Import/Export, Feed Icons, Discovery) and error injection queue.
- [x] **1.5**: Create `TestNetworkFactory` for test double base URL binding.
- [x] **1.6**: Verify Phase 1 test suite (`SimulatedMinifluxServerTest`, `MinifluxConfigRepositoryTest`, `MinifluxApiServiceTest`) passing 100%.

---

### ✅ Phase 2: Local Room Database & Data Layer (COMPLETED & AUDITED)
- [x] **2.1**: Implement Room entities (`CategoryEntity`, `FeedEntity`, `EntryEntity`, `EntryFtsEntity`, `SyncQueueEntity`) with `@ForeignKey(onDelete = CASCADE)` constraints and indices.
- [x] **2.2**: Implement `SyncDao` and persistent offline sync queue (`SyncQueueEntity`) to store and flush offline mutations.
- [x] **2.3**: Implement `EntryDao` `@Transaction upsertEntriesWithFts()` for automated SQLite FTS5 full-text search indexing.
- [x] **2.4**: Implement SQL `GROUP BY` unread count queries in `FeedDao` & `CategoryDao` (`FeedUnreadCount`, `CategoryUnreadCount`).
- [x] **2.5**: Implement `MinifluxRepositoryImpl` supporting pagination (`limit`, `offset`), FTS search, dynamic config, and offline queue flushing.
- [x] **2.6**: Verify Phase 2 database & repository unit tests (`RoomDatabaseTest`, `MinifluxRepositoryTest`) passing 100%.

---

### ✅ Phase 3: Domain Layer & ViewModel Architecture (COMPLETED & AUDITED)
- [x] **3.1**: Expose `unreadCountsByFeed` and `unreadCountsByCategory` in `MinifluxUiState`.
- [x] **3.2**: Synchronize Category & Feed selection state (`selectFeed` auto-sets parent category ID).
- [x] **3.3**: Implement sequential entry navigation (`selectNextEntry()`, `selectPreviousEntry()`).
- [x] **3.4**: Implement `clearError()` to clear non-null error states.
- [x] **3.5**: Verify Phase 3 Turbine unit tests (`MinifluxMainViewModelTest`) passing 100% with `./test.sh`.

---

### ✅ Phase 4: Foldable UI Components (Material 3 Adaptive) (COMPLETED & AUDITED)
- [x] **4.1**: Build `FoldablePostureHandler` handling `FoldingFeature` posture detection (Folded, Unfolded Canvas, 90° Flex Tabletop Mode) and hinge crease padding calculations.
- [x] **4.2**: Implement `BackHandler` bound to `navigator.canNavigateBack()` in `LyhtyAdaptiveApp` for single-pane cover screen gesture navigation.
- [x] **4.3**: Build `CategoryFeedTreePane` rendering expandable categories and nested child feeds with reactive unread count badges (`Badge`).
- [x] **4.4**: Build `EntryListPane` with `Unread`/`All` filter chips, search query clear button `[ ❌ Clear ]`, and empty state handling.
- [x] **4.5**: Build `EntryReaderPane` enforcing explicit Icon + Text Label buttons (`[ 🌐 Fetch Full Text ]`, `[ 👁️ Mark Read ]`, `[ 🔗 Open Browser ]`, `[ ⬆️ Prev ]`, `[ ⬇️ Next ]`, `[ 🎨 Theme ]`, `[ 🔤 Font ]`) and HTML readability parsing.
- [x] **4.6**: Verify Compose UI component tests (`CategoryFeedTreePaneTest`, `EntryListPaneTest`, `EntryReaderPaneTest`) passing 100% with `./test.sh`.

---

### ✅ Phase 5: Comprehensive End-to-End Test Suite & Final Verification (COMPLETED & AUDITED)
- [x] **5.1**: Build `E2E 1 - Complete 3-Pane Adaptive Navigation Flow` (`Category Tree` $\rightarrow$ `Entry List` $\rightarrow$ `Reader`).
- [x] **5.2**: Build `E2E 2 - Live FTS5 Search Filtering and Clear Action Flow` (`EntryListPane` query typing + `[ ❌ Clear ]`).
- [x] **5.3**: Build `E2E 3 - Reader Action Buttons and Optimistic Status Updates` (`[ 🌐 Fetch Full Text ]` & `[ 👁️ Mark Read ]`).
- [x] **5.4**: Build `E2E 4 - Offline Sync Queueing and Server Recovery Flushing Flow` (HTTP 500 error queueing in `SyncQueueEntity` + recovery `flushPendingSyncs()`).
- [x] **5.5**: Run `./coverage.sh` and verify 100% test pass rate and Kover coverage verification (`koverVerifyDebug`) with **BUILD SUCCESSFUL**.

---

### ✅ Phase 6: Production Hardening, Subpath Support & Diagnostic Error System (COMPLETED & AUDITED)
- [x] **6.1**: Fix `DynamicHostInterceptor` to preserve and merge server subpath prefixes (e.g. `https://veskuh.net/miniflux/v1/categories`).
- [x] **6.2**: Add `statusBarsPadding()` to `LyhtyAdaptiveApp` to eliminate status bar overlap.
- [x] **6.3**: Build comprehensive error classifier `LyhtyErrorClassifier` and data model `LyhtyError` mapping unique codes (`ERR-NET-404`, `ERR-AUTH-401`, `ERR-DNS-101`, `ERR-CONN-102`, `ERR-TIMEOUT-103`, `ERR-SSL-201`, `ERR-URL-301`, `ERR-PARSE-401`, `ERR-UNKNOWN-999`).
- [x] **6.4**: Render rich top Error Surface banner with unique error code badge, detailed explanation, actionable hint, and **`[ 📋 Copy Error ]`** button.
- [x] **6.5**: Implement Material 3 empty states in `CategoryFeedTreePane` and `EntryListPane` with **`[ ⚙️ Configure Server ]`** button.
- [x] **6.6**: Run `./coverage.sh` verifying 100% test pass rate (63 tests) and generate updated Production Release APK (`/Users/vesku/Developer/Lyhty/apk/Lyhty.apk`).

