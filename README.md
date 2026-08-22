# Lyhty

Miniflux RSS reader for Android. Lyhty is Finnish for "lantern".

An Android client for self-hosted [Miniflux](https://miniflux.app/) servers, targeting foldables (Honor Magic V5, Samsung Galaxy Z Fold, Pixel Fold), tablets, and phones.

Built with Jetpack Compose and Material 3 Adaptive. Syncs with the Miniflux v1 API. Offline-first.

---

## Features

- **Foldable and adaptive layout**
  - Dual-pane layout on unfolded displays and tablets: category/feed tree + article list or reader.
  - Single-pane list-detail navigation on cover displays and phones, with back-gesture handling.
  - Folding posture awareness: tabletop (flex control desk), book, and flat modes.
- **Star / bookmark**
  - Star and unstar articles from the reader menu.
  - "Starred" filter in the article list.
  - Starred state syncs offline (both directions).
- **Mark as read at category and feed level**
  - Long-press a category or feed to mark all of its articles read in one batch.
  - "Mark all as read" shortcut on the "All Unread Feeds" card.
- **Gestures**
  - Article list: swipe left to open the next unread article, swipe right to go back.
  - Reader: swipe between articles, quick-jump pill to the next unread feed.
- **Article rendering**
  - `HtmlParserUtil` parses headings, blockquotes, bullet lists, and images (SVG/raster via Coil).
  - Reader typography scaling and themes (OLED black, sepia, light).
- **Offline-first sync**
  - Local storage in Room.
  - `NetworkMonitor` detects connectivity; pending actions sync on reconnection.
- **Credentials**
  - API key stored in `EncryptedSharedPreferences`.
  - URL normalization (trailing `/v1`, extra slashes, protocol prefixes).
  - Multi-header auth fallback (`X-Auth-Token`, `X-Miniflux-API-Key`, `Authorization`).

---

## Tech stack

- **Language**: Kotlin 2.1.0
- **UI**: Jetpack Compose, Material 3 Adaptive (`ListDetailPaneScaffold`)
- **DI**: Dagger Hilt
- **Networking**: Retrofit 2, OkHttp 4, kotlinx.serialization (dynamic host and retry interceptors)
- **Storage**: Room + SQLite FTS, EncryptedSharedPreferences (AndroidX Security Crypto)
- **Images**: Coil 2 with SVG decoder
- **Concurrency**: Kotlin coroutines, StateFlow
- **Tests**: JUnit 4, MockK, Robolectric, Turbine, Kover (122 unit and scenario tests)

---

## Project layout

```
data/
  local/      Room database, entities, DAOs, migrations
  network/    Retrofit MinifluxApiService, DTOs, OkHttp interceptors
  repository/ MinifluxRepository(Impl), MinifluxConfigRepository(Impl)
di/           Hilt modules
ui/
  screens/    LyhtyAdaptiveApp, CategoryFeedTreePane, EntryListPane, EntryReaderPane,
              SettingsPane, ReaderComponents, MinifluxActionRow
  viewmodel/  MinifluxMainViewModel
  state/      MinifluxUiState, ReaderTheme
  theme/      LyhtyTheme + color schemes
util/         NetworkMonitor, LyhtyLogger, LyhtyErrorClassifier, DateFormatter, HtmlParserUtil
```

See `AGENTS.md` for key flows, conventions, and gotchas.

---

## Getting started

### Prerequisites

- Android Studio Ladybug (2024.2.1+) or command-line SDK tools.
- JDK 21 (the build and `./test.sh` are pinned to OpenJDK 21).
- A running Miniflux instance (v2.x with API enabled).

### Building and running

1. Clone the repository:

   ```bash
   git clone https://github.com/veskuh/Lyhty.git
   cd Lyhty
   ```

2. Run tests and coverage:

   ```bash
   ./test.sh
   ```

3. Build the debug APK:

   ```bash
   ./gradlew assembleDebug
   ```

   Output: `app/build/outputs/apk/debug/app-debug.apk`.

4. Install on a connected device:

   ```bash
   adb install -r apk/Lyhty-debug.apk
   ```

---

## Configuration

Open Settings in the app:

1. **Server URL**: your Miniflux server URL, e.g. `https://miniflux.example.com`.
2. **API key**: generated in the Miniflux web UI under Settings > API Keys.
3. **Log level**: `DEBUG`, `INFO`, `WARN`, or `ERROR`.
4. **Reading preferences**: font size scale, theme, and "hide feeds with no unread items".

---

## License

[BSD 3-Clause](LICENSE)
