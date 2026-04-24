# Android Compose Principles Reference

## Architecture: one direction, one source of truth

```text
UI (Composable)  →  sends event  →  ViewModel.handleEvent()
                 ←  reads state  ←  StateFlow<State>
                                       ↓
                                   Repository / DataSource
```

State flows down. Events flow up. The UI never writes to state directly.

---

## Every screen = State + Event + ViewModel + Composable

```kotlin
data class HomeState(
    val isLoading: Boolean = false,
    val items: List<Item> = emptyList(),
    val error: String? = null,
)

sealed class HomeEvent {
    data object Refresh : HomeEvent()
    data class Search(val query: String) : HomeEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(...) : BaseViewModel() {
    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    fun handleEvent(event: HomeEvent) { ... }
}

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(state = state, onEvent = viewModel::handleEvent)
}
```

---

## Key rules

- All dependency versions live in `gradle/libs.versions.toml`
- Prefer sealed results over exception-driven control flow
- Do not use `!!`
- Inject dependencies with Hilt instead of global business singletons
- Keep blocking work off the main thread
- Keep search debouncing and similar business logic in the ViewModel
- Use `collectAsStateWithLifecycle()`
- Keep UI-only state as low in the tree as possible
