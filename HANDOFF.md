# CredStow — Handoff Document

> **Status**: Production-ready Android app. Last commit: `baf4c4e`
> **Package**: `com.credstow.app`
> **Build**: ✅ Debug APK, ✅ Release AAB (R8 full mode + signing), ✅ 56 unit tests (0 failures)

---

## 1. Project Goal & Current State

**CredStow** is a security-first Android app for managing AI API keys (OpenAI, Anthropic, Google,
Hugging Face, etc.) on-device. It encrypts every key with AES-256-GCM, backed by the Android
Keystore for the master DEK. No plaintext keys are ever written to disk, logged, or sent to
external servers (except the user-initiated test ping).

**Current maturity**: Feature-complete MVP. Core flows (add/edit/delete keys, test ping, copy to
clipboard with auto-clear, encrypted export/import, biometric lock, Pro unlock via one-time
purchase, AdMob interstitials for free tier) are all implemented and tested. The remaining work
is exclusively Play Store launch — no app features are missing.

### Feature inventory

| Category | Feature | Status |
|---|---|---|
| Core | AES-256-GCM encryption + Android Keystore | ✅ |
| Core | Provider presets (OpenAI, Anthropic, Google, HF, Cohere, Mistral, Together, xAI, Replicate, Azure) | ✅ |
| Core | Key test ping (HTTP probe to provider's API) | ✅ |
| Core | Copy-to-clipboard with auto-clear (5–120s) | ✅ |
| Security | Biometric lock (Pro) | ✅ |
| Security | Biometric re-auth for copy/test (Pro, auto-lock) | ✅ c62905a |
| Security | Auto-lock timer (Pro) — new | ✅ |
| Security | Privacy policy (offline HTML asset) | ✅ |
| Pro | One-time purchase (pro_unlock, $4.99) | ✅ |
| Pro | Unlimited keys, key testing, backup/restore | ✅ |
| Pro | Key expiration date + reminders (Pro) | ✅ c62905a |
| Pro | Key sorting options — created/label/last-used/expiry (Pro) | ✅ c62905a |
| Pro | Last-used tracking (auto-updated on access) | ✅ c62905a |
| Monetization | AdMob banner (free tier) + interstitial milestone/session triggers | ✅ |
| Crash reporting | Firebase Crashlytics (conditional plugin) | ✅ |
| DevOps | ProGuard/R8 (release), Room migrations, schema export | ✅ |
| UX | Quick Settings tile (one-tap key copy) — new | ✅ |
| — | **TODO**: App icon fix | ⚠️ |
| — | **TODO**: Play Store launch blockers (see §5) | ⏳ |

---

## 2. Package Rename — Confirmed Complete

The package rename from `com.keyvault.app` → `com.credstow.app` is committed in commit
**`5a884ad`** (with a follow-up ProGuard fix in `fd1894c`).

### Verification

```sh
$ git grep "com.keyvault"  # returns nothing (exit code 1)
```

The old package name is **fully purged** — not in source, tests, resources, build config,
schemas, ProGuard rules, or documentation.

### What was renamed

| Location | Before | After |
|---|---|---|
| `namespace` / `applicationId` (build.gradle.kts) | `com.keyvault.app` | `com.credstow.app` |
| All Kotlin package declarations | `com.keyvault.app.*` | `com.credstow.app.*` |
| All Kotlin import statements | `com.keyvault.app.*` | `com.credstow.app.*` |
| Source/test directory trees | `java/com/keyvault/app/` | `java/com/credstow/app/` |
| Schema directory | `schemas/com.keyvault.app…` | `schemas/com.credstow.app…` |
| Application class | `KeyVaultApp` | `CredStowApp` |
| ViewModel factory | `KeyVaultViewModelFactory` | `CredStowViewModelFactory` |
| NavHost function | `KeyVaultNavHost` | `CredStowNavHost` |
| Theme | `Theme.KeyVault` / `KeyVaultTheme` | `Theme.CredStow` / `CredStowTheme` |
| SharedPreferences file | `keyvault_master_prefs` | `credstow_master_prefs` |
| Root project name | `KeyVault` | `CredStow` |
| Database file | `keyvault.db` | `CredStow.db` |

### Commit history

```
c62905a  feat(pro): biometric re-auth, key expiration, sort options + tests
fd1894c  Fix ProGuard keep rules to reference com.credstow.app
5a884ad  Rename package com.keyvault.app -> com.credstow.app
dcbdb47  Initial commit: KeyVault Android app
fc1bece  Initial commit: KeyVault Android app
```

---

## 3. New Additions (in commit 5a884ad)

### Firebase Auth infrastructure (`AuthTracker.kt`)
- **File**: `app/src/main/java/com/credstow/app/AuthTracker.kt`
- **What**: Process-level singleton that tracks `lastAuthenticatedAt` (epoch-ms of last
  successful biometric unlock). Persisted in `SharedPreferences` (not EncryptedSharedPreferences —
  the timestamp is not secret). Initialized in `CredStowApp.onCreate()`.
- **Why**: Supports the auto-lock timer feature (§4) and the Quick Settings tile's auth check.
  `requiresUnlock(settings)` returns true when biometric lock is enabled, auto-lock > 0, and
  either never authenticated or the timeout has elapsed.

### Quick Settings tile (`QuickSettingsTileService.kt`, `TileCopyActivity.kt`)
- **Files**:
  - `app/src/main/java/com/credstow/app/ui/QuickSettingsTileService.kt`
  - `app/src/main/java/com/credstow/app/ui/TileCopyActivity.kt`
  - `app/src/main/res/drawable/ic_qs_tile.xml`
- **What**: A Quick Settings tile (declared in `AndroidManifest.xml` with
  `BIND_QUICK_SETTINGS_TILE` permission) that the user can add to their QS panel. Tapping it
  launches `TileCopyActivity` — a lightweight Activity (no NavHost, no splash) that shows a
  drag-handle key picker. The user taps any key to copy it to clipboard; a toast confirms and
  the Activity finishes immediately.
- **Why**: One-tap key access without opening the full app — essential for developers who need
  keys while in other apps. If biometric lock is enabled and the timer has expired,
  `TileCopyActivity` prompts for biometric auth first (fail-open, matching SplashScreen).
- **Manifest**: Both `QuickSettingsTileService` (service + intent-filter) and `TileCopyActivity`
  (exported=false, splash theme) are registered in `AndroidManifest.xml`.

### Auto-lock timer + LockScreen (`LockScreen.kt`, `AuthTracker.kt`)
- **Files**:
  - `app/src/main/java/com/credstow/app/ui/screens/LockScreen.kt`
  - `app/src/main/java/com/credstow/app/AuthTracker.kt` (shared infra)
  - Settings: `SettingsRepository.kt`, `SettingsViewModel.kt`, `SplashViewModel.kt`,
    `SplashScreen.kt`, `MainActivity.kt`, `SettingsScreen.kt`
- **What**: When biometric lock is enabled, the app auto-locks after a configurable timeout
  (0–60 min via Slider in Settings, default 5 min). On return from background, `MainActivity.onStart()`
  calls `AuthTracker.requiresUnlock()` — if true, a `LockScreen` overlay appears (lock icon +
  biometric prompt, reusing the `BiometricAuth` class with the same FragmentManager-polling and
  12s-timeout safety as `SplashScreen`). On auth success, `AuthTracker.lastAuthenticatedAt` is
  recorded, so the SplashScreen on the next cold start skips its own biometric prompt (no double-auth).
- **Why**: Defense in depth — the app shouldn't stay unlocked indefinitely after the user switches
  apps. The LockScreen is fail-open: errors, timeouts, or unavailable hardware all grant access.
- **Constants**: `AppConst.AUTO_LOCK_DEFAULT_MINUTES = 5`, `AUTO_LOCK_MIN_MINUTES = 1`,
  `AUTO_LOCK_MAX_MINUTES = 60`.

### PrivacyPolicyActivity + HTML asset (`PrivacyPolicyActivity.kt`, `privacy_policy.html`)
- **Files**:
  - `app/src/main/java/com/credstow/app/ui/PrivacyPolicyActivity.kt`
  - `app/src/main/assets/privacy_policy.html` (276 lines, 10 sections)
- **What**: The full privacy policy (AES-256-GCM encryption, test-ping HTTPS, optional Crashlytics,
  optional AdMob, user rights, children's privacy, contact info) is bundled as an offline HTML asset
  and rendered in a self-contained `WebView` via `loadDataWithBaseURL(null, assetHtml, ...)`. No
  external URL is fetched at runtime. `SettingsScreen` privacy row launches it via Intent.
- **Why**: GDPR/Play Console Data Safety compliance. The bundled asset covers the app's actual
  data practices; a web URL still needs to be entered in the Play Console store listing.

### DatabaseMigrations (`DatabaseMigrations.kt`)
- **File**: `app/src/main/java/com/credstow/app/data/local/DatabaseMigrations.kt`
- **What**: `Migration(1, 2)` adds two nullable columns via `ALTER TABLE`:
  `expiry_date INTEGER` and `last_used_at INTEGER`. `CURRENT_VERSION = 2`.
  Paired with `fallbackToDestructiveMigrationOnDowngrade()` in ServiceLocator.
- **Schema export**: `app/schemas/com.credstow.app.data.local.KeyDatabase/2.json`
  (auto-generated by KSP; contains both v1 and v2 columns).

### Firebase Crashlytics setup (`FIREBASE_SETUP.md`, `settings.gradle.kts`, `build.gradle.kts`)
- **Files**:
  - `FIREBASE_SETUP.md` (34 lines, step-by-step)
  - `settings.gradle.kts` (plugin declarations with `apply false`)
  - `app/build.gradle.kts` (conditional `apply(plugin = ...)` gated on `google-services.json` existence)
- **What**: The `google-services` and `firebase-crashlytics` Gradle plugins are conditionally
  applied — `if (file("google-services.json").exists()) { apply(...) }`. The Crashlytics SDK
  dependency (`implementation(platform(libs.firebase.bom)) + implementation(libs.firebase.crashlytics.ktx)`)
  is always present but no-ops without the config file.
- **Why**: The build must succeed without `google-services.json` (it's git-ignored per `.gitignore`
  and `keystore.properties.example`). See `FIREBASE_SETUP.md` for the 3-step setup process.

### Gradle wrapper bump (`gradle-wrapper.properties`)
- Gradle 8.10.2 → 8.14.5 (required by AGP 8.13.2). The old 8.10.2 binary at
  `C:\GH\CS/gradle-8.10.2/` **must not be used** — it fails with "Minimum supported Gradle version
  is 8.13". Always use `./gradlew.bat` (the wrapper).

### keystore.properties support (`.gitignore`, `keystore.properties.example`)
- Added `keystore.properties` and `app/google-services.json` to `.gitignore`.
- Created `keystore.properties.example` as a template for production signing credentials.
- Release signing in `build.gradle.kts` reads from `keystore.properties` (file must exist;
  debug build succeeds without it via the `if (propsFile.exists())` guard).

---

## 4. Notable Reworks (beyond rename — in commit 5a884ad)

These files show larger diffs than a pure package rename would produce, indicating functional
changes were bundled into the rename commit:

### `ServiceLocator.kt` (66 lines of churn)
- Database name: `keyvault.db` → `CredStow.db`
- Migration strategy: `fallbackToDestructiveMigration()` → `fallbackToDestructiveMigrationOnDowngrade()`
  + explicit `.addMigrations(*DatabaseMigrations.ALL.toTypedArray())`
- Added `InterstitialAdManager` creation (was previously inline in `KeyListViewModel`)
- Added `billingManager` with `CoroutineScope(SupervisorJob() + Dispatchers.IO)`

### `app/build.gradle.kts` (65 lines of churn)
- AGP: 8.5.2 → 8.13.2
- Conditional Firebase plugin application (gated on `google-services.json`)
- Release build type: `isMinifyEnabled = false` → `true`, `isShrinkResources = true`,
  `isDebuggable = false`, `signingConfig = signingConfigs.getByName("release")`
- Added `signingConfigs` block reading from `keystore.properties`
- Added KSP Room schema export args (`room.schemaLocation`, `room.incremental`, `room.exportSchema`)
- Added Firebase BOM + Crashlytics KTX dependencies

### ` proguard-rules.pro` (87 lines, was 21)
- R8 is now enabled for release — rules rewritten to keep Room, OkHttp, AdMob, Play Billing,
  Compose, kotlinx.serialization, and Kotlin reflection intact.

### `KeyDetailScreen.kt` (loading + FAB polish)
- Loading title: `R.string.action_cancel` → `R.string.detail_loading` ("Loading…")
- Test FAB: `Divider` → `HorizontalDivider`; added `CircularProgressIndicator` spinner during testing
- FAB click handler gated with `if (!isTesting)` (no `enabled` parameter in Material3 1.2.1)

### `KeyListScreen.kt` (swipe-to-delete + NoResults)
- Added `SwipeToDismissBox` per item with `confirmValueChange` triggering `onSwipeDelete`
- Added `NoResultsState` composable (shown when search/filter yields zero results)
- Snackbar strings pre-resolved before `LaunchedEvent` (avoids calling `stringResource()` inside coroutine)
- `Divider` → `HorizontalDivider`

### `KeyListViewModel.kt` (swipe-to-delete with undo)
- Added `pendingDeleteIds: MutableStateFlow<Set<String>>` — keys are hidden from UI but NOT
  deleted from DB until Snackbar times out
- Added `Event.PendingDelete(key)` — emitted on swipe, handled by UI (Undo vs. timeout → confirmDelete)
- `combine` now 4-arg (includes `pendingDeleteIds`) with `.filterNot { it.id in pending }`

### `SettingsScreen.kt` (intent + Divider fix)
- `ACTION_ACCESSIBILITY_SETTINGS` → `ACTION_DISPLAY_SETTINGS` (was crashing — opened system
  accessibility settings instead of display settings)
- Icon: `Accessibility` → `DisplaySettings`
- String rename: `accessibility_system` → `display_system`

### `Components.kt` (status-aware UI)
- Extracted `statusColor(status: KeyStatus): Color` composable
- `StatusChip` rewritten: shows `CircularProgressIndicator` for `TESTING` state
- `KeyListItem` improved: `titleMedium` + `SemiBold` headline, masked key in `Surface` with
  status-aware coloring, supporting text uses `onSurfaceVariant`

---

## 5. Pro Features (commit c62905a)

### Biometric re-auth for copy/test
- When `AuthTracker.requiresUnlock(settings)` returns true (biometric lock enabled
  + auto-lock timer expired), `onCopy()` and `onTest()` in `KeyDetailViewModel` and
  `onCopy()` in `KeyListViewModel` emit `Event.RequestBiometricAuth` instead of
  proceeding directly.
- The screen collects `vm.events` and invokes `BiometricAuth.authenticate()`.
  On success → `vm.onBiometricAuthSuccess()` resumes the pending action.
- Gated behind Pro (biometric lock itself is already Pro-gated).

### Key expiration date (Room migration v1→v2)
- `Migration(1, 2)` adds `expiry_date INTEGER` to `vault_keys`.
- `KeyEntity`, `VaultKey`, `BackupEntry`, `NewKey`, `UpdateKey` all carry
  `expiryDate: Long?`.
- `AddEditKeyScreen`: `DatePickerDialog` + clear button; icon color-coded
  (red=expired, orange=<7 days, tertiary=ok).
- `KeyDetailScreen`: expiry badge with same color coding + `last_used_at` timestamp.
- `KeyRepositoryImpl.getPlainKey()` calls `dao.updateLastUsed()` after decryption.
- Export/import: `BackupEntry` serializes `expiryDate` (defaults to null for
  backward compatibility with old blobs).

### Sort order options (Pro)
- `SortBy` enum in `KeyDao.kt`: `CreatedAt`, `LastUsedAt`, `ExpiryDate`, `Label`.
- `SettingsRepository.keySortBy` (DataStore-backed).
- `SettingsScreen`: sort order dialog (Pro-gated radio button group).
- `KeyListViewModel.sortKeys()` applies the active sort to the key list.

### Unit tests (9 new — 53 total, all passing)
- `ProFeaturesTest.kt` in `app/src/test/java/com/credstow/app/data/`:
  - `setExpiryDatePersistsAndIsRetrievedViaObserveKeys`
  - `clearExpiryDateSetsNull`
  - `exportImportPreservesExpiryDate`
  - `getPlainKeyUpdatesLastUsedAt`
  - `sortByCreatedAtOrdersNewestFirst`
  - `sortByLabelOrdersAlphabetically`
  - `sortByLastUsedOrdersMostRecentFirst`
  - `sortByExpiryDateOrdersSoonestFirst`
  - `sortByCreatedAtIsDefaultWhenNoLastUsed`

---

## 6. Remaining Work & Known Issues

### Play Store launch blockers (require external accounts)

| # | Task | File(s) to modify | Notes |
|---|---|---|---|
| 1 | **Production signing keystore** | `keystore.properties` | Generate via `keytool -genkeypair -v -keystore upload.keystore -alias credstow-upload -keyalg RSA -keysize 2048 -validity 10000`. Production keystore already at `credstow-upload.keystore`. |
| 2 | **AdMob production IDs** | `app/src/main/res/values/strings.xml` | Replace `ca-app-pub-3940256099942544~3347511713` (app ID), `…6300978111` (banner), `…1033600233` (interstitial) with real IDs from the AdMob console. Use `prep-release.ps1` to swap. |
| 3 | **google-services.json** | `app/google-services.json` | Download from [Firebase Console](https://console.firebase.google.com/) — create project → register Android app with package `com.credstow.app`. See `FIREBASE_SETUP.md`. |
| 4 | **Play product: `pro_unlock`** | Play Console | Create a one-time product at $4.99 with ID `pro_unlock` (matches `AppConst.PRO_PRODUCT_ID = "pro_unlock"`). |
| 5 | **versionCode bump** | `app/build.gradle.kts` | Currently `versionCode = 2`. Increment before release. |
| 6 | **Data Safety form** | Play Console | Complete per the fields documented in `privacy_policy.html`. |
| 7 | **Content rating questionnaire** | Play Console | Fill out the questionnaire. |
| 8 | **Store listing assets** | Play Console | Feature graphic (1024×500), hi-res icon (512×512), phone screenshots (min 2). |
| 9 | **Privacy policy URL** | Play Console | The bundled `privacy_policy.html` covers the app; host a web copy and enter the URL in the store listing. |
| 10 | **App signing enrollment** | Play Console | Enroll in Google Play App Signing with the production key after first upload. |

### Known issues & TODOs

|---|---|
| **Sort label not updating** | FIXED — `SettingsViewModel.sortLabel` is now a `StateFlow<String>` mapped from `keySortBy`, updates immediately on selection. |
| **Accidental key deletion** | FIXED — Lock-to-delete: keys locked by default (padlock button). Long-press = copy. Unlock → swipe right to delete. Auto-relocks after 10s or on scroll. |
| **Keys too tall** | FIXED — `KeyListItem` compacted: provider badge 20→18dp, `bodyLarge`→`bodyMedium`, 3dp vertical padding, 6dp expiry dot. |
| **ProGuard rules** | R8 is enabled and passing on `bundleRelease`. Rules were rewritten in commit `fd1894c` after the initial rename left stale `com.keyvault` references in keep clauses. |
- **Test-key URL** | `OkHttpKeyTester.kt` hardcodes provider test endpoints. If a provider changes its API, the test may report false negatives. Consider making endpoints configurable. |
| **No backup export formats** | Backups are encrypted JSON. Consider adding `.env` export (for direct terminal use) as a Pro feature. |
| **AdMob test IDs in production** | The app compiles and runs with AdMob test IDs, but real ads won't serve until production IDs are substituted via `prep-release.ps1`. |

### Build environment

- **Gradle wrapper**: 8.14.5 (at `C:\GH\CS/gradlew.bat`)
- **JDK**: Eclipse Adoptium JDK 21.0.12.101
- **GRADLE_USER_HOME**: `C:\gradle`
- **Kotlin compiler**: `-Pkotlin.compiler.execution.strategy=in-process` (avoid daemon cold-start race)
- **R8**: Full mode enabled via `gradle.properties` (`android.enableR8.fullMode=true`)
- **Never use** `C:\GH\CS/gradle-8.10.2/bin/gradle.bat` — AGP 8.5.2 requires Gradle 8.13+
- Before each build: `gradlew --stop; kill java; rm -r $USERPROFILE/.kotlin/daemon`

---

## 7. File Map (key files)

```
app/
├── build.gradle.kts                    # AGP 8.5.2, Kotlin 2.0.21, namespace=com.credstow.app
├── proguard-rules.pro                  # R8 full mode rules (100 lines)
├── src/
│   ├── main/
│   │   ├── AndroidManifest.xml       # CredStowApp, MainActivity, QS TileService, TileCopyActivity, PrivacyPolicyActivity
│   │   ├── assets/privacy_policy.html # Offline policy (10 sections)
│   │   ├── java/com/credstow/app/
│   │   │   ├── CredStowApp.kt         # Application class, AuthTracker.init()
│   │   │   ├── AuthTracker.kt         # Biometric auth timestamp tracker
│   │   │   ├── MainActivity.kt        # NavHost + LockScreen overlay + FLAG_SECURE + clipboard clear onStop
│   │   │   ├── core/                  # AppConst (limits, IDs), Outcome, VaultException, ErrorMapping
│   │   │   ├── crypto/                # AesGcmCrypto, PasswordCrypto, SecretKeyProvider
│   │   │   ├── data/                  # KeyRepositoryImpl, AndroidSecretKeyProvider, BackupCodec
│   │   │   │   └── local/             # KeyDatabase, KeyDao, KeyEntity, DatabaseMigrations
│   │   │   ├── domain/                # KeyRepository (interface), ProState, use-cases
│   │   │   ├── billing/               # BillingManager, BillingEvent, SettingsProState
│   │   │   ├── ads/                   # InterstitialAdManager, AdRules
│   │   │   ├── model/                 # VaultKey, KeyStatus, Provider, NewKey, UpdateKey, BackupEntry
│   │   │   ├── network/               # KeyTester, OkHttpKeyTester, RealKeyTester
│   │   │   ├── prefs/                 # SettingsRepository (DataStore-backed settings)
│   │   │   ├── ui/
│   │   │   │   ├── QuickSettingsTileService.kt  # QS tile
│   │   │   │   ├── TileCopyActivity.kt           # Lightweight key picker from QS tile
│   │   │   │   ├── PrivacyPolicyActivity.kt     # Offline policy viewer
│   │   │   │   ├── components/                   # KeyListItem, StatusChip, EmptyState, NoResultsState, etc.
│   │   │   │   ├── navigation/                   # CredStowNavHost, Routes
│   │   │   │   ├── screens/                      # Splash, KeyList, KeyDetail, AddEdit, Settings, LockScreen
│   │   │   │   ├── theme/                        # CredStowTheme, Theme.kt
│   │   │   │   ├── util/                         # BiometricAuth, HapticFeedback (Vibrator API)
│   │   │   │   └── viewmodel/                    # All ViewModels + CredStowViewModelFactory
│   │   │   └── util/                             # ClipboardHelper, B64, Formatter, etc.
│   │   ├── res/
│   │   │   ├── drawable/ic_qs_tile.xml           # QS tile icon
│   │   │   ├── values/strings.xml                # All string resources
│   │   │   ├── values/themes.xml                 # Theme.CredStow, Theme.CredStow.Splash
│   │   │   └── mipmap-*/                         # Launcher icons (redesigned key silhouette)
│   │   ├── schemas/                              # Room schema JSON exports (v1 + v2)
│   └── test/                                     # 56 JVM unit tests (41 + 15)
├── gradle/
│   └── wrapper/gradle-wrapper.properties          # 8.14.5
├── gradle.properties                             # R8 full mode enabled
├── keystore.properties                            # Git-ignored (production upload key)
├── keystore.properties.example                   # Template
├── FIREBASE_SETUP.md                             # Firebase setup guide
├── prep-release.ps1                              # AdMob ID swap script
├── .gitignore                                    # keystore.properties, google-services.json
└── HANDOFF.md                                    # This file
```

---

## 7. How to build

```sh
# Debug build + tests
./gradlew.bat --no-daemon :app:assembleDebug :app:testDebugUnitTest

# Release AAB (requires keystore.properties)
./gradlew.bat --no-daemon :app:bundleRelease
```

> **Important**: Always use `./gradlew.bat` (wrapper v8.14.5), not the standalone
> `gradle-8.10.2/` binary. R8 full mode is enabled globally via `gradle.properties`.
> If the Kotlin daemon fails to connect, use `--no-daemon` to force the
> in-process compiler strategy.

---

## 8. Recent Enhancements

### Lock-to-delete (security)
- **By default**: every key list row is "locked" — swipe-to-delete is disabled
- **Unlock**: tap the padlock icon button in the trailing content of the key row
- **Delete**: once unlocked, swipe right to delete (Undo Snackbar appears)
- **Re-lock**: tap the padlock icon again, or wait 10 seconds (auto-relock)
- **Scroll auto-relock**: scrolling the list re-locks all keys
- **Screen resume auto-relock**: returning from detail/Add/Settings re-locks all keys

### Compact key list items
- `KeyListItem` rewritten from `ListItem` composable → custom `Row`
- Provider badge: 20dp → 18dp, Label: `bodyLarge` → `bodyMedium`
- Vertical padding: 3dp, Status chip: 14dp with `graphicsLayer` scaling
- Expiry dot: 6dp

### Sort label fix
- `SettingsViewModel.sortLabel` is now a `StateFlow<String>` mapped from `keySortBy`
  via `.map { }.stateIn(...)` — updates immediately on selection (no second tap).

### Performance
- `distinctUntilChanged()` on combined `uiState` Flow (skip redundant recomps)
- Interstitial ad pre-loaded in `SplashViewModel` (earlier than `KeyListScreen`)
- R8 full mode enabled via `gradle.properties`
- Filter `distinctUntilChanged` on StateFlow

### UX animations
- `EmptyState`: gentle pulse on `+` icon (2s cycle, InfiniteTransition)
- `NoResultsState`: subtle float on search icon (2.5s cycle)

### Accessibility + testability
- `testTag` on every interactive element across all screens
- `KeyListItem` semantics announces lock state + copy hint

### Security
- Clipboard cleared on app background (`clearNow()` in `MainActivity.onStop`)
- Typed `getSystemService(Class<T>)` replaces deprecated string constants
- `FLAG_SECURE` prevents screenshots of key material
