package com.davideagostini.summ.ui.entries

// EntriesScreen orchestrates the entries feature: it collects state, routes events to the ViewModel,
// and decides which overlay is visible at any given time without holding business logic itself.
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davideagostini.summ.R
import com.davideagostini.summ.data.entity.Category
import com.davideagostini.summ.ui.components.DeleteConfirmationDialog
import com.davideagostini.summ.ui.components.MonthCloseReadOnlyBanner
import com.davideagostini.summ.ui.components.MonthPickerOverlay
import com.davideagostini.summ.ui.components.MonthPickerViewModel
import com.davideagostini.summ.ui.components.buildRecentMonthOptions
import com.davideagostini.summ.ui.components.preferredRecentMonth
import com.davideagostini.summ.ui.entries.components.BalanceCard
import com.davideagostini.summ.ui.entries.components.CategorySpendingBreakdownCard
import com.davideagostini.summ.ui.entries.components.CategorySpendingChartCard
import com.davideagostini.summ.ui.entries.components.CategorySpendingSummaryCard
import com.davideagostini.summ.ui.entries.components.DayGroupHeader
import com.davideagostini.summ.ui.entries.components.EmptyState
import com.davideagostini.summ.ui.entries.components.EntriesToolbar
import com.davideagostini.summ.ui.entries.components.EntryActionSheet
import com.davideagostini.summ.ui.entries.components.EntryCard
import com.davideagostini.summ.ui.entries.components.EntryEditorScreen
import com.davideagostini.summ.ui.entries.components.UnusualSpendingCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Top-level transactions screen.
 *
 * This composable owns visual orchestration only: loading skeleton, list/report modes, sheets, and
 * dialogs. Business rules stay inside [EntriesViewModel].
 */
@Composable
fun EntriesRouteScreen(
    onFullscreenEditVisibilityChanged: (Boolean) -> Unit = {},
    onMonthPickerVisibilityChanged: (Boolean) -> Unit = {},
) {
    var mountContent by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!mountContent) {
            // Let navigation commit a couple of lightweight frames first so older devices can swap
            // tabs immediately before the full Entries tree, ViewModel, and collectors come online.
            withFrameNanos { }
            withFrameNanos { }
            delay(48)
            mountContent = true
        }
    }

    if (!mountContent) {
        EntriesLoadingContent(
            selectedMonth = preferredRecentMonth(buildRecentMonthOptions()),
            uiState = EntriesUiState(),
        )
        return
    }

    EntriesScreen(
        onFullscreenEditVisibilityChanged = onFullscreenEditVisibilityChanged,
        onMonthPickerVisibilityChanged = onMonthPickerVisibilityChanged,
    )
}

/**
 * Top-level transactions screen.
 *
 * This composable owns visual orchestration only: loading skeleton, list/report modes, sheets, and
 * dialogs. Business rules stay inside [EntriesViewModel].
 */
@Composable
fun EntriesScreen(
    viewModel: EntriesViewModel = hiltViewModel(),
    onFullscreenEditVisibilityChanged: (Boolean) -> Unit = {},
    onMonthPickerVisibilityChanged: (Boolean) -> Unit = {},
) {
    val monthPickerViewModel: MonthPickerViewModel = hiltViewModel()
    // The screen stays thin: it only observes immutable state and forwards callbacks to the ViewModel.
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isMonthRefreshing by viewModel.isMonthRefreshing.collectAsStateWithLifecycle()
    val isReportRefreshing by viewModel.isReportRefreshing.collectAsStateWithLifecycle()
    val monthOptions by monthPickerViewModel.monthOptions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val mostUsedEditCategories by viewModel.mostUsedEditCategories.collectAsStateWithLifecycle()
    val renderState by viewModel.renderState.collectAsStateWithLifecycle()
    val reportState by viewModel.reportState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onEvent = viewModel::handleEvent

    if (isLoading) {
        EntriesLoadingContent(
            selectedMonth = uiState.selectedMonth ?: preferredRecentMonth(monthOptions),
            uiState = uiState,
        )
        return
    }

    // All feature overlays are rendered from the same screen scope so their z-order stays predictable.
    EntriesContent(
        renderState = renderState,
        reportState = reportState,
        categories = categories,
        mostUsedEditCategories = mostUsedEditCategories,
        uiState = uiState,
        isMonthRefreshing = isMonthRefreshing,
        isReportRefreshing = isReportRefreshing,
        monthOptions = monthOptions,
        onEvent = onEvent,
        onFullscreenEditVisibilityChanged = onFullscreenEditVisibilityChanged,
        onMonthPickerVisibilityChanged = onMonthPickerVisibilityChanged,
    )
}

