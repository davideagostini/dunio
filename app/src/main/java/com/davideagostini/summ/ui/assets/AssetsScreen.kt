package com.davideagostini.summ.ui.assets

// Screen orchestration for the assets feature: this file wires state collection,
// sheet/fullscreen layering, and top-level overlays without owning business logic.
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.davideagostini.summ.data.entity.MonthClose
import com.davideagostini.summ.ui.assets.components.AssetActionSheet
import com.davideagostini.summ.ui.assets.components.AssetCard
import com.davideagostini.summ.ui.assets.components.AssetEditorScreen
import com.davideagostini.summ.ui.assets.components.AssetsSplitButton
import com.davideagostini.summ.ui.assets.components.AssetsSummaryCard
import com.davideagostini.summ.ui.assets.components.AssetsToolbar
import com.davideagostini.summ.ui.assets.components.EmptyAssetsState
import com.davideagostini.summ.ui.auth.components.AuthErrorCard
import com.davideagostini.summ.ui.components.DeleteConfirmationDialog
import com.davideagostini.summ.ui.components.FullScreenLoading
import com.davideagostini.summ.ui.components.MonthCloseReadOnlyBanner
import com.davideagostini.summ.ui.components.MonthPickerOverlay
import com.davideagostini.summ.ui.components.buildRecentMonthOptions
import com.davideagostini.summ.ui.components.preferredRecentMonth
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.util.Locale

