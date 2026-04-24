# CompositionLocals: Implicit Data Passing in Jetpack Compose

## What is CompositionLocal

`CompositionLocal` is a mechanism to pass data implicitly through the composition tree without threading it through every composable parameter. It creates a scoped ambient value that any composable below the provider can read.

```kotlin
// Without CompositionLocal — threading through every level
@Composable
fun Root(theme: AppTheme) {
    Screen(theme)
}
@Composable
fun Screen(theme: AppTheme) {
    Card(theme)
}
@Composable
fun Card(theme: AppTheme) {
    Text("Hello", color = theme.primaryColor)  // finally used here
}

// With CompositionLocal — no threading needed
val LocalAppTheme = staticCompositionLocalOf<AppTheme> { error("No theme provided") }

@Composable
fun Root(theme: AppTheme) {
    CompositionLocalProvider(LocalAppTheme provides theme) {
        Screen()  // no parameter needed
    }
}
@Composable
fun Card() {
    val theme = LocalAppTheme.current  // available anywhere below the provider
    Text("Hello", color = theme.primaryColor)
}
```

---

## Two types: staticCompositionLocalOf vs compositionLocalOf

### staticCompositionLocalOf

```kotlin
val LocalUser = staticCompositionLocalOf<User?> { null }
```

- Value **rarely changes** after initial provision
- When the value changes, the **entire** subtree under the provider recomposes
- Zero overhead at read sites — no snapshot tracking
- Best for: theme objects, navigation controllers, platform singletons

### compositionLocalOf

```kotlin
val LocalFontScale = compositionLocalOf { 1.0f }
```

- Value **can change** during the app's lifetime
- When the value changes, only composables that **read it** recompose
- Small overhead at read sites — tracks snapshot changes
- Best for: values that change at runtime (locale, font size, accessibility settings)

**Rule of thumb:** default to `staticCompositionLocalOf`. Switch to `compositionLocalOf` only when the value changes at runtime and you want fine-grained recomposition.

### compositionLocalWithComputedDefaultOf

A third variant introduced for computed default values. The lambda runs each time the local is read without an active provider, so no allocation happens until a value is actually needed.

```kotlin
// ❌ Workaround — unnecessary lazy allocation
val LocalResources = compositionLocalOf<Resources> { Resources() }

// ✅ Computed default — reads context only when accessed without a provider
val LocalResources = compositionLocalWithComputedDefaultOf { currentCompositionLocalContext[LocalContext].resources }
```

Use when the default value is cheap to compute from other locals but expensive to pre-allocate. Rarely needed in application code — most useful in library code.

---

## CompositionLocalProvider

Provides one or more values to the subtree:

```kotlin
CompositionLocalProvider(LocalAppTheme provides darkTheme) {
    AppContent()
}

// Multiple values at once
CompositionLocalProvider(
    LocalAppTheme provides darkTheme,
    LocalLocale provides Locale.ITALIAN,
    LocalTextScale provides 1.2f,
) {
    AppContent()
}
```

Providers **nest and shadow** — the nearest provider wins:

```kotlin
CompositionLocalProvider(LocalFontScale provides 1.0f) {
    Text("Normal")  // reads 1.0f

    CompositionLocalProvider(LocalFontScale provides 1.5f) {
        Text("Large")   // reads 1.5f — inner provider shadows outer
    }

    Text("Normal again")  // reads 1.0f — outer scope restored
}
```

---

## Reading CompositionLocal values

Always read via `.current` inside a composable:

```kotlin
@Composable
fun UserGreeting() {
    val user = LocalUser.current  // reads the nearest provided value
    Text("Hello, ${user?.name ?: "Guest"}")
}
```

`.current` is a property that reads the snapshot-tracked value. Never call it outside of a composable function or a `@Composable` lambda.

---

## Defining custom CompositionLocals

### Convention for naming

Always prefix with `Local`:

```kotlin
// ✅ Correct naming
val LocalAppTheme = staticCompositionLocalOf<AppTheme> { ... }
val LocalNavController = staticCompositionLocalOf<NavController> { error("Not provided") }

// ❌ Missing Local prefix — hard to identify at call sites
val AppThemeLocal = staticCompositionLocalOf<AppTheme> { ... }
```

### Default value strategies

```kotlin
// Strategy 1: null default — safe when absence is valid
val LocalCurrentUser = staticCompositionLocalOf<User?> { null }

// Strategy 2: error default — crashes fast if provider is missing (preferred for required values)
val LocalAppTheme = staticCompositionLocalOf<AppTheme> {
    error("No AppTheme provided. Wrap content with AppTheme { ... }")
}

// Strategy 3: fallback default — sensible no-op implementation
val LocalAnalytics = staticCompositionLocalOf<Analytics> { NoOpAnalytics() }
```

---

## Framework-provided CompositionLocals

These are provided by the Android framework and Compose runtime. Always use them — never re-provide unless testing:

