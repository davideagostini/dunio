package com.davideagostini.summ.ui.assets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davideagostini.summ.R
import com.davideagostini.summ.data.entity.AssetHistoryEntry
import com.davideagostini.summ.ui.assets.components.AssetActionSheet
import com.davideagostini.summ.ui.assets.components.AssetCard
import com.davideagostini.summ.ui.assets.components.AssetsSplitButton
import com.davideagostini.summ.ui.assets.components.AssetsSummaryCard
import com.davideagostini.summ.ui.assets.components.AssetsToolbar
import com.davideagostini.summ.ui.assets.components.EmptyAssetsState
import com.davideagostini.summ.ui.components.FullScreenLoading
import com.davideagostini.summ.ui.components.MonthPickerOverlay
import com.davideagostini.summ.ui.components.buildRecentMonthOptions
import com.davideagostini.summ.ui.components.preferredRecentMonth
import com.davideagostini.summ.ui.theme.AppButtonShape
import com.davideagostini.summ.ui.theme.ExpenseRed
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.util.Locale

@Composable
fun AssetsScreen(
    viewModel: AssetsViewModel = hiltViewModel(),
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val assetHistory by viewModel.assetHistory.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (isLoading) {
        FullScreenLoading()
        return
    }

    AssetsContent(
        assetHistory = assetHistory,
        uiState = uiState,
        onEvent = viewModel::handleEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssetsContent(
    assetHistory: List<AssetHistoryEntry>,
    uiState: AssetsUiState,
    onEvent: (AssetsEvent) -> Unit,
) {
    var allowSheetHide by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || allowSheetHide },
    )
    val monthOptions = remember { buildRecentMonthOptions() }
    val selectedMonth = uiState.selectedMonth ?: preferredRecentMonth(monthOptions)
    val monthAssets = remember(assetHistory, selectedMonth) {
        buildAssetsSnapshotForMonth(assetHistory, selectedMonth)
    }
    val filteredAssets = remember(monthAssets, uiState.searchQuery) {
        monthAssets.filter { asset ->
            val query = uiState.searchQuery.trim()
            query.isBlank() ||
                asset.name.contains(query, ignoreCase = true) ||
                asset.category.contains(query, ignoreCase = true) ||
                asset.currency.contains(query, ignoreCase = true)
        }
    }
    val totalAssets = monthAssets.filter { it.type == "asset" }.sumOf { it.value }
    val totalLiabilities = monthAssets.filter { it.type == "liability" }.sumOf { it.value }
    val netWorth = totalAssets - totalLiabilities
    val previousMonthAssets = remember(assetHistory, selectedMonth) {
        buildAssetsSnapshotForMonth(assetHistory, YearMonth.parse(selectedMonth).minusMonths(1).toString())
    }
    val canCopyPreviousMonth = remember(monthAssets, previousMonthAssets) {
        val currentNames = monthAssets.map { it.name.trim().lowercase(Locale.getDefault()) }.toSet()
        previousMonthAssets.any { it.name.trim().lowercase(Locale.getDefault()) !in currentNames }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 140.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.dashboard_assets_label),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
            }

            item {
                AssetsToolbar(
                    selectedMonth = selectedMonth,
                    searchVisible = uiState.searchVisible,
                    searchQuery = uiState.searchQuery,
                    onOpenMonthPicker = { showMonthPicker = true },
                    onToggleSearch = { onEvent(AssetsEvent.ToggleSearch) },
                    onSearchQueryChange = { onEvent(AssetsEvent.UpdateSearchQuery(it)) },
                )
            }

            item {
                AssetsSummaryCard(
                    totalAssets = totalAssets,
                    totalLiabilities = totalLiabilities,
                    netWorth = netWorth,
                )
            }

            if (filteredAssets.isEmpty()) {
                item {
                    EmptyAssetsState(hasAssets = monthAssets.isNotEmpty() || assetHistory.isNotEmpty())
                }
            } else {
                items(filteredAssets.size, key = { index -> filteredAssets[index].id }) { index ->
                    val asset = filteredAssets[index]
                    AssetCard(
                        asset = asset,
                        index = index,
                        count = filteredAssets.size,
                        change = calculateAssetChange(assetHistory, asset.name, selectedMonth),
                        onClick = { onEvent(AssetsEvent.Select(asset)) },
                    )
                }
            }
        }

        AssetsSplitButton(
            canCopyPreviousMonth = canCopyPreviousMonth,
            onAddAsset = { onEvent(AssetsEvent.StartAdd) },
            onCopyPreviousMonth = { onEvent(AssetsEvent.CopyPreviousMonth) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 94.dp),
        )

        MonthPickerOverlay(
            visible = showMonthPicker,
            selectedOption = selectedMonth,
            options = monthOptions,
            optionLabel = ::formatMonthLabel,
            onSelect = { onEvent(AssetsEvent.SelectMonth(it)) },
            onDismiss = { showMonthPicker = false },
        )
    }

    if (uiState.sheetMode != AssetSheetMode.Hidden) {
        ModalBottomSheet(
            onDismissRequest = {},
            sheetState = sheetState,
            containerColor = Color.Transparent,
            dragHandle = null,
            scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
        ) {
            AssetActionSheet(
                uiState = uiState,
                onEvent = onEvent,
                onDismiss = {
                    scope.launch {
                        allowSheetHide = true
                        sheetState.hide()
                        onEvent(AssetsEvent.DismissSheet)
                        allowSheetHide = false
                    }
                },
            )
        }
    }

    if (uiState.showDeleteDialog) {
        val name = uiState.selectedAsset?.name.orEmpty()
        AlertDialog(
            onDismissRequest = { onEvent(AssetsEvent.DismissDeleteDialog) },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            title = { Text(stringResource(R.string.assets_delete_title, name), fontWeight = FontWeight.SemiBold) },
            text = { Text(stringResource(R.string.assets_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = { onEvent(AssetsEvent.ConfirmDelete) },
                    shape = AppButtonShape,
                ) {
                    Text(stringResource(R.string.action_delete), color = ExpenseRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onEvent(AssetsEvent.DismissDeleteDialog) },
                    shape = AppButtonShape,
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
