# Summ — Project Overview

A personal finance tracker for Android built with **100% Kotlin + Jetpack Compose**. No XML layouts. No global singletons. Firebase Authentication and Cloud Firestore are the runtime backend, scoped by household.

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Dependencies](#dependencies)
3. [Module & Package Structure](#module--package-structure)
4. [Architecture](#architecture)
5. [Data Layer](#data-layer)
6. [Domain Layer](#domain-layer)
7. [UI Layer](#ui-layer)
8. [Navigation](#navigation)
9. [Theming](#theming)
10. [Dependency Injection](#dependency-injection)
11. [How to Add a New Feature](#how-to-add-a-new-feature)
12. [Next Steps](#next-steps)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.3.20 |
| UI | Jetpack Compose + Material Design 3 |
| Architecture | MVVM + Repository + Use Cases |
| DI | Hilt 2.59.1 |
| Backend | Firebase Authentication + Cloud Firestore |
| Async | Kotlin Coroutines + Flow |
| Navigation | Navigation Compose 2.9.0 |
| Build | Gradle 9.3.1 + AGP 9.1.0 (built-in Kotlin) |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 36 |

---

## Dependencies

All versions are managed centrally in `gradle/libs.versions.toml`.

```toml
[versions]
agp                  = "9.1.0"
kotlin               = "2.3.20"
ksp                  = "2.3.6"
hilt                 = "2.59.1"
hiltNavigationCompose = "1.2.0"
composeBom           = "2026.03.00"
activityCompose      = "1.12.3"
navigationCompose    = "2.9.0"
lifecycle            = "2.9.0"
room                 = "2.8.4"
coreKtx              = "1.16.0"
appcompat            = "1.7.0"
coroutines           = "1.10.2"
firebaseBom          = "34.3.0"
credentials          = "1.3.0"
googleId             = "1.1.1"
```

### Key notes

- **Compose BOM** pins all `androidx.compose.*` artifact versions — never set individual Compose library versions.
- **KSP** (Kotlin Symbol Processing) replaces KAPT for annotation processing (Hilt). Faster incremental builds.
- **AGP 9.1.0** uses built-in Kotlin support — the `org.jetbrains.kotlin.android` plugin is no longer applied. Only `kotlin.compose` and `ksp` are applied alongside AGP.
- **Hilt 2.59+** is required for AGP 9 built-in Kotlin compatibility.
- **Credential Manager + googleid** replace the deprecated Google Sign-In client for Google authentication.

---

## Next Steps

- Verify end-to-end `create household` and `join household` flows with two users on the same Firebase project.
- Verify Firestore CRUD for `categories` and `transactions`, including live updates across household members.
- Finish parity gaps with the web app around richer settings/admin flows and edge-case validation.
- Verify the new Credential Manager sign-in flow on multiple devices/accounts.

### Future roadmap

- Add Android home screen widgets for key household metrics and quick entry actions.
- Add Wear OS support for glanceable metrics and fast transaction capture.
- Add AI-powered features for insights, categorization help, unusual-spending explanations, and natural-language entry assistance.
- Add import/export data flows for backup, migration, and interoperability.
- Add recurring reminders and smart notifications for due recurring entries and monthly close tasks.
- Add household activity history and audit trail for shared changes.
- Add attachment support for receipts and relevant documents.
- Add budget goals and progress tracking by category and month.
- Add better onboarding, sample data, and demo mode for first-time users.

---

## Module & Package Structure

The project is a single-module app. All source lives under:

```
app/src/main/java/com/quickledger/app/
```

```
com.davideagostini.summ
│
├── MainActivity.kt              # App entry point, edge-to-edge setup
├── SummApp.kt                   # @HiltAndroidApp Application class
│
├── data/                        # Data layer (Firestore + Repositories)
│   ├── dao/
│   │   ├── AssetDao.kt
│   │   ├── CategoryDao.kt
│   │   ├── EntryDao.kt
│   │   ├── InviteDao.kt
│   │   ├── MemberDao.kt
│   │   ├── MonthCloseDao.kt
│   │   └── RecurringTransactionDao.kt
│   ├── di/
│   │   ├── AppScopeModule.kt
│   │   ├── DatabaseModule.kt    # Provides repositories / DAOs
│   │   └── FirebaseModule.kt    # FirebaseAuth, Firestore, default_web_client_id
│   ├── entity/
│   │   ├── AppUser.kt
│   │   ├── Asset.kt
│   │   ├── AssetHistoryEntry.kt
│   │   ├── Category.kt
│   │   ├── Entry.kt
│   │   ├── Household.kt
│   │   ├── Invite.kt
│   │   ├── Member.kt
│   │   ├── MonthClose.kt
│   │   └── RecurringTransaction.kt
│   ├── firebase/
│   │   ├── FirebaseConfig.kt
│   │   ├── FirestoreFlow.kt
│   │   └── FirestorePaths.kt
│   ├── session/
│   │   ├── SessionRepository.kt
│   │   └── SessionState.kt
│   └── repository/
│       ├── AssetRepository.kt
│       ├── CategoryRepository.kt
│       └── EntryRepository.kt
│
├── domain/                      # Business logic (no Android dependencies)
│   ├── model/
│   │   ├── EntryDisplayItem.kt
│   │   └── HomeState.kt
│   └── usecase/
│       └── GetHomeDataUseCase.kt
│
├── tile/
│   └── QuickEntryTileService.kt
│
└── ui/
    ├── assets/
    │   ├── AssetsScreen.kt
    │   ├── AssetsUiState.kt
    │   └── AssetsViewModel.kt
    ├── auth/
    │   ├── AuthGateScreen.kt
    │   └── SessionViewModel.kt
    ├── categories/
    │   ├── CategoriesEvent.kt
    │   ├── CategoriesScreen.kt
    │   ├── CategoriesUiState.kt
    │   ├── CategoriesViewModel.kt
    │   └── components/
    │       ├── CategoryActionSheet.kt   # Add / Edit / Delete sheet
    │       ├── CategoryCard.kt
    │       └── EmojiPickerGrid.kt       # 9 sections × 16 emojis, 6-column grid
    ├── dashboard/
    │   ├── DashboardScreen.kt
    │   ├── DashboardUiState.kt
    │   └── DashboardViewModel.kt
    ├── entries/
    │   ├── EntriesEvent.kt
    │   ├── EntriesScreen.kt
    │   ├── EntriesUiState.kt
    │   ├── EntriesViewModel.kt
    │   └── components/
    │       ├── BalanceCard.kt
    │       ├── EmptyState.kt
    │       ├── EntryActionSheet.kt  # View / Edit / Delete sheet
    │       └── EntryCard.kt
    ├── entry/                   # Quick-add flow (bottom sheet / tile activity)
    │   ├── EntryEvent.kt
    │   ├── EntryNavEvent.kt
    │   ├── QuickEntryActivity.kt
    │   ├── QuickEntryScreen.kt
    │   ├── QuickEntryViewModel.kt
    │   └── components/
    │       ├── StepAmount.kt
    │       ├── StepCategory.kt
    │       ├── StepDescription.kt
    │       ├── StepReview.kt
    │       ├── StepShared.kt    # StepIndicator, StepTitle, StepNavRow
    │       ├── StepSuccess.kt
    │       └── StepType.kt
    ├── navigation/
    │   └── AppNavGraph.kt
    ├── settings/
    │   ├── SettingsScreen.kt
    │   └── components/
    │       └── SettingsItem.kt
    └── theme/
        ├── Color.kt             # M3 color schemes + IncomeGreen, ExpenseRed
        ├── Shape.kt             # listItemShape() for segmented list items
        ├── Theme.kt             # SummTheme, dynamic color support
        └── Type.kt              # GoogleSansFlex FontFamily + AppTypography
```

---

## Architecture

Summ follows **MVVM + Repository** with clean-architecture-inspired layer separation. The dependency rule flows strictly downward:

``` 
UI  →  ViewModel  →  Use Case / Repository  →  DAO / Session Repo  →  Firestore / Firebase Auth
```

No layer references anything above it. The domain layer (`domain/`) has zero Android dependencies.

### Unidirectional Data Flow (UDF)

Every screen follows the same pattern:

```
User action
    │
    ▼
Composable calls onEvent(XxxEvent.Something)
    │
    ▼
ViewModel.handleEvent(event) mutates _uiState via MutableStateFlow.update { }
    │
    ▼
StateFlow emits new immutable state
    │
    ▼
Composable recomposes
```

### State

- All UI state is an `@Immutable data class` — guarantees Compose strong-skipping.
- One `MutableStateFlow<XxxUiState>` per ViewModel, exposed as `StateFlow` via `.asStateFlow()`.
- List data from Firestore-backed flows is typically converted to `StateFlow` with `.stateIn(viewModelScope, WhileSubscribed(5_000), emptyList())`.

### Events

- User interactions are modelled as a `sealed class XxxEvent`.
- One `handleEvent(event: XxxEvent)` dispatcher in the ViewModel — no individual `onXxx()` lambdas on the ViewModel.
- One-shot navigation events use `Channel<XxxNavEvent>(BUFFERED)` exposed as a `receiveAsFlow()`, preventing dropped events on configuration change.

### Authentication and session

- Google sign-in uses **Credential Manager** with `GetGoogleIdOption`.
- The Google ID token is exchanged with **FirebaseAuth** using `GoogleAuthProvider`.
- `SessionRepository` is the single source of truth for:
  - signed-out state
  - loading state
  - needs-household gating
  - ready state with resolved household
- Household data is never shown until the user document and household document are both resolved.

### Success feedback pattern

Operations that modify data (insert, update, delete) always:

```kotlin
viewModelScope.launch {
    repository.doSomething(...)
    _uiState.update { it.copy(sheetMode = XxxSheetMode.Success) }
    delay(1_500L)
    _uiState.update { XxxUiState() }   // reset to default (closes sheet)
}
```

---

## Data Layer

### Entities

| Entity | Table | Fields |
|---|---|---|
| `Entry` | `households/{householdId}/transactions/{entryId}` | `id`, `type`, `description`, `price`, `category`, `date`, timestamps |
| `Category` | `households/{householdId}/categories/{categoryId}` | `id`, `name`, `emoji`, `type` |
| `Asset` | `households/{householdId}/assets/{assetId}` | `id`, `name`, `type`, `history`, metadata |
| `Member` | `households/{householdId}/members/{userId}` | `userId`, `role`, joinedAt |
| `Invite` | `households/{householdId}/invites/{inviteId}` | invite metadata |

Category is referenced from entries by value, not by Firestore reference. This keeps historical entries stable if a category is renamed or deleted later.

### DAOs

Each DAO exposes:
- Suspension functions for mutations
- `Flow<>` for observations through Firestore snapshot listeners

Firestore-backed DAOs in this project are classes, not Room interfaces. They expose snapshot-based flows plus suspend mutations scoped to the current household.

### Repositories

Thin wrappers over DAOs. No business logic here — that belongs in use cases.

```kotlin
@Singleton
class EntryRepository @Inject constructor(private val entryDao: EntryDao) {
    val allEntries: Flow<List<Entry>> = entryDao.getAllEntries()
    val balance: Flow<Double?>        = entryDao.getBalance()

    suspend fun insert(entry: Entry) = entryDao.insert(entry)
    suspend fun update(entry: Entry) = entryDao.update(entry)
    suspend fun delete(entry: Entry) = entryDao.delete(entry)
}
```

### Default category seeding

Default categories are inserted when a household is created inside `SessionRepository.createHousehold(...)`, so every household starts with the same baseline category set.

---

## Domain Layer

The domain layer is pure Kotlin — no Android SDK imports.

### Models

| Class | Purpose |
|---|---|
| `EntryDisplayItem` | UI-ready version of `Entry` — includes `emoji` resolved from the category map |
| `HomeState` | Aggregated state for the Entries screen: `entries: List<EntryDisplayItem>` + `balance: Double` |

### Use Cases

Use cases are created when a ViewModel needs to coordinate multiple repositories.

```kotlin
class GetHomeDataUseCase @Inject constructor(
    private val entryRepository: EntryRepository,
    private val categoryRepository: CategoryRepository,
) {
    operator fun invoke(): Flow<HomeState> = combine(
        entryRepository.allEntries,
        categoryRepository.allCategories,
        entryRepository.balance,
    ) { entries, categories, balance ->
        val emojiMap = categories.associate { it.name to it.emoji }
        HomeState(
            entries = entries.map { entry ->
                EntryDisplayItem(/* ... */, emoji = emojiMap[entry.category] ?: "📦")
            },
            balance = balance ?: 0.0,
        )
    }
}
```

**Rule:** Create a use case when a ViewModel touches more than one repository, or when the transformation logic is non-trivial.

---

## UI Layer

### Screen anatomy

Every screen follows this file structure:

```
featureX/
├── FeatureXEvent.kt       # sealed class — all user interactions
├── FeatureXUiState.kt     # @Immutable data class + optional enum SheetMode
├── FeatureXViewModel.kt   # @HiltViewModel, exposes StateFlow<FeatureXUiState>
├── FeatureXScreen.kt      # Composable, splits into Screen() + Content()
└── components/
    └── FeatureXSomeComponent.kt
```

### Screen vs Content split

```kotlin
// Public composable — owns the ViewModel
@Composable
fun FeatureXScreen(viewModel: FeatureXViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FeatureXContent(state = state, onEvent = viewModel::handleEvent)
}

// Private composable — pure UI, easy to preview
@Composable
private fun FeatureXContent(state: FeatureXUiState, onEvent: (FeatureXEvent) -> Unit) { ... }
```

### Bottom sheet pattern

Transparent `ModalBottomSheet` so only the inner `Card` has a background:

```kotlin
ModalBottomSheet(
    onDismissRequest = { onEvent(XxxEvent.DismissSheet) },
    sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor   = Color.Transparent,
    dragHandle       = null,
    scrimColor       = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
) {
    XxxActionSheet(uiState = uiState, onEvent = onEvent)
}
```

Inside the sheet, `AnimatedContent` drives transitions between modes (Action → Edit → Success):

```kotlin
AnimatedContent(
    targetState    = uiState.sheetMode,
    transitionSpec = {
        val forward = targetState.ordinal > initialState.ordinal
        if (forward) (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
        else         (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
    },
) { mode -> when (mode) { ... } }
```

### Multi-step entry flow

`QuickEntryScreen` renders a single `Card` with `AnimatedContent` stepping through:

```
Step 0: Type (Income / Expense)
Step 1: Description
Step 2: Amount
Step 3: Category picker
Step 4: Review
Step 5: Success (auto-dismiss after 1.5 s)
```

Each step is its own composable in `ui/entry/components/`. Navigation between steps is driven by `EntryEvent.Next` / `EntryEvent.Back` in the ViewModel.

### BaseViewModel

```kotlin
// ui/shared/viewmodel/BaseViewModel.kt
abstract class BaseViewModel : ViewModel() {
    // Shared helpers: snackbar channel, etc.
}
```

All feature ViewModels extend `BaseViewModel`.

---

## Navigation

Navigation is entirely managed in `AppNavGraph.kt` using the Compose Navigation library.

### Routes

| Route | Screen | Start |
|---|---|---|
| `entries` | `EntriesScreen` | ✓ |
| `dashboard` | `DashboardScreen` | |
| `assets` | `AssetsScreen` | |
| `settings` | `SettingsScreen` | |
| `categories` | `CategoriesScreen` | |

### Bottom bar

A floating pill-shaped custom bottom bar with 5 tabs:

```
Dashboard (BarChart) | Entries (Receipt) | ➕ Add | Assets (Wallet) | Settings (Gear)
```

- **Selected tab**: filled `Button` (primary pill) showing icon + label text.
- **Unselected tab**: `IconButton` with dimmed icon (45% alpha).
- **Add Entry**: always unselected, opens a `ModalBottomSheet` over any route.
- The `+` button is not a navigation destination — it triggers `showEntrySheet = true`.

### Navigating to a sub-screen

```kotlin
// From SettingsScreen → CategoriesScreen
composable("settings") {
    SettingsScreen(onNavigateCategories = { navigate("categories") })
}
composable("categories") {
    CategoriesScreen(onBack = { navController.popBackStack() })
}
```

---

## Theming

### Colors (`ui/theme/Color.kt`)

Full Material 3 tonal palette in both light and dark variants. Two semantic colors used throughout:

```kotlin
val IncomeGreen = Color(0xFF006B5E)   // positive amounts, success states
val ExpenseRed  = Color(0xFFBA1A1A)   // negative amounts, delete actions
```

Access top app bar colors via the helper:

```kotlin
TopAppBar(colors = SummColors.topBarColors)
```

### Typography (`ui/theme/Type.kt`)

Custom `GoogleSansFlex` font family loaded from `res/font/google_sans_flex.ttf`. Applied to all M3 text styles via `AppTypography`, wired into `MaterialTheme(typography = AppTypography)`.

### Shapes (`ui/theme/Shape.kt`)

`listItemShape(index, count)` produces iOS-style grouped list corners:

```
Single item     → 16dp all corners
First item      → 16dp top,  4dp bottom
Middle items    → 4dp  all corners
Last item       → 4dp  top, 16dp bottom
```

Use it in any list card:

```kotlin
val shape = listItemShape(index, count)
Card(shape = shape) { ... }
```

---

## Dependency Injection

Hilt is configured at three levels:

| Level | Annotation | Where |
|---|---|---|
| Application | `@HiltAndroidApp` | `SummApp` |
| Activity / composable | `@AndroidEntryPoint` | `MainActivity`, `QuickEntryActivity` |
| ViewModel | `@HiltViewModel` | All ViewModels |

### Modules

**`DatabaseModule`** (`@InstallIn(SingletonComponent)`)
- Provides Firestore-backed DAO classes and repositories
- Keeps household-scoped data access isolated from UI code

**`FirebaseModule`** (`@InstallIn(SingletonComponent)`)
- `@Provides FirebaseAuth?`
- `@Provides FirebaseFirestore?`
- `@Provides default_web_client_id`

**`AppScopeModule`** (`@InstallIn(SingletonComponent)`)
- `@Provides @ApplicationScope CoroutineScope` — `SupervisorJob + Dispatchers.IO`
- Reserved for app-scoped background work; do **not** use this for UI work.

Repositories are `@Singleton` via `@Inject constructor` — no explicit `@Provides` needed.

---

## How to Add a New Feature

The following walkthrough adds a hypothetical **"Budgets"** feature. Follow the same pattern for any new screen.

### Step 1 — Create the data model

```kotlin
// data/entity/Budget.kt
data class Budget(
    val id: String = "",
    val category: String,
    val limitAmount: Double,
    val periodDays: Int = 30,
)
```

### Step 2 — Create the DAO

```kotlin
// data/dao/BudgetDao.kt
class BudgetDao @Inject constructor(...) {
    fun getAllBudgets(): Flow<List<Budget>> = ...
    suspend fun upsert(budget: Budget) { ... }
    suspend fun delete(budgetId: String) { ... }
}
```

### Step 3 — Register the DAO in DI

```kotlin
// data/di/DatabaseModule.kt
@Provides
@Singleton
fun provideBudgetDao(...): BudgetDao {
    return BudgetDao(...)
}
```

### Step 4 — Create the repository

```kotlin
// data/repository/BudgetRepository.kt
@Singleton
class BudgetRepository @Inject constructor(private val budgetDao: BudgetDao) {
    val allBudgets: Flow<List<Budget>> = budgetDao.getAllBudgets()

    suspend fun insert(budget: Budget) = budgetDao.insert(budget)
    suspend fun update(budget: Budget) = budgetDao.update(budget)
    suspend fun delete(budget: Budget) = budgetDao.delete(budget)
}
```

### Step 5 — (Optional) Create a domain use case

Create a use case only if the ViewModel must combine data from multiple repositories.

```kotlin
// domain/usecase/GetBudgetStatusUseCase.kt
class GetBudgetStatusUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val entryRepository: EntryRepository,
) {
    operator fun invoke(): Flow<List<BudgetStatus>> = combine(
        budgetRepository.allBudgets,
        entryRepository.allEntries,
    ) { budgets, entries ->
        budgets.map { budget ->
            val spent = entries
                .filter { it.category == budget.category && it.type == "expense" }
                .sumOf { it.price }
            BudgetStatus(budget = budget, spent = spent)
        }
    }
}
```

### Step 6 — Create the UI state and events

```kotlin
// ui/budgets/BudgetsUiState.kt
enum class BudgetSheetMode { Hidden, Action, Add, Edit, Success }

@Immutable
data class BudgetsUiState(
    val sheetMode: BudgetSheetMode  = BudgetSheetMode.Hidden,
    val selectedBudget: Budget?     = null,
    val editCategory: String        = "",
    val editLimit: String           = "",
    val limitError: String?         = null,
    val showDeleteDialog: Boolean   = false,
)
```

```kotlin
// ui/budgets/BudgetsEvent.kt
sealed class BudgetsEvent {
    data class Select(val budget: Budget)       : BudgetsEvent()
    data object StartAdd                        : BudgetsEvent()
    data object StartEdit                       : BudgetsEvent()
    data object RequestDelete                   : BudgetsEvent()
    data object DismissSheet                    : BudgetsEvent()
    data class UpdateCategory(val value: String): BudgetsEvent()
    data class UpdateLimit(val value: String)   : BudgetsEvent()
    data object SaveAdd                         : BudgetsEvent()
    data object SaveEdit                        : BudgetsEvent()
    data object ConfirmDelete                   : BudgetsEvent()
    data object DismissDeleteDialog             : BudgetsEvent()
}
```

### Step 7 — Create the ViewModel

```kotlin
// ui/budgets/BudgetsViewModel.kt
@HiltViewModel
class BudgetsViewModel @Inject constructor(
    getBudgetStatus: GetBudgetStatusUseCase,
    private val repository: BudgetRepository,
) : BaseViewModel() {

    val budgets: StateFlow<List<BudgetStatus>> = getBudgetStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(BudgetsUiState())
    val uiState: StateFlow<BudgetsUiState> = _uiState.asStateFlow()

    fun handleEvent(event: BudgetsEvent) {
        when (event) {
            is BudgetsEvent.Select      -> _uiState.update {
                it.copy(sheetMode = BudgetSheetMode.Action, selectedBudget = event.budget)
            }
            BudgetsEvent.StartAdd       -> _uiState.update {
                it.copy(sheetMode = BudgetSheetMode.Add, editCategory = "", editLimit = "")
            }
            BudgetsEvent.SaveAdd        -> saveAdd()
            BudgetsEvent.ConfirmDelete  -> confirmDelete()
            BudgetsEvent.DismissSheet   -> _uiState.update { BudgetsUiState() }
            // ... other events
            else -> Unit
        }
    }

    private fun saveAdd() {
        val state = _uiState.value
        val limit = state.editLimit.toDoubleOrNull()
        if (limit == null || limit <= 0) {
            _uiState.update { it.copy(limitError = "Enter a valid amount") }
            return
        }
        viewModelScope.launch {
            repository.insert(Budget(category = state.editCategory, limitAmount = limit))
            _uiState.update { it.copy(sheetMode = BudgetSheetMode.Success) }
            delay(1_500L)
            _uiState.update { BudgetsUiState() }
        }
    }

    private fun confirmDelete() {
        val budget = _uiState.value.selectedBudget ?: return
        viewModelScope.launch {
            repository.delete(budget)
            _uiState.update { it.copy(sheetMode = BudgetSheetMode.Success, showDeleteDialog = false) }
            delay(1_500L)
            _uiState.update { BudgetsUiState() }
        }
    }
}
```

### Step 8 — Create the screen

```kotlin
// ui/budgets/BudgetsScreen.kt
@Composable
fun BudgetsScreen(viewModel: BudgetsViewModel = hiltViewModel()) {
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BudgetsContent(budgets = budgets, uiState = uiState, onEvent = viewModel::handleEvent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetsContent(
    budgets: List<BudgetStatus>,
    uiState: BudgetsUiState,
    onEvent: (BudgetsEvent) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ... scaffold, list, FAB

    if (uiState.sheetMode != BudgetSheetMode.Hidden) {
        ModalBottomSheet(
            onDismissRequest = { onEvent(BudgetsEvent.DismissSheet) },
            sheetState       = sheetState,
            containerColor   = Color.Transparent,
            dragHandle       = null,
        ) {
            BudgetActionSheet(uiState = uiState, onEvent = onEvent)
        }
    }
}
```

### Step 9 — Register in navigation

```kotlin
// ui/navigation/AppNavGraph.kt
composable("budgets") { BudgetsScreen() }
```

Add the tab/icon to `SummBottomBar` or a `SettingsNavItem` linking to it.

---

## Important Conventions

- **Kotlin-first everywhere** — coroutines + Flow, no RxJava, no LiveData.
- **Null-safety** — use `?: return` or `requireNotNull()`, never `!!`.
- **No logic in Composables** — composables observe state and emit events only.
- **No direct ViewModel calls from nested composables** — pass `onEvent` lambda down.
- **`@Immutable` on all state classes** — enables Compose strong-skipping (performance).
- **`WhileSubscribed(5_000)`** — automatically cancels upstream Flow collection when the app is backgrounded for 5 seconds, resuming on foreground.
- **Components are `internal`** — screen-specific composables are `internal fun` to prevent accidental cross-feature reuse.
- **Use cases over fat ViewModels** — if a ViewModel needs two or more repositories, extract the combination into a use case.
