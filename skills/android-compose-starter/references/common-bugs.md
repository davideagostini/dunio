# Android Compose Common Bugs Reference

| Bug | Wrong | Correct |
|---|---|---|
| Search filter lost on timer or refresh | `search(filter.copy(search = query))` without persisting | `_uiState.update { it.copy(activeQuery = query) }` then `search(query)` |
| `AndroidView` overflows Compose layout | `Box { AndroidView(...) }` | `Box(Modifier.fillMaxSize()) { AndroidView(Modifier.fillMaxSize(), ...) }` |
| Mutable state exposed | `val uiState = _uiState` | `val uiState = _uiState.asStateFlow()` |
| State updated on the wrong thread | Modifying state inside a non-main callback | Use `viewModelScope.launch { }` |
| Previous job not cancelled | Starting a new coroutine without `job?.cancel()` | Cancel before starting a new load |
| Leaking collection | `collectAsState()` in a screen | Use `collectAsStateWithLifecycle()` |
| Modifier order bug | `background().padding()` vs `padding().background()` | Modifier order matters |
| Unstable type recomposing | `class Config(val title: String)` as parameter | Annotate with `@Immutable` or `@Stable` |