@Composable
fun AssetsScreen(
    viewModel: AssetsViewModel = hiltViewModel(),
    onFullscreenEditVisibilityChanged: (Boolean) -> Unit = {},
    onMonthPickerVisibilityChanged: (Boolean) -> Unit = {},
) {
    // Collect the feature state with lifecycle awareness so the UI only observes
    // active screens and avoids leaking recompositions in the background.
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val assetHistory by viewModel.assetHistory.collectAsStateWithLifecycle()
    val monthCloses by viewModel.monthCloses.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (isLoading) {
        FullScreenLoading()
        return
    }

    AssetsContent(
        assetHistory = assetHistory,
        monthCloses = monthCloses,
        uiState = uiState,
        onEvent = viewModel::handleEvent,
        onFullscreenEditVisibilityChanged = onFullscreenEditVisibilityChanged,
        onMonthPickerVisibilityChanged = onMonthPickerVisibilityChanged,
    )

    // Keep the delete confirmation outside the content tree so it can overlay the
    // action sheet consistently, matching the categories flow.
    if (uiState.showDeleteDialog) {
        val name = uiState.selectedAsset?.name.orEmpty()
        DeleteConfirmationDialog(
            title = stringResource(R.string.assets_delete_title, name),
            message = stringResource(R.string.assets_delete_message),
            onConfirm = { viewModel.handleEvent(AssetsEvent.ConfirmDelete) },
            onDismiss = { viewModel.handleEvent(AssetsEvent.DismissDeleteDialog) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssetsContent(
    assetHistory: List<AssetHistoryEntry>,
    monthCloses: List<MonthClose>,
    uiState: AssetsUiState,
    onEvent: (AssetsEvent) -> Unit,
    onFullscreenEditVisibilityChanged: (Boolean) -> Unit,
    onMonthPickerVisibilityChanged: (Boolean) -> Unit,
) {
    // Local UI flags stay inside the composable because they only describe how the
    // current screen is presented, not app data that must survive process death.
    var allowSheetHide by remember { mutableStateOf(false) }
    var showFullScreenEditor by remember { mutableStateOf(uiState.sheetMode == AssetSheetMode.Add || uiState.sheetMode == AssetSheetMode.Edit) }
    var showMonthPicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || allowSheetHide },
    )
    val monthOptions = remember { buildRecentMonthOptions() }
    val selectedMonth = uiState.selectedMonth ?: preferredRecentMonth(monthOptions)
    val isMonthClosed = remember(monthCloses, selectedMonth) {
        monthCloses.any { it.period == selectedMonth && it.status == "closed" }
    }
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
        // The copy action is only enabled when the previous month contains at least
        // one asset that is not already present in the selected month.
        val currentNames = monthAssets.map { it.name.trim().lowercase(Locale.getDefault()) }.toSet()
        previousMonthAssets.any { it.name.trim().lowercase(Locale.getDefault()) !in currentNames }
    }

    // Keep the shared bottom bar hidden while the month picker overlay is open.
    LaunchedEffect(showMonthPicker) {
        onMonthPickerVisibilityChanged(showMonthPicker)
    }

    // Add/Edit own the fullscreen editor flow. Save success stays there too, so
    // the presentation never snaps back to the compact sheet after a write.
    LaunchedEffect(uiState.sheetMode) {
        if (uiState.sheetMode == AssetSheetMode.Add || uiState.sheetMode == AssetSheetMode.Edit) {
            showFullScreenEditor = true
        } else if (uiState.sheetMode == AssetSheetMode.Hidden) {
            showFullScreenEditor = false
        }
    }

    // The nav host needs to know when the fullscreen editor is active so the shared bottom bar disappears.
    LaunchedEffect(showFullScreenEditor) {
        onFullscreenEditVisibilityChanged(showFullScreenEditor)
    }

    // Dismissing the fullscreen editor waits for the exit animation before resetting
    // the ViewModel state, so the UI never cuts off mid-transition.
    val dismissFullscreenEditor: () -> Unit = {
        showFullScreenEditor = false
        scope.launch {
            kotlinx.coroutines.delay(220)
            onEvent(AssetsEvent.DismissSheet)
        }
    }

    BackHandler(enabled = showFullScreenEditor) {
        // Preserve the same exit path for both the back button and the close action.
        dismissFullscreenEditor()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        // The main content stays in the background while overlays and sheets are
        // layered above it in the order defined below.
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
                    color = MaterialTheme.colorScheme.onSurface,
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

            if (isMonthClosed) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        MonthCloseReadOnlyBanner(
                            message = stringResource(
                                R.string.month_close_read_only_message,
                                formatMonthLabel(selectedMonth),
                            ),
                        )
                    }
                }
            }

            if (uiState.operationErrorMessage != null && uiState.sheetMode == AssetSheetMode.Hidden) {
                item {
                    // Top-level actions such as "Copy previous month" are not tied to a sheet, so show the same
                    // error card directly in the screen content when a write fails.
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        AuthErrorCard(uiState.operationErrorMessage)
                    }
                }
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
                        readOnly = isMonthClosed,
                        onClick = { onEvent(AssetsEvent.Select(asset)) },
                    )
                }
            }
        }

        AssetsSplitButton(
            canCopyPreviousMonth = canCopyPreviousMonth,
            readOnly = isMonthClosed,
            onAddAsset = { if (!isMonthClosed) onEvent(AssetsEvent.StartAdd) },
            onCopyPreviousMonth = { if (!isMonthClosed) onEvent(AssetsEvent.CopyPreviousMonth) },
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

    // The bottom sheet is now reserved for the compact action/success states only.
    // Fullscreen editor flows stay in their own overlay so the two presentations do
    // not compete for the same container.
    if ((uiState.sheetMode == AssetSheetMode.Action || uiState.sheetMode == AssetSheetMode.Success) && !showFullScreenEditor) {
        ModalBottomSheet(
            onDismissRequest = {},
            sheetState = sheetState,
            containerColor = Color.Transparent,
            dragHandle = null,
            scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
        ) {
            AssetActionSheet(
                uiState = uiState,
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
                        onEvent(AssetsEvent.DismissSheet)
                        allowSheetHide = false
                    }
                },
            )
        }
    }

    AnimatedVisibility(
        visible = showFullScreenEditor,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier.fillMaxSize(),
    ) {
        // The fullscreen editor uses an explicit scrim so it feels like a dedicated
        // screen, not a nested sheet state.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)),
        ) {
            AssetEditorScreen(
                uiState = uiState,
                readOnly = isMonthClosed,
                readOnlyMessage = stringResource(
                    R.string.month_close_edit_disabled_message,
                    formatMonthLabel(selectedMonth),
                ),
                onEvent = onEvent,
                onDismiss = dismissFullscreenEditor,
            )
        }
    }

    if (uiState.showDeleteDialog) {
        val name = uiState.selectedAsset?.name.orEmpty()
        DeleteConfirmationDialog(
            title = stringResource(R.string.assets_delete_title, name),
            message = stringResource(R.string.assets_delete_message),
            onConfirm = { onEvent(AssetsEvent.ConfirmDelete) },
            onDismiss = { onEvent(AssetsEvent.DismissDeleteDialog) },
        )
    }

}
