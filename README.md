# Lyhty

Miniflux RSS reader for Android. Lyhty is Finnish for "lantern".

An Android client for self-hosted [Miniflux](https://miniflux.app/) servers, targeting foldables (Honor Magic V6, Samsung Galaxy Z Fold, Pixel Fold, etc.), tablets, and phones.

Built with Jetpack Compose and Material 3 Adaptive. Syncs with the Miniflux v1 API.

---

## Main Features

- **Foldable and adaptive layout**
  - Dual-pane layout on unfolded displays and tablets: category/feed tree + article list or reader.
  - Single-pane list-detail navigation on cover displays and phones, with back-gesture handling.
- **Bookmarks & unread shortcuts**
  - Dedicated "Bookmarks" and "All Unread Feeds" quick-access cards at the top of the sidebar with live counts.
  - Star and unstar articles from the reader menu with offline sync.
- **Global full-text search (FTS)**
  - Search across feed names, article titles, body content, and authors.
- **Gestures**
  - Article list: swipe left to open the next unread article, swipe right to go back.
  - Reader: swipe between articles, quick-jump pill to the next unread feed.
- **Article rendering**
  - `HtmlParserUtil` parses headings, blockquotes, bullet lists, and images.
  - Reader typography scaling and themes (OLED black, sepia, light) configurable in Settings.

---

## Tech stack

- **Language**: Kotlin
- **UI**: Jetpack Compose, Material 3 Adaptive
- **Networking**: Retrofit 2, OkHttp 4, kotlinx.serialization
- **Storage**: Room + SQLite
- **Images**: Coil 2 with SVG decoder
- **Concurrency**: Kotlin coroutines, StateFlow
- **Tests**: JUnit 4, MockK, Robolectric, Turbine, Kover

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

- Android Studio or command-line SDK tools.
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
