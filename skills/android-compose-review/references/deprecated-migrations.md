# Deprecated Patterns & API Migrations in Jetpack Compose

Use this reference to identify outdated patterns and replace them with current APIs. Listed from most common to least.

---

## 1. collectAsState → collectAsStateWithLifecycle

**Why deprecated:** `collectAsState()` continues collecting even when the app is in the background (screen off, another app on top). This wastes resources and may cause unwanted side effects.

```kotlin
// ❌ Deprecated — leaks collection in background
val state by viewModel.uiState.collectAsState()

// ✅ Current — stops collection below STARTED lifecycle state
val state by viewModel.uiState.collectAsStateWithLifecycle()
```

**Dependency required:**
```toml
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
```

---

## 2. animateItemPlacement → animateItem

**Why deprecated:** `animateItemPlacement()` only animated position changes (reordering). `animateItem()` handles insertion, removal, and reordering in one modifier.

```kotlin
// ❌ Deprecated
LazyColumn {
    items(items, key = { it.id }) { item ->
        ItemRow(modifier = Modifier.animateItemPlacement())
    }
}

// ✅ Current
LazyColumn {
    items(items, key = { it.id }) { item ->
        ItemRow(modifier = Modifier.animateItem())
    }
}
```

`animateItem()` accepts optional specs:
```kotlin
Modifier.animateItem(
    fadeInSpec = tween(300),
    placementSpec = spring(stiffness = Spring.StiffnessMediumLow),
    fadeOutSpec = tween(150)
)
```

---

## 3. Modifier.composed → Modifier.Node

**Why deprecated:** `composed` creates a new composable scope for every modifier instance, triggering recomposition overhead and making the modifier heavier than it needs to be.

```kotlin
// ❌ Deprecated
fun Modifier.shimmer(): Modifier = composed {
    val alpha by rememberInfiniteTransition().animateFloat(...)
    this.then(Modifier.alpha(alpha))
}

// ✅ Current — Modifier.Node API
private class ShimmerNode : Modifier.Node(), DrawModifierNode {
    // Implement using node lifecycle, not composition
    override fun ContentDrawScope.draw() {
        drawContent()
        drawRect(color = Color.White.copy(alpha = 0.3f))
    }
}

private data object ShimmerElement : ModifierNodeElement<ShimmerNode>() {
    override fun create() = ShimmerNode()
    override fun update(node: ShimmerNode) = Unit
}

fun Modifier.shimmer(): Modifier = this.then(ShimmerElement)
```

For simple modifiers that do not use `remember` or composition locals, migrate eagerly. For complex ones using `rememberInfiniteTransition`, keep `composed` until the `Modifier.Node` animation APIs are stable.

---

## 4. KAPT → KSP

**Why deprecated:** KAPT (Kotlin Annotation Processing Tool) runs Java annotation processors, requires stub generation, and is significantly slower than KSP. KSP is Kotlin-first and supported by all major annotation processors (Hilt, Room, Moshi).

```kotlin
// ❌ Deprecated
plugins {
    id("kotlin-kapt")
}
dependencies {
    kapt("com.google.dagger:hilt-android-compiler:2.56.2")
    kapt("androidx.room:room-compiler:2.7.1")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.15.2")
}

// ✅ Current
plugins {
    alias(libs.plugins.ksp)
}
dependencies {
    ksp(libs.hilt.compiler)
    ksp(libs.room.compiler)
    ksp(libs.moshi.codegen)
}
```

KSP version must match the Kotlin version prefix:
```toml
ksp = "2.1.0-1.0.29"  # 2.1.0 = Kotlin version, 1.0.29 = KSP version
```

---

## 5. Accompanist migrations → Compose built-ins

Several Accompanist libraries were experimental wrappers that have since graduated into Compose or replaced by official APIs.

### Pager → foundation.pager

