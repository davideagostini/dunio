package com.davideagostini.summ.ui.entries

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.davideagostini.summ.domain.model.HomeState
import com.davideagostini.summ.ui.components.FullScreenLoading
import com.davideagostini.summ.ui.components.buildRecentMonthOptions
import com.davideagostini.summ.ui.components.preferredRecentMonth
import com.davideagostini.summ.ui.entries.components.BalanceCard
import com.davideagostini.summ.ui.entries.components.DayGroupSection
import com.davideagostini.summ.ui.entries.components.EmptyState
import com.davideagostini.summ.ui.entries.components.EntriesToolbar
import com.davideagostini.summ.ui.entries.components.EntryActionSheet
import com.davideagostini.summ.ui.entries.components.UnusualSpendingCard
import com.davideagostini.summ.ui.theme.AppButtonShape
import com.davideagostini.summ.ui.theme.ExpenseRed
import kotlinx.coroutines.launch

@Composable
fun EntriesScreen(viewModel: EntriesViewModel = hiltViewModel()) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val homeState by viewModel.homeState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (isLoading) {
        FullScreenLoading()
        return
    }

    EntriesContent(
        homeState = homeState,
        categories = categories,
        uiState = uiState,
        onEvent = viewModel::handleEvent,
        onFullscreenEditVisibilityChanged = {},
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EntriesContent(
    homeState: HomeState,
    categories: List<Category>,
    uiState: EntriesUiState,
    onEvent: (EntriesEvent) -> Unit,
    onFullscreenEditVisibilityChanged: (Boolean) -> Unit,
) {
    var allowSheetHide by remember { mutableStateOf(false) }
    var showFullScreenEdit by remember { mutableStateOf(uiState.sheetMode == EntrySheetMode.Edit) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || allowSheetHide },
    )
    val monthOptions = remember { buildRecentMonthOptions() }
    val selectedMonth = uiState.selectedMonth
        ?: preferredRecentMonth(monthOptions)

    val monthEntries = remember(homeState.entries, selectedMonth) {
        homeState.entries.filter { entry -> monthKey(entry.date) == selectedMonth }
    }
    val visibleEntries = remember(monthEntries, uiState.filterType, uiState.searchQuery) {
        monthEntries.filter { entry ->
            matchesFilter(entry, uiState.filterType) && matchesSearch(entry, uiState.searchQuery)
        }
    }
    val dayGroups = remember(visibleEntries) { buildDayGroups(visibleEntries) }
    val unusualSpendingInsights = remember(homeState.entries, selectedMonth) {
        buildUnusualSpendingInsights(homeState.entries, selectedMonth)
    }

    val totalExpenses = monthEntries.sumOf { entry -> if (entry.type == "expense") entry.price else 0.0 }
    val totalIncome = monthEntries.sumOf { entry -> if (entry.type == "income") entry.price else 0.0 }
    val monthLabel = formatMonthLabel(selectedMonth)

    LaunchedEffect(uiState.sheetMode) {
        if (uiState.sheetMode == EntrySheetMode.Edit) {
            showFullScreenEdit = true
        } else if (uiState.sheetMode == EntrySheetMode.Hidden) {
            showFullScreenEdit = false
        }
    }
    LaunchedEffect(showFullScreenEdit) {
        onFullscreenEditVisibilityChanged(showFullScreenEdit)
    }

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
                        text = "Activity",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    )
                }

                item {
                    EntriesToolbar(
                        monthOptions = monthOptions,
                        selectedMonth = selectedMonth,
                        searchVisible = uiState.searchVisible,
                        searchQuery = uiState.searchQuery,
                        onSelectMonth = { onEvent(EntriesEvent.SelectMonth(it)) },
                        onToggleSearch = { onEvent(EntriesEvent.ToggleSearch) },
                        onSearchQueryChange = { onEvent(EntriesEvent.UpdateSearchQuery(it)) },
                    )
                }

                item {
                    BalanceCard(
                        monthLabel = monthLabel,
                        expenses = totalExpenses,
                        income = totalIncome,
                        netCashFlow = totalIncome - totalExpenses,
                        filterType = uiState.filterType,
                        onFilterSelected = { onEvent(EntriesEvent.SelectFilter(it)) },
                    )
                }

                item {
                    if (unusualSpendingInsights.isNotEmpty()) {
                        UnusualSpendingCard(
                            insights = unusualSpendingInsights,
                        )
                    }
                }

                if (dayGroups.isEmpty()) {
                    item {
                        EmptyState(
                            message = if (homeState.entries.isEmpty()) {
                                "No entries yet.\nTap Add Entry to get started."
                            } else {
                                "No entries match the current month, filter, or search."
                            }
                        )
                    }
                } else {
                    items(dayGroups, key = { it.key.toString() }) { group ->
                        DayGroupSection(
                            group = group,
                            onEntryClick = { onEvent(EntriesEvent.Select(it)) },
                        )
                    }
                }
            }
        }

        if (uiState.sheetMode != EntrySheetMode.Hidden && uiState.sheetMode != EntrySheetMode.Edit) {
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
            visible = showFullScreenEdit,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)),
            ) {
                EntryActionSheet(
                    uiState = uiState,
                    categories = categories,
                    onEvent = onEvent,
                    onDismiss = dismissFullscreenEdit,
                    fullScreen = true,
                )
            }
        }
    }

    if (uiState.showDeleteDialog) {
        val desc = uiState.selectedEntry?.description.orEmpty()
        AlertDialog(
            onDismissRequest = { onEvent(EntriesEvent.DismissDeleteDialog) },
            shape = RoundedCornerShape(20.dp),
            title = { Text(stringResource(R.string.entries_delete_title, desc), fontWeight = FontWeight.SemiBold) },
            text = {
                Text(
                    stringResource(R.string.entries_delete_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onEvent(EntriesEvent.ConfirmDelete) },
                    shape = AppButtonShape,
                    colors = ButtonDefaults.textButtonColors(contentColor = ExpenseRed),
                ) {
                    Text(stringResource(R.string.action_delete), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onEvent(EntriesEvent.DismissDeleteDialog) },
                    shape = AppButtonShape,
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
