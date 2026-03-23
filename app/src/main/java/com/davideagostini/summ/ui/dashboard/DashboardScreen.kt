package com.davideagostini.summ.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davideagostini.summ.R
import com.davideagostini.summ.data.entity.AssetHistoryEntry
import com.davideagostini.summ.data.entity.Category
import com.davideagostini.summ.data.entity.Entry
import com.davideagostini.summ.ui.components.FullScreenLoading
import com.davideagostini.summ.ui.components.MonthPickerOverlay
import com.davideagostini.summ.ui.components.buildRecentMonthOptions
import com.davideagostini.summ.ui.components.preferredRecentMonth
import com.davideagostini.summ.ui.dashboard.components.DashboardToolbar
import com.davideagostini.summ.ui.dashboard.components.MetricCard
import com.davideagostini.summ.ui.dashboard.components.NetWorthCard
import com.davideagostini.summ.ui.format.formatEuro
import com.davideagostini.summ.ui.theme.ExpenseRed
import com.davideagostini.summ.ui.theme.IncomeGreen
import java.time.YearMonth
import kotlin.math.abs

@Composable
fun DashboardScreen(
    onMonthPickerVisibilityChanged: (Boolean) -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val assetHistory by viewModel.assetHistory.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (isLoading) {
        FullScreenLoading()
        return
    }

    DashboardContent(
        assetHistory = assetHistory,
        entries = entries,
        categories = categories,
        uiState = uiState,
        onSelectMonth = viewModel::selectMonth,
        onSelectRange = viewModel::selectRange,
        onMonthPickerVisibilityChanged = onMonthPickerVisibilityChanged,
    )
}

