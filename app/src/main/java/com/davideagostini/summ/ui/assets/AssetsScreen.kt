package com.davideagostini.summ.ui.assets

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davideagostini.summ.R
import com.davideagostini.summ.data.entity.Asset
import com.davideagostini.summ.data.entity.AssetHistoryEntry
import com.davideagostini.summ.ui.components.FullScreenLoading
import com.davideagostini.summ.ui.components.MonthPickerField
import com.davideagostini.summ.ui.components.buildRecentMonthOptions
import com.davideagostini.summ.ui.components.preferredRecentMonth
import com.davideagostini.summ.ui.format.formatAmount
import com.davideagostini.summ.ui.format.formatEuro
import com.davideagostini.summ.ui.theme.ExpenseRed
import com.davideagostini.summ.ui.theme.IncomeGreen
import com.davideagostini.summ.ui.theme.listItemShape
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
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
        monthAssets
            .filter { asset ->
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
                    text = "Assets",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
            }

            item {
                AssetsToolbar(
                    monthOptions = monthOptions,
                    selectedMonth = selectedMonth,
                    searchVisible = uiState.searchVisible,
                    searchQuery = uiState.searchQuery,
                    onSelectMonth = { onEvent(AssetsEvent.SelectMonth(it)) },
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
            shape = RoundedCornerShape(20.dp),
            title = { Text(stringResource(R.string.assets_delete_title, name), fontWeight = FontWeight.SemiBold) },
            text = { Text(stringResource(R.string.assets_delete_message)) },
            confirmButton = {
                TextButton(onClick = { onEvent(AssetsEvent.ConfirmDelete) }) {
                    Text(stringResource(R.string.action_delete), color = ExpenseRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(AssetsEvent.DismissDeleteDialog) }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun AssetsSplitButton(
    canCopyPreviousMonth: Boolean,
    onAddAsset: () -> Unit,
    onCopyPreviousMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.wrapContentSize(Alignment.BottomEnd),
        contentAlignment = Alignment.BottomEnd,
    ) {
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            offset = DpOffset(x = 0.dp, y = (-8).dp),
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            shadowElevation = 6.dp,
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.assets_add_asset)) },
                onClick = {
                    menuExpanded = false
                    onAddAsset()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.assets_copy_prev_month)) },
                enabled = canCopyPreviousMonth,
                onClick = {
                    menuExpanded = false
                    onCopyPreviousMonth()
                },
            )
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 6.dp,
            tonalElevation = 0.dp,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .clickable(onClick = onAddAsset)
                        .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.content_desc_add_asset))
                    Text(
                        text = stringResource(R.string.assets_add_asset),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .width(1.dp)
                        .height(24.dp),
                ) {}

                IconButton(onClick = { menuExpanded = !menuExpanded }) {
                    Icon(
                        imageVector = Icons.Outlined.UnfoldMore,
                        contentDescription = stringResource(R.string.assets_more_actions),
                    )
                }
            }
        }
    }
}

@Composable
private fun AssetsToolbar(
    monthOptions: List<String>,
    selectedMonth: String,
    searchVisible: Boolean,
    searchQuery: String,
    onSelectMonth: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonthPickerField(
                label = formatMonthLabel(selectedMonth),
                options = monthOptions,
                optionLabel = ::formatMonthLabel,
                onSelect = onSelectMonth,
                modifier = Modifier.weight(1f),
            )

            Surface(
                shape = CircleShape,
                color = if (searchVisible) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLowest
                },
                tonalElevation = 2.dp,
                modifier = Modifier.size(60.dp),
            ) {
                IconButton(onClick = onToggleSearch) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = stringResource(R.string.content_desc_search_assets),
                        tint = if (searchVisible) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }

        if (searchVisible) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                placeholder = { Text(stringResource(R.string.assets_search_placeholder)) },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.content_desc_clear_search),
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(22.dp),
            )
        }
    }
}

@Composable
private fun AssetsSummaryCard(
    totalAssets: Double,
    totalLiabilities: Double,
    netWorth: Double,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.assets_net_worth),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatCurrency(netWorth),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryStatCard(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.assets_label),
                    value = formatCurrency(totalAssets),
                    accent = IncomeGreen,
                )
                SummaryStatCard(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.liabilities_label),
                    value = formatCurrency(totalLiabilities),
                    accent = ExpenseRed,
                )
            }
        }
    }
}