@Composable
private fun EntriesLoadingContent(
    selectedMonth: String,
    uiState: EntriesUiState,
) {
    val shimmer = rememberShimmerBrush()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.entries_action_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
            }

            item {
                EntriesToolbar(
                    selectedMonth = selectedMonth,
                    contentMode = uiState.contentMode,
                    searchVisible = uiState.searchVisible,
                    searchQuery = uiState.searchQuery,
                    onOpenMonthPicker = {},
                    onToggleContentMode = {},
                    onToggleSearch = {},
                    onSearchQueryChange = {},
                )
            }

            item {
                SkeletonBalanceCard(shimmer)
            }

            items(4) { index ->
                SkeletonDaySection(
                    shimmer = shimmer,
                    modifier = Modifier.padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = if (index == 0) 4.dp else 0.dp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SkeletonBalanceCard(shimmer: Brush) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SkeletonBlock(
                brush = shimmer,
                modifier = Modifier
                    .width(120.dp)
                    .height(14.dp),
            )
            SkeletonBlock(
                brush = shimmer,
                modifier = Modifier
                    .width(220.dp)
                    .height(36.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SkeletonFilterChip(shimmer, Modifier.weight(1f))
                SkeletonFilterChip(shimmer, Modifier.weight(1f))
                SkeletonFilterChip(shimmer, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SkeletonFilterChip(
    shimmer: Brush,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SkeletonBlock(
                brush = shimmer,
                modifier = Modifier
                    .width(56.dp)
                    .height(12.dp),
            )
        }
    }
}

@Composable
private fun SkeletonDaySection(
    shimmer: Brush,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SkeletonBlock(
            brush = shimmer,
            modifier = Modifier
                .width(96.dp)
                .height(12.dp),
        )
        repeat(2) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                                shape = CircleShape,
                            ),
                    ) {
                        SkeletonBlock(
                            brush = shimmer,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SkeletonBlock(
                            brush = shimmer,
                            modifier = Modifier
                                .fillMaxWidth(0.72f)
                                .height(14.dp),
                        )
                        SkeletonBlock(
                            brush = shimmer,
                            modifier = Modifier
                                .fillMaxWidth(0.42f)
                                .height(12.dp),
                        )
                    }
                    SkeletonBlock(
                        brush = shimmer,
                        modifier = Modifier
                            .width(72.dp)
                            .height(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SkeletonBlock(
    brush: Brush,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(brush),
    )
}

@Composable
private fun rememberShimmerBrush(): Brush {
    val base = MaterialTheme.colorScheme.surfaceContainerHighest
    val highlight = MaterialTheme.colorScheme.surfaceBright
    val transition = rememberInfiniteTransition(label = "entries_shimmer")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "entries_shimmer_offset",
    )
    return Brush.linearGradient(
        colors = listOf(
            base.copy(alpha = 0.9f),
            highlight.copy(alpha = 0.65f),
            base.copy(alpha = 0.9f),
        ),
        start = Offset(offset - 220f, offset - 220f),
        end = Offset(offset, offset),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EntriesContent(
    renderState: EntriesRenderState,
    reportState: EntriesReportState,
    categories: List<Category>,
    mostUsedEditCategories: List<Category>,
    uiState: EntriesUiState,
    isMonthRefreshing: Boolean,
    isReportRefreshing: Boolean,
    monthOptions: List<String>,
    onEvent: (EntriesEvent) -> Unit,
    onFullscreenEditVisibilityChanged: (Boolean) -> Unit,
    onMonthPickerVisibilityChanged: (Boolean) -> Unit,
) {
    // This local state only manages transient UI concerns such as sheet visibility and overlay behavior.
    var allowSheetHide by remember { mutableStateOf(false) }
    var showFullScreenEdit by remember { mutableStateOf(uiState.sheetMode == EntrySheetMode.Edit) }
    var isFullscreenEditFlow by remember { mutableStateOf(uiState.sheetMode == EntrySheetMode.Edit) }
    var showMonthPicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || allowSheetHide },
    )
    val selectedMonth = renderState.selectedMonth
    val isMonthClosed = renderState.isMonthClosed
    val unusualSpendingInsights = renderState.unusualSpendingInsights
    val monthLabel = renderState.monthLabel
    val isReportsMode = uiState.contentMode == EntriesContentMode.Reports
    val shimmer = rememberShimmerBrush()
    var showHeavyBody by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!showHeavyBody) {
            // First render: keep only the shell visible, then mount the heavier list/report body
            // on the next frame so older devices can acknowledge the tab switch immediately.
            withFrameNanos { }
            showHeavyBody = true
        }
    }

    // Keep the shared bottom bar hidden while the month picker overlay is open.
    LaunchedEffect(showMonthPicker) {
        onMonthPickerVisibilityChanged(showMonthPicker)
    }

    // Fullscreen edit is a separate presentation mode from the compact action sheet.
    // Once edit starts, the edit/success sequence stays in the fullscreen editor until dismissed.
    LaunchedEffect(uiState.sheetMode) {
        if (uiState.sheetMode == EntrySheetMode.Edit) {
            isFullscreenEditFlow = true
            showFullScreenEdit = true
        } else if (uiState.sheetMode == EntrySheetMode.Hidden) {
            isFullscreenEditFlow = false
            showFullScreenEdit = false
        }
    }
    LaunchedEffect(showFullScreenEdit) {
        // Propagate the fullscreen state to the nav graph so the shared bottom bar can be hidden.
        onFullscreenEditVisibilityChanged(showFullScreenEdit)
    }

    // Back should close the fullscreen editor before it clears the underlying ViewModel state.
    val dismissFullscreenEdit: () -> Unit = {
        showFullScreenEdit = false
        scope.launch {
            kotlinx.coroutines.delay(220)
            onEvent(EntriesEvent.DismissSheet)
        }
    }

    BackHandler(enabled = showFullScreenEdit) {
        dismissFullscreenEdit()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        // The list is the primary content of the screen; all overlays are layered above it.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.entries_action_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    )
                }

                item {
                    EntriesToolbar(
                        selectedMonth = selectedMonth,
                        contentMode = uiState.contentMode,
                        searchVisible = uiState.searchVisible,
                        searchQuery = uiState.searchQuery,
                        onOpenMonthPicker = { showMonthPicker = true },
                        onToggleContentMode = { onEvent(EntriesEvent.ToggleContentMode) },
                        onToggleSearch = { onEvent(EntriesEvent.ToggleSearch) },
                        onSearchQueryChange = { onEvent(EntriesEvent.UpdateSearchQuery(it)) },
                    )
                }

                if (isMonthRefreshing) {
                    item {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }
                }

                if (isMonthClosed && !isMonthRefreshing) {
                    item {
                        MonthCloseReadOnlyBanner(
                            message = stringResource(
                                R.string.month_close_read_only_message,
                                formatMonthLabel(selectedMonth),
                            ),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }
                }

                if (!showHeavyBody) {
                    item {
                        SkeletonBalanceCard(shimmer)
                    }

                    items(4) { index ->
                        SkeletonDaySection(
                            shimmer = shimmer,
                            modifier = Modifier.padding(
                                start = 20.dp,
                                end = 20.dp,
                                top = if (index == 0) 4.dp else 0.dp,
                            ),
                        )
                    }
                } else if (!isReportsMode) {
                    item {
                        BalanceCard(
                            currency = renderState.householdCurrency,
                            monthLabel = monthLabel,
                            expenses = renderState.totalExpenses,
                            income = renderState.totalIncome,
                            netCashFlow = renderState.totalIncome - renderState.totalExpenses,
                            filterType = uiState.filterType,
                            onFilterSelected = { onEvent(EntriesEvent.SelectFilter(it)) },
                        )
                    }

                    item {
                        if (unusualSpendingInsights.isNotEmpty()) {
                            UnusualSpendingCard(
                                currency = renderState.householdCurrency,
                                insights = unusualSpendingInsights,
                            )
                        }
                    }

                    if (renderState.listItems.isEmpty()) {
                        // Empty state changes its copy depending on whether the month has data at all or only filters hide it.
                        item {
                            EmptyState(
                                message = if (!renderState.hasAnyEntries) {
                                    stringResource(R.string.entries_empty_message)
                                } else {
                                    stringResource(R.string.entries_empty_filtered_message)
                                }
                            )
                        }
                    } else {
                        items(renderState.listItems, key = { it.key }) { item ->
                            when (item) {
                                is EntriesDayHeaderItem -> DayGroupHeader(
                                    currency = renderState.householdCurrency,
                                    date = item.date,
                                    expenseTotal = item.expenseTotal,
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                )
                                is EntriesRowItem -> EntryCard(
                                    item = item.entry,
                                    currency = renderState.householdCurrency,
                                    index = item.groupIndex,
                                    count = item.groupCount,
                                    readOnly = isMonthClosed,
                                    onClick = { onEvent(EntriesEvent.Select(item.entry)) },
                                )
                            }
                        }
                    }
                } else if (isReportRefreshing || reportState.selectedMonth != selectedMonth) {
                    item {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }
                    item {
                        SkeletonBalanceCard(shimmer)
                    }
                } else if (reportState.categorySpendingBreakdown.isEmpty()) {
                    item {
                        EmptyState(
                            message = stringResource(R.string.entries_reports_empty_message),
                        )
                    }
                } else {
                    item {
                        CategorySpendingSummaryCard(
                            currency = reportState.householdCurrency,
                            totalExpenses = reportState.categorySpendingTotal,
                            categoryCount = reportState.categorySpendingBreakdown.size,
                            transactionCount = reportState.categorySpendingTransactionCount,
                        )
                    }

                    item {
                        CategorySpendingChartCard(
                            currency = reportState.householdCurrency,
                            items = reportState.categorySpendingBreakdown,
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item {
                        CategorySpendingBreakdownCard(
                            currency = reportState.householdCurrency,
                            items = reportState.categorySpendingBreakdown,
                        )
                    }
                }
            }
        }

        MonthPickerOverlay(
            visible = showMonthPicker,
            selectedOption = selectedMonth,
            options = monthOptions,
            optionLabel = ::formatMonthLabel,
            onSelect = { onEvent(EntriesEvent.SelectMonth(it)) },
            onDismiss = { showMonthPicker = false },
        )
    }

    // The compact action sheet stays mounted for both action and success states, matching the assets flow.
    // Keep delete success in the compact bottom sheet, like assets. Fullscreen remains reserved for edit flow.
    if ((uiState.sheetMode == EntrySheetMode.Action || uiState.sheetMode == EntrySheetMode.Success) && !isFullscreenEditFlow) {
        val dismissSheet: () -> Unit = {
            scope.launch {
                allowSheetHide = true
                sheetState.hide()
                onEvent(EntriesEvent.DismissSheet)
                allowSheetHide = false
            }
            Unit
        }
        // The sheet is only responsible for the compact action surface; success remains within the same container.
        ModalBottomSheet(
            onDismissRequest = dismissSheet,
            sheetState = sheetState,
            containerColor = Color.Transparent,
            dragHandle = null,
            scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
        ) {
            EntryActionSheet(
                uiState = uiState,
                categories = categories,
                currency = renderState.householdCurrency,
                readOnly = isMonthClosed,
                readOnlyMessage = stringResource(
                    R.string.month_close_edit_disabled_message,
                    formatMonthLabel(selectedMonth),
                ),
                onEvent = onEvent,
                onDismiss = dismissSheet,
            )
        }
    }

    AnimatedVisibility(
        visible = isFullscreenEditFlow && uiState.sheetMode != EntrySheetMode.Hidden && uiState.sheetMode != EntrySheetMode.Action,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier.fillMaxSize(),
    ) {
        // Fullscreen edit is rendered as a dedicated overlay so it reads like a separate screen.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)),
        ) {
            EntryEditorScreen(
                uiState = uiState,
                categories = categories,
                mostUsedCategories = mostUsedEditCategories,
                currency = renderState.householdCurrency,
                readOnly = isMonthClosed,
                readOnlyMessage = stringResource(
                    R.string.month_close_edit_disabled_message,
                    formatMonthLabel(selectedMonth),
                ),
                onEvent = onEvent,
                onDismiss = dismissFullscreenEdit,
            )
        }
    }

    if (uiState.showDeleteDialog) {
        // Delete confirmation remains top-level so it can sit above the current sheet or fullscreen overlay.
        val desc = uiState.selectedEntry?.description.orEmpty()
        DeleteConfirmationDialog(
            title = stringResource(R.string.entries_delete_title, desc),
            message = stringResource(R.string.entries_delete_message),
            isLoading = uiState.isSaving,
            onConfirm = { onEvent(EntriesEvent.ConfirmDelete) },
            onDismiss = { onEvent(EntriesEvent.DismissDeleteDialog) },
        )
    }

}