```kotlin
// Context
val context = LocalContext.current

// Lifecycle
val lifecycleOwner = LocalLifecycleOwner.current

// Activity (requires androidx.activity:activity-compose)
val activity = LocalActivity.current

// View (underlying Android View)
val view = LocalView.current

// Window insets controller (for status bar color)
val windowInfo = LocalWindowInfo.current

// Density (dp ↔ px conversion)
val density = LocalDensity.current
with(density) { 16.dp.toPx() }  // convert dp to pixels

// Font scale
val fontScale = LocalFontFamily.current

// Text input service
val textInputService = LocalTextInputService.current

// Haptic feedback
val hapticFeedback = LocalHapticFeedback.current
hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

// Clipboard
val clipboardManager = LocalClipboardManager.current

// URI handler (open links)
val uriHandler = LocalUriHandler.current
uriHandler.openUri("https://example.com")
```

---

## When to use CompositionLocal

**Use for:**
- **Theme data** — colors, typography, shapes that the entire UI tree needs
- **Platform singletons** — `Context`, `Activity`, `NavController`, `Density`
- **Cross-cutting concerns** — locale, text scale, accessibility settings
- **Testing overrides** — swap implementations in preview/test without changing signatures

**Do NOT use for:**
- **Business logic dependencies** — repositories, ViewModels, use cases (use Hilt/DI)
- **Data that changes frequently** — defeats `staticCompositionLocalOf` and causes broad recomposition
- **Data only 1–2 levels deep** — just pass it as a parameter

```kotlin
// ❌ Wrong — business dependency via CompositionLocal bypasses DI
val LocalUserRepository = staticCompositionLocalOf<UserRepository> {
    error("Not provided")
}

// ✅ Correct — inject via Hilt ViewModel
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel()
```

---

## Custom CompositionLocal — real-world example

### App theme

```kotlin
// Define
data class AppColors(
    val primary: Color,
    val background: Color,
    val textPrimary: Color,
)

val LocalAppColors = staticCompositionLocalOf<AppColors> {
    error("No AppColors provided")
}

// Provide at root
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkAppColors else LightAppColors
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(content = content)
    }
}

// Read anywhere in the tree
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
    ) {
        Text(text, color = colors.textPrimary)
    }
}
```

### Snackbar state

```kotlin
data class SnackbarContent(val message: String, val isError: Boolean = false)

class SnackbarState {
    var current by mutableStateOf<SnackbarContent?>(null)
    fun show(content: SnackbarContent) { current = content }
    fun dismiss() { current = null }
}

val LocalSnackbarState = staticCompositionLocalOf<SnackbarState> {
    error("No SnackbarState provided")
}

// Provide
@Composable
fun AppRoot() {
    val snackbarState = remember { SnackbarState() }
    CompositionLocalProvider(LocalSnackbarState provides snackbarState) {
        Scaffold(
            snackbarHost = {
                snackbarState.current?.let { content ->
                    Snackbar(/* ... */)
                }
            }
        ) {
            AppNavigation()
        }
    }
}

// Use deep in the tree — no parameter threading
@Composable
fun DeleteButton(onDelete: suspend () -> Unit) {
    val snackbar = LocalSnackbarState.current
    val scope = rememberCoroutineScope()
    Button(onClick = {
        scope.launch {
            onDelete()
            snackbar.show(SnackbarContent("Deleted successfully"))
        }
    }) {
        Text("Delete")
    }
}
```

---

## Testing and Previews

Override CompositionLocals in previews and tests:

```kotlin
// Preview with custom theme
@Preview
@Composable
fun PrimaryButtonPreview() {
    CompositionLocalProvider(LocalAppColors provides LightAppColors) {
        PrimaryButton("Save", onClick = {})
    }
}

// Compose test rule
composeTestRule.setContent {
    CompositionLocalProvider(
        LocalAppColors provides TestColors,
        LocalUser provides User(id = "1", name = "Alice")
    ) {
        ProfileScreen()
    }
}
```

---

## Anti-patterns

### Reading CompositionLocal outside composition

```kotlin
// ❌ Can't read .current outside a @Composable function
class MyRepository {
    fun doWork() {
        val ctx = LocalContext.current  // ERROR: not in composition
    }
}

// ✅ Inject Context via constructor or pass as parameter
class MyRepository(private val context: Context) {
    fun doWork() { context.contentResolver }
}
```

### Providing mutable objects without state observation

```kotlin
// ❌ Mutation is not observed — recomposition won't happen
class MutableConfig { var title = "App" }
val LocalConfig = staticCompositionLocalOf { MutableConfig() }

// ✅ Use immutable data + re-provide to trigger recomposition
@Immutable
data class Config(val title: String)
val LocalConfig = staticCompositionLocalOf { Config("App") }
```

### Using CompositionLocal for screen-specific data

```kotlin
// ❌ Screen-specific state doesn't belong in a CompositionLocal
val LocalSearchQuery = staticCompositionLocalOf { "" }

// ✅ Screen state belongs in ViewModel StateFlow
val searchQuery by viewModel.uiState.map { it.searchQuery }.collectAsStateWithLifecycle()
```

---

**Source references:** `androidx.compose.runtime.CompositionLocal`, `androidx.compose.runtime.staticCompositionLocalOf`, `androidx.compose.runtime.compositionLocalOf`