@Composable
private fun SummaryStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accent: Color,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
        }
    }
}

@Composable
private fun AssetCard(
    asset: Asset,
    index: Int,
    count: Int,
    change: Double?,
    onClick: () -> Unit,
) {
    val shape = listItemShape(index, count)
    val verticalPadding = when {
        count == 1 -> PaddingValues(horizontal = 20.dp, vertical = 2.dp)
        index == 0 -> PaddingValues(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 1.dp)
        index == count - 1 -> PaddingValues(start = 20.dp, end = 20.dp, top = 1.dp, bottom = 2.dp)
        else -> PaddingValues(start = 20.dp, end = 20.dp, top = 1.dp, bottom = 1.dp)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(verticalPadding)
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.AccountBalanceWallet,
                contentDescription = null,
                tint = if (asset.type == "asset") IncomeGreen else ExpenseRed,
                modifier = Modifier.size(28.dp),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = asset.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = asset.category.ifBlank {
                        if (asset.type == "asset") stringResource(R.string.asset_type_asset) else stringResource(R.string.asset_type_liability)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(asset.value, asset.currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (asset.type == "asset") IncomeGreen else ExpenseRed,
                )
                if (change != null) {
                    Spacer(Modifier.height(4.dp))
                    val changeColor = when {
                        kotlin.math.abs(change) < 0.0005 -> MaterialTheme.colorScheme.onSurfaceVariant
                        change > 0 -> IncomeGreen
                        else -> ExpenseRed
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = if (change >= 0) Icons.Outlined.ArrowOutward else Icons.Outlined.ArrowDownward,
                            contentDescription = null,
                            tint = changeColor,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = buildChangeLabel(change),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = changeColor,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAssetsState(hasAssets: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(76.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(34.dp),
                )
            }
        }
        Text(
            text = if (hasAssets) stringResource(R.string.assets_empty_filtered) else stringResource(R.string.assets_empty),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = if (hasAssets) {
                stringResource(R.string.assets_empty_filtered_message)
            } else {
                stringResource(R.string.assets_empty_message)
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AssetActionSheet(
    uiState: AssetsUiState,
    onEvent: (AssetsEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.content_desc_close))
                }
            }
            AnimatedContent(targetState = uiState.sheetMode, label = "asset_sheet_mode") { mode ->
                when (mode) {
                    AssetSheetMode.Action -> AssetActionContent(uiState = uiState, onEvent = onEvent)
                    AssetSheetMode.Add -> AssetFormContent(
                        title = stringResource(R.string.asset_new_title),
                        confirmLabel = stringResource(R.string.action_create),
                        uiState = uiState,
                        onEvent = onEvent,
                        onSave = { onEvent(AssetsEvent.SaveAdd) },
                        onCancel = onDismiss,
                    )
                    AssetSheetMode.Edit -> AssetFormContent(
                        title = stringResource(R.string.asset_edit_title),
                        confirmLabel = stringResource(R.string.action_save),
                        uiState = uiState,
                        onEvent = onEvent,
                        onSave = { onEvent(AssetsEvent.SaveEdit) },
                        onCancel = onDismiss,
                    )
                    AssetSheetMode.Success -> AssetSuccessContent()
                    AssetSheetMode.Hidden -> Unit
                }
            }
        }
    }
}

@Composable
private fun AssetActionContent(
    uiState: AssetsUiState,
    onEvent: (AssetsEvent) -> Unit,
) {
    val asset = uiState.selectedAsset ?: return
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null)
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(text = asset.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            text = formatCurrency(asset.value, asset.currency),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = { onEvent(AssetsEvent.RequestDelete) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(4.dp))
                Text(stringResource(R.string.action_delete), color = ExpenseRed)
            }

            Button(
                onClick = { onEvent(AssetsEvent.StartEdit) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(4.dp))
                Text(stringResource(R.string.action_edit))
            }
        }
    }
}

