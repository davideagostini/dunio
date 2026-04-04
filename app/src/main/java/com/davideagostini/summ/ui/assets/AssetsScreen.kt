package com.davideagostini.summ.ui.assets

// Screen orchestration for the assets feature: this file wires state collection,
// sheet/fullscreen layering, and top-level overlays without owning business logic.
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davideagostini.summ.R
import com.davideagostini.summ.ui.assets.components.AssetActionSheet
import com.davideagostini.summ.ui.assets.components.AssetCard
import com.davideagostini.summ.ui.assets.components.AssetEditorScreen
import com.davideagostini.summ.ui.assets.components.AssetsSplitButton
import com.davideagostini.summ.ui.assets.components.AssetsSummaryCard
import com.davideagostini.summ.ui.assets.components.AssetsToolbar
import com.davideagostini.summ.ui.assets.components.EmptyAssetsState
import com.davideagostini.summ.ui.auth.components.AuthErrorCard
import com.davideagostini.summ.ui.components.DeleteConfirmationDialog
import com.davideagostini.summ.ui.components.MonthCloseReadOnlyBanner
import com.davideagostini.summ.ui.components.MonthPickerOverlay
import com.davideagostini.summ.ui.components.buildRecentMonthOptions
import com.davideagostini.summ.ui.components.preferredRecentMonth
import kotlinx.coroutines.launch

/**
 * Top-level assets screen.
 *
 * It coordinates loading placeholders, the list/summary content, and edit or action overlays while
 * delegating month logic and mutations to [AssetsViewModel].
 */
@Composable
fun AssetsRouteScreen(
    onFullscreenEditVisibilityChanged: (Boolean) -> Unit = {},
    onMonthPickerVisibilityChanged: (Boolean) -> Unit = {},
    openAddOnLaunch: Boolean = false,
    onOpenAddConsumed: () -> Unit = {},
) {
    var mountContent by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!mountContent) {
            withFrameNanos { }
            withFrameNanos { }
            mountContent = true
        }
    }

    if (!mountContent) {
        AssetsLoadingContent(
            selectedMonth = preferredRecentMonth(buildRecentMonthOptions()),
            uiState = AssetsUiState(),
        )
        return
    }

    AssetsScreen(
        onFullscreenEditVisibilityChanged = onFullscreenEditVisibilityChanged,
        onMonthPickerVisibilityChanged = onMonthPickerVisibilityChanged,
        openAddOnLaunch = openAddOnLaunch,
        onOpenAddConsumed = onOpenAddConsumed,
    )
}

/**
 * Top-level assets screen.
 *
 * It coordinates loading placeholders, the list/summary content, and edit or action overlays while
 * delegating month logic and mutations to [AssetsViewModel].
 */
@Composable
fun AssetsScreen(
    viewModel: AssetsViewModel = hiltViewModel(),
    onFullscreenEditVisibilityChanged: (Boolean) -> Unit = {},
    onMonthPickerVisibilityChanged: (Boolean) -> Unit = {},
    openAddOnLaunch: Boolean = false,
    onOpenAddConsumed: () -> Unit = {},
) {
    // Collect the feature state with lifecycle awareness so the UI only observes
    // active screens and avoids leaking recompositions in the background.
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isMonthRefreshing by viewModel.isMonthRefreshing.collectAsStateWithLifecycle()
    val renderState by viewModel.renderState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (isLoading) {
        AssetsLoadingContent(
            selectedMonth = uiState.selectedMonth ?: preferredRecentMonth(buildRecentMonthOptions()),
            uiState = uiState,
        )
        return
    }

    AssetsContent(
        renderState = renderState,
        uiState = uiState,
        isMonthRefreshing = isMonthRefreshing,
        onEvent = viewModel::handleEvent,
        onFullscreenEditVisibilityChanged = onFullscreenEditVisibilityChanged,
        onMonthPickerVisibilityChanged = onMonthPickerVisibilityChanged,
        openAddOnLaunch = openAddOnLaunch,
        onOpenAddConsumed = onOpenAddConsumed,
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

@Composable
private fun AssetsLoadingContent(
    selectedMonth: String,
    uiState: AssetsUiState,
) {
    val shimmer = rememberAssetsShimmerBrush()

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
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
            }

            item {
                AssetsToolbar(
                    selectedMonth = selectedMonth,
                    searchVisible = uiState.searchVisible,
                    searchQuery = uiState.searchQuery,
                    onOpenMonthPicker = {},
                    onToggleSearch = {},
                    onSearchQueryChange = {},
                )
            }

            item {
                SkeletonAssetsSummaryCard(shimmer)
            }

            items(4) {
                SkeletonAssetCard(shimmer)
            }
        }
    }
}