```kotlin
// ❌ Deprecated (accompanist-pager)
implementation("com.google.accompanist:accompanist-pager:0.32.0")
HorizontalPager(count = pages.size, state = pagerState) { ... }

// ✅ Current (compose.foundation — no extra dependency)
HorizontalPager(state = rememberPagerState(pageCount = { pages.size })) { ... }
```

### System UI Controller → WindowCompat

```kotlin
// ❌ Deprecated (accompanist-systemuicontroller)
val controller = rememberSystemUiController()
controller.setStatusBarColor(Color.Transparent)

// ✅ Current — WindowCompat + SideEffect
val view = LocalView.current
val darkTheme = isSystemInDarkTheme()
SideEffect {
    val window = (view.context as Activity).window
    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
}
```

### SwipeRefresh → PullToRefreshBox

```kotlin
// ❌ Deprecated (accompanist-swiperefresh)
SwipeRefresh(state = rememberSwipeRefreshState(isRefreshing), onRefresh = { ... }) { ... }

// ✅ Current (Material 3 — compose.material3)
PullToRefreshBox(
    isRefreshing = state.isRefreshing,
    onRefresh = { viewModel.handleEvent(Event.Refresh) }
) {
    LazyColumn { /* list */ }
}
```

### Permissions — still use accompanist

```kotlin
// Still valid as of 2025 — no official Compose permissions API yet
implementation("com.google.accompanist:accompanist-permissions:0.37.0")

val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
if (cameraPermission.status.isGranted) { CameraView() }
else { Button(onClick = { cameraPermission.launchPermissionRequest() }) { Text("Grant") } }
```

---

## 6. Material 2 → Material 3

Material 2 (`androidx.compose.material`) is frozen — no new development. All new projects should use Material 3 (`androidx.compose.material3`).

### Key API differences

| Material 2 | Material 3 |
|---|---|
| `Scaffold(backgroundColor = ...)` | `Scaffold(containerColor = ...)` |
| `TopAppBar` | `TopAppBar`, `CenterAlignedTopAppBar`, `LargeTopAppBar` |
| `BottomNavigation` | `NavigationBar` + `NavigationBarItem` |
| `Drawer` / `ModalDrawer` | `ModalNavigationDrawer` |
| `DropdownMenu` | `DropdownMenu` (same, but in `material3`) |
| `Chip` / `FilterChip` | `FilterChip`, `AssistChip`, `InputChip`, `SuggestionChip` |
| `colors = ButtonDefaults.buttonColors(backgroundColor = ...)` | `colors = ButtonDefaults.buttonColors(containerColor = ...)` |
| `Colors.primary` | `ColorScheme.primary` |
| `Typography.h5` | `Typography.headlineMedium` |

### Theme migration

```kotlin
// ❌ Material 2 theme
MaterialTheme(
    colors = lightColors(primary = Blue500),
    typography = Typography(h5 = TextStyle(...))
) { ... }

// ✅ Material 3 theme
MaterialTheme(
    colorScheme = lightColorScheme(primary = Blue500),
    typography = Typography(headlineMedium = TextStyle(...))
) { ... }
```

---

## 7. Type-safe Navigation (Navigation 2.8+)

Navigation 2.8 introduced type-safe routes using `@Serializable` objects, replacing string routes.

```kotlin
// ❌ Old string-based routes (still works, not deprecated, but error-prone)
const val DETAIL_ROUTE = "detail/{id}"
navController.navigate("detail/$id")

composable(
    route = "detail/{id}",
    arguments = listOf(navArgument("id") { type = NavType.StringType })
) { backStack ->
    val id = backStack.arguments?.getString("id")!!
    DetailScreen(id)
}

// ✅ Type-safe routes (Navigation 2.8+)
@Serializable
data class DetailRoute(val id: String)

// Navigate
navController.navigate(DetailRoute(id = item.id))

// Register
composable<DetailRoute> { backStack ->
    val route = backStack.toRoute<DetailRoute>()
    DetailScreen(route.id)
}
```

**Dependency:**
```toml
navigation = "2.9.0"   # 2.8+ supports type-safe routes
```

