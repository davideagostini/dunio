package com.davideagostini.summ.ui.entries

// EntriesScreen orchestrates the entries feature: it collects state, routes events to the ViewModel,
// and decides which overlay is visible at any given time without holding business logic itself.
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davideagostini.summ.R
import com.davideagostini.summ.data.entity.Category
import com.davideagostini.summ.ui.components.DeleteConfirmationDialog
import com.davideagostini.summ.ui.components.FullScreenLoading
import com.davideagostini.summ.ui.components.MonthCloseReadOnlyBanner
import com.davideagostini.summ.ui.components.MonthPickerOverlay
import com.davideagostini.summ.ui.components.buildRecentMonthOptions
import com.davideagostini.summ.ui.entries.components.BalanceCard
import com.davideagostini.summ.ui.entries.components.CategorySpendingBreakdownCard
import com.davideagostini.summ.ui.entries.components.CategorySpendingChartCard
import com.davideagostini.summ.ui.entries.components.CategorySpendingSummaryCard
import com.davideagostini.summ.ui.entries.components.DayGroupSection
import com.davideagostini.summ.ui.entries.components.EmptyState
import com.davideagostini.summ.ui.entries.components.EntriesToolbar
import com.davideagostini.summ.ui.entries.components.EntryActionSheet
import com.davideagostini.summ.ui.entries.components.UnusualSpendingCard
import kotlinx.coroutines.launch

@Composable
fun EntriesScreen(viewModel: EntriesViewModel = hiltViewModel()) {
    // The screen stays thin: it only observes immutable state and forwards callbacks to the ViewModel.
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val renderState by viewModel.renderState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onEvent = viewModel::handleEvent

    if (isLoading) {
        // Keep the user on a single loading surface until all feature data streams are ready.
        FullScreenLoading()
        return
    }

    // All feature overlays are rendered from the same screen scope so their z-order stays predictable.
    EntriesContent(
        renderState = renderState,
        categories = categories,
        uiState = uiState,
        onEvent = onEvent,
        onFullscreenEditVisibilityChanged = {},
        onMonthPickerVisibilityChanged = {},
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EntriesContent(
    renderState: EntriesRenderState,
    categories: List<Category>,
    uiState: EntriesUiState,
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
    val monthOptions = remember { buildRecentMonthOptions() }
    val selectedMonth = renderState.selectedMonth
    val isMonthClosed = renderState.isMonthClosed
    val dayGroups = renderState.dayGroups
    val unusualSpendingInsights = renderState.unusualSpendingInsights
    val monthLabel = renderState.monthLabel
    val isReportsMode = uiState.contentMode == EntriesContentMode.Reports

    // Keep the shared bottom bar hidden while the month picker overlay is open.
    LaunchedEffect(showMonthPicker) {
        onMonthPickerVisibilityChanged(showMonthPicker)
    }

    // Fullscreen edit is a separate presentation mode from the compact action sheet.
    LaunchedEffect(uiState.sheetMode) {
        if (uiState.sheetMode == EntrySheetMode.Edit) {
            // Once edit starts, the whole edit/success flow stays fullscreen until it is fully dismissed.
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
                verticalArrangement = Arrangement.spacedBy(4.dp),
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

                if (isMonthClosed) {
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

                if (!isReportsMode) {
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

                    if (dayGroups.isEmpty()) {
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
                        items(dayGroups, key = { it.key.toString() }) { group ->
                            DayGroupSection(
                                currency = renderState.householdCurrency,
                                group = group,
                                readOnly = isMonthClosed,
                                onEntryClick = { onEvent(EntriesEvent.Select(it)) },
                            )
                        }
                    }
                } else if (renderState.categorySpendingBreakdown.isEmpty()) {
                    item {
                        EmptyState(
                            message = stringResource(R.string.entries_reports_empty_message),
                        )
                    }
                } else {
                    item {
                        CategorySpendingSummaryCard(
                            currency = renderState.householdCurrency,
                            totalExpenses = renderState.categorySpendingTotal,
                            categoryCount = renderState.categorySpendingBreakdown.size,
                            transactionCount = renderState.categorySpendingTransactionCount,
                        )
                    }

                    item {
                        CategorySpendingChartCard(
                            currency = renderState.householdCurrency,
                            items = renderState.categorySpendingBreakdown,
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item {
                        CategorySpendingBreakdownCard(
                            currency = renderState.householdCurrency,
                            items = renderState.categorySpendingBreakdown,
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
        // The sheet is only responsible for the compact action surface; success remains within the same container.
        ModalBottomSheet(
            onDismissRequest = {},
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
                onDismiss = {
                    scope.launch {
                        allowSheetHide = true
                        sheetState.hide()
                        onEvent(EntriesEvent.DismissSheet)
                        allowSheetHide = false
                    }
                },
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
                onDismiss = dismissFullscreenEdit,
                fullScreen = true,
            )
        }
    }

    if (uiState.showDeleteDialog) {
        // Delete confirmation remains top-level so it can sit above the current sheet or fullscreen overlay.
        val desc = uiState.selectedEntry?.description.orEmpty()
        DeleteConfirmationDialog(
            title = stringResource(R.string.entries_delete_title, desc),
            message = stringResource(R.string.entries_delete_message),
            onConfirm = { onEvent(EntriesEvent.ConfirmDelete) },
            onDismiss = { onEvent(EntriesEvent.DismissDeleteDialog) },
        )
    }

}