@Composable
private fun SkeletonAssetsSummaryCard(shimmer: Brush) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AssetsSkeletonBlock(
                brush = shimmer,
                modifier = Modifier
                    .width(120.dp)
                    .size(width = 120.dp, height = 14.dp),
            )
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
            AssetsSkeletonBlock(
                brush = shimmer,
                modifier = Modifier
                    .width(220.dp)
                    .size(width = 220.dp, height = 40.dp),
            )
            androidx.compose.foundation.layout.Spacer(Modifier.size(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    AssetsSkeletonBlock(
                        brush = shimmer,
                        modifier = Modifier
                            .width(72.dp)
                            .size(width = 72.dp, height = 12.dp),
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                    AssetsSkeletonBlock(
                        brush = shimmer,
                        modifier = Modifier
                            .width(96.dp)
                            .size(width = 96.dp, height = 18.dp),
                    )
                }
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    AssetsSkeletonBlock(
                        brush = shimmer,
                        modifier = Modifier
                            .width(84.dp)
                            .size(width = 84.dp, height = 12.dp),
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                    AssetsSkeletonBlock(
                        brush = shimmer,
                        modifier = Modifier
                            .width(96.dp)
                            .size(width = 96.dp, height = 18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SkeletonAssetCard(shimmer: Brush) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 2.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AssetsSkeletonBlock(
                brush = shimmer,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            ) {
                AssetsSkeletonBlock(
                    brush = shimmer,
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .size(width = 0.dp, height = 16.dp),
                )
                androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                AssetsSkeletonBlock(
                    brush = shimmer,
                    modifier = Modifier
                        .fillMaxWidth(0.35f)
                        .size(width = 0.dp, height = 12.dp),
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                AssetsSkeletonBlock(
                    brush = shimmer,
                    modifier = Modifier
                        .width(84.dp)
                        .size(width = 84.dp, height = 16.dp),
                )
                androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                AssetsSkeletonBlock(
                    brush = shimmer,
                    modifier = Modifier
                        .width(56.dp)
                        .size(width = 56.dp, height = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun AssetsSkeletonBlock(
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
private fun rememberAssetsShimmerBrush(): Brush {
    val base = MaterialTheme.colorScheme.surfaceContainerHighest
    val highlight = MaterialTheme.colorScheme.surfaceBright
    val transition = rememberInfiniteTransition(label = "assets_shimmer")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "assets_shimmer_offset",
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
private fun AssetsContent(
    renderState: AssetsRenderState,
    uiState: AssetsUiState,
    isMonthRefreshing: Boolean,
    onEvent: (AssetsEvent) -> Unit,
    onFullscreenEditVisibilityChanged: (Boolean) -> Unit,
    onMonthPickerVisibilityChanged: (Boolean) -> Unit,
    openAddOnLaunch: Boolean,
    onOpenAddConsumed: () -> Unit,
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
    val selectedMonth = renderState.selectedMonth.ifBlank { preferredRecentMonth(monthOptions) }
    val isMonthClosed = renderState.isMonthClosed
    val shimmer = rememberAssetsShimmerBrush()
    var showHeavyBody by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!showHeavyBody) {
            withFrameNanos { }
            showHeavyBody = true
        }
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

    // Dashboard get-started can request the same add flow used by the floating action button.
    LaunchedEffect(openAddOnLaunch) {
        if (openAddOnLaunch) {
            onEvent(AssetsEvent.StartAdd)
            onOpenAddConsumed()
        }
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

            if (isMonthRefreshing) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            }

            if (isMonthClosed) {
                item {
                    Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp)) {
                        MonthCloseReadOnlyBanner(
                            message = stringResource(
                                R.string.month_close_read_only_message,
                                formatMonthLabel(selectedMonth),
                            ),
                        )
                    }
                }
            }

            if (!showHeavyBody) {
                item {
                    SkeletonAssetsSummaryCard(shimmer)
                }

                items(4) {
                    SkeletonAssetCard(shimmer)
                }
            } else {
                item {
                    AssetsSummaryCard(
                        currency = renderState.householdCurrency,
                        totalAssets = renderState.totalAssets,
                        totalLiabilities = renderState.totalLiabilities,
                        netWorth = renderState.netWorth,
                    )
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

                if (renderState.filteredAssets.isEmpty()) {
                    item {
                        EmptyAssetsState(hasAssets = renderState.hasAnyAssets)
                    }
                } else {
                    items(renderState.filteredAssets.size, key = { index -> renderState.filteredAssets[index].asset.id }) { index ->
                        val item = renderState.filteredAssets[index]
                        AssetCard(
                            asset = item.asset,
                            currency = renderState.householdCurrency,
                            index = index,
                            count = renderState.filteredAssets.size,
                            change = item.change,
                            readOnly = isMonthClosed,
                            onClick = { onEvent(AssetsEvent.Select(item.asset)) },
                        )
                    }
                }
            }
        }

        AssetsSplitButton(
            canCopyPreviousMonth = renderState.canCopyPreviousMonth,
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

}