Also add the serialization plugin:
```toml
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

---

## 8. rememberImagePainter → AsyncImage (Coil)

```kotlin
// ❌ Deprecated (Coil 1.x)
Image(
    painter = rememberImagePainter(url),
    contentDescription = "Photo"
)

// ✅ Current (Coil 2.x / 3.x)
AsyncImage(
    model = url,
    contentDescription = "Photo",
    contentScale = ContentScale.Crop,
    modifier = Modifier.fillMaxWidth()
)

// With full request customisation
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(url)
        .crossfade(true)
        .placeholder(R.drawable.placeholder)
        .error(R.drawable.error)
        .build(),
    contentDescription = "Photo"
)
```

---

## 9. ambientOf → compositionLocalOf / staticCompositionLocalOf

The original `ambientOf` API was renamed during Compose alpha. You should never see it in new code, but it appears in old tutorials.

```kotlin
// ❌ Ancient alpha API
val AmbientTheme = ambientOf<AppTheme> { ... }

// ✅ Current
val LocalTheme = staticCompositionLocalOf<AppTheme> { error("Not provided") }
```

---

## 10. Deprecated Compose compiler options

### Compose compiler extension version

With Kotlin 2.0+, the Compose compiler is bundled with the Kotlin compiler. You no longer specify `kotlinCompilerExtensionVersion`.

```kotlin
// ❌ Old (pre Kotlin 2.0)
android {
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
}

// ✅ Current (Kotlin 2.0+ — use the compose compiler plugin instead)
plugins {
    alias(libs.plugins.compose.compiler)  // org.jetbrains.kotlin.plugin.compose
}
// No composeOptions block needed
```

---

## 11. BackHandler

`BackHandler` is the correct way to intercept back presses in Compose. Avoid overriding `onBackPressed()` in the Activity.

```kotlin
// ❌ Old — override in Activity
override fun onBackPressed() {
    if (canGoBack) goBack() else super.onBackPressed()
}

// ✅ Current — in the composable
@Composable
fun SearchScreen(onClose: () -> Unit) {
    var query by remember { mutableStateOf("") }

    // Handle back when search is active
    BackHandler(enabled = query.isNotEmpty()) {
        query = ""  // clear search instead of navigating back
    }

    // UI
}
```

`BackHandler` with `enabled = false` is a no-op — the system handles back navigation normally.

---

## 12. viewModel() → hiltViewModel()

In Hilt projects, use `hiltViewModel()` — never `viewModel()` from `lifecycle-viewmodel-compose` alone. `viewModel()` doesn't support constructor injection.

```kotlin
// ❌ Wrong in Hilt projects — misses injected dependencies
val vm: HomeViewModel = viewModel()

// ✅ Correct
val vm: HomeViewModel = hiltViewModel()
```

In non-Hilt projects, `viewModel()` with a factory is still valid:
```kotlin
val vm: HomeViewModel = viewModel { HomeViewModel(repository) }
```

---

## 13. Foundation LazyList API changes

### items with index

```kotlin
// Old — itemsIndexed still valid but rarely needed
LazyColumn {
    itemsIndexed(items) { index, item -> ItemRow(index, item) }
}

// Prefer items without index — index is unstable as key
LazyColumn {
    items(items, key = { it.id }) { item -> ItemRow(item) }
}
```

### LazyListScope.item vs items

```kotlin
// For single items (headers, footers, separators)
LazyColumn {
    item(key = "header") { Header() }
    items(list, key = { it.id }) { ItemRow(it) }
    item(key = "footer") { Footer() }
}
```

Always provide a `key` to `item()` too — without it, the header/footer may animate incorrectly when items are added/removed.

---

## 14. accompanist-flowlayout → FlowRow / FlowColumn

**Why deprecated:** `FlowRow` and `FlowColumn` graduated into `androidx.compose.foundation`.

```kotlin
// ❌ Deprecated (accompanist-flowlayout)
FlowRow(mainAxisSize = SizeMode.Expand) {
    items.forEach { item -> Chip(text = item.label) }
}

