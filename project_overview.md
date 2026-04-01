# Summ — Project Overview

Summ is the Android mobile app for the shared-household finance product in this repository. It is a Compose-first application built in Kotlin, backed by Firebase Authentication and Cloud Firestore, and organized around feature packages such as dashboard, assets, entries, categories, recurring, members, and month close.

This document describes the structure that exists today in `mobile-app`, including the quick-entry activity, Android tile integration, and home-screen widgets.

The current public positioning is open source and app-first. The app is maintained in public as a real shared-finance Android app and as an Android/Firebase learning project, not as a commercial monetization-first product.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Repository + lightweight domain use case layer |
| DI | Hilt |
| Backend | Firebase Authentication + Cloud Firestore |
| Async | Coroutines + Flow |
| Navigation | Navigation Compose |
| Widgets | Glance App Widgets |
| Tile | Android Quick Settings Tile |
| Build | Gradle + Android Gradle Plugin |
| Public hosting model | Self-hosted Firebase project per user |

All dependency versions are managed centrally in `gradle/libs.versions.toml`.

---

## High-Level Structure

The Android app lives in:

```text
mobile-app/
```

The main application source set lives in:

```text
mobile-app/app/src/main/
```

The main Kotlin package root is:

```text
app/src/main/java/com/davideagostini/summ
```

The project is Compose-first, but it is not literally XML-free:

- Compose is used for the main app UI, quick-entry flow, sheets, screens, and widget rendering logic.
- XML is still present where the Android platform requires it, especially widget metadata and preview resources under `res/xml` and `res/layout`.

---

## Package Map

```text
com.davideagostini.summ
│
├── MainActivity.kt
├── SummApp.kt
│
├── data/
├── domain/
├── tile/
├── ui/
└── widget/
```

### `data/`

The data layer contains Firebase-facing infrastructure and app repositories.

```text
data/
├── dao/              # Firestore DAO-like access classes per collection/scope
├── di/               # Hilt modules for Firebase, repositories, and app wiring
├── entity/           # App data models: Asset, Entry, Category, Household, etc.
├── firebase/         # Firestore path helpers, config, flow utilities, error mapping
├── repository/       # Repositories consumed by ViewModels and use cases
└── session/          # Session and household-scoped auth state helpers
```

Current repositories include:

- `AssetRepository.kt`
- `CategoryRepository.kt`
- `EntryRepository.kt`
- `MemberRepository.kt`
- `MonthCloseRepository.kt`
- `RecurringTransactionRepository.kt`

### `domain/`

The domain layer is intentionally small and contains app-level models/use cases that aggregate data for UI consumption.

```text
domain/
├── model/
└── usecase/
```

Notable examples:

- `HomeState.kt`
- `EntryDisplayItem.kt`
- `GetHomeDataUseCase.kt`

### `ui/`

The UI layer is feature-oriented. Each feature owns its screen, state/event types, ViewModel, and feature-scoped components.

```text
ui/
├── assets/
├── auth/
├── categories/
├── components/
├── dashboard/
├── entries/
├── entry/
├── format/
├── navigation/
├── settings/
└── theme/
```

### `tile/`

This package contains the Android Quick Settings tile integration used to jump quickly into the entry flow.

```text
tile/
└── QuickEntryTileService.kt
```

### `widget/`

This package contains the home-screen widget implementation.

```text
widget/
├── QuickAccessWidget.kt
├── QuickAccessWidgetReceiver.kt
├── RefreshSpendingWidgetAction.kt
├── SpendingSummaryWidget.kt
├── SpendingSummaryWidgetReceiver.kt
├── SummWidgetsUpdater.kt
├── components/
├── data/
└── model/
```

This is already part of the current product, not future roadmap work.

Widget follow-up note:

- The widget area should be re-reviewed before the next release iteration.
- Recent attempts to make widget refresh more aggressive/automatic were intentionally reverted because they added too much complexity and made behavior less predictable.
- Any future widget work should stay conservative, avoid extra infrastructure unless clearly justified, and start from the current simple baseline.

---

## UI Feature Breakdown

### `ui/auth`

Authentication and household setup flow.

Key files:

- `AuthGateScreen.kt`
- `SessionViewModel.kt`
- `components/SignInScreen.kt`
- `components/HouseholdSetupScreen.kt`
- `components/AuthShared.kt`

