# Startup Experience & Smooth Loading Plan

## Problem Statement

When Lyhty launches on a device:
1. **Splash & Theme Flash**: The system splash screen appears for a fraction of a second, followed by a momentary flash of a generic/light window background before switching to the user's configured theme (`OLED_DARK`, `SEPIA`, or `LIGHT`).
2. **Double-Tree Update (Layout Shift)**: The sidebar Category & Feed tree renders in an initial empty state, immediately updates when Room loads the local database cache (~30ms), and then updates a second time when network synchronization completes, causing visible layout shifting.

---

## Root Causes

1. **Missing Android 12+ SplashScreen API Integration**:
   - `MainActivity` does not use `androidx.core:core-splashscreen` (`installSplashScreen()`).
   - `Theme.Lyhty` inherits directly from `android:Theme.Material.NoActionBar` without custom `android:windowBackground` or splash screen theme attributes. The OS dismisses the splash icon before Compose renders its first meaningful frame.
2. **Asynchronous Initial State Emission**:
   - `MinifluxMainViewModel.uiState` initializes with `MinifluxUiState(isLoading = true, readerTheme = OLED_DARK, categories = emptyList())`.
   - If the user prefers `SEPIA` or `LIGHT`, Compose renders frame 0 in `OLED_DARK` before the preference flow emits.
   - Compose renders an empty tree in frame 0, local Room cache in frame 1, and synced data in frame 2.

---

## Architectural Solution

```
App Launch
   │
   ▼
[ Android Core SplashScreen ] ──> Styled with Theme.Lyhty.Starting (#000000 / OLED Dark)
   │
   ├─► Read User Preferences Synchronously (Theme, Font Scale)
   │
   ├─► Query Local Room DB (Categories, Feeds, Counts) ─── (~30-50ms)
   │                                                         │
   ▼                                                         ▼
[ SplashScreen.setKeepOnScreenCondition { !isReady } ] ◄── Local Cache Ready
   │
   ▼ (Smooth Alpha / Slide Exit)
[ Main Activity UI ] (Fully populated tree + Correct User Theme)
   │
   ▼ (Non-disruptive background sync)
[ Network Sync Completes ] ──> Updates Room via @Upsert (In-place animated updates)
```

---

## Detailed Step-by-Step Implementation

### Step 1: Add `androidx.core:core-splashscreen` Dependency
- In `gradle/libs.versions.toml`:
  - Add version `coreSplashscreen = "1.0.1"`.
  - Add library `androidx-core-splashscreen = { group = "androidx.core", name = "core-splashscreen", version.ref = "coreSplashscreen" }`.
- In `app/build.gradle.kts`:
  - Add `implementation(libs.androidx.core.splashscreen)`.

### Step 2: Configure Starting & App Themes in `res/values/themes.xml`
- Create `Theme.Lyhty.Starting` inheriting from `Theme.SplashScreen`:
  - `windowSplashScreenBackground`: `@android:color/black` (OLED pitch black).
  - `windowSplashScreenAnimatedIcon`: `@mipmap/ic_launcher` (or vector logo).
  - `postSplashScreenTheme`: `@style/Theme.Lyhty`.
- Configure `Theme.Lyhty`:
  - `android:windowBackground`: `@android:color/black` (prevents any intermediate white/gray frame before Compose attaches).
- Update `AndroidManifest.xml`:
  - Set `android:theme="@style/Theme.Lyhty.Starting"` on `<application>` and `<activity android:name=".MainActivity">`.

### Step 3: Seed Synchronous Preferences in `MinifluxMainViewModel`
- Pre-seed `initialValue` of `uiState` using synchronous repository reads:
  ```kotlin
  val initialTheme = configRepository?.getReaderThemeSync() ?: ReaderTheme.OLED_DARK
  val initialFontScale = configRepository?.getFontSizeScaleSync() ?: 1.0f
  val initialShowUnread = configRepository?.getShowOnlyUnreadFeedsSync() ?: true
  ```
- Pass these values to `initialValue = MinifluxUiState(readerTheme = initialTheme, fontSizeScale = initialFontScale, showOnlyUnreadFeeds = initialShowUnread, isLoading = true)`.
- Expose `val isReady: StateFlow<Boolean>` which becomes `true` once the local database flows have emitted their initial state.

### Step 4: Wire `installSplashScreen()` in `MainActivity.kt`
- In `MainActivity.onCreate()`:
  ```kotlin
  val splashScreen = installSplashScreen()
  val viewModel: MinifluxMainViewModel by viewModels()

  splashScreen.setKeepOnScreenCondition {
      !viewModel.isReady.value
  }
  ```
- Edge-to-edge styling is configured to match the user's active theme seamlessly.

### Step 5: Verification & Automated Tests
- Add tests in `MinifluxMainViewModelTest` to verify `isReady` transitions from `false` to `true` upon first local flow emission.
- Verify `LyhtyLoggerTest`, `SettingsPaneTest`, and all existing unit & UI tests pass (`./test.sh`).
- Deploy to physical test device and verify cold-start smoothness with zero layout jumps.