// ✅ Current (foundation 1.6+)
FlowRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    items.forEach { item -> Chip(text = item.label) }
}
```

Remove `accompanist-flowlayout` from `build.gradle.kts` entirely — no extra dependency needed.

---

## 15. mutableStateOf(0) → mutableIntStateOf(0)

**Why:** `mutableStateOf<Int>(0)` boxes the primitive on every read/write. Primitive-specialised variants avoid allocation.

```kotlin
// ❌ Boxes Int — unnecessary allocation
var count by remember { mutableStateOf(0) }
var progress by remember { mutableStateOf(0.5f) }
var index by remember { mutableStateOf(0L) }

// ✅ Primitive specialisations — no boxing
var count by remember { mutableIntStateOf(0) }
var progress by remember { mutableFloatStateOf(0.5f) }
var index by remember { mutableLongStateOf(0L) }
```

Available variants: `mutableIntStateOf`, `mutableFloatStateOf`, `mutableLongStateOf`, `mutableDoubleStateOf`. Boolean has no specialisation — `mutableStateOf(true)` is fine.

---

## 16. @ExperimentalMaterial3Api opt-in graduation

Several Material3 APIs were experimental on release and graduated to stable. Remove `@OptIn(ExperimentalMaterial3Api::class)` for APIs that are now stable.

```kotlin
// ❌ No longer needed for these APIs
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScreen() {
    DatePicker(state = rememberDatePickerState())
    TimePicker(state = rememberTimePickerState())
    SearchBar(query = query, onQueryChange = { }, onSearch = { }, active = false, onActiveChange = { }) { }
    ExposedDropdownMenuBox(expanded = false, onExpandedChange = { }) { }
}

// ✅ Current (Material3 1.3+ — all stable, no opt-in required)
@Composable
fun MyScreen() {
    DatePicker(state = rememberDatePickerState())
    TimePicker(state = rememberTimePickerState())
    SearchBar(query = query, onQueryChange = { }, onSearch = { }, active = false, onActiveChange = { }) { }
    ExposedDropdownMenuBox(expanded = false, onExpandedChange = { }) { }
}
```

Still experimental as of Material3 1.4 and requiring `@OptIn`: `ModalBottomSheet` scroll customisation, some `adaptive` layout APIs.

---

## 17. Scaffold innerPadding — must not be ignored

Since Compose 1.6, ignoring the `innerPadding` lambda parameter of `Scaffold` produces a lint warning that will become an error. Content drawn without applying the padding overlaps system bars.

```kotlin
// ❌ Content overlaps TopAppBar and navigation bar
Scaffold(topBar = { TopAppBar(title = { Text("Home") }) }) {
    LazyColumn { items(list, key = { it.id }) { ItemRow(it) } }
}

// ✅ Apply innerPadding to the content root
Scaffold(topBar = { TopAppBar(title = { Text("Home") }) }) { innerPadding ->
    LazyColumn(
        modifier = Modifier.padding(innerPadding)
        // or: contentPadding = innerPadding  for LazyColumn/LazyRow
    ) {
        items(list, key = { it.id }) { ItemRow(it) }
    }
}
```

For lazy lists, prefer `contentPadding = innerPadding` over `Modifier.padding(innerPadding)` — it pads the scroll area without clipping the scroll track.

---

## 18. Manual insets / systemBarsPadding → WindowInsets + enableEdgeToEdge

On Android 15+, edge-to-edge is enforced by default. Content draws behind system bars. The old approach of manually setting status bar colours is replaced.

```kotlin
// ❌ Old — manual status bar colour + padding
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT.toArgb()
        setContent {
            Surface(modifier = Modifier.systemBarsPadding()) {
                AppContent()
            }
        }
    }
}

// ✅ Current — call enableEdgeToEdge() once, handle insets in the layout
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()                    // handles status + navigation bar colour
        setContent {
            AppTheme {
                AppContent()
            }
        }
    }
}

