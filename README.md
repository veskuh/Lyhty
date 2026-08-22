# 🕯️ Lyhty (Miniflux RSS Reader for Android)

**Lyhty** (Finnish for *Lantern*) is a modern, privacy-focused Android RSS reader app specifically designed and optimized for foldable smartphones (such as the Honor Magic V3, Samsung Galaxy Z Fold series, Google Pixel Fold, and OnePlus Open), tablets, and standard phones.

Built with **Jetpack Compose** and **Material 3 Adaptive**, Lyhty seamlessly syncs with your self-hosted **Miniflux v1 API** server with full offline-first capabilities.

---

## ✨ Features

- **📱 Foldable & Adaptive Multi-Pane UI**:
  - **Dual-Pane / Multi-Pane**: Hierarchical Category & Feed Tree + Article List + Reader Pane on unfolded foldable displays and tablets.
  - **Single-Pane Navigation**: Smooth list-detail transitions with predictive back-gesture handling on cover displays and phone screens.
  - **Foldable Posture Awareness**: Dynamic layout adaptation for **Tabletop (Flex Control Desk)**, **Book mode**, and **Flat canvas** postures.
- **⭐ Starred / Bookmarks ("Save for Later")**:
  - Star or unstar articles directly from the article reader overflow menu with tactile haptic feedback.
  - Dedicated **"Starred" filter chip** in the feed list for fast access to bookmarked stories.
  - Bidirectional offline synchronization for starred and unstarred actions.
- **✅ Category & Feed-Level Mark-as-Read**:
  - Long-press context menus on any category or feed card to mark all child articles as read in a single batch.
  - Global "Mark all as read" shortcut directly from the "All Unread Feeds" card.
- **👆 Intuitive Gestures & Reading Flow**:
  - **Feed List Swipe Gestures**: Swipe left to immediately open the next unread article; swipe right to navigate back to the feed tree.
  - **Reader Swiping & Quick-Jump Pill**: Swipe horizontally across articles, and automatically advance to the next unread feed following the visual sidebar hierarchy.
- **📖 Rich Article Rendering**:
  - Modular native rendering engine with `HtmlParserUtil` parsing headings, custom blockquotes, bullet lists, and SVG/raster images via Coil.
  - Reader typography scaling and customizable reader themes (**OLED Black**, **Warm Sepia**, **Light**).
- **⚡ Offline-First Architecture & Resilient Sync**:
  - Instant offline access powered by Room SQLite Database.
  - Network state detection (`NetworkMonitor`) with offline action batching and automatic synchronization upon reconnection.
- **🔒 Secure Credential Management**:
  - Encrypted token storage powered by `EncryptedSharedPreferences` with Keystore fallback and sanitized logging.
  - Automatic URL normalization (handles trailing `/v1`, extra slashes, and protocol prefixes).
  - Multi-header authentication fallback (`X-Auth-Token`, `X-Miniflux-API-Key`, `Authorization: Bearer`, and `Basic Auth`).

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.1.0 (JVM 21)
- **UI Framework**: Jetpack Compose & Material 3 Adaptive Layout (`ListDetailPaneScaffold`)
- **Dependency Injection**: Dagger Hilt
- **Networking**: Retrofit 2 + OkHttp 4 + Kotlinx Serialization (with dynamic host resolution and transient retry interceptors)
- **Database & Storage**: Room Database + SQLite FTS + EncryptedSharedPreferences (AndroidX Security Crypto)
- **Image Loading**: Coil 2 with SVG decoder
- **Concurrency**: Kotlin Coroutines, `StateFlow`, and `SharedFlow`
- **Testing & Coverage**: JUnit 4, MockK, Robolectric, Turbine, and Kover (122 unit and scenario tests passing)

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (2024.2.1+) or command-line SDK tools.
- JDK 21+ (or JDK 17+).
- A running [Miniflux](https://miniflux.app/) instance (v2.x with API enabled).

### Building & Running

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/veskuh/Lyhty.git
   cd Lyhty
   ```

2. **Run Test Suite & Code Coverage**:
   ```bash
   ./test.sh
   ```

3. **Build Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```
   The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk` (or `apk/Lyhty-debug.apk`).

4. **Install onto Connected Device via ADB**:
   ```bash
   adb install -r apk/Lyhty-debug.apk
   ```

---

## ⚙️ Configuration

Launch Lyhty on your device and navigate to **[ ⚙️ Settings ]**:

1. **Server URL**: Enter your Miniflux server URL (e.g. `https://miniflux.example.com`).
2. **API Key / Token**: Enter your Miniflux API Key (generated under *Miniflux Web UI -> Settings -> API Keys*).
3. **Log Level**: Select `DEBUG`, `INFO`, `WARN`, or `ERROR` for diagnostic logging.
4. **Reading Preferences**: Adjust font size scaling (85% to 130%), color theme, and toggle *Hide feeds with no unread items*.

---

## 📄 License

Copyright © 2026. All rights reserved.
