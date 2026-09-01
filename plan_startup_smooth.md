# Startup Experience & Smooth Loading Plan (Refined)

## Problem Statement

When Lyhty launches on a device:
1. **Theme & Background Flash**:
   - `Theme.Lyhty` currently inherits from `android:Theme.Material.NoActionBar`, exposing Android's default `#303030` dark-gray window background during cold start.
   - If the user has configured `SEPIA` (cream `#F4ECD8`), `LIGHT` (clean white `#FAFAFC`), or `OLED_DARK` (`#000000`), there is a brief background color flash before Compose renders.
   - A static XML `android:windowBackground` only matches one theme and causes flashes for the other two.
2. **Double-Tree Update (Layout Shift)**:
   - The Category & Feed tree renders in an initial empty state (Frame 0), shifts when Room emits the cached local snapshot (Frame 1, ~30–50ms), and shifts again when network sync updates feeds or unread counts (Frame 2).

---

## Architectural Solution

```
App Launch (Cold Start)
   │
   ├─► 1. Synchronously Read Config (getReaderThemeSync, getFontSizeScaleSync)
   │      └─► Set Window Background Dynamically: window.setBackgroundDrawable(ColorDrawable(...))
   │
   ├─► 2. Initialize Core SplashScreen (installSplashScreen)
   │      └─► setKeepOnScreenCondition { !isLocalCacheReady }
   │
   ├─► 3. ViewModel Loads Local Room DB Snapshot (first() emission from combined Room flows)
   │      └─► Updates mutableStateOf(isLocalCacheReady = true) ─── (~30-50ms)
   │
   ▼
[ SplashScreen Drops Directly into Fully Loaded Tree in User's Theme ]
   │
   ▼
[ Network Sync Completes ]
   └─► Room @Upsert emits changes
   └─► CategoryFeedTreePane renders updates smoothly via Modifier.animateItem() & animateContentSize()
```

---

## Detailed Step-by-Step Implementation

### Step 1: Add `androidx.core:core-splashscreen` Dependency
- **`gradle/libs.versions.toml`**:
  - Add `coreSplashscreen = "1.0.1"`
  - Add `androidx-core-splashscreen = { group = "androidx.core", name = "core-splashscreen", version.ref = "coreSplashscreen" }`
- **`app/build.gradle.kts`**:
  - Add `implementation(libs.androidx.core.splashscreen)`

---

### Step 2: Dynamic Window Background & Splash Theme
1. **`res/values/themes.xml`**:
   - Define `Theme.Lyhty.Starting` inheriting from `Theme.SplashScreen`:
     - `windowSplashScreenBackground`: `@android:color/black` (or neutral dark)
     - `windowSplashScreenAnimatedIcon`: `@mipmap/ic_launcher`
     - `postSplashScreenTheme`: `@style/Theme.Lyhty`
   - Define `Theme.Lyhty` inheriting from `android:Theme.Material.NoActionBar`.
2. **`MainActivity.onCreate()` Dynamic Window Background**:
   - Inject `MinifluxConfigRepository` in `MainActivity`:
     ```kotlin
     @Inject lateinit var configRepository: MinifluxConfigRepository
     ```
   - Read the persisted theme synchronously *before* `setContent` and convert `Color.kt` constants directly via `toArgb()`:
     ```kotlin
     val persistedTheme = configRepository.getReaderThemeSync()
     val bgColor = when (persistedTheme) {
         ReaderTheme.OLED_DARK -> OledBackground.toArgb()
         ReaderTheme.SEPIA -> SepiaBackground.toArgb()
         ReaderTheme.LIGHT -> LightBackground.toArgb()
     }
     window.setBackgroundDrawable(ColorDrawable(bgColor))
     ```
   - Directly referencing `OledBackground`, `SepiaBackground`, and `LightBackground` ensures a single source of truth with `Color.kt` and eliminates background color flash for all three themes.

---

### Step 3: Concrete Splash Screen Dismissal Mechanism
1. **Concrete Local Cache Readiness in `MinifluxMainViewModel`**:
   - Combine the initial Room flows (`_categories`, `_feeds`, `_countsData`) and expose readiness via:
     ```kotlin
     val isLocalCacheReady = MutableStateFlow(false)

     init {
         viewModelScope.launch {
             // Wait for the first evaluated local database emission (or fallback after 1000ms safety timeout)
             withTimeoutOrNull(1000L) {
                 combine(_categories, _feeds, _countsData) { _, _, _ -> true }.first()
             }
             isLocalCacheReady.value = true
         }
         refreshAll()
     }
     ```
2. **Reactive Splash Dismissal in `MainActivity.kt`**:
   - Maintain Compose snapshot state `var isReady by mutableStateOf(false)` in `MainActivity`.
   - Collect `viewModel.isLocalCacheReady` in `lifecycleScope`:
     ```kotlin
     val splashScreen = installSplashScreen()
     val viewModel: MinifluxMainViewModel by viewModels()
     var isReady by mutableStateOf(false)

     lifecycleScope.launch {
         repeatOnLifecycle(Lifecycle.State.CREATED) {
             viewModel.isLocalCacheReady.collect { isReady = it }
         }
     }

     splashScreen.setKeepOnScreenCondition { !isReady }
     ```
   - Snapshot-state invalidation ensures the SplashScreen condition lambda is re-evaluated immediately upon frame readiness.

---

### Step 4: Synchronous `MinifluxUiState` Initial Value & ViewModel Scoping
- In `MinifluxMainViewModel`:
  - Pass synchronously read values from `configRepository` (`getReaderThemeSync()`, `getFontSizeScaleSync()`, `getShowOnlyUnreadFeedsSync()`) into `uiState`'s `initialValue`.
- In `MainActivity` & `LyhtyAdaptiveApp`:
  - Pass the Activity-scoped `viewModel` instance from `MainActivity` directly to `LyhtyAdaptiveApp(viewModel = viewModel, ...)` to ensure a single shared `ViewModelStore`.

---

### Step 5: Smooth Tree Updates on Network Sync (`CategoryFeedTreePane`)
- To eliminate jarring jumps when network sync finishes (Frame 2):
  1. **Item Placement Animations**: Add `Modifier.animateItem()` to `items(visibleCategories, key = { it.id })` in `CategoryFeedTreePane.kt`.
  2. **Unread Count Transitions**: Use `AnimatedContent` for unread count badge numbers so count increments/decrements animate cleanly rather than snapping.
  3. **Expansion Animations**: Use `Modifier.animateContentSize()` on category child-feed containers.

---

### Step 6: Testing & Verification
1. **Unit Tests**:
   - In `MinifluxMainViewModelTest`: Test that `isLocalCacheReady` emits `true` once Room flows emit, and verify safety timeout.
2. **Robolectric UI Tests**:
   - Verify `CategoryFeedTreePane` and `LyhtyAdaptiveApp` render seamlessly with `isLocalCacheReady`.
3. **Device Verification**:
   - Cold start test on device across `OLED_DARK`, `SEPIA`, and `LIGHT` themes to confirm 0 color flashes and smooth single-step tree loading.
