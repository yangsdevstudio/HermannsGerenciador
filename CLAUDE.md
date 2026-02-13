# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

This is a single-module Android project using Gradle with Kotlin DSL.

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device/emulator
./gradlew installDebug

# Clean build
./gradlew clean

# Run lint checks
./gradlew lint
```

There are no unit tests configured in the project currently.

**Build targets:** compileSdk 35, minSdk 26, targetSdk 35, Java 17, Kotlin 2.0.21.

## Architecture

MVVM architecture with Jetpack Compose UI. The app manages pharmaceutical inventory by fetching medication data from a Google Sheets document, parsing CSV exports, and displaying them with expiry date tracking and notifications.

### Data Flow

Google Sheets API → `SheetsApiRepository` (HTTP fetch + local file cache) → `CsvParser` (flexible date/column parsing) → `Medication` data objects → `ValidadeViewModel` (StateFlow) → Compose screens

### Key Layers

- **`data/`** — `Medication` data class with `daysUntilExpiry()` calculation using `java.time.LocalDate`
- **`repo/`** — `SheetsApiRepository` fetches CSV from Google Sheets via OkHttp, caches to `medications_cache.csv`, supports multiple sheet tabs
- **`net/`** — `CsvParser` handles flexible CSV formats with intelligent header detection and multiple date format support (dd/MM/yyyy, MM/yyyy, etc.)
- **`viewmodel/`** — `ValidadeViewModel` exposes `StateFlow<List<Medication>>` plus loading/error states; `ValidadeViewModelFactory` for construction
- **`ui/screens/`** — Compose screens: `HomeScreen` (landing), `ValidadeScreen` (main inventory list with search/filter/sort), `PromoScreen` (zoomable promo image from GitHub), `WelcomeScreen`
- **`ui/navigation/`** — `AppNavGraph` uses `ModalNavigationDrawer` with routes: `"home"`, `"validade"`, `"promocoes"`, `"relatorios"`
- **`util/`** — `NotificationHelper` (expiry alerts, daily scheduling at 8:15 AM, SharedPreferences tracking), `DailyExpiryReceiver` (BroadcastReceiver for alarm-triggered checks)

### Important Patterns

- **API keys are in `build.gradle.kts`** as `BuildConfig` fields (`SHEETS_API_KEY`, `SPREADSHEET_ID`)
- **Expiry color coding** in `MedicationRow`: red (≤15 days), orange (≤30), yellow (≤60), lime (≤90), green (>90)
- **PromoScreen** uses `FileProvider` (configured in `xml/provider_paths.xml`) for sharing downloaded images
- **Navigation drawer** contains external app launch intent for "Vendedor Hermanns" app
- **Notification deduplication** via SharedPreferences set of already-notified medication identifiers
- All UI uses Material 3 with custom teal/orange light theme and cyan/blue dark theme defined in `ui/theme/`

### Dependencies

Compose BOM 2024.10.00, Navigation Compose 2.8.2, Lifecycle ViewModel Compose 2.8.6, OkHttp 4.12.0, Coil 2.7.0, KSP 2.0.21-1.0.25.

## Language

The codebase, UI strings, and comments are in Brazilian Portuguese.
