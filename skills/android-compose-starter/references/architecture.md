# Android Architecture Reference

## Overview

The architecture is **feature-first MVVM** with unidirectional data flow:

```
UI (Composable Screen)
  └─ observes ──► ViewModel (StateFlow<State>)
                    └─ calls ──► Repository / DataSource
                    └─ emits ──► Channel<Event> for one-shot UI effects
```

State flows **down**. Events flow **up**. The UI never writes to state directly.

---

## Package structure

```
app/src/main/java/com/<company>/<app>/
│
├── App.kt                        # @HiltAndroidApp Application class
├── MainActivity.kt               # @AndroidEntryPoint, hosts NavHost
│
├── feature/
│   ├── <feature1>/
│   │   ├── <Feature1>Screen.kt   # @Composable, top-level screen
│   │   ├── <Feature1>ViewModel.kt
│   │   ├── components/           # feature-scoped composables (not shared)
│   │   └── model/
│   │       ├── <Feature1>State.kt
│   │       └── <Feature1>Event.kt
│   │
│   └── <feature2>/
│       └── ...
│
├── navigation/
│   ├── Screen.kt                 # sealed class of all routes
│   ├── Navigation.kt             # NavHost root
│   └── graph/
│       ├── <Feature1>Graph.kt    # NavGraphBuilder extension per feature
│       └── ...
│
├── data/
│   ├── repository/
│   │   └── <Entity>Repository.kt
│   ├── remote/
│   │   ├── ApiService.kt
│   │   └── dto/
│   └── local/
│       ├── AppDatabase.kt
│       └── dao/
│
├── domain/
│   └── model/                    # pure domain models (no Android deps)
│
├── di/
│   ├── AppModule.kt              # @Module @InstallIn(SingletonComponent)
│   ├── NetworkModule.kt
│   └── DatabaseModule.kt
│
└── shared/
    ├── ui/                       # shared composables (buttons, dialogs, etc.)
    ├── viewmodel/
    │   └── BaseViewModel.kt
    └── util/
```

---

## Hilt setup

### Application class

```kotlin
@HiltAndroidApp
class App : Application()
```

Register in `AndroidManifest.xml`:
```xml
<application
    android:name=".App"
    ...>
```

### Activity

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                AppNavigation()
            }
        }
    }
}
```

### DI module example

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)
}
```

---

## BaseViewModel

A shared base class for all ViewModels. Centralises toast and snackbar emission.

```kotlin
open class BaseViewModel : ViewModel() {

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    private val _snackbarMessage = MutableSharedFlow<SnackbarMessage>()
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    protected fun emitToast(message: String) {
        viewModelScope.launch { _toastMessage.emit(message) }
    }

    protected fun emitSnackbar(message: SnackbarMessage) {
        viewModelScope.launch { _snackbarMessage.emit(message) }
    }
}

data class SnackbarMessage(
    val message: String,
    val isError: Boolean = false
)
```

---

## State — immutable data class

```kotlin
// In feature/home/model/HomeState.kt
data class HomeState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val items: List<Item> = emptyList(),
    val error: String? = null,
)
```

Rules:
- All fields have **default values** — the empty constructor always works
- Use `Boolean = false`, `List<T> = emptyList()`, `String? = null` as defaults
- **Never** add `var` fields — all fields are `val`
- Use `_uiState.update { it.copy(...) }` for all mutations — never replace the whole object

---

## Event — sealed class

```kotlin
// In feature/home/model/HomeEvent.kt
sealed class HomeEvent {
    data object Refresh : HomeEvent()
    data object LoadMore : HomeEvent()
    data class Search(val query: String) : HomeEvent()
    data class OpenItem(val id: String) : HomeEvent()
}
```

Rules:
- `data object` for zero-argument events
- `data class` for events that carry data
- One sealed class per screen

---