@Composable
private fun AssetFormContent(
    title: String,
    confirmLabel: String,
    uiState: AssetsUiState,
    onEvent: (AssetsEvent) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Column {
        Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        AssetTypeButtonGroup(
            selectedType = uiState.editType,
            onSelectType = { onEvent(AssetsEvent.UpdateType(it)) },
        )

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.editName,
            onValueChange = { onEvent(AssetsEvent.UpdateName(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.asset_name_label)) },
            singleLine = true,
            isError = uiState.nameError != null,
            supportingText = uiState.nameError?.let { msg -> { Text(msg) } },
            shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.editCategory,
            onValueChange = { onEvent(AssetsEvent.UpdateCategory(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.asset_category_label)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = uiState.editValue,
                onValueChange = { onEvent(AssetsEvent.UpdateValue(it)) },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.asset_value_label)) },
                singleLine = true,
                isError = uiState.valueError != null,
                supportingText = uiState.valueError?.let { msg -> { Text(msg) } },
                shape = RoundedCornerShape(12.dp),
            )
            OutlinedTextField(
                value = uiState.editCurrency,
                onValueChange = { onEvent(AssetsEvent.UpdateCurrency(it)) },
                modifier = Modifier.weight(0.42f),
                label = { Text(stringResource(R.string.asset_currency_label)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
        }

        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(confirmLabel)
            }
        }
    }
}

@Composable
private fun AssetTypeButtonGroup(
    selectedType: String,
    onSelectType: (String) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        listOf(
            "asset" to stringResource(R.string.asset_type_asset),
            "liability" to stringResource(R.string.asset_type_liability),
        ).forEachIndexed { index, item ->
            val selected = selectedType == item.first
            Button(
                onClick = { onSelectType(item.first) },
                modifier = Modifier.weight(1f),
                shape = when (index) {
                    0 -> RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp, topEnd = 6.dp, bottomEnd = 6.dp)
                    else -> RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 18.dp, bottomEnd = 18.dp)
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLowest,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(item.second)
            }
        }
    }
}

@Composable
private fun AssetSuccessContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(IncomeGreen.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("✓", fontSize = 36.sp, color = IncomeGreen, fontWeight = FontWeight.Bold)
        }
        Text(text = "Done!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            text = "Asset updated successfully.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatCurrency(value: Double, currency: String = "EUR"): String =
    when (currency.uppercase()) {
        "EUR" -> formatEuro(value)
        else -> "$currency ${formatAmount(value)}"
    }

private fun buildAssetsSnapshotForMonth(entries: List<AssetHistoryEntry>, month: String): List<Asset> =
    entries
        .filter { it.period == month }
        .sortedWith(
            compareByDescending<AssetHistoryEntry> { it.period }
                .thenByDescending { it.snapshotDate }
        )
        .associateBy { it.name.trim().lowercase(Locale.getDefault()) }
        .values
        .filter { it.action != "deleted" }
        .map { entry ->
            Asset(
                id = entry.assetId,
                householdId = entry.householdId,
                name = entry.name,
                type = entry.type,
                category = entry.category,
                value = entry.value,
                currency = entry.currency,
                liquid = entry.liquid,
                period = entry.period,
                snapshotDate = entry.snapshotDate,
            )
        }
        .sortedByDescending { it.value }

private fun formatMonthLabel(month: String): String =
    YearMonth.parse(month).format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()))

private fun calculateAssetChange(
    historyEntries: List<AssetHistoryEntry>,
    assetName: String,
    month: String,
): Double? {
    val normalizedName = assetName.trim().lowercase(Locale.getDefault())
    val currentEntry = historyEntries.find { entry ->
        entry.name.trim().lowercase(Locale.getDefault()) == normalizedName &&
            entry.period == month &&
            entry.action != "deleted"
    }

    val previousEntry = historyEntries
        .filter { entry ->
            entry.name.trim().lowercase(Locale.getDefault()) == normalizedName &&
                entry.period < month &&
                entry.action != "deleted"
        }
        .sortedWith(
            compareByDescending<AssetHistoryEntry> { it.period }
                .thenByDescending { it.snapshotDate }
        )
        .firstOrNull()

    if (currentEntry == null || previousEntry == null || previousEntry.value == 0.0) {
        return null
    }

    return (currentEntry.value - previousEntry.value) / kotlin.math.abs(previousEntry.value)
}

private fun buildChangeLabel(change: Double): String =
    when {
        kotlin.math.abs(change) < 0.0005 -> "0.0%"
        else -> "${if (change > 0) "+" else ""}${"%.1f".format(change * 100)}%"
    }