Responsibility:

- gate the app on auth + household membership
- handle Google sign-in flow
- handle create/join household onboarding
- keep create/join transitions in a dedicated loading state until the session becomes ready
- expose household-level settings updates such as the shared currency

### `ui/dashboard`

The main household overview screen.

Key files:

- `DashboardScreen.kt`
- `DashboardUiState.kt`
- `DashboardViewModel.kt`
- `DashboardModels.kt`

Responsibility:

- summarize financial health
- show high-level household metrics
- coordinate month filtering, household currency formatting, and dashboard-specific presentation

### `ui/assets`

Asset and liability management.

Key files:

- `AssetsScreen.kt`
- `AssetsUiState.kt`
- `AssetsEvent.kt`
- `AssetsViewModel.kt`
- `AssetsHelpers.kt`
- `components/*`

Responsibility:

- monthly asset snapshot browsing
- asset/liability CRUD
- copy previous month snapshot
- action sheet, editor flow, delete confirmation, and success states
- inherit the household currency instead of asking for per-asset currency input

### `ui/entries`

Entries list, filtering, action sheet, and entry editing from the main ledger screen.

Key files:

- `EntriesScreen.kt`
- `EntriesUiState.kt`
- `EntriesEvent.kt`
- `EntriesViewModel.kt`
- `EntriesModels.kt`
- `components/*`

Responsibility:

- list and group entries by day
- search/filter by month and entry type
- view/edit/delete existing entries
- show unusual spending insights and monthly balance summary
- render amounts using the household currency from session state

### `ui/settings/currency`

Dedicated currency selection screen inside Settings.

Responsibility:

- list supported currencies using the same list-item language as the rest of Settings
- keep the current household currency pinned to the top
- provide inline search for faster selection
- update the shared household currency without introducing per-feature currency settings

### `ui/settings/language`

Dedicated app-language selection screen inside Settings.

Responsibility:

- list supported app languages using the same list style as the currency screen
- keep the language list in a stable alphabetical order while highlighting the current selection
- keep the remaining languages sorted alphabetically
- switch the app locale through Android per-app language APIs

### `ui/settings/theme`

Dedicated app-theme selection screen inside Settings.

Responsibility:

- list the three supported app themes using the same list style as the currency and language screens
- keep the theme list in a stable order while highlighting the current selection
- keep the remaining theme options sorted alphabetically
- persist the app theme as `light`, `dark`, or `system`
- apply the saved preference through Android night-mode APIs before the Compose tree is rendered

### Recurring auto-apply

Recurring items are also checked automatically on app startup after the session reaches `Ready`.

Responsibility:

- run a silent due-recurring sync for the current household
- limit the automatic sync to at most once per day per household on the device
- keep the operation idempotent by relying on the existing recurring-to-entry duplicate guard
- avoid background scheduling complexity until a stronger automation model is needed

### `ui/entries` reports mode

The entries screen also includes a secondary reports presentation mode for the selected month.

Responsibility:

- reuse the existing entries screen shell instead of introducing a separate navigation destination
- let the toolbar toggle between the standard grouped transaction list and the reports view
- show a compact horizontal bar chart for the top expense categories of the selected month
- summarize expense categories for the selected month with totals, averages, percentages, and transaction counts
- keep the visual treatment aligned with the existing entries cards and settings surfaces

### `ui/entry`

Dedicated quick-entry flow for creating a new transaction.

Key files:

- `QuickEntryActivity.kt`
- `QuickEntryScreen.kt`
- `QuickEntryViewModel.kt`
- `EntryEvent.kt`
- `EntryNavEvent.kt`
- `components/Step*.kt`

Responsibility:

- multi-step entry creation flow
- reusable quick capture entry point for activity, tile, and widget-triggered flows

### `ui/categories`

Category management.

Key files:

- `CategoriesScreen.kt`
- `CategoriesUiState.kt`
- `CategoriesEvent.kt`
- `CategoriesViewModel.kt`
- `components/*`

Responsibility:

- category CRUD
- emoji/category presentation
- action sheet, editor, delete confirmation, success messaging

### `ui/settings`

Settings and sub-features.

```text
settings/
├── SettingsScreen.kt
├── components/
├── language/
├── members/
├── monthclose/
└── recurring/
```

