# Lists and Scrolling in Jetpack Compose

## Choosing the right list component

| Component | Use when |
|---|---|
| `Column` + `verticalScroll` | Small, finite list (< ~20 items). All items composed at once. |
| `LazyColumn` | Long or dynamic list. Items composed on demand. |
| `LazyRow` | Horizontal list. |
| `LazyVerticalGrid` | Fixed-column grid. |
| `LazyHorizontalGrid` | Fixed-row horizontal grid. |
| `LazyVerticalStaggeredGrid` | Pinterest-style grid with variable item heights. |
| `HorizontalPager` / `VerticalPager` | Swipeable pages (full-screen or partial). |

**Never** put a `LazyColumn` inside a `Column` with `verticalScroll` — this causes unbounded height and a runtime exception.

---

## Column with verticalScroll — for small lists

```kotlin
val scrollState = rememberScrollState()

Column(
    modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(16.dp)
) {
    repeat(15) { index ->
        Text("Item $index", modifier = Modifier.padding(vertical = 8.dp))
    }
}
```

Programmatic scroll:
```kotlin
val scrollState = rememberScrollState()
val scope = rememberCoroutineScope()

Button(onClick = {
    scope.launch { scrollState.animateScrollTo(0) }  // scroll to top
}) {
    Text("Scroll to top")
}
```

---

## LazyColumn — the standard list

```kotlin
LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    items(
        items = users,
        key = { it.id },                   // stable key — required for animations and reuse
        contentType = { "user" }           // enables efficient ViewHolder-style reuse
    ) { user ->
        UserRow(user)
    }
}
```

### keys — always provide them

Keys allow Compose to identify items across list changes. Without keys, items are recreated on every change. With keys, Compose reuses composition and animates diffs.

```kotlin
// ❌ No key — items recreated on every list change
items(users) { user -> UserRow(user) }

// ✅ Stable key — string/int ID, never index
items(users, key = { it.id }) { user -> UserRow(user) }
```

Keys must be stable (`String`, `Int`, `Long`, `@Stable` objects) and unique within the list. Never use the item's index as a key.

### contentType — for mixed lists

```kotlin
sealed class FeedItem {
    data class Header(val title: String) : FeedItem()
    data class Post(val post: Post) : FeedItem()
    data class Ad(val ad: Ad) : FeedItem()
}

LazyColumn {
    items(
        items = feedItems,
        key = { item -> when (item) {
            is FeedItem.Header -> "header_${item.title}"
            is FeedItem.Post -> "post_${item.post.id}"
            is FeedItem.Ad -> "ad_${item.ad.id}"
        }},
        contentType = { item -> item::class }
    ) { item ->
        when (item) {
            is FeedItem.Header -> HeaderItem(item.title)
            is FeedItem.Post -> PostItem(item.post)
            is FeedItem.Ad -> AdItem(item.ad)
        }
    }
}
```

---

## LazyListState — scroll control and observation

```kotlin
val listState = rememberLazyListState()

LazyColumn(state = listState) {
    items(items, key = { it.id }) { item -> ItemRow(item) }
}
```

### Programmatic scrolling

```kotlin
val scope = rememberCoroutineScope()

// Instant scroll to index
Button(onClick = { scope.launch { listState.scrollToItem(0) } }) {
    Text("Jump to top")
}

// Animated scroll to index
Button(onClick = { scope.launch { listState.animateScrollToItem(0) } }) {
    Text("Animate to top")
}

// Scroll to item with offset
scope.launch { listState.animateScrollToItem(index = 10, scrollOffset = -50) }
```

### Observing scroll position

```kotlin
// Is the first item fully visible? (used for "scroll to top" FAB)
val showScrollToTop by remember {
    derivedStateOf { listState.firstVisibleItemIndex > 0 }
}

// Is the list scrolled to the bottom? (used for pagination)
val isAtBottom by remember {
    derivedStateOf {
        val layoutInfo = listState.layoutInfo
        val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        lastVisible >= layoutInfo.totalItemsCount - 1
    }
}

// Trigger load more when near the end
LaunchedEffect(isAtBottom) {
    if (isAtBottom) viewModel.loadMore()
}
```