// In your Scaffold or root composable — insets flow through Scaffold automatically
Scaffold(
    topBar = { TopAppBar(...) },
    // NavigationBar at the bottom also handles insets
) { innerPadding ->
    Content(modifier = Modifier.padding(innerPadding))
}
```

For content that draws full-bleed behind bars, use `WindowInsets.safeDrawing`:

```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .padding(WindowInsets.safeDrawing.asPaddingValues())
) {
    FullBleedBackground()
    Content()
}
```

---

## 19. Direct observer → snapshotFlow for observable state bridges

When integrating non-Compose observable types (custom state objects, callbacks, SDK listeners) into Compose state, `snapshotFlow` is the idiomatic bridge.

```kotlin
// ❌ Old — manual listener with mutableState
@Composable
fun SdkStatus(sdk: MySdk) {
    var status by remember { mutableStateOf(sdk.currentStatus) }

    DisposableEffect(sdk) {
        val listener = MySdk.StatusListener { newStatus -> status = newStatus }
        sdk.addListener(listener)
        onDispose { sdk.removeListener(listener) }
    }

    Text(status)
}

// ✅ Current — snapshotFlow when the source is already Compose state
@Composable
fun SearchScreen(viewModel: SearchViewModel) {
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        snapshotFlow { query }
            .distinctUntilChanged()
            .debounce(300)
            .collect { viewModel.search(it) }
    }

    TextField(value = query, onValueChange = { query = it }, label = { Text("Search") })
}
```

`snapshotFlow` emits initial value + subsequent changes. Use `DisposableEffect` when you need to register/unregister a non-Compose listener; use `snapshotFlow` when bridging existing Compose state to a Flow.

---

## Quick reference table

| Deprecated | Current | Since |
|---|---|---|
| `collectAsState()` | `collectAsStateWithLifecycle()` | lifecycle 2.6 |
| `animateItemPlacement()` | `animateItem()` | Compose 1.7 |
| `Modifier.composed { }` | `Modifier.Node` / `ModifierNodeElement` | Compose 1.5 |
| KAPT | KSP | Hilt 2.48, Room 2.6 |
| `accompanist-pager` | `HorizontalPager` / `VerticalPager` | Compose 1.4 |
| `accompanist-swiperefresh` | `PullToRefreshBox` | Material3 1.3 |
| `accompanist-flowlayout` | `FlowRow` / `FlowColumn` | Foundation 1.6 |
| `accompanist-systemuicontroller` | `enableEdgeToEdge()` + `WindowInsets` | AGP 8+ / Android 15 |
| `rememberImagePainter` | `AsyncImage` | Coil 2.0 |
| `ambientOf` | `compositionLocalOf` / `staticCompositionLocalOf` | Compose alpha |
| `kotlinCompilerExtensionVersion` | `compose-compiler` plugin | Kotlin 2.0 |
| `mutableStateOf(0)` | `mutableIntStateOf(0)` | Compose 1.4 |
| `@OptIn(ExperimentalMaterial3Api)` on DatePicker etc. | Remove opt-in | Material3 1.3 |
| Ignored `Scaffold` `innerPadding` | Apply `innerPadding` to content | Compose 1.6 |
| `systemBarsPadding()` | `enableEdgeToEdge()` + `WindowInsets.safeDrawing` | Android 15 |
| Direct listener → mutable state | `snapshotFlow { }` | Compose 1.1 |
| String-based nav routes | `@Serializable` type-safe routes | Navigation 2.8 |
| `onBackPressed()` override | `BackHandler` composable | Compose 1.1 |
| `viewModel()` in Hilt project | `hiltViewModel()` | Hilt 1.0 |
| `onBackPressed()` override | `BackHandler` composable | Compose 1.1 |
| `viewModel()` in Hilt project | `hiltViewModel()` | Hilt 1.0 |
| String-based nav routes | `@Serializable` type-safe routes | Navigation 2.8 |
