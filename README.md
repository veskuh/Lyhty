# 🕯️ Lyhty (Miniflux RSS Reader for Android)

**Lyhty** (Finnish for *Lantern*) is a premium, modern Android RSS reader app specifically optimized for foldable smartphones (such as the Honor Magic V3, Samsung Galaxy Z Fold series, and Google Pixel Fold) as well as tablets and standard phones.

Built with **Jetpack Compose** and **Material 3 Adaptive**, Lyhty seamlessly syncs with your self-hosted **Miniflux v1 API** server.

---

## ✨ Features

- **📱 Foldable & Adaptive UI**:
  - Dual-pane layout on unfolded foldables and tablets (Category/Feed Tree + Entry List + Reader Detail).
  - Single-pane layout with smooth back-navigation on cover screens and phone displays.
  - Folding posture awareness (Hinge, Flat, Half-Opened Book/Tent modes).
- **🔒 Encrypted & Robust Credentials**:
  - Secure token storage powered by `EncryptedSharedPreferences` with Keystore auto-recovery.
  - Multi-header authentication fallback (`X-Auth-Token`, `X-Miniflux-API-Key`, `Authorization: Bearer`, and `Basic Auth`).
  - Automatic URL sanitization (handles trailing `/v1`, extra slashes, and protocol prefixes).
- **⚡ Offline First & Action Queuing**:
  - Instant offline access powered by Room Database.
  - Network state detection (`NetworkMonitor`) with offline action queuing (mark as read, bookmark, star).
- **🎨 Personalized Reader Themes**:
  - System default, Sepia, Solarized Dark, OLED Black, and High Contrast reading themes.
- **🛡️ Diagnostic Error Surface & Logging**:
  - User-friendly error banner with actionable resolution hints.
  - In-app diagnostic log viewer with log export/share feature.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.1.0
- **UI Framework**: Jetpack Compose & Material 3 Adaptive Layout (`ListDetailPaneScaffold`)
- **Dependency Injection**: Hilt (Dagger)
- **Networking**: Retrofit 2 + OkHttp 4 + Kotlinx Serialization
- **Database & Storage**: Room Database + EncryptedSharedPreferences (AndroidX Security Crypto)
- **Concurrency**: Kotlin Coroutines & `StateFlow` / `Flow`
- **Testing & Coverage**: JUnit4, MockK, Kover (89 unit & connection scenario tests passing)

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (2024.2.1+) or command-line SDK tools.
- JDK 17+.
- A running [Miniflux](https://miniflux.app/) instance (v2.x with API enabled).

### Building & Running

1. **Clone the Repository**:
   ```bash
   git clone <repo-url>
   cd Lyhty
   ```

2. **Run Test Suite & Code Coverage**:
   ```bash
   ./coverage.sh
   ```

3. **Build Release APK**:
   ```bash
   ./coverage.sh assembleRelease
   ```
   The generated APK will be placed at `apk/Lyhty.apk` and `app/build/outputs/apk/release/app-release.apk`.

4. **Install onto Connected Device via ADB**:
   ```bash
   adb install -r apk/Lyhty.apk
   ```

---

## ⚙️ Configuration

Launch Lyhty on your device and tap **[ ⚙️ Settings ]**:

1. **Server URL**: Enter your Miniflux server URL (e.g. `https://veskuh.net/miniflux`).
2. **API Key / Token**: Enter your Miniflux API Key (generated under *Miniflux Web UI -> Settings -> API Keys*).
3. **Log Level**: Select `DEBUG`, `INFO`, `WARN`, or `ERROR` for diagnostic logging.

---

## 📄 License

Copyright © 2026. All rights reserved.
