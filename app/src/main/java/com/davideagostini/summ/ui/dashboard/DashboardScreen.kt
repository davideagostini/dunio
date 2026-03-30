package com.davideagostini.summ.ui.dashboard

// Dashboard screen orchestration: it reads aggregated finance state, syncs the shared month picker, and renders the KPI stack.
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
import com.davideagostini.summ.ui.components.FullScreenLoading
import com.davideagostini.summ.ui.components.MonthPickerOverlay
import com.davideagostini.summ.ui.components.buildRecentMonthOptions
import com.davideagostini.summ.ui.components.preferredRecentMonth
import com.davideagostini.summ.ui.dashboard.components.DashboardToolbar
import com.davideagostini.summ.ui.dashboard.components.MetricCard
import com.davideagostini.summ.ui.dashboard.components.NetWorthCard
import com.davideagostini.summ.ui.format.formatCurrency
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val renderState by viewModel.renderState.collectAsStateWithLifecycle()

    if (isLoading) {
        FullScreenLoading()
        return
    }

    DashboardContent(
        renderState = renderState,
        uiState = uiState,
        onSelectMonth = viewModel::selectMonth,
        onSelectRange = viewModel::selectRange,
        onMonthPickerVisibilityChanged = onMonthPickerVisibilityChanged,
    )
}

@Composable
private fun DashboardContent(
    renderState: DashboardRenderState,
    uiState: DashboardUiState,
    onSelectMonth: (String) -> Unit,
    onSelectRange: (DashboardRange) -> Unit,
    onMonthPickerVisibilityChanged: (Boolean) -> Unit,
) {
    var showMonthPicker by remember { mutableStateOf(false) }
    val currentMonth = YearMonth.now().toString()
    val monthOptions = remember { buildRecentMonthOptions() }
    // When the stored month is invalid or missing, fall back to the most recent available month.
    val defaultMonth = remember(monthOptions, currentMonth) {
        preferredRecentMonth(monthOptions, currentMonth)
    }
    val selectedMonth = uiState.selectedMonth
        ?.takeIf { it in monthOptions }
        ?: defaultMonth

    // Seed the dashboard with a valid month once, then let the ViewModel own the selected month state.
    LaunchedEffect(Unit) {
        if (defaultMonth.isNotBlank()) onSelectMonth(defaultMonth)
    }
    val metrics = renderState.metrics

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
            // The dashboard remains a vertically stacked mobile layout so the KPI cards stay readable on small screens.
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
                        currency = renderState.householdCurrency,
                        netWorth = metrics.netWorth,
                        monthlyChangePercent = renderState.monthlyChangePercent,
                        chartPoints = renderState.chartPoints,
                        selectedRange = renderState.selectedRange,
                        onSelectRange = onSelectRange,
                    )
                }

                item {
                    MetricCard(
                        label = stringResource(R.string.dashboard_assets_label),
                        value = formatCurrency(metrics.totalAssets, renderState.householdCurrency),
                        note = stringResource(R.string.dashboard_current_value),
                    )
                }

                item {
                    MetricCard(
                        label = stringResource(R.string.dashboard_liabilities_label),
                        value = formatCurrency(metrics.totalLiabilities, renderState.householdCurrency),
                        note = stringResource(R.string.dashboard_outstanding),
                    )
                }

                item {
                    MetricCard(
                        label = stringResource(R.string.dashboard_cash_flow_label),
                        value = formatCurrency(metrics.monthlyCashFlow, renderState.householdCurrency),
                        note = stringResource(R.string.dashboard_cash_flow_note),
                        trendLabel = renderState.cashFlowChangePercent?.let {
                            stringResource(R.string.dashboard_change_vs_previous_month, formatPercent(abs(it)))
                        },
                        trendPositive = renderState.cashFlowChangePercent?.let { it >= 0 },
                        trendColor = when {
                            renderState.cashFlowChangePercent == null -> MaterialTheme.colorScheme.onSurfaceVariant
                            renderState.cashFlowChangePercent > 0 -> IncomeGreen
                            renderState.cashFlowChangePercent < 0 -> ExpenseRed
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        valueColor = if (metrics.monthlyCashFlow >= 0) IncomeGreen else ExpenseRed,
                    )
                }

                item {
                    MetricCard(
                        label = stringResource(R.string.dashboard_savings_rate_label),
                        value = metrics.savingsRate?.let { formatPercent(it) } ?: stringResource(R.string.dashboard_value_not_available),
                        note = stringResource(R.string.dashboard_savings_rate_note),
                        trendLabel = renderState.savingsRateDelta?.let {
                            stringResource(R.string.dashboard_change_vs_3m_avg, formatPercent(abs(it)))
                        },
                        trendPositive = renderState.savingsRateDelta?.let { it >= 0 },
                        trendColor = when {
                            renderState.savingsRateDelta == null -> MaterialTheme.colorScheme.onSurfaceVariant
                            renderState.savingsRateDelta > 0 -> IncomeGreen
                            renderState.savingsRateDelta < 0 -> ExpenseRed
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }

                item {
                    MetricCard(
                        label = stringResource(R.string.dashboard_runway_label),
                        value = metrics.financialRunway?.let { formatRunwayMonths(it) } ?: stringResource(R.string.dashboard_value_not_available),
                        trailingValue = metrics.financialRunway?.let { "/ ${formatRunwayYears(it)}" },
                        trendLabel = renderState.runwayChangePercent?.let {
                            stringResource(R.string.dashboard_change_vs_previous_month, formatPercent(abs(it)))
                        },
                        trendPositive = renderState.runwayChangePercent?.let { it >= 0 },
                        trendColor = when {
                            renderState.runwayChangePercent == null -> MaterialTheme.colorScheme.onSurfaceVariant
                            renderState.runwayChangePercent > 0 -> IncomeGreen
                            renderState.runwayChangePercent < 0 -> ExpenseRed
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }

                item {
                    MetricCard(
                        label = stringResource(R.string.dashboard_monthly_expenses_label),
                        value = formatCurrency(renderState.monthlyExpenses, renderState.householdCurrency),
                        trendLabel = renderState.monthlyExpensesChangePercent?.let {
                            stringResource(R.string.dashboard_change_vs_previous_month, formatPercent(abs(it)))
                        },
                    trendPositive = renderState.monthlyExpensesChangePercent?.let { it >= 0 },
                    trendColor = when {
                        renderState.monthlyExpensesChangePercent == null -> MaterialTheme.colorScheme.onSurfaceVariant
                        renderState.monthlyExpensesChangePercent < 0 -> IncomeGreen
                        renderState.monthlyExpensesChangePercent > 0 -> ExpenseRed
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
