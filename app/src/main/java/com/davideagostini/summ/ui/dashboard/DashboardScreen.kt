package com.davideagostini.summ.ui.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davideagostini.summ.data.entity.AssetHistoryEntry
import com.davideagostini.summ.data.entity.Category
import com.davideagostini.summ.data.entity.Entry
import com.davideagostini.summ.ui.components.FullScreenLoading
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
) {
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
                    text = "Overview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
            }

            item {
                DashboardToolbar(
                    monthOptions = monthOptions,
                    selectedMonth = selectedMonth,
                    onSelectMonth = onSelectMonth,
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
                    label = "Assets",
                    value = formatEuro(metrics.totalAssets),
                    note = "Current value",
                )
            }

            item {
                MetricCard(
                    label = "Liabilities",
                    value = formatEuro(metrics.totalLiabilities),
                    note = "Outstanding",
                )
            }

            item {
                MetricCard(
                    label = "Cash flow",
                    value = formatEuro(metrics.monthlyCashFlow),
                    note = "Income - expenses",
                    valueColor = if (metrics.monthlyCashFlow >= 0) IncomeGreen else ExpenseRed,
                )
            }

            item {
                MetricCard(
                    label = "Savings rate",
                    value = metrics.savingsRate?.let { formatPercent(it) } ?: "N/A",
                    note = "Saved from income",
                    trendLabel = savingsRateDelta?.let {
                        "${if (it >= 0) "↑" else "↓"} ${formatPercent(abs(it))} vs 3M avg"
                    },
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
                    label = "Runway",
                    value = metrics.financialRunway?.let { formatRunwayMonths(it) } ?: "N/A",
                    trailingValue = metrics.financialRunway?.let { "/ ${formatRunwayYears(it)}" },
                )
            }

            item {
                MetricCard(
                    label = "Monthly expenses",
                    value = formatEuro(monthEntries.filter { it.type == "expense" }.sumOf { it.price }),
                )
            }
        }
    }
}