## ViewModel

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ItemRepository,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    // One-shot navigation/UI events
    private val _events = Channel<HomeEvent>()
    val events = _events.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        load()
        observeSearch()
    }

    fun handleEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.Refresh -> {
                _uiState.update { it.copy(isRefreshing = true) }
                load()
            }
            is HomeEvent.LoadMore -> loadMore()
            is HomeEvent.Search -> {
                _uiState.update { it.copy(searchQuery = event.query) }
                // debounce observer in observeSearch() handles the actual call
            }
            is HomeEvent.OpenItem -> {
                viewModelScope.launch { _events.send(event) }
            }
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getItems()
                .onSuccess { items ->
                    _uiState.update { it.copy(items = items, isLoading = false, isRefreshing = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
                    emitSnackbar(SnackbarMessage(e.message ?: "Unknown error", isError = true))
                }
        }
    }

    private fun loadMore() {
        // paginated load logic
    }

    private fun observeSearch() {
        _uiState
            .map { it.searchQuery }
            .distinctUntilChanged()
            .drop(1)
            .debounce(300)
            .onEach { query ->
                // persist search into state before calling search
                _uiState.update { it.copy(activeSearchQuery = query) }
                search(query)
            }
            .launchIn(viewModelScope)
    }

    private fun search(query: String) {
        viewModelScope.launch {
            // call repository with query
        }
    }
}
```

Rules:
- Always extend `BaseViewModel()`
- Never expose `MutableStateFlow` to the UI — always `.asStateFlow()`
- Cancel the previous `Job` before starting a new one: `loadJob?.cancel()`
- Rethrow `CancellationException` inside try/catch to preserve structured concurrency
- Use `_uiState.update { it.copy(...) }` for all state mutations
- One-shot navigation events go through `Channel`, not `StateFlow`

---

## Screen composable

```kotlin
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToDetail: (id: String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Collect one-shot navigation events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.OpenItem -> onNavigateToDetail(event.id)
                else -> Unit
            }
        }
    }

    Scaffold(
        topBar = { /* TopAppBar */ }
    ) { padding ->
        HomeContent(
            state = state,
            onEvent = viewModel::handleEvent,
            modifier = Modifier.padding(padding)
        )
    }
}

// Content composable is separate — pure, testable, previewable
@Composable
private fun HomeContent(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> LoadingIndicator()
        state.error != null -> ErrorView(state.error, onRetry = { onEvent(HomeEvent.Refresh) })
        else -> ItemList(state.items, onEvent = onEvent, modifier = modifier)
    }
}
```

Rules:
- Use `collectAsStateWithLifecycle()` — never `collectAsState()`
- Keep the screen composable thin — just ViewModel wiring and Scaffold
- Extract `Content` composable that is pure (no ViewModel reference)
- Never pass the ViewModel to child composables — pass only data and lambdas

---

## Repository pattern

```kotlin
interface ItemRepository {
    suspend fun getItems(): Result<List<Item>>
    suspend fun getItemById(id: String): Result<Item>
}

class ItemRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val itemDao: ItemDao,
) : ItemRepository {

    override suspend fun getItems(): Result<List<Item>> = runCatching {
        val remote = apiService.getItems()
        itemDao.insertAll(remote.map { it.toEntity() })
        remote.map { it.toDomain() }
    }

    override suspend fun getItemById(id: String): Result<Item> = runCatching {
        apiService.getItem(id).toDomain()
    }
}
```

Bind in DI module:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindItemRepository(impl: ItemRepositoryImpl): ItemRepository
}
```

---

## Navigation

### Screen routes

```kotlin
// navigation/Screen.kt
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Detail : Screen("detail/{id}") {
        fun createRoute(id: String) = "detail/$id"
    }
    data object Settings : Screen("settings")
}
```

### NavHost root

```kotlin
// navigation/Navigation.kt
@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        onHomeScreen(navController)
        onDetailScreen(navController)
        onSettingsScreen(navController)
    }
}
```

### Graph builder per feature

```kotlin
// navigation/graph/HomeGraph.kt
fun NavGraphBuilder.onHomeScreen(navController: NavHostController) {
    composable(Screen.Home.route) {
        HomeScreen(
            onNavigateToDetail = { id ->
                navController.navigate(Screen.Detail.createRoute(id))
            }
        )
    }
}

fun NavGraphBuilder.onDetailScreen(navController: NavHostController) {
    composable(
        route = Screen.Detail.route,
        arguments = listOf(navArgument("id") { type = NavType.StringType })
    ) { backStackEntry ->
        val id = backStackEntry.arguments?.getString("id") ?: return@composable
        DetailScreen(
            id = id,
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
```

---

## Result handling

Use Kotlin's built-in `Result<T>` or define a custom sealed class:

```kotlin
// Custom sealed result — more expressive for API errors
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class HttpError(val code: Int, val message: String) : ApiResult<Nothing>()
    data class NetworkError(val cause: Throwable) : ApiResult<Nothing>()
}

// In ViewModel: always handle all branches
when (val result = repository.getItems()) {
    is ApiResult.Success -> {
        _uiState.update { it.copy(items = result.data, isLoading = false) }
    }
    is ApiResult.HttpError -> {
        _uiState.update { it.copy(isLoading = false) }
        emitSnackbar(SnackbarMessage("${result.code}: ${result.message}", isError = true))
    }
    is ApiResult.NetworkError -> {
        _uiState.update { it.copy(isLoading = false) }
        emitSnackbar(SnackbarMessage("No connection", isError = true))
    }
}
```