### Saving scroll state across navigation

`rememberLazyListState()` persists across recompositions but is lost on back navigation. To restore across navigation, save and restore the scroll index:

```kotlin
// In ViewModel
var savedScrollIndex by mutableStateOf(0)
var savedScrollOffset by mutableStateOf(0)

// In Screen
val listState = rememberLazyListState(
    initialFirstVisibleItemIndex = viewModel.savedScrollIndex,
    initialFirstVisibleItemScrollOffset = viewModel.savedScrollOffset
)

DisposableEffect(Unit) {
    onDispose {
        viewModel.savedScrollIndex = listState.firstVisibleItemIndex
        viewModel.savedScrollOffset = listState.firstVisibleItemScrollOffset
    }
}
```

---

## LazyRow

Identical API to `LazyColumn`, horizontal:

```kotlin
LazyRow(
    state = rememberLazyListState(),
    contentPadding = PaddingValues(horizontal = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
) {
    items(categories, key = { it.id }) { category ->
        CategoryChip(category)
    }
}
```

---

## LazyVerticalGrid

```kotlin
LazyVerticalGrid(
    columns = GridCells.Fixed(2),          // fixed 2-column grid
    // or:
    columns = GridCells.Adaptive(150.dp),  // as many columns as fit at ≥150dp each
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
) {
    items(photos, key = { it.id }) { photo ->
        PhotoCard(photo)
    }
}
```

### Spanning multiple columns

```kotlin
LazyVerticalGrid(columns = GridCells.Fixed(2)) {
    item(span = { GridItemSpan(maxLineSpan) }) {  // full-width header
        SectionHeader("Photos")
    }
    items(photos, key = { it.id }) { photo ->
        PhotoCard(photo)
    }
}
```

---

## LazyVerticalStaggeredGrid

For items with variable heights (images, cards):

```kotlin
LazyVerticalStaggeredGrid(
    columns = StaggeredGridCells.Adaptive(150.dp),
    verticalItemSpacing = 8.dp,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
) {
    items(items, key = { it.id }) { item ->
        StaggeredCard(item)  // each card can have a different height
    }
}
```

---

## HorizontalPager — swipeable pages

```kotlin
val pagerState = rememberPagerState(pageCount = { pages.size })

HorizontalPager(
    state = pagerState,
    modifier = Modifier.fillMaxSize(),
) { pageIndex ->
    PageContent(pages[pageIndex])
}

// Pager indicators
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center
) {
    repeat(pagerState.pageCount) { index ->
        val selected = pagerState.currentPage == index
        Box(
            modifier = Modifier
                .padding(4.dp)
                .size(if (selected) 12.dp else 8.dp)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    shape = CircleShape
                )
        )
    }
}
```

### Programmatic page navigation

```kotlin
val scope = rememberCoroutineScope()

Button(onClick = {
    scope.launch { pagerState.animateScrollToPage(0) }
}) {
    Text("Go to first page")
}
```

### Scroll-linked TabRow

```kotlin
val tabs = listOf("Home", "Profile", "Settings")
val pagerState = rememberPagerState(pageCount = { tabs.size })
val scope = rememberCoroutineScope()

TabRow(selectedTabIndex = pagerState.currentPage) {
    tabs.forEachIndexed { index, title ->
        Tab(
            selected = pagerState.currentPage == index,
            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
            text = { Text(title) }
        )
    }
}

HorizontalPager(state = pagerState) { page ->
    TabContent(page)
}
```

---

## Item animations — animateItem

Animate item placement, insertion, and removal in lazy layouts:

```kotlin
LazyColumn {
    items(items, key = { it.id }) { item ->
        ItemRow(
            item = item,
            modifier = Modifier.animateItem()  // handles appear, disappear, reorder
        )
    }
}
```

`animateItem()` replaces the deprecated `animateItemPlacement()`. It accepts optional `fadeInSpec`, `placementSpec`, and `fadeOutSpec` parameters for custom animation specs.

> A stable `key` is **required** for `animateItem()` to work. Without a key, Compose cannot track identity across changes.