#### `settings/members`

Household member and invite management.

#### `settings/recurring`

Recurring transaction management and apply-due flow.

#### `settings/monthclose`

Month close review and read-only period handling.

### `ui/components`

Shared Compose primitives reused across multiple features.

Examples:

- `DeleteConfirmationDialog.kt`
- `MonthPickerField.kt`
- `MonthCloseReadOnlyBanner.kt`
- `MonthOptionUtils.kt`
- `LoadingState.kt`

### `ui/navigation`

App-level navigation orchestration.

Key file:

- `AppNavGraph.kt`

Responsibility:

- auth vs main-app routing
- bottom navigation container
- screen registration
- cross-screen overlay coordination such as fullscreen flows and month-picker visibility

### `ui/theme`

Shared design system primitives.

Key files:

- `Theme.kt`
- `Color.kt`
- `Shape.kt`
- `Type.kt`

Responsibility:

- Material theme wiring
- light/dark color schemes
- typography
- reusable shapes such as segmented list item shapes

---

## Widget and Tile Structure

The mobile app already includes two different Android surface integrations outside the main navigation flow.

### Quick Settings Tile

`tile/QuickEntryTileService.kt` exposes a Quick Settings tile that launches the quick-entry flow.

This is a system integration point, separate from Navigation Compose.

### Home-Screen Widgets

The widget package currently contains:

- `QuickAccessWidget.kt`
- `SpendingSummaryWidget.kt`
- `SummWidgetsUpdater.kt`
- `RefreshSpendingWidgetAction.kt`
- widget receivers for both widget families
- shared UI/widget components in `widget/components`
- presentation and layout models in `widget/model`
- data loading in `widget/data/SpendingSummaryWidgetDataSource.kt`

Supporting resources exist in:

```text
res/xml/
├── quick_access_widget_info.xml
└── spending_summary_widget_info.xml

res/layout/
├── widget_quick_access_preview.xml
└── widget_spending_summary_preview.xml
```

This means the current project already supports:

- quick access widget surfaces
- a spending summary widget
- widget refresh/update orchestration
- widget previews and metadata required by the launcher

---

## Architecture

The app follows a pragmatic MVVM structure with clear directional flow:

```text
Composable UI
  -> sends Events
ViewModel
  -> updates immutable UI state
Repository / Use Case
  -> reads/writes Firebase-backed data
Firebase / Firestore / Session layer
```

Important conventions used across the app:

- each feature keeps screen orchestration in `XxxScreen.kt`
- screen-local state is handled with Compose `remember`
- business state is owned by a `ViewModel` via `MutableStateFlow`
- heavy derived UI data is exposed as immutable `renderState` from the `ViewModel`
- UI reads state with `collectAsStateWithLifecycle()`
- feature-specific presentational pieces live under `components/`
- delete confirmation and success states are rendered in the same flow pattern across features where possible
- composables should avoid recomputing filtered lists, totals, chart series, or grouped display models when that work can be prepared upstream

---

## Resource Structure

Relevant non-code resources include:

```text
res/
├── drawable/
├── font/
├── layout/    # widget preview XML only
├── mipmap-*/
├── values/
└── xml/       # widget provider metadata
```

This keeps the main app UI Compose-driven while still supporting the Android resource types required for launcher and system integration.

---

## Notes About Current Scope

The existing mobile app already covers the main household finance flows:

- authentication and household setup
- dashboard
- assets
- entries
- categories
- recurring transactions
- month close
- household members/invites
- quick entry
- quick settings tile
- home-screen widgets

So `project_overview.md` should be read as documentation of an already fairly broad product surface, not as a greenfield starter skeleton.

---

## Suggested Next Features

The following additions would improve product polish, onboarding, and feedback loops without changing the core household-finance scope:

- add a theme toggle for `light` / `dark` / `system` mode and persist the user preference
- provide a dynamic walkthrough for new users based on their current state, for example first sign-in, empty household, or partially configured setup
- implement reports or summaries for monthly review, category spending, and higher-level household trends
- add a user feedback mechanism so people can quickly send bug reports, improvement ideas, or general product feedback from inside the app

---

## Verification

Use this to validate Android changes:

```bash
./gradlew assembleDebug
```

That is the baseline verification command used in this project after UI, navigation, widget, or state-management changes.