@Composable
private fun DashboardContent(
    assetHistory: List<AssetHistoryEntry>,
    entries: List<Entry>,
    categories: List<Category>,
    uiState: DashboardUiState,
    onSelectMonth: (String) -> Unit,
    onSelectRange: (DashboardRange) -> Unit,
    onMonthPickerVisibilityChanged: (Boolean) -> Unit,
) {
    var showMonthPicker by remember { mutableStateOf(false) }
    val currentMonth = YearMonth.now().toString()
    val monthOptions = remember { buildRecentMonthOptions() }
    val defaultMonth = remember(monthOptions, currentMonth) {
        preferredRecentMonth(monthOptions, currentMonth)
    }
    val selectedMonth = uiState.selectedMonth
        ?.takeIf { it in monthOptions }
        ?: defaultMonth

    LaunchedEffect(Unit) {
        if (defaultMonth.isNotBlank()) onSelectMonth(defaultMonth)
    }

    val assetsForMonth = remember(assetHistory, selectedMonth) {
        buildAssetsSnapshotForMonth(assetHistory, selectedMonth)
    }
    val monthEntries = remember(entries, selectedMonth) {
        entries.filter { entry -> monthKey(entry.date) == selectedMonth }
    }
    val metrics = remember(assetsForMonth, monthEntries, entries, selectedMonth) {
        calculateDashboardMetrics(assetsForMonth, monthEntries, entries, selectedMonth)
    }
    val averageSavingsRate = remember(entries, selectedMonth) {
        calculateAverageSavingsRate(entries, selectedMonth, 3)
    }
    val savingsRateDelta = if (metrics.savingsRate != null && averageSavingsRate != null) {
        metrics.savingsRate - averageSavingsRate
    } else {
        null
    }
    val rangeMonths = remember(monthOptions, selectedMonth, uiState.selectedRange) {
        buildSeriesMonths(monthOptions, selectedMonth, uiState.selectedRange.months)
    }
    val chartPoints = remember(rangeMonths, assetHistory) {
        rangeMonths.map { month ->
            ChartPoint(
                month = month,
                label = formatShortMonth(month),
                value = calculateNetWorthForMonth(assetHistory, month),
            )
        }
    }
    val previousMonth = remember(selectedMonth) {
        YearMonth.parse(selectedMonth).minusMonths(1).toString()
    }
    val previousAssetsForMonth = remember(assetHistory, previousMonth) {
        buildAssetsSnapshotForMonth(assetHistory, previousMonth)
    }
    val previousMonthEntries = remember(entries, previousMonth) {
        entries.filter { entry -> monthKey(entry.date) == previousMonth }
    }
    val previousMetrics = remember(previousAssetsForMonth, previousMonthEntries, entries, previousMonth) {
        calculateDashboardMetrics(previousAssetsForMonth, previousMonthEntries, entries, previousMonth)
    }
    val currentValue = metrics.netWorth
    val previousValue = remember(assetHistory, previousMonth) {
        calculateNetWorthForMonth(assetHistory, previousMonth)
            .takeIf { hasActiveSnapshotForMonth(assetHistory, previousMonth) }
    }
    val monthlyChange = previousValue?.let { currentValue - it }
    val monthlyChangePercent = if (previousValue != null && previousValue != 0.0) {
        monthlyChange?.div(abs(previousValue))
    } else {
        null
    }
    val cashFlowChangePercent = remember(metrics.monthlyCashFlow, previousMonthEntries) {
        calculateRelativeChange(
            currentValue = metrics.monthlyCashFlow,
            previousValue = previousMetrics.monthlyCashFlow.takeIf { previousMonthEntries.isNotEmpty() },
        )
    }
    val monthlyExpenses = remember(monthEntries) {
        monthEntries.filter { it.type == "expense" }.sumOf { it.price }
    }
    val previousMonthlyExpenses = remember(previousMonthEntries) {
        previousMonthEntries.filter { it.type == "expense" }.sumOf { it.price }
    }
    val monthlyExpensesChangePercent = remember(monthlyExpenses, previousMonthEntries) {
        calculateRelativeChange(
            currentValue = monthlyExpenses,
            previousValue = previousMonthlyExpenses.takeIf { previousMonthEntries.isNotEmpty() },
        )
    }
    val runwayChangePercent = remember(metrics.financialRunway, previousMetrics.financialRunway, previousMonthEntries) {
        calculateRelativeChange(
            currentValue = metrics.financialRunway ?: return@remember null,
            previousValue = previousMetrics.financialRunway?.takeIf { previousMonthEntries.isNotEmpty() },
        )
    }

    // Keep the shared bottom bar hidden while the month picker overlay is open.
    LaunchedEffect(showMonthPicker) {
        onMonthPickerVisibilityChanged(showMonthPicker)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
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
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.dashboard_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    )
                }

                item {
                    DashboardToolbar(
                        selectedMonth = selectedMonth,
                        onOpenMonthPicker = { showMonthPicker = true },
                    )
                }

                item {
                    NetWorthCard(
                        month = selectedMonth,
                        netWorth = metrics.netWorth,
                        monthlyChangePercent = monthlyChangePercent,
                        chartPoints = chartPoints,
                        selectedRange = uiState.selectedRange,
                        onSelectRange = onSelectRange,
                    )
                }

                item {
                    MetricCard(
                        label = stringResource(R.string.dashboard_assets_label),
                        value = formatEuro(metrics.totalAssets),
                        note = stringResource(R.string.dashboard_current_value),
                    )
                }

                item {
                    MetricCard(
                        label = stringResource(R.string.dashboard_liabilities_label),
                        value = formatEuro(metrics.totalLiabilities),
                        note = stringResource(R.string.dashboard_outstanding),
                    )
                }

                item {
                    MetricCard(
                        label = stringResource(R.string.dashboard_cash_flow_label),
                        value = formatEuro(metrics.monthlyCashFlow),
                        note = stringResource(R.string.dashboard_cash_flow_note),
                        trendLabel = cashFlowChangePercent?.let {
                            stringResource(R.string.dashboard_change_vs_previous_month, formatPercent(abs(it)))
                        },
                        trendPositive = cashFlowChangePercent?.let { it >= 0 },
                        trendColor = when {
                            cashFlowChangePercent == null -> MaterialTheme.colorScheme.onSurfaceVariant
                            cashFlowChangePercent > 0 -> IncomeGreen
                            cashFlowChangePercent < 0 -> ExpenseRed
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        valueColor = if (metrics.monthlyCashFlow >= 0) IncomeGreen else ExpenseRed,
                    )
                }

                item {
                    MetricCard(
                        label = stringResource(R.string.dashboard_savings_rate_label),
                        value = metrics.savingsRate?.let { formatPercent(it) } ?: "N/A",
                        note = stringResource(R.string.dashboard_savings_rate_note),
                        trendLabel = savingsRateDelta?.let {
                            stringResource(R.string.dashboard_change_vs_3m_avg, formatPercent(abs(it)))
                        },
                        trendPositive = savingsRateDelta?.let { it >= 0 },
                        trendColor = when {
                            savingsRateDelta == null -> MaterialTheme.colorScheme.onSurfaceVariant
                            savingsRateDelta > 0 -> IncomeGreen
                            savingsRateDelta < 0 -> ExpenseRed
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }

                item {
                    MetricCard(
                        label = stringResource(R.string.dashboard_runway_label),
                        value = metrics.financialRunway?.let { formatRunwayMonths(it) } ?: "N/A",
                        trailingValue = metrics.financialRunway?.let { "/ ${formatRunwayYears(it)}" },
                        trendLabel = runwayChangePercent?.let {
                            stringResource(R.string.dashboard_change_vs_previous_month, formatPercent(abs(it)))
                        },
                        trendPositive = runwayChangePercent?.let { it >= 0 },
                        trendColor = when {
                            runwayChangePercent == null -> MaterialTheme.colorScheme.onSurfaceVariant
                            runwayChangePercent > 0 -> IncomeGreen
                            runwayChangePercent < 0 -> ExpenseRed
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }

                item {
                    MetricCard(
                        label = stringResource(R.string.dashboard_monthly_expenses_label),
                        value = formatEuro(monthlyExpenses),
                        trendLabel = monthlyExpensesChangePercent?.let {
                            stringResource(R.string.dashboard_change_vs_previous_month, formatPercent(abs(it)))
                        },
                    trendPositive = monthlyExpensesChangePercent?.let { it >= 0 },
                    trendColor = when {
                        monthlyExpensesChangePercent == null -> MaterialTheme.colorScheme.onSurfaceVariant
                        monthlyExpensesChangePercent < 0 -> IncomeGreen
                        monthlyExpensesChangePercent > 0 -> ExpenseRed
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }

        MonthPickerOverlay(
            visible = showMonthPicker,
            selectedOption = selectedMonth,
            options = monthOptions,
            optionLabel = ::formatMonthOption,
            onSelect = onSelectMonth,
            onDismiss = { showMonthPicker = false },
        )
    }
}