---

## Paging 3 integration

For server-side pagination with `androidx.paging`:

```kotlin
// ViewModel
val items = repository.getPagedItems().cachedIn(viewModelScope)

// Screen
@Composable
fun PagedScreen(viewModel: MyViewModel = hiltViewModel()) {
    val items = viewModel.items.collectAsLazyPagingItems()

    LazyColumn {
        items(
            count = items.itemCount,
            key = items.itemKey { it.id },
            contentType = items.itemContentType { "item" }
        ) { index ->
            val item = items[index]  // can be null while loading
            if (item != null) {
                ItemRow(item)
            } else {
                ItemPlaceholder()
            }
        }

        // Footer: loading indicator or error
        when (val state = items.loadState.append) {
            is LoadState.Loading -> item { LoadingFooter() }
            is LoadState.Error -> item {
                ErrorFooter(
                    message = state.error.message ?: "Error",
                    onRetry = { items.retry() }
                )
            }
            else -> Unit
        }
    }
}
```

---

## Nested scrolling

When you need a list inside a scrollable container:

```kotlin
// ✅ Correct: use nestedScroll for coordinated scrolling (e.g. collapsing toolbar)
val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

Scaffold(
    topBar = { TopAppBar(scrollBehavior = scrollBehavior) },
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
) { padding ->
    LazyColumn(modifier = Modifier.padding(padding)) {
        items(items, key = { it.id }) { ItemRow(it) }
    }
}
```

```kotlin
// ❌ Never put LazyColumn inside a scrollable Column — causes unbounded height crash
Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
    LazyColumn { /* ERROR: unbounded height */ }
}

// ✅ If you must mix, constrain the LazyColumn height
Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
    LazyColumn(modifier = Modifier.height(300.dp)) { /* fixed height */ }
}
```

---

## Performance checklist for lists

- [ ] **Stable keys** on every `items()` call — never use index
- [ ] **`contentType`** when the list has multiple item layouts
- [ ] **`@Immutable` / `@Stable`** on item data classes to enable recomposition skipping
- [ ] **`animateItem()`** only when animations are required (not by default)
- [ ] **No heavy work in item composables** — move calculations to the ViewModel
- [ ] **`derivedStateOf`** for derived scroll state (isAtBottom, showFab) to avoid redundant recompositions

---

## Anti-patterns

### Using index as key

```kotlin
// ❌ Index is unstable — if item at index 0 is removed, all other items re-create
items(users, key = { index, _ -> index })

// ✅ Use a stable unique identifier
items(users, key = { it.id })
```

### Creating objects inside item lambdas

```kotlin
// ❌ Creates new lambda on every recomposition — defeats skipping
LazyColumn {
    items(users) { user ->
        UserRow(
            user = user,
            onFavourite = { viewModel.toggleFavourite(user.id) }  // new lambda every time
        )
    }
}

// ✅ Pass the ViewModel method reference or stable lambda
LazyColumn {
    items(users, key = { it.id }) { user ->
        UserRow(
            user = user,
            onFavourite = viewModel::toggleFavourite  // stable reference
        )
    }
}
```

### LazyColumn inside Column with verticalScroll

```kotlin
// ❌ Runtime crash: LazyColumn requires a bounded height
Column(Modifier.verticalScroll(rememberScrollState())) {
    LazyColumn { items(list) { ItemRow(it) } }
}

// ✅ Use LazyColumn at the top level; add non-scrollable content as header/footer items
LazyColumn {
    item { Header() }
    items(list, key = { it.id }) { ItemRow(it) }
    item { Footer() }
}
```

### Not using contentPadding for edge-to-edge

```kotlin
// ❌ Last item clipped by bottom navigation bar
LazyColumn(modifier = Modifier.fillMaxSize()) { ... }

// ✅ Use contentPadding to add bottom spacing without padding the scroll track
LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = 80.dp)
) { ... }
```

---

**Source references:** `androidx.compose.foundation.lazy`, `androidx.compose.foundation.lazy.grid`, `androidx.compose.foundation.lazy.staggeredgrid`, `androidx.compose.foundation.pager`
